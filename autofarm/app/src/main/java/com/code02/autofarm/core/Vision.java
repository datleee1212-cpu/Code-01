package com.code02.autofarm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nhận diện hình trên ảnh chụp màn hình: so khớp mẫu (normalized cross-correlation)
 * trên ảnh xám thu nhỏ, tìm thô ở 1/4 rồi tinh chỉnh ở 1/2 độ phân giải.
 */
public final class Vision {

    private Vision() {
    }

    /** Ảnh xám. */
    static final class Gray {
        final int w;
        final int h;
        final int[] p;

        Gray(int w, int h, int[] p) {
            this.w = w;
            this.h = h;
            this.p = p;
        }

        /** Thu nhỏ f lần (lấy trung bình khối f×f) và chuyển xám. */
        static Gray of(Img b, int f) {
            int bw = b.w;
            int bh = b.h;
            int w = Math.max(1, bw / f);
            int h = Math.max(1, bh / f);
            int[] out = new int[w * h];
            int[] src = b.px;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int s = 0;
                    int n = 0;
                    for (int j = 0; j < f && y * f + j < bh; j++) {
                        int base = (y * f + j) * bw + x * f;
                        for (int i = 0; i < f && x * f + i < bw; i++) {
                            int c = src[base + i];
                            s += (((c >> 16) & 255) * 77 + ((c >> 8) & 255) * 150 + (c & 255) * 29) >> 8;
                            n++;
                        }
                    }
                    out[y * w + x] = n == 0 ? 0 : s / n;
                }
            }
            return new Gray(w, h, out);
        }

        Gray half() {
            int w2 = Math.max(1, w / 2);
            int h2 = Math.max(1, h / 2);
            int[] out = new int[w2 * h2];
            for (int y = 0; y < h2; y++) {
                int y0 = Math.min(2 * y, h - 1);
                int y1 = Math.min(2 * y + 1, h - 1);
                for (int x = 0; x < w2; x++) {
                    int x0 = Math.min(2 * x, w - 1);
                    int x1 = Math.min(2 * x + 1, w - 1);
                    out[y * w2 + x] = (p[y0 * w + x0] + p[y0 * w + x1] + p[y1 * w + x0] + p[y1 * w + x1]) >> 2;
                }
            }
            return new Gray(w2, h2, out);
        }
    }

    /** Ảnh tích phân để tính nhanh tổng và tổng bình phương của một ô. */
    private static final class Integral {
        final long[] s;
        final long[] ss;
        final int stride;

        Integral(Gray g) {
            stride = g.w + 1;
            s = new long[stride * (g.h + 1)];
            ss = new long[stride * (g.h + 1)];
            for (int y = 0; y < g.h; y++) {
                long rs = 0;
                long rss = 0;
                for (int x = 0; x < g.w; x++) {
                    int v = g.p[y * g.w + x];
                    rs += v;
                    rss += (long) v * v;
                    int idx = (y + 1) * stride + (x + 1);
                    s[idx] = s[idx - stride] + rs;
                    ss[idx] = ss[idx - stride] + rss;
                }
            }
        }

        long sum(long[] a, int x, int y, int w, int h) {
            int x1 = x + w;
            int y1 = y + h;
            return a[y1 * stride + x1] - a[y * stride + x1] - a[y1 * stride + x] + a[y * stride + x];
        }
    }

    /** Một ảnh chụp màn hình đã chuẩn bị sẵn để tìm kiếm. */
    public static final class Frame {
        public final Img img;
        final Gray g2;
        final Gray g4;
        private final Integral i4;

        public Frame(Img b) {
            img = b;
            g2 = Gray.of(b, 2);
            g4 = g2.half();
            i4 = new Integral(g4);
        }

        public int width() {
            return img.w;
        }

        public int height() {
            return img.h;
        }
    }

    /** Hình mẫu đã dạy. */
    public static final class Tpl {
        public final int side;
        final Prep p2;
        final Prep p4;

        public Tpl(Img crop) {
            side = Math.max(crop.w, crop.h);
            Gray g2 = Gray.of(crop, 2);
            p2 = Prep.of(g2);
            p4 = Prep.of(g2.half());
        }

        public boolean usable() {
            return p2 != null && p4 != null;
        }
    }

    /** Mẫu đã trừ trung bình, sẵn sàng để tính tương quan. */
    static final class Prep {
        final int w;
        final int h;
        final float[] z;
        final double tss;

        private Prep(int w, int h, float[] z, double tss) {
            this.w = w;
            this.h = h;
            this.z = z;
            this.tss = tss;
        }

        static Prep of(Gray g) {
            int n = g.w * g.h;
            if (n < 4) return null;
            double mean = 0;
            for (int v : g.p) mean += v;
            mean /= n;
            float[] z = new float[n];
            double tss = 0;
            for (int i = 0; i < n; i++) {
                z[i] = (float) (g.p[i] - mean);
                tss += z[i] * z[i];
            }
            if (tss < n * 4.0) return null; // mẫu gần như một màu, không nhận diện được
            return new Prep(g.w, g.h, z, tss);
        }
    }

    public static final class Match {
        public final int x;
        public final int y;
        public final float score;

        Match(int x, int y, float score) {
            this.x = x;
            this.y = y;
            this.score = score;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float scoreFast(Gray img, Integral in, Prep t, int x, int y) {
        float num = 0;
        int w = img.w;
        int[] p = img.p;
        float[] z = t.z;
        for (int j = 0; j < t.h; j++) {
            int ro = (y + j) * w + x;
            int to = j * t.w;
            for (int i = 0; i < t.w; i++) num += p[ro + i] * z[to + i];
        }
        int n = t.w * t.h;
        double s = in.sum(in.s, x, y, t.w, t.h);
        double ss = in.sum(in.ss, x, y, t.w, t.h);
        double var = ss - s * s / n;
        if (var < n) return 0;
        return (float) (num / Math.sqrt(var * t.tss));
    }

    private static float scoreDirect(Gray img, Prep t, int x, int y) {
        float num = 0;
        long s = 0;
        long ss = 0;
        int w = img.w;
        int[] p = img.p;
        for (int j = 0; j < t.h; j++) {
            int ro = (y + j) * w + x;
            int to = j * t.w;
            for (int i = 0; i < t.w; i++) {
                int v = p[ro + i];
                num += v * t.z[to + i];
                s += v;
                ss += (long) v * v;
            }
        }
        int n = t.w * t.h;
        double var = ss - (double) s * s / n;
        if (var < n) return 0;
        return (float) (num / Math.sqrt(var * t.tss));
    }

    /**
     * Tìm mọi vị trí giống mẫu trong vùng (tọa độ màn hình) mà mẫu nằm gọn bên trong.
     * Kết quả là tâm của mẫu theo tọa độ màn hình, xếp theo độ giống giảm dần.
     */
    public static List<Match> findAll(Frame f, Tpl t, int rx0, int ry0, int rx1, int ry1, float thr, int max) {
        List<Match> out = new ArrayList<>();
        if (t == null || !t.usable()) return out;
        Prep p4 = t.p4;
        Prep p2 = t.p2;
        Gray g4 = f.g4;
        int x0 = clamp(rx0 / 4, 0, g4.w);
        int y0 = clamp(ry0 / 4, 0, g4.h);
        int x1 = Math.min(rx1 / 4, g4.w) - p4.w;
        int y1 = Math.min(ry1 / 4, g4.h) - p4.h;
        if (x1 < x0 || y1 < y0) return out;

        int cw = x1 - x0 + 1;
        int ch = y1 - y0 + 1;
        float[] map = new float[cw * ch];
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                map[y * cw + x] = scoreFast(g4, f.i4, p4, x0 + x, y0 + y);
            }
        }

        // Cực đại địa phương ở mức thô.
        float coarseThr = thr - 0.15f;
        List<float[]> cands = new ArrayList<>();
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                float v = map[y * cw + x];
                if (v < coarseThr) continue;
                boolean peak = true;
                for (int dy = -1; dy <= 1 && peak; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= cw || ny >= ch) continue;
                        if (map[ny * cw + nx] > v) {
                            peak = false;
                            break;
                        }
                    }
                }
                if (peak) cands.add(new float[]{x0 + x, y0 + y, v});
            }
        }
        Collections.sort(cands, (a, b) -> Float.compare(b[2], a[2]));
        if (cands.size() > 40) cands = cands.subList(0, 40);

        // Tinh chỉnh ở 1/2 độ phân giải.
        Gray g2 = f.g2;
        List<Match> found = new ArrayList<>();
        for (float[] c : cands) {
            int bx = (int) c[0] * 2;
            int by = (int) c[1] * 2;
            float best = -1;
            int bestX = bx;
            int bestY = by;
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int x = bx + dx;
                    int y = by + dy;
                    if (x < 0 || y < 0 || x + p2.w > g2.w || y + p2.h > g2.h) continue;
                    float s = scoreDirect(g2, p2, x, y);
                    if (s > best) {
                        best = s;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
            if (best >= thr) {
                found.add(new Match((bestX * 2) + p2.w, (bestY * 2) + p2.h, best));
            }
        }
        Collections.sort(found, (a, b) -> Float.compare(b.score, a.score));

        // Loại các kết quả trùng nhau.
        int minDist = Math.max(4, t.side / 2);
        for (Match m : found) {
            boolean dup = false;
            for (Match k : out) {
                if (Math.abs(k.x - m.x) < minDist && Math.abs(k.y - m.y) < minDist) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                out.add(m);
                if (out.size() >= max) break;
            }
        }
        return out;
    }

    public static Match find(Frame f, Tpl t, int rx0, int ry0, int rx1, int ry1, float thr) {
        List<Match> l = findAll(f, t, rx0, ry0, rx1, ry1, thr, 1);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Tỉ lệ điểm ảnh màu đỏ (chữ giá màu đỏ = không đủ tài nguyên) trong vùng. */
    public static float redRatio(Img b, Box r) {
        int[] c = countColors(b, r);
        return c[0] == 0 ? 0 : (float) c[1] / c[0];
    }

    /** Loại tài nguyên theo màu biểu tượng trong vùng: 1 vàng, 2 tiên dược, 3 tiên dược đen, 0 không rõ. */
    public static int resourceColor(Img b, Box r) {
        int[] c = countColors(b, r);
        if (c[0] == 0) return 0;
        int best = 0;
        int bestN = Math.max(4, c[0] / 100);
        for (int k = 1; k <= 3; k++) {
            if (c[k + 1] > bestN) {
                bestN = c[k + 1];
                best = k;
            }
        }
        return best;
    }

    /** {tổng, đỏ, vàng, hồng, đen-tím}. */
    private static int[] countColors(Img b, Box r) {
        int x0 = clamp(r.l, 0, b.w - 1);
        int x1 = clamp(r.r, 0, b.w - 1);
        int y0 = clamp(r.t, 0, b.h - 1);
        int y1 = clamp(r.b, 0, b.h - 1);
        int[] n = new int[5];
        for (int y = y0; y <= y1; y += 2) {
            for (int x = x0; x <= x1; x += 2) {
                int c = b.get(x, y);
                int rr = (c >> 16) & 255;
                int g = (c >> 8) & 255;
                int bl = c & 255;
                n[0]++;
                if (rr > 170 && g < 110 && bl < 110 && rr - g > 90) n[1]++;
                else if (rr > 200 && g > 160 && bl < 90) n[2]++;
                else if (rr > 170 && bl > 150 && g < 110) n[3]++;
                else if (rr < 90 && g < 70 && bl < 110 && bl > rr + 10) n[4]++;
            }
        }
        return n;
    }
}
