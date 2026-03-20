package com.example.tethervpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var logs: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var modeSelector: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logs = findViewById(R.id.logs)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        modeSelector = findViewById(R.id.modeSelector)

        startButton.setOnClickListener {
            prepareAndStartVpn()
        }

        stopButton.setOnClickListener {
            stopVpnService()
        }
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Ask user to allow VPN
            startActivityForResult(intent, 0)
        } else {
            // Already allowed
            startVpnService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVpnService() {
        val mode = when (modeSelector.checkedRadioButtonId) {
            R.id.radioTor -> 1
            R.id.radioDirect -> 2
            R.id.radioProxy -> 3
            else -> 1
        }
        logs.append("\n[Tether] Starting VPN in mode $mode…")
        val intent = Intent(this, TetherVpnService::class.java)
        intent.putExtra("MODE", mode)
        startService(intent)
    }

    private fun stopVpnService() {
        logs.append("\n[Tether] Stopping VPN…")
        val intent = Intent(this, TetherVpnService::class.java)
        stopService(intent)
    }
}
