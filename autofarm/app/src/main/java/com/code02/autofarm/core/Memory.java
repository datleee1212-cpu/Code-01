package com.code02.autofarm.core;

/**
 * Bộ nhớ lâu dài của bot (máy thật: SharedPreferences; giả lập: HashMap).
 * Dùng cho kênh tự cải thiện: lịch sử trận, tiến độ lộ trình, thông số đã học.
 */
public interface Memory {
    /** null nếu chưa có. */
    String get(String key);

    void put(String key, String value);

    /** Bộ nhớ tạm trong RAM (khi không cần lưu lâu dài). */
    static Memory inRam() {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        return new Memory() {
            @Override
            public String get(String key) {
                return m.get(key);
            }

            @Override
            public void put(String key, String value) {
                m.put(key, value);
            }
        };
    }
}
