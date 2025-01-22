package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chattingapp.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    private lateinit var binder: ActivitySearchBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binder = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binder.root)

        binder.bottomNavigationMenu.selectedItemId = R.id.search
        binder.bottomNavigationMenu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.search -> {
                    true
                }
                R.id.messages -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
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