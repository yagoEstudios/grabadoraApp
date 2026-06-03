package com.yago.grabadora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AudioAdapter(
    private var items: List<AudioItem>,
    private val onClick: (AudioItem) -> Unit,
    private val onDelete: (AudioItem) -> Unit
) : RecyclerView.Adapter<AudioAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.itemName)
        val meta: TextView = v.findViewById(R.id.itemMeta)
        val delete: ImageButton = v.findViewById(R.id.itemDelete)
    }

    fun submit(list: List<AudioItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.name.text = d.name
        val kb = d.size / 1024
        val type = if (d.name.endsWith(".wav")) "WAV" else "AAC"
        holder.meta.text = "$type · $kb KB"
        holder.itemView.setOnClickListener { onClick(d) }
        holder.delete.setOnClickListener { onDelete(d) }
    }

    override fun getItemCount() = items.size
}
