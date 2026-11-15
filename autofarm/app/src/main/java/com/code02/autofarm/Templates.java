package com.code02.autofarm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Lưu và nạp hình mẫu (ảnh PNG cắt từ màn hình khi bạn dạy). */
final class Templates {

    private static final Map<String, Vision.Tpl> cache = new HashMap<>();

    private Templates() {
    }

    private static File file(Context c, String key) {
        File dir = new File(c.getFilesDir(), "templates");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, key + ".png");
    }

    static boolean has(Context c, String key) {
        return file(c, key).exists();
    }

    /** Cắt hình vuông cạnh side quanh (cx, cy) từ ảnh màn hình và lưu lại. */
    static synchronized boolean save(Context c, String key, Bitmap screen, int cx, int cy, int side) {
        int w = screen.getWidth();
        int h = screen.getHeight();
        side = Math.max(16, Math.min(side, Math.min(w, h)));
        int x0 = Math.max(0, Math.min(w - side, cx - side / 2));
        int y0 = Math.max(0, Math.min(h - side, cy - side / 2));
        Bitmap crop = Bitmap.createBitmap(screen, x0, y0, side, side);
        try (FileOutputStream out = new FileOutputStream(file(c, key))) {
            crop.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            return false;
        }
        cache.remove(key);
        return true;
    }

    static synchronized Vision.Tpl get(Context c, String key) {
        Vision.Tpl t = cache.get(key);
        if (t != null) return t;
        File f = file(c, key);
        if (!f.exists()) return null;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
        if (b == null) return null;
        t = new Vision.Tpl(b);
        cache.put(key, t);
        return t;
    }

    static synchronized void delete(Context c, String key) {
        //noinspection ResultOfMethodCallIgnored
        file(c, key).delete();
        cache.remove(key);
    }
}
