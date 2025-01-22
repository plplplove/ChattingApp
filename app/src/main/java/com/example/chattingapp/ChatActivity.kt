package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {
    private lateinit var binder: ActivityChatBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binder = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binder.root)

        binder.bottomNavigationMenu.selectedItemId = R.id.messages
        binder.bottomNavigationMenu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    finish()
                    true
                }
                R.id.messages -> {
                    true
                }
                R.id.userHome -> {
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}