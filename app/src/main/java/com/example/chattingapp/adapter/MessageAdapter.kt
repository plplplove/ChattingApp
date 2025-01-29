package com.example.chattingapp.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chattingapp.R
import com.example.chattingapp.model.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val onMessageLongClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sentMessage: View = itemView.findViewById(R.id.sentMessage)
        private val receivedMessage: View = itemView.findViewById(R.id.receivedMessage)

        // Sent message views
        private val sentMessageText: TextView = itemView.findViewById(R.id.messageText)
        private val sentMessageImage: ImageView = itemView.findViewById(R.id.messageImage)
        private val sentMessageFile: View = itemView.findViewById(R.id.messageFile)
        private val sentFileIcon: ImageView = itemView.findViewById(R.id.fileIcon)
        private val sentFileName: TextView = itemView.findViewById(R.id.fileName)
        private val sentMessageTime: TextView = itemView.findViewById(R.id.messageTime)

        // Received message views
        private val receivedMessageText: TextView = itemView.findViewById(R.id.messageTextReceived)
        private val receivedMessageImage: ImageView = itemView.findViewById(R.id.messageImageReceived)
        private val receivedMessageFile: View = itemView.findViewById(R.id.messageFileReceived)
        private val receivedFileIcon: ImageView = itemView.findViewById(R.id.fileIconReceived)
        private val receivedFileName: TextView = itemView.findViewById(R.id.fileNameReceived)
        private val receivedMessageTime: TextView = itemView.findViewById(R.id.messageTimeReceived)

        fun bind(message: Message) {
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }

            val isCurrentUser = message.senderId == currentUserId

            // Set visibility of message containers
            sentMessage.visibility = if (isCurrentUser) View.VISIBLE else View.GONE
            receivedMessage.visibility = if (isCurrentUser) View.GONE else View.VISIBLE

            // Hide all content views initially
            sentMessageText.visibility = View.GONE
            sentMessageImage.visibility = View.GONE
            sentMessageFile.visibility = View.GONE
            receivedMessageText.visibility = View.GONE
            receivedMessageImage.visibility = View.GONE
            receivedMessageFile.visibility = View.GONE

            when (message.type) {
                "text" -> {
                    if (isCurrentUser) {
                        sentMessageText.visibility = View.VISIBLE
                        sentMessageText.text = message.content
                    } else {
                        receivedMessageText.visibility = View.VISIBLE
                        receivedMessageText.text = message.content
                    }
                }
                "image" -> {
                    if (isCurrentUser) {
                        sentMessageImage.visibility = View.VISIBLE
                        Glide.with(itemView.context)
                            .load(message.fileUrl)
                            .placeholder(R.drawable.image_placeholder)
                            .into(sentMessageImage)
                        
                        sentMessageImage.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(message.fileUrl)
                            itemView.context.startActivity(intent)
                        }
                    } else {
                        receivedMessageImage.visibility = View.VISIBLE
                        Glide.with(itemView.context)
                            .load(message.fileUrl)
                            .placeholder(R.drawable.image_placeholder)
                            .into(receivedMessageImage)
                        
                        receivedMessageImage.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(message.fileUrl)
                            itemView.context.startActivity(intent)
                        }
                    }
                }
                "file" -> {
                    if (isCurrentUser) {
                        sentMessageFile.visibility = View.VISIBLE
                        sentFileIcon.setImageResource(R.drawable.file_icon)
                        sentFileName.text = message.fileName

                        sentMessageFile.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(message.fileUrl)
                            itemView.context.startActivity(intent)
                        }
                    } else {
                        receivedMessageFile.visibility = View.VISIBLE
                        receivedFileIcon.setImageResource(R.drawable.file_icon)
                        receivedFileName.text = message.fileName

                        receivedMessageFile.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(message.fileUrl)
                            itemView.context.startActivity(intent)
                        }
                    }
                }
            }

            // Set message time
            if (isCurrentUser) {
                sentMessageTime.text = formatTime(message.timestamp)
            } else {
                receivedMessageTime.text = formatTime(message.timestamp)
            }
        }
    }
}
