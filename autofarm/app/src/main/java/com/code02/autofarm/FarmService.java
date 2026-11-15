package com.code02.autofarm;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ Trợ năng: bảng nổi điều khiển, chế độ dạy vị trí / hình mẫu,
 * và vòng farm có đọc màn hình (Android 11+) để xử lý tình huống.
 */
public class FarmService extends AccessibilityService {

    private static volatile FarmService instance;

    static boolean isRunning() {
        return instance != null;
    }

    static boolean visionSupported() {
        return Build.VERSION.SDK_INT >= 30;
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;

    // Bảng nổi
    private LinearLayout panel;
    private WindowManager.LayoutParams panelParams;
    private TextView statusView;
    private boolean collapsed;

    // Hộp thoại nổi (menu nhóm, thông tin)
    private View dialogView;

    // Chế độ dạy
    private View pickerView;
    private List<Points.Def> pickList;
    private int pickIndex;
    private int[] regionFirst;

    // Vòng farm
    private volatile boolean farming;
    private Thread worker;
    private long lastShotMs;

    // ================================================================ vòng đời

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showPanel();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Không đọc cây giao diện; game vẽ bằng OpenGL nên chỉ dùng ảnh chụp màn hình.
    }

    @Override
    public void onInterrupt() {
        stopFarm();
    }

    @Override
    public void onDestroy() {
        stopFarm();
        removePicker();
        removeDialog();
        if (panel != null) {
            try {
                wm.removeView(panel);
            } catch (Exception ignored) {
            }
            panel = null;
        }
        instance = null;
        super.onDestroy();
    }

    // ================================================================ tiện ích giao diện

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(10));
        return g;
    }

    private Button smallButton(String text, int color, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setAllCaps(false);
        b.setBackground(rounded(color));
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView label(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private WindowManager.LayoutParams overlayParams(int w, int h, int extraFlags) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | extraFlags,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void setStatus(final String s) {
        main.post(() -> {
            if (statusView != null) statusView.setText(s);
        });
    }

    // ================================================================ bảng nổi

    private void showPanel() {
        if (panel != null) {
            panel.setVisibility(View.VISIBLE);
            return;
        }
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setBackground(rounded(0xDD222222));
        panel.setPadding(dp(4), dp(4), dp(8), dp(4));

        TextView handle = label("☰", 20, Color.WHITE);
        handle.setPadding(dp(8), 0, dp(8), 0);
        panel.addView(handle);                                                   // 0
        panel.addView(smallButton("▶", 0xFF2E7D32, v -> startFarm()));          // 1
        panel.addView(smallButton("■", 0xFFC62828, v -> stopFarm()));           // 2
        panel.addView(smallButton("📍", 0xFF1565C0, v -> showGroupMenu()));     // 3
        panel.addView(smallButton("🔍", 0xFF00838F, v -> testVision()));        // 4
        panel.addView(smallButton("_", 0xFF555555, v -> toggleCollapsed()));    // 5

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setPadding(dp(6), 0, 0, 0);
        statusView.setMaxWidth(dp(280));
        statusView.setText("Sẵn sàng. Kéo ☰ để di chuyển");
        panel.addView(statusView);                                               // 6

        panelParams = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, 0);
        panelParams.x = dp(12);
        panelParams.y = dp(80);

        handle.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = panelParams.x;
                        startY = panelParams.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        panelParams.x = startX + (int) (e.getRawX() - downX);
                        panelParams.y = startY + (int) (e.getRawY() - downY);
                        try {
                            wm.updateViewLayout(panel, panelParams);
                        } catch (Exception ignored) {
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });

        wm.addView(panel, panelParams);
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        for (int i = 1; i < panel.getChildCount(); i++) {
            if (i == 5) continue; // nút _
            panel.getChildAt(i).setVisibility(collapsed ? View.GONE : View.VISIBLE);
        }
    }

    private void hidePanel() {
        if (panel != null) panel.setVisibility(View.GONE);
    }

    // ================================================================ hộp thoại nổi

    private void removeDialog() {
        if (dialogView != null) {
            try {
                wm.removeView(dialogView);
            } catch (Exception ignored) {
            }
            dialogView = null;
        }
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(rounded(0xF0263238));
        box.setPadding(dp(16), dp(12), dp(16), dp(12));
        return box;
    }

    private void showDialog(View content) {
        removeDialog();
        WindowManager.LayoutParams p = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, 0);
        p.gravity = Gravity.CENTER;
        dialogView = content;
        wm.addView(content, p);
    }

    private void showGroupMenu() {
        if (farming) {
            setStatus("Bấm ■ dừng trước khi dạy");
            return;
        }
        LinearLayout box = dialogBox();
        box.addView(label("Chọn nhóm để dạy", 17, Color.WHITE));
        box.addView(label(visionSupported()
                ? "Máy hỗ trợ đọc màn hình ✓"
                : "Máy dưới Android 11: chỉ chạm theo tọa độ, không đọc màn hình", 12, 0xFFB0BEC5));
        for (final String g : Points.GROUPS) {
            String t = g + "  (" + Points.countSet(this, g) + "/" + Points.countGroup(g) + ")";
            Button b = smallButton(t, 0xFF1565C0, v -> {
                removeDialog();
                startPicker(g);
            });
            b.setLayoutParams(new LinearLayout.LayoutParams(dp(300), dp(44)));
            box.addView(b);
        }
        box.addView(smallButton("Đóng", 0xFF546E7A, v -> removeDialog()));
        showDialog(box);
    }

    private void showInfo(String text) {
        LinearLayout box = dialogBox();
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setText(text);
        ScrollView sv = new ScrollView(this);
        sv.addView(t);
        box.addView(sv, new LinearLayout.LayoutParams(dp(340), dp(320)));
        box.addView(smallButton("Đóng", 0xFF546E7A, v -> removeDialog()));
        showDialog(box);
    }

    // ================================================================ chạm

    private static GestureDescription buildTap(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        return new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 60))
                .build();
    }

    /** Chạm và chờ xong. Chỉ gọi từ luồng nền. */
    private boolean tapBlocking(float x, float y) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] ok = {false};
        boolean sent = dispatchGesture(buildTap(x, y), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription g) {
                ok[0] = true;
                latch.countDown();
            }

            @Override
            public void onCancelled(GestureDescription g) {
                latch.countDown();
            }
        }, main);
        if (!sent) return false;
        latch.await(3, TimeUnit.SECONDS);
        return ok[0];
    }

    private void tapAsync(float x, float y, final Runnable done) {
        boolean sent = dispatchGesture(buildTap(x, y), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription g) {
                done.run();
            }

            @Override
            public void onCancelled(GestureDescription g) {
                done.run();
            }
        }, main);
        if (!sent) main.post(done);
    }

    // ================================================================ chụp màn hình

    /** Chụp màn hình (Android 11+). Trả về null nếu không hỗ trợ / lỗi. Chỉ gọi từ luồng nền. */
    private Bitmap capture() throws InterruptedException {
        if (!visionSupported()) return null;
        for (int attempt = 0; attempt < 3; attempt++) {
            long wait = 400 - (SystemClock.uptimeMillis() - lastShotMs);
            if (wait > 0) Thread.sleep(wait);
            int[] err = {0};
            Bitmap b = captureOnce(err);
            lastShotMs = SystemClock.uptimeMillis();
            if (b != null) return b;
            Thread.sleep(1000); // thường do chụp quá nhanh, chờ rồi thử lại
        }
        return null;
    }

    @TargetApi(30)
    private Bitmap captureOnce(final int[] err) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Bitmap[] out = {null};
        takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
            @Override
            public void onSuccess(ScreenshotResult r) {
                HardwareBuffer hb = r.getHardwareBuffer();
                try {
                    Bitmap hw = Bitmap.wrapHardwareBuffer(hb, r.getColorSpace());
                    if (hw != null) {
                        out[0] = hw.copy(Bitmap.Config.ARGB_8888, false);
                        hw.recycle();
                    }
                } finally {
                    hb.close();
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(int errorCode) {
                err[0] = errorCode;
                latch.countDown();
            }
        });
        latch.await(4, TimeUnit.SECONDS);
        return out[0];
    }

    // ================================================================ nhận diện

    private float thr() {
        return Config.get(this, Config.MATCH_THRESHOLD) / 100f;
    }

    private boolean visual(String key) {
        return visionSupported() && Templates.has(this, key);
    }

    private Vision.Frame frame() throws InterruptedException {
        Bitmap b = capture();
        checkStop();
        return b == null ? null : new Vision.Frame(b);
    }

    private Vision.Match find(Vision.Frame f, String key) {
        if (f == null) return null;
        Vision.Tpl t = Templates.get(this, key);
        if (t == null) return null;
        Points.Def d = Points.find(key);
        int w = f.width();
        int h = f.height();
        int[] c = Points.get(this, key);
        if (d != null && d.scope == Points.Scope.NEAR && c != null) {
            int m = Math.max(t.side * 2, Math.min(w, h) / 8);
            return Vision.find(f, t, c[0] - m, c[1] - m, c[0] + m, c[1] + m, thr());
        }
        return Vision.find(f, t, 0, 0, w, h, thr());
    }

    /** Nếu thấy thông báo đã dạy thì bấm đóng. Trả về true nếu đã đóng một cái. */
    private boolean handlePopups(Vision.Frame f) throws InterruptedException {
        for (String key : Points.POPUPS) {
            if (!visual(key)) continue;
            Vision.Match m = find(f, key);
            if (m != null) {
                setStatus("Đóng thông báo (" + Math.round(m.score * 100) + "%)");
                tapBlocking(m.x, m.y);
                sleepMs(1000);
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

    /** Chờ tới khi thấy hình key (vừa chờ vừa đóng thông báo). */
    private Vision.Match waitFor(String key, long timeoutMs) throws InterruptedException {
        long end = SystemClock.uptimeMillis() + timeoutMs;
        while (true) {
            Vision.Frame f = frame();
            if (f == null) return null;
            Vision.Match m = find(f, key);
            if (m != null) return m;
            if (SystemClock.uptimeMillis() > end) return null;
            if (!handlePopups(f)) sleepMs(500);
        }
    }

    /**
     * Bấm vào mục key: nếu đã dạy hình thì tìm trên màn hình rồi bấm đúng chỗ,
     * không thì bấm theo tọa độ. Trả về false nếu không bấm được.
     */
    private boolean tapTarget(String key, long timeoutMs, long waitAfterMs) throws InterruptedException {
        if (visual(key)) {
            Vision.Match m = waitFor(key, timeoutMs);
            if (m == null) return false;
            tapBlocking(m.x, m.y);
            sleepMs(waitAfterMs);
            return true;
        }
        return tapPoint(key, waitAfterMs);
    }

    private boolean tapPoint(String key, long waitAfterMs) throws InterruptedException {
        int[] pt = Points.get(this, key);
        if (pt == null) return false;
        checkStop();
        tapBlocking(pt[0], pt[1]);
        sleepMs(waitAfterMs);
        return true;
    }

    private long readLoot(Vision.Frame f) {
        if (f == null) return -1;
        Rect g = Points.getRegion(this, Points.LOOT_GOLD);
        Rect e = Points.getRegion(this, Points.LOOT_ELIXIR);
        long a = g != null ? Ocr.readNumber(f.bmp, g) : -1;
        long b = e != null ? Ocr.readNumber(f.bmp, e) : -1;
        if (a < 0 && b < 0) return -1;
        return Math.max(a, 0) + Math.max(b, 0);
    }

    // ================================================================ vòng farm

    private void startFarm() {
        if (farming) return;
        removePicker();
        removeDialog();
        String missing = Points.missingRequired(this);
        if (!missing.isEmpty()) {
            setStatus("Chưa dạy: " + missing + ". Bấm 📍");
            return;
        }
        farming = true;
        worker = new Thread(this::farmLoop, "autofarm");
        worker.start();
    }

    private void stopFarm() {
        farming = false;
        Thread t = worker;
        if (t != null) t.interrupt();
        worker = null;
        setStatus("Đã dừng");
    }

    private void checkStop() throws InterruptedException {
        if (!farming || Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }

    private void sleepMs(long ms) throws InterruptedException {
        checkStop();
        if (ms > 0) Thread.sleep(ms);
        checkStop();
    }

    private void waitSeconds(String what, int seconds) throws InterruptedException {
        for (int s = seconds; s > 0; s--) {
            setStatus(what + " … " + s + " giây");
            sleepMs(1000);
        }
    }

    private int step() {
        return Config.get(this, Config.STEP_DELAY);
    }

    /** Đưa game về làng chính: đóng thông báo, bấm Về nhà / Kết thúc trận nếu đang kẹt. */
    private boolean ensureHome() throws InterruptedException {
        if (!visual(Points.ATTACK)) return true; // không đọc màn hình được thì coi như đang ở nhà
        long end = SystemClock.uptimeMillis() + 60000;
        String[] escapes = {Points.RETURN_HOME, Points.SURRENDER_OK, Points.END_BATTLE, Points.TRAIN_CLOSE};
        while (SystemClock.uptimeMillis() < end) {
            Vision.Frame f = frame();
            if (f == null) return true;
            if (find(f, Points.ATTACK) != null) return true;
            if (handlePopups(f)) continue;
            boolean tapped = false;
            for (String k : escapes) {
                if (!visual(k)) continue;
                Vision.Match m = find(f, k);
                if (m != null) {
                    setStatus("Đang quay về làng…");
                    tapBlocking(m.x, m.y);
                    sleepMs(step());
                    tapped = true;
                    break;
                }
            }
            if (!tapped) {
                setStatus("Chờ màn hình làng chính…");
                sleepMs(1500);
            }
        }
        return false;
    }

    private void collect(int tapDelay) throws InterruptedException {
        boolean anyVisual = false;
        for (String k : Points.COLLECTS) if (visual(k)) anyVisual = true;
        if (!anyVisual) {
            for (String k : Points.COLLECTS) tapPoint(k, step() / 2);
            return;
        }
        Vision.Frame f = frame();
        if (f == null) return;
        int n = 0;
        for (String k : Points.COLLECTS) {
            if (!visual(k)) continue;
            List<Vision.Match> ms = Vision.findAll(f, Templates.get(this, k), 0, 0, f.width(), f.height(), thr(), 15);
            for (Vision.Match m : ms) {
                tapBlocking(m.x, m.y);
                n++;
                sleepMs(tapDelay + 150);
            }
        }
        if (n > 0) setStatus("Đã thu " + n + " chỗ tài nguyên");
    }

    private void train(int trainWait) throws InterruptedException {
        if (Points.get(this, Points.TRAIN_OPEN) != null) {
            if (tapTarget(Points.TRAIN_OPEN, 4000, step())) {
                tapTarget(Points.QUICK_TRAIN, 4000, step());
                tapTarget(Points.TRAIN_ARMY, 4000, step());
                tapTarget(Points.TRAIN_CLOSE, 4000, step());
            }
        }
        if (trainWait > 0) waitSeconds("Chờ quân", trainWait);
    }

    /** Ở màn hình xem làng địch: đọc tài nguyên, bỏ qua làng nghèo. Trả về false nếu bị lạc. */
    private boolean chooseBase(String tag) throws InterruptedException {
        if (!visual(Points.NEXT)) {
            waitSeconds(tag + "đang tìm trận", Config.get(this, Config.SEARCH_WAIT));
            return true;
        }
        int minLoot = Config.get(this, Config.MIN_LOOT);
        int maxSkips = Config.get(this, Config.MAX_SKIPS);
        for (int skip = 0; ; skip++) {
            setStatus(tag + "đang tìm trận…");
            Vision.Match next = waitFor(Points.NEXT, 30000);
            if (next == null) {
                setStatus(tag + "không thấy nút Tiếp theo");
                return false;
            }
            if (minLoot <= 0) return true;
            sleepMs(1200); // chờ số tài nguyên hiện đủ
            Vision.Frame f = frame();
            long loot = readLoot(f);
            if (loot < 0) {
                setStatus(tag + "không đọc được tài nguyên → đánh luôn");
                return true;
            }
            if (loot >= minLoot) {
                setStatus(tag + "làng ngon: " + loot + " → đánh");
                return true;
            }
            if (skip >= maxSkips) {
                setStatus(tag + "đã bỏ qua " + skip + " làng → đánh luôn");
                return true;
            }
            setStatus(tag + "làng nghèo (" + loot + ") → Tiếp theo [" + (skip + 1) + "]");
            Vision.Match m = find(f, Points.NEXT);
            if (m == null) m = next;
            tapBlocking(m.x, m.y);
            sleepMs(1500);
        }
    }

    private void deployLine(int count, int tapDelay) throws InterruptedException {
        int[] a = Points.get(this, Points.DEPLOY_A);
        if (a == null) return;
        int[] b = Points.get(this, Points.DEPLOY_B);
        if (b == null) b = a;
        for (int i = 0; i < count; i++) {
            float t = count == 1 ? 0.5f : (float) i / (count - 1);
            checkStop();
            tapBlocking(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t);
            sleepMs(tapDelay);
        }
    }

    private void deploy(String tag, int deployCount, int tapDelay) throws InterruptedException {
        setStatus(tag + "thả quân");
        if (tapPoint(Points.TROOP1, 300)) deployLine(deployCount, tapDelay);
        if (tapPoint(Points.TROOP2, 300)) deployLine(deployCount, tapDelay);
        if (tapPoint(Points.HERO, 300)) {
            int[] a = Points.get(this, Points.DEPLOY_A);
            int[] b = Points.get(this, Points.DEPLOY_B);
            if (b == null) b = a;
            if (a != null) tapBlocking((a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f);
        }
    }

    private void battle(String tag, int battleWait) throws InterruptedException {
        if (!visual(Points.RETURN_HOME)) {
            waitSeconds(tag + "đang đánh", battleWait);
            setStatus(tag + "kết thúc trận");
            tapPoint(Points.END_BATTLE, step());
            tapPoint(Points.SURRENDER_OK, step());
            tapPoint(Points.RETURN_HOME, 0);
            waitSeconds(tag + "về làng", Config.get(this, Config.HOME_WAIT));
            return;
        }
        long start = SystemClock.uptimeMillis();
        long end = start + battleWait * 1000L;
        boolean ended = false;
        while (SystemClock.uptimeMillis() < end) {
            Vision.Frame f = frame();
            if (f != null && find(f, Points.RETURN_HOME) != null) {
                ended = true;
                break;
            }
            long left = (end - SystemClock.uptimeMillis()) / 1000;
            setStatus(tag + "đang đánh … còn " + left + " giây");
            sleepMs(1000);
        }
        if (!ended) {
            setStatus(tag + "hết giờ → kết thúc trận");
            tapTarget(Points.END_BATTLE, 3000, step());
            tapTarget(Points.SURRENDER_OK, 3000, step());
        }
        setStatus(tag + "về làng");
        tapTarget(Points.RETURN_HOME, 20000, step());
    }

    /** Mở danh sách thợ xây và thử nâng cấp các dòng đầu. Trả về số công trình đã bấm nâng cấp. */
    private int upgrade() throws InterruptedException {
        if (!visual(Points.UPGRADE_BTN) || !visual(Points.UPGRADE_CONFIRM)
                || Points.get(this, Points.BUILDER) == null || Points.get(this, Points.UPGRADE_ROW1) == null) {
            setStatus("Tự nâng cấp: chưa dạy đủ nhóm \"Tự nâng cấp\"");
            sleepMs(1500);
            return 0;
        }
        int[] r1 = Points.get(this, Points.UPGRADE_ROW1);
        int[] r2 = Points.get(this, Points.UPGRADE_ROW2);
        int rows = r2 != null ? 3 : 1;
        int dy = r2 != null ? r2[1] - r1[1] : 0;
        int done = 0;
        for (int i = 0; i < rows; i++) {
            setStatus("Nâng cấp: thử dòng " + (i + 1));
            if (!ensureHome()) break;
            if (!tapTarget(Points.BUILDER, 5000, step())) break;
            tapBlocking(r1[0], r1[1] + dy * i);
            sleepMs(step() * 2L); // camera chạy tới công trình

            if (!tapTarget(Points.UPGRADE_BTN, 5000, step())) {
                setStatus("Nâng cấp: không thấy nút Nâng cấp (thợ bận hoặc đang nâng cấp)");
                closeAllPopups();
                continue;
            }
            Vision.Match c = waitFor(Points.UPGRADE_CONFIRM, 5000);
            if (c == null) {
                closeAllPopups();
                continue;
            }
            Vision.Frame f = frame();
            Vision.Tpl t = Templates.get(this, Points.UPGRADE_CONFIRM);
            float red = (f != null && t != null) ? Vision.redRatio(f.bmp, c.x, c.y, t.side / 2) : 0;
            if (red > 0.03f) {
                setStatus("Nâng cấp: không đủ tài nguyên (giá màu đỏ) → bỏ qua");
                closeAllPopups();
                continue;
            }
            tapBlocking(c.x, c.y);
            sleepMs(step());
            done++;
            setStatus("Nâng cấp: đã bấm nâng cấp dòng " + (i + 1));
            closeAllPopups(); // ví dụ "tất cả thợ đều bận"
        }
        return done;
    }

    private void farmLoop() {
        final int cycles = Config.get(this, Config.CYCLES);
        final int deployCount = Config.get(this, Config.DEPLOY_COUNT);
        final int tapDelay = Config.get(this, Config.TAP_DELAY);
        final int trainWait = Config.get(this, Config.TRAIN_WAIT);
        final int battleWait = Config.get(this, Config.BATTLE_WAIT);
        final boolean autoUpgrade = Config.on(this, Config.AUTO_UPGRADE);
        final int upgradeEvery = Config.get(this, Config.UPGRADE_EVERY);

        int cycle = 0;
        int lost = 0;
        int upgrades = 0;
        try {
            while (farming && (cycles == 0 || cycle < cycles)) {
                cycle++;
                String tag = "Vòng " + cycle + (cycles > 0 ? "/" + cycles : "") + ": ";

                if (!ensureHome()) {
                    lost++;
                    setStatus(tag + "không tìm thấy làng chính (" + lost + "/3)");
                    if (lost >= 3) {
                        setStatus("Dừng: 3 lần không về được làng. Kiểm tra game / dạy thêm thông báo");
                        break;
                    }
                    continue;
                }
                lost = 0;

                setStatus(tag + "thu tài nguyên");
                collect(tapDelay);

                if (autoUpgrade && (cycle - 1) % upgradeEvery == 0) {
                    upgrades += upgrade();
                    if (!ensureHome()) continue;
                }

                setStatus(tag + "luyện quân");
                train(trainWait);

                setStatus(tag + "tìm trận");
                if (!tapTarget(Points.ATTACK, 5000, step())) continue;
                if (!tapTarget(Points.FIND_MATCH, 8000, 0)) continue;
                if (!chooseBase(tag)) continue;

                deploy(tag, deployCount, tapDelay);
                battle(tag, battleWait);
            }
            setStatus("Xong " + cycle + " vòng" + (upgrades > 0 ? ", nâng cấp " + upgrades : ""));
        } catch (InterruptedException e) {
            setStatus("Đã dừng ở vòng " + cycle);
        } catch (RuntimeException e) {
            setStatus("Lỗi: " + e.getClass().getSimpleName() + " – " + e.getMessage());
        } finally {
            farming = false;
        }
    }

    // ================================================================ thử nhận diện

    private void testVision() {
        if (farming) {
            setStatus("Đang chạy – bấm ■ trước");
            return;
        }
        if (!visionSupported()) {
            showInfo("Máy cần Android 11 trở lên để đọc màn hình.\nBot vẫn chạy được theo tọa độ.");
            return;
        }
        setStatus("Đang chụp và nhận diện…");
        new Thread(() -> {
            String result;
            try {
                farming = true; // cho phép frame()/checkStop chạy
                Vision.Frame f = frame();
                if (f == null) {
                    result = "Không chụp được màn hình. Thử tắt rồi bật lại Trợ năng cho AutoFarm.";
                } else {
                    StringBuilder ok = new StringBuilder();
                    StringBuilder miss = new StringBuilder();
                    for (Points.Def d : Points.ALL) {
                        if (!d.template || !Templates.has(this, d.key)) continue;
                        Vision.Match m;
                        if (d.scope == Points.Scope.FULL && d.group.equals(Points.G_COLLECT)) {
                            List<Vision.Match> l = Vision.findAll(f, Templates.get(this, d.key),
                                    0, 0, f.width(), f.height(), thr(), 15);
                            m = l.isEmpty() ? null : l.get(0);
                            if (m != null) {
                                ok.append("✓ ").append(d.label).append(" ×").append(l.size())
                                        .append(" (").append(Math.round(m.score * 100)).append("%)\n");
                                continue;
                            }
                        } else {
                            m = find(f, d.key);
                        }
                        if (m != null) {
                            ok.append("✓ ").append(d.label).append(" (").append(Math.round(m.score * 100)).append("%)\n");
                        } else {
                            miss.append("✗ ").append(d.label).append('\n');
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Màn hình ").append(f.width()).append("×").append(f.height())
                            .append(", ngưỡng ").append(Math.round(thr() * 100)).append("%\n\n");
                    sb.append("THẤY TRÊN MÀN HÌNH:\n").append(ok.length() > 0 ? ok : "(không có)\n");
                    sb.append("\nKHÔNG THẤY:\n").append(miss.length() > 0 ? miss : "(không có)\n");
                    Rect g = Points.getRegion(this, Points.LOOT_GOLD);
                    Rect e = Points.getRegion(this, Points.LOOT_ELIXIR);
                    if (g != null || e != null) {
                        sb.append("\nĐỌC SỐ (đúng khi đang xem làng địch):\n");
                        if (g != null) sb.append("Vàng: ").append(fmt(Ocr.readNumber(f.bmp, g))).append('\n');
                        if (e != null) sb.append("Tiên dược: ").append(fmt(Ocr.readNumber(f.bmp, e))).append('\n');
                    }
                    result = sb.toString();
                }
            } catch (InterruptedException ex) {
                result = "Bị ngắt";
            } catch (RuntimeException ex) {
                result = "Lỗi: " + ex;
            } finally {
                farming = false;
            }
            final String r = result;
            main.post(() -> {
                setStatus("Sẵn sàng");
                showInfo(r);
            });
        }, "autofarm-test").start();
    }

    private static String fmt(long v) {
        return v < 0 ? "không đọc được" : String.valueOf(v);
    }

    // ================================================================ chế độ dạy

    private void startPicker(String group) {
        stopFarm();
        hidePanel();
        pickList = Points.inGroup(group);
        pickIndex = 0;
        regionFirst = null;
        showPicker();
    }

    private void removePicker() {
        if (pickerView != null) {
            try {
                wm.removeView(pickerView);
            } catch (Exception ignored) {
            }
            pickerView = null;
        }
    }

    private void finishPicker() {
        removePicker();
        showPanel();
        setStatus("Đã dạy " + Points.countSet(this, null) + "/" + Points.ALL.length + " mục. Bấm 🔍 để thử");
    }

    private void nextPick() {
        regionFirst = null;
        pickIndex++;
        showPicker();
    }

    private void showPicker() {
        removePicker();
        if (pickList == null || pickIndex >= pickList.size()) {
            finishPicker();
            return;
        }
        final Points.Def d = pickList.get(pickIndex);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x30000000);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(rounded(0xEE1A237E));
        box.setPadding(dp(14), dp(10), dp(14), dp(10));

        String what;
        if (d.kind == Points.Kind.REGION) {
            what = regionFirst == null ? "chạm GÓC TRÊN-TRÁI của\n" : "chạm GÓC DƯỚI-PHẢI của\n";
        } else {
            what = "chạm vào\n";
        }
        box.addView(label(d.group + " – bước " + (pickIndex + 1) + "/" + pickList.size() + ": " + what + d.label,
                16, Color.WHITE));
        boolean set = Points.isSet(this, d);
        String extra = set ? "đã dạy" : "chưa dạy";
        if (d.template && Templates.has(this, d.key)) extra += " + có hình mẫu";
        box.addView(label(d.hint + "  •  " + extra, 13, 0xFFBBDEFB));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        row.addView(smallButton("Bỏ qua", 0xFF546E7A, v -> nextPick()));
        row.addView(smallButton("Xóa", 0xFF8D6E63, v -> {
            Points.clear(this, d.key);
            nextPick();
        }));
        row.addView(smallButton("Ẩn 5 giây", 0xFF6A1B9A, v -> {
            removePicker();
            main.postDelayed(this::showPicker, 5000);
        }));
        row.addView(smallButton("Xong", 0xFF2E7D32, v -> finishPicker()));
        box.addView(row);

        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        boxLp.topMargin = dp(12);
        root.addView(box, boxLp);

        root.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                onPicked(d, Math.round(e.getRawX()), Math.round(e.getRawY()));
            }
            return true;
        });

        WindowManager.LayoutParams p = overlayParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        pickerView = root;
        wm.addView(root, p);
    }

    private void onPicked(final Points.Def d, final int x, final int y) {
        if (d.kind == Points.Kind.REGION) {
            if (regionFirst == null) {
                regionFirst = new int[]{x, y};
                showPicker();
            } else {
                Points.set(this, d.key + "_1", regionFirst[0], regionFirst[1]);
                Points.set(this, d.key + "_2", x, y);
                nextPick();
            }
            return;
        }

        Points.set(this, d.key, x, y);
        removePicker();

        final Runnable afterCapture = () -> {
            if (d.passTap) {
                tapAsync(x, y, () -> main.postDelayed(this::nextPick, 1500));
            } else {
                main.postDelayed(this::nextPick, 300);
            }
        };

        if (d.template && visionSupported()) {
            // Chụp màn hình (đã gỡ lớp phủ) rồi cắt hình mẫu quanh chỗ vừa chạm.
            new Thread(() -> {
                boolean saved = false;
                try {
                    Thread.sleep(350);
                    Bitmap shot = capture();
                    if (shot != null) {
                        int pct = Config.get(this, Config.TEMPLATE_SIZE);
                        int side = Math.min(shot.getWidth(), shot.getHeight()) * pct / 100;
                        saved = Templates.save(this, d.key, shot, x, y, side);
                    }
                } catch (InterruptedException ignored) {
                }
                final boolean ok = saved;
                main.post(() -> {
                    if (!ok) setStatus("Không chụp được hình mẫu cho: " + d.label);
                    main.postDelayed(afterCapture, 100);
                });
            }, "autofarm-teach").start();
        } else {
            main.postDelayed(afterCapture, 250);
        }
    }
}
