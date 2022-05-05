package ru.irinavb.chitchat.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import ru.irinavb.chitchat.R
import ru.irinavb.chitchat.databinding.ActivityLoginBinding
import ru.irinavb.chitchat.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    fun btnSignupClick(view: View) {}
}