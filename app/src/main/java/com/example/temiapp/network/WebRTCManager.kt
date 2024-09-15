package com.example.temiapp.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoEncoderFactory
import org.webrtc.VideoTrack



class WebRTCManager(
    private val context: Context,
    private val signalingServerUrl: String
) {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var websocket: WebSocket
    private val rootEglBase = EglBase.create() // Initialize EglBase for video encoding/decoding

    private val TAG = "WebRTCManager"

    // Initialize WebRTC resources
    fun init() {
        Log.d(TAG, "Initializing WebRTCManager")
        createPeerConnectionFactory()
        createPeerConnection()
        setupLocalMediaStream()
        connectToSignalingServer()
    }

    // Create PeerConnectionFactory with video encoder and decoder
    private fun createPeerConnectionFactory() {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(initializationOptions)
        val options = PeerConnectionFactory.Options()

        val encoderFactory: VideoEncoderFactory =
            DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val decoderFactory: VideoDecoderFactory =
            DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoDecoderFactory(decoderFactory)
            .setVideoEncoderFactory(encoderFactory)
            .createPeerConnectionFactory()
    }

    // Create PeerConnection and handle ICE candidates
    private fun createPeerConnection() {
        Log.d(TAG, "Creating PeerConnection")

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d(TAG, "onIceCandidate: ${candidate?.sdp}")
                candidate?.let {
                    sendIceCandidateToServer(it)
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
            }

            override fun onAddStream(mediaStream: MediaStream?) {
                Log.d(TAG, "onAddStream: ${mediaStream?.id}")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d(TAG, "onDataChannel: ${dataChannel?.label()}")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "onIceGatheringChange: $state")
            }

            override fun onRemoveStream(mediaStream: MediaStream?) {
                Log.d(TAG, "onRemoveStream: ${mediaStream?.id}")
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "onSignalingChange: $state")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "onIceCandidatesRemoved")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded")
            }
        })!!

        Log.d(TAG, "PeerConnection created")
    }

    private fun setupLocalMediaStream() {
        Log.d(TAG, "Setting up local media stream")
        val rootEglBase = EglBase.create()
        // Ensure the video capturer is created successfully
        videoCapturer = createVideoCapturer() ?: run {
            Log.e(TAG, "Error: Video capturer could not be created. Exiting setup.")
            return
        }

        // Create video source and track
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        val localVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource)
        try {
            Log.d(TAG, "Starting video capture")
            videoCapturer.startCapture(1280, 720, 30)  // Adjust resolution and frame rate if needed
            Log.d(TAG, "Started capturing video")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error starting video capture: ${e.message}")
        }

        // Create and add audio track
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio", audioSource)

        // Add video and audio tracks to the peer connection
        peerConnection.addTrack(localVideoTrack)
        peerConnection.addTrack(localAudioTrack)

        Log.d(TAG, "Local media tracks added to PeerConnection")
    }

    // Stop video capture to release resources
    fun stopCapture() {
        try {
            videoCapturer.stopCapture()
            Log.d(TAG, "Stopped capturing video")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video capture: ${e.message}")
        }
    }

    // Connect to the signaling server via WebSocket
    private fun connectToSignalingServer() {
        Log.d(TAG, "Connecting to signaling server at $signalingServerUrl")
        val client = OkHttpClient()
        val request = Request.Builder().url(signalingServerUrl).build()
        websocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "WebSocket connected successfully")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d(TAG, "WebSocket received message: $text")
                    val jsonObject = JSONObject(text)
                    Log.d(TAG, "Parsed WebSocket message: $jsonObject")
                    handleWebSocketMessage(jsonObject)
                } catch (e: JSONException) {
                    Log.e(TAG, "Failed to parse WebSocket message: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
            }
        })
    }

    // Send ICE candidate to the signaling server
    private fun sendIceCandidateToServer(candidate: IceCandidate) {
        Log.d(TAG, "Sending ICE candidate to server: ${candidate.sdp}")
        val message = JSONObject().apply {
            put("type", "candidate")
            put("candidate", JSONObject().apply {
                put("candidate", candidate.sdp)
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
            })
        }
        websocket.send(message.toString())
    }

    // Handle messages from the signaling server
    private fun handleWebSocketMessage(message: JSONObject) {
        Log.d(TAG, "Handling WebSocket message: $message")
        when (message.getString("type")) {
            "answer" -> {
                val answer = message.getJSONObject("sdp")
                peerConnection.setRemoteDescription(
                    SdpObserverImpl(),
                    SessionDescription(SessionDescription.Type.ANSWER, answer.getString("sdp"))
                )
                Log.d(TAG, "Answer SDP set as remote description")
            }
            "candidate" -> {
                val candidate = message.getJSONObject("candidate")
                peerConnection.addIceCandidate(
                    IceCandidate(
                        candidate.getString("sdpMid"),
                        candidate.getInt("sdpMLineIndex"),
                        candidate.getString("candidate")
                    )
                )
                Log.d(TAG, "Added ICE candidate: ${candidate.getString("candidate")}")
            }
        }
    }

    fun startCall() {
        Log.d(TAG, "Starting WebRTC call by creating an offer")

        if (!::peerConnection.isInitialized) {
            Log.e(TAG, "Error: peerConnection is not initialized.")
            return
        }

        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) {
                    Log.e(TAG, "SDP offer creation failed, SDP is null")
                    return
                }

                // Modify the SDP if needed
                val modifiedSdp = sdp.description.replace("a=recvonly", "a=sendrecv")

                Log.d(TAG, "Modified SDP offer: $modifiedSdp")

                // Set the modified SDP as the local description
                peerConnection.setLocalDescription(this, SessionDescription(sdp.type, modifiedSdp))

                // Send the offer to the signaling server
                val message = JSONObject().apply {
                    put("type", "offer")
                    put("sdp", JSONObject().apply {
                        put("type", sdp.type.canonicalForm())
                        put("sdp", modifiedSdp)
                    })
                }
                websocket.send(message.toString())
                Log.d(TAG, "Offer SDP sent to signaling server: $modifiedSdp")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Error setting local description: $error")
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Error creating offer: $error")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "Local description set successfully")
            }
        }, MediaConstraints())
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front-facing camera capturer")
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                Log.d(TAG, "Using back-facing camera as fallback")
                return enumerator.createCapturer(deviceName, null)
            }
        }
        Log.e(TAG, "Failed to find a suitable camera")
        return null
    }

    // Implement SdpObserver
    private class SdpObserverImpl : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
