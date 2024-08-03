package ru.irinavb.chitchat.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import ru.irinavb.chitchat.databinding.ActivityLoginBinding
import ru.irinavb.chitchat.util.Validation

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var email: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun btnLoginClick(v: View) {
        email = binding.etEmail.text.toString().trim()
        password = binding.etPassword.text.toString().trim()

        Validation.validateEmail(context = this, textInputLayout = binding.tilEmail,  email = email)
        Validation.validatePasswordForLogin(context = this, textInputLayout = binding
            .tilPassword,  password = password)

        if (binding.tilEmail.error.isNullOrEmpty() &&
            binding.tilPassword.error.isNullOrEmpty()
        ) {
            val firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Successfully logged in", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login Failed: ${task.exception}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}