package com.example.chattingapp

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.chattingapp.model.User

abstract class BaseActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
        .reference
        .child("Users")

    override fun onResume() {
        super.onResume()
        updateOnlineStatus(true)
    }

    override fun onPause() {
        super.onPause()
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(online: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return

        dbRef.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(User::class.java)
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(online = online)
                    dbRef.child(currentUserId).setValue(updatedUser)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
