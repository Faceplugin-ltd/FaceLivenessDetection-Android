package com.faceplugin.faceliveness.kit

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Process-wide engine owner: activation and init on a background thread, a lock that serializes
 * every SDK call, and a small state observable for the UI.
 *
 * The engine is [FaceRecognitionSDK] from `libs/facerecognitionsdk.aar`, built by
 * Face-Recognition-LivenessDetection-Android-LX-SDK. It is not concurrent, so every call made
 * from this app goes through [lock], and long calls belong on [worker].
 */
object FaceEngine {

    enum class State { LOADING, READY, NOT_ACTIVATED, LICENSE_INVALID, LICENSE_EXPIRED, FAILED }

    /** Every SDK call takes this lock — camera analyzer included. */
    val lock = Any()

    /** Background worker for activation, init and still-image work. */
    val worker: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "faceliveness-engine").apply { isDaemon = true }
    }

    @Volatile
    var state: State = State.LOADING
        private set

    private val listeners = CopyOnWriteArrayList<(State) -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    val isReady: Boolean get() = state == State.READY

    fun addListener(listener: (State) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (State) -> Unit) {
        listeners -= listener
    }

    /**
     * Activate with [license], then init. Call once per process; the work runs on [worker] and the
     * result arrives on [onDone] (main thread) and through [addListener].
     */
    @JvmStatic
    @JvmOverloads
    fun start(context: Context, license: String, onDone: ((Int) -> Unit)? = null) {
        val app = context.applicationContext
        if (isReady) {
            onDone?.let { cb -> main.post { cb(FaceRecognitionSDK.SDK_SUCCESS) } }
            return
        }
        publish(State.LOADING)
        worker.execute {
            val code = synchronized(lock) {
                try {
                    FaceRecognitionSDK.machineCode(app)
                    var ret = FaceRecognitionSDK.activate(app, license)
                    if (ret == FaceRecognitionSDK.SDK_SUCCESS) {
                        ret = FaceRecognitionSDK.init(app)
                    }
                    ret
                } catch (_: Throwable) {
                    FaceRecognitionSDK.SDK_INIT_FAILED
                }
            }
            publish(
                when (code) {
                    FaceRecognitionSDK.SDK_SUCCESS -> State.READY
                    FaceRecognitionSDK.SDK_NOT_ACTIVATED -> State.NOT_ACTIVATED
                    FaceRecognitionSDK.SDK_LICENSE_INVALID -> State.LICENSE_INVALID
                    FaceRecognitionSDK.SDK_LICENSE_EXPIRED -> State.LICENSE_EXPIRED
                    else -> State.FAILED
                },
            )
            onDone?.let { cb -> main.post { cb(code) } }
        }
    }

    /** Queue [work] on the engine thread. */
    fun async(work: Runnable) {
        worker.execute(work)
    }

    fun licenseStatus(): LicenseStatus = try {
        synchronized(lock) { LicenseStatus.from(FaceRecognitionSDK.license()) }
    } catch (_: Throwable) {
        LicenseStatus.notLicensed()
    }

    fun allowsLiveness(): Boolean = try {
        synchronized(lock) { FaceRecognitionSDK.allowsLiveness() }
    } catch (_: Throwable) {
        false
    }

    fun lastLicenseError(): String = try {
        FaceRecognitionSDK.lastLicenseError().orEmpty()
    } catch (_: Throwable) {
        ""
    }

    /**
     * Every face in [bitmap] with the attributes [options] asks for, or an empty list when the
     * engine is not ready. Runs on the calling thread — use [worker].
     */
    fun detect(bitmap: Bitmap, options: DetectOptions): List<DetectedFace> {
        if (!isReady) return emptyList()
        return synchronized(lock) {
            try {
                FaceRecognitionSDK.detect(bitmap, options.toSdk()).map { DetectedFace.from(it) }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    private fun publish(next: State) {
        state = next
        main.post { listeners.forEach { it(next) } }
    }
}
