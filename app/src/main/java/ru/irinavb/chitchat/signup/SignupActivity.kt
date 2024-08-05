package ru.irinavb.chitchat.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import ru.irinavb.chitchat.R
import ru.irinavb.chitchat.common.NodeNames
import ru.irinavb.chitchat.databinding.ActivitySignupBinding
import ru.irinavb.chitchat.login.LoginActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var user: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var localFileUri: Uri? = null
    private lateinit var serverFileUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storageReference = FirebaseStorage.getInstance().reference
    }

    private val imagePickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            localFileUri = result.data?.data
            binding.ivProfile.setImageURI(localFileUri)
        }
    }

    fun pickImage(v: View) {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    fun btnSignupClick(v: View) {
        val email = binding.etEmail.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        when {
            email.isEmpty() -> binding.etEmail.error = getString(R.string.enter_email)
            name.isEmpty() -> binding.etName.error = getString(R.string.enter_name)
            password.isEmpty() -> binding.etPassword.error = getString(R.string.enter_password)
            confirmPassword.isEmpty() -> binding.etConfirmPassword.error =
                getString(R.string.enter_confirm_password)

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.etEmail.error =
                getString(R.string.enter_correct_email)

            password != confirmPassword -> binding.etConfirmPassword.error =
                getString(R.string.passwords_do_not_match)

            else -> createAccount(email, password, name)
        }
    }

    private fun createAccount(email: String, password: String, name: String) {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user = firebaseAuth.currentUser!!
                    if (localFileUri != null) {
                        updateUserInfoAndPhoto(name)
                    } else {
                        updateUserInfo(name)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Registration Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun updateUserInfo(name: String) {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        user.updateProfile(request).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveUserToDatabase(name, null)
            } else {
                showError("Failed to update profile: ${task.exception?.message}")
            }
        }
    }

    private fun updateUserInfoAndPhoto(name: String) {
        val fileName = "${user.uid}.jpg"
        val storageRef = storageReference.child("images/$fileName")

        localFileUri?.let { uri ->
            storageRef.putFile(uri).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        serverFileUri = uri
                        val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .setPhotoUri(serverFileUri)
                            .build()

                        user.updateProfile(request).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                saveUserToDatabase(name, serverFileUri.toString())
                            } else {
                                showError("Failed to update profile: ${task.exception?.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveUserToDatabase(name: String, photoUrl: String?) {
        val userId = user.uid
        databaseReference = FirebaseDatabase.getInstance().reference.child(NodeNames.USERS)

        val userMap = mapOf(
            NodeNames.NAME to binding.etName.text.toString().trim(),
            NodeNames.EMAIL to binding.etEmail.text.toString().trim(),
            NodeNames.ONLINE to true.toString(),
            NodeNames.PHOTO to (photoUrl ?: "")
        )

        databaseReference.child(userId).setValue(userMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Successfully created user", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
            } else {
                showError("Failed to save user data: ${task.exception?.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
