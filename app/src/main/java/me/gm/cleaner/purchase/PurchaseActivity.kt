package me.gm.cleaner.purchase

import android.os.Bundle
import androidx.fragment.app.commit
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseActivity
import me.gm.cleaner.databinding.PurchaseActivityBinding

class PurchaseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState ?: supportFragmentManager.commit {
            replace(R.id.purchase, PurchaseFragment())
        }
        val binding = PurchaseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
