package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {
    private lateinit var binder: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()

        binder = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binder.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binder.signInButton.setOnClickListener {
            val signInIntent = Intent(this, MainActivity::class.java)
            startActivity(signInIntent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binder.signUpButton.setOnClickListener {
            val userName = binder.usernameInput.text.toString()
            val email = binder.emailInput.text.toString()
            val password = binder.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty() || userName.isEmpty()) {
                Toast.makeText(applicationContext, "Fields cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { authTask ->
                        if(authTask.isSuccessful) {
                            val databasereRef = database.reference.child("Users")
                                .child(firebaseAuth.currentUser!!.uid)
                            val users:Users = Users(userName, email, password, firebaseAuth.currentUser!!.uid)

                            databasereRef.setValue(users).addOnCompleteListener{
                                if (it.isSuccessful){
                                    intent = Intent(this, MainPage::class.java)
                                    startActivity(intent)
                                }
                                else{
                                    Toast.makeText(this, "Something went wrong, try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else{
                            Toast.makeText(this, "Something went wrong, try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            }

        }
    }