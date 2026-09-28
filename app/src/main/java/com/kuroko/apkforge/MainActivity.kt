package com.kuroko.apkforge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kuroko.apkforge.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val robloxWatcher = object : Runnable {
        override fun run() {
            pollRoblox()
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

        binding.btnExecute.setOnClickListener { execute() }

        handler.post(robloxWatcher)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(robloxWatcher)
    }

    private fun refreshPreview() {
        binding.txtScript.text = currentName
        binding.txtPreview.text = if (currentCode.isBlank()) {
            "no script loaded.\n\ntap Edit to write one, or Hub to pull from the script list."
        } else {
            currentCode
        }
    }

    private fun pollRoblox() {
        lifecycleScope.launch {
            val pid = withContext(Dispatchers.IO) {
                try { RootShell.findPid("com.roblox.client") } catch (_: Exception) { -1 }
            }
            if (pid > 0) {
                binding.dotRoblox.setBackgroundColor(getColor(R.color.accent2))
                binding.txtRoblox.text = "roblox running  (pid $pid)"
                binding.txtAttach.text = if (Native.loaded) "native ready" else "native missing"
            } else {
                binding.dotRoblox.setBackgroundColor(getColor(R.color.danger))
                binding.txtRoblox.text = "roblox not detected"
                binding.txtAttach.text = "not attached"
            }
        }
    }

    private fun execute() {
        if (currentCode.isBlank()) {
            log("nothing to execute")
            return
        }
        binding.txtStatus.text = "● running"
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val hasRoot = RootShell.hasRoot()
                if (!hasRoot) return@withContext "no injection path — device not rooted"
                val pid = RootShell.findPid("com.roblox.client")
                if (pid <= 0) return@withContext "roblox is not running"
                if (!Native.loaded) return@withContext "native library missing"
                Native.executeScript(pid, currentCode)
            }
            log("result: $result")
            binding.txtStatus.text = "● idle"
        }
    }

    private fun log(s: String) {
        binding.txtLog.append("\n> $s")
    }
}
