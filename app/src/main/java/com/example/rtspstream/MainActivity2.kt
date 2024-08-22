package com.example.rtspstream

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.concurrent.Executors


class MainActivity2 : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the PreviewView
       // viewFinder = findViewById(R.id.viewFinder)
        localFileStreaming()


//        // Request camera and storage permissions
//        val requestPermissionLauncher = registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()
//        ) { permissions ->
//            if (permissions[Manifest.permission.CAMERA] == true &&
//                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
//                startCamera()
//            } else {
//                // Permission denied, handle appropriately
//            }
//        }
//
//        requestPermissionLauncher.launch(
//            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        )
    }

    private fun localFileStreaming(){
        try {
            val videoFile = copyRawResourceToInternalStorage(this, R.raw.demo, "demo.mp4")

            val ipAddress = getLocalIpAddress()
            Log.i("FFmpeg", "IPaddress == $ipAddress")


            val ffmpegCommand = arrayOf(
                "-stream_loop", "10",
                "-re",
                "-i", videoFile.absolutePath, // Path to the video file in internal storage
                "-f", "rtp_mpegts",
                "rtp://192.168.1.101:1234"
            )

            FFmpeg.executeAsync(ffmpegCommand) { executionId, returnCode ->
                if (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                    Log.i("FFmpeg", "Stream started successfully.")
                } else {
                    Log.e("FFmpeg", "Stream failed with return code: $returnCode")
                }
            }

        } catch (e: Exception) {
            Log.i("FFmpeg error", e.toString())
            e.printStackTrace()
        }
    }


    fun copyRawResourceToInternalStorage(context: Context, resourceId: Int, outputFileName: String): File {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val outputFile = File(context.filesDir, outputFileName)

        FileOutputStream(outputFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        return outputFile
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), ImageAnalysis.Analyzer { image ->
                val buffer = image.planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                // Pass this data to FFmpeg
                streamToFFmpeg(data)

                image.close()
            })

            try {
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun streamToFFmpeg(data: ByteArray) {
        val pipe = File(filesDir, "ffmpegpipe")
        if (!pipe.exists()) {
            Runtime.getRuntime().exec("mkfifo ${pipe.absolutePath}")
            startFFmpeg(pipe)
        }

        FileOutputStream(pipe).use { outputStream ->
            outputStream.write(data)
            outputStream.flush()
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }
    private fun startFFmpeg(pipe: File) {
        val ipAddress = getLocalIpAddress()
        Log.i("FFmpeg", "IPaddress == $ipAddress")

        //val rtspUrl = "rtsp://$ipAddress:port/stream"

        val ffmpegCommand = arrayOf(
            "-pix_fmt", "yuv420p",
            "-stream_loop", "10",
            "-re",
            "-i", pipe.absolutePath,
            "-f", "rtp_mpegts",
            "rtp://$ipAddress:1234"
        )

//        val ffmpegCommand = arrayOf(
//            "-f", "rawvideo",                 // Format is raw video
//            "-pix_fmt", "yuv420p",            // Pixel format, must match your input data
//            "-s", "1280x720",                 // Frame size, ensure this matches your input
//            "-r", "30",                       // Frame rate (frames per second)
//            "-i", pipe.absolutePath,          // Input is the named pipe
//            "-c:v", "libx264",                // Video codec to use
//            "-preset", "ultrafast",           // Faster encoding preset
//            "-f", "rtsp",                     // Output format is RTSP
//            rtspUrl                           // RTSP server URL
//        )

        FFmpeg.executeAsync("ffmpeg -re -i ${pipe.absolutePath} -f rtp_mpegts rtp://$ipAddress:1234") { executionId, returnCode ->
            if (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                Log.i("FFmpeg", "Stream started successfully.")
            } else {
                Log.e("FFmpeg", "Stream failed with return code: $returnCode")
            }
        }
    }

}