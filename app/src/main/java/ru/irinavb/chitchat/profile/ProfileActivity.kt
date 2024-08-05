package ru.irinavb.chitchat.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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
import ru.irinavb.chitchat.databinding.ActivityProfileBinding
import ru.irinavb.chitchat.login.LoginActivity

@Suppress("NAME_SHADOWING")
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    /** Auth */
    private lateinit var firebaseAuth: FirebaseAuth
    private var firebaseUser: FirebaseUser? = null

    /** Db */
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference

    /** Photo Uri */
    private var localFileUri: Uri? = null
    private var serverFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser

        storageReference = FirebaseStorage.getInstance().reference

        if (firebaseUser != null) {
            firebaseUser!!.displayName?.let {
                binding.etName.setText(it)
            }
            firebaseUser!!.email?.let {
                binding.etEmail.setText(it)
            }
            firebaseUser!!.photoUrl?.let {
                serverFileUri = it
            }

            Glide.with(this)
                .load(serverFileUri)
                .placeholder(R.drawable.ic_user_default)
                .error(R.drawable.ic_user_default)
                .into(binding.ivProfile)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuLogout) {
            btnLogOutClick()
        }
        return super.onOptionsItemSelected(item)
    }

    private val imagePickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            localFileUri = result.data?.data
            binding.ivProfile.setImageURI(localFileUri)
        }
    }

    fun btnSaveClick(v: View) {
        if (binding.etName.text.toString().trim() == "") {
            binding.etName.error = "Enter name"
        } else {
            if (localFileUri != null) {
                updateUserInfoAndPhoto()
            } else {
                updateUserInfo()
            }
        }
    }

    fun btnLogOutClick() {
        firebaseAuth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun changeImage(v: View) {
        if (serverFileUri != null) {
            pickImage()
        } else {
            val popupMenu = PopupMenu(this, v)
            popupMenu.menuInflater.inflate(R.menu.menu_picture, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menuChangePicture -> {
                        pickImage()
                        true
                    }

                    R.id.menuRemovePicture -> {
                        removePhoto()
                        true
                    }

                    else -> {

                        true
                    }
                }
            }
        }
    }

    private fun removePhoto() {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.etName.toString().trim())
            .setPhotoUri(null)
            .build()
        firebaseUser?.updateProfile(request)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = firebaseUser?.uid
                databaseReference = FirebaseDatabase.getInstance().reference.child(NodeNames.USERS)

                val userMap = mapOf(
                    NodeNames.PHOTO to "",
                )

                databaseReference.child(userId!!).setValue(userMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Photo Removed Successfully", Toast.LENGTH_LONG).show()
                    } else {
                        showError("Failed to save user data: ${task.exception?.message}")
                    }
                }
            } else {
                showError("Failed to update profile: ${task.exception?.message}")
            }
        }
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    private fun updateUserInfo() {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.etName.text.toString().trim())
            .build()

        firebaseUser?.updateProfile(request)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = firebaseUser?.uid
                databaseReference = FirebaseDatabase.getInstance().reference.child(NodeNames.USERS)

                val userMap = mapOf(
                    NodeNames.NAME to binding.etName.text.toString().trim(),
                )

                databaseReference.child(userId!!).setValue(userMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Successfully updated user", Toast.LENGTH_LONG).show()
                    } else {
                        showError("Failed to save user data: ${task.exception?.message}")
                    }
                }
            } else {
                showError("Failed to update profile: ${task.exception?.message}")
            }
        }
    }

    private fun updateUserInfoAndPhoto() {
        val fileName = "${firebaseUser?.uid}.jpg"
        val storageRef = storageReference.child("images/$fileName")

        localFileUri?.let { uri ->
            storageRef.putFile(uri).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        serverFileUri = uri
                        val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(binding.etName.text.toString().trim())
                            .setPhotoUri(serverFileUri)
                            .build()

                        firebaseUser?.updateProfile(request)?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = firebaseUser?.uid
                                databaseReference =
                                    FirebaseDatabase.getInstance().reference.child(NodeNames.USERS)

                                val userMap = mapOf(
                                    NodeNames.NAME to binding.etName.text.toString(),
                                    NodeNames.PHOTO to serverFileUri.toString()
                                )

                                databaseReference.child(userId!!).setValue(userMap)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(this, "Successfully updated user", Toast.LENGTH_LONG).show()
                                        } else {
                                            showError("Failed to save user data: ${task.exception?.message}")
                                        }
                                    }
                            } else {
                                showError("Failed to update profile: ${task.exception?.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}














