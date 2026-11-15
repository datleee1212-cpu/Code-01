package sim;

import com.code02.autofarm.core.Bot;
import com.code02.autofarm.core.Device;
import com.code02.autofarm.core.Img;
import com.code02.autofarm.core.Keys;
import com.code02.autofarm.core.Learner;
import com.code02.autofarm.core.Memory;
import com.code02.autofarm.core.Settings;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Kiểm thử phần tự cải thiện (Learner): toán chọn ngưỡng, lưu / đọc, độ trễ, hình mẫu,
 * và chạy dài trên game giả lập để so lượng cướp mỗi giờ khi tự chỉnh với khi để ngưỡng tồi.
 * Thoát với mã 1 nếu có mục không đạt.
 */
public final class LearnTests {

    private static int failures = 0;
    private static final StringBuilder report = new StringBuilder();

    private static void check(String group, boolean ok, String what) {
        report.append(ok ? "   ✓ " : "   ✗ ").append(what).append('\n');
        if (!ok) {
            failures++;
            System.out.println("FAIL [" + group + "] " + what);
        }
    }

    private static void header(String name) {
        report.append("\n▶ ").append(name).append('\n');
    }

    // ================================================================ toán ngưỡng

    /** Mẫu giống game giả lập: vàng + tiên dược, mỗi loại đều trong 50k–800k. */
    private static long[] syntheticSamples(int n, long seed) {
        Random r = new Random(seed);
        long[] v = new long[n];
        for (int i = 0; i < n; i++) v[i] = 100_000 + r.nextInt(750_000) + r.nextInt(750_000);
        return v;
    }

    /** Tốc độ (giá trị / ms) với ngưỡng t, tính độc lập để đối chiếu. */
    private static double rate(long[] v, double searchMs, double overheadMs, int m, long t) {
        double all = 0;
        double good = 0;
        int k = 0;
        for (long x : v) {
            all += x;
            if (x >= t) {
                good += x;
                k++;
            }
        }
        all /= v.length;
        if (k == 0) return all / (overheadMs + m * searchMs);
        double p = k / (double) v.length;
        double q = 1 - p;
        double looks = 0;
        for (int i = 0; i <= m; i++) looks += Math.pow(q, i);
        double value = (1 - Math.pow(q, m)) * (good / k) + Math.pow(q, m) * all;
        return value / (overheadMs + (looks - 1) * searchMs);
    }

    static void thresholdMath() {
        String n = "1. Toán chọn ngưỡng trên mẫu tổng hợp";
        header(n);
        long[] v = syntheticSamples(200, 42);
        long t = Learner.bestThreshold(v, 5_000, 125_000, 20, 300_000);
        double best = rate(v, 5_000, 125_000, 20, t);
        double gridBest = 0;
        long gridT = 0;
        for (long x = 0; x <= 1_600_000; x += 10_000) {
            int above = 0;
            for (long s : v) if (s >= x) above++;
            if (above < 3) break;
            double r = rate(v, 5_000, 125_000, 20, x);
            if (r > gridBest) {
                gridBest = r;
                gridT = x;
            }
        }
        report.append(String.format("   tìm 5 s/làng, vòng 125 s: ngưỡng %,d (dò lưới: %,d)%n", t, gridT));
        check(n, t >= 800_000 && t <= 1_300_000, "ngưỡng hợp lý 0,8–1,3 triệu (" + t + ")");
        check(n, best >= gridBest * 0.999, "tốt ít nhất bằng dò lưới mọi ngưỡng");
        check(n, best > rate(v, 5_000, 125_000, 20, 0) * 1.1, "hơn hẳn đánh làng đầu tiên (ngưỡng 0)");
        check(n, best > rate(v, 5_000, 125_000, 20, 1_400_000) * 1.1, "hơn hẳn ngưỡng quá cao 1,4 triệu");

        long slow = Learner.bestThreshold(v, 200_000, 125_000, 20, 300_000);
        check(n, slow < 400_000, "tìm trận rất chậm → hạ ngưỡng gần 0 (" + slow + ")");
        long fast = Learner.bestThreshold(v, 200, 125_000, 20, 300_000);
        check(n, fast >= 1_250_000, "tìm trận rất nhanh → ngưỡng cao (" + fast + ")");
        long noSkip = Learner.bestThreshold(v, 5_000, 125_000, 0, 300_000);
        check(n, noSkip == 0, "không được bỏ qua làng nào → ngưỡng 0 (" + noSkip + ")");
        long tight = Learner.bestThreshold(v, 5_000, 125_000, 3, 300_000);
        check(n, tight < t, "ít lượt bỏ qua (3) → ngưỡng thấp hơn (" + tight + " < " + t + ")");
        check(n, Learner.bestThreshold(new long[0], 5_000, 125_000, 20, 777) == 777, "không có mẫu → giữ ngưỡng người dùng");
        long[] few = {2_000_000, 2_000_000, 100_000, 100_000, 100_000, 100_000};
        long tf = Learner.bestThreshold(few, 1, 125_000, 20, 0);
        check(n, tf <= 100_000, "không học thuộc vài làng giàu hiếm gặp (cần ≥ 3 làng ≥ ngưỡng) (" + tf + ")");
    }

    // ================================================================ Learner lẻ

    static void learnerUnits() {
        String n = "2. Ghi nhớ, lưu / đọc, độ trễ, hình mẫu";
        header(n);
        Memory mem = Memory.inRam();
        Learner l = new Learner(mem);
        l.note(1_400_000, 1500, 20, 82);
        long[] v = syntheticSamples(30, 7);
        for (long x : v) l.scouted(x / 2, x - x / 2, x);
        check(n, l.threshold(1_400_000) == 1_400_000, "chưa đủ trận → giữ ngưỡng người dùng");
        for (int i = 0; i < 6; i++) {
            l.searchGap(5_000);
            l.attacked(1_000_000, 500_000, 500_000, 3, 135_000, 100_000);
            l.gained(400_000, 400_000, 1_000);
        }
        long t = l.threshold(1_400_000);
        check(n, l.ready() && t != 1_400_000 && t > 300_000, "đủ dữ liệu → tự chọn ngưỡng (" + t + ")");
        double[] lph = l.lootPerHour();
        double expect = 400_000 * 3_600_000.0 / 135_000;
        check(n, Math.abs(lph[1] - expect) < 1 && Math.abs(lph[2] - expect) < 1 && lph[3] > 0,
                String.format("tốc độ farm từng loại: vàng %,.0f, tiên dược %,.0f, đen %,.0f mỗi giờ", lph[1], lph[2], lph[3]));

        // Hình mẫu sát ngưỡng / hay trượt
        for (int i = 0; i < 10; i++) l.matched(Keys.NEXT, 0.84f);
        for (int i = 0; i < 10; i++) l.matched(Keys.ATTACK, 0.97f);
        for (int i = 0; i < 6; i++) l.waited(Keys.RETURN_HOME, 40, false);
        List<String> bad = l.reteach(82);
        check(n, bad.contains(Keys.NEXT), "độ giống 84% với ngưỡng 82% → nên dạy lại 'next'");
        check(n, bad.contains(Keys.RETURN_HOME), "hay không thấy → nên dạy lại 'return_home'");
        check(n, !bad.contains(Keys.ATTACK), "hình mẫu tốt (97%) không bị báo");

        // Độ trễ: màn hình hiện ngay → giảm; hay trượt → tăng; luôn trong [300, 2×]
        Learner s = new Learner(Memory.inRam());
        s.note(0, 1500, 20, 82);
        for (int i = 0; i < 80; i++) s.waited(Keys.ATTACK, 1, true);
        int fastStep = s.stepDelay(1500);
        check(n, fastStep < 1500 && fastStep >= 300, "màn hình hiện ngay → độ trễ giảm (" + fastStep + " ms)");
        for (int i = 0; i < 200; i++) s.waited(Keys.ATTACK, 1, true);
        check(n, s.stepDelay(1500) == 300, "không thấp hơn 300 ms (" + s.stepDelay(1500) + ")");
        for (int i = 0; i < 120; i++) s.waited(Keys.ATTACK, 30, i % 3 != 0);
        int slowStep = s.stepDelay(1500);
        check(n, slowStep > fastStep && slowStep <= 3000, "hay trượt → độ trễ tăng, tối đa 2× cài đặt (" + slowStep + " ms)");

        // Lưu / đọc
        l.save();
        Learner back = new Learner(mem);
        check(n, back.threshold(1_400_000) == t, "đọc lại từ bộ nhớ → cùng ngưỡng (" + back.threshold(1_400_000) + ")");
        check(n, back.scoutCount() == 30 && back.attackCount() == 6, "đọc lại đủ số làng / số trận");
        check(n, back.reteach(82).contains(Keys.NEXT), "đọc lại cả sức khỏe hình mẫu");
        String sum = Learner.summary(mem);
        check(n, sum.contains("Ngưỡng chọn làng đã học") && sum.contains("Nên dạy lại") && sum.contains("Độ trễ"),
                "tóm tắt có ngưỡng, độ trễ, hình mẫu cần dạy lại");
        double[] st = Learner.lootPerHour(mem);
        check(n, Math.abs(st[1] - lph[1]) < 1, "lootPerHour(Memory) khớp");

        // Giới hạn kích thước
        Memory big = Memory.inRam();
        Learner b = new Learner(big);
        for (int i = 0; i < 1000; i++) b.scouted(1000 + i, 2000 + i, 3000 + i);
        for (int i = 0; i < 500; i++) b.attacked(i, i, i, 1, 1000, 500);
        b.save();
        int size = 0;
        for (String k : new String[]{"learn.scout", "learn.attack"}) size += big.get(k).length();
        check(n, new Learner(big).scoutCount() == 200 && new Learner(big).attackCount() == 100 && size < 12_000,
                "giữ tối đa 200 làng, 100 trận (" + size + " ký tự)");

        // Dữ liệu hỏng không làm sập
        Memory bad2 = Memory.inRam();
        bad2.put("learn.scout", "zz:??,,1");
        bad2.put("learn.tpl", "x=a/b/c");
        boolean ok = true;
        try {
            new Learner(bad2).threshold(5);
            Learner.summary(bad2);
        } catch (RuntimeException e) {
            ok = false;
        }
        check(n, ok, "dữ liệu hỏng → học lại từ đầu, không lỗi");
    }

    // ================================================================ chạy dài trên game giả lập

    /**
     * Đồ đã dạy, bỏ phần thu tài nguyên để "cướp vào kho" chỉ tính tài nguyên đánh trận,
     * và bỏ hình nút đóng thông báo (các lần chạy này không có thông báo) để chạy nhanh hơn.
     */
    private static Sim.SimStore farmStore() {
        Sim.SimStore full = Sim.teach(true);
        Sim.SimStore s = new Sim.SimStore();
        for (Map.Entry<String, int[]> e : full.points.entrySet())
            if (!e.getKey().startsWith("collect_")) s.points.put(e.getKey(), e.getValue());
        for (Map.Entry<String, Img> e : full.templates.entrySet())
            if (!e.getKey().startsWith("collect_") && !e.getKey().startsWith("close"))
                s.templates.put(e.getKey(), e.getValue());
        s.regions.putAll(full.regions);
        return s;
    }

    /**
     * Thiết bị bọc game giả lập để chạy nhanh: khi màn hình đứng yên và không có chữ
     * (đang đánh, đang tìm, đang tải) thì dùng lại ảnh vừa vẽ thay vì vẽ lại cả khung hình.
     * Đồng hồ giả lập vẫn chạy như khi chụp thật (+120 ms mỗi lần chụp).
     */
    static final class FastDevice implements Device {
        final FakeGame g;
        private Img cached;
        private FakeGame.Screen cachedScreen;

        FastDevice(FakeGame g) {
            this.g = g;
        }

        private static boolean still(FakeGame.Screen s) {
            return s == FakeGame.Screen.BATTLE || s == FakeGame.Screen.SEARCHING || s == FakeGame.Screen.LOADING;
        }

        @Override
        public Img capture() {
            if (cached != null && g.screen == cachedScreen && still(g.screen)) {
                g.sleep(120);
                if (g.screen == cachedScreen) return cached;
            }
            Img i = g.capture();
            cached = i;
            cachedScreen = g.screen;
            return i;
        }

        @Override
        public void tap(int x, int y) {
            cached = null;
            g.tap(x, y);
        }

        @Override
        public void swipe(int x1, int y1, int x2, int y2, int ms) {
            cached = null;
            g.swipe(x1, y1, x2, y2, ms);
        }

        @Override
        public void sleep(long ms) {
            g.sleep(ms);
        }

        @Override
        public long now() {
            return g.now();
        }

        @Override
        public void status(String s) {
            g.status(s);
        }
    }

    static final class Run implements Runnable {
        final String name;
        final Settings cfg;
        final Memory memory;
        final Sim.SimStore store;
        final long seed;
        FakeGame game;
        Bot bot;
        Exception error;

        Run(String name, Sim.SimStore store, Settings cfg, Memory memory, long seed) {
            this.name = name;
            this.store = store;
            this.cfg = cfg;
            this.memory = memory;
            this.seed = seed;
        }

        @Override
        public void run() {
            game = new FakeGame(seed);
            game.buildings.addAll(Sim.village(false));
            game.cap = new long[]{0, 2_000_000_000L, 2_000_000_000L, 200_000};
            bot = new Bot(new FastDevice(game), game, store, cfg, memory);
            try {
                bot.run();
            } catch (Exception e) {
                error = e;
            }
        }

        double lootPerHour() {
            return game.now() <= 0 ? 0 : game.lootGained * 3_600_000.0 / game.now();
        }

        String line() {
            return String.format("   %-34s %3d trận, %3d làng bỏ qua, %5.0f phút giả lập → %,12.0f mỗi giờ (ngưỡng cuối %,d)%n",
                    name, bot.stats.attacks, bot.stats.skippedBases, game.now() / 60000.0, lootPerHour(), bot.stats.threshold);
        }
    }

    private static Settings farmSettings(int cycles, int minLoot, boolean autoTune) {
        Settings s = Sim.settings();
        s.cycles = cycles;
        s.minLoot = minLoot;
        s.autoTune = autoTune;
        s.autoUpgrade = false;
        s.wallPct = 0;
        s.goldCap = 2_000_000_000;
        s.elixirCap = 2_000_000_000;
        return s;
    }

    static void longRuns(int cycles) throws InterruptedException {
        String n = "3. Chạy dài " + cycles + " vòng: tự chỉnh so với ngưỡng tồi";
        header(n);
        Sim.SimStore store = farmStore();
        Memory tunedHigh = Memory.inRam();
        Memory tunedLow = Memory.inRam();
        Run[] runs = {
                new Run("cố định 1.400.000 (quá cao)", store, farmSettings(cycles, 1_400_000, false), Memory.inRam(), 11),
                new Run("tự chỉnh, bắt đầu từ 1.400.000", store, farmSettings(cycles, 1_400_000, true), tunedHigh, 11),
                new Run("cố định 0 (đánh làng đầu tiên)", store, farmSettings(cycles, 0, false), Memory.inRam(), 11),
                new Run("tự chỉnh, bắt đầu từ 0", store, farmSettings(cycles, 0, true), tunedLow, 11),
        };
        Thread[] th = new Thread[runs.length];
        for (int i = 0; i < runs.length; i++) {
            th[i] = new Thread(runs[i], "run-" + i);
            th[i].start();
        }
        for (Thread t : th) t.join();
        for (Run r : runs) {
            report.append(r.line());
            if (r.error != null) report.append("   LỖI ").append(r.name).append(": ").append(r.error).append('\n');
        }
        for (Run r : runs) check(n, r.error == null && r.bot.stats.attacks >= cycles * 9 / 10,
                r.name + ": không lỗi, đánh ≥ 90% số vòng (" + r.bot.stats.attacks + ")");

        Run fixedHigh = runs[0];
        Run tuneHigh = runs[1];
        Run fixedZero = runs[2];
        Run tuneZero = runs[3];
        double gainHigh = tuneHigh.lootPerHour() / Math.max(1, fixedHigh.lootPerHour());
        double gainZero = tuneZero.lootPerHour() / Math.max(1, fixedZero.lootPerHour());
        report.append(String.format("   tự chỉnh / cố định 1,4 triệu: %+.0f%% | tự chỉnh / cố định 0: %+.0f%%%n",
                (gainHigh - 1) * 100, (gainZero - 1) * 100));
        check(n, gainHigh >= 1.15, "tự chỉnh hơn hẳn ngưỡng quá cao (≥ +15%)");
        check(n, gainZero >= 1.05, "tự chỉnh hơn ngưỡng 0 (≥ +5%)");
        long tH = tuneHigh.bot.learner().threshold(1_400_000);
        long tZ = tuneZero.bot.learner().threshold(0);
        check(n, tH > 500_000 && tH < 1_400_000, "ngưỡng học được (từ 1,4 triệu) nằm giữa: " + tH);
        check(n, tZ > 500_000 && tZ < 1_400_000, "ngưỡng học được (từ 0) nằm giữa: " + tZ);
        check(n, Math.abs(tH - tZ) < 450_000, "hai lần học từ hai đầu ra ngưỡng gần nhau");
        check(n, fixedHigh.bot.stats.tunedSearches == 0 && fixedHigh.bot.stats.threshold == 1_400_000,
                "tắt tự chỉnh → luôn dùng đúng ngưỡng người dùng");

        // Tốc độ farm từng loại (cho lộ trình nâng cấp)
        double[] lph = tuneHigh.bot.learner().lootPerHour();
        double est = lph[1] + lph[2];
        double real = tuneHigh.lootPerHour();
        report.append(String.format("   lootPerHour(): vàng %,.0f + tiên dược %,.0f (+ đen %,.0f) / giờ; thực tế game %,.0f%n",
                lph[1], lph[2], lph[3], real));
        check(n, lph[1] > 0 && lph[2] > 0, "lootPerHour() dương cho vàng và tiên dược");
        check(n, est > real * 0.7 && est < real * 1.3, "lootPerHour() sát thực tế (±30%)");

        // Lưu lâu dài: bot mới cùng bộ nhớ bắt đầu với ngưỡng đã học
        String p = "4. Bot mới cùng bộ nhớ dùng ngay ngưỡng đã học";
        header(p);
        Run again = new Run("bot mới (tự chỉnh, cài 1.400.000)", store, farmSettings(2, 1_400_000, true), tunedHigh, 99);
        again.run();
        report.append(again.line());
        Bot fresh = new Bot(new FakeGame(1), new FakeGame(1), store, farmSettings(1, 1_400_000, true), tunedHigh);
        check(p, fresh.learner().ready(), "đọc lại đủ dữ liệu từ bộ nhớ");
        check(p, again.error == null && again.bot.stats.attacks == 2, "chạy tiếp được 2 trận");
        check(p, again.bot.stats.tunedSearches == 2, "ngay lần tìm trận đầu đã dùng ngưỡng tự học (" + again.bot.stats.tunedSearches + "/2)");
        long t0 = again.bot.stats.threshold;
        check(p, Math.abs(t0 - tH) < 300_000 && t0 != 1_400_000, "ngưỡng ≈ đã học trước đó (" + t0 + " ~ " + tH + ")");
        String sum = Learner.summary(tunedHigh);
        report.append("   --- tóm tắt (Learner.summary) ---\n");
        for (String line : sum.split("\n")) report.append("   | ").append(line).append('\n');
        check(p, sum.contains("Ngưỡng chọn làng đã học"), "tóm tắt có ngưỡng đã học");
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        int cycles = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        thresholdMath();
        learnerUnits();
        longRuns(cycles);
        System.out.println("\n=============== KIỂM THỬ TỰ CẢI THIỆN ===============");
        System.out.print(report);
        System.out.printf("%nThời gian chạy thật: %.1f giây%n", (System.currentTimeMillis() - t0) / 1000.0);
        System.out.println(failures == 0 ? "KẾT QUẢ: TẤT CẢ ĐẠT ✓" : "KẾT QUẢ: " + failures + " MỤC KHÔNG ĐẠT ✗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
