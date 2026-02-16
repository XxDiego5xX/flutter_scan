package com.chavesgu.scan;

import android.app.Activity;
import android.content.Context;

import java.util.Map;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class ScanViewFactory extends PlatformViewFactory {

    @NonNull private final BinaryMessenger messenger;
    @NonNull private final Activity activity;
    @NonNull private final ActivityPluginBinding activityPluginBinding;

    ScanViewFactory(
            @NonNull BinaryMessenger messenger,
            @NonNull Activity activity,
            @NonNull ActivityPluginBinding activityPluginBinding
    ) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
        this.activity = activity;
        this.activityPluginBinding = activityPluginBinding;
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {

        Map<String, Object> creationParams = null;

        if (args instanceof Map) {
            creationParams = (Map<String, Object>) args;
        }

        return new ScanPlatformView(
                messenger,
                context, // usar el context correcto aquí
                activity,
                activityPluginBinding,
                viewId,
                creationParams
        );
    }
}
