package ru.irinavb.chitchat.password

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import ru.irinavb.chitchat.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun btnChangePasswordClick(v: View) {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (password == "") {
            binding.etPassword.error = "Enter password"
        } else if (confirmPassword == "") {
            binding.etConfirmPassword.error = "Enter confirm password"
        } else if(password != confirmPassword) {
            binding.etPassword.error = "Passwords do not match"
        } else {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser

            firebaseUser?.updatePassword(password)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password was changed successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Something went wrong: ${task.exception}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}