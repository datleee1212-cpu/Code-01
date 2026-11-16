package com.code02.autofarm;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
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

import com.code02.autofarm.core.Bot;
import com.code02.autofarm.core.Box;
import com.code02.autofarm.core.Device;
import com.code02.autofarm.core.Img;
import com.code02.autofarm.core.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ Trợ năng: bảng nổi điều khiển, chế độ dạy vị trí / hình mẫu,
 * và hai kênh chạy có đọc màn hình (Android 11+):
 * kênh Farm (▶: đánh + thu + nâng cấp) và kênh Lộ trình (🛠: chỉ nâng cấp theo lộ trình, chạy định kỳ).
 * Mỗi lúc chỉ chạy một kênh; ■ dừng kênh đang chạy.
 */
public class FarmService extends AccessibilityService {

    private static volatile FarmService instance;

    static boolean isRunning() {
        return instance != null;
    }

    static boolean visionSupported() {
        return Build.VERSION.SDK_INT >= 30;
    }

    /** Đang có kênh chạy (hoặc đang thử nhận diện) không – màn hình chính dùng để cảnh báo. */
    static boolean isBusy() {
        FarmService s = instance;
        return s != null && s.worker != null;
    }

    // Kênh đang chạy
    private static final int CH_NONE = 0;
    private static final int CH_FARM = 1;
    private static final int CH_ROADMAP = 2;
    private static final int CH_TEST = 3;

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;

    // Bảng nổi
    private LinearLayout panel;
    private WindowManager.LayoutParams panelParams;
    private TextView statusView;
    private View collapseButton;
    private boolean collapsed;

    // Hộp thoại nổi (menu nhóm, thông tin)
    private View dialogView;

    // Chế độ dạy
    private View pickerView;
    private List<Points.Def> pickList;
    private int pickIndex;
    private int[] regionFirst;

    // Kênh chạy: chỉ luồng `worker` được điều khiển máy; ■ đặt worker = null là luồng cũ tự dừng.
    private volatile Thread worker;
    private volatile int channel = CH_NONE;
    private long lastShotMs;
    private PrefsMemory memory;

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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | extraFlags,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void setStatus(final String s) {
        main.post(() -> {
            if (statusView != null) statusView.setText(s);
        });
    }

    private static String channelName(int ch) {
        switch (ch) {
            case CH_FARM:
                return "Farm";
            case CH_ROADMAP:
                return "Lộ trình";
            default:
                return "";
        }
    }

    /** Giữ màn hình sáng chỉ khi có kênh đang chạy (màn hình tắt là bot không chạm được vào game). */
    private void keepScreenOn(final boolean on) {
        main.post(() -> {
            if (panel == null || panelParams == null) return;
            int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            int flags = on ? (panelParams.flags | flag) : (panelParams.flags & ~flag);
            if (flags == panelParams.flags) return;
            panelParams.flags = flags;
            try {
                wm.updateViewLayout(panel, panelParams);
            } catch (Exception ignored) {
            }
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
        panel.addView(handle);                                                        // 0
        panel.addView(smallButton("▶", 0xFF2E7D32, v -> startChannel(CH_FARM)));     // 1 kênh Farm
        panel.addView(smallButton("🛠", 0xFFEF6C00, v -> startChannel(CH_ROADMAP))); // 2 kênh Lộ trình
        panel.addView(smallButton("■", 0xFFC62828, v -> stopFarm()));                // 3
        panel.addView(smallButton("📍", 0xFF1565C0, v -> showGroupMenu()));          // 4
        panel.addView(smallButton("🔍", 0xFF00838F, v -> testVision()));             // 5
        collapseButton = smallButton("_", 0xFF555555, v -> toggleCollapsed());
        panel.addView(collapseButton);                                                // 6

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setPadding(dp(6), 0, 0, 0);
        statusView.setMaxWidth(dp(280));
        statusView.setText("Sẵn sàng. ▶ Farm, 🛠 Lộ trình. Kéo ☰ để di chuyển");
        panel.addView(statusView);                                                    // 7

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
        // Thu gọn: chỉ còn ☰ và nút _ (kênh vẫn chạy; mở lại để bấm ■).
        for (int i = 1; i < panel.getChildCount(); i++) {
            View v = panel.getChildAt(i);
            if (v == collapseButton) continue;
            v.setVisibility(collapsed ? View.GONE : View.VISIBLE);
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
        if (worker != null) {
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
    private Img capture() throws InterruptedException {
        if (!visionSupported()) return null;
        for (int attempt = 0; attempt < 3; attempt++) {
            long wait = 400 - (SystemClock.uptimeMillis() - lastShotMs);
            if (wait > 0) Thread.sleep(wait);
            int[] err = {0};
            Bitmap b = captureOnce(err);
            lastShotMs = SystemClock.uptimeMillis();
            if (b != null) {
                b = matchOrientation(b);
                Img img = Templates.toImg(b);
                b.recycle();
                return img;
            }
            Thread.sleep(1000); // thường do chụp quá nhanh, chờ rồi thử lại
        }
        return null;
    }

    /**
     * Một số máy trả ảnh chụp theo hướng "gốc" của màn hình. Galaxy Tab S10+ có hướng gốc là ngang
     * nên thường đã khớp với game; nếu lệch (ảnh dọc khi game đang ngang) thì xoay cho khớp tọa độ chạm.
     */
    @SuppressWarnings("deprecation")
    private Bitmap matchOrientation(Bitmap b) {
        android.graphics.Point real = new android.graphics.Point();
        Display d = wm.getDefaultDisplay();
        d.getRealSize(real);
        boolean shotLandscape = b.getWidth() > b.getHeight();
        boolean screenLandscape = real.x > real.y;
        if (shotLandscape == screenLandscape) return b;
        int degrees = (360 - d.getRotation() * 90) % 360;
        if (degrees == 0) degrees = 90;
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.postRotate(degrees);
        Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, false);
        if (r != b) b.recycle();
        return r;
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

    // ================================================================ nối với lõi bot

    private void swipeBlocking(int x1, int y1, int x2, int y2, int ms) throws InterruptedException {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, Math.max(100, ms)))
                .build();
        final CountDownLatch latch = new CountDownLatch(1);
        boolean sent = dispatchGesture(g, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription d) {
                latch.countDown();
            }

            @Override
            public void onCancelled(GestureDescription d) {
                latch.countDown();
            }
        }, main);
        if (sent) latch.await(ms + 3000L, TimeUnit.MILLISECONDS);
    }

    /** Thiết bị thật cho lõi bot. */
    private final Device device = new Device() {
        @Override
        public Img capture() throws InterruptedException {
            checkStop();
            return FarmService.this.capture();
        }

        @Override
        public void tap(int x, int y) throws InterruptedException {
            checkStop();
            tapBlocking(x, y);
        }

        @Override
        public void swipe(int x1, int y1, int x2, int y2, int ms) throws InterruptedException {
            checkStop();
            swipeBlocking(x1, y1, x2, y2, ms);
        }

        @Override
        public void sleep(long ms) throws InterruptedException {
            sleepMs(ms);
        }

        @Override
        public long now() {
            return SystemClock.uptimeMillis();
        }

        @Override
        public void status(String s) {
            if (Thread.currentThread() != worker) return; // luồng cũ đã bị dừng
            String name = channelName(channel);
            setStatus(name.isEmpty() ? s : "[" + name + "] " + s);
        }
    };

    /** Những gì đã dạy, cho lõi bot. */
    private final Store store = new Store() {
        @Override
        public int[] point(String key) {
            return Points.get(FarmService.this, key);
        }

        @Override
        public Box region(String key) {
            return Points.getRegion(FarmService.this, key);
        }

        @Override
        public Img template(String key) {
            Points.Def d = Points.find(key);
            if (d != null && !d.template) return null;
            return Templates.load(FarmService.this, key);
        }
    };

    private Ocr ocr;

    private Bot newBot() {
        if (ocr == null) ocr = new Ocr();
        if (memory == null) memory = new PrefsMemory(this);
        return new Bot(device, ocr, store, Config.settings(this), memory);
    }

    // ================================================================ kênh chạy

    /** Bắt đầu một kênh trên luồng nền. Chỉ một kênh chạy một lúc. */
    private void startChannel(final int ch) {
        if (worker != null) {
            String name = channelName(channel);
            setStatus((name.isEmpty() ? "Đang bận" : "Kênh " + name + " đang chạy") + " – bấm ■ trước");
            return;
        }
        removePicker();
        removeDialog();
        if (ch == CH_FARM) {
            String missing = Points.missingRequired(this);
            if (!missing.isEmpty()) {
                setStatus("Chưa dạy: " + missing + ". Bấm 📍");
                return;
            }
        } else {
            if (!visionSupported()) {
                setStatus("Kênh Lộ trình cần Android 11+ để đọc danh sách thợ xây");
                return;
            }
            if (Points.get(this, Points.ATTACK) == null) {
                setStatus("Chưa dạy nút Tấn công (nhóm Đánh trận) – bot cần nó để biết đang ở làng. Bấm 📍");
                return;
            }
        }
        Thread t = new Thread(() -> {
            try {
                Bot bot = newBot();
                if (ch == CH_FARM) {
                    bot.run();
                } else {
                    String miss = bot.upgradeMissing();
                    if (!miss.isEmpty()) {
                        device.status("Chưa dạy nhóm Tự nâng cấp: " + miss + ". Bấm 📍");
                        return;
                    }
                    bot.runUpgradeChannel();
                }
            } catch (InterruptedException e) {
                if (Thread.currentThread() == worker) setStatus("Đã dừng");
            } catch (RuntimeException e) {
                device.status("Lỗi: " + e.getClass().getSimpleName() + " – " + e.getMessage());
            } finally {
                finishWorker();
            }
        }, ch == CH_FARM ? "autofarm-farm" : "autofarm-lotrinh");
        synchronized (this) {
            channel = ch;
            worker = t;
        }
        keepScreenOn(true);
        t.start();
    }

    /** Luồng nền tự dọn khi kết thúc (chỉ khi nó vẫn là luồng hiện hành). */
    private void finishWorker() {
        synchronized (this) {
            if (worker != Thread.currentThread()) return;
            worker = null;
            channel = CH_NONE;
        }
        keepScreenOn(false);
    }

    /** Dừng kênh đang chạy (nút ■). */
    private void stopFarm() {
        Thread t;
        synchronized (this) {
            t = worker;
            worker = null;
            channel = CH_NONE;
        }
        if (t != null) t.interrupt();
        keepScreenOn(false);
        setStatus("Đã dừng");
    }

    private void checkStop() throws InterruptedException {
        if (Thread.currentThread() != worker || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void sleepMs(long ms) throws InterruptedException {
        checkStop();
        if (ms > 0) Thread.sleep(ms);
        checkStop();
    }

    // ================================================================ thử nhận diện

    private void testVision() {
        if (worker != null) {
            setStatus("Đang chạy – bấm ■ trước");
            return;
        }
        if (!visionSupported()) {
            showInfo("Máy cần Android 11 trở lên để đọc màn hình.\nBot vẫn chạy được theo tọa độ.");
            return;
        }
        setStatus("Đang chụp và nhận diện…");
        Thread t = new Thread(() -> {
            String result;
            try {
                List<String> keys = new ArrayList<>();
                for (Points.Def d : Points.ALL) if (d.template) keys.add(d.key);
                result = newBot().diagnose(keys, k -> Points.find(k).label);
            } catch (InterruptedException ex) {
                result = "Bị ngắt";
            } catch (RuntimeException ex) {
                result = "Lỗi: " + ex;
            } finally {
                finishWorker();
            }
            final String r = result;
            main.post(() -> {
                setStatus("Sẵn sàng");
                showInfo(r);
            });
        }, "autofarm-test");
        synchronized (this) {
            channel = CH_TEST; // luồng thử được phép chụp màn hình
            worker = t;
        }
        t.start();
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
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                    Img shot = capture();
                    if (shot != null) {
                        int pct = Config.get(this, Config.TEMPLATE_SIZE);
                        int side = Math.min(shot.w, shot.h) * pct / 100;
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
