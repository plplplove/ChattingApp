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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : AppCompatActivity() {

    private lateinit var binder: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference
    private lateinit var loadingContainer: FrameLayout
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binder = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binder.root)
        
        auth = FirebaseAuth.getInstance()
        loadingContainer = binder.loadingContainer
        lottieAnimationView = binder.lottieAnimationView

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("547067947014-oshfsh96n93hpea1up9r9a9nnfbbv6e8.apps.googleusercontent.com") // Replace this with your Web Client ID from Firebase Console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
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

        binder.googleButton.setOnClickListener {
            loadingContainer.visibility = View.VISIBLE
            lottieAnimationView.playAnimation()
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                loadingContainer.visibility = View.GONE
                lottieAnimationView.pauseAnimation()
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Save user info to database
                        reference = FirebaseDatabase
                            .getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
                            .reference
                            .child("Users")
                            .child(user.uid)

                        val userMap = HashMap<String, Any>()
                        userMap["uid"] = user.uid
                        userMap["username"] = user.displayName ?: "User"
                        userMap["email"] = user.email ?: ""
                        userMap["profileImage"] = user.photoUrl?.toString() ?: ""
                        userMap["status"] = "online"

                        reference.updateChildren(userMap)
                            .addOnCompleteListener { dbTask ->
                                loadingContainer.visibility = View.GONE
                                lottieAnimationView.pauseAnimation()
                                
                                if (dbTask.isSuccessful) {
                                    startActivity(Intent(this, ChatActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    loadingContainer.visibility = View.GONE
                    lottieAnimationView.pauseAnimation()
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
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