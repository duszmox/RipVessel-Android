package hu.cock.ripvessel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import hu.cock.ripvessel.SessionManager
import hu.gyulakiri.ripvessel.model.UserModel

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application.applicationContext)
    
    fun getUser(): UserModel? = sessionManager.getUser()
    
    fun logout() {
        sessionManager.clearSession()
    }
} 