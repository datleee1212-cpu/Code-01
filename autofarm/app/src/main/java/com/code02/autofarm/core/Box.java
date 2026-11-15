package com.code02.autofarm.core;

/** Hình chữ nhật theo tọa độ màn hình. */
public final class Box {
    public final int l;
    public final int t;
    public final int r;
    public final int b;

    public Box(int l, int t, int r, int b) {
        this.l = Math.min(l, r);
        this.t = Math.min(t, b);
        this.r = Math.max(l, r);
        this.b = Math.max(t, b);
    }

    public int w() {
        return r - l;
    }

    public int h() {
        return b - t;
    }

    public int cx() {
        return (l + r) / 2;
    }

    public int cy() {
        return (t + b) / 2;
    }

    public Box shift(int dx, int dy) {
        return new Box(l + dx, t + dy, r + dx, b + dy);
    }

    /** Phần bên trái / phải theo tỉ lệ chiều rộng. */
    public Box leftPart(float f) {
        return new Box(l, t, l + Math.round(w() * f), b);
    }

    public Box rightPart(float f) {
        return new Box(r - Math.round(w() * f), t, r, b);
    }

    public boolean inside(int W, int H) {
        return l >= 0 && t >= 0 && r <= W && b <= H && w() >= 4 && h() >= 4;
    }

    @Override
    public String toString() {
        return "[" + l + "," + t + "-" + r + "," + b + "]";
    }
}
