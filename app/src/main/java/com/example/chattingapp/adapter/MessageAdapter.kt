package com.example.chattingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chattingapp.databinding.ItemReceivedMessageBinding
import com.example.chattingapp.databinding.ItemSentMessageBinding
import com.example.chattingapp.model.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    inner class SentMessageViewHolder(private val binding: ItemSentMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageText.text = message.content
            binding.messageTime.text = formatTime(message.timestamp)
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemReceivedMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageText.text = message.content
            binding.messageTime.text = formatTime(message.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemSentMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemReceivedMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder.itemViewType == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).bind(message)
        } else {
            (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
