package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivityUserProfileBinding

class UserProfileActivity : AppCompatActivity() {
    private lateinit var binder: ActivityUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binder = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binder.root)

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
    }
}