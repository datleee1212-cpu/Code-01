package com.code02.autofarm;

import android.content.Context;
import android.content.SharedPreferences;

/** Các thông số thời gian và số lượng, chỉnh trong màn hình chính. */
final class Config {

    static final class Field {
        final String key;
        final String label;
        final int def;
        final int min;
        final int max;

        Field(String key, String label, int def, int min, int max) {
            this.key = key;
            this.label = label;
            this.def = def;
            this.min = min;
            this.max = max;
        }
    }

    static final Field CYCLES = new Field("cycles", "Số vòng farm (0 = chạy mãi)", 0, 0, 10000);
    static final Field DEPLOY_COUNT = new Field("deploy_count", "Số lần chạm thả mỗi ô quân", 10, 1, 100);
    static final Field TAP_DELAY = new Field("tap_delay", "Nghỉ giữa các lần thả quân (mili giây)", 150, 50, 5000);
    static final Field STEP_DELAY = new Field("step_delay", "Nghỉ giữa các bước bấm nút (mili giây)", 1500, 300, 10000);
    static final Field TRAIN_WAIT = new Field("train_wait", "Chờ luyện quân trước khi đánh (giây)", 0, 0, 3600);
    static final Field SEARCH_WAIT = new Field("search_wait", "Chờ tìm trận (giây)", 8, 2, 60);
    static final Field BATTLE_WAIT = new Field("battle_wait", "Thời gian đánh trước khi kết thúc (giây)", 90, 10, 180);
    static final Field HOME_WAIT = new Field("home_wait", "Chờ về làng (giây)", 6, 2, 60);

    static final Field[] ALL = {CYCLES, DEPLOY_COUNT, TAP_DELAY, STEP_DELAY, TRAIN_WAIT, SEARCH_WAIT, BATTLE_WAIT, HOME_WAIT};

    private Config() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("autofarm_config", Context.MODE_PRIVATE);
    }

    static int get(Context c, Field f) {
        return prefs(c).getInt(f.key, f.def);
    }

    static void set(Context c, Field f, int value) {
        int v = Math.max(f.min, Math.min(f.max, value));
        prefs(c).edit().putInt(f.key, v).apply();
    }
}
