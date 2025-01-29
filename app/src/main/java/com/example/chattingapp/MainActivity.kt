package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binder: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference
    private lateinit var loadingContainer: FrameLayout
    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binder = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binder.root)
        
        auth = FirebaseAuth.getInstance()
        loadingContainer = binder.loadingContainer
        lottieAnimationView = binder.lottieAnimationView
        
        if (auth.currentUser != null) {
            startActivity(Intent(this, ChatActivity::class.java))
            finish()
            return
        }

        binder.signUpButton.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
            finish()
        }

        binder.signInButton.setOnClickListener {
            binder.signInButton.isEnabled = false
            loadingContainer.visibility = View.VISIBLE
            lottieAnimationView.playAnimation()
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binder.emailInput.text.toString().trim()
        val password = binder.passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            binder.signInButton.isEnabled = true
            loadingContainer.visibility = View.GONE
            lottieAnimationView.pauseAnimation()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        updateUserStatus(user.uid, "online")
                    } else {
                        handleLoginError("User is null after successful login")
                    }
                } else {
                    handleLoginError(task.exception?.message ?: "Unknown error")
                }
            }
    }

    private fun updateUserStatus(uid: String, status: String) {
        reference = FirebaseDatabase
            .getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")
            .child(uid)

        val updates = HashMap<String, Any>()
        updates["status"] = status

        reference.updateChildren(updates)
            .addOnCompleteListener { task ->
                binder.signInButton.isEnabled = true
                loadingContainer.visibility = View.GONE
                lottieAnimationView.pauseAnimation()

                if (task.isSuccessful) {
                    Log.d("MainActivity", "User status updated successfully")
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                } else {
                    handleLoginError(task.exception?.message ?: "Failed to update user status")
                }
            }
    }

    private fun handleLoginError(errorMessage: String) {
        binder.signInButton.isEnabled = true
        loadingContainer.visibility = View.GONE
        lottieAnimationView.pauseAnimation()
        Toast.makeText(this, "Login failed: $errorMessage", Toast.LENGTH_LONG).show()
        Log.e("MainActivity", "Login failed: $errorMessage")
    }
}