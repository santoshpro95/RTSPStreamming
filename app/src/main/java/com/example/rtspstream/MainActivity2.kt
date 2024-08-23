package com.example.rtspstream

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.concurrent.Executors


class MainActivity2 : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private val cameraId = "0"  // The ID of the camera (usually "0" for the back camera)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the PreviewView
        // viewFinder = findViewById(R.id.viewFinder)
        // localFileStreaming()
      // val vidFile = copyRawResourceToInternalStorage(this, R.raw.demo, "demo.mp4")
//        val vidFile = File("/data/user/0/com.example.rtspstream/files/", "frame.mp4");
//       startFFmpeg(vidFile)

        // Check and request camera permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            return
        }

        startCamera()
    }

    private fun startCamera() {
        val backgroundThread = HandlerThread("CameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)

        imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
        }
    }
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        image?.let {
            val width = image.width
            val height = image.height

            // Log the dimensions of the image
            Log.d("ImageReader", "Captured image with width: $width, height: $height")

            if (width > 0 && height > 0) {
                val buffer: ByteBuffer = image.planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                // Save the frame to a file
                val frameFile = File(filesDir, "frame.jpeg")
                saveFrameToFile(data, frameFile)

                // Log the size of the saved file
                Log.d("File", "Frame file size: ${frameFile.length()} bytes")

                // Stream the saved frame using FFmpeg
                startFFmpeg(frameFile)
            } else {
                Log.e("ImageReader", "Invalid image size: $width x $height")
            }

            image.close()
        } ?: Log.e("ImageReader", "Image is null")
    }


    private fun saveFrameToFile(data: ByteArray, file: File) {
        FileOutputStream(file).use { output ->
            output.write(data)
        }

        if (file.length() == 0L) {
            Log.e("File", "Frame file is empty, skipping FFmpeg command")
        } else {
            Log.d("File", "Frame file size: ${file.length()} bytes")
        }
    }

    private fun createCameraPreviewSession() {
        val surface = imageReader.surface
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureSession.setRepeatingRequest(
                        captureRequestBuilder.build(),
                        null,
                        backgroundHandler
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraCaptureSession", "Configuration failed")
                }
            },
            backgroundHandler
        )
    }


    private fun copyRawResourceToInternalStorage(
        context: Context,
        resourceId: Int,
        outputFileName: String
    ): File {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val outputFile = File(context.filesDir, outputFileName)

        FileOutputStream(outputFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        return outputFile
    }


    private fun startFFmpeg(pipe: File) {
        Log.i("pipe == data", pipe.absolutePath)

        val ffmpegCommand = arrayOf(
            "-re",
            "-i", pipe.absolutePath,  // Replace with actual file path
            "-pix_fmt", "yuyv422",
            "-maxrate", "2000k",
            "-bufsize", "2000k",
            "-acodec", "aac",
            "-ar", "44100",
            "-b:a", "128k",
            "-f", "rtp_mpegts",
            "rtp://192.168.1.101:9988"
        )

        FFmpeg.executeAsync(ffmpegCommand) { executionId, returnCode ->
            if (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                Log.i("FFmpeg", "Stream started successfully.")
            } else {
                Log.e("FFmpeg", "Stream failed with return code: $returnCode")
            }
        }
    }

}