package com.example.chattingapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chattingapp.adapter.ChatAdapter
import com.example.chattingapp.databinding.ActivityChatBinding
import com.example.chattingapp.model.Chat
import com.example.chattingapp.model.ChatPreview
import com.example.chattingapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private var chatPreviews = mutableListOf<ChatPreview>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Chats")
        usersRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")

        setupRecyclerView()
        setupBottomNavigation()
        loadChats()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatPreviews) { chatPreview ->
            val intent = Intent(this, UserChatActivity::class.java).apply {
                putExtra("chatId", chatPreview.chatId)
                putExtra("otherUserId", chatPreview.otherUserId)
                putExtra("otherUserName", chatPreview.otherUserName)
                putExtra("otherUserImage", chatPreview.otherUserImage)
            }
            startActivity(intent)
        }

        binding.chatList.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = chatAdapter
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationMenu.selectedItemId = R.id.messages
        binding.bottomNavigationMenu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.messages -> true
                R.id.search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
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

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.noChatsText.visibility = View.GONE

        dbRef.orderByChild("participants/$currentUserId")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatPreviews.clear()
                    var loadedChats = 0
                    val totalChats = snapshot.childrenCount.toInt()

                    if (totalChats == 0) {
                        binding.progressBar.visibility = View.GONE
                        binding.noChatsText.visibility = View.VISIBLE
                        chatAdapter.updateChats(chatPreviews)
                        return
                    }

                    for (chatSnapshot in snapshot.children) {
                        val chat = chatSnapshot.getValue(Chat::class.java) ?: continue
                        val chatId = chatSnapshot.key ?: continue

                        // Find the other user's ID
                        val otherUserId = chat.participants.keys.find { it != currentUserId } ?: continue

                        // Get other user's info
                        usersRef.child(otherUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (user != null) {
                                    val chatPreview = ChatPreview(
                                        chatId = chatId,
                                        otherUserId = otherUserId,
                                        otherUserName = user.username,
                                        otherUserImage = user.profileImageUrl,
                                        lastMessage = chat.lastMessage,
                                        lastMessageTime = chat.lastMessageTime,
                                        lastMessageSenderId = chat.lastMessageSenderId
                                    )
                                    chatPreviews.add(chatPreview)
                                }

                                loadedChats++
                                if (loadedChats >= totalChats) {
                                    // Sort chats by last message time
                                    chatPreviews.sortByDescending { it.lastMessageTime }
                                    chatAdapter.updateChats(chatPreviews)
                                    binding.progressBar.visibility = View.GONE
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("ChatActivity", "Error loading user: ${error.message}")
                                loadedChats++
                                if (loadedChats >= totalChats) {
                                    binding.progressBar.visibility = View.GONE
                                }
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatActivity", "Error loading chats: ${error.message}")
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ChatActivity,
                        "Error loading chats: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}