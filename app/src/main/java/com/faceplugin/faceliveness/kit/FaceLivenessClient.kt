package com.faceplugin.faceliveness.kit

import android.content.Context
import android.graphics.Bitmap
import com.faceplugin.facelivenessdk.FaceBox
import com.faceplugin.facelivenessdk.FaceDetectionParam
import com.faceplugin.facelivenessdk.FaceLivenessSDK

/**
 * Thin app-facing wrapper around [FaceLivenessSDK] for the liveness demo.
 * Activate once, then use face detection + optional VideoWorker tracking.
 */
class FaceLivenessClient private constructor(context: Context) {
    private val appContext = context.applicationContext

    val isEngineReady: Boolean get() = engineReady

    fun activate(license: String, completion: (Int) -> Unit) {
        FaceLivenessQueue.async {
            if (engineReady) {
                completion(FaceLivenessSDK.SDK_SUCCESS)
                return@async
            }
            try {
                FaceLivenessSDK.getMachineCode(appContext)
                var ret = FaceLivenessSDK.setActivation(appContext, license)
                if (ret == FaceLivenessSDK.SDK_SUCCESS) {
                    ret = FaceLivenessSDK.init(appContext)
                }
                if (ret == FaceLivenessSDK.SDK_SUCCESS) {
                    engineReady = true
                }
                completion(ret)
            } catch (_: Throwable) {
                completion(FaceLivenessSDK.SDK_INIT_FAILED)
            }
        }
    }

    fun getLicenseStatus(): LicenseStatus = LicenseStatus.current()

    fun allowsLiveness(): Boolean = FaceLivenessSDK.allowsLiveness()

    fun faceDetection(bitmap: Bitmap, param: FaceDetectionParam? = null): List<FaceBox> =
        FaceLivenessQueue.faceDetection(bitmap, param)

    fun makeTrackingConfig(matchThreshold: Float): FaceLivenessSDK.VideoWorkerConfig {
        val config = FaceLivenessSDK.VideoWorkerConfig.withMatchThreshold(matchThreshold)
        val al = FaceLivenessSDK.ActiveLivenessConfig.defaults()
        al.enabled = false
        config.activeLiveness = al
        return config
    }

    fun startVideoWorker(config: FaceLivenessSDK.VideoWorkerConfig): Int =
        FaceLivenessQueue.startVideoWorker(config)

    fun stopVideoWorker() {
        FaceLivenessQueue.stopVideoWorker()
    }

    fun setVideoWorkerEventHandler(handler: java.util.function.Consumer<String>?) {
        FaceLivenessQueue.setVideoWorkerEventHandler(
            if (handler == null) null else { json -> handler.accept(json) },
        )
    }

    fun syncDatabase(matchThreshold: Float): Int =
        FaceLivenessQueue.syncVideoWorkerDatabase(emptyList(), matchThreshold)

    fun addFrame(bitmap: Bitmap): Int = FaceLivenessQueue.addVideoWorkerFrame(bitmap)

    fun async(work: Runnable) {
        FaceLivenessQueue.async(work)
    }

    companion object {
        @Volatile
        var engineReady: Boolean = false
            private set

        @Volatile
        private var instance: FaceLivenessClient? = null

        @JvmStatic
        fun get(context: Context): FaceLivenessClient {
            return instance ?: synchronized(this) {
                instance ?: FaceLivenessClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
