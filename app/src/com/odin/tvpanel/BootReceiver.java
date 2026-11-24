package com.odin.tvpanel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the panel when the TV boots and after the app is updated. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            PanelService.start(ctx);
        }
    }
}
