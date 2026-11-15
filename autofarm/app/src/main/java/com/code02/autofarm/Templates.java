package com.code02.autofarm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.code02.autofarm.core.Img;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Lưu và nạp hình mẫu (ảnh PNG cắt từ màn hình khi bạn dạy). */
final class Templates {

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

    static Bitmap toBitmap(Img i) {
        return Bitmap.createBitmap(i.px, i.w, i.h, Bitmap.Config.ARGB_8888);
    }

    static Img toImg(Bitmap b) {
        int w = b.getWidth();
        int h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        return new Img(w, h, px);
    }

    /** Cắt hình vuông cạnh side quanh (cx, cy) từ ảnh màn hình và lưu lại. */
    static synchronized boolean save(Context c, String key, Img screen, int cx, int cy, int side) {
        Img crop = screen.cropSquare(cx, cy, side);
        try (FileOutputStream out = new FileOutputStream(file(c, key))) {
            return toBitmap(crop).compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            return false;
        }
    }

    static synchronized Img load(Context c, String key) {
        File f = file(c, key);
        if (!f.exists()) return null;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
        return b == null ? null : toImg(b);
    }

    static synchronized void delete(Context c, String key) {
        //noinspection ResultOfMethodCallIgnored
        file(c, key).delete();
    }
}
