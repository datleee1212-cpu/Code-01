package com.code02.autofarm;

import android.content.Context;
import android.content.SharedPreferences;

/** Các thông số chỉnh trong màn hình chính. */
final class Config {

    static final class Field {
        final String key;
        final String label;
        final int def;
        final int min;
        final int max;
        /** true: hiển thị dạng ô tích (0 = tắt, 1 = bật). */
        final boolean bool;

        Field(String key, String label, int def, int min, int max, boolean bool) {
            this.key = key;
            this.label = label;
            this.def = def;
            this.min = min;
            this.max = max;
            this.bool = bool;
        }

        Field(String key, String label, int def, int min, int max) {
            this(key, label, def, min, max, false);
        }
    }

    static final Field CYCLES = new Field("cycles", "Số vòng chạy (0 = chạy mãi; kênh Lộ trình: số lượt)", 0, 0, 100000);
    static final Field MIN_LOOT = new Field("min_loot", "Chỉ đánh làng có vàng + tiên dược từ (0 = đánh mọi làng)", 300000, 0, 20000000);
    static final Field MAX_SKIPS = new Field("max_skips", "Bỏ qua tối đa bao nhiêu làng rồi đánh luôn", 20, 0, 200);
    static final Field AUTO_TUNE = new Field("auto_tune", "Tự cải thiện: tự chỉnh ngưỡng chọn làng & thời gian chờ", 1, 0, 1, true);
    static final Field AUTO_UPGRADE = new Field("auto_upgrade", "Kênh Farm: tự nâng cấp công trình", 0, 0, 1, true);
    static final Field UPGRADE_EVERY = new Field("upgrade_every", "Kênh Farm: thử nâng cấp sau mỗi bao nhiêu vòng", 3, 1, 100);
    static final Field CHANNEL_INTERVAL = new Field("channel_interval", "Kênh Lộ trình: kiểm tra thợ xây mỗi (phút)", 30, 5, 720);
    static final Field ROADMAP = new Field("roadmap", "Lộ trình nâng cấp: 1 Cân bằng, 2 Farm, 3 Phòng thủ, 4 TH16→TH17", 1, 1, 4);
    static final Field TOWN_HALL = new Field("town_hall", "Cấp Nhà chính hiện tại", 16, 1, 18);
    static final Field ALLOW_TOWN_HALL = new Field("allow_town_hall", "Cho phép tự nâng Nhà chính", 0, 0, 1, true);
    static final Field MAX_HEROES_BUSY = new Field("max_heroes_busy", "Số tướng nâng cùng lúc tối đa", 2, 1, 6);
    static final Field MAX_UPGRADES = new Field("max_upgrades", "Số công trình nâng tối đa mỗi lượt", 3, 1, 10);
    static final Field LIST_ROWS = new Field("list_rows", "Số dòng nhìn thấy trong danh sách thợ xây", 5, 1, 10);
    static final Field LIST_PAGES = new Field("list_pages", "Số trang danh sách cần đọc (cuộn)", 2, 1, 5);
    static final Field GOLD_CAP = new Field("gold_cap", "Sức chứa kho VÀNG (0 = không rõ)", 0, 0, 2000000000);
    static final Field ELIXIR_CAP = new Field("elixir_cap", "Sức chứa kho TIÊN DƯỢC (0 = không rõ)", 0, 0, 2000000000);
    static final Field DARK_CAP = new Field("dark_cap", "Sức chứa kho TIÊN DƯỢC ĐEN (0 = không rõ)", 0, 0, 2000000000);
    static final Field WALL_PCT = new Field("wall_pct", "Xả tài nguyên vào tường khi kho đầy từ (%) (0 = tắt)", 90, 0, 100);
    static final Field FULL_WAIT = new Field("full_wait", "Kho đầy mà không còn gì để nâng: nghỉ (phút)", 10, 1, 240);
    static final Field DEPLOY_COUNT = new Field("deploy_count", "Số lần chạm thả mỗi ô quân", 10, 1, 100);
    static final Field TAP_DELAY = new Field("tap_delay", "Nghỉ giữa các lần thả quân (mili giây)", 150, 50, 5000);
    static final Field STEP_DELAY = new Field("step_delay", "Nghỉ giữa các bước bấm nút (mili giây)", 1500, 300, 10000);
    static final Field TRAIN_WAIT = new Field("train_wait", "Chờ luyện quân trước khi đánh (giây)", 0, 0, 3600);
    static final Field BATTLE_WAIT = new Field("battle_wait", "Thời gian đánh tối đa trước khi kết thúc (giây)", 90, 10, 180);
    static final Field MATCH_THRESHOLD = new Field("match_thr", "Độ giống tối thiểu khi nhận diện hình (%)", 82, 60, 99);
    static final Field TEMPLATE_SIZE = new Field("tpl_size", "Cỡ hình mẫu khi dạy (% cạnh ngắn màn hình)", 8, 3, 25);
    static final Field SEARCH_WAIT = new Field("search_wait", "[Không đọc màn hình] Chờ tìm trận (giây)", 8, 2, 60);
    static final Field HOME_WAIT = new Field("home_wait", "[Không đọc màn hình] Chờ về làng (giây)", 6, 2, 60);

    /** Một nhóm thông số trên màn hình cài đặt. */
    static final class Section {
        final String title;
        final String note;
        final Field[] fields;

        Section(String title, String note, Field... fields) {
            this.title = title;
            this.note = note;
            this.fields = fields;
        }
    }

    static final Section[] SECTIONS = {
            new Section("⚔ Farm (kênh ▶)",
                    "Đánh trận, chọn làng. \"Tự cải thiện\" cho bot tự chỉnh ngưỡng chọn làng và thời gian chờ theo kết quả đã học.",
                    CYCLES, MIN_LOOT, MAX_SKIPS, DEPLOY_COUNT, AUTO_TUNE),
            new Section("🛠 Nâng cấp & lộ trình",
                    "Dùng cho cả kênh Farm (nếu bật tự nâng cấp) và kênh Lộ trình (luôn nâng cấp). "
                            + "Chọn lộ trình 4 nếu đang ở TH16 muốn lên TH17. Nhà chính chỉ tự nâng khi bạn tích ô cho phép.",
                    ROADMAP, TOWN_HALL, ALLOW_TOWN_HALL, MAX_HEROES_BUSY, AUTO_UPGRADE, UPGRADE_EVERY,
                    CHANNEL_INTERVAL, MAX_UPGRADES, LIST_ROWS, LIST_PAGES),
            new Section("🏦 Kho",
                    "Điền sức chứa kho (xem trong game) để bot biết kho sắp đầy và xả vào tường.",
                    GOLD_CAP, ELIXIR_CAP, DARK_CAP, WALL_PCT, FULL_WAIT),
            new Section("⏱ Thời gian",
                    "Máy chậm hoặc mạng yếu thì tăng các thời gian chờ.",
                    TAP_DELAY, STEP_DELAY, TRAIN_WAIT, BATTLE_WAIT, SEARCH_WAIT, HOME_WAIT),
            new Section("👁 Nhận diện",
                    "Nhận nhầm → tăng độ giống; bỏ sót → giảm. Đổi cỡ hình mẫu thì phải dạy lại.",
                    MATCH_THRESHOLD, TEMPLATE_SIZE),
    };

    /** Mọi thông số, theo đúng thứ tự hiển thị trong các nhóm. */
    static final Field[] ALL;

    static {
        java.util.List<Field> l = new java.util.ArrayList<>();
        for (Section s : SECTIONS) java.util.Collections.addAll(l, s.fields);
        ALL = l.toArray(new Field[0]);
    }

    private Config() {
    }

    /** Gom thông số cho lõi bot. */
    static com.code02.autofarm.core.Settings settings(Context c) {
        com.code02.autofarm.core.Settings s = new com.code02.autofarm.core.Settings();
        s.cycles = get(c, CYCLES);
        s.minLoot = get(c, MIN_LOOT);
        s.maxSkips = get(c, MAX_SKIPS);
        s.autoUpgrade = on(c, AUTO_UPGRADE);
        s.upgradeEvery = get(c, UPGRADE_EVERY);
        s.roadmap = get(c, ROADMAP);
        s.maxUpgrades = get(c, MAX_UPGRADES);
        s.listRows = get(c, LIST_ROWS);
        s.listPages = get(c, LIST_PAGES);
        s.goldCap = get(c, GOLD_CAP);
        s.elixirCap = get(c, ELIXIR_CAP);
        s.darkCap = get(c, DARK_CAP);
        s.wallPct = get(c, WALL_PCT);
        s.fullWaitMin = get(c, FULL_WAIT);
        s.deployCount = get(c, DEPLOY_COUNT);
        s.tapDelay = get(c, TAP_DELAY);
        s.stepDelay = get(c, STEP_DELAY);
        s.trainWait = get(c, TRAIN_WAIT);
        s.battleWait = get(c, BATTLE_WAIT);
        s.matchPct = get(c, MATCH_THRESHOLD);
        s.searchWait = get(c, SEARCH_WAIT);
        s.homeWait = get(c, HOME_WAIT);
        s.channelIntervalMin = get(c, CHANNEL_INTERVAL);
        s.townHall = get(c, TOWN_HALL);
        s.allowTownHall = on(c, ALLOW_TOWN_HALL);
        s.maxHeroesBusy = get(c, MAX_HEROES_BUSY);
        s.autoTune = on(c, AUTO_TUNE);
        return s;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("autofarm_config", Context.MODE_PRIVATE);
    }

    static int get(Context c, Field f) {
        return prefs(c).getInt(f.key, f.def);
    }

    static boolean on(Context c, Field f) {
        return get(c, f) != 0;
    }

    static void set(Context c, Field f, int value) {
        int v = Math.max(f.min, Math.min(f.max, value));
        prefs(c).edit().putInt(f.key, v).apply();
    }
}
