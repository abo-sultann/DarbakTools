package com.abosultan.darbaktools;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";

    private static volatile byte[] latestFrame;
    private static volatile boolean running;
    private static volatile int captureWidth = 1024;
    private static volatile int captureHeight = 600;
    private static volatile long lastFrameAt;

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread captureThread;
    private Handler captureHandler;

    public static boolean isRunning() {
        return running;
    }

    public static byte[] getLatestFrame() {
        return latestFrame;
    }

    public static int getCaptureWidth() {
        return captureWidth;
    }

    public static int getCaptureHeight() {
        return captureHeight;
    }

    public static long getLastFrameAt() {
        return lastFrameAt;
    }

    public static void startCapture(Context context, int resultCode, Intent resultData) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_RESULT_DATA, resultData);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && projection == null) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            if (resultCode != 0 && resultData != null) startProjection(resultCode, resultData);
        }
        return START_STICKY;
    }

    private void startProjection(int resultCode, Intent resultData) {
        try {
            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) return;
            projection = manager.getMediaProjection(resultCode, resultData);
            if (projection == null) return;

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) wm.getDefaultDisplay().getRealMetrics(metrics);
            int width = metrics.widthPixels > 0 ? metrics.widthPixels : 1024;
            int height = metrics.heightPixels > 0 ? metrics.heightPixels : 600;
            if (width < height) {
                int t = width;
                width = height;
                height = t;
            }
            captureWidth = width;
            captureHeight = height;
            int density = metrics.densityDpi > 0 ? metrics.densityDpi : 160;

            captureThread = new HandlerThread("DarbakScreenCapture");
            captureThread.start();
            captureHandler = new Handler(captureThread.getLooper());
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override public void onImageAvailable(ImageReader reader) {
                    consumeFrame(reader);
                }
            }, captureHandler);

            virtualDisplay = projection.createVirtualDisplay(
                    "DarbakRemoteScreen",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    captureHandler
            );
            running = virtualDisplay != null;
        } catch (Exception ignored) {
            running = false;
        }
    }

    private void consumeFrame(ImageReader reader) {
        long now = System.currentTimeMillis();
        if (now - lastFrameAt < 450) {
            Image skip = null;
            try { skip = reader.acquireLatestImage(); } catch (Exception ignored) {}
            if (skip != null) skip.close();
            return;
        }

        Image image = null;
        Bitmap padded = null;
        Bitmap cropped = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null) return;
            Image.Plane[] planes = image.getPlanes();
            if (planes == null || planes.length == 0) return;
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * captureWidth;
            int paddedWidth = captureWidth + Math.max(0, rowPadding / Math.max(1, pixelStride));

            padded = Bitmap.createBitmap(paddedWidth, captureHeight, Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(buffer);
            cropped = Bitmap.createBitmap(padded, 0, 0, captureWidth, captureHeight);

            ByteArrayOutputStream out = new ByteArrayOutputStream(180 * 1024);
            cropped.compress(Bitmap.CompressFormat.JPEG, 45, out);
            latestFrame = out.toByteArray();
            lastFrameAt = now;
            out.close();
        } catch (Exception ignored) {
        } finally {
            if (cropped != null && cropped != padded) cropped.recycle();
            if (padded != null) padded.recycle();
            if (image != null) image.close();
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        latestFrame = null;
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (projection != null) {
            projection.stop();
            projection = null;
        }
        if (captureThread != null) {
            captureThread.quitSafely();
            captureThread = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
