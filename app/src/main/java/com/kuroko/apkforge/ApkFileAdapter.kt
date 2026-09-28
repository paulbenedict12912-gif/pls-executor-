package com.kuroko.apkforge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kuroko.apkforge.databinding.ItemFileBinding
import java.util.zip.ZipEntry

class ApkFileAdapter(
    private val items: List<ZipEntry>,
    private val onClick: (ZipEntry) -> Unit
) : RecyclerView.Adapter<ApkFileAdapter.VH>() {

    inner class VH(val b: ItemFileBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.b.fileName.text = e.name
        holder.b.fileSize.text = if (e.isDirectory) "dir"
            else "%.2f KB".format(e.size / 1024.0)
        holder.b.root.setOnClickListener { onClick(e) }
    }

    override fun getItemCount() = items.size
}
