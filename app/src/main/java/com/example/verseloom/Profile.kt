package com.example.verseloom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class Profile : AppCompatActivity() {
    private lateinit var pfpview: CardView
    private lateinit var pfp: ImageView
    private lateinit var name: TextView
    private lateinit var etname: EditText
    private lateinit var editbtn: Button
    private lateinit var saveBtn: Button
    private lateinit var sharedPreferences: SharedPreferences

    //to store current yser data
    private var currentUser: User? = null

    //store image uri temporarily before upload
    private var selectedImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission required to select image", Toast.LENGTH_SHORT).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                val compressedBitmap = compressImage(uri)
                pfp.setImageBitmap(compressedBitmap)
                saveImageToInternalStorage(compressedBitmap)
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

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        pfpview = findViewById(R.id.pfpcv)
        pfp = findViewById(R.id.pfp)
        name = findViewById(R.id.name)
        etname = findViewById(R.id.etname)
        editbtn = findViewById(R.id.editbutton)
        saveBtn = findViewById(R.id.savebutton)

        // Load saved name when activity starts
        loadSavedName()

        pfp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        editbtn.setOnClickListener {
            etname.setText(name.text) // Set current name in EditText
            etname.visibility = View.VISIBLE
            name.visibility = View.GONE
            saveBtn.visibility = View.VISIBLE
            editbtn.visibility = View.GONE
        }

        saveBtn.setOnClickListener {
            val uname = etname.text.toString()

            if (uname.isNotEmpty()) {
                saveName(uname)
                etname.visibility = View.GONE
                name.visibility = View.VISIBLE
                saveBtn.visibility = View.GONE
                editbtn.visibility = View.VISIBLE
                name.text = uname
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        loadSavedProfilePicture()
    }

    private fun saveName(userName: String) {
        sharedPreferences.edit().apply {
            putString("USER_NAME", userName)
            apply()
        }
    }

    private fun loadSavedName() {
        val savedName = sharedPreferences.getString("USER_NAME", "Venom Snake") // Default name if none saved
        name.text = savedName
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun compressImage(uri: Uri): Bitmap {
        // Load the original bitmap
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Calculate new dimensions while maintaining aspect ratio
        val maxDimension = 1024 // Max width or height
        val scale = if (originalBitmap.width > originalBitmap.height) {
            maxDimension.toFloat() / originalBitmap.width
        } else {
            maxDimension.toFloat() / originalBitmap.height
        }

        val newWidth = (originalBitmap.width * scale).toInt()
        val newHeight = (originalBitmap.height * scale).toInt()

        // Create scaled bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        try {
            val file = File(filesDir, "profile_picture.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            fos.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadSavedProfilePicture() {
        val file = File(filesDir, "profile_picture.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            pfp.setImageBitmap(bitmap)
        }
    }
}