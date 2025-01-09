package com.example.chattingapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityFillProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FillProfile : AppCompatActivity() {
    private lateinit var binder: ActivityFillProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = ActivityFillProfileBinding.inflate(layoutInflater)
        setContentView(binder.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binder.fillProfileFinishButton.setOnClickListener {
            saveProfileData()
        }
    }

    private fun saveProfileData() {
        val firstName = binder.firstNameInput.text.toString().trim()
        val lastName = binder.lastNameInput.text.toString().trim() // опційне
        val phone = binder.phoneInput.text.toString().trim()

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val userRef = database.reference.child("users").child(userId)
            // Формуємо карту оновлень
            val updates = mutableMapOf<String, Any>(
                "firstName" to firstName,
                "phone" to phone
            )
            // Додатково додаємо прізвище, якщо введене
            if (lastName.isNotEmpty()) {
                updates["lastName"] = lastName
            }

            userRef.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    // Перехід на наступний екран чи завершення реєстрації
                    // Наприклад: startActivity(Intent(this, MainPage::class.java))
                    // finish()
                } else {
                    Toast.makeText(this, task.exception?.message ?: "Profile update failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
        }
    }
}