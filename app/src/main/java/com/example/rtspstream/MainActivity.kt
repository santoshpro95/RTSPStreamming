package com.example.rtspstream

import android.Manifest
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer


class MainActivity :  AppCompatActivity(),  SurfaceHolder.Callback{
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private val rtspUrl = "rtsp://192.168.1.102:554/play1.sdp"  // Change this to your IP address and port
    private val CAMERA_REQUEST_CODE = 101
    private lateinit var camera: Camera
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up a full-screen black window.
        // Set up a full-screen black window.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window = window
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.setBackgroundDrawableResource(android.R.color.black)

        setContentView(R.layout.activity_main)

        // Initialize SurfaceView
       // surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setFixedSize(320, 240);
        requestPermissions() // Request necessary permissions
    }



    private fun startCamera() {
        camera = Camera.open()
        camera.setPreviewDisplay(surfaceHolder)
        camera.startPreview()
    }

    private fun startRTSPServer() {
        val media = Media(libVLC, rtspUrl)
        media.setHWDecoderEnabled(true, false)
        media.addOption(":sout=#rtp{sdp=rtsp://:554/play1.sdp}")
        media.addOption(":no-sout-all")
        media.addOption(":sout-keep")

        mediaPlayer.media = media
        mediaPlayer.play()
    }



    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), CAMERA_REQUEST_CODE
        )

        startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVLC.release()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Initialize LibVLC
        libVLC = LibVLC(this)
        mediaPlayer = MediaPlayer(libVLC)

        startCamera()
        startRTSPServer()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if necessary
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera.stopPreview()
        camera.release()
        mediaPlayer.release()
        libVLC.release()
    }
}