package com.example.chattingapp

import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.chattingapp.databinding.ActivityInformationAboutUserBinding
import com.google.firebase.database.*

class InformationAboutUserActivity : BaseActivity() {
    private lateinit var binding: ActivityInformationAboutUserBinding
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationAboutUserBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val userId = intent.getStringExtra("userId") ?: return finish()
        val username = intent.getStringExtra("username")
        val userImage = intent.getStringExtra("userImage")
        val chatId = intent.getStringExtra("chatId")

        dbRef = FirebaseDatabase
            .getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")
            .child(userId)


        binding.username.text = username
        if (!userImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(userImage)
                .placeholder(R.drawable.user_photo)
                .into(binding.userImage)
        }


        loadUserData()


        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.messageButton.setOnClickListener {
            if (chatId != null) {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }

        binding.userInfo.text = "${username}'s Profile"
    }

    private fun loadUserData() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val email = snapshot.child("email").getValue(String::class.java)
                binding.email.text = email
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@InformationAboutUserActivity,
                    "Error loading user data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
