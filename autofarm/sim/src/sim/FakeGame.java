package sim;

import com.code02.autofarm.core.Box;
import com.code02.autofarm.core.Device;
import com.code02.autofarm.core.Img;
import com.code02.autofarm.core.Roadmap;
import com.code02.autofarm.core.TextReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game giả lập kiểu "làng – tấn công – nâng cấp" để thử bot trên máy tính, không cần máy tính bảng.
 * Vẽ màn hình thật bằng điểm ảnh (có nhiễu), nhận chạm, đọc chữ giả (có lỗi), có thông báo bất ngờ,
 * thợ xây, kho, giá đỏ/trắng, và nút mua bằng ngọc làm bẫy.
 */
public final class FakeGame implements Device, TextReader {

    public static final int W = 1280;
    public static final int H = 720;

    enum Screen {
        HOME, ATTACK_MENU, SEARCHING, SCOUT, BATTLE, SURRENDER, RESULT, TRAIN,
        BUILDER_LIST, SELECTED, UPGRADE_DLG, GEM_POPUP, BUSY_POPUP, EVENT_POPUP, CONN_LOST, LOADING
    }

    /** Công trình trong làng. */
    public static final class Building {
        public final String name;
        public final Roadmap.Cat cat;
        public int level;
        public final int maxLevel;
        public long cost;
        public final int res;
        public final int minutes;
        public long busyUntil = -1;

        public Building(String name, Roadmap.Cat cat, int level, int maxLevel, long cost, int res, int minutes) {
            this.name = name;
            this.cat = cat;
            this.level = level;
            this.maxLevel = maxLevel;
            this.cost = cost;
            this.res = res;
            this.minutes = minutes;
        }

        boolean wall() {
            return cat == Roadmap.Cat.WALL;
        }
    }

    /** Phần tử giao diện. */
    static final class El {
        final String id;
        final Box box;
        final boolean button;
        final String text;
        final int color;

        El(String id, Box box, boolean button, String text, int color) {
            this.id = id;
            this.box = box;
            this.button = button;
            this.text = text;
            this.color = color;
        }
    }

    // ------------------------------------------------------------ tham số kịch bản
    public long[] own = {0, 1_000_000, 1_000_000, 10_000};
    public long[] cap = {0, 3_000_000, 3_000_000, 100_000};
    public int buildersTotal = 3;
    public double popupPerMin = 0.0;
    public double connLostPerMin = 0.0;
    public double ocrError = 0.0;
    public boolean screenshotSupported = true;
    public boolean elixirWalls = true;
    public final List<Building> buildings = new ArrayList<>();

    // ------------------------------------------------------------ trạng thái
    Screen screen = Screen.HOME;
    private Screen beforePopup = Screen.HOME;
    private final Random rnd;
    private long now = 0;
    private long screenSince = 0;
    boolean armyReady = false;
    private String selectedTroop = null;
    private int deployed = 0;
    private long scoutLootG;
    private long scoutLootE;
    private long battleEnd;
    private int listScroll = 0;
    Building selected;
    private boolean selectedElixir;
    final long[] collector = {0, 0, 0, 0};
    private final int[][] collectorPos = {{0, 0}, {520, 330}, {760, 420}, {380, 470}};
    private long lastTick = 0;

    // ------------------------------------------------------------ thống kê
    public int gemTaps;
    public int popupsShown;
    public int searches;
    public long lootGained;
    public long lootWasted;
    public final List<String> upgradesDone = new ArrayList<>();
    public final List<Building> upgradedRefs = new ArrayList<>();
    public int insufficientConfirms;
    public final List<String> log = new ArrayList<>();
    public String lastStatus = "";

    public FakeGame(long seed) {
        rnd = new Random(seed);
    }

    public void start(Screen s) {
        go(s);
    }

    private void go(Screen s) {
        screen = s;
        screenSince = now;
    }

    // ================================================================ mô phỏng thời gian

    private void tick() {
        long dt = now - lastTick;
        if (dt <= 0) return;
        lastTick = now;
        double minutes = dt / 60000.0;
        // Mỏ đầy dần
        collector[1] += (long) (dt * 0.5);
        collector[2] += (long) (dt * 0.5);
        collector[3] += (long) (dt * 0.005);
        // Thợ xây làm xong
        for (Building b : buildings) {
            if (b.busyUntil >= 0 && now >= b.busyUntil) {
                b.busyUntil = -1;
                b.level++;
                b.cost = Math.round(b.cost * 1.25);
            }
        }
        // Tự chuyển màn hình theo thời gian
        long in = now - screenSince;
        switch (screen) {
            case SEARCHING:
                if (in > 3000) {
                    scoutLootG = 50_000 + rnd.nextInt(750_000);
                    scoutLootE = 50_000 + rnd.nextInt(750_000);
                    go(Screen.SCOUT);
                }
                break;
            case SCOUT:
                if (in > 30000) startBattle();
                break;
            case BATTLE:
                if (now >= battleEnd) finishBattle();
                break;
            case LOADING:
                if (in > 4000) go(Screen.HOME);
                break;
            default:
                break;
        }
        // Thông báo bất ngờ
        if (screen != Screen.CONN_LOST && screen != Screen.LOADING && rnd.nextDouble() < connLostPerMin * minutes) {
            beforePopup = screen;
            go(Screen.CONN_LOST);
            popupsShown++;
        } else if (screen == Screen.HOME && rnd.nextDouble() < popupPerMin * minutes) {
            go(Screen.EVENT_POPUP);
            popupsShown++;
        }
    }

    private void startBattle() {
        go(Screen.BATTLE);
        battleEnd = now + (deployed > 0 ? 60_000 + rnd.nextInt(90_000) : 180_000);
    }

    private void finishBattle() {
        double pct = Math.min(1.0, deployed / 20.0) * 0.8;
        long g = Math.round(scoutLootG * pct);
        long e = Math.round(scoutLootE * pct);
        add(1, g);
        add(2, e);
        go(Screen.RESULT);
    }

    private void add(int res, long amount) {
        long room = Math.max(0, cap[res] - own[res]);
        long put = Math.min(room, amount);
        own[res] += put;
        lootGained += put;
        lootWasted += amount - put;
    }

    public int freeBuilders() {
        int busy = 0;
        for (Building b : buildings) if (b.busyUntil >= 0) busy++;
        return buildersTotal - busy;
    }

    /** Danh sách hiện trong bảng thợ xây: chưa tối đa, không đang nâng, sắp theo giá. */
    List<Building> listItems() {
        List<Building> l = new ArrayList<>();
        for (Building b : buildings) if (b.level < b.maxLevel && b.busyUntil < 0) l.add(b);
        l.sort((a, b) -> Long.compare(a.cost, b.cost));
        return l;
    }

    // ================================================================ bố cục từng màn hình

    static final int WHITE = 0xFFF5F5F5;
    static final int RED = 0xFFE03C3C;

    private static Box at(int cx, int cy, int w, int h) {
        return new Box(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
    }

    private static El btn(String id, int cx, int cy) {
        return new El(id, at(cx, cy, 72, 72), true, null, 0);
    }

    private static El txt(String id, Box b, String text, int color) {
        return new El(id, b, false, text, color);
    }

    static final int ROW_X0 = 420;
    static final int ROW_X1 = 900;
    static final int ROW_Y0 = 80;
    static final int ROW_H = 56;
    static final int ROWS = 5;

    List<El> layout() {
        List<El> l = new ArrayList<>();
        switch (screen) {
            case HOME:
            case BUILDER_LIST:
            case SELECTED:
                l.add(btn("btn_attack", 80, 640));
                l.add(btn("btn_train", 80, 540));
                l.add(btn("btn_builder", 600, 40));
                l.add(txt("builders", new Box(650, 25, 740, 55), freeBuilders() + "/" + buildersTotal, WHITE));
                l.add(txt("own_gold", new Box(1020, 20, 1180, 50), fmt(own[1]), WHITE));
                l.add(txt("own_elixir", new Box(1020, 70, 1180, 100), fmt(own[2]), WHITE));
                l.add(txt("own_dark", new Box(1020, 120, 1180, 150), fmt(own[3]), WHITE));
                String[] bub = {"", "bubble_gold", "bubble_elixir", "bubble_dark"};
                long[] full = {0, 200_000, 200_000, 2_000};
                for (int r = 1; r <= 3; r++) {
                    if (collector[r] >= full[r]) l.add(btn(bub[r], collectorPos[r][0], collectorPos[r][1]));
                }
                if (screen == Screen.BUILDER_LIST) {
                    List<Building> items = listItems();
                    for (int i = 0; i < ROWS; i++) {
                        int k = i + listScroll;
                        if (k >= items.size()) break;
                        Building b = items.get(k);
                        int y0 = ROW_Y0 + i * ROW_H;
                        Box row = new Box(ROW_X0, y0, ROW_X1, y0 + ROW_H - 8);
                        l.add(new El("row" + i, row, true, null, 0xFF37474F));
                        l.add(txt("rowname" + i, row.leftPart(0.6f), b.name, WHITE));
                        boolean ok = own[b.res] >= b.cost;
                        l.add(txt("rowcost" + i, new Box(row.r - 170, row.t + 8, row.r - 40, row.b - 8),
                                fmt(b.cost), ok ? WHITE : RED));
                        l.add(new El("icon" + b.res, new Box(row.r - 34, row.t + 10, row.r - 12, row.b - 10), false, null, resColor(b.res)));
                    }
                }
                if (screen == Screen.SELECTED && selected != null) {
                    l.add(btn("btn_info", 560, 640));
                    // Công trình có nút "tăng tốc" làm nút Nâng cấp lệch chỗ.
                    int ux = selected.cat == Roadmap.Cat.COLLECTOR || selected.cat == Roadmap.Cat.ARMY_BUILDING ? 790 : 700;
                    l.add(btn("btn_upgrade", ux, 640));
                    if (selected.wall() && elixirWalls) l.add(btn("btn_upgrade_elx", ux + 100, 640));
                }
                break;
            case ATTACK_MENU:
                l.add(btn("btn_find", 320, 560));
                l.add(btn("btn_x", 1200, 80));
                break;
            case SEARCHING:
            case LOADING:
                break;
            case SCOUT:
            case BATTLE:
                if (screen == Screen.SCOUT) {
                    l.add(btn("btn_next", 1180, 560));
                    l.add(txt("loot_gold", new Box(40, 80, 240, 110), fmt(scoutLootG), WHITE));
                    l.add(txt("loot_elixir", new Box(40, 120, 240, 150), fmt(scoutLootE), WHITE));
                }
                l.add(btn("btn_end", 80, 560));
                l.add(btn(armyReady ? "troop_slot" : "troop_empty", 200, 660));
                l.add(btn(armyReady ? "troop_slot" : "troop_empty", 290, 660));
                l.add(btn("hero_slot", 380, 660));
                break;
            case SURRENDER:
                l.add(btn("btn_ok", 740, 420));
                l.add(btn("btn_cancel", 540, 420));
                break;
            case RESULT:
                l.add(btn("btn_home", 640, 620));
                break;
            case TRAIN:
                l.add(btn("btn_quick", 400, 150));
                l.add(btn("btn_trainarmy", 900, 300));
                l.add(btn("btn_x", 1200, 80));
                break;
            case UPGRADE_DLG: {
                boolean ok = selected != null && own[costRes()] >= selected.cost;
                l.add(btn("btn_x", 1180, 90));
                Box cb = at(900, 600, 140, 72);
                l.add(new El("btn_confirm", cb, true, null, 0));
                l.add(txt("confirm_cost", at(900, 600, 50, 10), selected == null ? "" : fmt(selected.cost), ok ? WHITE : RED));
                break;
            }
            case GEM_POPUP:
            case BUSY_POPUP:
                l.add(btn("btn_x", 900, 200));
                l.add(btn("btn_gem", 640, 460));
                break;
            case EVENT_POPUP:
                l.add(btn("btn_x", 1050, 150));
                break;
            case CONN_LOST:
                l.add(btn("btn_reload", 640, 450));
                break;
        }
        return l;
    }

    private int costRes() {
        if (selected == null) return 1;
        return selected.wall() && selectedElixir ? 2 : selected.res;
    }

    static int resColor(int res) {
        return res == 1 ? 0xFFF0C820 : res == 2 ? 0xFFD040D8 : 0xFF2A1E48;
    }

    static String fmt(long v) {
        String s = Long.toString(v);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    // ================================================================ vẽ màn hình

    private Img render() {
        int[] px = new int[W * H];
        int shift = rnd.nextInt(11) - 5; // độ sáng dao động mỗi khung hình
        int bg = screen == Screen.SEARCHING || screen == Screen.LOADING ? 1 : 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int g;
                if (bg == 1) {
                    g = 200 + (int) (30 * Math.sin((x + y) / 50.0));
                } else {
                    g = 90 + (int) (40 * Math.sin(x / 41.0) * Math.cos(y / 57.0)) + ((x / 160 + y / 160) % 2) * 15;
                }
                g += shift + rnd.nextInt(9) - 4;
                int r = clamp(g - 20);
                int gg = clamp(g + 25);
                int b = clamp(g - 35);
                px[y * W + x] = 0xFF000000 | (r << 16) | (gg << 8) | b;
            }
        }
        Img img = new Img(W, H, px);
        // Lớp phủ tối cho hộp thoại
        if (screen == Screen.UPGRADE_DLG || screen == Screen.GEM_POPUP || screen == Screen.BUSY_POPUP
                || screen == Screen.EVENT_POPUP || screen == Screen.CONN_LOST || screen == Screen.TRAIN) {
            fill(img, new Box(160, 60, 1240, 680), 0xFF3E2723);
        }
        for (El e : layout()) {
            if (e.button && e.color == 0) drawPattern(img, e.box, e.id);
            else if (e.text != null) drawText(img, e.box, e.text, e.color);
            else fill(img, e.box, e.color);
        }
        return img;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void fill(Img img, Box b, int c) {
        for (int y = Math.max(0, b.t); y < Math.min(H, b.b); y++)
            for (int x = Math.max(0, b.l); x < Math.min(W, b.r); x++) img.px[y * W + x] = c;
    }

    /** Mỗi nút có họa tiết riêng (khối màu theo mã), giống mỗi nút trong game có hình riêng. */
    private void drawPattern(Img img, Box b, String id) {
        Random pr = new Random(id.hashCode() * 7919L);
        int cells = 6;
        int cw = Math.max(1, b.w() / cells);
        int ch = Math.max(1, b.h() / cells);
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                int c = 0xFF000000 | (pr.nextInt(256) << 16) | (pr.nextInt(256) << 8) | pr.nextInt(256);
                fill(img, new Box(b.l + i * cw, b.t + j * ch, b.l + (i + 1) * cw, b.t + (j + 1) * ch), c);
            }
        }
        // viền
        fill(img, new Box(b.l, b.t, b.r, b.t + 3), 0xFF101010);
        fill(img, new Box(b.l, b.b - 3, b.r, b.b), 0xFF101010);
    }

    /** "Chữ" giả: các vạch theo mã ký tự, đúng màu (trắng / đỏ). */
    private static void drawText(Img img, Box b, String s, int color) {
        fill(img, b, 0xFF202020);
        if (s.isEmpty()) return;
        int n = s.length();
        int gw = Math.max(1, b.w() / n);
        for (int k = 0; k < n; k++) {
            char c = s.charAt(k);
            if (c == ' ') continue;
            int x0 = b.l + k * gw;
            for (int bit = 0; bit < 6; bit++) {
                if (((c >> bit) & 1) == 0 && bit != 0) continue;
                int y0 = b.t + 2 + bit * Math.max(1, (b.h() - 4) / 6);
                fill(img, new Box(x0 + 1, y0, x0 + Math.max(2, gw - 1), y0 + Math.max(1, (b.h() - 4) / 7)), color);
            }
        }
    }

    // ================================================================ Device

    @Override
    public Img capture() {
        now += 120;
        tick();
        return screenshotSupported ? render() : null;
    }

    @Override
    public void tap(int x, int y) {
        now += 80;
        tick();
        El hit = null;
        for (El e : layout()) {
            if (e.button && x >= e.box.l && x < e.box.r && y >= e.box.t && y < e.box.b) {
                hit = e;
                break;
            }
        }
        onTap(hit, x, y);
    }

    private void onTap(El e, int x, int y) {
        String id = e == null ? "" : e.id;
        if (id.equals("btn_gem")) {
            gemTaps++;
            log.add("!!! BẤM NÚT MUA BẰNG NGỌC");
            return;
        }
        switch (screen) {
            case HOME:
                if (id.equals("btn_attack")) go(Screen.ATTACK_MENU);
                else if (id.equals("btn_train")) go(Screen.TRAIN);
                else if (id.equals("btn_builder")) {
                    listScroll = 0;
                    go(Screen.BUILDER_LIST);
                } else if (id.startsWith("bubble_")) {
                    int r = id.equals("bubble_gold") ? 1 : id.equals("bubble_elixir") ? 2 : 3;
                    add(r, collector[r]);
                    collector[r] = 0;
                }
                break;
            case BUILDER_LIST:
                if (id.startsWith("row")) {
                    int i = Integer.parseInt(id.substring(3));
                    List<Building> items = listItems();
                    int k = i + listScroll;
                    if (k < items.size()) {
                        selected = items.get(k);
                        go(Screen.SELECTED);
                    }
                } else {
                    go(Screen.HOME); // bấm biểu tượng thợ xây hoặc chỗ trống: đóng danh sách
                }
                break;
            case SELECTED:
                if (id.equals("btn_upgrade") || id.equals("btn_upgrade_elx")) {
                    selectedElixir = id.equals("btn_upgrade_elx");
                    go(Screen.UPGRADE_DLG);
                } else if (id.equals("btn_attack")) go(Screen.ATTACK_MENU);
                else if (id.equals("btn_builder")) {
                    listScroll = 0;
                    go(Screen.BUILDER_LIST);
                } else if (!id.equals("btn_info")) go(Screen.HOME);
                break;
            case UPGRADE_DLG:
                if (id.equals("btn_x")) go(Screen.HOME);
                else if (id.equals("btn_confirm")) confirmUpgrade();
                break;
            case GEM_POPUP:
            case BUSY_POPUP:
            case EVENT_POPUP:
                if (id.equals("btn_x")) go(Screen.HOME);
                break;
            case CONN_LOST:
                if (id.equals("btn_reload")) go(Screen.LOADING);
                break;
            case ATTACK_MENU:
                if (id.equals("btn_find")) {
                    own[1] = Math.max(0, own[1] - 1000);
                    searches++;
                    deployed = 0;
                    go(Screen.SEARCHING);
                } else if (id.equals("btn_x")) go(Screen.HOME);
                break;
            case SCOUT:
                if (id.equals("btn_next")) {
                    own[1] = Math.max(0, own[1] - 1000);
                    searches++;
                    go(Screen.SEARCHING);
                    break;
                }
                // fall through: trong lúc xem làng cũng thả quân / kết thúc được
            case BATTLE:
                if (id.equals("btn_end")) {
                    if (deployed == 0 && screen == Screen.SCOUT) go(Screen.HOME);
                    else go(Screen.SURRENDER);
                } else if (id.equals("troop_slot") || id.equals("hero_slot")) {
                    selectedTroop = id;
                } else if (e == null && selectedTroop != null && y > 200 && y < 600 && x > 300 && x < 1100) {
                    deployed++;
                    if (deployed >= 20 && selectedTroop.equals("troop_slot")) armyReady = false;
                    if (screen == Screen.SCOUT) startBattle();
                }
                break;
            case SURRENDER:
                if (id.equals("btn_ok")) finishBattle();
                else if (id.equals("btn_cancel")) go(Screen.BATTLE);
                break;
            case RESULT:
                if (id.equals("btn_home")) {
                    selectedTroop = null;
                    go(Screen.HOME);
                }
                break;
            case TRAIN:
                if (id.equals("btn_trainarmy")) armyReady = true;
                else if (id.equals("btn_x")) go(Screen.HOME);
                break;
            default:
                break;
        }
    }

    private void confirmUpgrade() {
        int res = costRes();
        if (selected == null) return;
        if (own[res] < selected.cost) {
            insufficientConfirms++;
            go(Screen.GEM_POPUP);
            return;
        }
        if (freeBuilders() <= 0) {
            go(Screen.BUSY_POPUP);
            return;
        }
        own[res] -= selected.cost;
        upgradesDone.add(selected.name);
        upgradedRefs.add(selected);
        if (selected.wall()) {
            selected.level++;
        } else {
            selected.busyUntil = now + selected.minutes * 60_000L;
        }
        go(Screen.HOME);
    }

    @Override
    public void swipe(int x1, int y1, int x2, int y2, int ms) {
        now += ms;
        tick();
        if (screen == Screen.BUILDER_LIST && y2 < y1) {
            int rows = Math.round((y1 - y2) / (float) ROW_H);
            int max = Math.max(0, listItems().size() - ROWS);
            listScroll = Math.min(max, listScroll + rows);
        }
    }

    @Override
    public void sleep(long ms) {
        // Bước nhỏ để thông báo bất ngờ có thể xuất hiện giữa chừng.
        long end = now + ms;
        while (now < end) {
            now = Math.min(end, now + 1000);
            tick();
        }
    }

    @Override
    public long now() {
        return now;
    }

    @Override
    public void status(String s) {
        lastStatus = s;
        if (log.isEmpty() || !log.get(log.size() - 1).equals(s)) log.add(String.format("%7.1fs %s", now / 1000.0, s));
    }

    // ================================================================ OCR giả

    @Override
    public String read(Img img, Box region, boolean digits) {
        now += 150;
        El best = null;
        int bestArea = 0;
        for (El e : layout()) {
            if (e.text == null) continue;
            int ix = Math.min(region.r, e.box.r) - Math.max(region.l, e.box.l);
            int iy = Math.min(region.b, e.box.b) - Math.max(region.t, e.box.t);
            if (ix <= 0 || iy <= 0) continue;
            int area = ix * iy;
            if (area * 2 >= e.box.w() * e.box.h() && area > bestArea) {
                best = e;
                bestArea = area;
            }
        }
        if (best == null) return null;
        if (rnd.nextDouble() < ocrError) {
            // Lỗi đọc: hoặc không đọc được, hoặc nhầm 0 thành O (bot phải tự sửa).
            return rnd.nextBoolean() ? null : best.text.replace('0', 'O');
        }
        return best.text;
    }
}
