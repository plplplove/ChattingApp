package com.example.chattingapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityMainPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainPage : AppCompatActivity() {

    private lateinit var binder: ActivityMainPageBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binder.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            database.reference.child("users").child(userId).get()
                .addOnSuccessListener { snapshot ->
                    val name = snapshot.child("name").value?.toString() ?: "N/A"
                    val surname = snapshot.child("surname").value?.toString() ?: "N/A"
                    val email = snapshot.child("email").value?.toString() ?: "N/A"
                    val phone = snapshot.child("phone").value?.toString() ?: "N/A"

                    binder.nameTextView.text = "Name: $name"
                    binder.surnameTextView.text = "Surname: $surname"
                    binder.emailTextView.text = "Email: $email"
                    binder.phoneTextView.text = "Phone: $phone"
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
        }
    }
}