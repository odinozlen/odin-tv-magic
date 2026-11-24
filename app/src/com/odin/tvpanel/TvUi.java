package com.odin.tvpanel;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * For settings Philips does not expose through its API: read the TV menu from the screen
 * (uiautomator) and navigate it with key presses (several keys in one `input keyevent` call).
 */
final class TvUi {
    static final class Node {
        String text, cls;
        boolean checkable, checked, selected, enabled;
    }

    private final AdbClient adb;

    TvUi(AdbClient adb) {
        this.adb = adb;
    }

    List<Node> dump() throws Exception {
        String xml = adb.shell("uiautomator dump /sdcard/odin-ui.xml >/dev/null 2>&1; cat /sdcard/odin-ui.xml; rm -f /sdcard/odin-ui.xml");
        List<Node> nodes = new ArrayList<>();
        int start = xml.indexOf("<?xml");
        if (start < 0) return nodes;
        XmlPullParser x = Xml.newPullParser();
        x.setInput(new StringReader(xml.substring(start)));
        for (int ev = x.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = x.next()) {
            if (ev != XmlPullParser.START_TAG || !"node".equals(x.getName())) continue;
            Node n = new Node();
            n.text = attr(x, "text");
            n.cls = attr(x, "class");
            n.checkable = "true".equals(attr(x, "checkable"));
            n.checked = "true".equals(attr(x, "checked"));
            n.selected = "true".equals(attr(x, "selected"));
            n.enabled = "true".equals(attr(x, "enabled"));
            nodes.add(n);
        }
        return nodes;
    }

    private static String attr(XmlPullParser x, String name) {
        String v = x.getAttributeValue(null, name);
        return v == null ? "" : v;
    }

    void keys(String key, int times) throws Exception {
        if (times <= 0) return;
        StringBuilder b = new StringBuilder("input keyevent");
        for (int i = 0; i < times; i++) b.append(' ').append(key);
        adb.shell(b.toString());
    }

    void key(String key) throws Exception {
        keys(key, 1);
    }

    String foregroundActivity() throws Exception {
        String out = adb.shell("dumpsys activity activities | grep -m1 mResumedActivity");
        for (String tok : out.split("\\s+")) {
            if (tok.contains("/")) return tok;
        }
        return "";
    }
}
