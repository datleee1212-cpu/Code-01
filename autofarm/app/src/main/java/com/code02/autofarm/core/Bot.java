package com.code02.autofarm.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ não của bot: vòng farm, xử lý tình huống, quản lý tài nguyên và nâng cấp theo lộ trình.
 * Không phụ thuộc Android: app và giả lập cùng chạy đúng đoạn mã này.
 */
public final class Bot {

    private final Device dev;
    private final TextReader ocr;
    private final Store store;
    private final Settings cfg;
    private final Memory memory;
    private final Learner learner;
    private final Map<String, Vision.Tpl> tplCache = new HashMap<>();

    /** Thống kê để báo cáo / kiểm thử. */
    public static final class Stats {
        public int cycles;
        public int attacks;
        public int skippedBases;
        public int popupsClosed;
        public int recoveries;
        public int collected;
        public int upgrades;
        public int walls;
        public int fullWaits;
        public final List<String> upgradeLog = new ArrayList<>();
        public final List<Long> attackedStorable = new ArrayList<>();
        /** Ngưỡng chọn làng dùng ở lần tìm trận gần nhất (-1 = chưa tìm). */
        public long threshold = -1;
        /** Số lần ngưỡng tự học khác cài đặt người dùng. */
        public int tunedSearches;
        /** Số làng đã xem (đọc được tài nguyên). */
        public int scouted;
    }

    public final Stats stats = new Stats();

    // Kho của mình (đọc ở làng chính). -1 = chưa biết.
    private final long[] own = {-1, -1, -1, -1};

    public Bot(Device dev, TextReader ocr, Store store, Settings cfg) {
        this(dev, ocr, store, cfg, Memory.inRam());
    }

    public Bot(Device dev, TextReader ocr, Store store, Settings cfg, Memory memory) {
        this.dev = dev;
        this.ocr = ocr;
        this.store = store;
        this.cfg = cfg;
        this.memory = memory;
        this.learner = new Learner(memory);
        learner.note(cfg.minLoot, cfg.stepDelay, cfg.maxSkips, cfg.matchPct);
    }

    public Memory memory() {
        return memory;
    }

    /** Phần tự học (dùng chung bộ nhớ với bot). */
    public Learner learner() {
        return learner;
    }

    /** Độ trễ sau chạm: đã học (khi bật tự chỉnh) hoặc đúng cài đặt. */
    private long step() {
        return cfg.autoTune ? learner.stepDelay(cfg.stepDelay) : cfg.stepDelay;
    }

    /** Co giãn một lần chờ cố định theo độ trễ đã học, không thấp hơn floorMs. */
    private long scaled(long ms, long floorMs) {
        if (!cfg.autoTune) return ms;
        return Math.max(Math.min(ms, floorMs), Math.min(ms * 2, Math.round(ms * learner.stepFactor(cfg.stepDelay))));
    }

    // ================================================================ tiện ích

    private void status(String s) {
        dev.status(s);
    }

    private void sleep(long ms) throws InterruptedException {
        if (ms > 0) dev.sleep(ms);
    }

    private float thr() {
        return cfg.matchPct / 100f;
    }

    private Vision.Tpl tpl(String key) {
        if (tplCache.containsKey(key)) return tplCache.get(key);
        Img i = store.template(key);
        Vision.Tpl t = i == null ? null : new Vision.Tpl(i);
        if (t != null && !t.usable()) t = null;
        tplCache.put(key, t);
        return t;
    }

    /** Mục này có hình mẫu để nhận diện không. */
    public boolean visual(String key) {
        return visionOk && tpl(key) != null;
    }

    private boolean visionOk = true;

    private Vision.Frame frame() throws InterruptedException {
        Img i = dev.capture();
        if (i == null) {
            visionOk = false;
            return null;
        }
        return new Vision.Frame(i);
    }

    public Vision.Match find(Vision.Frame f, String key) {
        if (f == null) return null;
        Vision.Tpl t = tpl(key);
        if (t == null) return null;
        int[] c = store.point(key);
        Vision.Match found;
        if (!Keys.fullScope(key) && c != null) {
            int m = Math.max(t.side * 2, Math.min(f.width(), f.height()) / 8);
            found = Vision.find(f, t, c[0] - m, c[1] - m, c[0] + m, c[1] + m, thr());
        } else {
            found = Vision.find(f, t, 0, 0, f.width(), f.height(), thr());
        }
        if (found != null) learner.matched(key, found.score);
        return found;
    }

    private boolean handlePopups(Vision.Frame f) throws InterruptedException {
        for (String key : Keys.POPUPS) {
            if (!visual(key)) continue;
            Vision.Match m = find(f, key);
            if (m != null) {
                status("Đóng thông báo (" + Math.round(m.score * 100) + "%)");
                dev.tap(m.x, m.y);
                stats.popupsClosed++;
                sleep(1000);
                return true;
            }
        }
        return false;
    }

    private void closeAllPopups() throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            Vision.Frame f = frame();
            if (f == null || !handlePopups(f)) return;
        }
    }

    private Vision.Match waitFor(String key, long timeoutMs) throws InterruptedException {
        long end = dev.now() + timeoutMs;
        int frames = 0;
        while (true) {
            Vision.Frame f = frame();
            if (f == null) return null;
            frames++;
            Vision.Match m = find(f, key);
            if (m != null) {
                learner.waited(key, frames, true);
                return m;
            }
            if (dev.now() > end) {
                learner.waited(key, frames, false);
                return null;
            }
            if (!handlePopups(f)) sleep(500);
        }
    }

    private boolean tapPoint(String key, long waitAfterMs) throws InterruptedException {
        int[] pt = store.point(key);
        if (pt == null) return false;
        dev.tap(pt[0], pt[1]);
        sleep(waitAfterMs);
        return true;
    }

    private boolean tapTarget(String key, long timeoutMs, long waitAfterMs) throws InterruptedException {
        if (visual(key)) {
            Vision.Match m = waitFor(key, timeoutMs);
            if (m == null) return false;
            dev.tap(m.x, m.y);
            sleep(waitAfterMs);
            return true;
        }
        return tapPoint(key, waitAfterMs);
    }

    private long readNumber(Img img, String regionKey) {
        Box r = store.region(regionKey);
        if (r == null || img == null) return -1;
        return Parse.number(ocr.read(img, r, true));
    }

    // ================================================================ kho & tài nguyên

    private int cap(int res) {
        return res == 1 ? cfg.goldCap : res == 2 ? cfg.elixirCap : res == 3 ? cfg.darkCap : 0;
    }

    /** Độ đầy kho 0..1, hoặc -1 nếu không biết. */
    public float fill(int res) {
        int c = cap(res);
        if (c <= 0 || own[res] < 0) return -1;
        return Math.min(1f, (float) own[res] / c);
    }

    private float[] fills() {
        return new float[]{-1, fill(1), fill(2), fill(3)};
    }

    private void readOwn(Vision.Frame f) {
        if (f == null) return;
        long g = readNumber(f.img, Keys.OWN_GOLD);
        long e = readNumber(f.img, Keys.OWN_ELIXIR);
        long d = readNumber(f.img, Keys.OWN_DARK);
        if (g >= 0) own[1] = g;
        if (e >= 0) own[2] = e;
        if (d >= 0) own[3] = d;
    }

    /** Phần cướp được mà kho còn chứa được. */
    public long storable(int res, long loot) {
        if (loot <= 0) return 0;
        int c = cap(res);
        if (c <= 0 || own[res] < 0) return loot;
        return Math.max(0, Math.min(loot, c - own[res]));
    }

    private boolean storagesFull() {
        float g = fill(1);
        float e = fill(2);
        return g >= 0.98f && e >= 0.98f;
    }

    private String ownText() {
        StringBuilder sb = new StringBuilder();
        String[] n = {"", "Vàng", "Tiên dược", "Đen"};
        for (int r = 1; r <= 3; r++) {
            if (own[r] < 0) continue;
            if (sb.length() > 0) sb.append(" • ");
            sb.append(n[r]).append(' ').append(own[r]);
            float f = fill(r);
            if (f >= 0) sb.append(" (").append(Math.round(f * 100)).append("%)");
        }
        return sb.toString();
    }

    // ================================================================ các bước farm

    private boolean ensureHome() throws InterruptedException {
        if (!visual(Keys.ATTACK)) return true;
        long end = dev.now() + 60000;
        String[] escapes = {Keys.RETURN_HOME, Keys.SURRENDER_OK, Keys.END_BATTLE, Keys.TRAIN_CLOSE};
        boolean recovered = false;
        while (dev.now() < end) {
            Vision.Frame f = frame();
            if (f == null) return true;
            if (find(f, Keys.ATTACK) != null) {
                if (recovered) stats.recoveries++;
                readOwn(f);
                return true;
            }
            if (handlePopups(f)) continue;
            boolean tapped = false;
            for (String k : escapes) {
                if (!visual(k)) continue;
                Vision.Match m = find(f, k);
                if (m != null) {
                    status("Đang quay về làng…");
                    dev.tap(m.x, m.y);
                    recovered = true;
                    sleep(cfg.stepDelay);
                    tapped = true;
                    break;
                }
            }
            if (!tapped) {
                status("Chờ màn hình làng chính…");
                sleep(1500);
            }
        }
        return false;
    }

    private void collect() throws InterruptedException {
        boolean anyVisual = false;
        for (String k : Keys.COLLECTS) if (visual(k)) anyVisual = true;
        if (!anyVisual) {
            for (String k : Keys.COLLECTS) tapPoint(k, cfg.stepDelay / 2);
            return;
        }
        Vision.Frame f = frame();
        if (f == null) return;
        int n = 0;
        for (String k : Keys.COLLECTS) {
            if (!visual(k)) continue;
            for (Vision.Match m : Vision.findAll(f, tpl(k), 0, 0, f.width(), f.height(), thr(), 15)) {
                dev.tap(m.x, m.y);
                n++;
                sleep(cfg.tapDelay + 150);
            }
        }
        stats.collected += n;
        if (n > 0) status("Đã thu " + n + " chỗ tài nguyên");
    }

    private void train() throws InterruptedException {
        if (store.point(Keys.TRAIN_OPEN) != null && tapTarget(Keys.TRAIN_OPEN, 4000, cfg.stepDelay)) {
            tapTarget(Keys.QUICK_TRAIN, 4000, cfg.stepDelay);
            tapTarget(Keys.TRAIN_ARMY, 4000, cfg.stepDelay);
            tapTarget(Keys.TRAIN_CLOSE, 4000, cfg.stepDelay);
        }
        for (int s = cfg.trainWait; s > 0; s--) {
            status("Chờ quân … " + s + " giây");
            sleep(1000);
        }
    }

    // Làng đã chọn ở lần tìm trận gần nhất (cho phần tự học).
    private long pickValue = -1;
    private long pickGold = -1;
    private long pickElixir = -1;
    private int pickLooks = 1;

    private void picked(long value, long gold, long elixir, int looks) {
        pickValue = value;
        pickGold = gold;
        pickElixir = elixir;
        pickLooks = Math.max(1, looks);
        stats.attackedStorable.add(value);
    }

    private boolean chooseBase(String tag) throws InterruptedException {
        pickValue = pickGold = pickElixir = -1;
        pickLooks = 1;
        if (!visual(Keys.NEXT)) {
            for (int s = cfg.searchWait; s > 0; s--) {
                status(tag + "đang tìm trận … " + s);
                sleep(1000);
            }
            return true;
        }
        // Ngưỡng: tự học (khi bật và đủ dữ liệu) hoặc đúng cài đặt.
        long minLoot = cfg.autoTune ? learner.threshold(cfg.minLoot, cfg.maxSkips) : cfg.minLoot;
        stats.threshold = minLoot;
        if (minLoot != cfg.minLoot) stats.tunedSearches++;
        String thrText = minLoot != cfg.minLoot ? " (ngưỡng tự học " + minLoot + ")" : "";
        long lastNext = -1;
        for (int skip = 0; ; skip++) {
            status(tag + "đang tìm trận…");
            Vision.Match next = waitFor(Keys.NEXT, 30000);
            if (next == null) {
                status(tag + "không thấy nút Tiếp theo");
                return false;
            }
            // Không lọc làng và không học → đánh luôn như trước.
            if (cfg.minLoot <= 0 && !cfg.autoTune) {
                picked(-1, -1, -1, skip + 1);
                return true;
            }
            sleep(scaled(1200, 800));
            Vision.Frame f = frame();
            long g = f == null ? -1 : readNumber(f.img, Keys.LOOT_GOLD);
            long e = f == null ? -1 : readNumber(f.img, Keys.LOOT_ELIXIR);
            if (g < 0 && e < 0) {
                status(tag + "không đọc được tài nguyên → đánh luôn");
                picked(-1, -1, -1, skip + 1);
                return true;
            }
            long useG = storable(1, g);
            long useE = storable(2, e);
            long useful = useG + useE;
            stats.scouted++;
            learner.scouted(g, e, useful);
            if (lastNext >= 0) learner.searchGap(dev.now() - lastNext);
            if (useful >= minLoot) {
                status(tag + "làng ngon: dùng được " + useful + thrText + " → đánh");
                picked(useful, useG, useE, skip + 1);
                return true;
            }
            if (skip >= cfg.maxSkips) {
                status(tag + "đã bỏ qua " + skip + " làng → đánh luôn");
                picked(useful, useG, useE, skip + 1);
                return true;
            }
            status(tag + "làng nghèo (dùng được " + useful + thrText + ") → Tiếp theo");
            stats.skippedBases++;
            learner.skipped();
            Vision.Match m = f == null ? null : find(f, Keys.NEXT);
            if (m == null) m = next;
            lastNext = dev.now();
            dev.tap(m.x, m.y);
            sleep(scaled(1500, 500));
        }
    }

    private void deployLine() throws InterruptedException {
        int[] a = store.point(Keys.DEPLOY_A);
        if (a == null) return;
        int[] b = store.point(Keys.DEPLOY_B);
        if (b == null) b = a;
        int n = cfg.deployCount;
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0.5f : (float) i / (n - 1);
            dev.tap(Math.round(a[0] + (b[0] - a[0]) * t), Math.round(a[1] + (b[1] - a[1]) * t));
            sleep(cfg.tapDelay);
        }
    }

    private void deploy(String tag) throws InterruptedException {
        status(tag + "thả quân");
        if (tapPoint(Keys.TROOP1, 300)) deployLine();
        if (tapPoint(Keys.TROOP2, 300)) deployLine();
        if (tapPoint(Keys.HERO, 300)) {
            int[] a = store.point(Keys.DEPLOY_A);
            int[] b = store.point(Keys.DEPLOY_B);
            if (b == null) b = a;
            if (a != null) dev.tap((a[0] + b[0]) / 2, (a[1] + b[1]) / 2);
        }
    }

    private void battle(String tag) throws InterruptedException {
        if (!visual(Keys.RETURN_HOME)) {
            for (int s = cfg.battleWait; s > 0; s--) {
                status(tag + "đang đánh … " + s);
                sleep(1000);
            }
            tapPoint(Keys.END_BATTLE, step());
            tapPoint(Keys.SURRENDER_OK, step());
            tapPoint(Keys.RETURN_HOME, 0);
            for (int s = cfg.homeWait; s > 0; s--) {
                status(tag + "về làng … " + s);
                sleep(1000);
            }
            return;
        }
        long end = dev.now() + cfg.battleWait * 1000L;
        boolean ended = false;
        while (dev.now() < end) {
            Vision.Frame f = frame();
            if (f != null && find(f, Keys.RETURN_HOME) != null) {
                ended = true;
                break;
            }
            if (f != null && handlePopups(f)) {
                // Ví dụ mất kết nối giữa trận: trận coi như kết thúc, quay về làng ở vòng sau.
                status(tag + "trận bị gián đoạn bởi thông báo");
                return;
            }
            status(tag + "đang đánh … còn " + (end - dev.now()) / 1000 + " giây");
            sleep(1000);
        }
        if (!ended) {
            status(tag + "hết giờ → kết thúc trận");
            tapTarget(Keys.END_BATTLE, 3000, step());
            tapTarget(Keys.SURRENDER_OK, 3000, step());
        }
        status(tag + "về làng");
        tapTarget(Keys.RETURN_HOME, 20000, step());
    }

    // ================================================================ nâng cấp theo lộ trình

    /** Đã dạy đủ để tự nâng cấp chưa (trống = đủ). */
    public String upgradeMissing() {
        StringBuilder sb = new StringBuilder();
        if (store.point(Keys.BUILDER) == null) sb.append("biểu tượng thợ xây, ");
        if (store.region(Keys.UPGRADE_ROW1) == null) sb.append("dòng 1 danh sách, ");
        if (!visual(Keys.UPGRADE_BTN)) sb.append("nút Nâng cấp, ");
        if (!visual(Keys.UPGRADE_CONFIRM)) sb.append("nút giá xác nhận, ");
        return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 2);
    }

    private int freeBuilders(Vision.Frame f) {
        Box r = store.region(Keys.BUILDERS_FREE);
        if (r == null || f == null) return -1;
        int[] ab = Parse.fraction(ocr.read(f.img, r, true));
        return ab == null ? -1 : ab[0];
    }

    private int rowStep() {
        Box r1 = store.region(Keys.UPGRADE_ROW1);
        int[] p2 = store.point(Keys.UPGRADE_ROW2);
        if (r1 == null || p2 == null) return 0;
        return p2[1] - r1.cy();
    }

    private Box rowBox(int i) {
        return store.region(Keys.UPGRADE_ROW1).shift(0, rowStep() * i);
    }

    private void scrollList() throws InterruptedException {
        int step = rowStep();
        int rows = Math.max(2, cfg.listRows);
        if (step <= 0) return;
        Box last = rowBox(rows - 1);
        Box first = rowBox(0);
        dev.swipe(last.cx(), last.cy(), first.cx(), first.cy(), 1200);
        sleep(900);
    }

    /** Mở danh sách thợ xây, đọc các dòng (qua nhiều trang), rồi đóng lại. */
    private List<Roadmap.Row> scanList() throws InterruptedException {
        List<Roadmap.Row> rows = new ArrayList<>();
        if (!tapTarget(Keys.BUILDER, 5000, cfg.stepDelay)) return rows;
        int pages = rowStep() > 0 ? Math.max(1, cfg.listPages) : 1;
        int perPage = rowStep() > 0 ? Math.max(1, cfg.listRows) : 1;
        for (int p = 0; p < pages; p++) {
            Vision.Frame f = frame();
            if (f == null) break;
            int got = 0;
            for (int i = 0; i < perPage; i++) {
                Box b = rowBox(i);
                if (!b.inside(f.width(), f.height())) break;
                String name = ocr.read(f.img, b.leftPart(0.62f), false);
                if (name == null || Roadmap.normalize(name).trim().isEmpty()) continue;
                Box cost = b.rightPart(0.38f);
                boolean affordable = Vision.redRatio(f.img, cost) < 0.03f;
                int res = Vision.resourceColor(f.img, cost);
                long price = Parse.number(ocr.read(f.img, cost, true)); // -1 nếu không đọc được
                rows.add(new Roadmap.Row(p, i, name.trim(), Roadmap.classify(name), affordable, res, price));
                got++;
            }
            if (got == 0 || p == pages - 1) break;
            scrollList();
        }
        tapTarget(Keys.BUILDER, 3000, cfg.stepDelay); // đóng danh sách
        closeAllPopups();
        return rows;
    }

    /** Bấm vào dòng đã chọn và nâng cấp. */
    private boolean execute(Roadmap.Row row, Roadmap.Economy eco) throws InterruptedException {
        if (!ensureHome()) return false;
        if (!tapTarget(Keys.BUILDER, 5000, cfg.stepDelay)) return false;
        for (int p = 0; p < row.page; p++) scrollList();
        Box b = rowBox(row.index);
        dev.tap(b.cx(), b.cy());
        sleep(cfg.stepDelay * 2L);

        // Tường: trả bằng loại tài nguyên đang dư (bộ chọn báo qua eco.wallRes), không rõ thì loại đầy hơn.
        int wallRes = eco == null ? 0 : eco.wallRes;
        boolean useElixir = row.cat == Roadmap.Cat.WALL && visual(Keys.UPGRADE_BTN_ELIXIR)
                && (wallRes == 2 || (wallRes == 0 && fill(2) > fill(1)));
        boolean opened = useElixir
                ? tapTarget(Keys.UPGRADE_BTN_ELIXIR, 4000, cfg.stepDelay)
                : tapTarget(Keys.UPGRADE_BTN, 5000, cfg.stepDelay);
        if (!opened) {
            status("Nâng cấp: không thấy nút Nâng cấp");
            closeAllPopups();
            return false;
        }
        Vision.Match c = waitFor(Keys.UPGRADE_CONFIRM, 5000);
        if (c == null) {
            closeAllPopups();
            return false;
        }
        Vision.Frame f = frame();
        Vision.Tpl t = tpl(Keys.UPGRADE_CONFIRM);
        int half = t.side / 2;
        // So với lúc dạy (giá màu trắng): nút có sẵn chi tiết đỏ thì không bị báo nhầm.
        Img taught = store.template(Keys.UPGRADE_CONFIRM);
        float base = taught == null ? 0 : Vision.redRatio(taught, new Box(0, 0, taught.w, taught.h));
        float now = f == null ? 0 : Vision.redRatio(f.img, new Box(c.x - half, c.y - half, c.x + half, c.y + half));
        if (f != null && now - base > 0.02f) {
            status("Nâng cấp: giá màu đỏ (không đủ) → bỏ qua");
            closeAllPopups();
            return false;
        }
        dev.tap(c.x, c.y);
        sleep(cfg.stepDelay);
        closeAllPopups();
        return true;
    }

    /** Một lượt nâng cấp: đọc kho, số thợ, chọn mục theo lộ trình, nâng cấp tới khi hết thợ / hết mục hợp lý. */
    private void upgradeSession() throws InterruptedException {
        String miss = upgradeMissing();
        if (!miss.isEmpty()) {
            status("Tự nâng cấp: chưa dạy " + miss);
            sleep(1500);
            return;
        }
        // Tường nâng tức thì (không tốn thời gian thợ) → cho phép nhiều lượt để xả hết phần dư.
        int maxWalls = Math.max(10, cfg.maxUpgrades * 4);
        int buildings = 0;
        int walls = 0;
        float wallFill = cfg.wallPct > 0 ? cfg.wallPct / 100f : -1;
        boolean th17 = cfg.roadmap == Roadmap.TH16_TH17;
        Roadmap.Economy eco = new Roadmap.Economy();
        eco.cap = new long[]{0, cfg.goldCap, cfg.elixirCap, cfg.darkCap};
        // Tốc độ farm đã học → quyết định nên để dành hay cho thợ làm mục kế tiếp (tối ưu thời gian).
        eco.lootPerHour = learner.lootPerHour();
        for (int round = 0; round < cfg.maxUpgrades + maxWalls; round++) {
            if (!ensureHome()) return;
            Vision.Frame f = frame();
            readOwn(f);
            eco.own = own.clone();
            int free = freeBuilders(f);
            status("Nâng cấp: đọc danh sách… " + ownText());
            if (free == 0 && (wallFill <= 0 || walls >= maxWalls)) {
                status("Nâng cấp: không còn thợ rảnh");
                return;
            }
            List<Roadmap.Row> rows = scanList();
            if (rows.isEmpty()) {
                status("Nâng cấp: không đọc được danh sách");
                return;
            }
            Roadmap.Row pick;
            if (free == 0) {
                // Hết thợ: chỉ còn xả tài nguyên dư vào tường.
                pick = th17 ? PlanTH17.chooseWall(rows, cfg, fills(), memory, dev.now(), wallFill, eco)
                        : Roadmap.chooseWall(rows, cfg.roadmap, fills(), wallFill, eco);
                if (pick == null) {
                    status("Nâng cấp: không còn thợ rảnh, không có tài nguyên dư");
                    return;
                }
            } else {
                pick = th17 ? PlanTH17.choose(rows, cfg, fills(), memory, dev.now(), 0.95f, wallFill, eco)
                        : Roadmap.choose(rows, cfg.roadmap, fills(), 0.95f, wallFill, eco);
            }
            if (pick == null) {
                status("Nâng cấp: chưa có mục hợp lý (đang để dành)");
                return;
            }
            boolean isWall = pick.cat == Roadmap.Cat.WALL;
            if (isWall && walls >= maxWalls) return;
            if (!isWall && buildings >= cfg.maxUpgrades) return;
            status("Nâng cấp: " + pick);
            if (!execute(pick, eco)) return;
            if (th17) PlanTH17.recordUpgrade(memory, pick, dev.now());
            stats.upgradeLog.add(pick.name + " (" + pick.cat.vi + ")");
            if (isWall) {
                walls++;
                stats.walls++;
            } else {
                buildings++;
                stats.upgrades++;
            }
        }
    }

    // ================================================================ vòng chính

    // Đo tài nguyên thật sự vào kho sau trận (phần tự học).
    private long[] ownBefore;
    private boolean pendingGain;
    private long battleEndAt = -1;

    /** Sau trận: đọc lại kho và báo cho phần tự học phần đã vào kho. */
    private void measureGain() throws InterruptedException {
        if (battleEndAt >= 0) {
            learner.addFarmTime(Math.min(60_000, dev.now() - battleEndAt)); // thời gian quay về làng
            battleEndAt = -1;
        }
        if (!pendingGain) return;
        pendingGain = false;
        long[] before = ownBefore;
        ownBefore = null;
        if (before == null || store.region(Keys.OWN_GOLD) == null && store.region(Keys.OWN_ELIXIR) == null) return;
        Vision.Frame f = frame();
        if (f == null) return;
        long[] gain = {-1, -1, -1, -1};
        String[] keys = {null, Keys.OWN_GOLD, Keys.OWN_ELIXIR, Keys.OWN_DARK};
        for (int r = 1; r <= 3; r++) {
            if (before[r] < 0 || store.region(keys[r]) == null) continue;
            long now = readNumber(f.img, keys[r]);
            if (now >= 0) gain[r] = now - before[r];
        }
        learner.gained(gain[1], gain[2], gain[3]);
    }

    /** Thỉnh thoảng báo những gì đã học. */
    private void learnStatus(String tag, int cycle) throws InterruptedException {
        if (!cfg.autoTune || cycle % 10 != 0) return;
        StringBuilder sb = new StringBuilder(tag).append("tự học: ");
        sb.append(learner.ready() ? "ngưỡng " + learner.threshold(cfg.minLoot, cfg.maxSkips) : "đang thu thập dữ liệu");
        sb.append(", trễ ").append(step()).append(" ms");
        List<String> bad = learner.reteach(cfg.matchPct);
        if (!bad.isEmpty()) sb.append(" • nên dạy lại ").append(String.join(", ", bad));
        status(sb.toString());
        if (!bad.isEmpty()) sleep(1000);
    }

    /** Chạy vòng farm cho tới khi đủ số vòng hoặc bị dừng (InterruptedException). */
    public void run() throws InterruptedException {
        try {
            farm();
        } finally {
            learner.save();
        }
    }

    private void farm() throws InterruptedException {
        learner.note(cfg.minLoot, cfg.stepDelay, cfg.maxSkips, cfg.matchPct);
        int lost = 0;
        int cycle = 0;
        while (cfg.cycles == 0 || cycle < cfg.cycles) {
            cycle++;
            String tag = "Vòng " + cycle + (cfg.cycles > 0 ? "/" + cfg.cycles : "") + ": ";

            if (!ensureHome()) {
                lost++;
                pendingGain = false;
                battleEndAt = -1;
                status(tag + "không tìm thấy làng chính (" + lost + "/3)");
                if (lost >= 3) {
                    status("Dừng: 3 lần không về được làng. Kiểm tra game / dạy thêm thông báo");
                    return;
                }
                continue;
            }
            lost = 0;
            measureGain();
            learner.save();
            learnStatus(tag, cycle);
            long cycleStart = dev.now();

            status(tag + "thu tài nguyên");
            collect();
            if (visual(Keys.ATTACK)) {
                Vision.Frame f = frame();
                readOwn(f);
            }

            long upgradeMs = 0;
            boolean nearFull = cfg.wallPct > 0
                    && (fill(1) >= cfg.wallPct / 100f || fill(2) >= cfg.wallPct / 100f);
            if (cfg.autoUpgrade && ((cycle - 1) % cfg.upgradeEvery == 0 || nearFull)) {
                long u0 = dev.now();
                upgradeSession();
                boolean home = ensureHome();
                upgradeMs = dev.now() - u0;
                if (!home) continue;
            }

            if (storagesFull()) {
                stats.fullWaits++;
                for (int s = cfg.fullWaitMin * 60; s > 0; s -= 5) {
                    status(tag + "kho đầy, chờ thợ xây rảnh … " + s / 60 + " phút");
                    sleep(5000);
                }
                stats.cycles++;
                continue;
            }

            status(tag + "luyện quân");
            train();

            status(tag + "tìm trận " + ownText());
            ownBefore = own.clone();
            if (!tapTarget(Keys.ATTACK, 5000, step())) continue;
            if (!tapTarget(Keys.FIND_MATCH, 8000, 0)) continue;
            if (!chooseBase(tag)) continue;

            deploy(tag);
            long b0 = dev.now();
            battle(tag);
            long now = dev.now();
            learner.attacked(pickValue, pickGold, pickElixir, pickLooks, now - cycleStart - upgradeMs, now - b0);
            pendingGain = true;
            battleEndAt = now;
            stats.attacks++;
            stats.cycles++;
        }
        status("Xong " + cycle + " vòng, nâng cấp " + stats.upgrades + ", tường " + stats.walls);
    }

    /**
     * Kênh Lộ trình: không đi đánh, chỉ giữ thợ xây luôn bận theo lộ trình.
     * Mỗi lượt: về làng → thu tài nguyên → nâng cấp → nghỉ channelIntervalMin phút.
     */
    public void runUpgradeChannel() throws InterruptedException {
        int round = 0;
        while (cfg.cycles == 0 || round < cfg.cycles) {
            round++;
            String tag = "Lộ trình lượt " + round + ": ";
            if (!ensureHome()) {
                status(tag + "không tìm thấy làng chính, thử lại sau");
            } else {
                collect();
                upgradeSession();
                ensureHome();
            }
            stats.cycles++;
            for (int s = cfg.channelIntervalMin * 60; s > 0; s -= 10) {
                status(tag + "xong, lượt sau sau " + (s + 59) / 60 + " phút");
                sleep(10_000);
            }
        }
        status("Kênh Lộ trình: xong " + round + " lượt, nâng cấp " + stats.upgrades + ", tường " + stats.walls);
    }

    // ================================================================ chẩn đoán (nút 🔍)

    public interface Labeler {
        String label(String key);
    }

    public String diagnose(Iterable<String> keys, Labeler names) throws InterruptedException {
        Vision.Frame f = frame();
        if (f == null) return "Không chụp được màn hình (cần Android 11+ và bật lại Trợ năng).";
        StringBuilder ok = new StringBuilder();
        StringBuilder miss = new StringBuilder();
        for (String k : keys) {
            if (tpl(k) == null) continue;
            if (k.startsWith("collect_")) {
                List<Vision.Match> l = Vision.findAll(f, tpl(k), 0, 0, f.width(), f.height(), thr(), 15);
                if (!l.isEmpty()) {
                    ok.append("✓ ").append(names.label(k)).append(" ×").append(l.size()).append('\n');
                    continue;
                }
                miss.append("✗ ").append(names.label(k)).append('\n');
                continue;
            }
            Vision.Match m = find(f, k);
            if (m != null) {
                ok.append("✓ ").append(names.label(k)).append(" (").append(Math.round(m.score * 100)).append("%)\n");
            } else {
                miss.append("✗ ").append(names.label(k)).append('\n');
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Màn hình ").append(f.width()).append("×").append(f.height())
                .append(", ngưỡng ").append(cfg.matchPct).append("%\n\nTHẤY:\n")
                .append(ok.length() > 0 ? ok : "(không có)\n")
                .append("\nKHÔNG THẤY:\n").append(miss.length() > 0 ? miss : "(không có)\n");

        readOwn(f);
        String o = ownText();
        if (!o.isEmpty()) sb.append("\nKHO CỦA BẠN: ").append(o).append('\n');
        int free = freeBuilders(f);
        if (free >= 0) sb.append("Thợ rảnh: ").append(free).append('\n');
        long g = readNumber(f.img, Keys.LOOT_GOLD);
        long e = readNumber(f.img, Keys.LOOT_ELIXIR);
        if (store.region(Keys.LOOT_GOLD) != null || store.region(Keys.LOOT_ELIXIR) != null) {
            sb.append("Làng địch (khi đang xem): vàng ").append(g < 0 ? "?" : g)
                    .append(", tiên dược ").append(e < 0 ? "?" : e).append('\n');
        }
        if (store.region(Keys.UPGRADE_ROW1) != null) {
            sb.append("\nDANH SÁCH THỢ XÂY (nếu đang mở):\n");
            int n = rowStep() > 0 ? cfg.listRows : 1;
            for (int i = 0; i < n; i++) {
                Box b = rowBox(i);
                if (!b.inside(f.width(), f.height())) break;
                String name = ocr.read(f.img, b.leftPart(0.62f), false);
                if (name == null) continue;
                Box cost = b.rightPart(0.38f);
                sb.append("• ").append(name.trim()).append(" → ").append(Roadmap.classify(name).vi)
                        .append(Vision.redRatio(f.img, cost) < 0.03f ? "" : " (thiếu tiền)").append('\n');
            }
        }
        sb.append("\nLộ trình: ").append(Roadmap.describe(cfg.roadmap));
        learner.note(cfg.minLoot, cfg.stepDelay, cfg.maxSkips, cfg.matchPct);
        sb.append("\n\n").append(learner.describe());
        return sb.toString();
    }
}
