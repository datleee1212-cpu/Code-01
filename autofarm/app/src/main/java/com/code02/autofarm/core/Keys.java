package com.code02.autofarm.core;

/** Tên các mục được dạy, dùng chung cho app và giả lập. */
public final class Keys {
    private Keys() {
    }

    // Thu tài nguyên
    public static final String COLLECT_GOLD = "collect_gold";
    public static final String COLLECT_ELIXIR = "collect_elixir";
    public static final String COLLECT_DARK = "collect_dark";
    public static final String[] COLLECTS = {COLLECT_GOLD, COLLECT_ELIXIR, COLLECT_DARK};

    // Đánh trận
    public static final String TRAIN_OPEN = "train_open";
    public static final String QUICK_TRAIN = "quick_train";
    public static final String TRAIN_ARMY = "train_army";
    public static final String TRAIN_CLOSE = "train_close";
    public static final String ATTACK = "attack";
    public static final String FIND_MATCH = "find_match";
    public static final String NEXT = "next";
    public static final String LOOT_GOLD = "loot_gold";
    public static final String LOOT_ELIXIR = "loot_elixir";
    public static final String TROOP1 = "troop1";
    public static final String TROOP2 = "troop2";
    public static final String HERO = "hero";
    public static final String DEPLOY_A = "deploy_a";
    public static final String DEPLOY_B = "deploy_b";
    public static final String END_BATTLE = "end_battle";
    public static final String SURRENDER_OK = "surrender_ok";
    public static final String RETURN_HOME = "return_home";

    // Kho & nâng cấp
    public static final String OWN_GOLD = "own_gold";
    public static final String OWN_ELIXIR = "own_elixir";
    public static final String OWN_DARK = "own_dark";
    public static final String BUILDERS_FREE = "builders_free";
    public static final String BUILDER = "builder";
    public static final String UPGRADE_ROW1 = "upgrade_row1";
    public static final String UPGRADE_ROW2 = "upgrade_row2";
    public static final String UPGRADE_BTN = "upgrade_btn";
    public static final String UPGRADE_BTN_ELIXIR = "upgrade_btn_elixir";
    public static final String UPGRADE_CONFIRM = "upgrade_confirm";

    // Thông báo
    public static final String[] POPUPS = {"close1", "close2", "close3", "close4", "close5", "close6"};

    public static final String[] REQUIRED = {ATTACK, FIND_MATCH, DEPLOY_A, RETURN_HOME};

    /** Mục cần tìm trên cả màn hình (không cố định chỗ). */
    public static boolean fullScope(String key) {
        if (key.startsWith("close") || key.startsWith("collect_")) return true;
        return key.equals(UPGRADE_BTN) || key.equals(UPGRADE_BTN_ELIXIR) || key.equals(UPGRADE_CONFIRM);
    }
}
