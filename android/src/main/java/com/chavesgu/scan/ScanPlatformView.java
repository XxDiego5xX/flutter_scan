package com.chavesgu.scan;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

public class ScanPlatformView implements
        PlatformView,
        MethodChannel.MethodCallHandler,
        ScanViewNew.CaptureListener {

    private MethodChannel channel;
    private final Context context;
    private final Activity activity;
    private final ActivityPluginBinding activityPluginBinding;

    private ParentView parentView;
    private ScanViewNew scanViewNew;
    private ScanDrawView scanDrawView;

    private boolean flashlight = false;

    ScanPlatformView(
            @NonNull BinaryMessenger messenger,
            @NonNull Context context,
            @NonNull Activity activity,
            @NonNull ActivityPluginBinding activityPluginBinding,
            int viewId,
            @Nullable Map<String, Object> args
    ) {

        this.context = context;
        this.activity = activity;
        this.activityPluginBinding = activityPluginBinding;

        channel = new MethodChannel(
                messenger,
                "chavesgu/scan/method_" + viewId
        );
        channel.setMethodCallHandler(this);

        checkCameraPermission();

        initForBinding(args);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            Log.e("ScanPlugin", "Camera permission not granted. Handle it in Flutter.");
        }
    }

    private void initForBinding(@Nullable Map<String, Object> args) {

        scanViewNew = new ScanViewNew(
                context,
                activity,
                activityPluginBinding,
                args
        );

        scanViewNew.setCaptureListener(this);

        scanDrawView = new ScanDrawView(context, activity, args);

        parentView = new ParentView(context);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        scanViewNew.setLayoutParams(params);
        scanDrawView.setLayoutParams(params);
        parentView.setLayoutParams(params);

        parentView.addView(scanViewNew);
        parentView.addView(scanDrawView);
    }

    @Override
    public View getView() {
        return parentView;
    }

    @Override
    public void dispose() {

        if (scanViewNew != null) {
            scanViewNew.dispose();
            scanViewNew = null;
        }

        if (scanDrawView != null) {
            scanDrawView.pause();
            scanDrawView = null;
        }

        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }

        parentView = null;
    }

    @Override
    public void onMethodCall(
            @NonNull MethodCall call,
            @NonNull MethodChannel.Result result
    ) {

        switch (call.method) {

            case "resume":
                resume();
                result.success(null);
                break;

            case "pause":
                pause();
                result.success(null);
                break;

            case "toggleTorchMode":
                toggleTorchMode();
                result.success(null);
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    private void resume() {
        if (scanViewNew != null) scanViewNew.resume();
        if (scanDrawView != null) scanDrawView.resume();
    }

    private void pause() {
        if (scanViewNew != null) scanViewNew.pause();
        if (scanDrawView != null) scanDrawView.pause();
    }

    private void toggleTorchMode() {
        if (scanViewNew != null) {
            flashlight = !flashlight;
            scanViewNew.toggleTorchMode(flashlight);
        }
    }

    @Override
    public void onCapture(String text) {

        if (channel != null) {
            channel.invokeMethod("onCaptured", text);
        }

        pause();
    }
}
