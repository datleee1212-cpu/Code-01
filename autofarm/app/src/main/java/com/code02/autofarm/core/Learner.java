package com.code02.autofarm.core;

/**
 * Tự cải thiện: ghi nhớ kết quả và tự chỉnh thông số.
 * KHUNG: agent "tự cải thiện" sẽ viết phần thân.
 */
public final class Learner {
    private final Memory memory;

    public Learner(Memory memory) {
        this.memory = memory;
    }

    /** Tóm tắt những gì bot đã học (văn bản nhiều dòng, tiếng Việt) để hiện trong app. */
    public static String summary(Memory m) {
        return "";
    }
}
