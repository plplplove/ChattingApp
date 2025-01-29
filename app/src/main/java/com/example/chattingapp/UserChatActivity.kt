package com.example.chattingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chattingapp.adapter.MessageAdapter
import com.example.chattingapp.databinding.ActivityUserChatBinding
import com.example.chattingapp.model.Message
import com.example.chattingapp.model.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UserChatActivity : BaseActivity() {
    private lateinit var binding: ActivityUserChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var otherUserId: String
    private var messages = mutableListOf<Message>()
    private var isActive = false
    private var userListener: ValueEventListener? = null
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val PICK_IMAGE_REQUEST = 1
    private val PICK_FILE_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        chatId = intent.getStringExtra("chatId") ?: return finish()
        otherUserId = intent.getStringExtra("otherUserId") ?: return finish()
        val otherUserName = intent.getStringExtra("otherUserName")
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

        // Add click listeners for user info
        binding.profileImage.setOnClickListener {
            openUserInfo(otherUserId, otherUserName, otherUserImage)
        }
        
        binding.userName.setOnClickListener {
            openUserInfo(otherUserId, otherUserName, otherUserImage)
        }

        dbRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Messages")
            .child(chatId)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setupUI()
        setupRecyclerView()
        setupMessageSending()
        loadMessages()
        loadUserData()
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
        messageAdapter = MessageAdapter(messages, auth.currentUser?.uid ?: "") { message ->
            showDeleteMessageDialog(message)
        }

        binding.messageList.apply {
            layoutManager = LinearLayoutManager(this@UserChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        binding.messageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.messageList.postDelayed({
                    binding.messageList.smoothScrollToPosition(messages.size)
                }, 300)
            }
        }
    }

    private fun setupMessageSending() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendTextMessage(messageText)
                binding.messageInput.text?.clear()
            }
        }

        binding.attachButton.setOnClickListener {
            showAttachmentOptions()
        }
    }

    private fun showAttachmentOptions() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_attachments, null)

        view.findViewById<View>(R.id.imageAttachmentLayout).setOnClickListener {
            bottomSheetDialog.dismiss()
            pickImage()
        }

        view.findViewById<View>(R.id.fileAttachmentLayout).setOnClickListener {
            bottomSheetDialog.dismiss()
            pickFile()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null && data.data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> uploadImage(data.data!!)
                PICK_FILE_REQUEST -> uploadFile(data.data!!)
            }
        }
    }

    private fun uploadImage(imageUri: Uri) {
        showProgressDialog("Uploading image...")
        val imageRef = storageRef.child("chat_images/${System.currentTimeMillis()}_${auth.currentUser?.uid}")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    sendImageMessage(uri.toString())
                    hideProgressDialog()
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadFile(fileUri: Uri) {
        val fileName = getFileName(fileUri)
        showProgressDialog("Uploading file...")
        val fileRef = storageRef.child("chat_files/${System.currentTimeMillis()}_${auth.currentUser?.uid}_$fileName")

        fileRef.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    sendFileMessage(uri.toString(), fileName)
                    hideProgressDialog()
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Failed to upload file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown_file"
    }

    private fun sendTextMessage(content: String) {
        val messageId = dbRef.push().key ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            content = content,
            timestamp = timestamp,
            type = "text"
        )

        sendMessage(message, content)
    }

    private fun sendImageMessage(imageUrl: String) {
        val messageId = dbRef.push().key ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            content = "📷 Image",
            timestamp = timestamp,
            type = "image",
            fileUrl = imageUrl
        )

        sendMessage(message, "📷 Image")
    }

    private fun sendFileMessage(fileUrl: String, fileName: String) {
        val messageId = dbRef.push().key ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            content = "📎 $fileName",
            timestamp = timestamp,
            type = "file",
            fileName = fileName,
            fileUrl = fileUrl
        )

        sendMessage(message, "📎 $fileName")
    }

    private fun sendMessage(message: Message, displayContent: String) {
        Log.d("UserChatActivity", "Sending message: $displayContent")

        dbRef.child(message.messageId).setValue(message)
            .addOnSuccessListener {
                Log.d("UserChatActivity", "Message sent successfully")
                // Update last message in chat
                val chatRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
                    .reference
                    .child("Chats")
                    .child(chatId)

                val updates = hashMapOf<String, Any>(
                    "lastMessage" to displayContent,
                    "lastMessageTime" to message.timestamp,
                    "lastMessageSenderId" to message.senderId
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

    private fun showProgressDialog(message: String) {
        // Show a progress dialog
        // You can implement this using your preferred progress indicator
    }

    private fun hideProgressDialog() {
        // Hide the progress dialog
        // You can implement this using your preferred progress indicator
    }

    private fun markMessagesAsRead() {
        val currentUserId = auth.currentUser?.uid ?: return
        val updates = mutableMapOf<String, Any>()

        for (message in messages) {
            if (message.senderId != currentUserId && !message.seen) {
                updates["${message.messageId}/seen"] = true
            }
        }

        if (updates.isNotEmpty()) {
            dbRef.updateChildren(updates)
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

                if (isActive) {
                    markMessagesAsRead()
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

    private fun loadUserData() {
        val usersRef = FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("Users")

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.userName.text = it.username

                    binding.onlineStatusIndicator.setImageResource(
                        if (it.online) R.drawable.online_status_indicator
                        else R.drawable.offline_status_indicator
                    )

                    Glide.with(this@UserChatActivity)
                        .load(it.profileImageUrl)
                        .placeholder(R.drawable.user_photo)
                        .error(R.drawable.user_photo)
                        .into(binding.profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserChatActivity", "Error loading user data: ${error.message}")
            }
        }

        usersRef.child(otherUserId).addValueEventListener(userListener!!)
    }

    private fun openUserInfo(userId: String, username: String?, userImage: String?) {
        val intent = Intent(this, InformationAboutUserActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("username", username)
            putExtra("userImage", userImage)
            putExtra("chatId", chatId)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showDeleteMessageDialog(message: Message) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_message)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val yesButton = dialog.findViewById<Button>(R.id.yesButton)
        val noButton = dialog.findViewById<Button>(R.id.noButton)

        yesButton.setOnClickListener {
            deleteMessage(message)
            dialog.dismiss()
        }

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteMessage(message: Message) {
        dbRef.child(message.messageId ?: return)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        markMessagesAsRead()
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    override fun onDestroy() {
        super.onDestroy()

        userListener?.let { listener ->
            FirebaseDatabase.getInstance("https://chattingapp-d6b91-default-rtdb.europe-west1.firebasedatabase.app/")
                .reference
                .child("Users")
                .child(otherUserId)
                .removeEventListener(listener)
        }
    }
}