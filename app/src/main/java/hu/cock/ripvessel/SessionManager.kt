package hu.cock.ripvessel

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import hu.gyulakiri.ripvessel.model.UserModel

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    private val gson = Gson()

    fun saveUser(user: UserModel) {
        // Serialize the user object to a JSON string before saving.
        val userJson = gson.toJson(user)
        editor.putString("user", userJson)
        editor.apply()
    }

    fun getUser(): UserModel? {
        // Retrieve the JSON string and deserialize it back into a UserModel object.
        val userJson = prefs.getString("user", null)
        return if (userJson != null) {
            gson.fromJson(userJson, UserModel::class.java)
        } else {
            null
        }
    }

    fun clearSession() {
        // Remove the user data from SharedPreferences.
        editor.remove("user")
        editor.apply()
    }

    fun saveAuthCookie(cookie: String) {
        editor.putString("auth_cookie", cookie)
        editor.apply()
    }

    fun getAuthCookie(): String? = prefs.getString("auth_cookie", null)
}