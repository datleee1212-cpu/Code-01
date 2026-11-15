package sim;

import com.code02.autofarm.core.Bot;
import com.code02.autofarm.core.Box;
import com.code02.autofarm.core.Img;
import com.code02.autofarm.core.Keys;
import com.code02.autofarm.core.Roadmap;
import com.code02.autofarm.core.Roadmap.Cat;
import com.code02.autofarm.core.Settings;
import com.code02.autofarm.core.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chạy bot thật (lõi core) trên game giả lập qua nhiều kịch bản và kiểm tra kết quả.
 * Thoát với mã 1 nếu có kịch bản không đạt.
 */
public final class Sim {

    private static int failures = 0;
    private static final StringBuilder report = new StringBuilder();

    // ================================================================ "người dùng" dạy bot

    static final class SimStore implements Store {
        final Map<String, int[]> points = new HashMap<>();
        final Map<String, Box> regions = new HashMap<>();
        final Map<String, Img> templates = new HashMap<>();

        @Override
        public int[] point(String key) {
            return points.get(key);
        }

        @Override
        public Box region(String key) {
            return regions.get(key);
        }

        @Override
        public Img template(String key) {
            return templates.get(key);
        }
    }

    /** Dạy giống người dùng thật: mở đúng màn hình, chạm vào nút, bot cắt hình quanh chỗ chạm. */
    static SimStore teach(boolean withUpgrade) {
        FakeGame g = new FakeGame(999);
        g.buildings.addAll(village(false));
        SimStore s = new SimStore();
        int side = FakeGame.H * 8 / 100;

        teachTap(g, s, FakeGame.Screen.HOME, Keys.ATTACK, 80, 640, side);
        teachTap(g, s, FakeGame.Screen.HOME, Keys.TRAIN_OPEN, 80, 540, side);
        teachTap(g, s, FakeGame.Screen.TRAIN, Keys.QUICK_TRAIN, 400, 150, side);
        teachTap(g, s, FakeGame.Screen.TRAIN, Keys.TRAIN_ARMY, 900, 300, side);
        teachTap(g, s, FakeGame.Screen.TRAIN, Keys.TRAIN_CLOSE, 1200, 80, side);
        teachTap(g, s, FakeGame.Screen.ATTACK_MENU, Keys.FIND_MATCH, 320, 560, side);
        teachTap(g, s, FakeGame.Screen.SCOUT, Keys.NEXT, 1180, 560, side);
        teachTap(g, s, FakeGame.Screen.SCOUT, Keys.END_BATTLE, 80, 560, side);
        teachTap(g, s, FakeGame.Screen.SURRENDER, Keys.SURRENDER_OK, 740, 420, side);
        teachTap(g, s, FakeGame.Screen.RESULT, Keys.RETURN_HOME, 640, 620, side);
        s.regions.put(Keys.LOOT_GOLD, new Box(34, 76, 246, 114));
        s.regions.put(Keys.LOOT_ELIXIR, new Box(34, 116, 246, 154));
        s.points.put(Keys.TROOP1, new int[]{200, 660});
        s.points.put(Keys.TROOP2, new int[]{290, 660});
        s.points.put(Keys.HERO, new int[]{380, 660});
        s.points.put(Keys.DEPLOY_A, new int[]{420, 260});
        s.points.put(Keys.DEPLOY_B, new int[]{1000, 520});

        // Biểu tượng thu tài nguyên (dạy lúc mỏ đầy)
        g.collector[1] = g.collector[2] = 1_000_000;
        g.collector[3] = 10_000;
        teachTap(g, s, FakeGame.Screen.HOME, Keys.COLLECT_GOLD, 520, 330, side);
        teachTap(g, s, FakeGame.Screen.HOME, Keys.COLLECT_ELIXIR, 760, 420, side);
        teachTap(g, s, FakeGame.Screen.HOME, Keys.COLLECT_DARK, 380, 470, side);

        // Thông báo: nút X và nút Tải lại
        teachTap(g, s, FakeGame.Screen.EVENT_POPUP, Keys.POPUPS[0], 1050, 150, side);
        teachTap(g, s, FakeGame.Screen.CONN_LOST, Keys.POPUPS[1], 640, 450, side);

        if (withUpgrade) {
            s.regions.put(Keys.OWN_GOLD, new Box(1015, 15, 1185, 55));
            s.regions.put(Keys.OWN_ELIXIR, new Box(1015, 65, 1185, 105));
            s.regions.put(Keys.OWN_DARK, new Box(1015, 115, 1185, 155));
            s.regions.put(Keys.BUILDERS_FREE, new Box(645, 20, 745, 60));
            teachTap(g, s, FakeGame.Screen.HOME, Keys.BUILDER, 600, 40, side);
            s.regions.put(Keys.UPGRADE_ROW1, new Box(FakeGame.ROW_X0, FakeGame.ROW_Y0,
                    FakeGame.ROW_X1, FakeGame.ROW_Y0 + FakeGame.ROW_H - 8));
            s.points.put(Keys.UPGRADE_ROW2, new int[]{660, FakeGame.ROW_Y0 + FakeGame.ROW_H + 24});
            g.selected = find(g, "Laboratory");
            teachTap(g, s, FakeGame.Screen.SELECTED, Keys.UPGRADE_BTN, 700, 640, side);
            g.selected = find(g, "Wall");
            teachTap(g, s, FakeGame.Screen.SELECTED, Keys.UPGRADE_BTN_ELIXIR, 800, 640, side);
            g.selected = find(g, "Cannon");
            g.own[1] = 9_000_000; // dạy lúc giá màu trắng
            teachTap(g, s, FakeGame.Screen.UPGRADE_DLG, Keys.UPGRADE_CONFIRM, 900, 600, side);
        }
        return s;
    }

    private static FakeGame.Building find(FakeGame g, String name) {
        for (FakeGame.Building b : g.buildings) if (b.name.equals(name)) return b;
        throw new IllegalStateException(name);
    }

    private static void teachTap(FakeGame g, SimStore s, FakeGame.Screen screen, String key, int x, int y, int side) {
        g.start(screen);
        Img shot = g.capture();
        s.templates.put(key, shot.cropSquare(x, y, side));
        s.points.put(key, new int[]{x, y});
    }

    // ================================================================ làng mẫu

    static List<FakeGame.Building> village(boolean vietnamese) {
        List<FakeGame.Building> l = new ArrayList<>();
        l.add(b(vietnamese ? "Phòng thí nghiệm" : "Laboratory", Cat.LAB, 1_500_000, 2, 120));
        l.add(b(vietnamese ? "Lâu đài bang hội" : "Clan Castle", Cat.CLAN_CASTLE, 900_000, 1, 90));
        l.add(b(vietnamese ? "Trại quân" : "Army Camp", Cat.ARMY_CAMP, 600_000, 2, 60));
        l.add(b(vietnamese ? "Nhà lính" : "Barracks", Cat.ARMY_BUILDING, 350_000, 2, 45));
        l.add(b(vietnamese ? "Nữ hoàng cung thủ" : "Archer Queen", Cat.HERO, 40_000, 3, 60));
        l.add(b(vietnamese ? "Kho vàng" : "Gold Storage", Cat.STORAGE, 700_000, 2, 60));
        l.add(b(vietnamese ? "Kho tiên dược" : "Elixir Storage", Cat.STORAGE, 700_000, 1, 60));
        l.add(b(vietnamese ? "Tháp phù thủy" : "Wizard Tower", Cat.SPLASH_DEF, 1_100_000, 1, 100));
        l.add(b(vietnamese ? "Súng cối" : "Mortar", Cat.SPLASH_DEF, 800_000, 1, 80));
        l.add(b(vietnamese ? "Phòng không" : "Air Defense", Cat.AIR_DEF, 1_200_000, 1, 100));
        l.add(b(vietnamese ? "Tháp cung thủ" : "Archer Tower", Cat.POINT_DEF, 500_000, 1, 50));
        l.add(b(vietnamese ? "Đại bác" : "Cannon", Cat.POINT_DEF, 300_000, 1, 40));
        l.add(b(vietnamese ? "Mỏ vàng" : "Gold Mine", Cat.COLLECTOR, 250_000, 2, 30));
        l.add(b(vietnamese ? "Bơm tiên dược" : "Elixir Collector", Cat.COLLECTOR, 250_000, 1, 30));
        l.add(b(vietnamese ? "Bom lớn" : "Giant Bomb", Cat.TRAP, 200_000, 1, 20));
        l.add(b(vietnamese ? "Nhà chính" : "Town Hall", Cat.TOWN_HALL, 2_000_000, 1, 300));
        l.add(b(vietnamese ? "Tường" : "Wall", Cat.WALL, 150_000, 1, 0));
        return l;
    }

    private static FakeGame.Building b(String n, Cat c, long cost, int res, int min) {
        return new FakeGame.Building(n, c, 1, c == Cat.WALL ? 500 : 20, cost, res, min);
    }

    static Settings settings() {
        Settings s = new Settings();
        s.cycles = 10;
        s.minLoot = 600_000;
        s.maxSkips = 20;
        s.battleWait = 150;
        s.goldCap = 10_000_000;
        s.elixirCap = 10_000_000;
        s.darkCap = 200_000;
        s.listRows = 5;
        s.listPages = 4;
        s.fullWaitMin = 10;
        return s;
    }

    // ================================================================ khung kiểm thử

    static final class Result {
        final FakeGame game;
        final Bot bot;
        final Exception error;

        Result(FakeGame game, Bot bot, Exception error) {
            this.game = game;
            this.bot = bot;
            this.error = error;
        }
    }

    static Result run(FakeGame g, SimStore s, Settings cfg) {
        Bot bot = new Bot(g, g, s, cfg);
        Exception err = null;
        try {
            bot.run();
        } catch (Exception e) {
            err = e;
        }
        return new Result(g, bot, err);
    }

    private static void check(String scenario, boolean ok, String what) {
        report.append(ok ? "   ✓ " : "   ✗ ").append(what).append('\n');
        if (!ok) {
            failures++;
            System.out.println("FAIL [" + scenario + "] " + what);
        }
    }

    private static void header(String name, Result r) {
        Bot.Stats st = r.bot.stats;
        FakeGame g = r.game;
        report.append("\n▶ ").append(name).append('\n');
        report.append(String.format("   thời gian giả lập %.0f phút | vòng %d | đánh %d | bỏ qua làng %d | đóng thông báo %d/%d hiện | " +
                        "về làng khi lạc %d | thu %d | nâng cấp %d | tường %d | chờ kho đầy %d%n",
                g.now() / 60000.0, st.cycles, st.attacks, st.skippedBases, st.popupsClosed, g.popupsShown,
                st.recoveries, st.collected, st.upgrades, st.walls, st.fullWaits));
        report.append(String.format("   cướp vào kho %,d | bị tràn (lãng phí) %,d | bấm nhầm ngọc %d | xác nhận khi thiếu tiền %d%n",
                g.lootGained, g.lootWasted, g.gemTaps, g.insufficientConfirms));
        if (!g.upgradesDone.isEmpty()) report.append("   thứ tự nâng cấp: ").append(g.upgradesDone).append('\n');
        if (r.error != null) report.append("   LỖI: ").append(r.error).append('\n');
    }

    private static void dumpLog(FakeGame g, int last) {
        int from = Math.max(0, g.log.size() - last);
        for (int i = from; i < g.log.size(); i++) System.out.println("      " + g.log.get(i));
    }

    // ================================================================ kịch bản

    static void s1Farm(SimStore store) {
        FakeGame g = new FakeGame(1);
        g.buildings.addAll(village(false));
        g.popupPerMin = 0.5;
        g.connLostPerMin = 0.3;
        g.cap = new long[]{0, 10_000_000, 10_000_000, 200_000};
        g.ocrError = 0.05;
        Settings cfg = settings();
        cfg.cycles = 15;
        Result r = run(g, store, cfg);
        String n = "1. Farm + thông báo bất ngờ + OCR lỗi 5%";
        header(n, r);
        check(n, r.error == null, "không lỗi chương trình");
        check(n, r.bot.stats.attacks >= 12, "đánh ≥ 12/15 trận (" + r.bot.stats.attacks + ")");
        check(n, g.gemTaps == 0, "không bao giờ bấm nút ngọc");
        check(n, g.popupsShown >= 3, "có thông báo bất ngờ để thử (" + g.popupsShown + ")");
        check(n, r.bot.stats.popupsClosed >= g.popupsShown * 8 / 10, "đóng ≥ 80% thông báo (" + r.bot.stats.popupsClosed + "/" + g.popupsShown + ")");
        int good = 0;
        int total = 0;
        for (long v : r.bot.stats.attackedStorable) {
            total++;
            if (v < 0 || v >= cfg.minLoot) good++;
        }
        check(n, total > 0 && good * 100 / total >= 85, "≥ 85% trận đánh vào làng đủ tài nguyên (" + good + "/" + total + ")");
        check(n, r.bot.stats.skippedBases > 0, "có bỏ qua làng nghèo");
        if (r.error != null || r.bot.stats.attacks < 12) dumpLog(g, 40);
    }

    static void s2Lost(SimStore store) {
        FakeGame.Screen[] starts = {FakeGame.Screen.RESULT, FakeGame.Screen.BUILDER_LIST, FakeGame.Screen.SURRENDER,
                FakeGame.Screen.TRAIN, FakeGame.Screen.CONN_LOST, FakeGame.Screen.EVENT_POPUP, FakeGame.Screen.ATTACK_MENU};
        String n = "2. Bắt đầu ở màn hình lạ → tự về làng";
        report.append("\n▶ ").append(n).append('\n');
        for (FakeGame.Screen s : starts) {
            FakeGame g = new FakeGame(2 + s.ordinal());
            g.buildings.addAll(village(false));
            g.start(s);
            Settings cfg = settings();
            cfg.cycles = 1;
            Result r = run(g, store, cfg);
            check(n, r.error == null && r.bot.stats.attacks == 1, "bắt đầu ở " + s + " → vẫn đánh xong 1 trận");
            if (r.bot.stats.attacks != 1) dumpLog(g, 25);
        }
    }

    static void s3Roadmap(SimStore store, boolean vietnamese, int preset) {
        FakeGame g = new FakeGame(3);
        g.buildings.addAll(village(vietnamese));
        g.own = new long[]{0, 6_000_000, 6_000_000, 10_000};
        g.cap = new long[]{0, 10_000_000, 10_000_000, 200_000};
        Settings cfg = settings();
        cfg.autoUpgrade = true;
        cfg.upgradeEvery = 1;
        cfg.cycles = 2;
        cfg.roadmap = preset;
        cfg.wallPct = 0;
        Result r = run(g, store, cfg);
        String n = "3. Lộ trình " + Roadmap.presetName(preset) + (vietnamese ? " – tên tiếng Việt" : " – tên tiếng Anh");
        header(n, r);
        check(n, r.error == null, "không lỗi chương trình");
        check(n, g.insufficientConfirms == 0 && g.gemTaps == 0, "không xác nhận khi thiếu tiền, không bấm ngọc");
        List<Cat> cats = new ArrayList<>();
        for (FakeGame.Building b : g.upgradedRefs) if (b.cat != Cat.WALL) cats.add(b.cat);
        check(n, cats.size() >= 3, "dùng hết 3 thợ (" + cats.size() + ")");
        Cat[] expect;
        if (preset == Roadmap.FARM) expect = new Cat[]{Cat.LAB, Cat.ARMY_CAMP, Cat.STORAGE};
        else if (preset == Roadmap.DEFENSE) expect = new Cat[]{Cat.LAB, Cat.CLAN_CASTLE, Cat.SPLASH_DEF};
        else expect = new Cat[]{Cat.LAB, Cat.CLAN_CASTLE, Cat.ARMY_CAMP};
        boolean order = cats.size() >= 3;
        for (int i = 0; i < 3 && order; i++) order = cats.get(i) == expect[i];
        check(n, order, "3 mục đầu đúng lộ trình " + java.util.Arrays.toString(expect) + " → thực tế " + cats);
        check(n, !cats.contains(Cat.TOWN_HALL), "không nâng Nhà chính khi còn mục khác");
        if (!order || r.error != null) dumpLog(g, 40);
    }

    static void s4Saving(SimStore store) {
        FakeGame g = new FakeGame(4);
        g.buildings.addAll(village(false));
        g.own = new long[]{0, 2_000_000, 1_000_000, 0};
        g.cap = new long[]{0, 3_000_000, 3_000_000, 100_000};
        Settings cfg = settings();
        cfg.goldCap = 3_000_000;
        cfg.elixirCap = 3_000_000;
        cfg.autoUpgrade = true;
        cfg.upgradeEvery = 1;
        cfg.cycles = 1;
        cfg.minLoot = 0;
        cfg.wallPct = 0;
        Result r = run(g, store, cfg);
        String n = "4. Để dành: Phòng thí nghiệm thiếu tiên dược";
        header(n, r);
        boolean ok = true;
        for (FakeGame.Building b : g.upgradedRefs) {
            if (b.res == 2 && b.cat != Cat.LAB) ok = false;
        }
        check(n, r.error == null, "không lỗi chương trình");
        check(n, ok, "không tiêu tiên dược vào mục kém ưu tiên hơn Phòng thí nghiệm");
        check(n, !g.upgradedRefs.isEmpty(), "vẫn nâng mục dùng vàng (" + g.upgradesDone + ")");
    }

    static void s5Walls(SimStore store) {
        FakeGame g = new FakeGame(5);
        for (FakeGame.Building b : village(false)) {
            if (b.res == 1 && b.cat != Cat.WALL) b.cost = 5_000_000; // mục vàng đều quá đắt
            g.buildings.add(b);
        }
        g.own = new long[]{0, 2_950_000, 500_000, 0};
        g.cap = new long[]{0, 3_000_000, 3_000_000, 100_000};
        Settings cfg = settings();
        cfg.goldCap = 3_000_000;
        cfg.elixirCap = 3_000_000;
        cfg.autoUpgrade = true;
        cfg.upgradeEvery = 1;
        cfg.cycles = 2;
        cfg.wallPct = 90;
        Result r = run(g, store, cfg);
        String n = "5. Kho vàng sắp tràn → xả vào tường";
        header(n, r);
        check(n, r.error == null, "không lỗi chương trình");
        check(n, r.bot.stats.walls >= 2, "nâng tường ≥ 2 lần (" + r.bot.stats.walls + ")");
        check(n, g.gemTaps == 0 && g.insufficientConfirms == 0, "không bấm ngọc / không xác nhận khi thiếu");
        check(n, r.bot.stats.attacks >= 1, "vẫn đi đánh sau khi xả");
    }

    static void s6Full(SimStore store) {
        FakeGame g = new FakeGame(6);
        g.buildings.addAll(village(false));
        g.buildersTotal = 1;
        g.buildings.get(0).busyUntil = 10L * 3600_000; // thợ duy nhất đang bận 10 giờ
        g.own = new long[]{0, 3_000_000, 3_000_000, 0};
        g.cap = new long[]{0, 3_000_000, 3_000_000, 100_000};
        Settings cfg = settings();
        cfg.goldCap = 3_000_000;
        cfg.elixirCap = 3_000_000;
        cfg.autoUpgrade = true;
        cfg.upgradeEvery = 1;
        cfg.cycles = 2;
        cfg.wallPct = 0;
        Result r = run(g, store, cfg);
        String n = "6. Kho đầy, hết thợ → không đi đánh phí";
        header(n, r);
        check(n, r.error == null, "không lỗi chương trình");
        check(n, r.bot.stats.attacks == 0 && r.bot.stats.fullWaits >= 1, "không đánh, chuyển sang chờ");
        check(n, g.searches == 0, "không tốn vàng tìm trận");
    }

    static void s7Storable(SimStore store) {
        FakeGame g = new FakeGame(7);
        g.buildings.addAll(village(false));
        g.own = new long[]{0, 2_990_000, 200_000, 0};
        g.cap = new long[]{0, 3_000_000, 3_000_000, 100_000};
        Settings cfg = settings();
        cfg.goldCap = 3_000_000;
        cfg.elixirCap = 3_000_000;
        cfg.minLoot = 500_000;
        cfg.cycles = 6;
        cfg.wallPct = 0;
        Result r = run(g, store, cfg);
        String n = "7. Vàng gần đầy → chỉ tính tiên dược khi chọn làng";
        header(n, r);
        check(n, r.error == null, "không lỗi chương trình");
        long wastedPct = g.lootGained + g.lootWasted == 0 ? 0 : g.lootWasted * 100 / (g.lootGained + g.lootWasted);
        check(n, r.bot.stats.attacks >= 5, "vẫn farm (" + r.bot.stats.attacks + " trận)");
        check(n, g.own[2] > 1_500_000, "tiên dược tăng mạnh (" + g.own[2] + ")");
        report.append("   tỉ lệ tài nguyên bị tràn: ").append(wastedPct).append("%\n");
    }

    static void s8NoVision(SimStore store) {
        FakeGame g = new FakeGame(8);
        g.buildings.addAll(village(false));
        g.screenshotSupported = false;
        Settings cfg = settings();
        cfg.cycles = 3;
        Result r = run(g, store, cfg);
        String n = "8. Máy không chụp được màn hình (Android < 11) → chạm theo tọa độ";
        header(n, r);
        check(n, r.error == null, "không lỗi chương trình");
        check(n, g.searches >= 3, "vẫn tìm trận (" + g.searches + ")");
    }

    static void s9Classifier() {
        String n = "9. Nhận biết tên công trình";
        report.append("\n▶ ").append(n).append('\n');
        Object[][] cases = {
                {"Laboratory", Cat.LAB}, {"Phòng Thí Nghiệm", Cat.LAB}, {"Bomb Tower", Cat.SPLASH_DEF},
                {"Bomb", Cat.TRAP}, {"Bơm tiên dược", Cat.COLLECTOR}, {"Kho tiên dược đen", Cat.STORAGE},
                {"Khoan tiên dược đen", Cat.COLLECTOR}, {"Archer Queen", Cat.HERO}, {"Archer Tower", Cat.POINT_DEF},
                {"Town Hall", Cat.TOWN_HALL}, {"Nhà chính", Cat.TOWN_HALL}, {"Wall x12", Cat.WALL},
                {"Tường", Cat.WALL}, {"Hero Hall", Cat.ARMY_BUILDING}, {"Dark Elixir Drill", Cat.COLLECTOR},
                {"X-Bow", Cat.AIR_DEF}, {"Inferno Tower", Cat.AIR_DEF}, {"Clan Castle", Cat.CLAN_CASTLE},
                {"Lâu đài bang hội", Cat.CLAN_CASTLE}, {"Builder's Hut", Cat.POINT_DEF}, {"Eagle Artillery", Cat.SPLASH_DEF},
        };
        int ok = 0;
        for (Object[] c : cases) {
            Cat got = Roadmap.classify((String) c[0]);
            if (got == c[1]) ok++;
            else check(n, false, "\"" + c[0] + "\" → " + got + " (mong đợi " + c[1] + ")");
        }
        check(n, ok == cases.length, ok + "/" + cases.length + " tên nhận đúng loại");
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        SimStore farmOnly = teach(false);
        SimStore full = teach(true);

        s9Classifier();
        s1Farm(farmOnly);
        s2Lost(farmOnly);
        s3Roadmap(full, false, Roadmap.BALANCED);
        s3Roadmap(full, true, Roadmap.BALANCED);
        s3Roadmap(full, false, Roadmap.FARM);
        s3Roadmap(full, false, Roadmap.DEFENSE);
        s4Saving(full);
        s5Walls(full);
        s6Full(full);
        s7Storable(full);
        s8NoVision(farmOnly);

        System.out.println("\n=============== BÁO CÁO GIẢ LẬP AUTOFARM ===============");
        System.out.print(report);
        System.out.printf("%nThời gian chạy thật: %.1f giây%n", (System.currentTimeMillis() - t0) / 1000.0);
        System.out.println(failures == 0 ? "KẾT QUẢ: TẤT CẢ ĐẠT ✓" : "KẾT QUẢ: " + failures + " MỤC KHÔNG ĐẠT ✗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
