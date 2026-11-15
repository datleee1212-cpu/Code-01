package com.code02.autofarm;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.TimeUnit;

/** Đọc số trên màn hình bằng ML Kit (chạy offline trên máy). Chỉ gọi từ luồng nền. */
final class Ocr {

    private static TextRecognizer recognizer;

    private Ocr() {
    }

    private static synchronized TextRecognizer get() {
        if (recognizer == null) {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
        return recognizer;
    }

    /** Đọc số nguyên trong vùng r của ảnh màn hình. Trả về -1 nếu không đọc được. */
    static long readNumber(Bitmap screen, Rect r) {
        Rect c = new Rect(r);
        if (!c.intersect(0, 0, screen.getWidth(), screen.getHeight()) || c.width() < 4 || c.height() < 4) {
            return -1;
        }
        Bitmap crop = Bitmap.createBitmap(screen, c.left, c.top, c.width(), c.height());
        int scale = c.height() < 60 ? 3 : 2;
        Bitmap up = Bitmap.createScaledBitmap(crop, c.width() * scale, c.height() * scale, true);
        long v = recognize(up);
        if (v < 0) v = recognize(binarize(up));
        return v;
    }

    private static long recognize(Bitmap b) {
        try {
            Text t = Tasks.await(get().process(InputImage.fromBitmap(b, 0)), 5, TimeUnit.SECONDS);
            return parse(t.getText());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } catch (Exception e) {
            return -1;
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

    static long parse(String s) {
        if (s == null) return -1;
        StringBuilder d = new StringBuilder();
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case 'O': case 'o': case 'D': case 'Q':
                    d.append('0');
                    break;
                case 'I': case 'l': case 'i': case '|': case '!':
                    d.append('1');
                    break;
                case 'Z': case 'z':
                    d.append('2');
                    break;
                case 'S': case 's':
                    d.append('5');
                    break;
                case 'G': case 'b':
                    d.append('6');
                    break;
                case 'B':
                    d.append('8');
                    break;
                case 'g': case 'q':
                    d.append('9');
                    break;
                default:
                    if (ch >= '0' && ch <= '9') d.append(ch);
            }
        }
        if (d.length() == 0 || d.length() > 10) return -1;
        return Long.parseLong(d.toString());
    }
}
