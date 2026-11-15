package com.code02.autofarm.core;

/** Chuyển chữ OCR thành số (sửa các lỗi nhận nhầm thường gặp). */
public final class Parse {
    private Parse() {
    }

    private static char fix(char ch) {
        switch (ch) {
            case 'O': case 'o': case 'D': case 'Q':
                return '0';
            case 'I': case 'l': case 'i': case '|': case '!':
                return '1';
            case 'Z': case 'z':
                return '2';
            case 'S': case 's':
                return '5';
            case 'G': case 'b':
                return '6';
            case 'B':
                return '8';
            case 'g': case 'q':
                return '9';
            default:
                return ch;
        }
    }

    /** Số nguyên (bỏ khoảng trắng, dấu chấm phân cách nghìn). -1 nếu không đọc được. */
    public static long number(String s) {
        if (s == null) return -1;
        StringBuilder d = new StringBuilder();
        for (char ch : s.toCharArray()) {
            char c = fix(ch);
            if (c >= '0' && c <= '9') d.append(c);
        }
        if (d.length() == 0 || d.length() > 10) return -1;
        return Long.parseLong(d.toString());
    }

    /** "2/5" → {2, 5}. null nếu không đọc được. */
    public static int[] fraction(String s) {
        if (s == null) return null;
        int slash = s.indexOf('/');
        if (slash < 0) return null;
        long a = number(s.substring(0, slash));
        long b = number(s.substring(slash + 1));
        if (a < 0 || b <= 0 || a > b || b > 10) return null;
        return new int[]{(int) a, (int) b};
    }
}
