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
import com.example.chattingapp.model.Message
import com.example.chattingapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : BaseActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var messagesRef: DatabaseReference
    private var chatPreviews = mutableListOf<ChatPreview>()
    private val messageListeners = mutableMapOf<String, ValueEventListener>()
    private val userListeners = mutableMapOf<String, ValueEventListener>()

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
        messagesRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Messages")

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

        // Remove existing message listeners
        messageListeners.forEach { (chatId, listener) ->
            messagesRef.child(chatId).removeEventListener(listener)
        }
        messageListeners.clear()

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

                        // Create message listener for this chat
                        val messageListener = object : ValueEventListener {
                            override fun onDataChange(messagesSnapshot: DataSnapshot) {
                                var unreadCount = 0
                                for (messageSnapshot in messagesSnapshot.children) {
                                    val message = messageSnapshot.getValue(Message::class.java)
                                    if (message != null && message.senderId != currentUserId && !message.seen) {
                                        unreadCount++
                                    }
                                }

                                // Update unread count in existing chat preview
                                val existingChatIndex = chatPreviews.indexOfFirst { it.chatId == chatId }
                                if (existingChatIndex != -1) {
                                    val existingChat = chatPreviews[existingChatIndex]
                                    chatPreviews[existingChatIndex] = existingChat.copy(unreadCount = unreadCount)
                                    chatAdapter.updateChats(chatPreviews)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("ChatActivity", "Error listening to messages: ${error.message}")
                            }
                        }

                        // Store and attach the listener
                        messageListeners[chatId] = messageListener
                        messagesRef.child(chatId).addValueEventListener(messageListener)

                        // Get other user's info
                        val userListener = object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val user = userSnapshot.getValue(User::class.java)
                                Log.d("ChatActivity", "User ${user?.username} data updated: online=${user?.online}")
                                if (user != null) {
                                    // Update chat preview with new user data
                                    val existingIndex = chatPreviews.indexOfFirst { it.chatId == chatId }
                                    if (existingIndex != -1) {
                                        val existingChat = chatPreviews[existingIndex]
                                        chatPreviews[existingIndex] = existingChat.copy(
                                            otherUserName = user.username,
                                            otherUserImage = user.profileImageUrl,
                                            otherUserOnline = user.online
                                        )
                                        Log.d("ChatActivity", "Updated chat preview for ${user.username}: online=${user.online}")
                                        chatAdapter.updateChats(chatPreviews)
                                    } else {
                                        // Create new chat preview
                                        messagesRef.child(chatId).get().addOnSuccessListener { messagesSnapshot ->
                                            var unreadCount = 0
                                            for (messageSnapshot in messagesSnapshot.children) {
                                                val message = messageSnapshot.getValue(Message::class.java)
                                                if (message != null && message.senderId != currentUserId && !message.seen) {
                                                    unreadCount++
                                                }
                                            }

                                            val chatPreview = ChatPreview(
                                                chatId = chatId,
                                                otherUserId = otherUserId,
                                                otherUserName = user.username,
                                                otherUserImage = user.profileImageUrl,
                                                lastMessage = chat.lastMessage,
                                                lastMessageTime = chat.lastMessageTime,
                                                lastMessageSenderId = chat.lastMessageSenderId,
                                                unreadCount = unreadCount,
                                                otherUserOnline = user.online
                                            )
                                            Log.d("ChatActivity", "Created new chat preview for ${user.username}: online=${user.online}")

                                            chatPreviews.add(chatPreview)
                                            loadedChats++
                                            
                                            if (loadedChats >= totalChats) {
                                                // Sort chats by last message time
                                                chatPreviews.sortByDescending { it.lastMessageTime }
                                                chatAdapter.updateChats(chatPreviews)
                                                binding.progressBar.visibility = View.GONE
                                            }
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("ChatActivity", "Error loading user: ${error.message}")
                                loadedChats++
                                if (loadedChats >= totalChats) {
                                    binding.progressBar.visibility = View.GONE
                                }
                            }
                        }

                        // Store and attach the user listener
                        userListeners[otherUserId] = userListener
                        usersRef.child(otherUserId).addValueEventListener(userListener)
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

    override fun onDestroy() {
        super.onDestroy()
        
        // Remove all message listeners
        messageListeners.forEach { (chatId, listener) ->
            messagesRef.child(chatId).removeEventListener(listener)
        }
        messageListeners.clear()

        // Remove all user listeners
        userListeners.forEach { (userId, listener) ->
            usersRef.child(userId).removeEventListener(listener)
        }
        userListeners.clear()
    }
}