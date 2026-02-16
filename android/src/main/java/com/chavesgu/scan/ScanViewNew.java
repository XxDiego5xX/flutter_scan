package com.chavesgu.scan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.Size;

import java.lang.ref.WeakReference;
import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

import static android.content.Context.VIBRATOR_SERVICE;
import static java.lang.Math.min;

public class ScanViewNew extends BarcodeView {

    public interface CaptureListener {
        void onCapture(String text);
    }

    private CaptureListener captureListener;
    private final Context context;
    private final Activity activity;

    private double vw;
    private double vh;
    private double scale = 0.7;

    private QrCodeAsyncTask task;

    // ✅ Constructor que ScanPlatformView necesita
    public ScanViewNew(
            Context context,
            Activity activity,
            ActivityPluginBinding binding,
            Map<String, Object> args
    ) {
        super(context);
        this.context = context;
        this.activity = activity;

        init();
    }

    // Constructor alternativo (por compatibilidad)
    public ScanViewNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.activity = null;
        init();
    }

    private void init() {

        setCameraSettings(new com.journeyapps.barcodescanner.camera.CameraSettings());

        setDecoderFactory(
                new DefaultDecoderFactory(
                        QRCodeDecoder.allFormats,
                        QRCodeDecoder.HINTS,
                        "utf-8",
                        2
                )
        );

        decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (captureListener != null && result.getText() != null) {
                    captureListener.onCapture(result.getText());
                    vibrate();
                }
            }
        });
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                    )
            );
        } else {
            vibrator.vibrate(50);
        }
    }

    // ✅ Ahora coinciden con ScanPlatformView
    public void resume() {
        super.resume();
    }

    public void pause() {
        super.pause();
    }

    public void toggleTorchMode(boolean mode) {
        setTorch(mode);
    }

    public void setCaptureListener(CaptureListener captureListener) {
        this.captureListener = captureListener;
    }

    public void dispose() {
        pause();
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resume();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        vw = getWidth();
        vh = getHeight();

        if (scale < 1.0) {
            int areaWidth = (int) (min(vw, vh) * scale);
            setFramingRectSize(new Size(areaWidth, areaWidth));
        } else {
            setFramingRectSize(new Size((int) vw, (int) vh));
        }
    }

    static class QrCodeAsyncTask extends AsyncTask<Bitmap, Integer, String> {

        private final WeakReference<ScanViewNew> weakReference;

        QrCodeAsyncTask(ScanViewNew view) {
            weakReference = new WeakReference<>(view);
        }

        @Override
        protected String doInBackground(Bitmap... params) {
            ScanViewNew view = weakReference.get();
            if (view == null) return null;

            return QRCodeDecoder.syncDecodeQRCode(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            ScanViewNew view = weakReference.get();
            if (view == null) return;

            if (view.captureListener != null && result != null) {
                view.captureListener.onCapture(result);
                view.vibrate();
            }

            if (view.task != null) {
                view.task.cancel(true);
                view.task = null;
            }
        }
    }
}
