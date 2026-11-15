package com.code02.autofarm.core;

/** Những gì bot đã được dạy. */
public interface Store {
    /** Tọa độ {x, y} hoặc null. */
    int[] point(String key);

    /** Vùng hoặc null. */
    Box region(String key);

    /** Hình mẫu hoặc null. */
    Img template(String key);
}
