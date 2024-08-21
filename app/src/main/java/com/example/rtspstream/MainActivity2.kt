package com.example.rtspstream

import android.Manifest
import android.content.Context

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.FFmpeg

class MainActivity2 : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the PreviewView
        viewFinder = findViewById(R.id.viewFinder)


        requestPermissions()


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Attach the camera preview to the view
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                // Handle exceptions
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRtspStreaming() {
        // Example FFmpeg command to start an RTSP server
        // Note: This is highly simplified and depends on your setup
        val rtspCommand =
            "-re -i input.mp4 -f rtsp -rtsp_transport tcp rtsp://localhost:8554/live.sdp"

        // Execute FFmpeg command
        FFmpeg.executeAsync(rtspCommand) { _, returnCode ->
            if (returnCode == 0) {
                Log.i("RTSP Streamming", "Started Successfully")
                // Streaming started successfully
            } else {
                // Streaming failed
                Log.i("RTSP Streamming", "Failed")
            }
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
        startCamera()

        startRtspStreaming()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}