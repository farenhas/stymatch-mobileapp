package com.example.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraFragment extends Fragment {

    private TextureView textureView;
    private ImageView imageView;
    private Button btnCapture, btnRetake;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private ImageReader imageReader;

    private final int REQUEST_CAMERA = 200;

    public CameraFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        textureView  = view.findViewById(R.id.textureView);
        imageView    = view.findViewById(R.id.imageView);
        btnCapture   = view.findViewById(R.id.btnCapture);
        btnRetake    = view.findViewById(R.id.btnRetake);

        textureView.setSurfaceTextureListener(surfaceListener);

        btnCapture.setOnClickListener(v -> takePhoto());
        btnRetake.setOnClickListener(v -> retakePhoto());

        return view;
    }

    private final TextureView.SurfaceTextureListener surfaceListener =
            new TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                    if (checkCameraPermissions()) openCamera();
                    else requestCameraPermissions();
                }
                @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}
                @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
                @Override public void onSurfaceTextureUpdated(SurfaceTexture st) {}
            };

    private boolean checkCameraPermissions() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermissions() {
        requestPermissions(
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CAMERA);
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(getContext().CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                image.close();
                saveAndShowImage(bytes);
            }, null);

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                return;

            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
        }
        @Override public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }
        @Override public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
        }
    };

    private void startPreview() {
        try {
            SurfaceTexture st = textureView.getSurfaceTexture();
            Surface surface = new Surface(st);

            CaptureRequest.Builder previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(@NonNull CameraCaptureSession camSession) {
                            session = camSession;
                            try {
                                session.setRepeatingRequest(previewBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override public void onConfigureFailed(@NonNull CameraCaptureSession camSession) {}
                    }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePhoto() {
        try {
            CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            session.capture(captureBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void saveAndShowImage(byte[] bytes) {
        File dir = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera/");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "IMG_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        getActivity().runOnUiThread(() -> {
            imageView.setImageBitmap(rotatedBmp);
            imageView.setVisibility(View.VISIBLE);
            btnRetake.setVisibility(View.VISIBLE);
            btnCapture.setVisibility(View.GONE);
            textureView.setVisibility(View.GONE);
        });
    }

    private void retakePhoto() {
        imageView.setVisibility(View.GONE);
        btnRetake.setVisibility(View.GONE);
        btnCapture.setVisibility(View.VISIBLE);
        textureView.setVisibility(View.VISIBLE);
        startPreview();
    }

    @Override
    public void onDestroy() {
        if (cameraDevice != null) cameraDevice.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (textureView.isAvailable()) openCamera();
        }
    }
}
