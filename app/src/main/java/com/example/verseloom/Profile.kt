package com.example.verseloom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.verseloom.database.UserDao
import com.example.verseloom.database.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.output.ByteArrayOutputStream
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class Profile : AppCompatActivity() {
    private lateinit var pfpview: CardView
    private lateinit var pfp: ImageView
    private lateinit var name: TextView
    private lateinit var etname: EditText
    private lateinit var editbtn: Button
    private lateinit var saveBtn: Button
    private lateinit var bio: TextView
    private lateinit var editbio: EditText

    private lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var userDao: UserDao // Inject UserDao via Hilt

    private var selectedImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var firestoreListener: ListenerRegistration? = null // To manage the listener

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openImagePicker() else Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri // Store the original URI
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.Main).launch {
                    handleImageSelection(uri) // Process and upload image immediately
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        pfpview = findViewById(R.id.pfpcv)
        pfp = findViewById(R.id.pfp)
        name = findViewById(R.id.name)
        etname = findViewById(R.id.etname)
        editbtn = findViewById(R.id.editbutton)
        saveBtn = findViewById(R.id.savebutton)
        bio = findViewById(R.id.Bio)
        editbio = findViewById(R.id.etbio)

        loadProfileData()
        startFirestoreListener() // Start real-time sync

        pfp.setOnClickListener { requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES) }
        editbtn.setOnClickListener {
            etname.setText(name.text)
            etname.visibility = View.VISIBLE
            name.visibility = View.GONE

            editbio.setText(bio.text)
            editbio.visibility = View.VISIBLE
            bio.visibility = View.GONE

            saveBtn.visibility = View.VISIBLE
            editbtn.visibility = View.GONE
        }
        saveBtn.setOnClickListener { saveProfile() }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove() // Clean up the listener
    }

    private fun loadProfileData() {
        val savedName = sharedPreferences.getString("USER_NAME", null)
        val savedBio = sharedPreferences.getString("USER_BIO", null)

        if (savedName != null) {
            name.text = savedName
        }

        if (savedBio != null) {
            bio.text = savedBio
        } else {
            bio.text = "Add your bio here" // Default text
        }

        // If we don't have cached data, fetch from Firestore
        if (savedName == null || savedBio == null) {
            fetchProfileFromFirestore()
        }
    }

    private fun startFirestoreListener() {
        val uid = auth.currentUser?.uid ?: return
        firestoreListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Listener error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val name = it.getString("name")
                    val imageUrl = it.getString("imageUrl")
                    val userBio = it.getString("bio")

                    CoroutineScope(Dispatchers.Main).launch {
                        name?.let { n ->
                            saveToPrefs("USER_NAME", n)
                            this@Profile.name.text = n
                        }

                        userBio?.let { b ->
                            saveToPrefs("USER_BIO", b)
                            this@Profile.bio.text = b
                        }

                        imageUrl?.let { url ->
                            val bitmap = withContext(Dispatchers.IO) { downloadImage(url) }
                            bitmap?.let { bmp ->
                                pfp.setImageBitmap(bmp)
                                saveImageToInternalStorage(bmp) // Cache locally
                            }
                        }

                        // Update Room with the latest data
                        val user = UserData(
                            uid = uid.toIntOrNull() ?: 0,
                            name = name ?: "",
                            imageUrl = imageUrl ?: "",
                            bio = userBio ?: ""
                        )
                        withContext(Dispatchers.IO) {
                            userDao.upsert(user)
                        }
                    }
                }
            }
    }

    private fun fetchProfileFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                val name = document.getString("name")
                val imageUrl = document.getString("imageUrl")
                val userBio = document.getString("bio")

                withContext(Dispatchers.Main) {
                    name?.let {
                        saveToPrefs("USER_NAME", it)
                        this@Profile.name.text = it
                    }

                    userBio?.let {
                        saveToPrefs("USER_BIO", it)
                        this@Profile.bio.text = it
                    }

                    imageUrl?.let { url ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val bitmap = downloadImage(url)
                            bitmap?.let { bmp ->
                                withContext(Dispatchers.Main) {
                                    pfp.setImageBitmap(bmp)
                                    saveImageToInternalStorage(bmp) // Cache locally
                                }
                            }
                        }
                    }
                }

                // Update Room
                val user = UserData(
                    uid = uid.toIntOrNull() ?: 0,
                    name = name ?: "",
                    imageUrl = imageUrl ?: "",
                    bio = userBio ?: ""
                )
                userDao.upsert(user)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Profile, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun downloadImage(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = storage.getReferenceFromUrl(url).stream.await().stream
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val uname = etname.text.toString()
        val ubio = editbio.text.toString()

        if (uname.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Get current imageUrl to preserve it
                    val currentDoc = withContext(Dispatchers.IO) {
                        firestore.collection("users").document(uid).get().await()
                    }
                    val currentImageUrl = currentDoc.getString("imageUrl") ?: ""

                    // Update Firestore with both name and bio
                    val userData = hashMapOf(
                        "name" to uname,
                        "imageUrl" to currentImageUrl,
                        "bio" to ubio
                    )

                    withContext(Dispatchers.IO) {
                        firestore.collection("users").document(uid).set(userData).await()
                    }

                    // Save to SharedPreferences
                    saveToPrefs("USER_NAME", uname)
                    saveToPrefs("USER_BIO", ubio)

                    // Update Room
                    val user = UserData(
                        uid = uid.toIntOrNull() ?: 0,
                        name = uname,
                        imageUrl = currentImageUrl,
                        bio = ubio
                    )
                    withContext(Dispatchers.IO) {
                        userDao.upsert(user)
                    }

                    // Update UI
                    name.text = uname
                    bio.text = ubio

                    // Toggle visibility of edit/view fields
                    toggleEditMode(false)

                } catch (e: Exception) {
                    Toast.makeText(this@Profile, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleEditMode(editing: Boolean) {
        if (editing) {
            etname.visibility = View.VISIBLE
            name.visibility = View.GONE
            editbio.visibility = View.VISIBLE
            bio.visibility = View.GONE
            saveBtn.visibility = View.VISIBLE
            editbtn.visibility = View.GONE
        } else {
            etname.visibility = View.GONE
            name.visibility = View.VISIBLE
            editbio.visibility = View.GONE
            bio.visibility = View.VISIBLE
            saveBtn.visibility = View.GONE
            editbtn.visibility = View.VISIBLE
        }
    }

    private suspend fun handleImageSelection(uri: Uri) {
        try {
            val compressedBitmap = withContext(Dispatchers.IO) { compressImage(uri) }
            withContext(Dispatchers.Main) {
                pfp.setImageBitmap(compressedBitmap) // Update UI immediately
                saveImageToInternalStorage(compressedBitmap) // Cache locally
            }

            val uid = auth.currentUser?.uid ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Profile, "User not authenticated", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Upload to Firebase Storage
            val imageUrl = withContext(Dispatchers.IO) { uploadImageToStorage(uid) }
            imageUrl?.let { url ->
                // Get current user data to preserve other fields
                val currentDoc = withContext(Dispatchers.IO) {
                    firestore.collection("users").document(uid).get().await()
                }
                val currentName = currentDoc.getString("name") ?: ""
                val currentBio = currentDoc.getString("bio") ?: ""

                // Update Firestore with new image URL but keep other data
                val userData = hashMapOf(
                    "name" to currentName,
                    "imageUrl" to url,
                    "bio" to currentBio
                )

                withContext(Dispatchers.IO) {
                    firestore.collection("users").document(uid).set(userData).await()
                }

                // Update Room
                val user = UserData(
                    uid = uid.toIntOrNull() ?: 0,
                    name = currentName,
                    imageUrl = url,
                    bio = currentBio
                )
                withContext(Dispatchers.IO) {
                    userDao.upsert(user)
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Profile, "Failed to upload image to Firebase", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Profile, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadImageToStorage(uid: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child("profile_images/$fileName")
                val bitmap = selectedImageUri?.let { compressImage(it) }
                bitmap?.let {
                    val baos = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val data = baos.toByteArray()
                    val uploadTask = ref.putBytes(data).await()
                    ref.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Unified method to save preferences
    private fun saveToPrefs(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        try {
            val file = File(filesDir, "profile_picture.jpg")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image locally: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun compressImage(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close() // Close the stream to prevent leaks

        val maxDimension = 1024
        val scale = if (originalBitmap.width > originalBitmap.height) {
            maxDimension.toFloat() / originalBitmap.width
        } else {
            maxDimension.toFloat() / originalBitmap.height
        }
        val newWidth = (originalBitmap.width * scale).toInt()
        val newHeight = (originalBitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
}