package com.kuroko.apkforge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuroko.apkforge.databinding.ActivityScriptHubBinding

class ScriptHubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScriptHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScriptHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = ScriptAdapter(ScriptRepository.builtIn) { script ->
            val data = Intent().apply {
                putExtra("script_name", script.name)
                putExtra("script_code", script.code)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
