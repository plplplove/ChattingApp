package com.example.chattingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chattingapp.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : BaseActivity() {
    private lateinit var binder: ActivityUserProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binder.changeImage.setImageURI(it)
            uploadImage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binder = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binder.root)

        auth = FirebaseAuth.getInstance()
        reference = FirebaseDatabase
            .getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")
            .child(auth.currentUser?.uid ?: "")

        loadUserProfile()

        binder.bottomNavigationMenu.selectedItemId = R.id.userHome
        binder.bottomNavigationMenu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    finish()
                    true
                }
                R.id.messages -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                    true
                }
                R.id.userHome -> {
                    true
                }
                else -> false
            }
        }

        val openGallery = {
            pickImageLauncher.launch("image/*")
        }

        binder.changeImage.setOnClickListener { openGallery() }
        binder.changeImageButton.setOnClickListener { openGallery() }

        binder.editProfileButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binder.username.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binder.email.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binder.logOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun uploadImage() {
        if (imageUri != null) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val storageRef = FirebaseStorage.getInstance()
                    .reference
                    .child("profile_images")
                    .child("$uid.jpg")

                storageRef.putFile(imageUri!!)
                    .addOnProgressListener { taskSnapshot ->
                        val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    }
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        storageRef.downloadUrl
                    }
                    .addOnCompleteListener { urlTask ->
                        if (urlTask.isSuccessful) {
                            val downloadUri = urlTask.result
                            updateProfileImageUrl(downloadUri.toString())
                        } else {
                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun updateProfileImageUrl(imageUrl: String) {
        val updates = HashMap<String, Any>()
        updates["profileImageUrl"] = imageUrl

        reference.updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update profile image", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadUserProfile() {
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    binder.username.text = username ?: "No username"

                    binder.email.text = email ?: "No email"

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@UserProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.user_photo)
                            .error(R.drawable.user_photo)
                            .into(binder.changeImage)
                    } else {
                        binder.changeImage.setImageResource(R.drawable.user_photo)
                    }
                } else {
                    Toast.makeText(this@UserProfileActivity, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@UserProfileActivity,
                    "Error loading profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}