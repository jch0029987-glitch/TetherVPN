package com.example.tethervpn

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var logs: TextView
    private lateinit var modeSelector: Spinner
    private lateinit var startVpnBtn: Button
    private lateinit var stopVpnBtn: Button

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logs = findViewById(R.id.logs)
        modeSelector = findViewById(R.id.mode_selector)
        startVpnBtn = findViewById(R.id.start_vpn)
        stopVpnBtn = findViewById(R.id.stop_vpn)

        // Request Nearby Wi-Fi permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (checkSelfPermission(android.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.permission.NEARBY_WIFI_DEVICES), PERMISSION_REQUEST_CODE)
            }
        }

        startVpnBtn.setOnClickListener { startVpn() }
        stopVpnBtn.setOnClickListener { stopVpn() }
    }

    private fun startVpn() {
        logs.append("\n[Tether] Starting VPN…")
        val intent = Intent(this, TetherVpnService::class.java)
        intent.putExtra("MODE", modeSelector.selectedItemPosition + 1)
        startService(intent)
    }

    private fun stopVpn() {
        logs.append("\n[Tether] Stopping VPN…")
        val intent = Intent(this, TetherVpnService::class.java)
        stopService(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logs.append("\n[Permissions] Nearby Wi-Fi granted")
            } else {
                logs.append("\n[Permissions] Nearby Wi-Fi denied")
            }
        }
    }
}
