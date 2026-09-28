package com.kuroko.apkforge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kuroko.apkforge.databinding.ItemScriptBinding

class ScriptAdapter(
    private val items: List<Script>,
    private val onClick: (Script) -> Unit
) : RecyclerView.Adapter<ScriptAdapter.VH>() {

    inner class VH(val b: ItemScriptBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemScriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.b.txtName.text = s.name
        holder.b.txtDesc.text = s.description
        holder.b.txtAuthor.text = "by ${s.author}"
        holder.b.root.setOnClickListener { onClick(s) }
    }

    override fun getItemCount() = items.size
}
