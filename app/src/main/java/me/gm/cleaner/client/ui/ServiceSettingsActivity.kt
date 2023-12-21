package me.gm.cleaner.client.ui

import android.os.Bundle
import me.gm.cleaner.app.BaseActivity
import me.gm.cleaner.databinding.ServiceSettingsActivityBinding

class ServiceSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ServiceSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
