package com.example.chattingapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chattingapp.R
import com.example.chattingapp.databinding.ItemChatBinding
import com.example.chattingapp.model.ChatPreview
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var chats: List<ChatPreview>,
    private val onChatClick: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatPreview) {
            binding.userName.text = chat.otherUserName
            binding.lastMessage.text = chat.lastMessage
            binding.messageTime.text = formatTime(chat.lastMessageTime)

            Glide.with(itemView.context)
                .load(chat.otherUserImage)
                .placeholder(R.drawable.user_photo)
                .error(R.drawable.user_photo)
                .into(binding.profileImage)

            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount() = chats.size

    fun updateChats(newChats: List<ChatPreview>) {
        chats = newChats
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return when {
            isSameDay(now, messageTime) -> {
                // Today - show time
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isYesterday(now, messageTime) -> {
                // Yesterday
                "Yesterday"
            }
            else -> {
                // Other days - show date
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, time: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, time)
    }
}
