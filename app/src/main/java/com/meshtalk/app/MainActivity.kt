package com.meshtalk.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.meshtalk.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val adapter = MessageAdapter(mutableListOf())
    private var manager: MeshManager? = null
    private var requestedName = ""

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) joinMesh(requestedName)
        else Snackbar.make(b.root, "خاصك تسمح بصلاحيات الأجهزة القريبة", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        b.messagesList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        b.messagesList.adapter = adapter
        b.nameInput.setText(getPreferences(MODE_PRIVATE).getString("name", ""))
        b.joinButton.setOnClickListener {
            requestedName = b.nameInput.text?.toString()?.trim().orEmpty()
            if (requestedName.length < 2) b.nameInput.error = "دخل اسم من حرفين على الأقل" else ensurePermissions()
        }
        b.sendButton.setOnClickListener {
            val text = b.messageInput.text.toString().trim()
            if (text.isNotEmpty()) { manager?.send(text); b.messageInput.text?.clear(); scrollDown() }
        }
    }

    private fun ensurePermissions() {
        val wanted = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) wanted += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        else wanted += Manifest.permission.ACCESS_FINE_LOCATION
        if (Build.VERSION.SDK_INT >= 33) wanted += Manifest.permission.NEARBY_WIFI_DEVICES
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) joinMesh(requestedName) else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun joinMesh(name: String) {
        getPreferences(MODE_PRIVATE).edit().putString("name", name).apply()
        adapter.setMyName(name)
        b.setupPanel.visibility = View.GONE; b.sendPanel.visibility = View.VISIBLE
        manager = MeshManager(this, name,
            onMessage = { runOnUiThread { adapter.add(it); scrollDown() } },
            onPeersChanged = { runOnUiThread { b.peersText.text = "الأجهزة المتصلة: $it"; if (it > 0) b.statusText.text = "متصل بالشبكة • $it قريب" } },
            onStatus = { runOnUiThread { b.statusText.text = it } }
        ).also { it.start() }
    }

    private fun scrollDown() { if (adapter.itemCount > 0) b.messagesList.scrollToPosition(adapter.itemCount - 1) }
    override fun onDestroy() { manager?.stop(); super.onDestroy() }
}
