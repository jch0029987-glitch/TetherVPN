package com.example.tethervpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var modeGroup: RadioGroup
    private lateinit var radioDirect: RadioButton
    private lateinit var radioTor: RadioButton
    private lateinit var radioProxy: RadioButton
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBtn = findViewById(R.id.startButton)
        stopBtn = findViewById(R.id.stopButton)
        modeGroup = findViewById(R.id.modeSelector)
        radioDirect = findViewById(R.id.radioDirect)
        radioTor = findViewById(R.id.radioTor)
        radioProxy = findViewById(R.id.radioProxy)
        statusText = findViewById(R.id.statusText)

        startBtn.setOnClickListener { prepareVpnAndStart() }
        stopBtn.setOnClickListener { stopVpnService() }
    }

    private fun prepareVpnAndStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, 1)
        else startVpnService()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) startVpnService()
    }

    private fun startVpnService() {
        val mode = when (modeGroup.checkedRadioButtonId) {
            R.id.radioTor -> "TOR"
            R.id.radioProxy -> "SOCKS"
            else -> "DIRECT"
        }
        val intent = Intent(this, TetherVpnService::class.java)
        intent.putExtra("mode", mode)
        startService(intent)
        statusText.text = "Status: VPN Started ($mode)"
    }

    private fun stopVpnService() {
        val intent = Intent(this, TetherVpnService::class.java)
        stopService(intent)
        statusText.text = "Status: VPN Stopped"
    }
}
