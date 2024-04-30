package pers.lenwind.wol

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)

        val savedIpAddress = sharedPref.getString("ip_address", "")
        if (savedIpAddress!!.isEmpty()) {
            return
        }
        val savedMacAddress = sharedPref.getString("mac_address", "")
        findViewById<EditText>(R.id.edit_ip_address).text.insert(0, savedIpAddress)
        findViewById<EditText>(R.id.edit_mac_address).text.insert(0, savedMacAddress)
    }

    private fun sendWakeOnLanPacket(ipAddress: String, macAddress: String) {
        val macBytes = ByteArray(6)
        macAddress.replace(":", "").chunked(2)
            .forEachIndexed { index, twoChars ->
                macBytes[index] = twoChars.toIntOrNull(16)?.toByte() ?: throw IllegalArgumentException("Invalid MAC address format.")
            }
        val wolMagicPacket = ByteArray(6 + (macBytes.size * 16))
        wolMagicPacket.fill(0xFF.toByte(), 0, 6)

        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, wolMagicPacket, i * 6 + 6, 6)
        }

        val socket = DatagramSocket()
        val packet =
            DatagramPacket(wolMagicPacket, wolMagicPacket.size, InetAddress.getByName(ipAddress), 9)

        try {
            socket.send(packet)
            Toast.makeText(this, "Wake-on-LAN packet sent to $ipAddress", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error sending Wake-on-LAN packet: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            socket.close()
        }
    }

    // 保存按钮点击事件
    fun button_save(view: View) {

        val ipAddress = findViewById<EditText>(R.id.edit_ip_address).text
        val macAddress = findViewById<EditText>(R.id.edit_mac_address).text

        // 这里简单地将它们保存到 SharedPreferences 或者其他持久化存储中
        with(sharedPref.edit()) {
            putString("ip_address", ipAddress.toString())
            putString("mac_address", macAddress.toString())
            apply() // 应用更改
        }
    }

    // 唤醒按钮点击事件
    fun button_wake_on_lan(view: View) {
        val savedIpAddress = sharedPref.getString("ip_address", "")
        val savedMacAddress = sharedPref.getString("mac_address", "")

        if (savedIpAddress!!.isNotEmpty() && savedMacAddress!!.isNotEmpty()) {
            // 实现发送 Wake-on-LAN 数据包的逻辑
            Thread(kotlinx.coroutines.Runnable {
                sendWakeOnLanPacket(savedIpAddress, savedMacAddress)
            }).start()

        } else {
            Toast.makeText(this, "请先保存有效的IP和MAC地址", Toast.LENGTH_SHORT).show()
        }
    }
}