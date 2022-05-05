package ru.irinavb.chitchat.util

import android.content.Context
import com.google.android.material.textfield.TextInputLayout
import ru.irinavb.chitchat.R

class Validation {
    companion object {
        fun validateEmail(context: Context, textInputLayout: TextInputLayout, email: String) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                textInputLayout.error = context.getString(R.string.invalid_email_format)
            }
            if (email == "") {
                textInputLayout.error = context.getString(R.string.enter_email)
            }
        }

        fun validatePasswordForLogin(context: Context, textInputLayout: TextInputLayout, password:
        String) {
            if (password == "") {
                textInputLayout.error = context.getString(R.string.enter_password)
            }
        }
    }
}