package com.code02.autofarm;

import android.content.Context;
import android.content.SharedPreferences;

/** Danh sách các vị trí trên màn hình mà bot sẽ chạm, và nơi lưu chúng. */
final class Points {

    static final class Def {
        final String key;
        final String label;
        final String hint;
        /** true: khi cài đặt, sau khi ghi vị trí thì chạm luôn vào game để chuyển sang màn hình tiếp theo. */
        final boolean passTap;

        Def(String key, String label, String hint, boolean passTap) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.passTap = passTap;
        }
    }

    static final String COLLECT1 = "collect1";
    static final String COLLECT2 = "collect2";
    static final String COLLECT3 = "collect3";
    static final String TRAIN_OPEN = "train_open";
    static final String QUICK_TRAIN = "quick_train";
    static final String TRAIN_ARMY = "train_army";
    static final String TRAIN_CLOSE = "train_close";
    static final String ATTACK = "attack";
    static final String FIND_MATCH = "find_match";
    static final String TROOP1 = "troop1";
    static final String TROOP2 = "troop2";
    static final String HERO = "hero";
    static final String DEPLOY_A = "deploy_a";
    static final String DEPLOY_B = "deploy_b";
    static final String END_BATTLE = "end_battle";
    static final String SURRENDER_OK = "surrender_ok";
    static final String RETURN_HOME = "return_home";

    /** Thứ tự cài đặt đi theo đúng thứ tự màn hình trong game. */
    static final Def[] ALL = {
            new Def(COLLECT1, "Mỏ vàng (thu tài nguyên)", "Có thể bỏ qua", true),
            new Def(COLLECT2, "Bơm tiên dược (thu tài nguyên)", "Có thể bỏ qua", true),
            new Def(COLLECT3, "Khoan tiên dược đen", "Có thể bỏ qua", true),
            new Def(TRAIN_OPEN, "Nút Huấn luyện quân", "Có thể bỏ qua nếu tự luyện quân", true),
            new Def(QUICK_TRAIN, "Thẻ Huấn luyện nhanh", "Có thể bỏ qua", true),
            new Def(TRAIN_ARMY, "Nút Huấn luyện của đội hình 1", "Có thể bỏ qua", true),
            new Def(TRAIN_CLOSE, "Nút X đóng bảng huấn luyện", "Có thể bỏ qua", true),
            new Def(ATTACK, "Nút Tấn công! (góc dưới bên trái)", "Bắt buộc", true),
            new Def(FIND_MATCH, "Nút Tìm trận", "Bắt buộc – sẽ tốn vàng tìm trận", true),
            new Def(TROOP1, "Ô quân thứ 1 (thanh quân phía dưới)", "Nên có", true),
            new Def(TROOP2, "Ô quân thứ 2", "Có thể bỏ qua", false),
            new Def(HERO, "Ô anh hùng (Vua / Nữ hoàng)", "Có thể bỏ qua", false),
            new Def(DEPLOY_A, "Điểm thả quân ĐẦU (mép ngoài làng địch)", "Bắt buộc", false),
            new Def(DEPLOY_B, "Điểm thả quân CUỐI (đầu kia của đường thả)", "Có thể bỏ qua", false),
            new Def(END_BATTLE, "Nút Kết thúc trận / Đầu hàng", "Nên có", true),
            new Def(SURRENDER_OK, "Nút Đồng ý (xác nhận kết thúc)", "Nên có", true),
            new Def(RETURN_HOME, "Nút Về nhà", "Bắt buộc", true),
    };

    static final String[] REQUIRED = {ATTACK, FIND_MATCH, DEPLOY_A, RETURN_HOME};

    private Points() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("autofarm_points", Context.MODE_PRIVATE);
    }

    /** Trả về {x, y} hoặc null nếu chưa cài. */
    static int[] get(Context c, String key) {
        SharedPreferences p = prefs(c);
        int x = p.getInt(key + "_x", -1);
        int y = p.getInt(key + "_y", -1);
        if (x < 0 || y < 0) return null;
        return new int[]{x, y};
    }

    static void set(Context c, String key, int x, int y) {
        prefs(c).edit().putInt(key + "_x", x).putInt(key + "_y", y).apply();
    }

    static void clear(Context c, String key) {
        prefs(c).edit().remove(key + "_x").remove(key + "_y").apply();
    }

    static Def find(String key) {
        for (Def d : ALL) if (d.key.equals(key)) return d;
        return null;
    }

    /** Tên các vị trí bắt buộc còn thiếu, hoặc chuỗi rỗng nếu đã đủ. */
    static String missingRequired(Context c) {
        StringBuilder sb = new StringBuilder();
        for (String k : REQUIRED) {
            if (get(c, k) == null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(find(k).label);
            }
        }
        return sb.toString();
    }

    static int countSet(Context c) {
        int n = 0;
        for (Def d : ALL) if (get(c, d.key) != null) n++;
        return n;
    }
}
