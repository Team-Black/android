package com.ivt.black.screen_streamer

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Environment
import android.os.StrictMode
import android.provider.Settings

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import java.lang.Exception
import  java.io.IOException

import com.google.android.material.snackbar.Snackbar

import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val streamController = "ivt.black/stream_controller"
    private val TAG = "MainActivity"
    private val REQUEST_CODE = 1000
    private var mScreenDensity: Int = 0
    private var mProjectionManager: MediaProjectionManager? = null
    private val DISPLAY_WIDTH = 1080
    private val DISPLAY_HEIGHT = 1920
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    private var mToggleButton: ToggleButton? = null
    private var mMediaRecorder: MediaRecorder? = null
    private val ORIENTATIONS = SparseIntArray()
    private val REQUEST_PERMISSIONS = 10
    private lateinit var _result: MethodChannel.Result
    private var socket: java.net.Socket? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build());
        }
        val metrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(metrics)
        mScreenDensity = metrics.densityDpi

        mMediaRecorder = MediaRecorder()

        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        MethodChannel(flutterView, streamController).setMethodCallHandler { call, result ->
            when (call.method) {
                "start" -> {
                    _result = result
                    thread().start()
                    _result.success("started")
//                    return
//
//                    if (ContextCompat.checkSelfPermission(this@MainActivity,
//                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
//                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//
//                            ActivityCompat.requestPermissions(this@MainActivity,
//                                    arrayOf<String>(Manifest.permission
//                                            .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
//                                    REQUEST_PERMISSIONS)
//
//                        } else {
//                            ActivityCompat.requestPermissions(this@MainActivity,
//                                    arrayOf<String>(Manifest.permission
//                                            .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
//                                    REQUEST_PERMISSIONS)
//                        }
//                    } else {
//                        initRecorder()
//                        shareScreen()
//                        result.success("started")
//                    }

                }
                "stop" -> {
                    _result = result
                    mMediaRecorder!!.stop()
                    mMediaRecorder!!.reset()
                    Log.v(TAG, "Stopping Recording")
                    stopScreenSharing()
                    result.success("stopped")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: $requestCode")
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            return
        }
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder!!.start()
    }


    private fun shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE)
            return
        }
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder!!.start()
        _result.success("started")
        thread().start()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder!!.getSurface(), null, null
                /*Handler*/)/*Callbacks*/
    }

    private fun initRecorder() {
        var sf: String = ""
        try {

            val hostname: String = "10.1.99.196"
            val port: Int = 3333
            sf += "0"
            val socket: java.net.Socket = java.net.Socket(hostname, port)
            sf += "1"
            //val pfd: android.os.ParcelFileDescriptor = android.os.ParcelFileDescriptor.fromSocket(socket)
            sf += "2"
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            val kk: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

            sf += "3"
            mMediaRecorder!!.setOutputFile(kk + "/video.mp4")
            sf += "4"

            mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            sf += "5"
            mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            sf += "6"
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            sf += "7"
            mMediaRecorder!!.setVideoEncodingBitRate(5120 * 1000)
            sf += "8"
            mMediaRecorder!!.setVideoFrameRate(24)
            sf += "9"
            val rotation = getWindowManager().getDefaultDisplay().getRotation()
            sf += "10"
            val orientation = ORIENTATIONS.get(rotation + 90)
            sf += "11"
            mMediaRecorder!!.setOrientationHint(orientation)
            sf += "12"
            mMediaRecorder!!.prepare()
            sf += "13"
        } catch (e: Exception) {
            _result.success("fdf $e" + sf)
            Log.v(TAG, "Recording Stopped $e" + sf)
            e.printStackTrace()
        }

    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            if (false) {
                mMediaRecorder!!.stop()
                mMediaRecorder!!.reset()
                Log.v(TAG, "Recording Stopped")
            }
            mMediaProjection = null
            stopScreenSharing()
        }
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay!!.release()
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
        Log.i(TAG, "MediaProjection Stopped")
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            @NonNull permissions: Array<String>,
                                            @NonNull grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if (grantResults.size > 0 && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    initRecorder()
                    shareScreen()
                } else {
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.setData(Uri.parse("package:" + getPackageName()))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(intent)
                }
                return
            }
        }
    }

    fun thread(): Thread {
        var inputStream: java.io.FileInputStream? = null;
        try {
            val kk: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            inputStream = java.io.FileInputStream(kk + "/video.mp4");
        } catch (e: java.io.FileNotFoundException) {
            Log.v(TAG, "$e")
            e.printStackTrace();
        }
        while (true) {
            var buffer: ByteArray = kotlin.ByteArray(1024)
            try {
                buffer = byteArrayOf(inputStream!!.available().toByte())
            } catch (e: java.io.IOException) {
                Log.v(TAG, "$e")
                e.printStackTrace();
            }
            try {
                inputStream!!.read(buffer);
            } catch (e: java.io.IOException) {
                Log.v(TAG, "$e")
                e.printStackTrace();
            }
            try {
                socket!!.getOutputStream().write(buffer);
                socket!!.getOutputStream().flush();
            } catch (e: java.io.IOException) {
                Log.v(TAG, "$e")
                e.printStackTrace();
            }
        }
    }

}


