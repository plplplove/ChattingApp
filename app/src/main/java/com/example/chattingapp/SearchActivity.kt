package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chattingapp.adapter.UserAdapter
import com.example.chattingapp.databinding.ActivitySearchBinding
import com.example.chattingapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var userAdapter: UserAdapter
    private var allUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase
            .getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")

        setupRecyclerView()
        setupSearchInput()
        setupBottomNavigation()
        loadUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(allUsers) { user ->
            Toast.makeText(this, "Clicked on ${user.username}", Toast.LENGTH_SHORT).show()
        }

        binding.userList.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = userAdapter
        }
    }

    private fun setupSearchInput() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterUsers(s.toString())
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationMenu.selectedItemId = R.id.search
        binding.bottomNavigationMenu.setOnItemSelectedListener { item ->
            when (item.itemId) {
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
                R.id.search -> true
                else -> false
            }
        }
    }

    private fun loadUsers() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && user.uid != auth.currentUser?.uid) {
                        allUsers.add(user)
                    }
                }
                userAdapter.updateUsers(allUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@SearchActivity,
                    "Error loading users: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun filterUsers(query: String) {
        if (query.isEmpty()) {
            userAdapter.updateUsers(allUsers)
        } else {
            val filteredUsers = allUsers.filter { user ->
                user.username.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            userAdapter.updateUsers(filteredUsers)
        }
    }
}