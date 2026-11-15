package com.code02.autofarm;

import android.content.Context;
import android.content.SharedPreferences;

import com.code02.autofarm.core.Memory;

/**
 * Bộ nhớ lâu dài của bot trên máy thật: lưu trong SharedPreferences "autofarm_memory".
 * Giữ lịch sử trận, tiến độ lộ trình và thông số đã học qua các lần chạy / tắt máy.
 */
final class PrefsMemory implements Memory {

    private static final String FILE = "autofarm_memory";

    private final SharedPreferences prefs;

    PrefsMemory(Context c) {
        prefs = c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public String get(String key) {
        try {
            return prefs.getString(key, null);
        } catch (ClassCastException e) {
            return null; // giá trị cũ không phải chuỗi
        }
    }

    @Override
    public void put(String key, String value) {
        SharedPreferences.Editor e = prefs.edit();
        if (value == null) {
            e.remove(key);
        } else {
            e.putString(key, value);
        }
        e.apply(); // ghi nền, không chặn luồng bot
    }

    /** Có dữ liệu đã học / tiến độ nào chưa. */
    static boolean isEmpty(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE).getAll().isEmpty();
    }

    /** Xóa toàn bộ dữ liệu đã học và tiến độ lộ trình (không đụng tới vị trí đã dạy và thông số). */
    static void clear(Context c) {
        c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
