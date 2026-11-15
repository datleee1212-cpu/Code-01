package com.code02.autofarm;

import android.content.Context;
import android.content.SharedPreferences;
import com.code02.autofarm.core.Box;
import com.code02.autofarm.core.Keys;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách các mục bạn "dạy" cho bot: vị trí chạm, hình mẫu để nhận diện, và vùng đọc số.
 */
final class Points {

    enum Kind { TAP, REGION }

    /** NEAR: chỉ tìm hình quanh vị trí đã dạy (nút cố định). FULL: tìm trên cả màn hình. */
    enum Scope { NEAR, FULL }

    static final String G_FARM = "Đánh trận (farm)";
    static final String G_COLLECT = "Thu tài nguyên";
    static final String G_UPGRADE = "Tự nâng cấp";
    static final String G_POPUP = "Đóng thông báo";

    static final String[] GROUPS = {G_FARM, G_COLLECT, G_UPGRADE, G_POPUP};

    static final class Def {
        final String key;
        final String label;
        final String hint;
        final String group;
        final Kind kind;
        final Scope scope;
        /** Có lưu hình mẫu để nhận diện không (false: chỉ dùng tọa độ). */
        final boolean template;
        /** Khi dạy: sau khi ghi thì chạm luôn vào game để sang màn hình tiếp theo. */
        final boolean passTap;

        Def(String key, String label, String hint, String group, Kind kind, Scope scope,
            boolean template, boolean passTap) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.group = group;
            this.kind = kind;
            this.scope = scope;
            this.template = template;
            this.passTap = passTap;
        }
    }

    // Tên các mục: dùng chung với lõi bot (core/Keys).
    static final String COLLECT_GOLD = Keys.COLLECT_GOLD;
    static final String COLLECT_ELIXIR = Keys.COLLECT_ELIXIR;
    static final String COLLECT_DARK = Keys.COLLECT_DARK;
    static final String TRAIN_OPEN = Keys.TRAIN_OPEN;
    static final String QUICK_TRAIN = Keys.QUICK_TRAIN;
    static final String TRAIN_ARMY = Keys.TRAIN_ARMY;
    static final String TRAIN_CLOSE = Keys.TRAIN_CLOSE;
    static final String ATTACK = Keys.ATTACK;
    static final String FIND_MATCH = Keys.FIND_MATCH;
    static final String NEXT = Keys.NEXT;
    static final String LOOT_GOLD = Keys.LOOT_GOLD;
    static final String LOOT_ELIXIR = Keys.LOOT_ELIXIR;
    static final String TROOP1 = Keys.TROOP1;
    static final String TROOP2 = Keys.TROOP2;
    static final String HERO = Keys.HERO;
    static final String DEPLOY_A = Keys.DEPLOY_A;
    static final String DEPLOY_B = Keys.DEPLOY_B;
    static final String END_BATTLE = Keys.END_BATTLE;
    static final String SURRENDER_OK = Keys.SURRENDER_OK;
    static final String RETURN_HOME = Keys.RETURN_HOME;
    static final String[] POPUPS = Keys.POPUPS;

    private static Def tap(String key, String label, String hint, String group, Scope scope,
                           boolean template, boolean passTap) {
        return new Def(key, label, hint, group, Kind.TAP, scope, template, passTap);
    }

    private static Def region(String key, String label, String hint, String group) {
        return new Def(key, label, hint, group, Kind.REGION, Scope.FULL, false, false);
    }

    static final Def[] ALL;

    static {
        List<Def> l = new ArrayList<>();
        // --- Farm (thứ tự đúng theo màn hình trong game)
        l.add(tap(TRAIN_OPEN, "Nút Huấn luyện quân", "Có thể bỏ qua", G_FARM, Scope.NEAR, true, true));
        l.add(tap(QUICK_TRAIN, "Thẻ Huấn luyện nhanh", "Có thể bỏ qua", G_FARM, Scope.NEAR, true, true));
        l.add(tap(TRAIN_ARMY, "Nút Huấn luyện của đội hình 1", "Có thể bỏ qua", G_FARM, Scope.NEAR, true, true));
        l.add(tap(TRAIN_CLOSE, "Nút X đóng bảng huấn luyện", "Có thể bỏ qua", G_FARM, Scope.NEAR, true, true));
        l.add(tap(ATTACK, "Nút Tấn công! (góc dưới trái)", "Bắt buộc – dùng để nhận biết đang ở làng chính", G_FARM, Scope.NEAR, true, true));
        l.add(tap(FIND_MATCH, "Nút Tìm trận", "Bắt buộc – app sẽ bấm, tốn vàng tìm trận", G_FARM, Scope.NEAR, true, true));
        l.add(tap(NEXT, "Nút Tiếp theo (khi đang xem làng địch)", "Nên có – để bỏ qua làng nghèo", G_FARM, Scope.NEAR, true, false));
        l.add(region(LOOT_GOLD, "Vùng SỐ VÀNG có thể cướp (góc trên trái)", "Chạm 2 góc của ô chứa số vàng", G_FARM));
        l.add(region(LOOT_ELIXIR, "Vùng SỐ TIÊN DƯỢC có thể cướp", "Chạm 2 góc của ô chứa số tiên dược", G_FARM));
        l.add(tap(TROOP1, "Ô quân thứ 1 (thanh quân phía dưới)", "Nên có", G_FARM, Scope.NEAR, false, true));
        l.add(tap(TROOP2, "Ô quân thứ 2", "Có thể bỏ qua", G_FARM, Scope.NEAR, false, false));
        l.add(tap(HERO, "Ô anh hùng (Vua / Nữ hoàng)", "Có thể bỏ qua", G_FARM, Scope.NEAR, false, false));
        l.add(tap(DEPLOY_A, "Điểm thả quân ĐẦU (mép ngoài làng địch)", "Bắt buộc", G_FARM, Scope.NEAR, false, false));
        l.add(tap(DEPLOY_B, "Điểm thả quân CUỐI (đầu kia đường thả)", "Có thể bỏ qua", G_FARM, Scope.NEAR, false, false));
        l.add(tap(END_BATTLE, "Nút Kết thúc trận / Đầu hàng", "Nên có", G_FARM, Scope.NEAR, true, true));
        l.add(tap(SURRENDER_OK, "Nút Đồng ý (xác nhận kết thúc)", "Nên có", G_FARM, Scope.NEAR, true, true));
        l.add(tap(RETURN_HOME, "Nút Về nhà", "Bắt buộc – dùng để biết trận đã xong", G_FARM, Scope.NEAR, true, true));
        // --- Thu tài nguyên: biểu tượng nổi trên công trình, tìm khắp màn hình
        l.add(tap(COLLECT_GOLD, "Biểu tượng VÀNG nổi trên mỏ vàng", "Chỉ hiện khi có vàng để thu", G_COLLECT, Scope.FULL, true, true));
        l.add(tap(COLLECT_ELIXIR, "Biểu tượng TIÊN DƯỢC nổi trên bơm", "Chỉ hiện khi có tiên dược để thu", G_COLLECT, Scope.FULL, true, true));
        l.add(tap(COLLECT_DARK, "Biểu tượng TIÊN DƯỢC ĐEN nổi trên khoan", "Có thể bỏ qua", G_COLLECT, Scope.FULL, true, true));
        // --- Kho & nâng cấp
        l.add(region(Keys.OWN_GOLD, "Vùng SỐ VÀNG của bạn (góc trên phải, ở làng)", "Để tối ưu tài nguyên – chỉ khoanh phần chữ số", G_UPGRADE));
        l.add(region(Keys.OWN_ELIXIR, "Vùng SỐ TIÊN DƯỢC của bạn", "Chỉ khoanh phần chữ số", G_UPGRADE));
        l.add(region(Keys.OWN_DARK, "Vùng SỐ TIÊN DƯỢC ĐEN của bạn", "Có thể bỏ qua", G_UPGRADE));
        l.add(region(Keys.BUILDERS_FREE, "Vùng số thợ rảnh (vd \"1/5\") cạnh biểu tượng thợ xây", "Nên có – bot không mở danh sách khi hết thợ", G_UPGRADE));
        l.add(tap(Keys.BUILDER, "Biểu tượng Thợ xây (trên cùng giữa)", "Bắt buộc – app sẽ bấm để mở danh sách", G_UPGRADE, Scope.NEAR, true, true));
        l.add(region(Keys.UPGRADE_ROW1, "DÒNG ĐẦU TIÊN trong danh sách (từ đầu tên tới hết giá)", "Bắt buộc – bot đọc tên + giá trong khung này", G_UPGRADE));
        l.add(tap(Keys.UPGRADE_ROW2, "Giữa DÒNG THỨ HAI trong danh sách", "Nên có – để bot đọc nhiều dòng và cuộn trang", G_UPGRADE, Scope.NEAR, false, false));
        l.add(tap(Keys.UPGRADE_BTN, "Nút Nâng cấp (hàng nút dưới công trình)", "Bấm \"Ẩn 5 giây\", tự chọn 1 công trình, rồi chạm nút này", G_UPGRADE, Scope.FULL, true, true));
        l.add(tap(Keys.UPGRADE_CONFIRM, "Nút giá nâng cấp (xác nhận) trong bảng", "Bắt buộc – app KHÔNG bấm, dạy xong tự đóng bảng", G_UPGRADE, Scope.FULL, true, false));
        l.add(tap(Keys.UPGRADE_BTN_ELIXIR, "Nút nâng cấp TƯỜNG bằng tiên dược", "Chỉ khi chọn tường (nhà chính 9+) – có thể bỏ qua", G_UPGRADE, Scope.FULL, true, false));
        // --- Thông báo
        for (int i = 0; i < POPUPS.length; i++) {
            l.add(tap(POPUPS[i], "Nút X / Đóng / OK / Thử lại / Tải lại – mẫu " + (i + 1),
                    "Dạy khi có thông báo hiện ra. KHÔNG dạy nút mua bằng ngọc!", G_POPUP, Scope.FULL, true, true));
        }
        ALL = l.toArray(new Def[0]);
    }

    static final String[] REQUIRED = Keys.REQUIRED;

    private Points() {
    }

    static List<Def> inGroup(String group) {
        List<Def> out = new ArrayList<>();
        for (Def d : ALL) if (d.group.equals(group)) out.add(d);
        return out;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("autofarm_points", Context.MODE_PRIVATE);
    }

    /** Trả về {x, y} hoặc null nếu chưa dạy. */
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

    static Box getRegion(Context c, String key) {
        int[] a = get(c, key + "_1");
        int[] b = get(c, key + "_2");
        if (a == null || b == null) return null;
        Box r = new Box(a[0], a[1], b[0], b[1]);
        if (r.w() < 4 || r.h() < 4) return null;
        return r;
    }

    static void clear(Context c, String key) {
        prefs(c).edit()
                .remove(key + "_x").remove(key + "_y")
                .remove(key + "_1_x").remove(key + "_1_y")
                .remove(key + "_2_x").remove(key + "_2_y")
                .apply();
        Templates.delete(c, key);
    }

    static boolean isSet(Context c, Def d) {
        return d.kind == Kind.REGION ? getRegion(c, d.key) != null : get(c, d.key) != null;
    }

    static Def find(String key) {
        for (Def d : ALL) if (d.key.equals(key)) return d;
        return null;
    }

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

    static int countSet(Context c, String group) {
        int n = 0;
        for (Def d : ALL) if ((group == null || d.group.equals(group)) && isSet(c, d)) n++;
        return n;
    }

    static int countGroup(String group) {
        int n = 0;
        for (Def d : ALL) if (d.group.equals(group)) n++;
        return n;
    }
}
