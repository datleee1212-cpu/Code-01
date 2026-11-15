package com.code02.autofarm.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tự cải thiện: ghi nhớ kết quả và tự chỉnh thông số.
 *
 * <p>Ghi lại (lưu qua {@link Memory}, mã hóa gọn, giới hạn kích thước):
 * <ul>
 *   <li>mỗi làng đã xem (vàng, tiên dược, phần kho còn chứa được);</li>
 *   <li>mỗi trận đánh (giá trị đã chọn, số lần xem làng, tài nguyên thật sự vào kho nếu đọc được, thời gian);</li>
 *   <li>thời gian giữa hai lần xem làng (bấm Tiếp theo → đọc làng mới);</li>
 *   <li>thời gian chờ màn hình (waitFor) và độ giống của từng hình mẫu.</li>
 * </ul>
 *
 * <p>Từ đó tính:
 * <ul>
 *   <li>ngưỡng chọn làng tối ưu (cướp được nhiều nhất mỗi phút, kiểu "dừng tối ưu");</li>
 *   <li>độ trễ sau mỗi lần chạm (rút ngắn khi màn hình hiện nhanh, kéo dài khi hay trượt);</li>
 *   <li>hình mẫu nên dạy lại;</li>
 *   <li>tốc độ farm từng loại tài nguyên mỗi giờ (cho lộ trình nâng cấp).</li>
 * </ul>
 */
public final class Learner {

    // ---------------------------------------------------------------- giới hạn & ngưỡng học
    /** Số làng đã xem giữ lại tối đa. */
    static final int MAX_SCOUTS = 200;
    /** Số trận giữ lại tối đa. */
    static final int MAX_ATTACKS = 100;
    static final int MAX_GAPS = 100;
    static final int MAX_WAITS = 60;
    static final int TPL_SCORES = 20;
    /** Cần ít nhất chừng này làng đã xem trước khi tự đổi ngưỡng. */
    public static final int MIN_SCOUTS = 15;
    /** …và chừng này trận có đo thời gian. */
    public static final int MIN_ATTACKS = 5;
    /** Số lần chờ màn hình tối thiểu trước khi tự chỉnh độ trễ. */
    static final int MIN_WAITS = 20;
    /** Độ trễ sau chạm thấp nhất (ms). */
    public static final int STEP_MIN = 300;
    /** Tỉ lệ cướp được / giá trị làng khi chưa đo được (trận farm thường ~75%). */
    static final double DEFAULT_GAIN_RATIO = 0.75;

    // ---------------------------------------------------------------- khóa trong Memory
    static final String K_SCOUT = "learn.scout";
    static final String K_ATTACK = "learn.attack";
    static final String K_GAP = "learn.gap";
    static final String K_WAIT = "learn.wait";
    static final String K_TPL = "learn.tpl";
    static final String K_STEP = "learn.step";
    static final String K_COUNT = "learn.count";
    static final String K_INFO = "learn.info";

    /** Một trận đã đánh. -1 = không biết. */
    static final class Attack {
        long value;      // giá trị dùng được lúc chọn
        long scoutG;     // phần vàng dùng được lúc chọn
        long scoutE;     // phần tiên dược dùng được lúc chọn
        int looks;       // số làng đã xem trong lượt tìm này
        long farmMs;     // thời gian cả vòng (không tính lượt nâng cấp)
        long battleMs;   // thời gian từ lúc thả quân tới lúc bấm về làng
        long gainG = -1; // vào kho thật (đo bằng kho trước / sau)
        long gainE = -1;
        long gainD = -1;

        String enc() {
            return b36(value) + ":" + b36(scoutG) + ":" + b36(scoutE) + ":" + b36(looks) + ":" + b36(farmMs)
                    + ":" + b36(gainG) + ":" + b36(gainE) + ":" + b36(gainD) + ":" + b36(battleMs);
        }

        static Attack dec(String s) {
            String[] p = s.split(":");
            if (p.length < 8) return null;
            Attack a = new Attack();
            a.value = l36(p[0]);
            a.scoutG = l36(p[1]);
            a.scoutE = l36(p[2]);
            a.looks = (int) l36(p[3]);
            a.farmMs = l36(p[4]);
            a.gainG = l36(p[5]);
            a.gainE = l36(p[6]);
            a.gainD = l36(p[7]);
            a.battleMs = p.length > 8 ? l36(p[8]) : 0;
            return a;
        }

        boolean measured() {
            return gainG >= 0 || gainE >= 0;
        }
    }

    /** Sức khỏe một hình mẫu. */
    static final class TplHealth {
        final List<Integer> scores = new ArrayList<>(); // độ giống ×100 của các lần thấy gần đây
        int hits;   // waitFor thấy
        int misses; // waitFor hết giờ

        double avg() {
            if (scores.isEmpty()) return -1;
            double s = 0;
            for (int v : scores) s += v;
            return s / scores.size();
        }
    }

    private final Memory memory;
    private final List<long[]> scouts = new ArrayList<>(); // {vàng, tiên dược, dùng được}
    private final List<Attack> attacks = new ArrayList<>();
    private final List<Long> gaps = new ArrayList<>();      // ms giữa hai lần xem làng
    private final List<int[]> waits = new ArrayList<>();    // {số khung hình, 1 = thấy / 0 = hết giờ}
    private final Map<String, TplHealth> tpl = new LinkedHashMap<>();
    /** Tổng cộng từ trước tới giờ: {làng đã xem, trận, làng bỏ qua}. */
    private final long[] count = new long[3];
    /** Độ trễ sau chạm đã học (ms), 0 = chưa học. */
    private int step;
    private int waitsSinceTune;

    public Learner(Memory memory) {
        this.memory = memory == null ? Memory.inRam() : memory;
        load();
    }

    // ================================================================ ghi nhận

    /** Một làng vừa xem: vàng, tiên dược (-1 nếu không đọc được) và phần kho còn chứa được. */
    public void scouted(long gold, long elixir, long storable) {
        scouts.add(new long[]{gold, elixir, Math.max(0, storable)});
        trim(scouts, MAX_SCOUTS);
        count[0]++;
    }

    /** Bỏ qua một làng (bấm Tiếp theo). */
    public void skipped() {
        count[2]++;
    }

    /** Thời gian từ lúc bấm Tiếp theo tới lúc đọc được làng mới. */
    public void searchGap(long ms) {
        if (ms <= 0 || ms > 120_000) return;
        gaps.add(ms);
        trim(gaps, MAX_GAPS);
    }

    /**
     * Một trận vừa xong.
     *
     * @param value  giá trị dùng được của làng đã chọn (-1 nếu không đọc được)
     * @param scoutG phần vàng dùng được (-1 nếu không biết)
     * @param scoutE phần tiên dược dùng được (-1 nếu không biết)
     * @param looks  số làng đã xem trong lượt tìm (≥ 1)
     * @param farmMs thời gian cả vòng farm (không tính nâng cấp)
     * @param battleMs thời gian trận (thả quân → về làng)
     */
    public void attacked(long value, long scoutG, long scoutE, int looks, long farmMs, long battleMs) {
        Attack a = new Attack();
        a.value = value;
        a.scoutG = scoutG;
        a.scoutE = scoutE;
        a.looks = Math.max(1, looks);
        a.farmMs = Math.max(0, farmMs);
        a.battleMs = Math.max(0, battleMs);
        attacks.add(a);
        trim(attacks, MAX_ATTACKS);
        count[1]++;
    }

    /** Tài nguyên thật sự vào kho sau trận vừa rồi (đo bằng kho trước / sau; -1 = không đọc được). */
    public void gained(long gold, long elixir, long dark) {
        if (attacks.isEmpty()) return;
        Attack a = attacks.get(attacks.size() - 1);
        a.gainG = gold < 0 ? -1 : gold;
        a.gainE = elixir < 0 ? -1 : elixir;
        a.gainD = dark < 0 ? -1 : dark;
    }

    /** Thêm thời gian vào trận gần nhất (ví dụ lúc quay về làng sau trận). */
    public void addFarmTime(long ms) {
        if (attacks.isEmpty() || ms <= 0) return;
        attacks.get(attacks.size() - 1).farmMs += ms;
    }

    /** Một lần chờ màn hình: số khung hình đã chụp và có thấy hay không. */
    public void waited(String key, int frames, boolean found) {
        waits.add(new int[]{Math.max(1, frames), found ? 1 : 0});
        trim(waits, MAX_WAITS);
        TplHealth h = health(key);
        if (found) h.hits++;
        else h.misses++;
        if (h.hits + h.misses > 40) {
            h.hits /= 2;
            h.misses /= 2;
        }
        if (++waitsSinceTune >= 10) {
            waitsSinceTune = 0;
            tuneStep();
        }
    }

    /** Độ giống của một lần tìm thấy hình mẫu (0..1). */
    public void matched(String key, float score) {
        TplHealth h = health(key);
        h.scores.add(Math.round(score * 100));
        trim(h.scores, TPL_SCORES);
    }

    private TplHealth health(String key) {
        TplHealth h = tpl.get(key);
        if (h == null) {
            h = new TplHealth();
            tpl.put(key, h);
        }
        return h;
    }

    private static <T> void trim(List<T> l, int max) {
        while (l.size() > max) l.remove(0);
    }

    // ================================================================ ngưỡng chọn làng

    /** Đã đủ dữ liệu để tự chọn ngưỡng chưa. */
    public boolean ready() {
        return scouts.size() >= MIN_SCOUTS && timedAttacks() >= MIN_ATTACKS;
    }

    private int timedAttacks() {
        int n = 0;
        for (Attack a : attacks) if (a.farmMs > 0) n++;
        return n;
    }

    /** Thời gian trung bình giữa hai lần xem làng (ms), dùng trung vị cho bền với số liệu lạ. */
    double searchMs() {
        if (gaps.isEmpty()) return 6000;
        long[] a = new long[gaps.size()];
        for (int i = 0; i < a.length; i++) a[i] = gaps.get(i);
        Arrays.sort(a);
        return a[a.length / 2];
    }

    /** Thời gian một vòng nếu chỉ xem 1 làng (thu, luyện quân, đánh, về làng…), ms. */
    double overheadMs() {
        double s = searchMs();
        double sum = 0;
        int n = 0;
        for (Attack a : attacks) {
            if (a.farmMs <= 0) continue;
            sum += Math.max(a.farmMs * 0.3, a.farmMs - (a.looks - 1) * s);
            n++;
        }
        return n == 0 ? 120_000 : sum / n;
    }

    private long[] values() {
        long[] v = new long[scouts.size()];
        for (int i = 0; i < v.length; i++) v[i] = scouts.get(i)[2];
        return v;
    }

    /**
     * Ngưỡng tối thiểu (phần kho còn chứa được) để đánh một làng.
     * Chưa đủ dữ liệu → giữ ngưỡng của người dùng.
     */
    public long threshold(long userMinLoot, int maxSkips) {
        if (!ready()) return userMinLoot;
        return bestThreshold(values(), searchMs(), overheadMs(), maxSkips, userMinLoot);
    }

    /** Như trên, dùng số làng được bỏ qua đã ghi nhận lần chạy gần nhất (mặc định 20). */
    public long threshold(long userMinLoot) {
        return threshold(userMinLoot, (int) userInfo[2]);
    }

    /**
     * Chọn ngưỡng T làm lớn nhất lượng cướp mỗi phút:
     * <pre>
     *   p = P(giá trị ≥ T), q = 1 − p, M = số làng được bỏ qua tối đa
     *   số làng xem   L(T) = 1 + q + … + q^M
     *   giá trị trận  V(T) = (1 − q^M)·E[giá trị | ≥ T] + q^M·E[giá trị]   (hết lượt bỏ qua thì đánh luôn)
     *   tốc độ        V(T) / (thời gian vòng + (L(T) − 1)·thời gian mỗi lần Tiếp theo)
     * </pre>
     * Ước lượng từ phân bố giá trị đã gặp. T chỉ lấy trong các giá trị có ít nhất 3 làng ≥ T
     * (tránh "học thuộc" vài làng giàu hiếm gặp).
     */
    public static long bestThreshold(long[] samples, double searchMs, double overheadMs, int maxSkips, long fallback) {
        int n = samples == null ? 0 : samples.length;
        if (n == 0) return fallback;
        long[] v = samples.clone();
        Arrays.sort(v);
        double total = 0;
        for (long x : v) total += x;
        double meanAll = total / n;
        int m = Math.max(0, maxSkips);
        double s = Math.max(1, searchMs);
        double b = Math.max(1, overheadMs);

        long best = 0;
        double bestRate = meanAll / b; // T = 0: đánh làng đầu tiên
        double suffix = total;          // tổng các giá trị từ chỉ số i trở đi
        for (int i = 0; i < n; i++) {
            if (i > 0) suffix -= v[i - 1];
            if (i > 0 && v[i] == v[i - 1]) continue;
            int above = n - i;
            if (above < 3) break;
            double p = above / (double) n;
            double q = 1 - p;
            double qm = Math.pow(q, m);
            double looks = q >= 1 ? m + 1 : (1 - qm * q) / p;
            double value = (1 - qm) * (suffix / above) + qm * meanAll;
            double rate = value / (b + (looks - 1) * s);
            if (rate > bestRate * 1.0001) {
                bestRate = rate;
                best = v[i];
            }
        }
        return best;
    }

    /** Lượng cướp mỗi phút (theo giá trị làng) ước tính khi dùng ngưỡng T. */
    static double ratePerMs(long[] samples, double searchMs, double overheadMs, int maxSkips, long t) {
        int n = samples.length;
        if (n == 0) return 0;
        double total = 0;
        double above = 0;
        int k = 0;
        for (long x : samples) {
            total += x;
            if (x >= t) {
                above += x;
                k++;
            }
        }
        double meanAll = total / n;
        if (k == 0) return meanAll / (overheadMs + maxSkips * searchMs);
        double p = k / (double) n;
        double q = 1 - p;
        double qm = Math.pow(q, maxSkips);
        double looks = q <= 0 ? 1 : (1 - qm * q) / p;
        double value = (1 - qm) * (above / k) + qm * meanAll;
        return value / (overheadMs + (looks - 1) * searchMs);
    }

    // ================================================================ độ trễ sau chạm

    /**
     * Độ trễ sau mỗi lần chạm (ms) nên dùng, trong khoảng [300, 2 × cài đặt].
     * Chưa học → đúng cài đặt của người dùng.
     */
    public int stepDelay(int userStep) {
        int lo = Math.min(STEP_MIN, userStep);
        int hi = Math.max(lo, userStep * 2);
        int s = step <= 0 ? userStep : step;
        return Math.max(lo, Math.min(hi, s));
    }

    /** Hệ số so với cài đặt (dùng để co giãn các lần chờ cố định khác). */
    public float stepFactor(int userStep) {
        if (userStep <= 0) return 1f;
        return stepDelay(userStep) / (float) userStep;
    }

    private int userStepHint = 1500;

    /** Bot báo cài đặt độ trễ hiện tại để việc tự chỉnh có gốc. */
    public void baseStep(int userStep) {
        userStepHint = userStep;
    }

    /** Xem lại các lần chờ gần đây và chỉnh độ trễ. */
    private void tuneStep() {
        if (waits.size() < MIN_WAITS) return;
        int n = Math.min(30, waits.size());
        int fast = 0;
        int miss = 0;
        for (int i = waits.size() - n; i < waits.size(); i++) {
            int[] w = waits.get(i);
            if (w[1] == 0) miss++;
            else if (w[0] <= 1) fast++;
        }
        int cur = stepDelay(userStepHint);
        double next = cur;
        if (miss * 100 >= n * 15) next = cur * 1.25;      // hay trượt → chờ lâu hơn
        else if (fast * 100 >= n * 80) next = cur * 0.85; // màn hình hầu như hiện ngay → chờ ít đi
        else if (fast * 100 < n * 40) next = cur * 1.10;  // thường phải chờ thêm
        int lo = Math.min(STEP_MIN, userStepHint);
        int hi = Math.max(lo, userStepHint * 2);
        step = (int) Math.max(lo, Math.min(hi, Math.round(next)));
    }

    // ================================================================ hình mẫu

    /** Các hình mẫu nên dạy lại (độ giống sát ngưỡng hoặc hay không thấy). */
    public List<String> reteach(int matchPct) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, TplHealth> e : tpl.entrySet()) {
            TplHealth h = e.getValue();
            boolean weak = h.scores.size() >= 5 && h.avg() - matchPct < 4;
            boolean missy = h.misses >= 3 && h.misses * 10 > (h.hits + h.misses) * 3;
            if (weak || missy) out.add(e.getKey());
        }
        return out;
    }

    // ================================================================ tốc độ farm

    /** Tỉ lệ (vào kho thật) / (giá trị lúc chọn), đo từ các trận đọc được kho. */
    double gainRatio() {
        double got = 0;
        double want = 0;
        for (Attack a : attacks) {
            if (!a.measured() || a.value <= 0) continue;
            got += Math.max(0, a.gainG) + Math.max(0, a.gainE);
            want += a.value;
        }
        return want <= 0 ? -1 : got / want;
    }

    /**
     * Tài nguyên vào kho mỗi giờ farm: {0, vàng, tiên dược, đen}.
     * Ưu tiên số đo thật (kho trước / sau); nếu không đọc được kho thì ước tính từ giá trị làng đã chọn.
     * Toàn 0 khi chưa đủ dữ liệu.
     */
    public double[] lootPerHour() {
        double[] out = new double[4];
        int measured = 0;
        for (Attack a : attacks) if (a.measured() && a.farmMs > 0) measured++;
        if (measured >= 3) {
            double ms = 0;
            double[] sum = new double[4];
            for (Attack a : attacks) {
                if (!a.measured() || a.farmMs <= 0) continue;
                ms += a.farmMs;
                sum[1] += Math.max(0, a.gainG);
                sum[2] += Math.max(0, a.gainE);
                sum[3] += Math.max(0, a.gainD);
            }
            if (ms > 0) for (int r = 1; r <= 3; r++) out[r] = sum[r] * 3_600_000.0 / ms;
            return out;
        }
        int known = 0;
        double ms = 0;
        double g = 0;
        double e = 0;
        for (Attack a : attacks) {
            if (a.farmMs <= 0 || a.scoutG < 0 || a.scoutE < 0) continue;
            known++;
            ms += a.farmMs;
            g += a.scoutG;
            e += a.scoutE;
        }
        if (known < 3 || ms <= 0) return out;
        double ratio = gainRatio();
        if (ratio < 0) ratio = DEFAULT_GAIN_RATIO;
        out[1] = g * ratio * 3_600_000.0 / ms;
        out[2] = e * ratio * 3_600_000.0 / ms;
        return out;
    }

    /** Như {@link #lootPerHour()} nhưng đọc thẳng từ bộ nhớ. */
    public static double[] lootPerHour(Memory m) {
        return new Learner(m).lootPerHour();
    }

    /** Ước tính tổng (vàng + tiên dược) mỗi giờ khi dùng ngưỡng đã học. */
    double estimatedLootPerHour(long t, int maxSkips) {
        if (scouts.isEmpty() || timedAttacks() == 0) return 0;
        double ratio = gainRatio();
        if (ratio < 0) ratio = DEFAULT_GAIN_RATIO;
        return ratePerMs(values(), searchMs(), overheadMs(), maxSkips, t) * ratio * 3_600_000.0;
    }

    /** Số làng xem trung bình mỗi trận. */
    double avgLooks() {
        if (attacks.isEmpty()) return 0;
        double s = 0;
        for (Attack a : attacks) s += a.looks;
        return s / attacks.size();
    }

    public int scoutCount() {
        return scouts.size();
    }

    public int attackCount() {
        return attacks.size();
    }

    // ================================================================ tóm tắt

    /** Ghi lại cài đặt người dùng để phần tóm tắt so sánh (không cần Settings). */
    public void note(long userMinLoot, int userStep, int maxSkips, int matchPct) {
        userInfo = new long[]{userMinLoot, userStep, maxSkips, matchPct};
        userStepHint = userStep;
    }

    private long[] userInfo = {-1, 1500, 20, 82};

    /** Tóm tắt những gì bot đã học (văn bản nhiều dòng, tiếng Việt) để hiện trong app. */
    public static String summary(Memory m) {
        return new Learner(m).describe();
    }

    public String describe() {
        long userMin = userInfo[0];
        int userStep = (int) userInfo[1];
        int maxSkips = (int) userInfo[2];
        int matchPct = (int) userInfo[3];
        StringBuilder sb = new StringBuilder();
        sb.append("TỰ HỌC\n");
        sb.append("Đã ghi nhớ: ").append(scouts.size()).append(" làng đã xem, ")
                .append(attacks.size()).append(" trận (tổng cộng ").append(count[0]).append(" làng, ")
                .append(count[1]).append(" trận, bỏ qua ").append(count[2]).append(")\n");
        if (ready()) {
            long t = threshold(Math.max(0, userMin), maxSkips);
            sb.append("Ngưỡng chọn làng đã học: ").append(fmt(t));
            if (userMin >= 0) sb.append(" (cài đặt của bạn: ").append(fmt(userMin)).append(")");
            sb.append('\n');
            sb.append("Ước tính cướp được: ~").append(fmt(Math.round(estimatedLootPerHour(t, maxSkips))))
                    .append(" vàng+tiên dược mỗi giờ\n");
        } else {
            sb.append("Ngưỡng chọn làng: đang dùng cài đặt của bạn")
                    .append(userMin >= 0 ? " (" + fmt(userMin) + ")" : "")
                    .append(" – cần ≥ ").append(MIN_SCOUTS).append(" làng và ≥ ").append(MIN_ATTACKS)
                    .append(" trận để tự chỉnh\n");
        }
        double[] r = lootPerHour();
        if (r[1] + r[2] + r[3] > 0) {
            sb.append("Tốc độ farm đo được mỗi giờ: vàng ").append(fmt(Math.round(r[1])))
                    .append(", tiên dược ").append(fmt(Math.round(r[2])));
            if (r[3] > 0) sb.append(", đen ").append(fmt(Math.round(r[3])));
            sb.append('\n');
        }
        if (!attacks.isEmpty()) {
            sb.append(String.format(java.util.Locale.ROOT, "Trung bình %.1f lần tìm mỗi trận, %.0f giây mỗi vòng\n",
                    avgLooks(), overheadMs() / 1000 + (avgLooks() - 1) * searchMs() / 1000));
        }
        sb.append("Độ trễ sau chạm: ").append(stepDelay(userStep)).append(" ms");
        sb.append(step > 0 ? " (đã tự chỉnh, cài đặt " + userStep + " ms)\n" : " (theo cài đặt)\n");
        List<String> bad = reteach(matchPct);
        if (bad.isEmpty()) sb.append("Hình mẫu: ổn");
        else sb.append("Nên dạy lại: ").append(String.join(", ", bad));
        return sb.toString();
    }

    static String fmt(long v) {
        return String.format(java.util.Locale.ROOT, "%,d", v).replace(',', '.');
    }

    // ================================================================ lưu / đọc

    /** Lưu vào bộ nhớ lâu dài. */
    public void save() {
        StringBuilder sb = new StringBuilder();
        for (long[] s : scouts) {
            if (sb.length() > 0) sb.append(',');
            sb.append(b36(s[0])).append(':').append(b36(s[1])).append(':').append(b36(s[2]));
        }
        memory.put(K_SCOUT, sb.toString());

        sb.setLength(0);
        for (Attack a : attacks) {
            if (sb.length() > 0) sb.append(',');
            sb.append(a.enc());
        }
        memory.put(K_ATTACK, sb.toString());

        sb.setLength(0);
        for (long g : gaps) {
            if (sb.length() > 0) sb.append(',');
            sb.append(b36(g));
        }
        memory.put(K_GAP, sb.toString());

        sb.setLength(0);
        for (int[] w : waits) {
            if (sb.length() > 0) sb.append(',');
            sb.append(b36(w[0])).append(w[1] == 1 ? "" : "x");
        }
        memory.put(K_WAIT, sb.toString());

        sb.setLength(0);
        for (Map.Entry<String, TplHealth> e : tpl.entrySet()) {
            TplHealth h = e.getValue();
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(h.hits).append('/').append(h.misses).append('/');
            for (int i = 0; i < h.scores.size(); i++) {
                if (i > 0) sb.append('.');
                sb.append(h.scores.get(i));
            }
        }
        memory.put(K_TPL, sb.toString());
        memory.put(K_STEP, Integer.toString(step));
        memory.put(K_COUNT, count[0] + "," + count[1] + "," + count[2]);
        memory.put(K_INFO, userInfo[0] + "," + userInfo[1] + "," + userInfo[2] + "," + userInfo[3]);
    }

    private void load() {
        try {
            for (String s : parts(memory.get(K_SCOUT), ",")) {
                String[] p = s.split(":");
                if (p.length == 3) scouts.add(new long[]{l36(p[0]), l36(p[1]), l36(p[2])});
            }
            trim(scouts, MAX_SCOUTS);
            for (String s : parts(memory.get(K_ATTACK), ",")) {
                Attack a = Attack.dec(s);
                if (a != null) attacks.add(a);
            }
            trim(attacks, MAX_ATTACKS);
            for (String s : parts(memory.get(K_GAP), ",")) gaps.add(l36(s));
            trim(gaps, MAX_GAPS);
            for (String s : parts(memory.get(K_WAIT), ",")) {
                boolean miss = s.endsWith("x");
                waits.add(new int[]{(int) l36(miss ? s.substring(0, s.length() - 1) : s), miss ? 0 : 1});
            }
            trim(waits, MAX_WAITS);
            for (String s : parts(memory.get(K_TPL), ";")) {
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String[] p = s.substring(eq + 1).split("/", -1);
                if (p.length < 3) continue;
                TplHealth h = health(s.substring(0, eq));
                h.hits = Integer.parseInt(p[0]);
                h.misses = Integer.parseInt(p[1]);
                for (String v : parts(p[2], "\\.")) h.scores.add(Integer.parseInt(v));
                trim(h.scores, TPL_SCORES);
            }
            String st = memory.get(K_STEP);
            if (st != null && !st.isEmpty()) step = Integer.parseInt(st);
            String[] c = parts(memory.get(K_COUNT), ",");
            for (int i = 0; i < Math.min(3, c.length); i++) count[i] = Long.parseLong(c[i]);
            String[] u = parts(memory.get(K_INFO), ",");
            if (u.length == 4) {
                for (int i = 0; i < 4; i++) userInfo[i] = Long.parseLong(u[i]);
                userStepHint = (int) userInfo[1];
            }
        } catch (RuntimeException e) {
            // Dữ liệu hỏng (đổi phiên bản…): học lại từ đầu.
            scouts.clear();
            attacks.clear();
            gaps.clear();
            waits.clear();
            tpl.clear();
            step = 0;
            Arrays.fill(count, 0);
        }
    }

    private static String[] parts(String s, String sep) {
        if (s == null || s.isEmpty()) return new String[0];
        return s.split(sep);
    }

    private static String b36(long v) {
        return Long.toString(v, 36);
    }

    private static long l36(String s) {
        return Long.parseLong(s, 36);
    }
}
