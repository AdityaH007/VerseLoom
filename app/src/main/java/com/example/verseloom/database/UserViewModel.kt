package com.example.verseloom.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {
    val allUsers: Flow<List<UserData>> = userDao.getAll()

    suspend fun updateUser(userData: UserData) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.update(userData)
        }
    }

    suspend fun insertUser(userData: UserData) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.insertAll(userData)
        }
    }

    suspend fun deleteUser(userData: UserData) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.delete(userData)
        }
    }
}