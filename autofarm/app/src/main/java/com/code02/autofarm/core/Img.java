package com.code02.autofarm.core;

/** Ảnh ARGB đơn giản, không phụ thuộc Android (để chạy được cả trong giả lập). */
public final class Img {
    public final int w;
    public final int h;
    public final int[] px;

    public Img(int w, int h, int[] px) {
        this.w = w;
        this.h = h;
        this.px = px;
    }

    public Img(int w, int h) {
        this(w, h, new int[w * h]);
    }

    public int get(int x, int y) {
        return px[y * w + x];
    }

    /** Cắt một vùng (tự kẹp vào trong ảnh). */
    public Img crop(int x, int y, int cw, int ch) {
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        cw = Math.max(1, Math.min(cw, w - x));
        ch = Math.max(1, Math.min(ch, h - y));
        int[] o = new int[cw * ch];
        for (int j = 0; j < ch; j++) System.arraycopy(px, (y + j) * w + x, o, j * cw, cw);
        return new Img(cw, ch, o);
    }

    /** Cắt hình vuông cạnh side quanh tâm (cx, cy), dùng khi dạy hình mẫu. */
    public Img cropSquare(int cx, int cy, int side) {
        side = Math.max(16, Math.min(side, Math.min(w, h)));
        int x0 = Math.max(0, Math.min(w - side, cx - side / 2));
        int y0 = Math.max(0, Math.min(h - side, cy - side / 2));
        return crop(x0, y0, side, side);
    }
}
