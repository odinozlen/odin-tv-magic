package com.odin.tvpanel;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Fast key input. Not part of the app at runtime: the panel starts it once over ADB as
 *   CLASSPATH=<this apk> app_process / com.odin.tvpanel.KeyInjector
 * so it runs with the shell user's right to inject input, and then feeds it one Android
 * keycode per line on stdin. `input keyevent` starts a new Java VM for every key (~0.4 s);
 * this process stays warm, so a key takes a few milliseconds.
 */
public final class KeyInjector {
    private KeyInjector() {
    }

    public static void main(String[] args) throws Exception {
        Object im;
        try {
            im = Class.forName("android.hardware.input.InputManagerGlobal").getMethod("getInstance").invoke(null);  // Android 14+
        } catch (ClassNotFoundException e) {
            im = Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null);
        }
        Method inject = im.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        System.out.println("ready");
        System.out.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int code;
            try {
                code = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                continue;
            }
            if (code <= 0 || code > KeyEvent.getMaxKeyCode()) continue;
            long now = SystemClock.uptimeMillis();
            for (int action : new int[]{KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP}) {
                KeyEvent ev = new KeyEvent(now, now, action, code, 0, 0,
                        KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
                inject.invoke(im, ev, 0);  // 0 = async
            }
        }
    }
}
