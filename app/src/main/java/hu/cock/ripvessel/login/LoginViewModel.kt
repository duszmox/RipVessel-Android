package hu.cock.ripvessel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import hu.gyulakiri.ripvessel.api.AuthV2Api
import hu.gyulakiri.ripvessel.model.AuthLoginV2Request
import hu.gyulakiri.ripvessel.model.UserModel

class LoginViewModel : ViewModel() {
    // Use a StateFlow to hold the error message
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> get() = _errorMessage

    /**
     * Call the login API on a background thread. If successful, clear error and invoke [onSuccess].
     * Otherwise, update the error message.
     */
    fun login(username: String, password: String, onSuccess: (UserModel) -> Unit) {
        viewModelScope.launch {
            try {
                // Call the login API on the IO dispatcher to ensure thread-safety.
                val response = withContext(Dispatchers.IO) {
                    AuthV2Api().login(AuthLoginV2Request(username, password))
                }
                if (response.user != null) {
                    _errorMessage.value = ""
                    onSuccess(response.user)
                } else if (response.needs2FA) {
                    _errorMessage.value = "Two-factor authentication required"
                } else {
                    _errorMessage.value = "Invalid credentials"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Login failed"
            }
        }
    }
}