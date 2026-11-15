package com.code02.autofarm.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lộ trình TH16 → TH17 (theo autofarm/docs/lo-trinh-TH16-TH17.txt).
 *
 * - TH16 = giai đoạn A "TH16 cuối" (mục 3): công trình tấn công trước, rồi tướng (Nữ hoàng / Cận vệ /
 *   Nhà vô địch, sau đó Vua / Công tước rồng / Hoàng tử), phòng thủ ưu tiên cao (Đại bàng, Nguyên khối,
 *   Tháp phép, Tháp đa năng), vừa (Tán xạ, Tháp lửa, Nỏ lớn), thấp (còn lại), tường cuối cùng.
 * - TH17 = giai đoạn B–E (mục 4, 5): Lab + tướng chủ lực, Lâu đài + Trại quân, Sảnh anh hùng, Xưởng phép,
 *   Firespitter + Bom Giga, các tướng còn lại MỖI LẦN MỘT, phòng thủ lan/lõi, phòng không, phần còn lại, tường.
 * - Nhà chính: chỉ khi cho phép trong cài đặt; ở TH16 còn phải đánh dấu Đại bàng cấp 7.
 * - Tướng: không bắt đầu nâng nếu số tướng đang nâng (bot ghi nhận qua recordUpgrade) đã đủ giới hạn.
 * - Tường: chỉ để xả tài nguyên thừa (xem Roadmap.pick).
 *
 * Tiến độ lưu trong Memory với tiền tố "plan.".
 */
public final class PlanTH17 {
    private PlanTH17() {
    }

    // ---------------------------------------------------------------- khóa bộ nhớ

    private static final String K_TH = "plan.th";
    private static final String K_MAX_HEROES = "plan.maxHeroes";
    private static final String K_HEROES = "plan.heroes";
    private static final String K_HERO_HOURS = "plan.heroHours";
    private static final String K_MAIN = "plan.mainHero";
    private static final String K_LAST = "plan.last";
    private static final String K_FLAG = "plan.flag.";
    private static final String K_COUNT = "plan.n.";

    /** Thời gian nâng một tướng ước tính (giờ) nếu chưa chỉnh. Ước dư cho an toàn: thà chờ còn hơn quá số tướng. */
    public static final int DEFAULT_HERO_HOURS = 168;

    // Mục người dùng tự đánh dấu (bot không tự biết được).
    public static final String FLAG_EAGLE7 = "eagle7";
    public static final String FLAG_EQUIP16 = "equip16";
    public static final String FLAG_PETS16 = "pets16";
    public static final String FLAG_ARMY16 = "army16";
    public static final String FLAG_BOOK = "book";
    public static final String FLAG_HELPERS = "helpers";
    public static final String FLAG_PETS17 = "pets17";
    public static final String FLAG_ARMY17 = "army17";
    public static final String FLAG_TH18 = "th18";

    /** {khóa, nhãn} của các mục tự đánh dấu, để app hiện ô tích. */
    public static final String[][] MANUAL_FLAGS = {
            {FLAG_EAGLE7, "Eagle Artillery (Pháo đại bàng) cấp 7"},
            {FLAG_EQUIP16, "Fireball 18+ / Frozen Arrow 15+"},
            {FLAG_PETS16, "Unicorn, Phoenix, Spirit Fox"},
            {FLAG_ARMY16, "Đội hình chính max Lab TH16"},
            {FLAG_BOOK, "Có Book of Building cho Nhà chính"},
            {FLAG_HELPERS, "Gắn Apprentice + Lab Assistant"},
            {FLAG_PETS17, "Sneezy / pet tướng chính"},
            {FLAG_ARMY17, "Quân chính max Lab TH17"},
            {FLAG_TH18, "Cân nhắc TH18"},
    };

    // ---------------------------------------------------------------- bảng ưu tiên

    private static final Map<PlanItem, int[]> T16 = new EnumMap<>(PlanItem.class);
    private static final Map<PlanItem, int[]> T17 = new EnumMap<>(PlanItem.class);
    /** Nhóm "cốt lõi" phải xong trước khi được nâng Nhà chính (TH16: tới phòng thủ cao; TH17: tới công trình quân). */
    private static final int CORE16 = 4;
    private static final int CORE17 = 6;
    private static final int LAST16 = 8;
    private static final int LAST17 = 11;

    private static final String[] TIERS16 = {
            "Tấn công chính (Phòng thí nghiệm, Trại quân, Lâu đài, Sảnh anh hùng)",
            "Công trình quân (Lò rèn, Nhà lính, Xưởng phép, Xưởng công thành, Nhà thú cưng)",
            "Tướng chính (Nữ hoàng, Cận vệ, Nhà vô địch)",
            "Tướng phụ (Vua, Công tước rồng, Hoàng tử)",
            "Phòng thủ cao (Đại bàng, Nguyên khối, Tháp phép, Tháp đa năng)",
            "Phòng thủ vừa (Tán xạ, Tháp lửa, Nỏ lớn, Chế tạo)",
            "Phòng thủ còn lại",
            "Bẫy",
            "Kho, mỏ, khác",
    };
    private static final String[] TIERS17 = {
            "Phòng thí nghiệm + tướng chủ lực",
            "Lâu đài + Trại quân",
            "Sảnh anh hùng",
            "Xưởng phép + Xưởng phép đen",
            "Firespitter + Bom Giga",
            "Tướng còn lại, mỗi lần một (Cận vệ, Nhà vô địch, Công tước rồng, Vua, Hoàng tử)",
            "Nhà thú cưng, Lò rèn, Chế tạo, Pháo hỏa ngục, nhà lính",
            "Phòng thủ lan / lõi",
            "Phòng không",
            "Bẫy",
            "Phòng thủ rìa (Đại bác, Tháp cung thủ, Súng cối)",
            "Kho, mỏ, khác",
    };

    private static void tier(Map<PlanItem, int[]> t, int tier, PlanItem... items) {
        for (int i = 0; i < items.length; i++) t.put(items[i], new int[]{tier, i});
    }

    static {
        // Giai đoạn A (TH16 cuối) – mục 3 của file lộ trình.
        tier(T16, 0, PlanItem.LAB, PlanItem.ARMY_CAMP, PlanItem.CLAN_CASTLE, PlanItem.HERO_HALL);
        tier(T16, 1, PlanItem.BLACKSMITH, PlanItem.BARRACKS, PlanItem.DARK_BARRACKS, PlanItem.SPELL_FACTORY,
                PlanItem.DARK_SPELL_FACTORY, PlanItem.WORKSHOP, PlanItem.PET_HOUSE, PlanItem.GENERIC_ARMY);
        tier(T16, 2, PlanItem.ARCHER_QUEEN, PlanItem.GRAND_WARDEN, PlanItem.ROYAL_CHAMPION);
        tier(T16, 3, PlanItem.BARBARIAN_KING, PlanItem.DRAGON_DUKE, PlanItem.MINION_PRINCE, PlanItem.HERO_OTHER);
        tier(T16, 4, PlanItem.EAGLE_ARTILLERY, PlanItem.MONOLITH, PlanItem.SPELL_TOWER, PlanItem.MULTI_GEAR_TOWER,
                PlanItem.FIRESPITTER, PlanItem.INFERNO_ARTILLERY);
        tier(T16, 5, PlanItem.SCATTERSHOT, PlanItem.INFERNO_TOWER, PlanItem.X_BOW, PlanItem.CRAFTING_STATION);
        tier(T16, 6, PlanItem.WIZARD_TOWER, PlanItem.BOMB_TOWER, PlanItem.AIR_DEFENSE, PlanItem.MULTI_ARCHER_TOWER,
                PlanItem.RICOCHET_CANNON, PlanItem.HIDDEN_TESLA, PlanItem.AIR_SWEEPER, PlanItem.GENERIC_SPLASH,
                PlanItem.GENERIC_AIR, PlanItem.ARCHER_TOWER, PlanItem.CANNON, PlanItem.MORTAR, PlanItem.GENERIC_POINT,
                PlanItem.BUILDERS_HUT);
        tier(T16, 7, PlanItem.GIGA_BOMB, PlanItem.AIR_SEEKING_MINE, PlanItem.GIANT_BOMB, PlanItem.BOMB,
                PlanItem.AIR_BOMB, PlanItem.SPRING_TRAP, PlanItem.SKELETON_TRAP, PlanItem.TORNADO_TRAP,
                PlanItem.OTHER_TRAP);
        tier(T16, LAST16, PlanItem.HELPER_HUT, PlanItem.STORAGE, PlanItem.COLLECTOR, PlanItem.UNKNOWN);

        // Giai đoạn B–E (TH17) – mục 4 và 5. Tướng chủ lực được đẩy lên nhóm 0 (xem priority()).
        tier(T17, 0, PlanItem.LAB);
        tier(T17, 1, PlanItem.CLAN_CASTLE, PlanItem.ARMY_CAMP);
        tier(T17, 2, PlanItem.HERO_HALL);
        tier(T17, 3, PlanItem.SPELL_FACTORY, PlanItem.DARK_SPELL_FACTORY);
        tier(T17, 4, PlanItem.FIRESPITTER, PlanItem.GIGA_BOMB);
        tier(T17, 5, PlanItem.ARCHER_QUEEN, PlanItem.GRAND_WARDEN, PlanItem.ROYAL_CHAMPION, PlanItem.DRAGON_DUKE,
                PlanItem.BARBARIAN_KING, PlanItem.MINION_PRINCE, PlanItem.HERO_OTHER);
        tier(T17, 6, PlanItem.PET_HOUSE, PlanItem.BLACKSMITH, PlanItem.CRAFTING_STATION, PlanItem.INFERNO_ARTILLERY,
                PlanItem.BARRACKS, PlanItem.DARK_BARRACKS, PlanItem.WORKSHOP, PlanItem.GENERIC_ARMY);
        tier(T17, 7, PlanItem.MONOLITH, PlanItem.SPELL_TOWER, PlanItem.MULTI_GEAR_TOWER, PlanItem.SCATTERSHOT,
                PlanItem.INFERNO_TOWER, PlanItem.X_BOW, PlanItem.WIZARD_TOWER, PlanItem.BOMB_TOWER,
                PlanItem.EAGLE_ARTILLERY, PlanItem.GENERIC_SPLASH);
        tier(T17, 8, PlanItem.AIR_DEFENSE, PlanItem.MULTI_ARCHER_TOWER, PlanItem.RICOCHET_CANNON,
                PlanItem.AIR_SWEEPER, PlanItem.HIDDEN_TESLA, PlanItem.GENERIC_AIR);
        tier(T17, 9, PlanItem.AIR_SEEKING_MINE, PlanItem.GIANT_BOMB, PlanItem.BOMB, PlanItem.AIR_BOMB,
                PlanItem.SPRING_TRAP, PlanItem.SKELETON_TRAP, PlanItem.TORNADO_TRAP, PlanItem.OTHER_TRAP);
        tier(T17, 10, PlanItem.CANNON, PlanItem.ARCHER_TOWER, PlanItem.MORTAR, PlanItem.GENERIC_POINT,
                PlanItem.BUILDERS_HUT);
        tier(T17, LAST17, PlanItem.HELPER_HUT, PlanItem.STORAGE, PlanItem.COLLECTOR, PlanItem.UNKNOWN);
    }

    /**
     * Thứ hạng {nhóm, thứ tự trong nhóm} của một công trình ở cấp Nhà chính đã cho (nhỏ = ưu tiên hơn).
     * Nhà chính trả về nhóm -1 (chỉ dùng khi đã được phép), tường nhóm 99 (không nằm trong lộ trình).
     */
    public static int[] priority(PlanItem it, int townHall, Memory mem) {
        if (it == PlanItem.TOWN_HALL) return new int[]{-1, 0};
        if (it == PlanItem.WALL) return new int[]{99, 0};
        if (townHall >= 17) {
            if (it == mainHero(mem)) return new int[]{0, 1};
            int n = count(mem, 17, it);
            // Firespitter: đặt mới + cấp 2 sớm (giai đoạn B, C), sau đó nằm cùng phòng thủ lõi.
            if (it == PlanItem.FIRESPITTER && n >= 2) return new int[]{7, -1};
            // Bom Giga: đặt sớm, các cấp sau nằm cùng bẫy.
            if (it == PlanItem.GIGA_BOMB && n >= 1) return new int[]{9, -1};
            // Phòng thủ chế tạo: máu x3 + Hero Hunter rồi dừng.
            if (it == PlanItem.CRAFTING_STATION && n >= 4) return new int[]{LAST17, 9};
            int[] p = T17.get(it);
            return p == null ? new int[]{LAST17, 9} : p;
        }
        if (it == PlanItem.CRAFTING_STATION && count(mem, 16, it) >= 4) return new int[]{LAST16, 9};
        int[] p = T16.get(it);
        return p == null ? new int[]{LAST16, 9} : p;
    }

    // ---------------------------------------------------------------- chọn mục nâng cấp

    /** Chọn mục nâng cấp (kể cả xả tường) theo lộ trình TH16 → TH17. null nếu nên để dành / không có gì. */
    public static Roadmap.Row choose(List<Roadmap.Row> rows, Settings cfg, float[] fill, Memory mem, long nowMs) {
        return choose(rows, cfg, fill, mem, nowMs, null);
    }

    /** Như trên, thêm thông tin kinh tế (giá, tài nguyên, tốc độ farm) để tối ưu thời gian thợ và tài nguyên. */
    public static Roadmap.Row choose(List<Roadmap.Row> rows, Settings cfg, float[] fill, Memory mem, long nowMs,
                                     Roadmap.Economy eco) {
        return choose(rows, cfg, fill, mem, nowMs, 0.95f, wallFill(cfg), eco);
    }

    /** Bản đầy đủ: tự đặt ngưỡng tràn kho và ngưỡng xả tường (≤ 0 = tắt tường). */
    public static Roadmap.Row choose(List<Roadmap.Row> rows, Settings cfg, float[] fill, Memory mem, long nowMs,
                                     float overflow, float wallFill, Roadmap.Economy eco) {
        if (rows == null || rows.isEmpty()) return null;
        remember(cfg, mem);
        return Roadmap.pick(rows, ranking(rows, cfg, mem, nowMs), fill, overflow, wallFill, eco);
    }

    /** Chỉ xét tường (vd. khi hết thợ rảnh: tường nâng xong ngay, không tốn thợ). */
    public static Roadmap.Row chooseWall(List<Roadmap.Row> rows, Settings cfg, float[] fill, Memory mem, long nowMs,
                                         float wallFill, Roadmap.Economy eco) {
        if (rows == null || rows.isEmpty()) return null;
        remember(cfg, mem);
        return Roadmap.pickWall(rows, ranking(rows, cfg, mem, nowMs), fill, wallFill, eco);
    }

    private static float wallFill(Settings cfg) {
        return cfg.wallPct > 0 ? cfg.wallPct / 100f : -1;
    }

    private static void remember(Settings cfg, Memory mem) {
        if (mem == null) return;
        String th = String.valueOf(cfg.townHall);
        if (!th.equals(mem.get(K_TH))) mem.put(K_TH, th);
        String mh = String.valueOf(cfg.maxHeroesBusy);
        if (!mh.equals(mem.get(K_MAX_HEROES))) mem.put(K_MAX_HEROES, mh);
    }

    private static Roadmap.Ranking ranking(List<Roadmap.Row> rows, Settings cfg, Memory mem, long nowMs) {
        final int th = cfg.townHall;
        final Map<Roadmap.Row, PlanItem> items = new IdentityHashMap<>();
        for (Roadmap.Row r : rows) items.put(r, PlanItem.of(r));
        final PlanItem main = mainHero(mem);
        final int busy = heroesBusy(mem, nowMs);
        final int busyOthers = heroesBusyExcept(mem, nowMs, main);
        final boolean thOk = townHallAllowed(rows, items, cfg, mem);
        return new Roadmap.Ranking() {
            private int[] prio(Roadmap.Row r) {
                return priority(items.get(r), th, mem);
            }

            @Override
            public boolean eligible(Roadmap.Row r) {
                PlanItem it = items.get(r);
                if (it == null) it = PlanItem.of(r);
                if (it == PlanItem.WALL) return false;
                if (it == PlanItem.TOWN_HALL) return thOk;
                if (it.hero) {
                    if (busy >= cfg.maxHeroesBusy) return false;
                    // TH17: các tướng ngoài tướng chủ lực nâng MỖI LẦN MỘT.
                    if (th >= 17 && it != main && busyOthers > 0) return false;
                }
                return true;
            }

            @Override
            public int group(Roadmap.Row r) {
                return prio(r)[0];
            }

            @Override
            public float base(Roadmap.Row r) {
                int[] p = prio(r);
                return p[0] * 10f + p[1];
            }

            @Override
            public float overflowBonus() {
                return 30f;
            }

            @Override
            public float fillWeight() {
                return 2f;
            }
        };
    }

    /** Nhà chính: cài đặt cho phép; TH16 cần Đại bàng cấp 7; và các mục cốt lõi không còn trong danh sách. */
    private static boolean townHallAllowed(List<Roadmap.Row> rows, Map<Roadmap.Row, PlanItem> items, Settings cfg,
                                           Memory mem) {
        if (!cfg.allowTownHall) return false;
        int th = cfg.townHall;
        if (th < 17 && !eagleMaxed(mem)) return false;
        int core = th >= 17 ? CORE17 : CORE16;
        for (Roadmap.Row r : rows) {
            PlanItem it = items.get(r);
            if (it == PlanItem.TOWN_HALL || it == PlanItem.WALL) continue;
            if (priority(it, th, mem)[0] <= core) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- ghi nhận

    /** Gọi sau mỗi lần nâng cấp thành công (kể cả tường). */
    public static void recordUpgrade(Memory mem, Roadmap.Row row, long nowMs) {
        if (mem == null || row == null) return;
        PlanItem it = PlanItem.of(row);
        int th = townHall(mem);
        String k = K_COUNT + th + "." + it.name();
        mem.put(k, String.valueOf(getInt(mem, k, 0) + 1));
        mem.put(K_LAST, row.name);
        if (it.hero) {
            long end = nowMs + heroHours(mem) * 3_600_000L;
            StringBuilder sb = new StringBuilder();
            String log = mem.get(K_HEROES);
            if (log != null) {
                for (String e : log.split(";")) {
                    String[] p = e.split("@");
                    if (p.length == 3 && parseLong(p[2], 0) > nowMs) sb.append(e).append(';');
                }
            }
            sb.append(it.name()).append('@').append(nowMs).append('@').append(end);
            mem.put(K_HEROES, sb.toString());
        }
    }

    /** Số tướng đang nâng (theo lịch sử bot đã ghi nhận và thời gian nâng ước tính). */
    public static int heroesBusy(Memory mem, long nowMs) {
        return heroesBusyExcept(mem, nowMs, null);
    }

    private static int heroesBusyExcept(Memory mem, long nowMs, PlanItem except) {
        String log = mem == null ? null : mem.get(K_HEROES);
        if (log == null || log.isEmpty()) return 0;
        int n = 0;
        for (String e : log.split(";")) {
            String[] p = e.split("@");
            if (p.length != 3) continue;
            if (except != null && p[0].equals(except.name())) continue;
            if (parseLong(p[2], 0) > nowMs) n++;
        }
        return n;
    }

    /** Số lần bot đã nâng một công trình ở cấp Nhà chính đã cho. */
    public static int count(Memory mem, int townHall, PlanItem it) {
        return mem == null ? 0 : getInt(mem, K_COUNT + townHall + "." + it.name(), 0);
    }

    /** Thời gian nâng một tướng ước tính (giờ). */
    public static int heroHours(Memory mem) {
        return Math.max(1, getInt(mem, K_HERO_HOURS, DEFAULT_HERO_HOURS));
    }

    public static void setHeroHours(Memory mem, int hours) {
        mem.put(K_HERO_HOURS, String.valueOf(Math.max(1, hours)));
    }

    /** Tướng chủ lực ở TH17 (mặc định Nữ hoàng cung thủ). */
    public static PlanItem mainHero(Memory mem) {
        String s = mem == null ? null : mem.get(K_MAIN);
        if (s != null) {
            try {
                PlanItem it = PlanItem.valueOf(s);
                if (it.hero) return it;
            } catch (IllegalArgumentException ignored) {
                // giá trị cũ / sai → dùng mặc định
            }
        }
        return PlanItem.ARCHER_QUEEN;
    }

    public static void setMainHero(Memory mem, PlanItem hero) {
        if (hero != null && hero.hero) mem.put(K_MAIN, hero.name());
    }

    public static boolean flag(Memory mem, String key) {
        return mem != null && "1".equals(mem.get(K_FLAG + key));
    }

    public static void setFlag(Memory mem, String key, boolean on) {
        mem.put(K_FLAG + key, on ? "1" : "0");
    }

    /** Đại bàng đã cấp 7 (người dùng đánh dấu; lên TH17 rồi thì chắc chắn đã xong). */
    public static boolean eagleMaxed(Memory mem) {
        return flag(mem, FLAG_EAGLE7) || townHall(mem) >= 17;
    }

    public static void setEagleMaxed(Memory mem, boolean on) {
        setFlag(mem, FLAG_EAGLE7, on);
    }

    /** Cấp Nhà chính bot ghi nhận lần gần nhất (từ cài đặt khi chọn), mặc định 16. */
    public static int townHall(Memory mem) {
        return getInt(mem, K_TH, 16);
    }

    // ---------------------------------------------------------------- giai đoạn & checklist

    private static final class Req {
        final PlanItem item; // null = tướng chủ lực
        final int n;

        Req(PlanItem item, int n) {
            this.item = item;
            this.n = n;
        }
    }

    private static final class Line {
        final String label;
        final String flag;
        final Req[] reqs;

        Line(String label, String flag, Req... reqs) {
            this.label = label;
            this.flag = flag;
            this.reqs = reqs;
        }
    }

    private static final class Section {
        final String title;
        final String phase;
        final int th;
        final Line[] lines;

        Section(String title, String phase, int th, Line... lines) {
            this.title = title;
            this.phase = phase;
            this.th = th;
            this.lines = lines;
        }
    }

    private static Line auto(String label, Object... reqs) {
        Req[] r = new Req[reqs.length / 2];
        for (int i = 0; i < r.length; i++) r[i] = new Req((PlanItem) reqs[2 * i], (Integer) reqs[2 * i + 1]);
        return new Line(label, null, r);
    }

    private static Line manual(String label, String flag) {
        return new Line(label, flag);
    }

    /** Bảng checklist tổng – mục 17 của file lộ trình. */
    private static final Section[] SECTIONS = {
            new Section("TRƯỚC KHI LÊN TH17", "A", 16,
                    auto("Laboratory max TH16", PlanItem.LAB, 1),
                    auto("4 Army Camps max", PlanItem.ARMY_CAMP, 4),
                    auto("Clan Castle max", PlanItem.CLAN_CASTLE, 1),
                    auto("Hero Hall 10", PlanItem.HERO_HALL, 1),
                    auto("Blacksmith max", PlanItem.BLACKSMITH, 1),
                    auto("Pet House max", PlanItem.PET_HOUSE, 1),
                    auto("Queen / Warden / RC gần max", PlanItem.ARCHER_QUEEN, 1, PlanItem.GRAND_WARDEN, 1,
                            PlanItem.ROYAL_CHAMPION, 1),
                    manual("Fireball 18+ / Frozen Arrow 15+", FLAG_EQUIP16),
                    manual("Unicorn, Phoenix, Spirit Fox", FLAG_PETS16),
                    auto("Monolith, Spell Towers, Multi-Gear", PlanItem.MONOLITH, 1, PlanItem.SPELL_TOWER, 2,
                            PlanItem.MULTI_GEAR_TOWER, 1),
                    manual("EAGLE ARTILLERY CẤP 7 (bắt buộc)", FLAG_EAGLE7),
                    manual("Đội hình chính max Lab", FLAG_ARMY16),
                    manual("Có Book of Building cho Town Hall", FLAG_BOOK),
                    auto("Đổ hết tài nguyên thừa vào tường", PlanItem.WALL, 1)),
            new Section("TUẦN ĐẦU TH17", "B", 17,
                    auto("Laboratory", PlanItem.LAB, 1),
                    auto("Tướng chủ lực", null, 1),
                    auto("Clan Castle", PlanItem.CLAN_CASTLE, 1),
                    auto("Army Camp", PlanItem.ARMY_CAMP, 1),
                    auto("Hero Hall 11", PlanItem.HERO_HALL, 1),
                    auto("Đặt Firespitter", PlanItem.FIRESPITTER, 1),
                    auto("Đặt Giga Bomb", PlanItem.GIGA_BOMB, 1),
                    manual("Gắn Apprentice + Lab Assistant", FLAG_HELPERS)),
            new Section("THÁNG 1-2", "C", 17,
                    auto("Army Camps còn lại", PlanItem.ARMY_CAMP, 4),
                    auto("Spell Factory + Dark Spell Factory", PlanItem.SPELL_FACTORY, 1,
                            PlanItem.DARK_SPELL_FACTORY, 1),
                    auto("Grand Warden", PlanItem.GRAND_WARDEN, 1),
                    auto("Royal Champion", PlanItem.ROYAL_CHAMPION, 1),
                    manual("Sneezy / pet tướng chính", FLAG_PETS17),
                    auto("Crafted Defense: máu x3 + Hero Hunter", PlanItem.CRAFTING_STATION, 4),
                    auto("Inferno Artillery", PlanItem.INFERNO_ARTILLERY, 1),
                    auto("Firespitter cấp 2", PlanItem.FIRESPITTER, 2)),
            new Section("THÁNG 3-6", "D", 17,
                    manual("Quân chính max Lab TH17", FLAG_ARMY17),
                    auto("Dragon Duke / King / Minion Prince", PlanItem.DRAGON_DUKE, 1, PlanItem.BARBARIAN_KING, 1,
                            PlanItem.MINION_PRINCE, 1),
                    auto("Scattershot, Inferno, Air Defense", PlanItem.SCATTERSHOT, 1, PlanItem.INFERNO_TOWER, 1,
                            PlanItem.AIR_DEFENSE, 1),
                    auto("Wizard Tower, Bomber Tower", PlanItem.WIZARD_TOWER, 1, PlanItem.BOMB_TOWER, 1),
                    auto("Multi-Archer, Ricochet", PlanItem.MULTI_ARCHER_TOWER, 1, PlanItem.RICOCHET_CANNON, 1),
                    auto("Bẫy (Giga Bomb, Air Seeking Mine...)", PlanItem.GIGA_BOMB, 2, PlanItem.AIR_SEEKING_MINE, 1)),
            new Section("CUỐI TH17", "E", 17,
                    auto("Phòng thủ rìa", PlanItem.CANNON, 1, PlanItem.ARCHER_TOWER, 1, PlanItem.MORTAR, 1),
                    auto("Tường", PlanItem.WALL, 1),
                    manual("Cân nhắc TH18", FLAG_TH18)),
    };

    private static PlanItem reqItem(Req q, Memory mem) {
        return q.item == null ? mainHero(mem) : q.item;
    }

    private static boolean done(Line l, int th, Memory mem) {
        if (l.flag != null) return l.flag.equals(FLAG_EAGLE7) ? eagleMaxed(mem) : flag(mem, l.flag);
        for (Req q : l.reqs) if (count(mem, th, reqItem(q, mem)) < q.n) return false;
        return true;
    }

    /** Ký tự giai đoạn hiện tại: A (TH16 cuối), B, C, D, E (TH17). */
    public static String phaseCode(int townHall, Memory mem) {
        if (townHall < 17) return "A";
        for (Section s : SECTIONS) {
            if (s.th < 17 || s.phase.equals("E")) continue;
            for (Line l : s.lines) if (l.flag == null && !done(l, s.th, mem)) return s.phase;
        }
        return "E";
    }

    /** Tên giai đoạn hiện tại (tiếng Việt). */
    public static String phaseName(int townHall, Memory mem) {
        switch (phaseCode(townHall, mem)) {
            case "B":
                return "Giai đoạn B: tuần 1-2 ở TH17";
            case "C":
                return "Giai đoạn C: tháng 1-2 ở TH17";
            case "D":
                return "Giai đoạn D: tháng 3-6 ở TH17";
            case "E":
                return "Giai đoạn E: cuối TH17";
            default:
                return townHall < 16 ? "TH" + townHall + " (dùng thứ tự giai đoạn A)"
                        : "Giai đoạn A: TH16 cuối (trước khi bấm lên TH17)";
        }
    }

    /** Mục tiêu số lần nâng của một công trình theo checklist của cấp Nhà chính (mặc định 1). */
    private static int target(PlanItem it, int th, Memory mem) {
        int t = 1;
        for (Section s : SECTIONS) {
            if ((th >= 17) != (s.th >= 17)) continue;
            for (Line l : s.lines) for (Req q : l.reqs) if (reqItem(q, mem) == it) t = Math.max(t, q.n);
        }
        return t;
    }

    private static boolean exists(PlanItem it, int th) {
        switch (it) {
            case WALL:
            case TOWN_HALL:
            case UNKNOWN:
            case GENERIC_ARMY:
            case HERO_OTHER:
            case GENERIC_SPLASH:
            case GENERIC_AIR:
            case GENERIC_POINT:
            case OTHER_TRAP:
                return false;
            case FIRESPITTER:
            case GIGA_BOMB:
            case INFERNO_ARTILLERY:
                return th >= 17; // công trình mới của TH17
            case EAGLE_ARTILLERY:
                return th < 17; // đã gộp vào Nhà chính ở TH17
            default:
                return true;
        }
    }

    /** Các công trình theo đúng thứ tự ưu tiên của cấp Nhà chính (bỏ tường, Nhà chính, nhóm chung). */
    public static List<PlanItem> order(int townHall, Memory mem) {
        List<PlanItem> l = new ArrayList<>();
        for (PlanItem it : PlanItem.values()) if (exists(it, townHall)) l.add(it);
        l.sort((a, b) -> {
            int[] pa = priority(a, townHall, mem);
            int[] pb = priority(b, townHall, mem);
            return pa[0] != pb[0] ? Integer.compare(pa[0], pb[0]) : Integer.compare(pa[1], pb[1]);
        });
        return l;
    }

    /** n mục nên nâng tiếp theo (chưa đạt checklist, tướng chỉ khi còn chỗ nâng). */
    public static List<PlanItem> recommend(Memory mem, int townHall, long nowMs, int n) {
        int maxHeroes = getInt(mem, K_MAX_HEROES, 2);
        PlanItem main = mainHero(mem);
        int busy = heroesBusy(mem, nowMs);
        int busyOthers = heroesBusyExcept(mem, nowMs, main);
        List<PlanItem> out = new ArrayList<>();
        for (PlanItem it : order(townHall, mem)) {
            if (out.size() >= n) break;
            if (it.hero && (busy >= maxHeroes || (townHall >= 17 && it != main && busyOthers > 0))) continue;
            if (count(mem, townHall, it) >= target(it, townHall, mem)) continue;
            out.add(it);
        }
        return out;
    }

    /** Checklist tiến độ (văn bản nhiều dòng, tiếng Việt) để hiện trong app. */
    public static String checklist(Memory m) {
        return checklist(m, System.currentTimeMillis());
    }

    /** Như trên, với thời điểm hiện tại cho trước (để tính tướng đang nâng). */
    public static String checklist(Memory m, long nowMs) {
        if (m == null) m = Memory.inRam();
        int th = townHall(m);
        StringBuilder sb = new StringBuilder();
        sb.append("LỘ TRÌNH TH16 → TH17\n");
        sb.append("Nhà chính: ").append(th).append(" | ").append(phaseName(th, m)).append('\n');
        sb.append("Tướng đang nâng (bot ghi nhận): ").append(heroesBusy(m, nowMs)).append('/')
                .append(getInt(m, K_MAX_HEROES, 2)).append(" | tướng chủ lực: ").append(mainHero(m).vi).append('\n');
        String last = m.get(K_LAST);
        if (last != null) sb.append("Nâng gần nhất: ").append(last).append('\n');
        for (Section s : SECTIONS) {
            sb.append('\n').append(s.title).append('\n');
            for (Line l : s.lines) {
                boolean ok = done(l, s.th, m);
                sb.append("  ").append(ok ? "✓ " : "☐ ").append(l.label);
                if (l.flag != null) {
                    if (!ok) sb.append(" (tự đánh dấu)");
                } else {
                    int have = 0;
                    int need = 0;
                    for (Req q : l.reqs) {
                        have += Math.min(count(m, s.th, reqItem(q, m)), q.n);
                        need += q.n;
                    }
                    if (!ok || need > 1) sb.append(" (").append(have).append('/').append(need).append(')');
                }
                sb.append('\n');
            }
        }
        sb.append("\nGỢI Ý TIẾP THEO\n");
        List<PlanItem> next = recommend(m, th, nowMs, 3);
        if (next.isEmpty()) sb.append("  (đã đạt hết checklist bot theo dõi được: xả tài nguyên thừa vào tường)\n");
        for (int i = 0; i < next.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(next.get(i).vi).append('\n');
        }
        sb.append("\n✓ = bot đã ghi nhận đủ số lần nâng (hoặc bạn đã tự đánh dấu). Bot không đọc được cấp,\n")
                .append("  nên hãy kiểm tra lại trong game.");
        return sb.toString();
    }

    /** Mô tả ngắn thứ tự ưu tiên của một cấp Nhà chính. */
    public static String describe(int townHall) {
        String[] tiers = townHall >= 17 ? TIERS17 : TIERS16;
        StringBuilder sb = new StringBuilder(townHall >= 17 ? "TH17 (giai đoạn B–E): " : "TH16 (giai đoạn A): ");
        for (int i = 0; i < tiers.length; i++) {
            if (i > 0) sb.append(" → ");
            sb.append(tiers[i]);
        }
        sb.append(" → Tường (chỉ xả tài nguyên thừa). Nhà chính: chỉ khi cho phép")
                .append(townHall >= 17 ? "" : " và Đại bàng cấp 7").append('.');
        return sb.toString();
    }

    // ---------------------------------------------------------------- tiện ích

    private static int getInt(Memory mem, String key, int def) {
        if (mem == null) return def;
        return (int) parseLong(mem.get(key), def);
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
