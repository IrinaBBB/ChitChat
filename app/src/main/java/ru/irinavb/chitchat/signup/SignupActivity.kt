package ru.irinavb.chitchat.signup

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
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

    private lateinit var email: String
    private lateinit var name: String
    private lateinit var password: String
    private lateinit var confirmPassword: String

    private lateinit var user: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var  storageReference: StorageReference
    private var localFileUri: Uri? = null
    private lateinit var serverFileUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storageReference = FirebaseStorage.getInstance().reference

    }

    fun pickImage(view: View) {
        Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show()
        ImagePicker.with(this)
            .crop()	    			//Crop image(Optional), Check Customization for more option
            .compress(1024)			//Final image size will be less than 1 MB(Optional)
            .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
            .start()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        localFileUri = data?.data
        binding.ivProfile.setImageURI(data?.data)
    }

    fun btnSignupClick(view: View) {
        email = binding.etEmail.text.toString().trim()
        name = binding.etName.text.toString().trim()
        password = binding.etPassword.text.toString().trim()
        confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email == "") {
            binding.etEmail.error = R.string.enter_email.toString()
        } else if (name == "") {
            binding.etName.error = R.string.enter_name.toString()
        } else if (password == "") {
            binding.etPassword.error = R.string.enter_password.toString()
        } else if (confirmPassword == "") {
            binding.etPassword.error = R.string.enter_confirm_password.toString()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = R.string.enter_correct_email.toString()
        } else if (password != confirmPassword) {
            binding.etPassword.error = R.string.passwords_do_not_match.toString()
        } else {
            val firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user = firebaseAuth.currentUser!!
                        if(localFileUri != null) {
                            updateUserInfoAndPhoto()
                        } else {
                            updateUserInfo()
                        }
                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            "Registration Failed: ${task.exception}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun updateUserInfo() {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.etName.toString().trim())
            .build()
        user.updateProfile(request).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = user.uid
                databaseReference = FirebaseDatabase.getInstance().reference.child(NodeNames.USERS)
                val hashMap = HashMap<String, String>()
                hashMap[NodeNames.NAME] = binding.etName.text.toString().trim()
                hashMap[NodeNames.EMAIL] = binding.etEmail.text.toString().trim()
                hashMap[NodeNames.ONLINE] = "true"
                hashMap[NodeNames.PHOTO] = ""
                databaseReference.child(userId).setValue(hashMap).addOnCompleteListener {
                    Toast.makeText(this, "Successfully created user", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignupActivity, LoginActivity::class.java))

                }
            } else {
                Toast.makeText(
                    this,
                    "Failed to update profile: ${task.exception}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUserInfoAndPhoto() {
        val fileName = user.uid + ".jpg"
        val storageReference = storageReference.child("images/$fileName")
        localFileUri?.let { storageReference.putFile(it) }?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                storageReference.downloadUrl.addOnSuccessListener {uri ->
                    serverFileUri = uri
                    val request = UserProfileChangeRequest.Builder()
                        .setDisplayName(binding.etName.toString().trim())
                        .setPhotoUri(serverFileUri)
                        .build()
                    user.updateProfile(request).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = user.uid
                            databaseReference = FirebaseDatabase.getInstance().reference.child(NodeNames.USERS)
                            val hashMap = HashMap<String, String>()
                            hashMap[NodeNames.NAME] = binding.etName.text.toString().trim()
                            hashMap[NodeNames.EMAIL] = binding.etEmail.text.toString().trim()
                            hashMap[NodeNames.ONLINE] = "true"
                            hashMap[NodeNames.PHOTO] = serverFileUri.path.toString()
                            databaseReference.child(userId).setValue(hashMap).addOnCompleteListener {
                                Toast.makeText(this, "Successfully created user", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to update profile: ${task.exception}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}


















