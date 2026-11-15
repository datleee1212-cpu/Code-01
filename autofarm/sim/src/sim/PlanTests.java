package sim;

import com.code02.autofarm.core.Memory;
import com.code02.autofarm.core.PlanItem;
import com.code02.autofarm.core.PlanTH17;
import com.code02.autofarm.core.Roadmap;
import com.code02.autofarm.core.Roadmap.Cat;
import com.code02.autofarm.core.Roadmap.Economy;
import com.code02.autofarm.core.Roadmap.Row;
import com.code02.autofarm.core.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Kiểm thử lộ trình TH16 → TH17: nhận biết tên, thứ tự ưu tiên theo giai đoạn, luật Nhà chính,
 * giới hạn tướng, để dành, xả tường, tối ưu thời gian thợ, checklist, và chạy thật trên game giả lập.
 * Thoát với mã 1 nếu có mục không đạt.
 */
public final class PlanTests {

    private static int failures = 0;
    private static int passed = 0;
    private static final StringBuilder report = new StringBuilder();
    private static final long H = 3_600_000L;

    private static void check(String group, boolean ok, String what) {
        report.append(ok ? "   ✓ " : "   ✗ ").append(what).append('\n');
        if (ok) passed++;
        else {
            failures++;
            System.out.println("FAIL [" + group + "] " + what);
        }
    }

    private static void header(String name) {
        report.append("\n▶ ").append(name).append('\n');
    }

    // ---------------------------------------------------------------- tiện ích dựng dòng

    private static int idx = 0;

    private static Row row(String name, boolean affordable, int res) {
        return row(name, affordable, res, -1);
    }

    private static Row row(String name, boolean affordable, int res, long cost) {
        int i = idx++;
        return new Row(i / 5, i % 5, name, Roadmap.classify(name), affordable, res, cost);
    }

    private static float[] fill(float v) {
        return new float[]{-1, v, v, v};
    }

    private static Settings cfg(int th) {
        Settings s = new Settings();
        s.townHall = th;
        s.wallPct = 90;
        s.maxHeroesBusy = 2;
        return s;
    }

    private static PlanItem item(Row r) {
        return r == null ? null : PlanItem.of(r);
    }

    /** Chọn liên tục (mỗi lần bỏ dòng đã chọn) → thứ tự nâng. */
    private static List<PlanItem> sequence(List<Row> rows, Settings c, Memory m, boolean record) {
        List<Row> left = new ArrayList<>(rows);
        List<PlanItem> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row p = PlanTH17.choose(left, c, fill(0.5f), m, 0L);
            if (p == null) break;
            out.add(PlanItem.of(p));
            if (record) PlanTH17.recordUpgrade(m, p, 0L);
            left.remove(p);
        }
        return out;
    }

    private static boolean before(List<PlanItem> seq, PlanItem... chain) {
        int last = -1;
        for (PlanItem it : chain) {
            int i = seq.indexOf(it);
            if (i < 0 || i < last) return false;
            last = i;
        }
        return true;
    }

    // ---------------------------------------------------------------- 1. nhận biết tên

    static void tClassify() {
        String n = "1. Nhận biết công trình TH16/TH17 (Anh + Việt, bỏ dấu, đọc sai nhẹ)";
        header(n);
        Object[][] cases = {
                {"Laboratory", PlanItem.LAB}, {"Phòng Thí Nghiệm", PlanItem.LAB},
                {"Army Camp", PlanItem.ARMY_CAMP}, {"Trại quân", PlanItem.ARMY_CAMP},
                {"Clan Castle", PlanItem.CLAN_CASTLE}, {"Lâu đài bang hội", PlanItem.CLAN_CASTLE},
                {"Hero Hall", PlanItem.HERO_HALL}, {"Sảnh Anh Hùng", PlanItem.HERO_HALL},
                {"Blacksmith", PlanItem.BLACKSMITH}, {"Lò rèn", PlanItem.BLACKSMITH},
                {"Barracks", PlanItem.BARRACKS}, {"Nhà lính", PlanItem.BARRACKS},
                {"Dark Barracks", PlanItem.DARK_BARRACKS}, {"Nhà lính đen", PlanItem.DARK_BARRACKS},
                {"Spell Factory", PlanItem.SPELL_FACTORY}, {"Xưởng phép", PlanItem.SPELL_FACTORY},
                {"Dark Spell Factory", PlanItem.DARK_SPELL_FACTORY}, {"Xưởng phép đen", PlanItem.DARK_SPELL_FACTORY},
                {"Workshop", PlanItem.WORKSHOP}, {"Siege Workshop", PlanItem.WORKSHOP},
                {"Pet House", PlanItem.PET_HOUSE}, {"Nhà thú cưng", PlanItem.PET_HOUSE},
                {"Archer Queen", PlanItem.ARCHER_QUEEN}, {"Nữ hoàng cung thủ", PlanItem.ARCHER_QUEEN},
                {"Grand Warden", PlanItem.GRAND_WARDEN}, {"Hiệp sĩ", PlanItem.GRAND_WARDEN},
                {"Cẩn vệ", PlanItem.GRAND_WARDEN}, {"Cận vệ", PlanItem.GRAND_WARDEN},
                {"Royal Champion", PlanItem.ROYAL_CHAMPION}, {"Nhà vô địch", PlanItem.ROYAL_CHAMPION},
                {"Barbarian King", PlanItem.BARBARIAN_KING}, {"Vua man rợ", PlanItem.BARBARIAN_KING},
                {"Dragon Duke", PlanItem.DRAGON_DUKE}, {"Công tước rồng", PlanItem.DRAGON_DUKE},
                {"Minion Prince", PlanItem.MINION_PRINCE}, {"Hoàng tử", PlanItem.MINION_PRINCE},
                {"Eagle Artillery", PlanItem.EAGLE_ARTILLERY}, {"Pháo Đại Bàng", PlanItem.EAGLE_ARTILLERY},
                {"Monolith", PlanItem.MONOLITH}, {"Nguyên khối", PlanItem.MONOLITH},
                {"Spell Tower", PlanItem.SPELL_TOWER}, {"Tháp Phép", PlanItem.SPELL_TOWER},
                {"Multi-Gear Tower", PlanItem.MULTI_GEAR_TOWER}, {"Tháp đa năng", PlanItem.MULTI_GEAR_TOWER},
                {"Scattershot", PlanItem.SCATTERSHOT}, {"Súng tán xạ", PlanItem.SCATTERSHOT},
                {"Inferno Tower", PlanItem.INFERNO_TOWER}, {"Tháp lửa", PlanItem.INFERNO_TOWER},
                {"X-Bow", PlanItem.X_BOW}, {"Nỏ lớn", PlanItem.X_BOW},
                {"Firespitter", PlanItem.FIRESPITTER}, {"Súng phun lửa", PlanItem.FIRESPITTER},
                {"Giga Bomb", PlanItem.GIGA_BOMB}, {"Bom Giga", PlanItem.GIGA_BOMB},
                {"Inferno Artillery", PlanItem.INFERNO_ARTILLERY}, {"Town Hall Weapon", PlanItem.INFERNO_ARTILLERY},
                {"Pháo hỏa ngục", PlanItem.INFERNO_ARTILLERY},
                {"Crafting Station", PlanItem.CRAFTING_STATION}, {"Crafted Defense", PlanItem.CRAFTING_STATION},
                {"Phòng thủ chế tạo", PlanItem.CRAFTING_STATION},
                {"Wizard Tower", PlanItem.WIZARD_TOWER}, {"Tháp phù thủy", PlanItem.WIZARD_TOWER},
                {"Bomb Tower", PlanItem.BOMB_TOWER}, {"Bomber Tower", PlanItem.BOMB_TOWER}, {"Tháp bom", PlanItem.BOMB_TOWER},
                {"Air Defense", PlanItem.AIR_DEFENSE}, {"Phòng không", PlanItem.AIR_DEFENSE},
                {"Multi-Archer Tower", PlanItem.MULTI_ARCHER_TOWER}, {"Tháp đa cung thủ", PlanItem.MULTI_ARCHER_TOWER},
                {"Ricochet Cannon", PlanItem.RICOCHET_CANNON}, {"Đại bác nảy", PlanItem.RICOCHET_CANNON},
                {"Archer Tower", PlanItem.ARCHER_TOWER}, {"Tháp cung thủ", PlanItem.ARCHER_TOWER},
                {"Cannon", PlanItem.CANNON}, {"Đại bác", PlanItem.CANNON},
                {"Mortar", PlanItem.MORTAR}, {"Súng cối", PlanItem.MORTAR},
                {"Air Sweeper", PlanItem.AIR_SWEEPER}, {"Máy thổi khí", PlanItem.AIR_SWEEPER},
                {"Hidden Tesla", PlanItem.HIDDEN_TESLA}, {"Tesla", PlanItem.HIDDEN_TESLA},
                {"Builder's Hut", PlanItem.BUILDERS_HUT}, {"Nhà thợ xây", PlanItem.BUILDERS_HUT},
                {"Air Seeking Mine", PlanItem.AIR_SEEKING_MINE}, {"Seeking Air Mine", PlanItem.AIR_SEEKING_MINE},
                {"Bomb", PlanItem.BOMB}, {"Bom", PlanItem.BOMB},
                {"Giant Bomb", PlanItem.GIANT_BOMB}, {"Bom lớn", PlanItem.GIANT_BOMB},
                {"Air Bomb", PlanItem.AIR_BOMB}, {"Bom trên không", PlanItem.AIR_BOMB},
                {"Spring Trap", PlanItem.SPRING_TRAP}, {"Skeleton Trap", PlanItem.SKELETON_TRAP},
                {"Gold Storage", PlanItem.STORAGE}, {"Kho tiên dược đen", PlanItem.STORAGE},
                {"Gold Mine", PlanItem.COLLECTOR}, {"Bơm tiên dược", PlanItem.COLLECTOR},
                {"Dark Elixir Drill", PlanItem.COLLECTOR},
                {"Wall", PlanItem.WALL}, {"Tường x10", PlanItem.WALL},
                {"Town Hall", PlanItem.TOWN_HALL}, {"Nhà chính", PlanItem.TOWN_HALL},
                // đọc sai nhẹ / dính chữ / có cấp
                {"Arcer Queen", PlanItem.ARCHER_QUEEN}, {"ArcherQueen", PlanItem.ARCHER_QUEEN},
                {"Mortor", PlanItem.MORTAR}, {"Multi-GearTower", PlanItem.MULTI_GEAR_TOWER},
                {"Firespiter", PlanItem.FIRESPITTER}, {"Cannon (Lv 21)", PlanItem.CANNON},
                {"MONOLITH", PlanItem.MONOLITH}, {"pháo đại bàng cấp 6", PlanItem.EAGLE_ARTILLERY},
                {"xyz", PlanItem.UNKNOWN}, {"", PlanItem.UNKNOWN},
        };
        int ok = 0;
        for (Object[] c : cases) {
            PlanItem got = PlanItem.of((String) c[0]);
            if (got == c[1]) ok++;
            else check(n, false, "\"" + c[0] + "\" → " + got + " (mong đợi " + c[1] + ")");
        }
        check(n, ok == cases.length, ok + "/" + cases.length + " tên nhận đúng công trình");

        // Loại thô (Roadmap.classify) cho tên mới không còn "Không rõ"; tên cũ giữ nguyên.
        String[] fresh = {"Firespitter", "Multi-Gear Tower", "Dragon Duke", "Tháp đa năng", "Súng phun lửa",
                "Scattershot", "Monolith", "Ricochet Cannon", "Pháo hỏa ngục"};
        boolean allKnown = true;
        for (String s : fresh) {
            if (Roadmap.classify(s) == Cat.UNKNOWN) {
                allKnown = false;
                check(n, false, "Roadmap.classify(\"" + s + "\") vẫn Không rõ");
            }
        }
        check(n, allKnown, "Roadmap.classify nhận được các công trình mới của TH16/TH17");
        check(n, Roadmap.classify("Bomb Tower") == Cat.SPLASH_DEF && Roadmap.classify("Hero Hall") == Cat.ARMY_BUILDING
                        && Roadmap.classify("Builder's Hut") == Cat.POINT_DEF && Roadmap.classify("X-Bow") == Cat.AIR_DEF,
                "loại thô của tên cũ không đổi (Bomb Tower, Hero Hall, Builder's Hut, X-Bow)");
    }

    // ---------------------------------------------------------------- 2. thứ tự theo giai đoạn

    static List<Row> fullList(boolean th17) {
        String[] names = {"Cannon", "Wall", "Town Hall", "Minion Prince", "Scattershot", "Archer Tower", "Pet House",
                "Barbarian King", "Monolith", "Army Camp", "Laboratory", "Royal Champion", "Spell Tower", "Clan Castle",
                "Grand Warden", "Hero Hall", "Archer Queen", "Blacksmith", "Multi-Gear Tower", "Dragon Duke",
                "Inferno Tower", "X-Bow", "Air Defense", "Spell Factory", "Dark Spell Factory", "Giant Bomb",
                "Gold Storage", "Mortar", "Workshop", "Barracks"};
        List<Row> l = new ArrayList<>();
        for (String s : names) l.add(row(s, true, s.equals("Wall") ? 1 : 2));
        if (th17) {
            l.add(row("Firespitter", true, 1));
            l.add(row("Giga Bomb", true, 1));
            l.add(row("Inferno Artillery", true, 1));
            l.add(row("Crafting Station", true, 1));
            l.add(row("Ricochet Cannon", true, 1));
        } else {
            l.add(row("Eagle Artillery", true, 1));
        }
        return l;
    }

    static void tOrder() {
        String n = "2. Thứ tự ưu tiên theo giai đoạn";
        header(n);
        Settings c16 = cfg(16);
        c16.maxHeroesBusy = 99;
        List<PlanItem> s16 = sequence(fullList(false), c16, Memory.inRam(), false);
        report.append("   TH16: ").append(s16).append('\n');
        check(n, before(s16, PlanItem.LAB, PlanItem.ARMY_CAMP, PlanItem.CLAN_CASTLE, PlanItem.HERO_HALL,
                PlanItem.BLACKSMITH, PlanItem.BARRACKS, PlanItem.SPELL_FACTORY, PlanItem.DARK_SPELL_FACTORY,
                PlanItem.WORKSHOP, PlanItem.PET_HOUSE), "TH16: công trình tấn công trước, đúng thứ tự mục 3.1");
        check(n, before(s16, PlanItem.PET_HOUSE, PlanItem.ARCHER_QUEEN, PlanItem.GRAND_WARDEN, PlanItem.ROYAL_CHAMPION,
                PlanItem.BARBARIAN_KING, PlanItem.DRAGON_DUKE, PlanItem.MINION_PRINCE),
                "TH16: tướng Nữ hoàng → Cận vệ → Nhà vô địch rồi Vua → Công tước → Hoàng tử");
        check(n, before(s16, PlanItem.MINION_PRINCE, PlanItem.EAGLE_ARTILLERY, PlanItem.MONOLITH, PlanItem.SPELL_TOWER,
                PlanItem.MULTI_GEAR_TOWER, PlanItem.SCATTERSHOT, PlanItem.INFERNO_TOWER, PlanItem.X_BOW,
                PlanItem.AIR_DEFENSE, PlanItem.CANNON, PlanItem.GIANT_BOMB),
                "TH16: phòng thủ cao (Đại bàng, Nguyên khối, Tháp phép, Đa năng) → vừa → thấp → bẫy");
        check(n, !s16.contains(PlanItem.TOWN_HALL), "TH16: không nâng Nhà chính (chưa cho phép)");
        check(n, s16.indexOf(PlanItem.WALL) == s16.size() - 1 || !s16.contains(PlanItem.WALL),
                "TH16: tường sau cùng (chỉ xả)");

        Settings c17 = cfg(17);
        c17.maxHeroesBusy = 99;
        List<PlanItem> s17 = sequence(fullList(true), c17, Memory.inRam(), false);
        report.append("   TH17: ").append(s17).append('\n');
        check(n, before(s17, PlanItem.LAB, PlanItem.ARCHER_QUEEN, PlanItem.CLAN_CASTLE, PlanItem.ARMY_CAMP,
                PlanItem.HERO_HALL, PlanItem.SPELL_FACTORY, PlanItem.DARK_SPELL_FACTORY, PlanItem.FIRESPITTER,
                PlanItem.GIGA_BOMB), "TH17: Lab + tướng chủ lực → Lâu đài + Trại → Sảnh anh hùng → Xưởng phép → Firespitter + Bom Giga");
        check(n, before(s17, PlanItem.GIGA_BOMB, PlanItem.GRAND_WARDEN, PlanItem.ROYAL_CHAMPION, PlanItem.DRAGON_DUKE,
                PlanItem.BARBARIAN_KING, PlanItem.MINION_PRINCE, PlanItem.PET_HOUSE),
                "TH17: các tướng còn lại sau Firespitter/Bom Giga, trước Nhà thú cưng");
        check(n, before(s17, PlanItem.PET_HOUSE, PlanItem.INFERNO_ARTILLERY, PlanItem.MONOLITH, PlanItem.SCATTERSHOT,
                PlanItem.AIR_DEFENSE, PlanItem.RICOCHET_CANNON, PlanItem.GIANT_BOMB, PlanItem.CANNON),
                "TH17: phòng thủ lan/lõi → phòng không → bẫy → phòng thủ rìa");
        check(n, !s17.contains(PlanItem.TOWN_HALL), "TH17: không nâng Nhà chính (chưa cho phép)");

        Memory m = Memory.inRam();
        PlanTH17.setMainHero(m, PlanItem.GRAND_WARDEN);
        List<PlanItem> sw = sequence(fullList(true), c17, m, false);
        check(n, sw.indexOf(PlanItem.GRAND_WARDEN) == 1, "TH17: đổi tướng chủ lực thành Cận vệ → Cận vệ đứng thứ 2 sau Lab");

        // Firespitter: sau 2 lần nâng thì xuống nhóm phòng thủ lõi.
        Memory mf = Memory.inRam();
        mf.put("plan.th", "17");
        Row fs = row("Firespitter", true, 1);
        PlanTH17.recordUpgrade(mf, fs, 0);
        PlanTH17.recordUpgrade(mf, fs, 0);
        List<Row> two = Arrays.asList(row("Firespitter", true, 1), row("Pet House", true, 1));
        check(n, item(PlanTH17.choose(two, c17, fill(0.5f), mf, 0)) == PlanItem.PET_HOUSE,
                "TH17: Firespitter đã nâng 2 lần → nhường Nhà thú cưng");

        // Gọi qua Roadmap.choose (chưa có bộ nhớ) vẫn theo lộ trình TH16.
        Row pick = Roadmap.choose(fullList(false), Roadmap.TH16_TH17, fill(0.5f), 0.95f, 0.9f);
        check(n, item(pick) == PlanItem.LAB, "Roadmap.choose(preset TH16_TH17) → Phòng thí nghiệm trước (" + pick + ")");
        String d = Roadmap.describe(Roadmap.TH16_TH17);
        check(n, d.contains("TH16") && d.contains("TH17") && d.contains("Tường") && Roadmap.presetName(4).contains("TH16"),
                "describe / presetName tiếng Việt cho TH16 → TH17");
    }

    // ---------------------------------------------------------------- 3. Nhà chính

    static void tTownHall() {
        String n = "3. Luật Nhà chính";
        header(n);
        List<Row> onlyLow = Arrays.asList(row("Town Hall", true, 1), row("Cannon", true, 1), row("Wall", true, 1));
        List<Row> withCore = Arrays.asList(row("Town Hall", true, 1), row("Cannon", true, 1), row("Monolith", false, 1));
        Settings c = cfg(16);
        Memory m = Memory.inRam();
        check(n, item(PlanTH17.choose(onlyLow, c, fill(0.99f), m, 0)) != PlanItem.TOWN_HALL,
                "allowTownHall = false → không bao giờ chọn Nhà chính (kể cả kho đầy)");
        List<Row> onlyTh = Arrays.asList(row("Town Hall", true, 1));
        check(n, PlanTH17.choose(onlyTh, c, fill(0.5f), m, 0) == null, "allowTownHall = false, chỉ còn Nhà chính → không chọn");
        c.allowTownHall = true;
        check(n, item(PlanTH17.choose(onlyLow, c, fill(0.5f), m, 0)) == PlanItem.CANNON,
                "TH16 cho phép nhưng chưa đánh dấu Đại bàng cấp 7 → không chọn Nhà chính");
        PlanTH17.setEagleMaxed(m, true);
        check(n, item(PlanTH17.choose(onlyLow, c, fill(0.5f), m, 0)) == PlanItem.TOWN_HALL,
                "TH16 cho phép + Đại bàng cấp 7 + mục cốt lõi đã xong → chọn Nhà chính");
        check(n, item(PlanTH17.choose(withCore, c, fill(0.5f), m, 0)) != PlanItem.TOWN_HALL,
                "TH16 còn Nguyên khối (phòng thủ cao) trong danh sách → chưa lên Nhà chính");
        Settings c17 = cfg(17);
        c17.allowTownHall = false;
        check(n, item(PlanTH17.choose(onlyLow, c17, fill(0.5f), Memory.inRam(), 0)) != PlanItem.TOWN_HALL,
                "TH17 không cho phép → không chọn Nhà chính");
    }

    // ---------------------------------------------------------------- 4. giới hạn tướng

    static void tHeroes() {
        String n = "4. Giới hạn số tướng nâng cùng lúc";
        header(n);
        Settings c = cfg(16);
        c.maxHeroesBusy = 2;
        Memory m = Memory.inRam();
        long t0 = 1_000 * H;
        PlanTH17.recordUpgrade(m, row("Archer Queen", true, 3), t0);
        PlanTH17.recordUpgrade(m, row("Grand Warden", true, 2), t0 + H);
        check(n, PlanTH17.heroesBusy(m, t0 + 2 * H) == 2, "ghi nhận 2 tướng đang nâng (" + PlanTH17.heroesBusy(m, t0 + 2 * H) + ")");
        List<Row> rows = Arrays.asList(row("Barbarian King", true, 3), row("Royal Champion", true, 3), row("Cannon", true, 1));
        Row p = PlanTH17.choose(rows, c, fill(0.5f), m, t0 + 2 * H);
        check(n, item(p) == PlanItem.CANNON, "đủ 2 tướng đang nâng → không bắt đầu tướng thứ 3 (chọn " + p + ")");
        List<Row> heroesOnly = Arrays.asList(row("Barbarian King", true, 3));
        check(n, PlanTH17.choose(heroesOnly, c, fill(0.5f), m, t0 + 2 * H) == null, "chỉ còn tướng → chờ, không chọn gì");
        long later = t0 + (PlanTH17.DEFAULT_HERO_HOURS + 1) * H;
        check(n, item(PlanTH17.choose(rows, c, fill(0.5f), m, later)) == PlanItem.ROYAL_CHAMPION,
                "hết thời gian nâng ước tính (" + PlanTH17.DEFAULT_HERO_HOURS + " giờ) → được nâng tướng tiếp");
        PlanTH17.setHeroHours(m, 24);
        Memory m2 = Memory.inRam();
        PlanTH17.setHeroHours(m2, 24);
        PlanTH17.recordUpgrade(m2, row("Archer Queen", true, 3), 0);
        check(n, PlanTH17.heroesBusy(m2, 23 * H) == 1 && PlanTH17.heroesBusy(m2, 25 * H) == 0,
                "chỉnh thời gian nâng tướng (24 giờ) được tôn trọng");

        // TH17: tướng ngoài tướng chủ lực MỖI LẦN MỘT.
        Settings c17 = cfg(17);
        c17.maxHeroesBusy = 3;
        Memory m3 = Memory.inRam();
        m3.put("plan.th", "17");
        PlanTH17.recordUpgrade(m3, row("Grand Warden", true, 2), 0);
        List<Row> r17 = Arrays.asList(row("Royal Champion", true, 3), row("Archer Queen", true, 3), row("Cannon", true, 1));
        check(n, item(PlanTH17.choose(r17, c17, fill(0.5f), m3, H)) == PlanItem.ARCHER_QUEEN,
                "TH17: Cận vệ đang nâng → vẫn được nâng tướng chủ lực (Nữ hoàng)");
        List<Row> r17b = Arrays.asList(row("Royal Champion", true, 3), row("Cannon", true, 1));
        check(n, item(PlanTH17.choose(r17b, c17, fill(0.5f), m3, H)) == PlanItem.CANNON,
                "TH17: Cận vệ đang nâng → chưa nâng Nhà vô địch (mỗi lần một tướng phụ)");
    }

    // ---------------------------------------------------------------- 5. để dành & tường

    static void tSaving() {
        String n = "5. Để dành tài nguyên và xả tường (luật cũ, không có giá)";
        header(n);
        Settings c = cfg(16);
        Memory m = Memory.inRam();
        List<Row> rows = Arrays.asList(row("Laboratory", false, 2), row("Cannon", true, 2), row("Wall", true, 1));
        check(n, PlanTH17.choose(rows, c, fill(0.5f), m, 0) == null,
                "Lab thiếu tiên dược → không tiêu tiên dược vào Đại bác, chưa xả tường khi kho 50%");
        check(n, item(PlanTH17.choose(rows, c, new float[]{-1, 0.5f, 0.97f, 0.5f}, m, 0)) == PlanItem.CANNON,
                "kho tiên dược sắp tràn → vẫn tiêu vào Đại bác");
        List<Row> mixed = Arrays.asList(row("Laboratory", false, 2), row("Cannon", true, 2), row("Mortar", true, 1));
        check(n, item(PlanTH17.choose(mixed, c, fill(0.5f), m, 0)) == PlanItem.MORTAR,
                "vẫn nâng mục dùng vàng (Súng cối) khi để dành tiên dược");
        List<Row> walls = Arrays.asList(row("Laboratory", false, 2), row("Wall", true, 1));
        check(n, item(PlanTH17.choose(walls, c, fill(0.95f), m, 0)) == PlanItem.WALL,
                "kho ≥ wallPct (90%) và không còn mục hợp lý → xả vào tường");
        check(n, PlanTH17.choose(walls, c, fill(0.6f), m, 0) == null, "kho 60% (không rõ giá) → chưa xả tường");
        c.wallPct = 0;
        check(n, PlanTH17.choose(walls, c, fill(0.99f), m, 0) == null, "wallPct = 0 → tắt tường");
    }

    static void tEconomy() {
        String n = "6. Tối ưu thời gian thợ + tường \"thừa cứ nâng\" (có giá, tài nguyên, tốc độ farm)";
        header(n);
        Settings c = cfg(16);
        Memory m = Memory.inRam();

        // Tường khi có phần thừa, dù kho chưa tới wallPct.
        Economy e = new Economy();
        e.own = new long[]{-1, 5_000_000, 3_000_000, 0};
        e.cap = new long[]{0, 20_000_000, 20_000_000, 300_000};
        e.lootPerHour = new double[]{0, 1_000_000, 1_000_000, 5_000};
        List<Row> w = Arrays.asList(row("Laboratory", false, 2, 12_000_000), row("Wall", true, 1, 1_000_000));
        Row p = PlanTH17.choose(w, c, fill(0.25f), m, 0, e);
        check(n, item(p) == PlanItem.WALL && e.wallRes == 1,
                "kho 25% nhưng vàng thừa 5tr ≥ giá tường 1tr → nâng tường bằng vàng (" + p + ", trả bằng " + e.wallRes + ")");

        // Không ăn vào phần để dành.
        Economy e2 = new Economy();
        e2.own = new long[]{-1, 5_500_000, 500_000, 0};
        e2.cap = new long[]{0, 20_000_000, 20_000_000, 300_000};
        e2.lootPerHour = new double[]{0, 1_000_000, 1_000_000, 5_000};
        List<Row> keep = Arrays.asList(row("Monolith", false, 1, 6_000_000), row("Wall", true, 1, 1_000_000));
        check(n, PlanTH17.choose(keep, c, fill(0.25f), m, 0, e2) == null,
                "Nguyên khối 6tr còn thiếu 0,5tr (≈0,5 giờ) → để dành, không nâng tường 1tr");
        check(n, PlanTH17.choose(keep, c, fill(0.95f), m, 0, e2) == null,
                "kể cả kho ≥ wallPct: biết giá thì tường không ăn vào phần để dành");
        e2.own[1] = 7_500_000;
        check(n, item(PlanTH17.choose(keep, c, fill(0.25f), m, 0, e2)) == PlanItem.WALL,
                "vàng 7,5tr − để dành 6tr = 1,5tr ≥ 1tr → được nâng tường phần thừa");

        // Không bắt thợ chờ lâu.
        Economy e3 = new Economy();
        e3.own = new long[]{-1, 1_000_000, 2_000_000, 0};
        e3.cap = new long[]{0, 20_000_000, 20_000_000, 300_000};
        e3.lootPerHour = new double[]{0, 500_000, 500_000, 5_000};
        List<Row> lab = Arrays.asList(row("Laboratory", false, 2, 10_000_000), row("Cannon", true, 2, 1_500_000));
        Row q = PlanTH17.choose(lab, c, fill(0.3f), m, 0, e3);
        check(n, item(q) == PlanItem.CANNON,
                "Lab còn thiếu 8tr (≈16 giờ farm) → không để thợ rảnh, nâng Đại bác (" + q + ")");
        e3.lootPerHour[2] = 10_000_000;
        check(n, PlanTH17.choose(lab, c, fill(0.3f), m, 0, e3) == null,
                "farm 10tr/giờ → Lab đủ tiền sau ≈0,8 giờ → vẫn để dành");
        Economy e4 = new Economy();
        e4.own = new long[]{-1, 1_000_000, 2_000_000, 0};
        check(n, PlanTH17.choose(lab, c, fill(0.3f), m, 0, e4) == null,
                "không rõ tốc độ farm → giữ luật để dành cũ");
        e4.cap = new long[]{0, 8_000_000, 8_000_000, 300_000};
        check(n, item(PlanTH17.choose(lab, c, fill(0.3f), m, 0, e4)) == PlanItem.CANNON,
                "giá Lab vượt sức chứa kho → không chờ vô ích");

        // Các lộ trình cũ cũng dùng cùng bộ chọn.
        check(n, item(Roadmap.choose(lab, Roadmap.BALANCED, fill(0.3f), 0.95f, 0.9f, e3)) == null
                        && item(Roadmap.choose(lab, Roadmap.BALANCED, fill(0.3f), 0.95f, 0.9f, null)) == null,
                "lộ trình Cân bằng: để dành khi sắp đủ / không có dữ liệu");
        e3.lootPerHour[2] = 500_000;
        check(n, item(Roadmap.choose(lab, Roadmap.BALANCED, fill(0.3f), 0.95f, 0.9f, e3)) == PlanItem.CANNON,
                "lộ trình Cân bằng: Lab còn lâu mới đủ → nâng mục khác");
        Row wall = Roadmap.chooseWall(w, Roadmap.BALANCED, fill(0.25f), 0.9f, e);
        check(n, item(wall) == PlanItem.WALL, "Roadmap.chooseWall: hết thợ vẫn xả phần thừa vào tường");
    }

    // ---------------------------------------------------------------- 7. checklist

    static void tChecklist() {
        String n = "7. Checklist tiến độ";
        header(n);
        Memory m = Memory.inRam();
        Settings c = cfg(16);
        PlanTH17.choose(Arrays.asList(row("Laboratory", true, 2)), c, fill(0.5f), m, 0);
        PlanTH17.recordUpgrade(m, row("Laboratory", true, 2), 0);
        PlanTH17.recordUpgrade(m, row("Clan Castle", true, 1), 0);
        String t = PlanTH17.checklist(m, 0);
        check(n, t.contains("Giai đoạn A"), "TH16 → hiện \"Giai đoạn A\"");
        check(n, t.contains("✓ Laboratory max TH16") && t.contains("✓ Clan Castle max"), "đã nâng Lab, Lâu đài → ✓");
        check(n, t.contains("☐ 4 Army Camps max (0/4)") && t.contains("☐ EAGLE ARTILLERY CẤP 7"), "chưa nâng → ☐ (kèm tiến độ)");
        check(n, t.contains("GỢI Ý TIẾP THEO") && t.contains("1. Trại quân"), "gợi ý tiếp theo bắt đầu bằng Trại quân");
        PlanTH17.setEagleMaxed(m, true);
        check(n, PlanTH17.checklist(m, 0).contains("✓ EAGLE ARTILLERY CẤP 7"), "tự đánh dấu Đại bàng → ✓");

        Settings c17 = cfg(17);
        PlanTH17.choose(Arrays.asList(row("Laboratory", true, 2)), c17, fill(0.5f), m, 0);
        check(n, PlanTH17.checklist(m, 0).contains("Giai đoạn B"), "lên TH17 → \"Giai đoạn B\"");
        String[] wk1 = {"Laboratory", "Archer Queen", "Clan Castle", "Army Camp", "Hero Hall", "Firespitter", "Giga Bomb"};
        for (String s : wk1) PlanTH17.recordUpgrade(m, row(s, true, 1), 0);
        String t2 = PlanTH17.checklist(m, 0);
        check(n, t2.contains("Giai đoạn C") && t2.contains("✓ Đặt Firespitter") && t2.contains("✓ Tướng chủ lực"),
                "xong tuần đầu TH17 → \"Giai đoạn C\", ✓ Firespitter, ✓ tướng chủ lực");
        check(n, t2.contains("Tướng đang nâng (bot ghi nhận): 1/2"), "hiện số tướng đang nâng (1/2)");
        System.out.println("\n--- Checklist mẫu ---\n" + t2 + "\n---------------------");
    }

    // ---------------------------------------------------------------- 8. chạy thật trên game giả lập

    static void tIntegration() {
        String n = "8. Chạy bot trên game giả lập, lộ trình TH16 → TH17 (qua Roadmap.choose)";
        header(n);
        FakeGame g = new FakeGame(16);
        g.buildings.add(b("Laboratory", 1_500_000, 2, 120));
        g.buildings.add(b("Army Camp", 600_000, 2, 60));
        g.buildings.add(b("Clan Castle", 900_000, 1, 90));
        g.buildings.add(b("Hero Hall", 800_000, 2, 90));
        g.buildings.add(b("Blacksmith", 700_000, 2, 60));
        g.buildings.add(b("Archer Queen", 40_000, 3, 60));
        g.buildings.add(b("Monolith", 1_800_000, 1, 120));
        g.buildings.add(b("Spell Tower", 1_000_000, 1, 100));
        g.buildings.add(b("Multi-Gear Tower", 1_200_000, 1, 100));
        g.buildings.add(b("Scattershot", 1_100_000, 1, 90));
        g.buildings.add(b("Cannon", 300_000, 1, 40));
        g.buildings.add(b("Mortar", 400_000, 1, 40));
        g.buildings.add(b("Giant Bomb", 200_000, 1, 20));
        g.buildings.add(b("Gold Storage", 700_000, 2, 60));
        g.buildings.add(b("Town Hall", 2_000_000, 1, 300));
        g.buildings.add(b("Wall", 150_000, 1, 0));
        g.own = new long[]{0, 6_000_000, 6_000_000, 10_000};
        g.cap = new long[]{0, 10_000_000, 10_000_000, 200_000};
        Settings cfg = Sim.settings();
        cfg.autoUpgrade = true;
        cfg.upgradeEvery = 1;
        cfg.cycles = 2;
        cfg.roadmap = Roadmap.TH16_TH17;
        cfg.wallPct = 0;
        Sim.Result r = Sim.run(g, Sim.teach(true), cfg);
        report.append("   thứ tự nâng cấp: ").append(g.upgradesDone).append('\n');
        check(n, r.error == null, "không lỗi chương trình" + (r.error == null ? "" : " (" + r.error + ")"));
        check(n, g.insufficientConfirms == 0 && g.gemTaps == 0, "không xác nhận khi thiếu tiền, không bấm ngọc");
        List<String> done = g.upgradesDone;
        boolean order = done.size() >= 3 && done.get(0).equals("Laboratory") && done.get(1).equals("Army Camp")
                && done.get(2).equals("Clan Castle");
        check(n, order, "3 mục đầu: Laboratory → Army Camp → Clan Castle (thực tế " + done + ")");
        check(n, !done.contains("Town Hall"), "không nâng Nhà chính");
    }

    private static FakeGame.Building b(String name, long cost, int res, int min) {
        Cat c = Roadmap.classify(name);
        return new FakeGame.Building(name, c, 1, c == Cat.WALL ? 500 : 20, cost, res, min);
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        tClassify();
        tOrder();
        tTownHall();
        tHeroes();
        tSaving();
        tEconomy();
        tChecklist();
        tIntegration();
        System.out.println("\n=============== KIỂM THỬ LỘ TRÌNH TH16 → TH17 ===============");
        System.out.print(report);
        System.out.printf("%nĐạt %d mục, thời gian chạy %.1f giây%n", passed, (System.currentTimeMillis() - t0) / 1000.0);
        System.out.println(failures == 0 ? "KẾT QUẢ: TẤT CẢ ĐẠT ✓" : "KẾT QUẢ: " + failures + " MỤC KHÔNG ĐẠT ✗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
