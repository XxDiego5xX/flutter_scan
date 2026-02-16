package com.chavesgu.scan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

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
  private QrCodeAsyncTask task;

  // ================= ENGINE =================

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    flutterPluginBinding = binding;
    channel = new MethodChannel(binding.getBinaryMessenger(), "chavesgu/scan");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }
    flutterPluginBinding = null;
  }

  // ================= ACTIVITY =================

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

  // ================= METHOD CHANNEL =================

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
      pendingResult = result;

      task = new QrCodeAsyncTask(this, path);
      task.execute();

      return;
    }

    result.notImplemented();
  }

  // ================= ASYNC TASK =================

  static class QrCodeAsyncTask extends AsyncTask<Void, Void, String> {

    private final WeakReference<ScanPlugin> weakReference;
    private final String path;

    QrCodeAsyncTask(ScanPlugin plugin, String path) {
      weakReference = new WeakReference<>(plugin);
      this.path = path;
    }

    @Override
    protected String doInBackground(Void... voids) {

      ScanPlugin plugin = weakReference.get();
      if (plugin == null) return null;

      Bitmap bitmap = BitmapFactory.decodeFile(path);
      if (bitmap == null) return null;

      return QRCodeDecoder.syncDecodeQRCode(bitmap);
    }

    @Override
    protected void onPostExecute(String result) {

      ScanPlugin plugin = weakReference.get();
      if (plugin == null) return;

      if (plugin.pendingResult != null) {
        plugin.pendingResult.success(result);
        plugin.pendingResult = null;
      }

      if (plugin.task != null) {
        plugin.task.cancel(true);
        plugin.task = null;
      }

      if (result != null) {
        Vibrator vibrator = (Vibrator)
                plugin.flutterPluginBinding
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
    }
  }
}
