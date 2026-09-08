package com.faceplugin.faceliveness.kit

import android.graphics.Bitmap
import com.faceplugin.facelivenessdk.FaceBox
import com.faceplugin.facelivenessdk.FaceDetectionParam
import com.faceplugin.facelivenessdk.FaceLivenessSDK
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean

/** Serial access to native FaceLivenessSDK — not safe for concurrent use. */
object FaceLivenessQueue {
    private const val THREAD = "faceliveness-sdk"

    private val executor = Executors.newSingleThreadExecutor(
        ThreadFactory { r ->
            Thread(r, THREAD).apply { isDaemon = true }
        },
    )
    private val frameBusy = AtomicBoolean(false)

    fun async(work: Runnable) {
        executor.execute(work)
    }

    fun <T> sync(work: () -> T): T {
        if (Thread.currentThread().name == THREAD) {
            return work()
        }
        val future: Future<T> = executor.submit(Callable { work() })
        return future.get()
    }

    fun faceDetection(bitmap: Bitmap, param: FaceDetectionParam?): List<FaceBox> =
        sync { FaceLivenessSDK.faceDetection(bitmap, param) }

    fun startVideoWorker(config: FaceLivenessSDK.VideoWorkerConfig): Int =
        sync {
            FaceLivenessSDK.stopVideoWorker()
            FaceLivenessSDK.startVideoWorker(config)
        }

    fun stopVideoWorker() {
        FaceLivenessSDK.setVideoWorkerEventHandler(null)
        sync {
            FaceLivenessSDK.stopVideoWorker()
            frameBusy.set(false)
        }
    }

    fun setVideoWorkerEventHandler(handler: ((String) -> Unit)?) {
        if (handler == null) {
            FaceLivenessSDK.setVideoWorkerEventHandler(null)
        } else {
            FaceLivenessSDK.setVideoWorkerEventHandler { json -> handler(json) }
        }
    }

    fun syncVideoWorkerDatabase(features: List<ByteArray>, matchThreshold: Float): Int =
        sync { FaceLivenessSDK.syncVideoWorkerDatabase(features, matchThreshold) }

    fun addVideoWorkerFrame(bitmap: Bitmap): Int {
        if (Thread.currentThread().name == THREAD) {
            return FaceLivenessSDK.addVideoWorkerFrame(bitmap)
        }
        if (!frameBusy.compareAndSet(false, true)) return 0
        val copy = try {
            if (bitmap.isRecycled) null else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: Throwable) {
            null
        }
        if (copy == null) {
            frameBusy.set(false)
            return -1
        }
        async {
            try {
                FaceLivenessSDK.addVideoWorkerFrame(copy)
            } finally {
                if (!copy.isRecycled) copy.recycle()
                frameBusy.set(false)
            }
        }
        return 0
    }
}
