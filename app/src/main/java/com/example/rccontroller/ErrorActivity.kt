package com.example.rccontroller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getDrawable
import kotlinx.android.synthetic.main.activity_error.*

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ErrorPageTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        val attributes = intent.getIntegerArrayListExtra("attributes")

        errorView.text = getString(attributes!![0])
        imageView.setImageDrawable(getDrawable(this, attributes[1]))
        mainLayout.background = getDrawable(this, attributes[2])

        retryButton.setOnClickListener { view ->
            finish()
        }
    }
}