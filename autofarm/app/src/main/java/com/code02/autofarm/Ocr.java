package com.code02.autofarm;

import android.graphics.Bitmap;

import com.code02.autofarm.core.Box;
import com.code02.autofarm.core.Img;
import com.code02.autofarm.core.Parse;
import com.code02.autofarm.core.TextReader;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.TimeUnit;

/** Đọc chữ trên màn hình bằng ML Kit (chạy offline trên máy). Chỉ gọi từ luồng nền. */
final class Ocr implements TextReader {

    private TextRecognizer recognizer;

    private synchronized TextRecognizer get() {
        if (recognizer == null) {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
        return recognizer;
    }

    @Override
    public String read(Img img, Box r, boolean digits) {
        if (img == null || r == null) return null;
        int l = Math.max(0, r.l);
        int t = Math.max(0, r.t);
        int w = Math.min(img.w, r.r) - l;
        int h = Math.min(img.h, r.b) - t;
        if (w < 4 || h < 4) return null;
        Bitmap crop = Templates.toBitmap(img.crop(l, t, w, h));
        // Màn hình nét (vd Galaxy Tab S10+) chữ đã đủ to: phóng ít hoặc không phóng để đọc nhanh hơn.
        int scale = h < 32 ? 3 : h < 64 ? 2 : 1;
        Bitmap up = scale == 1 ? crop : Bitmap.createScaledBitmap(crop, w * scale, h * scale, true);
        String s = recognize(up);
        if (digits && Parse.number(s) < 0) s = recognize(binarize(up));
        return s;
    }

    private String recognize(Bitmap b) {
        try {
            Text t = Tasks.await(get().process(InputImage.fromBitmap(b, 0)), 5, TimeUnit.SECONDS);
            String s = t.getText();
            return s == null || s.trim().isEmpty() ? null : s;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Chữ số trong game có viền đen: giữ phần sáng thành chữ đen trên nền trắng. */
    private static Bitmap binarize(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int lum = (((c >> 16) & 255) * 77 + ((c >> 8) & 255) * 150 + (c & 255) * 29) >> 8;
            px[i] = lum >= 160 ? 0xFF000000 : 0xFFFFFFFF;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }
}
