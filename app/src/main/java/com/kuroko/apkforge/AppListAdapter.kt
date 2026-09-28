package com.kuroko.apkforge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kuroko.apkforge.databinding.ItemAppBinding

class AppListAdapter(
    private val items: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    inner class VH(val b: ItemAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.b.icon.setImageDrawable(app.icon)
        holder.b.name.text = app.appName
        holder.b.pkg.text = app.packageName
        holder.b.size.text = "%.2f MB".format(app.sizeBytes / 1024.0 / 1024.0)
        holder.b.root.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = items.size
}
