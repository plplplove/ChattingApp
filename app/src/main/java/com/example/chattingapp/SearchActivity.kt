package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chattingapp.adapter.UserAdapter
import com.example.chattingapp.databinding.ActivitySearchBinding
import com.example.chattingapp.model.Chat
import com.example.chattingapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SearchActivity : BaseActivity() {
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
            .getInstance("YOUR_FIREBASE_DATABASE_URL")
            .reference
            .child("Users")

        setupRecyclerView()
        setupSearchInput()
        setupBottomNavigation()
        loadUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(allUsers) { user ->
            Log.d("SearchActivity", "User clicked: ${user.username}")
            createOrOpenChat(user)
        }

        binding.userList.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = userAdapter
        }
    }

    private fun createOrOpenChat(otherUser: User) {
        Log.d("SearchActivity", "Creating/Opening chat with: ${otherUser.username}")
        val currentUserId = auth.currentUser?.uid ?: return
        val chatRef = FirebaseDatabase.getInstance("YOUR_FIREBASE_DATABASE_URL")
            .reference
            .child("Chats")

        chatRef.orderByChild("participants/$currentUserId")
            .equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("SearchActivity", "Chat query result: ${snapshot.childrenCount} chats found")
                    var chatId: String? = null

                    // Look for existing chat with these two users
                    for (chatSnapshot in snapshot.children) {
                        val chat = chatSnapshot.getValue(Chat::class.java)
                        if (chat != null && 
                            chat.participants.containsKey(otherUser.uid) && 
                            chat.participants.size == 2
                        ) {
                            chatId = chatSnapshot.key
                            Log.d("SearchActivity", "Existing chat found with ID: $chatId")
                            break
                        }
                    }

                    if (chatId == null) {
                        // Create new chat
                        chatId = chatRef.push().key ?: return
                        Log.d("SearchActivity", "Creating new chat with ID: $chatId")
                        val participants = mapOf(
                            currentUserId to true,
                            otherUser.uid to true
                        )
                        val newChat = Chat(
                            chatId = chatId,
                            participants = participants
                        )
                        chatRef.child(chatId).setValue(newChat)
                    }

                    Log.d("SearchActivity", "Starting UserChatActivity with chatId: $chatId")
                    val intent = Intent(this@SearchActivity, UserChatActivity::class.java).apply {
                        putExtra("chatId", chatId)
                        putExtra("otherUserId", otherUser.uid)
                        putExtra("otherUserName", otherUser.username)
                        putExtra("otherUserImage", otherUser.profileImageUrl)
                    }
                    startActivity(intent)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SearchActivity", "Error creating chat: ${error.message}")
                    Toast.makeText(
                        this@SearchActivity,
                        "Error creating chat: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
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
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.userHome -> {
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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