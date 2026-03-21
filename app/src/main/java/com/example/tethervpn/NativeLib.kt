package com.example.tethervpn

object NativeLib {
    init { System.loadLibrary("tethervpn") }

    external fun startVpn(fd: Int)
    external fun stopVpn()
}
