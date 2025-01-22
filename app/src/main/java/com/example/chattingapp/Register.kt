package com.example.chattingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage


class Register : AppCompatActivity() {

    private lateinit var binder: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference
    private lateinit var loadingContainer: FrameLayout
    private lateinit var lottieAnimationView: com.airbnb.lottie.LottieAnimationView

    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binder.changeImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binder = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binder.root)
        auth = FirebaseAuth.getInstance()

        loadingContainer = findViewById(R.id.loadingContainer)
        lottieAnimationView = findViewById(R.id.lottieAnimationView)

        binder.signInButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binder.signUpButton.setOnClickListener {
            binder.signUpButton.isEnabled = false
            loadingContainer.visibility = View.VISIBLE
            lottieAnimationView.playAnimation()
            saveProfileData()
        }

        val openGallery = {
            pickImageLauncher.launch("image/*")
        }

        binder.changeImage.setOnClickListener { openGallery() }
        binder.changeImageButton.setOnClickListener { openGallery() }
    }

    private fun saveProfileData() {
        val userName = binder.usernameInput.text.toString().trim()
        val email = binder.emailInput.text.toString().trim()
        val password = binder.passwordInput.text.toString().trim()

        if (userName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            binder.signUpButton.isEnabled = true
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        if (imageUri != null) {
                            val storageRef = FirebaseStorage.getInstance()
                                .reference
                                .child("profile_images")
                                .child("$uid.jpg")

                            storageRef.putFile(imageUri!!)
                                .continueWithTask { task ->
                                    if (!task.isSuccessful) {
                                        task.exception?.let { throw it }
                                    }
                                    storageRef.downloadUrl
                                }.addOnCompleteListener { urlTask ->
                                    if (urlTask.isSuccessful) {
                                        val downloadUri = urlTask.result
                                        saveUserData(uid, userName, email, downloadUri.toString())
                                    } else {
                                        saveUserData(uid, userName, email, null)
                                    }
                                }
                        } else {
                            saveUserData(uid, userName, email, null)
                        }
                    } else {
                        Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show()
                        Log.e("Register", "User ID is null after successful authentication")
                        binder.signUpButton.isEnabled = true
                    }
                } else {
                    val errorMessage = authTask.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("Register", "Registration failed", authTask.exception)
                    binder.signUpButton.isEnabled = true
                }
            }
    }

    private fun saveUserData(uid: String, userName: String, email: String, profileImageUrl: String?) {
        reference = FirebaseDatabase
            .getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")
            .child(uid)

        val hashMap = HashMap<String, Any>()
        hashMap["uid"] = uid
        hashMap["username"] = userName
        hashMap["email"] = email
        hashMap["status"] = "offline"
        profileImageUrl?.let {
            hashMap["profileImageUrl"] = it
        }

        reference.setValue(hashMap)
            .addOnCompleteListener { dbTask ->
                binder.signUpButton.isEnabled = true
                if (dbTask.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    Log.d("Register", "User data saved successfully under UID: $uid")
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                } else {
                    val errorMessage = dbTask.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Profile update failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("Register", "Profile update failed", dbTask.exception)
                }
            }
    }
}