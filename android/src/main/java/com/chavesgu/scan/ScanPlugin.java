package com.chavesgu.scan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import static android.content.Context.VIBRATOR_SERVICE;

public class ScanPlugin implements FlutterPlugin,
        MethodChannel.MethodCallHandler,
        ActivityAware {

  private MethodChannel channel;
  private Activity activity;
  private FlutterPluginBinding flutterPluginBinding;

  private MethodChannel.Result pendingResult;

  private ExecutorService executor;
  private Handler mainHandler;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    flutterPluginBinding = binding;
    channel = new MethodChannel(binding.getBinaryMessenger(), "chavesgu/scan");
    channel.setMethodCallHandler(this);

    executor = Executors.newSingleThreadExecutor();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

    if (executor != null) {
      executor.shutdown();
      executor = null;
    }

    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }

    flutterPluginBinding = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();

    flutterPluginBinding.getPlatformViewRegistry()
            .registerViewFactory(
                    "chavesgu/scan_view",
                    new ScanViewFactory(
                            flutterPluginBinding.getBinaryMessenger(),
                            flutterPluginBinding.getApplicationContext(),
                            activity,
                            binding
                    )
            );
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call,
                           @NonNull MethodChannel.Result result) {

    if ("getPlatformVersion".equals(call.method)) {
      result.success("Android " + Build.VERSION.RELEASE);
      return;
    }

    if ("parse".equals(call.method)) {

      if (pendingResult != null) {
        result.error("ALREADY_RUNNING", "QR decoding already in progress", null);
        return;
      }

      String path = call.arguments();

      if (path == null || path.isEmpty()) {
        result.error("INVALID_PATH", "Image path is null or empty", null);
        return;
      }

      pendingResult = result;

      executor.execute(() -> {

        String decodeResult = null;

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
          decodeResult = QRCodeDecoder.syncDecodeQRCode(bitmap);
        }

        String finalResult = decodeResult;

        mainHandler.post(() -> {

          if (pendingResult != null) {
            pendingResult.success(finalResult); // null si no hay QR
            pendingResult = null;
          }

          // Vibrar solo si hay resultado válido
          if (finalResult != null && flutterPluginBinding != null) {

            Vibrator vibrator = (Vibrator)
                    flutterPluginBinding
                            .getApplicationContext()
                            .getSystemService(VIBRATOR_SERVICE);

            if (vibrator != null) {
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
          }

        });

      });

      return;
    }

    result.notImplemented();
  }
}
