package com.kuroko.apkforge

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuroko.apkforge.databinding.ActivityAppListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                ApkUtils.listInstalledApps(this@AppListActivity, includeSystem = false)
            }
            binding.recycler.adapter = AppListAdapter(apps) { pickAction(it) }
        }
    }

    private fun pickAction(app: AppInfo) {
        val options = arrayOf("Copy APK", "Copy & Explore", "Copy & Sign")
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyOnly(app)
                    1 -> copyAndExplore(app)
                    2 -> copyAndSign(app)
                }
            }
            .show()
    }

    private fun copyOnly(app: AppInfo) = doCopy(app) { file ->
        Toast.makeText(this, "Saved: ${file.name}", Toast.LENGTH_LONG).show()
    }

    private fun copyAndExplore(app: AppInfo) = doCopy(app) { file ->
        val i = Intent(this, ApkExploreActivity::class.java)
        i.putExtra("apk_path", file.absolutePath)
        startActivity(i)
    }

    private fun copyAndSign(app: AppInfo) = doCopy(app) { file ->
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                val signed = java.io.File(file.parentFile, file.nameWithoutExtension + "_signed.apk")
                val ok = ApkSigner.sign(file, signed)
                if (ok) signed else null
            }
            if (out != null) Toast.makeText(this@AppListActivity,
                "Signed: ${out.name}", Toast.LENGTH_LONG).show()
            else Toast.makeText(this@AppListActivity,
                "Sign failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun doCopy(app: AppInfo, onDone: (java.io.File) -> Unit) {
        val input = EditText(this)
        input.hint = "custom name (optional)"
        AlertDialog.Builder(this)
            .setTitle("Copy ${app.appName}")
            .setView(input)
            .setPositiveButton("Copy") { _, _ ->
                lifecycleScope.launch {
                    val file = withContext(Dispatchers.IO) {
                        ApkUtils.copyApk(app, input.text.toString())
                    }
                    if (file != null) onDone(file)
                    else Toast.makeText(this@AppListActivity,
                        "Copy failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
