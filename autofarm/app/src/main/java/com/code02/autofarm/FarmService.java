package com.code02.autofarm;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ Trợ năng: hiện bảng nổi điều khiển, cho phép chọn vị trí trên màn hình
 * và tự chạm theo vòng farm. Chạy hoàn toàn trên máy, không cần root, không cần máy tính.
 */
public class FarmService extends AccessibilityService {

    private static volatile FarmService instance;

    static boolean isRunning() {
        return instance != null;
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;

    // Bảng nổi
    private LinearLayout panel;
    private WindowManager.LayoutParams panelParams;
    private TextView statusView;

    // Chế độ chọn vị trí
    private View pickerView;
    private int pickIndex;

    // Vòng farm
    private volatile boolean farming;
    private Thread worker;

    // ---------------------------------------------------------------- vòng đời

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showPanel();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Không cần đọc nội dung màn hình.
    }

    @Override
    public void onInterrupt() {
        stopFarm();
    }

    @Override
    public void onDestroy() {
        stopFarm();
        removePicker();
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

    // ---------------------------------------------------------------- tiện ích giao diện

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
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        return b;
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

    // ---------------------------------------------------------------- bảng nổi

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

        TextView handle = new TextView(this);
        handle.setText("☰");
        handle.setTextColor(Color.WHITE);
        handle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        handle.setPadding(dp(8), 0, dp(8), 0);
        panel.addView(handle);

        panel.addView(smallButton("▶", 0xFF2E7D32, v -> startFarm()));
        panel.addView(smallButton("■", 0xFFC62828, v -> stopFarm()));
        panel.addView(smallButton("📍", 0xFF1565C0, v -> startPicker()));
        panel.addView(smallButton("_", 0xFF555555, v -> toggleCollapsed()));

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setPadding(dp(6), 0, 0, 0);
        statusView.setMaxWidth(dp(260));
        statusView.setText("Sẵn sàng. Kéo ☰ để di chuyển");
        panel.addView(statusView);

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

    private boolean collapsed;

    /** Thu gọn: chỉ còn ☰ và nút _, để bảng không che game. */
    private void toggleCollapsed() {
        collapsed = !collapsed;
        for (int i = 1; i < panel.getChildCount(); i++) {
            View c = panel.getChildAt(i);
            if (i == 4) continue; // nút _
            c.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        }
    }

    private void hidePanel() {
        if (panel != null) panel.setVisibility(View.GONE);
    }

    // ---------------------------------------------------------------- chạm

    /** Chạm và chờ hệ thống thực hiện xong. Chỉ gọi từ luồng nền. */
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

    /** Chạm không chặn luồng, gọi done khi xong. Dùng trên luồng chính. */
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

    private static GestureDescription buildTap(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        return new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 60))
                .build();
    }

    // ---------------------------------------------------------------- vòng farm

    private void startFarm() {
        if (farming) return;
        removePicker();
        String missing = Points.missingRequired(this);
        if (!missing.isEmpty()) {
            setStatus("Chưa cài vị trí: " + missing + ". Bấm 📍");
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
        Thread.sleep(ms);
        checkStop();
    }

    private void waitSeconds(String what, int seconds) throws InterruptedException {
        for (int s = seconds; s > 0; s--) {
            setStatus(what + " … " + s + " giây");
            sleepMs(1000);
        }
    }

    /** Chạm vào vị trí đã cài (nếu có), rồi nghỉ. Trả về false nếu vị trí chưa cài. */
    private boolean tapPoint(String key, long waitAfterMs) throws InterruptedException {
        int[] pt = Points.get(this, key);
        if (pt == null) return false;
        checkStop();
        tapBlocking(pt[0], pt[1]);
        sleepMs(waitAfterMs);
        return true;
    }

    /** Thả quân: chạm count lần, rải đều trên đoạn thẳng từ điểm đầu tới điểm cuối. */
    private void deployLine(int count, int tapDelay) throws InterruptedException {
        int[] a = Points.get(this, Points.DEPLOY_A);
        if (a == null) return;
        int[] b = Points.get(this, Points.DEPLOY_B);
        if (b == null) b = a;
        for (int i = 0; i < count; i++) {
            float t = count == 1 ? 0.5f : (float) i / (count - 1);
            float x = a[0] + (b[0] - a[0]) * t;
            float y = a[1] + (b[1] - a[1]) * t;
            checkStop();
            tapBlocking(x, y);
            sleepMs(tapDelay);
        }
    }

    private void deployHero() throws InterruptedException {
        int[] a = Points.get(this, Points.DEPLOY_A);
        int[] b = Points.get(this, Points.DEPLOY_B);
        if (a == null) return;
        if (b == null) b = a;
        tapBlocking((a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f);
    }

    private void farmLoop() {
        final int cycles = Config.get(this, Config.CYCLES);
        final int deployCount = Config.get(this, Config.DEPLOY_COUNT);
        final int tapDelay = Config.get(this, Config.TAP_DELAY);
        final int step = Config.get(this, Config.STEP_DELAY);
        final int trainWait = Config.get(this, Config.TRAIN_WAIT);
        final int searchWait = Config.get(this, Config.SEARCH_WAIT);
        final int battleWait = Config.get(this, Config.BATTLE_WAIT);
        final int homeWait = Config.get(this, Config.HOME_WAIT);

        int cycle = 0;
        try {
            while (farming && (cycles == 0 || cycle < cycles)) {
                cycle++;
                String tag = "Vòng " + cycle + (cycles > 0 ? "/" + cycles : "") + ": ";

                setStatus(tag + "thu tài nguyên");
                tapPoint(Points.COLLECT1, step / 2);
                tapPoint(Points.COLLECT2, step / 2);
                tapPoint(Points.COLLECT3, step / 2);

                if (Points.get(this, Points.TRAIN_OPEN) != null) {
                    setStatus(tag + "luyện quân");
                    tapPoint(Points.TRAIN_OPEN, step);
                    tapPoint(Points.QUICK_TRAIN, step);
                    tapPoint(Points.TRAIN_ARMY, step);
                    tapPoint(Points.TRAIN_CLOSE, step);
                }
                if (trainWait > 0) waitSeconds(tag + "chờ quân", trainWait);

                setStatus(tag + "tìm trận");
                tapPoint(Points.ATTACK, step);
                tapPoint(Points.FIND_MATCH, 0);
                waitSeconds(tag + "đang tìm trận", searchWait);

                setStatus(tag + "thả quân");
                if (tapPoint(Points.TROOP1, 300)) deployLine(deployCount, tapDelay);
                if (tapPoint(Points.TROOP2, 300)) deployLine(deployCount, tapDelay);
                if (tapPoint(Points.HERO, 300)) deployHero();

                waitSeconds(tag + "đang đánh", battleWait);

                setStatus(tag + "kết thúc trận");
                tapPoint(Points.END_BATTLE, step);
                tapPoint(Points.SURRENDER_OK, step);
                tapPoint(Points.RETURN_HOME, 0);
                waitSeconds(tag + "về làng", homeWait);
            }
            setStatus("Xong " + cycle + " vòng");
        } catch (InterruptedException e) {
            setStatus("Đã dừng ở vòng " + cycle);
        } finally {
            farming = false;
        }
    }

    // ---------------------------------------------------------------- chọn vị trí

    private void startPicker() {
        stopFarm();
        hidePanel();
        pickIndex = 0;
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
        setStatus("Đã lưu " + Points.countSet(this) + "/" + Points.ALL.length + " vị trí. Bấm ▶");
    }

    private void nextPick() {
        pickIndex++;
        showPicker();
    }

    private void showPicker() {
        removePicker();
        if (pickIndex >= Points.ALL.length) {
            finishPicker();
            return;
        }
        final Points.Def d = Points.ALL[pickIndex];
        int[] old = Points.get(this, d.key);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x40000000);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(rounded(0xEE1A237E));
        box.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setGravity(Gravity.CENTER);
        title.setText("Bước " + (pickIndex + 1) + "/" + Points.ALL.length + ": chạm vào\n" + d.label);
        box.addView(title);

        TextView hint = new TextView(this);
        hint.setTextColor(0xFFBBDEFB);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setGravity(Gravity.CENTER);
        hint.setText(d.hint + (old != null ? "  •  đã lưu: " + old[0] + ", " + old[1] : "  •  chưa cài"));
        box.addView(hint);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        row.addView(smallButton("Bỏ qua", 0xFF546E7A, v -> nextPick()));
        row.addView(smallButton("Xóa điểm", 0xFF8D6E63, v -> {
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
        boxLp.topMargin = dp(16);
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

    private void onPicked(Points.Def d, int x, int y) {
        Points.set(this, d.key, x, y);
        removePicker();
        if (d.passTap) {
            // Chạm luôn vào game để chuyển sang màn hình có nút tiếp theo.
            main.postDelayed(() -> tapAsync(x, y, () -> main.postDelayed(this::nextPick, 1500)), 250);
        } else {
            main.postDelayed(this::nextPick, 300);
        }
    }
}
