package hu.cock.ripvessel.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.cock.ripvessel.SessionManager
import hu.gyulakiri.ripvessel.api.AuthV2Api
import hu.gyulakiri.ripvessel.model.AuthLoginV2Request
import hu.gyulakiri.ripvessel.model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authApi: AuthV2Api
) : AndroidViewModel(application) {
    // Use a StateFlow to hold the error message
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> get() = _errorMessage

    // Create an instance of SessionManager using the Application context
    private val sessionManager = SessionManager(application.applicationContext)

    /**
     * Call the login API on a background thread. If successful, clear the error and invoke [onSuccess].
     * Otherwise, update the error message.
     */
    fun login(username: String, password: String, onSuccess: (UserModel) -> Unit) {
        viewModelScope.launch {
            try {
                // Call the login API and get the full HTTP info to access cookies
                val response = withContext(Dispatchers.IO) {
                    authApi.loginWithHttpInfo(AuthLoginV2Request(username, password))
                }
                // Log all Set-Cookie headers for debugging
                val setCookieHeaders = response.headers["Set-Cookie"] ?: emptyList()
                setCookieHeaders.forEach { cookie ->
                    android.util.Log.d("LoginViewModel", "Set-Cookie: $cookie")
                }
                // Find the relevant auth cookie (adjust name if needed)
                val authCookie = setCookieHeaders.find { it.startsWith("sails.sid") }
                if (authCookie != null) {
                    sessionManager.saveAuthCookie(authCookie)
                }
                // Cast response to Success<AuthLoginV2Response?> to access data
                val successResponse = response as? org.openapitools.client.infrastructure.Success<hu.gyulakiri.ripvessel.model.AuthLoginV2Response?>
                val user = successResponse?.data?.user
                if (user != null) {
                    sessionManager.saveUser(user)
                    _errorMessage.value = ""
                    onSuccess(user)
                } else if (successResponse?.data?.needs2FA == true) {
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