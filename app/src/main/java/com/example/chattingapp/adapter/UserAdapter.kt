package com.example.chattingapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chattingapp.R
import com.example.chattingapp.databinding.ItemUserBinding
import com.example.chattingapp.model.User

class UserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.username.text = user.username
            
            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.user_photo)
                .error(R.drawable.user_photo)
                .into(binding.userImage)

            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    fun filterUsers(query: String) {
        val filteredList = users.filter { user ->
            user.username.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
        }
        updateUsers(filteredList)
    }
}
