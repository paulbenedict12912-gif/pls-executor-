package com.kuroko.apkforge

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuroko.apkforge.databinding.ActivityApkExploreBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ApkExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApkExploreBinding
    private lateinit var apk: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApkExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra("apk_path")
        if (path.isNullOrEmpty()) { finish(); return }
        apk = File(path)
        if (!apk.exists()) { finish(); return }

        binding.title.text = apk.name
        binding.recycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) { ApkUtils.listEntries(apk) }
            binding.recycler.adapter = ApkFileAdapter(entries) { showEntry(it.name) }
            binding.status.text = "${entries.size} entries"
        }
    }

    private fun showEntry(name: String) {
        val options = arrayOf("View", "Extract", "Extract all")
        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewEntry(name)
                    1 -> extractOne(name)
                    2 -> extractAll()
                }
            }
            .show()
    }

    private fun viewEntry(name: String) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                val bytes = ApkUtils.readEntryBytes(apk, name) ?: return@withContext null
                if (name == "AndroidManifest.xml") {
                    try { AxmlParser.decode(bytes) }
                    catch (_: Exception) { "AXML decode failed\n\n" + hexPreview(bytes) }
                } else if (ApkUtils.isTextLike(name)) {
                    String(bytes, Charsets.UTF_8)
                } else {
                    hexPreview(bytes)
                }
            }
            if (text == null) {
                Toast.makeText(this@ApkExploreActivity,
                    "Too large or unreadable", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val scroll = android.widget.ScrollView(this@ApkExploreActivity)
            val tv = android.widget.TextView(this@ApkExploreActivity)
            tv.text = text
            tv.setTextIsSelectable(true)
            tv.setPadding(32, 32, 32, 32)
            scroll.addView(tv)
            AlertDialog.Builder(this@ApkExploreActivity)
                .setTitle(name.substringAfterLast('/'))
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun extractOne(name: String) {
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                ApkUtils.extractEntry(apk, name, ApkUtils.outputDir())
            }
            if (out != null) Toast.makeText(this@ApkExploreActivity,
                "Extracted: ${out.name}", Toast.LENGTH_LONG).show()
        }
    }

    private fun extractAll() {
        lifecycleScope.launch {
            val dir = withContext(Dispatchers.IO) {
                val d = File(ApkUtils.outputDir(),
                    apk.nameWithoutExtension + "_extracted")
                d.mkdirs()
                val entries = ApkUtils.listEntries(apk)
                for (e in entries) {
                    if (e.isDirectory) continue
                    ApkUtils.extractEntry(apk, e.name, d)
                }
                d
            }
            Toast.makeText(this@ApkExploreActivity,
                "Extracted to ${dir.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hexPreview(bytes: ByteArray, limit: Int = 4096): String {
        val sb = StringBuilder()
        val n = minOf(bytes.size, limit)
        var i = 0
        while (i < n) {
            sb.append("%08X  ".format(i))
            val end = minOf(i + 16, n)
            for (j in i until end) sb.append("%02X ".format(bytes[j]))
            for (j in end until i + 16) sb.append("   ")
            sb.append(" ")
            for (j in i until end) {
                val c = bytes[j].toInt() and 0xFF
                sb.append(if (c in 32..126) c.toChar() else '.')
            }
            sb.append("\n")
            i += 16
        }
        if (bytes.size > limit) sb.append("\n... (${bytes.size - limit} more bytes)")
        return sb.toString()
    }
}
