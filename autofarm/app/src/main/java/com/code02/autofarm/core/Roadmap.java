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
                return "TH16 → TH17 (theo file lộ trình)";
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
        if (preset == TH16_TH17) return PlanTH17.describe(16) + "\n" + PlanTH17.describe(17);
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
        // Tên công trình mới (TH16/TH17: Firespitter, Multi-Gear, Dragon Duke…) → lấy loại thô từ lớp chi tiết.
        return PlanItem.of(name).cat;
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
        /** Giá nâng cấp đọc được (-1 = không rõ). */
        public final long cost;

        public Row(int page, int index, String name, Cat cat, boolean affordable, int res) {
            this(page, index, name, cat, affordable, res, -1);
        }

        public Row(int page, int index, String name, Cat cat, boolean affordable, int res, long cost) {
            this.page = page;
            this.index = index;
            this.name = name;
            this.cat = cat;
            this.affordable = affordable;
            this.res = res;
            this.cost = cost;
        }

        @Override
        public String toString() {
            return name + " [" + cat.vi + (affordable ? "" : ", thiếu") + "]";
        }
    }

    /**
     * Tình hình kinh tế để tối ưu cả thời gian lẫn tài nguyên (chỉ số 1 vàng, 2 tiên dược, 3 tiên dược đen).
     * Giá trị -1 / 0 = không rõ → giữ cách chọn cũ.
     */
    public static final class Economy {
        /** Tài nguyên đang có (-1 = không rõ). */
        public long[] own = {-1, -1, -1, -1};
        /** Sức chứa kho (≤ 0 = không rõ). */
        public long[] cap = {0, 0, 0, 0};
        /** Ước tính tài nguyên farm được mỗi giờ (≤ 0 = không rõ). */
        public double[] lootPerHour = {0, 0, 0, 0};
        /** Chỉ để dành cho mục ưu tiên hơn nếu đủ tiền trong vòng chừng này giờ; lâu hơn thì cho thợ làm việc khác. */
        public float maxWaitHours = 1.5f;
        /** KẾT QUẢ: khi vừa chọn tường, tài nguyên nên trả (1 vàng, 2 tiên dược, 0 không rõ → bot tự chọn). */
        public int wallRes = 0;
    }

    /** Cách xếp hạng dùng chung cho bộ chọn (lộ trình cũ và lộ trình TH16 → TH17). */
    public interface Ranking {
        /** Được phép chọn và được tính vào luật để dành (vd. Nhà chính bị khóa, tướng đã đủ số nâng → false). */
        boolean eligible(Row r);

        /** Nhóm ưu tiên (nhỏ = ưu tiên hơn), dùng cho luật để dành. */
        int group(Row r);

        /** Điểm gốc (nhỏ = ưu tiên hơn, tăng theo nhóm), dùng để chọn trong số mục mua được. */
        float base(Row r);

        /** Điểm cộng khi kho của mục đó sắp tràn. */
        float overflowBonus();

        /** Trọng số độ đầy kho (kho đầy hơn → ưu tiên tiêu trước). */
        float fillWeight();
    }

    /** Xếp hạng của các lộ trình cũ (giữ nguyên hành vi). */
    private static Ranking classic(final List<Row> rows, final int preset) {
        boolean others = false;
        for (Row r : rows) if (r.cat != Cat.WALL && r.cat != Cat.TOWN_HALL) others = true;
        final boolean othersExist = others;
        return new Ranking() {
            @Override
            public boolean eligible(Row r) {
                // Nhà chính để sau cùng: chỉ nâng khi không còn gì khác cần nâng.
                return r.cat != Cat.WALL && !(r.cat == Cat.TOWN_HALL && othersExist);
            }

            @Override
            public int group(Row r) {
                return rank(preset, r.cat);
            }

            @Override
            public float base(Row r) {
                return rank(preset, r.cat);
            }

            @Override
            public float overflowBonus() {
                return 3f;
            }

            @Override
            public float fillWeight() {
                return 0.5f;
            }
        };
    }

    /**
     * Chọn mục nên nâng cấp.
     *
     * @param fill    độ đầy kho theo loại tài nguyên (chỉ số 1..3), -1 nếu không biết
     * @param overflow ngưỡng coi là sắp tràn kho (0..1)
     * @param wallFill ngưỡng xả vào tường (0..1), ≤0 để tắt
     */
    public static Row choose(List<Row> rows, int preset, float[] fill, float overflow, float wallFill) {
        return choose(rows, preset, fill, overflow, wallFill, null);
    }

    /** Như trên, thêm thông tin kinh tế (giá, tài nguyên đang có, tốc độ farm) để thợ không rảnh vô ích. */
    public static Row choose(List<Row> rows, int preset, float[] fill, float overflow, float wallFill, Economy eco) {
        if (preset == TH16_TH17) {
            // Gọi khi chưa có bộ nhớ: thứ tự lộ trình TH16 → TH17 với bộ nhớ trống và cài đặt mặc định.
            return PlanTH17.choose(rows, new Settings(), fill, Memory.inRam(), 0L, overflow, wallFill, eco);
        }
        return pick(rows, classic(rows, preset), fill, overflow, wallFill, eco);
    }

    /** Chỉ xét tường (vd. khi hết thợ rảnh: tường nâng xong ngay, không tốn thợ). null nếu không nên. */
    public static Row chooseWall(List<Row> rows, int preset, float[] fill, float wallFill, Economy eco) {
        if (preset == TH16_TH17) {
            return PlanTH17.chooseWall(rows, new Settings(), fill, Memory.inRam(), 0L, wallFill, eco);
        }
        return pickWall(rows, classic(rows, preset), fill, wallFill, eco);
    }

    /** Mục thiếu tiền có đáng để dành không: sẽ đủ tiền sớm (≤ maxWaitHours), hoặc không đủ dữ liệu để biết. */
    public static boolean worthWaiting(Row r, Economy eco) {
        if (eco == null || r.cost < 0 || r.res <= 0 || r.res > 3) return true;
        if (eco.cap != null && eco.cap[r.res] > 0 && r.cost > eco.cap[r.res]) return false; // kho chứa không đủ
        long own = eco.own == null ? -1 : eco.own[r.res];
        double rate = eco.lootPerHour == null ? 0 : eco.lootPerHour[r.res];
        if (own < 0 || rate <= 0) return true;
        long shortfall = r.cost - own;
        if (shortfall <= 0) return true;
        return shortfall / rate <= eco.maxWaitHours;
    }

    /** Mục đang để dành theo từng loại tài nguyên: {nhóm[], giá[]} (nhóm 99 = không để dành; giá -1 = không rõ). */
    private static long[][] reserves(List<Row> rows, Ranking rk, Economy eco) {
        long[] group = {99, 99, 99, 99};
        long[] cost = {0, 0, 0, 0};
        float[] best = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        for (Row r : rows) {
            if (r.affordable || r.cat == Cat.WALL || r.res <= 0 || r.res > 3 || !rk.eligible(r)) continue;
            if (!worthWaiting(r, eco)) continue; // còn lâu mới đủ tiền → không bắt thợ chờ
            float b = rk.base(r);
            if (b < best[r.res]) {
                best[r.res] = b;
                group[r.res] = rk.group(r);
                cost[r.res] = r.cost;
            }
        }
        return new long[][]{group, cost};
    }

    /** Bộ chọn dùng chung: mục mua được tốt nhất, tôn trọng luật để dành; không có thì xét tường. */
    public static Row pick(List<Row> rows, Ranking rk, float[] fill, float overflow, float wallFill, Economy eco) {
        long[][] rv = reserves(rows, rk, eco);
        long[] blockedGroup = rv[0];
        Row best = null;
        float bestScore = Float.MAX_VALUE;
        for (Row r : rows) {
            if (!r.affordable || r.cat == Cat.WALL || !rk.eligible(r)) continue;
            float f = r.res > 0 && r.res < fill.length && fill[r.res] >= 0 ? fill[r.res] : 0f;
            boolean overflowing = f >= overflow;
            // Để dành: không tiêu tài nguyên vào mục kém ưu tiên hơn mục đang chờ tiền (sắp đủ), trừ khi kho sắp tràn.
            if (r.res > 0 && r.res <= 3 && rk.group(r) > blockedGroup[r.res] && !overflowing) continue;
            float score = rk.base(r) - (overflowing ? rk.overflowBonus() : 0f) - f * rk.fillWeight();
            if (score < bestScore) {
                bestScore = score;
                best = r;
            }
        }
        if (best != null) {
            if (eco != null) eco.wallRes = 0;
            return best;
        }
        return wallFrom(rows, rv, fill, wallFill, eco);
    }

    /** Chỉ xét tường với cùng luật để dành của bộ chọn. */
    public static Row pickWall(List<Row> rows, Ranking rk, float[] fill, float wallFill, Economy eco) {
        return wallFrom(rows, reserves(rows, rk, eco), fill, wallFill, eco);
    }

    /**
     * Tường = chỗ xả tài nguyên thừa ("thừa cứ nâng"). Thừa = đang có − phần để dành (giá mục đang chờ tiền).
     * Biết giá tường và biết phần thừa → nâng nếu thừa ≥ giá tường (không bao giờ ăn vào phần để dành).
     * Không đủ dữ liệu → luật cũ: kho đầy ≥ wallFill. wallFill ≤ 0 = tắt hẳn tường.
     */
    private static Row wallFrom(List<Row> rows, long[][] rv, float[] fill, float wallFill, Economy eco) {
        if (eco != null) eco.wallRes = 0;
        if (wallFill <= 0) return null;
        long[] surplus = {-1, -1, -1};
        if (eco != null && eco.own != null) {
            for (int res = 1; res <= 2; res++) {
                long own = eco.own[res];
                long reserve = rv[0][res] < 99 ? rv[1][res] : 0;
                if (own >= 0 && reserve >= 0) surplus[res] = Math.max(0, own - reserve);
            }
        }
        // Tường trả được bằng vàng hoặc (nhà chính 9+) tiên dược → dùng loại thừa nhiều hơn.
        int payRes = surplus[2] > surplus[1] ? 2 : 1;
        long s = surplus[payRes];
        float mf = maxFill(fill);
        for (Row r : rows) {
            if (r.cat != Cat.WALL || !r.affordable) continue;
            if (r.cost >= 0 && s >= 0) {
                if (s < r.cost) continue; // nâng tường sẽ ăn vào phần để dành
                if (eco != null) eco.wallRes = payRes;
                return r;
            }
            if (mf >= wallFill) return r; // không rõ giá / phần thừa → kho sắp đầy thì xả
        }
        return null;
    }

    private static float maxFill(float[] fill) {
        float m = -1;
        for (int i = 1; i < fill.length; i++) m = Math.max(m, fill[i]);
        return m;
    }
}

