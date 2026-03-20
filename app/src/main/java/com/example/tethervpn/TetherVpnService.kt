package com.example.tethervpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

class TetherVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var gatewayProcess: Process? = null
    private var currentMode = 1 // default Tor

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentMode = intent?.getIntExtra("MODE", 1) ?: 1

        // Copy binary from assets if not already present
        val binaryFile = File(filesDir, "tor_gateway")
        if (!binaryFile.exists()) {
            assets.open("tor_gateway").use { input ->
                binaryFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            binaryFile.setExecutable(true)
            Log.i("TetherVpnService", "Binary copied from assets")
        }

        setupVpn()
        startGateway(binaryFile)
        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()
        Log.i("TetherVpnService", "VPN established: fd=${vpnInterface?.fileDescriptor}")
    }

    private fun startGateway(binaryFile: File) {
        vpnInterface?.fileDescriptor?.let { fd ->
            gatewayProcess = ProcessBuilder(binaryFile.absolutePath, fd.toString(), currentMode.toString())
                .redirectErrorStream(true)
                .start()

            Thread {
                gatewayProcess?.inputStream?.bufferedReader()?.forEachLine {
                    Log.i("Gateway", it)
                }
            }.start()
        }
    }

    override fun onDestroy() {
        Log.i("TetherVpnService", "Stopping VPN and gateway")
        gatewayProcess?.destroy()
        vpnInterface?.close()
        super.onDestroy()
    }
}
