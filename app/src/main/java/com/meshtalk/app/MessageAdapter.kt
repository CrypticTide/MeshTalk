package com.meshtalk.app

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meshtalk.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messages: MutableList<MeshMessage>) : RecyclerView.Adapter<MessageAdapter.Holder>() {
    private var myName = ""
    fun setMyName(name: String) { myName = name }
    fun add(message: MeshMessage) { messages.add(message); notifyItemInserted(messages.lastIndex) }

    class Holder(val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = messages.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val m = messages[position]
        val mine = m.senderName == myName
        holder.b.senderText.text = "${m.senderName} • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.timestamp))}"
        holder.b.bodyText.text = m.body
        holder.b.bodyText.setTextColor(if (mine) Color.WHITE else Color.rgb(21,34,56))
        holder.b.bodyText.setBackgroundResource(if (mine) R.drawable.bg_message_me else R.drawable.bg_message_other)
        holder.b.messageRoot.gravity = if (mine) Gravity.END else Gravity.START
    }
}
