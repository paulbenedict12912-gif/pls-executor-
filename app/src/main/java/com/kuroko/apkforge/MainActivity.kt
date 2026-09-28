package com.kuroko.apkforge

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kuroko.apkforge.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentName: String = "untitled.lua"
    private var currentCode: String = ""
    private val handler = Handler(Looper.getMainLooper())

    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            currentName = res.data?.getStringExtra("script_name") ?: currentName
            currentCode = res.data?.getStringExtra("script_code") ?: currentCode
            refreshPreview()
        }
    }

    private val hubLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            currentName = res.data?.getStringExtra("script_name") ?: currentName
            currentCode = res.data?.getStringExtra("script_code") ?: currentCode
            refreshPreview()
            log("loaded from hub: $currentName")
        }
    }

    private val watcher = object : Runnable {
        override fun run() {
            pollStatus()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEdit.setOnClickListener {
            val i = Intent(this, EditorActivity::class.java).apply {
                putExtra("script_name", currentName)
                putExtra("script_code", currentCode)
            }
            editorLauncher.launch(i)
        }

        binding.btnHub.setOnClickListener {
            hubLauncher.launch(Intent(this, ScriptHubActivity::class.java))
        }

        binding.btnClear.setOnClickListener {
            currentCode = ""
            currentName = "untitled.lua"
            refreshPreview()
            log("cleared")
        }

        binding.btnForge.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        binding.btnExecute.setOnClickListener { runScript() }

        handler.post(watcher)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(watcher)
    }

    private fun refreshPreview() {
        binding.txtScript.text = currentName
        binding.txtPreview.text = if (currentCode.isBlank()) {
            "no script loaded.\n\ntap Edit to write one, or Hub to pull from the script list."
        } else {
            currentCode
        }
    }

    private fun pollStatus() {
        val installed = try {
            packageManager.getPackageInfo("com.roblox.client", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (installed) {
            binding.dotRoblox.setBackgroundColor(getColor(R.color.accent2))
            binding.txtRoblox.text = "roblox installed on device"
            binding.txtAttach.text = "shell mode"
        } else {
            binding.dotRoblox.setBackgroundColor(getColor(R.color.danger))
            binding.txtRoblox.text = "roblox not found"
            binding.txtAttach.text = "shell mode"
        }
    }

    private fun runScript() {
        if (currentCode.isBlank()) {
            log("nothing to run — write or load a script first")
            return
        }
        val f = java.io.File(ApkUtils.outputDir(), currentName)
        try {
            f.writeText(currentCode)
            log("saved to ${f.absolutePath}")
            log("shell mode — script export only, no in-game execution")
        } catch (e: Exception) {
            log("save failed: ${e.message}")
        }
    }

    private fun log(s: String) {
        binding.txtLog.append("\n> $s")
    }
}
