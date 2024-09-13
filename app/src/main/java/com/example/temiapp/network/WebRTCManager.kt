package com.example.temiapp.network

import android.content.Context
import android.content.Intent  // <-- Add this import
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    private val signalingServerUrl: String
) {

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var screenCapturer: ScreenCapturerAndroid
    private lateinit var websocket: WebSocket
    private val rootEglBase = EglBase.create() // Initialize EglBase for video encoding/decoding

    private val TAG = "WebRTCManager"

    // Initialize WebRTC resources
    fun init() {
        Log.d(TAG, "Initializing WebRTCManager")
        createPeerConnectionFactory()
        createPeerConnection()
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

        // Define ICE servers (STUN/TURN)
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

    fun startScreenCapture(data: Intent) {
        Log.d(TAG, "Starting screen capture")

        // Initialize ScreenCapturerAndroid with the Intent and the MediaProjection.Callback
        screenCapturer = ScreenCapturerAndroid(
            data, // Pass the Intent object here
            object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "Screen capture stopped")
                }
            }
        )

        val videoSource = peerConnectionFactory.createVideoSource(false)
        localVideoTrack = peerConnectionFactory.createVideoTrack("screen_video", videoSource)

        // Start capturing the screen
        try {
            screenCapturer.startCapture(1280, 720, 30)  // Adjust resolution and frame rate as needed
            Log.d(TAG, "Started capturing screen")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error starting screen capture: ${e.message}")
        }

        // Add the screen video track to the PeerConnection
        peerConnection.addTrack(localVideoTrack)

        // Optionally, add an audio track if you want to capture microphone audio
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio", audioSource)
        peerConnection.addTrack(localAudioTrack)

        Log.d(TAG, "Local media tracks added to PeerConnection")
    }




    // Connect to the signaling server via WebSocket
    private fun connectToSignalingServer() {
        Log.d(TAG, "Connecting to signaling server at $signalingServerUrl")
        val client = OkHttpClient()
        val request = Request.Builder().url(signalingServerUrl).build()
        websocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket received message: $text")
                handleWebSocketMessage(JSONObject(text))
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
            }
        }
    }

    // Start a call by creating an offer
    fun startCall() {
        Log.d(TAG, "Starting WebRTC call by creating an offer")
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) {
                    Log.e(TAG, "SDP offer creation failed, SDP is null")
                    return
                }

                // Modify the SDP to correct video parameters
                var modifiedSdp = sdp.description

                // Fix the m=video section for sendrecv or other issues
                modifiedSdp = modifiedSdp.replace("a=recvonly", "a=sendrecv")
                modifiedSdp = modifiedSdp.replace("m=video 9 UDP/TLS/RTP/SAVPF 0", "m=video 9 UDP/TLS/RTP/SAVPF 96")

                // Log the modified SDP for debugging
                Log.d(TAG, "Modified SDP offer: $modifiedSdp")

                // Set the modified SDP as the local description
                peerConnection.setLocalDescription(this, SessionDescription(sdp.type, modifiedSdp))
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

    // Implement SdpObserver
    private class SdpObserverImpl : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}





//package com.example.temiapp.network
//
//import android.content.Context
//import android.util.Log
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.WebSocket
//import okhttp3.WebSocketListener
//import org.json.JSONObject
//import org.webrtc.*
//import org.webrtc.PeerConnection.IceServer
//import org.webrtc.PeerConnection.RTCConfiguration
//import org.webrtc.PeerConnectionFactory.InitializationOptions
//import android.media.projection.MediaProjection
//import org.webrtc.ScreenCapturerAndroid
//
//class WebRTCManager(
//    private val context: Context,
//    private val signalingServerUrl: String
//) {
//
//    private lateinit var peerConnectionFactory: PeerConnectionFactory
//    private lateinit var peerConnection: PeerConnection
//    private lateinit var localAudioTrack: AudioTrack
//    private lateinit var localVideoTrack: VideoTrack
//    private lateinit var videoCapturer: VideoCapturer
//    private lateinit var websocket: WebSocket
//    private val rootEglBase = EglBase.create() // Initialize EglBase for video encoding/decoding
//
//    private val TAG = "WebRTCManager"  // Add a tag for easier log filtering
//
//    // Initialize WebRTC resources
//    fun init() {
//        Log.d(TAG, "Initializing WebRTCManager")
//        createPeerConnectionFactory()
//        createPeerConnection()
//        setupLocalMediaStream()
//        connectToSignalingServer()
//    }
//
//    // Create PeerConnectionFactory with video encoder and decoder
//    private fun createPeerConnectionFactory() {
//        val initializationOptions = InitializationOptions.builder(context)
//            .createInitializationOptions()
//
//        PeerConnectionFactory.initialize(initializationOptions)
//        val options = PeerConnectionFactory.Options()
//
//        val encoderFactory: VideoEncoderFactory =
//            DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
//        val decoderFactory: VideoDecoderFactory =
//            DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
//
//        peerConnectionFactory = PeerConnectionFactory.builder()
//            .setOptions(options)
//            .setVideoDecoderFactory(decoderFactory)
//            .setVideoEncoderFactory(encoderFactory)
//            .createPeerConnectionFactory()
//    }
//
//    // Create PeerConnection and handle ICE candidates
//    private fun createPeerConnection() {
//        Log.d(TAG, "Creating PeerConnection")
//
//        // Define ICE servers (STUN/TURN)
//        val iceServers = listOf(
//            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
//        )
//
//        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
//
//        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
//            override fun onIceCandidate(candidate: IceCandidate?) {
//                Log.d(TAG, "onIceCandidate: ${candidate?.sdp}")
//                candidate?.let {
//                    sendIceCandidateToServer(it)
//                }
//            }
//
//            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
//                Log.d(TAG, "onIceConnectionChange: $state")
//            }
//
//            override fun onIceConnectionReceivingChange(receiving: Boolean) {
//                Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
//            }
//
//            override fun onAddStream(mediaStream: MediaStream?) {
//                Log.d(TAG, "onAddStream: ${mediaStream?.id}")
//            }
//
//            override fun onDataChannel(dataChannel: DataChannel?) {
//                Log.d(TAG, "onDataChannel: ${dataChannel?.label()}")
//            }
//
//            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
//                Log.d(TAG, "onIceGatheringChange: $state")
//            }
//
//            override fun onRemoveStream(mediaStream: MediaStream?) {
//                Log.d(TAG, "onRemoveStream: ${mediaStream?.id}")
//            }
//
//            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
//                Log.d(TAG, "onSignalingChange: $state")
//            }
//
//            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
//                Log.d(TAG, "onIceCandidatesRemoved")
//            }
//
//            override fun onRenegotiationNeeded() {
//                Log.d(TAG, "onRenegotiationNeeded")
//            }
//        })!!
//
//        Log.d(TAG, "PeerConnection created")
//    }
//
//    private fun setupLocalMediaStream() {
//        Log.d(TAG, "Setting up local media stream")
//
//        // Video: capture from Temi’s camera
//        videoCapturer = createVideoCapturer() ?: throw RuntimeException("Video capturer could not be created")
//
//        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
//        localVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource)
//
//        // Start capturing video if the capturer is properly initialized
//        try {
//            videoCapturer.startCapture(1280, 720, 30)
//            Log.d(TAG, "Started capturing video")
//        } catch (e: RuntimeException) {
//            Log.e(TAG, "Error starting video capture: ${e.message}")
//        }
//
//        // Audio: capture audio from Temi’s mic
//        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
//        localAudioTrack = peerConnectionFactory.createAudioTrack("audio", audioSource)
//
//        // Add each track separately using AddTrack (Unified Plan)
//        peerConnection.addTrack(localVideoTrack)
//        peerConnection.addTrack(localAudioTrack)
//
//        Log.d(TAG, "Local media tracks added to PeerConnection")
//    }
//
//
//    private fun createVideoCapturer(): VideoCapturer {
//        val enumerator = Camera2Enumerator(context)
//
//        // Try to find a front-facing camera
//        for (deviceName in enumerator.deviceNames) {
//            if (enumerator.isFrontFacing(deviceName)) {
//                Log.d(TAG, "Creating front-facing camera capturer")
//                return enumerator.createCapturer(deviceName, null)
//            }
//        }
//
//        // If no front-facing camera is found, try to find any back-facing camera as a fallback
//        for (deviceName in enumerator.deviceNames) {
//            if (enumerator.isBackFacing(deviceName)) {
//                Log.d(TAG, "No front-facing camera found. Using back-facing camera as fallback.")
//                return enumerator.createCapturer(deviceName, null)
//            }
//        }
//
//        throw IllegalStateException("No front-facing or back-facing camera found.")
//    }
//
//    // Connect to the signaling server via WebSocket
//    private fun connectToSignalingServer() {
//        Log.d(TAG, "Connecting to signaling server at $signalingServerUrl")
//        val client = OkHttpClient()
//        val request = Request.Builder().url(signalingServerUrl).build()
//        websocket = client.newWebSocket(request, object : WebSocketListener() {
//            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
//                Log.d(TAG, "WebSocket connected")
//            }
//
//            override fun onMessage(webSocket: WebSocket, text: String) {
//                Log.d(TAG, "WebSocket received message: $text")
//                handleWebSocketMessage(JSONObject(text))
//            }
//
//            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
//                Log.d(TAG, "WebSocket closing: $reason")
//            }
//
//            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
//                Log.d(TAG, "WebSocket closed: $reason")
//            }
//
//            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
//                Log.e(TAG, "WebSocket error: ${t.message}")
//            }
//        })
//    }
//
//    // Send ICE candidate to the signaling server
//    private fun sendIceCandidateToServer(candidate: IceCandidate) {
//        Log.d(TAG, "Sending ICE candidate to server: ${candidate.sdp}")
//        val message = JSONObject().apply {
//            put("type", "candidate")
//            put("candidate", JSONObject().apply {
//                put("candidate", candidate.sdp)
//                put("sdpMid", candidate.sdpMid)
//                put("sdpMLineIndex", candidate.sdpMLineIndex)
//            })
//        }
//        websocket.send(message.toString())
//    }
//
//    // Handle messages from the signaling server
//    private fun handleWebSocketMessage(message: JSONObject) {
//        Log.d(TAG, "Handling WebSocket message: $message")
//        when (message.getString("type")) {
//            "answer" -> {
//                val answer = message.getJSONObject("sdp")
//                peerConnection.setRemoteDescription(
//                    SdpObserverImpl(),
//                    SessionDescription(SessionDescription.Type.ANSWER, answer.getString("sdp"))
//                )
//            }
//            "candidate" -> {
//                val candidate = message.getJSONObject("candidate")
//                peerConnection.addIceCandidate(
//                    IceCandidate(
//                        candidate.getString("sdpMid"),
//                        candidate.getInt("sdpMLineIndex"),
//                        candidate.getString("candidate")
//                    )
//                )
//            }
//        }
//    }
//
//    // Start a call by creating an offer
//    fun startCall() {
//        Log.d(TAG, "Starting WebRTC call by creating an offer")
//        peerConnection.createOffer(object : SdpObserver {
//            override fun onCreateSuccess(sdp: SessionDescription?) {
//                if (sdp == null) {
//                    Log.e(TAG, "SDP offer creation failed, SDP is null")
//                    return
//                }
//
//                // Modify the SDP to correct video parameters
//                var modifiedSdp = sdp.description
//
//                // Fix the m=video section for sendrecv or other issues
//                modifiedSdp = modifiedSdp.replace("a=recvonly", "a=sendrecv")
//                modifiedSdp = modifiedSdp.replace("m=video 9 UDP/TLS/RTP/SAVPF 0", "m=video 9 UDP/TLS/RTP/SAVPF 96")
//
//                // Log the modified SDP for debugging
//                Log.d(TAG, "Modified SDP offer: $modifiedSdp")
//
//                // Set the modified SDP as the local description
//                peerConnection.setLocalDescription(this, SessionDescription(sdp.type, modifiedSdp))
//            }
//
//            override fun onSetFailure(error: String?) {
//                Log.e(TAG, "Error setting local description: $error")
//            }
//
//            override fun onCreateFailure(error: String?) {
//                Log.e(TAG, "Error creating offer: $error")
//            }
//
//            override fun onSetSuccess() {
//                Log.d(TAG, "Local description set successfully")
//            }
//        }, MediaConstraints())
//    }
//
//    // Implement SdpObserver
//    private class SdpObserverImpl : SdpObserver {
//        override fun onCreateSuccess(sdp: SessionDescription?) {}
//        override fun onSetSuccess() {}
//        override fun onCreateFailure(error: String?) {}
//        override fun onSetFailure(error: String?) {}
//    }
//}
