package com.example.chattingapp

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chattingapp.databinding.ActivityMainBinding
import com.example.chattingapp.databinding.ActivityRegisterBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binder: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()
        supportActionBar?.hide()

        binder = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binder.root)

        binder.signUpButton.setOnClickListener{
            intent = Intent(this, Register::class.java)
            startActivity(intent)
        }




        }
    }