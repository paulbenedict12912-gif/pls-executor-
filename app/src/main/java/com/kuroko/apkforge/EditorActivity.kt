package com.kuroko.apkforge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kuroko.apkforge.databinding.ActivityEditorBinding
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var scriptName: String = "untitled.lua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scriptName = intent.getStringExtra("script_name") ?: "untitled.lua"
        val code = intent.getStringExtra("script_code").orEmpty()
        binding.txtTitle.text = scriptName
        binding.editScript.setText(code)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val f = File(ApkUtils.outputDir(), scriptName)
            f.writeText(binding.editScript.text.toString())
            Toast.makeText(this, "saved ${f.name}", Toast.LENGTH_SHORT).show()
        }

        binding.btnRun.setOnClickListener {
            val data = Intent().apply {
                putExtra("script_name", scriptName)
                putExtra("script_code", binding.editScript.text.toString())
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
