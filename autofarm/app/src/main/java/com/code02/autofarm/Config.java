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

    static final Field CYCLES = new Field("cycles", "Số vòng farm (0 = chạy mãi)", 0, 0, 100000);
    static final Field MIN_LOOT = new Field("min_loot", "Chỉ đánh làng có vàng + tiên dược từ (0 = đánh mọi làng)", 300000, 0, 20000000);
    static final Field MAX_SKIPS = new Field("max_skips", "Bỏ qua tối đa bao nhiêu làng rồi đánh luôn", 20, 0, 200);
    static final Field AUTO_UPGRADE = new Field("auto_upgrade", "Tự nâng cấp công trình", 0, 0, 1, true);
    static final Field UPGRADE_EVERY = new Field("upgrade_every", "Thử nâng cấp sau mỗi bao nhiêu vòng", 3, 1, 100);
    static final Field DEPLOY_COUNT = new Field("deploy_count", "Số lần chạm thả mỗi ô quân", 10, 1, 100);
    static final Field TAP_DELAY = new Field("tap_delay", "Nghỉ giữa các lần thả quân (mili giây)", 150, 50, 5000);
    static final Field STEP_DELAY = new Field("step_delay", "Nghỉ giữa các bước bấm nút (mili giây)", 1500, 300, 10000);
    static final Field TRAIN_WAIT = new Field("train_wait", "Chờ luyện quân trước khi đánh (giây)", 0, 0, 3600);
    static final Field BATTLE_WAIT = new Field("battle_wait", "Thời gian đánh tối đa trước khi kết thúc (giây)", 90, 10, 180);
    static final Field MATCH_THRESHOLD = new Field("match_thr", "Độ giống tối thiểu khi nhận diện hình (%)", 82, 60, 99);
    static final Field TEMPLATE_SIZE = new Field("tpl_size", "Cỡ hình mẫu khi dạy (% cạnh ngắn màn hình)", 8, 3, 25);
    static final Field SEARCH_WAIT = new Field("search_wait", "[Không đọc màn hình] Chờ tìm trận (giây)", 8, 2, 60);
    static final Field HOME_WAIT = new Field("home_wait", "[Không đọc màn hình] Chờ về làng (giây)", 6, 2, 60);

    static final Field[] ALL = {
            CYCLES, MIN_LOOT, MAX_SKIPS, AUTO_UPGRADE, UPGRADE_EVERY,
            DEPLOY_COUNT, TAP_DELAY, STEP_DELAY, TRAIN_WAIT, BATTLE_WAIT,
            MATCH_THRESHOLD, TEMPLATE_SIZE, SEARCH_WAIT, HOME_WAIT,
    };

    private Config() {
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
