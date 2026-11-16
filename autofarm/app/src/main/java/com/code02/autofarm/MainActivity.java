package com.code02.autofarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.code02.autofarm.core.Learner;
import com.code02.autofarm.core.PlanTH17;
import com.code02.autofarm.core.Roadmap;

/** Màn hình chính: cấp quyền, mở game, chỉnh thông số, xem tiến độ lộ trình & những gì bot đã học. */
public class MainActivity extends Activity {

    private static final String COC_PACKAGE = "com.supercell.clashofclans";

    private static final int BLUE = 0xFF1565C0;
    private static final int DARK = 0xFF333333;

    private TextView statusView;
    private TextView roadmapView;
    private TextView planView;
    private TextView learnView;
    private final View[] inputs = new View[Config.ALL.length];

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    private TextView heading(String s) {
        TextView t = text(s, 19, BLUE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(18), 0, dp(4));
        return t;
    }

    /** Tiêu đề nhóm thông số: chữ trắng trên nền xanh, dễ nhìn khi cuộn danh sách dài. */
    private TextView sectionHeader(String s) {
        TextView t = text(s, 16, Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF37474F);
        g.setCornerRadius(dp(6));
        t.setBackground(g);
        t.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        t.setLayoutParams(lp);
        return t;
    }

    /** Khung chữ có nền nhạt (giải thích kênh, tiến độ). */
    private TextView card(String s, int bg) {
        TextView t = text(s, 14, DARK);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(8));
        t.setBackground(g);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    private Button button(String s, Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(16), dp(20), dp(32));

        col.addView(text("AutoFarm 0.4", 26, Color.BLACK));
        col.addView(text("Bot đọc màn hình, tự xử lý tình huống, tự nâng cấp theo lộ trình và tự cải thiện. "
                + "Chạy ngay trên máy tính bảng, không cần root, không cần máy tính.", 15, 0xFF444444));

        col.addView(heading("Hai kênh chạy"));
        col.addView(card("▶  Kênh Farm: đi đánh + thu tài nguyên + nâng cấp (nếu bật \"Kênh Farm: tự nâng cấp\"). "
                + "Chạy liên tục, kiếm tài nguyên.\n\n"
                + "🛠  Kênh Lộ trình: KHÔNG đi đánh. Cứ mỗi vài chục phút bot về làng, thu tài nguyên rồi giao việc "
                + "cho thợ xây rảnh theo lộ trình đã chọn (ví dụ TH16 → TH17), xong thì nghỉ chờ lượt sau. "
                + "Hợp khi bạn tự đi đánh hoặc chỉ muốn thợ xây không bao giờ rảnh.\n\n"
                + "Mỗi lúc chỉ chạy một kênh. ■ dừng kênh đang chạy. Dòng trạng thái trên bảng nổi ghi "
                + "[Farm] hoặc [Lộ trình] để biết kênh nào đang chạy.", 0xFFE3F2FD));

        statusView = text("", 15, Color.BLACK);
        col.addView(statusView);

        col.addView(heading("Bước 1 – Bật quyền"));
        col.addView(text("Cài đặt → Trợ năng → Ứng dụng đã tải xuống → AutoFarm → Bật.\n"
                + "Android 13+: nếu nút bị mờ, vào Thông tin ứng dụng → ⋮ → \"Cho phép cài đặt bị hạn chế\".\n"
                + "Vừa cập nhật app từ bản cũ: tắt rồi bật lại Trợ năng để có quyền chụp màn hình.",
                14, DARK));
        col.addView(button("Mở cài đặt Trợ năng", () ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        col.addView(button("Mở thông tin ứng dụng", () -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }));
        col.addView(text("Máy Samsung (vd Galaxy Tab S10+): để bot không bị tắt giữa chừng, đặt Pin của AutoFarm "
                + "là \"Không hạn chế\" (Thông tin ứng dụng → Pin → Không hạn chế). Trong Game Booster, tắt "
                + "\"Chặn cử chỉ / chạm\" nếu có. Chơi game ở chế độ toàn màn hình, xoay ngang như bình thường.",
                14, 0xFF333333));
        col.addView(button("Tắt tối ưu pin cho AutoFarm", () -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        }));

        col.addView(heading("Bước 2 – Dạy bot"));
        col.addView(text("Vào game, bấm 📍 trên bảng nổi, chọn nhóm rồi chạm vào từng thứ theo hướng dẫn. "
                + "Bot chụp lại hình quanh chỗ bạn chạm để sau này tự tìm trên màn hình.\n\n"
                + "• Đánh trận: Tấn công, Tìm trận, Tiếp theo, 2 vùng số tài nguyên, ô quân, đường thả quân, "
                + "Kết thúc trận, Về nhà.\n"
                + "• Thu tài nguyên: biểu tượng vàng/tiên dược nổi trên mỏ (dạy lúc mỏ đang đầy).\n"
                + "• Tự nâng cấp: biểu tượng thợ xây, dòng 1–2 trong danh sách, nút Nâng cấp, nút giá xác nhận.\n"
                + "• Đóng thông báo: mỗi khi gặp thông báo lạ, dạy nút X / OK / Thử lại của nó. "
                + "Tuyệt đối không dạy nút mua bằng ngọc.\n\n"
                + "Kênh Lộ trình chỉ cần nút Tấn công (để biết đang ở làng) và nhóm Tự nâng cấp.\n"
                + "Sau đó bấm 🔍 để kiểm tra bot nhìn thấy gì trên màn hình hiện tại.",
                14, DARK));
        col.addView(button("Mở Clash of Clans", () -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(COC_PACKAGE);
            if (i == null) {
                Toast.makeText(this, "Chưa cài Clash of Clans", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(i);
            }
        }));

        col.addView(heading("Bước 3 – Chỉnh thông số"));
        int idx = 0;
        for (Config.Section sec : Config.SECTIONS) {
            col.addView(sectionHeader(sec.title));
            if (sec.note != null && !sec.note.isEmpty()) col.addView(text(sec.note, 13, 0xFF607D8B));
            for (Config.Field f : sec.fields) {
                if (f.bool) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(f.label);
                    cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    cb.setChecked(Config.on(this, f));
                    inputs[idx] = cb;
                    col.addView(cb);
                } else {
                    col.addView(text(f.label + "  (" + f.min + "–" + f.max + ")", 14, DARK));
                    EditText e = new EditText(this);
                    e.setInputType(InputType.TYPE_CLASS_NUMBER);
                    e.setText(String.valueOf(Config.get(this, f)));
                    inputs[idx] = e;
                    col.addView(e);
                    if (f == Config.ROADMAP) {
                        roadmapView = text("", 13, 0xFF2E7D32);
                        col.addView(roadmapView);
                    }
                }
                idx++;
            }
        }
        col.addView(button("Lưu thông số", this::save));

        col.addView(heading("Bước 4 – Chạy"));
        col.addView(text("Ở làng chính bấm ▶ (Farm) hoặc 🛠 (Lộ trình).\n"
                + "▶ Farm: về làng nếu bị lạc → đóng thông báo → thu tài nguyên → nâng cấp (nếu bật) → "
                + "luyện quân → tìm trận, bỏ qua làng nghèo → thả quân → chờ trận xong → về nhà.\n"
                + "🛠 Lộ trình: về làng → thu tài nguyên → giao việc cho thợ rảnh theo lộ trình → nghỉ "
                + "theo \"Kênh Lộ trình: kiểm tra thợ xây mỗi (phút)\" → lặp lại. "
                + "Màn hình được giữ sáng trong lúc kênh chạy, đừng tắt game.\n"
                + "■ dừng, _ thu gọn, kéo ☰ để đưa bảng ra chỗ không che nút.\n\n"
                + "⚠ Supercell cấm dùng bot trong Clash of Clans, tài khoản có thể bị khóa. "
                + "Nên thử trên tài khoản phụ.", 14, DARK));

        col.addView(heading("Tiến độ lộ trình & tự cải thiện"));
        col.addView(text("Bot tự ghi nhớ sau mỗi trận / mỗi lượt nâng cấp. Mục này cập nhật mỗi lần bạn mở lại app.",
                13, 0xFF607D8B));
        col.addView(sectionHeader("📋 Tiến độ lộ trình TH16 → TH17"));
        planView = card("", 0xFFF1F8E9);
        col.addView(planView);
        col.addView(sectionHeader("🧠 Những gì bot đã học"));
        learnView = card("", 0xFFFFF8E1);
        col.addView(learnView);
        col.addView(button("Xóa dữ liệu đã học", this::confirmClearMemory));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshProgress();
        refreshRoadmapName();
    }

    private void refreshStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(FarmService.isRunning() ? "✅ Trợ năng đã bật – bảng nổi đang hiện" : "❌ Chưa bật Trợ năng cho AutoFarm");
        sb.append(FarmService.visionSupported()
                ? "\n✅ Đọc màn hình: được hỗ trợ"
                : "\n⚠ Android dưới 11: không đọc được màn hình, chỉ chạm theo tọa độ (kênh Lộ trình không dùng được)");
        for (String g : Points.GROUPS) {
            sb.append("\n📍 ").append(g).append(": ").append(Points.countSet(this, g))
                    .append("/").append(Points.countGroup(g));
        }
        String missing = Points.missingRequired(this);
        if (!missing.isEmpty()) sb.append("\n⚠ Kênh Farm còn thiếu: ").append(missing);
        statusView.setText(sb.toString());
    }

    private void refreshRoadmapName() {
        if (roadmapView == null) return;
        int r = Config.get(this, Config.ROADMAP);
        roadmapView.setText("Đang dùng: " + r + " – " + Roadmap.presetName(r));
    }

    /** Đọc tiến độ lộ trình và tóm tắt đã học từ bộ nhớ lâu dài. */
    private void refreshProgress() {
        PrefsMemory memory = new PrefsMemory(this);
        String plan = safe(() -> PlanTH17.checklist(memory));
        String learn = safe(() -> Learner.summary(memory));
        planView.setText(plan.trim().isEmpty()
                ? "Chưa có tiến độ. Chọn lộ trình 4 (TH16→TH17) rồi chạy 🛠 hoặc ▶ (có bật tự nâng cấp): "
                + "bot sẽ đánh dấu các mục đã nâng ở đây."
                : plan.trim());
        learnView.setText(learn.trim().isEmpty()
                ? "Bot chưa học được gì. Cứ chạy vài trận / vài lượt nâng cấp, kết quả sẽ hiện ở đây."
                : learn.trim());
    }

    private interface Text {
        String get();
    }

    private static String safe(Text t) {
        try {
            String s = t.get();
            return s == null ? "" : s;
        } catch (RuntimeException e) {
            return "Không đọc được dữ liệu: " + e.getMessage();
        }
    }

    private void confirmClearMemory() {
        if (FarmService.isBusy()) {
            Toast.makeText(this, "Bot đang chạy – bấm ■ trên bảng nổi trước rồi hãy xóa.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xóa dữ liệu đã học?")
                .setMessage("Bot sẽ quên lịch sử trận, các thông số đã tự chỉnh và tiến độ lộ trình, "
                        + "rồi học lại từ đầu.\n\nVị trí / hình mẫu đã dạy và thông số bạn nhập vẫn được giữ.")
                .setPositiveButton("Xóa", (d, w) -> {
                    PrefsMemory.clear(this);
                    refreshProgress();
                    Toast.makeText(this, "Đã xóa dữ liệu đã học.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void save() {
        for (int i = 0; i < Config.ALL.length; i++) {
            Config.Field f = Config.ALL[i];
            if (f.bool) {
                Config.set(this, f, ((CheckBox) inputs[i]).isChecked() ? 1 : 0);
                continue;
            }
            EditText e = (EditText) inputs[i];
            int v;
            try {
                v = Integer.parseInt(e.getText().toString().trim());
            } catch (NumberFormatException ex) {
                v = f.def;
            }
            Config.set(this, f, v);
            e.setText(String.valueOf(Config.get(this, f)));
        }
        refreshRoadmapName();
        Toast.makeText(this, "Đã lưu. Áp dụng từ lần bấm ▶ / 🛠 tiếp theo.", Toast.LENGTH_SHORT).show();
    }
}
