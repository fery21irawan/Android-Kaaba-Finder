package id.uviwi.kaabafinder.utils

import android.content.SharedPreferences

class Helper(private val prefs: SharedPreferences) {
    fun saveString(title: String?, tex: String?) {
        val edit = prefs.edit()
        edit.putString(title, tex)
        edit.apply()
    }

    fun setString(title: String?): String? {
        return prefs.getString(title, "")
    }

    fun saveBoolean(title: String?, bbb: Boolean?) {
        val edit = prefs.edit()
        edit.putBoolean(title, bbb!!)
        edit.apply()
    }

    fun getBoolean(title: String?): Boolean {
        return prefs.getBoolean(title, false)
    }

    fun saveLong(title: String?, bbb: Long?) {
        val edit = prefs.edit()
        edit.putLong(title, bbb!!)
        edit.apply()
    }

    fun getLong(title: String?): Long {
        return prefs.getLong(title, 0)
    }

    fun saveFloat(title: String?, bbb: Float?) {
        val edit = prefs.edit()
        edit.putFloat(title, bbb!!)
        edit.apply()
    }

    fun getFloat(title: String?): Float {
        return prefs.getFloat(title, 0f)
    }
}