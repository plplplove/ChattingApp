package com.example.chattingapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chattingapp.adapter.MessageAdapter
import com.example.chattingapp.databinding.ActivityUserChatBinding
import com.example.chattingapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var otherUserId: String
    private var messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        chatId = intent.getStringExtra("chatId") ?: return finish()
        otherUserId = intent.getStringExtra("otherUserId") ?: return finish()
        
        dbRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Messages")
            .child(chatId)

        setupUI()
        setupRecyclerView()
        setupMessageSending()
        loadMessages()
    }

    private fun setupUI() {
        val otherUserName = intent.getStringExtra("otherUserName") ?: "User"
        val otherUserImage = intent.getStringExtra("otherUserImage")

        binding.userName.text = otherUserName
        
        Glide.with(this)
            .load(otherUserImage)
            .placeholder(R.drawable.user_photo)
            .error(R.drawable.user_photo)
            .into(binding.profileImage)

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages, auth.currentUser?.uid ?: "")
        
        binding.messageList.apply {
            layoutManager = LinearLayoutManager(this@UserChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupMessageSending() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.messageInput.text?.clear()
            }
        }
    }

    private fun sendMessage(content: String) {
        val messageId = dbRef.push().key ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            content = content,
            timestamp = timestamp
        )

        Log.d("UserChatActivity", "Sending message: $content")
        
        dbRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d("UserChatActivity", "Message sent successfully")
                // Update last message in chat
                val chatRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
                    .reference
                    .child("Chats")
                    .child(chatId)

                val updates = hashMapOf<String, Any>(
                    "lastMessage" to content,
                    "lastMessageTime" to timestamp,
                    "lastMessageSenderId" to currentUserId
                )

                chatRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("UserChatActivity", "Chat last message updated")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserChatActivity", "Error updating chat: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserChatActivity", "Error sending message: ${e.message}")
                Toast.makeText(
                    this,
                    "Failed to send message: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadMessages() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }
                messageAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    binding.messageList.smoothScrollToPosition(messages.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserChatActivity", "Error loading messages: ${error.message}")
                Toast.makeText(
                    this@UserChatActivity,
                    "Error loading messages: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}