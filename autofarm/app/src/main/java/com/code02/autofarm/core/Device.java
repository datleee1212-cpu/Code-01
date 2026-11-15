package com.code02.autofarm.core;

/** Những gì bot cần từ thiết bị: chụp, chạm, vuốt, chờ. Máy thật và giả lập đều cài đặt giao diện này. */
public interface Device {
    /** Chụp màn hình, null nếu không hỗ trợ. */
    Img capture() throws InterruptedException;

    void tap(int x, int y) throws InterruptedException;

    /** Vuốt chậm (không văng) từ (x1,y1) tới (x2,y2) trong ms mili giây. */
    void swipe(int x1, int y1, int x2, int y2, int ms) throws InterruptedException;

    /** Chờ; ném InterruptedException nếu người dùng bấm dừng. */
    void sleep(long ms) throws InterruptedException;

    long now();

    void status(String s);
}
