package com.faceplugin.faceliveness.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import com.faceplugin.faceliveness.R;
import com.faceplugin.faceliveness.kit.CameraFrameUtils;
import com.faceplugin.faceliveness.kit.CameraPreview;
import com.faceplugin.faceliveness.kit.FaceJson;
import com.faceplugin.faceliveness.kit.FaceLivenessClient;
import com.faceplugin.faceliveness.kit.LiveDetect;
import com.faceplugin.faceliveness.kit.VideoWorkerEvent;
import com.faceplugin.faceliveness.kit.VideoWorkerFace;
import com.faceplugin.facelivenessdk.FaceBox;
import com.faceplugin.facelivenessdk.FaceLivenessSDK;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends AppCompatActivity {

    private ExecutorService cameraExecutorService;
    private PreviewView viewFinder;
    private FaceView faceView;
    private TextView txtCameraHint;
    private Context context;
    private final AtomicBoolean pbBusy = new AtomicBoolean(false);
    private volatile boolean videoWorkerReady = false;
    private ProcessCameraProvider cameraProvider = null;
    private Bitmap lastFrame = null;
    private int lastFrameW;
    private int lastFrameH;
    private volatile List<FaceBox> lastLivenessBoxes = java.util.Collections.emptyList();
    private volatile boolean hasFaces = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this;
        viewFinder = findViewById(R.id.preview);
        viewFinder.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        viewFinder.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        faceView = findViewById(R.id.faceView);
        txtCameraHint = findViewById(R.id.txtCameraHint);
        cameraExecutorService = Executors.newFixedThreadPool(1);
        faceView.setMirrorX(SettingsActivity.getCameraLens(this) == CameraSelector.LENS_FACING_FRONT);

        findViewById(R.id.btnCloseCamera).setOnClickListener(v -> finish());
        findViewById(R.id.btnSwitchCamera).setOnClickListener(v -> toggleCamera());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            viewFinder.post(this::setUpCamera);
        }
        updateHint();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopVideoWorker();
        lastLivenessBoxes = java.util.Collections.emptyList();
        hasFaces = false;
        faceView.setFaceBoxes(null);
        updateHint();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoWorkerReady = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        FaceLivenessClient.get(this).stopVideoWorker();
        cameraExecutorService.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            viewFinder.post(this::setUpCamera);
        }
    }

    private void toggleCamera() {
        int current = SettingsActivity.getCameraLens(this);
        String next = current == CameraSelector.LENS_FACING_BACK ? "front" : "back";
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("camera_lens", next)
                .apply();
        faceView.setMirrorX("front".equals(next));
        stopVideoWorker();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        lastLivenessBoxes = java.util.Collections.emptyList();
        hasFaces = false;
        faceView.setFaceBoxes(null);
        updateHint();
        viewFinder.post(this::setUpCamera);
    }

    private void startVideoWorker() {
        FaceLivenessClient client = FaceLivenessClient.get(this);
        client.setVideoWorkerEventHandler(this::onVideoWorkerEvent);
        float threshold = SettingsActivity.getLivenessThreshold(this);
        client.async(() -> {
            FaceLivenessSDK.VideoWorkerConfig config = client.makeTrackingConfig(threshold);
            int started = client.startVideoWorker(config);
            if (started == 0) {
                client.syncDatabase(threshold);
            }
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                client.stopVideoWorker();
                return;
            }
            videoWorkerReady = started == 0;
        });
    }

    private void stopVideoWorker() {
        videoWorkerReady = false;
        FaceLivenessClient.get(this).stopVideoWorker();
    }

    private void onVideoWorkerEvent(String json) {
        VideoWorkerEvent event = FaceJson.parseVideoWorkerEvent(json);
        if (event == null) return;
        if (event instanceof VideoWorkerEvent.Tracking) {
            VideoWorkerEvent.Tracking tracking = (VideoWorkerEvent.Tracking) event;
            List<VideoWorkerFace> faces = tracking.getFaces();
            // Include weak tracks so every detected face is shown.
            List<FaceBox> boxes = FaceJson.toFaceBoxes(faces, true);
            LiveDetect.mergePbDetect(boxes, lastLivenessBoxes);
            ensureScores(boxes);
            hasFaces = !boxes.isEmpty();
            int frameW = lastFrameW > 0 ? lastFrameW : (int) tracking.getFrameWidth();
            int frameH = lastFrameH > 0 ? lastFrameH : (int) tracking.getFrameHeight();
            runOnUiThread(() -> {
                if (frameW > 0 && frameH > 0) {
                    faceView.setFrameSize(new Size(frameW, frameH));
                }
                faceView.setFaceBoxes(boxes);
                updateHint();
            });
        }
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception ignored) {
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        try {
            CameraPreview.bind(
                    this,
                    cameraProvider,
                    viewFinder,
                    SettingsActivity.getCameraLens(this),
                    cameraExecutorService,
                    this::analyzeImage);
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(androidx.camera.core.ImageProxy imageProxy) {
        Bitmap frame = null;
        try {
            if (!FaceLivenessClient.get(this).isEngineReady()) return;
            boolean backCamera = SettingsActivity.getCameraLens(context) == CameraSelector.LENS_FACING_BACK;
            frame = CameraFrameUtils.fromImageProxy(imageProxy, backCamera);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
        if (frame == null || !FaceLivenessClient.get(this).isEngineReady()) return;
        synchronized (this) {
            if (lastFrame != null && lastFrame != frame && !lastFrame.isRecycled()) {
                lastFrame.recycle();
            }
            lastFrame = frame;
            lastFrameW = frame.getWidth();
            lastFrameH = frame.getHeight();
        }
        if (videoWorkerReady) {
            FaceLivenessClient.get(this).addFrame(frame);
        }
        requestLiveness(frame);
    }

    private void requestLiveness(Bitmap frame) {
        if (!pbBusy.compareAndSet(false, true)) return;
        final Bitmap copy;
        try {
            copy = CameraFrameUtils.copyArgb(frame);
        } catch (Exception e) {
            pbBusy.set(false);
            return;
        }
        FaceLivenessClient client = FaceLivenessClient.get(this);
        int level = SettingsActivity.getLivenessLevel(this);
        client.async(() -> {
            try {
                List<FaceBox> pb = client.faceDetection(copy, LiveDetect.livenessOnly(level));
                assignDetectIds(pb);
                ensureScores(pb);
                lastLivenessBoxes = pb;
                if (!videoWorkerReady) {
                    final int frameW = copy.getWidth();
                    final int frameH = copy.getHeight();
                    hasFaces = !pb.isEmpty();
                    runOnUiThread(() -> {
                        faceView.setFrameSize(new Size(frameW, frameH));
                        faceView.setFaceBoxes(new ArrayList<>(pb));
                        updateHint();
                    });
                }
            } catch (Exception ignored) {
            } finally {
                if (!copy.isRecycled()) copy.recycle();
                pbBusy.set(false);
                runOnUiThread(this::updateHint);
            }
        });
    }

    /** Give every PB face a stable TrackID when VideoWorker is not providing one. */
    private static void assignDetectIds(List<FaceBox> boxes) {
        if (boxes == null) return;
        for (int i = 0; i < boxes.size(); i++) {
            FaceBox box = boxes.get(i);
            if (box.trackId < 0) {
                box.trackId = i + 1;
            }
        }
    }

    private static void ensureScores(List<FaceBox> boxes) {
        if (boxes == null) return;
        for (FaceBox box : boxes) {
            if (box.score < 0f && box.face_quality > 0.001f) {
                box.score = box.face_quality;
            }
            if (box.score < 0f && box.liveness > 0.001f) {
                box.score = box.liveness;
            }
        }
    }

    private void updateHint() {
        if (hasFaces) {
            txtCameraHint.setText(R.string.liveness_status_analyzing);
        } else {
            txtCameraHint.setText(R.string.camera_hint);
        }
    }
}
