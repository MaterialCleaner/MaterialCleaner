package me.gm.cleaner.about

import android.os.Bundle
import androidx.fragment.app.commit
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseActivity
import me.gm.cleaner.databinding.AboutActivityBinding

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState ?: supportFragmentManager.commit {
            replace(R.id.about, AboutFragment())
        }
        val binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
