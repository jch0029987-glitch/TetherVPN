package com.example.tethervpn

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
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
    private val VPN_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logs = findViewById(R.id.logs)
        modeSelector = findViewById(R.id.mode_selector)
        startVpnBtn = findViewById(R.id.start_vpn)
        stopVpnBtn = findViewById(R.id.stop_vpn)

        // 1️⃣ Check Nearby Wi-Fi permission
        requestNearbyWifiPermission()

        // 2️⃣ Check VPN permission
        requestVpnPermission()

        // 3️⃣ Check if the native library loaded
        checkLibrary()

        startVpnBtn.setOnClickListener { requestVpnPermission() } // start VPN only after permission
        stopVpnBtn.setOnClickListener { stopVpn() }
    }

    private fun requestNearbyWifiPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.NEARBY_WIFI_DEVICES), PERMISSION_REQUEST_CODE)
            } else {
                logs.append("\n[Permissions] Nearby Wi-Fi already granted")
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            logs.append("\n[Tether] VPN permission already granted")
            startVpn()
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                logs.append("\n[Tether] VPN permission granted")
                startVpn()
            } else {
                logs.append("\n[Tether] VPN permission denied")
            }
        }
    }

    // Check if native library loaded
    private fun checkLibrary() {
        try {
            System.loadLibrary("tun2socks")
            logs.append("\n[Library] tun2socks loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            logs.append("\n[Library] Failed to load tun2socks: ${e.message}")
        }
    }
}
