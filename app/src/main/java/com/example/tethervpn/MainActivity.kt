package com.example.tethervpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
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

        log("[UI] Ready")

        startButton.setOnClickListener {
            log("[UI] Start clicked")
            prepareVpn()
        }

        stopButton.setOnClickListener {
            log("[UI] Stop clicked")
            stopVpn()
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            log("[VPN] Requesting permission...")
            startActivityForResult(intent, 100)
        } else {
            log("[VPN] Permission already granted")
            startVpn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                log("[VPN] Permission granted")
                startVpn()
            } else {
                log("[VPN] Permission denied")
                Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVpn() {
        val mode = when (modeSelector.checkedRadioButtonId) {
            R.id.radioTor -> 1
            R.id.radioDirect -> 2
            R.id.radioProxy -> 3
            else -> 1
        }

        log("[VPN] Starting with mode=$mode")

        val intent = Intent(this, TetherVpnService::class.java)
        intent.putExtra("MODE", mode)
        startService(intent)
    }

    private fun stopVpn() {
        log("[VPN] Stopping service")

        val intent = Intent(this, TetherVpnService::class.java)
        stopService(intent)
    }

    private fun log(msg: String) {
        runOnUiThread {
            logs.append("\n$msg")
        }
    }
}
