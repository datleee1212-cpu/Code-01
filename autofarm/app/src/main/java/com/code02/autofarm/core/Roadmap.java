package com.code02.autofarm.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lộ trình nâng cấp: nhận biết loại công trình từ tên (tiếng Anh / tiếng Việt)
 * và xếp thứ tự ưu tiên theo bộ lộ trình đã chọn.
 */
public final class Roadmap {

    private Roadmap() {
    }

    public enum Cat {
        LAB("Phòng thí nghiệm"),
        CLAN_CASTLE("Lâu đài bang hội"),
        ARMY_CAMP("Trại quân"),
        ARMY_BUILDING("Nhà lính / Xưởng phép / Xưởng"),
        HERO("Anh hùng"),
        STORAGE("Kho"),
        SPLASH_DEF("Phòng thủ diện rộng"),
        AIR_DEF("Phòng không / thủ mạnh"),
        POINT_DEF("Phòng thủ đơn"),
        COLLECTOR("Mỏ / bơm"),
        TRAP("Bẫy"),
        WALL("Tường"),
        TOWN_HALL("Nhà chính"),
        UNKNOWN("Không rõ");

        public final String vi;

        Cat(String vi) {
            this.vi = vi;
        }
    }

    public static final int BALANCED = 1;
    public static final int FARM = 2;
    public static final int DEFENSE = 3;
    /** Lộ trình TH16 → TH17 (theo file hướng dẫn của người dùng). */
    public static final int TH16_TH17 = 4;

    public static String presetName(int p) {
        switch (p) {
            case FARM:
                return "Farm (kinh tế + quân trước)";
            case DEFENSE:
                return "Phòng thủ trước";
            case TH16_TH17:
                return "TH16 → TH17";
            default:
                return "Cân bằng (chuẩn)";
        }
    }

    private static final Cat[] ORDER_BALANCED = {
            Cat.LAB, Cat.CLAN_CASTLE, Cat.ARMY_CAMP, Cat.ARMY_BUILDING, Cat.HERO, Cat.STORAGE,
            Cat.SPLASH_DEF, Cat.AIR_DEF, Cat.POINT_DEF, Cat.COLLECTOR, Cat.TRAP, Cat.UNKNOWN, Cat.TOWN_HALL};
    private static final Cat[] ORDER_FARM = {
            Cat.LAB, Cat.ARMY_CAMP, Cat.STORAGE, Cat.CLAN_CASTLE, Cat.ARMY_BUILDING, Cat.HERO,
            Cat.COLLECTOR, Cat.SPLASH_DEF, Cat.AIR_DEF, Cat.POINT_DEF, Cat.TRAP, Cat.UNKNOWN, Cat.TOWN_HALL};
    private static final Cat[] ORDER_DEFENSE = {
            Cat.LAB, Cat.CLAN_CASTLE, Cat.SPLASH_DEF, Cat.AIR_DEF, Cat.HERO, Cat.ARMY_CAMP,
            Cat.ARMY_BUILDING, Cat.STORAGE, Cat.POINT_DEF, Cat.TRAP, Cat.COLLECTOR, Cat.UNKNOWN, Cat.TOWN_HALL};

    /** Thứ hạng (0 = ưu tiên nhất). Tường không nằm trong lộ trình (dùng để xả tài nguyên). */
    public static int rank(int preset, Cat c) {
        Cat[] order = preset == FARM ? ORDER_FARM : preset == DEFENSE ? ORDER_DEFENSE : ORDER_BALANCED;
        for (int i = 0; i < order.length; i++) if (order[i] == c) return i;
        return 99;
    }

    public static String describe(int preset) {
        Cat[] order = preset == FARM ? ORDER_FARM : preset == DEFENSE ? ORDER_DEFENSE : ORDER_BALANCED;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            if (order[i] == Cat.UNKNOWN) continue;
            if (sb.length() > 0) sb.append(" → ");
            sb.append(order[i].vi);
        }
        return sb.toString();
    }

    private static final class Kw {
        final String phrase;
        final Cat cat;

        Kw(String phrase, Cat cat) {
            this.phrase = phrase;
            this.cat = cat;
        }
    }

    private static final List<Kw> KEYWORDS = new ArrayList<>();

    private static void kw(Cat c, String... phrases) {
        for (String p : phrases) KEYWORDS.add(new Kw(p, c));
    }

    static {
        kw(Cat.TOWN_HALL, "town hall", "nha chinh", "toa thi chinh", "thi chinh");
        kw(Cat.LAB, "laboratory", "lab", "phong thi nghiem", "thi nghiem");
        kw(Cat.CLAN_CASTLE, "clan castle", "lau dai bang hoi", "lau dai", "thanh bang hoi");
        kw(Cat.ARMY_CAMP, "army camp", "trai quan", "trai linh", "doanh trai");
        kw(Cat.ARMY_BUILDING, "barracks", "dark barracks", "nha linh", "spell factory", "dark spell factory",
                "xuong phep", "workshop", "siege workshop", "xuong", "pet house", "nha thu cung",
                "blacksmith", "lo ren", "tho ren", "hero hall", "sanh anh hung", "dai anh hung");
        kw(Cat.HERO, "barbarian king", "archer queen", "grand warden", "royal champion", "minion prince",
                "vua", "vua man ro", "nu hoang", "nu hoang cung thu", "hiep si", "can phong",
                "nha vo dich", "hoang tu");
        kw(Cat.STORAGE, "storage", "gold storage", "elixir storage", "dark elixir storage",
                "kho", "kho vang", "kho tien duoc", "kho tien duoc den");
        kw(Cat.SPLASH_DEF, "wizard tower", "thap phu thuy", "mortar", "sung coi", "phao coi",
                "bomb tower", "thap bom", "eagle artillery", "phao dai bang", "dai bang",
                "scattershot", "spell tower", "thap phep", "monolith", "nguyen khoi");
        kw(Cat.AIR_DEF, "air defense", "phong khong", "air sweeper", "may thoi khi", "may thoi",
                "x bow", "xbow", "no lon", "inferno tower", "inferno", "thap lua", "hidden tesla",
                "tesla", "thap tesla");
        kw(Cat.POINT_DEF, "archer tower", "thap cung", "thap cung thu", "cannon", "dai bac",
                "builder s hut", "builders hut", "nha tho xay", "chong tho xay");
        kw(Cat.COLLECTOR, "gold mine", "mo vang", "elixir collector", "collector", "bom tien duoc",
                "may bom tien duoc", "dark elixir drill", "drill", "khoan", "khoan tien duoc den");
        kw(Cat.TRAP, "bomb", "giant bomb", "air bomb", "seeking air mine", "spring trap", "skeleton trap",
                "tornado trap", "trap", "bom", "bom lon", "bay", "min", "bay lo xo", "bay xuong");
        kw(Cat.WALL, "wall", "walls", "tuong", "tuong thanh");
        // Cụm dài khớp trước, để "bomb tower" không bị hiểu là "bomb", "bom tien duoc" không bị hiểu là "bom".
        Collections.sort(KEYWORDS, (a, b) -> b.phrase.length() - a.phrase.length());
    }

    /** Bỏ dấu tiếng Việt, chữ thường, chỉ giữ chữ và số. */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replace('đ', 'd').replace('Đ', 'D').toLowerCase();
        n = n.replaceAll("[^a-z0-9]+", " ").trim();
        return " " + n + " ";
    }

    public static Cat classify(String name) {
        String n = normalize(name);
        if (n.trim().isEmpty()) return Cat.UNKNOWN;
        for (Kw k : KEYWORDS) {
            if (n.contains(" " + k.phrase + " ")) return k.cat;
        }
        return Cat.UNKNOWN;
    }

    // ---------------------------------------------------------------- chọn mục nâng cấp

    /** Một dòng trong danh sách thợ xây. */
    public static final class Row {
        public final int page;
        public final int index;
        public final String name;
        public final Cat cat;
        public final boolean affordable;
        /** 1 vàng, 2 tiên dược, 3 tiên dược đen, 0 không rõ. */
        public final int res;

        public Row(int page, int index, String name, Cat cat, boolean affordable, int res) {
            this.page = page;
            this.index = index;
            this.name = name;
            this.cat = cat;
            this.affordable = affordable;
            this.res = res;
        }

        @Override
        public String toString() {
            return name + " [" + cat.vi + (affordable ? "" : ", thiếu") + "]";
        }
    }

    /**
     * Chọn mục nên nâng cấp.
     *
     * @param fill    độ đầy kho theo loại tài nguyên (chỉ số 1..3), -1 nếu không biết
     * @param overflow ngưỡng coi là sắp tràn kho (0..1)
     * @param wallFill ngưỡng xả vào tường (0..1), ≤0 để tắt
     */
    public static Row choose(List<Row> rows, int preset, float[] fill, float overflow, float wallFill) {
        // Mục ưu tiên nhất đang thiếu tiền, theo từng loại tài nguyên → để dành tài nguyên cho nó.
        int[] blockedRank = {99, 99, 99, 99};
        boolean othersExist = false;
        for (Row r : rows) {
            if (r.cat != Cat.WALL && r.cat != Cat.TOWN_HALL) othersExist = true;
            if (!r.affordable && r.cat != Cat.WALL && r.res > 0) {
                blockedRank[r.res] = Math.min(blockedRank[r.res], rank(preset, r.cat));
            }
        }

        Row best = null;
        float bestScore = Float.MAX_VALUE;
        for (Row r : rows) {
            if (!r.affordable || r.cat == Cat.WALL) continue;
            // Nhà chính để sau cùng: chỉ nâng khi không còn gì khác cần nâng.
            if (r.cat == Cat.TOWN_HALL && othersExist) continue;
            int rk = rank(preset, r.cat);
            float f = r.res > 0 && fill[r.res] >= 0 ? fill[r.res] : 0f;
            boolean overflowing = f >= overflow;
            // Để dành: không tiêu tài nguyên vào mục kém ưu tiên hơn mục đang chờ tiền, trừ khi kho sắp tràn.
            if (r.res > 0 && rk > blockedRank[r.res] && !overflowing) continue;
            float score = rk - (overflowing ? 3f : 0f) - f * 0.5f;
            if (score < bestScore) {
                bestScore = score;
                best = r;
            }
        }
        if (best != null) return best;

        // Xả tài nguyên dư vào tường.
        if (wallFill > 0) {
            Row wall = null;
            float wf = -1;
            for (Row r : rows) {
                if (r.cat != Cat.WALL || !r.affordable) continue;
                // Tường nâng được bằng vàng hoặc (nhà chính 9+) tiên dược → xét kho đầy nhất.
                float f = maxFill(fill);
                if (f >= wallFill && f > wf) {
                    wf = f;
                    wall = r;
                }
            }
            return wall;
        }
        return null;
    }

    private static float maxFill(float[] fill) {
        float m = -1;
        for (int i = 1; i < fill.length; i++) m = Math.max(m, fill[i]);
        return m;
    }
}
