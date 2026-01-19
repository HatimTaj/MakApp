package com.hatim.makmanager.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.User
import com.hatim.makmanager.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repo = AuthRepository()
    val loginState = MutableLiveData<Resource<User>>()

    fun login(email: String, pass: String) {
        if(email.isEmpty() || pass.isEmpty()) {
            loginState.value = Resource.Error("Fields cannot be empty")
            return
        }

        loginState.value = Resource.Loading
        viewModelScope.launch {
            loginState.value = repo.login(email, pass)
        }
    }
}