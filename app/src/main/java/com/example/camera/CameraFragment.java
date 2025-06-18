package com.example.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraFragment extends Fragment {

    private TextureView textureView;
    private ImageView imageView;
    private Button btnCapture, btnRetake, btnGotoShape;
    private TextView txtResult;
    private LinearLayout cardResult;

    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private ImageReader imageReader;
    private Handler bgHandler;
    private HandlerThread bgThread;

    private static final int REQ_CAM = 200;
    private static final String ROBOFLOW_URL =
            "https://serverless.roboflow.com/faceline/2?api_key=g56k2DYQMopGLEWYqp9w";

    private final OkHttpClient http = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle st) {

        View v = inf.inflate(R.layout.fragment_camera, parent, false);
        btnGotoShape = v.findViewById(R.id.btnGotoShape);
        textureView = v.findViewById(R.id.textureView);
        imageView = v.findViewById(R.id.imageView);
        btnCapture = v.findViewById(R.id.btnCapture);
        btnRetake = v.findViewById(R.id.btnRetake);
        txtResult = v.findViewById(R.id.txtResult);
        cardResult = v.findViewById(R.id.cardResult);

        textureView.setSurfaceTextureListener(texListener);
        btnCapture.setOnClickListener(vi -> takePhoto());
        btnRetake.setOnClickListener(vi -> retake());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    public void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        bgThread = new HandlerThread("CamBg");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (bgThread != null) {
            bgThread.quitSafely();
            try {
                bgThread.join();
                bgThread = null;
                bgHandler = null;
            } catch (InterruptedException ignored) {}
        }
    }

    private final TextureView.SurfaceTextureListener texListener = new TextureView.SurfaceTextureListener() {
        @Override public void onSurfaceTextureAvailable(@NonNull SurfaceTexture st, int w, int h) {
            if (hasCamPerm()) openCamera();
            else reqCamPerm();
        }
        @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture st, int w, int h) {}
        @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture st) { return true; }
        @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture st) {}
    };

    private boolean hasCamPerm() {
        return ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void reqCamPerm() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] r) {
        if (code == REQ_CAM && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED)
            if (textureView.isAvailable()) openCamera();
    }

    private void openCamera() {
        try {
            CameraManager mgr = requireActivity().getSystemService(CameraManager.class);
            String camId = mgr.getCameraIdList()[0];

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image img = null;
                try {
                    img = reader.acquireLatestImage();
                    if (img == null) return;
                    ByteBuffer buf = img.getPlanes()[0].getBuffer();
                    byte[] data = new byte[buf.remaining()];
                    buf.get(data);
                    processImage(data);
                } finally {
                    if (img != null) img.close();
                }
            }, bgHandler);

            if (!hasCamPerm()) return;
            mgr.openCamera(camId, camCB, bgHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback camCB = new CameraDevice.StateCallback() {
        @Override public void onOpened(@NonNull CameraDevice cam) { cameraDevice = cam; startPreview(); }
        @Override public void onDisconnected(@NonNull CameraDevice cam) { cam.close(); }
        @Override public void onError(@NonNull CameraDevice cam, int err) { cam.close(); }
    };

    private void startPreview() {
        try {
            SurfaceTexture st = textureView.getSurfaceTexture();
            st.setDefaultBufferSize(640, 480);
            Surface surf = new Surface(st);

            CaptureRequest.Builder pb = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            pb.addTarget(surf);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surf, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(@NonNull CameraCaptureSession s) {
                            session = s;
                            try { s.setRepeatingRequest(pb.build(), null, bgHandler); }
                            catch (CameraAccessException e) { e.printStackTrace(); }
                        }
                        @Override public void onConfigureFailed(@NonNull CameraCaptureSession s) {}
                    }, bgHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takePhoto() {
        try {
            CaptureRequest.Builder cb = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            cb.addTarget(imageReader.getSurface());
            cb.set(CaptureRequest.JPEG_ORIENTATION, 90);
            session.capture(cb.build(), null, bgHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processImage(byte[] jpegBytes) {
        Bitmap bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        Matrix m = new Matrix();
        m.postRotate(90);
        Bitmap rot = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        rot.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] finalBytes = baos.toByteArray();

        requireActivity().runOnUiThread(() -> {
            imageView.setImageBitmap(rot);
            imageView.setVisibility(View.VISIBLE);
            btnRetake.setVisibility(View.VISIBLE);
            btnCapture.setVisibility(View.GONE);
            textureView.setVisibility(View.GONE);
            cardResult.setVisibility(View.GONE);
        });

        sendToRoboflow(finalBytes);
    }

    private void sendToRoboflow(byte[] jpegBytes) {
        String b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);

        Request req = new Request.Builder()
                .url(ROBOFLOW_URL + "&name=capture.jpg")
                .post(RequestBody.create(b64, MediaType.get("application/x-www-form-urlencoded")))
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to call Roboflow", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response res) throws IOException {
                String resp = res.body() != null ? res.body().string() : "empty";
                Log.d("ROBOFLOW", resp);

                requireActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(resp);
                        JSONArray preds = json.getJSONArray("predictions");
                        if (preds.length() > 0) {
                            String className = preds.getJSONObject(0).getString("class");
                            double confidence = preds.getJSONObject(0).getDouble("confidence");
                            txtResult.setText("Face Shape: " + className + "\nConfidence: " + String.format("%.2f", confidence));

                            // tampilkan tombol ke fragment shape
                            btnGotoShape.setVisibility(View.VISIBLE);
                            btnGotoShape.setText("See recommendation for " + className);
                            btnGotoShape.setOnClickListener(view -> goToPage(className));
                        } else {
                            txtResult.setText("⚠️ No face shape detected.");
                        }
                    } catch (Exception e) {
                        txtResult.setText("Failed to parse result.");
                        e.printStackTrace();
                    }
                    cardResult.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void retake() {
        imageView.setVisibility(View.GONE);
        btnRetake.setVisibility(View.GONE);
        btnCapture.setVisibility(View.VISIBLE);
        textureView.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        startPreview();
    }

    private void goToPage(String shape) {
        Fragment fragment = null;
        switch (shape.toLowerCase()) {
            case "square":
                fragment = new SquareFragment();
                break;
            case "egg":
//                fragment = new EggFragment();
                break;
        }

        if (fragment != null) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(getContext(), "No page for shape: " + shape, Toast.LENGTH_SHORT).show();
        }
    }
}
