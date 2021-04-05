package com.example.rccontroller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_error.*

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ErrorPageTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        errorView.text = intent.getStringExtra(getString(R.string.error))
        retryButton.setOnClickListener { view ->
            finish()
        }
    }
}