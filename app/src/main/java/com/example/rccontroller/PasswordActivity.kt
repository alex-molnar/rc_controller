package com.example.rccontroller

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_password.*
import kotlin.concurrent.thread

class PasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        thread {
            sendButton.setOnClickListener(onSendClicked)
        }
    }

    private val onSendClicked = View.OnClickListener { view ->
        when {
            oldPasswordInput.text.toString() == "" -> {
                Snackbar.make(view, getString(R.string.password_empty), Snackbar.LENGTH_LONG).show()
            }
            newPasswordInputFirst.text.toString() != newPasswordInputSecond.text.toString() -> {
                Snackbar.make(view, getString(R.string.password_not_match), Snackbar.LENGTH_LONG)
                    .show()
            }
            else -> {
                thread {
                    Channel.setPasswordChangeRequest(
                        nameInput.text.toString(),
                        oldPasswordInput.text.toString(),
                        newPasswordInputFirst.text.toString()
                    ) { success ->
                        if (success) {
                            runOnUiThread {
                                AlertDialog.Builder(this)
                                    .setIcon(R.drawable.ic_hotroad)
                                    .setNeutralButton("OK") { _: DialogInterface, _: Int -> finish() }
                                    .setTitle("SUCCESS")
                                    .setMessage(getString(R.string.password_success))
                                    .show()
                            }
                        } else {
                            Snackbar.make(
                                view,
                                getString(R.string.password_incorrect),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}