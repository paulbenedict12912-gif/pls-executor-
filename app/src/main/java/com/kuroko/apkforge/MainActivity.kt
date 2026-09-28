package com.kuroko.apkforge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kuroko.apkforge.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPick.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
        binding.btnExplore.setOnClickListener {
            val i = Intent(this, ApkExploreActivity::class.java)
            val dir = ApkUtils.outputDir()
            val last = dir.listFiles { f -> f.extension == "apk" }
                ?.maxByOrNull { it.lastModified() }
            if (last != null) {
                i.putExtra("apk_path", last.absolutePath)
                startActivity(i)
            }
        }
        binding.btnOut.text = "Output: ${ApkUtils.outputDir().absolutePath}"

        requestPerms()
    }

    private fun requestPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), 100
                )
            }
        }
    }
}
