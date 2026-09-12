package com.meshtalk.app

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.util.UUID

class MeshManager(
    context: Context,
    private val username: String,
    private val onMessage: (MeshMessage) -> Unit,
    private val onPeersChanged: (Int) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val client = Nearby.getConnectionsClient(context)
    private val serviceId = "com.meshtalk.app.mesh.v1"
    private val strategy = Strategy.P2P_CLUSTER
    private val nodeId = UUID.randomUUID().toString()
    private val endpoints = linkedSetOf<String>()
    private val pending = hashSetOf<String>()
    private val seen = LinkedHashSet<String>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val message = MeshMessage.decode(bytes) ?: return
            if (!seen.add(message.id)) return
            trimSeen()
            onMessage(message)
            if (message.ttl > 1) broadcast(message.nextHop(), except = endpointId)
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pending.remove(endpointId)
            if (result.status.isSuccess) endpoints.add(endpointId)
            onPeersChanged(endpoints.size)
        }
        override fun onDisconnected(endpointId: String) {
            endpoints.remove(endpointId); pending.remove(endpointId); onPeersChanged(endpoints.size)
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (endpointId in endpoints || !pending.add(endpointId)) return
            client.requestConnection(username, endpointId, lifecycle)
                .addOnFailureListener { pending.remove(endpointId) }
        }
        override fun onEndpointLost(endpointId: String) { pending.remove(endpointId) }
    }

    fun start() {
        val advertise = AdvertisingOptions.Builder().setStrategy(strategy).build()
        val discover = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(username, serviceId, lifecycle, advertise)
            .addOnFailureListener { onStatus("تعذر تشغيل الإعلان: ${it.localizedMessage}") }
        client.startDiscovery(serviceId, discovery, discover)
            .addOnSuccessListener { onStatus("كتقلب على الأجهزة القريبة…") }
            .addOnFailureListener { onStatus("تعذر البحث: ${it.localizedMessage}") }
    }

    fun send(text: String): MeshMessage {
        val message = MeshMessage(senderId = nodeId, senderName = username, body = text)
        seen.add(message.id); onMessage(message); broadcast(message)
        return message
    }

    private fun broadcast(message: MeshMessage, except: String? = null) {
        endpoints.filter { it != except }.forEach { client.sendPayload(it, Payload.fromBytes(message.encode())) }
    }

    private fun trimSeen() { while (seen.size > 2_000) seen.remove(seen.first()) }

    fun stop() {
        client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints()
        endpoints.clear(); pending.clear(); onPeersChanged(0)
    }
}
