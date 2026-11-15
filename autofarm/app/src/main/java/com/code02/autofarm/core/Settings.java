package com.code02.autofarm.core;

/** Thông số chạy (app đọc từ màn hình cài đặt; giả lập tự đặt). */
public final class Settings {
    public int cycles = 0;
    public int minLoot = 300000;
    public int maxSkips = 20;
    public boolean autoUpgrade = false;
    public int upgradeEvery = 3;
    public int maxUpgrades = 3;
    public int roadmap = Roadmap.BALANCED;
    public int listRows = 5;
    public int listPages = 2;
    public int wallPct = 90;
    public int fullWaitMin = 10;
    public int goldCap = 0;
    public int elixirCap = 0;
    public int darkCap = 0;
    public int deployCount = 10;
    public int tapDelay = 150;
    public int stepDelay = 1500;
    public int trainWait = 0;
    public int battleWait = 90;
    public int matchPct = 82;
    public int searchWait = 8;
    public int homeWait = 6;
    /** Kênh Lộ trình: kiểm tra thợ xây mỗi bao nhiêu phút. */
    public int channelIntervalMin = 30;
    /** Cấp Nhà chính hiện tại (dùng cho lộ trình TH16 → TH17). */
    public int townHall = 16;
    /** Cho phép bot tự nâng Nhà chính (mặc định tắt: tự quyết định khi nào lên). */
    public boolean allowTownHall = false;
    /** Số tướng nâng cùng lúc tối đa (giữ tướng đi war). */
    public int maxHeroesBusy = 2;
    /** Tự chỉnh thông số theo kết quả đã học. */
    public boolean autoTune = true;
}
