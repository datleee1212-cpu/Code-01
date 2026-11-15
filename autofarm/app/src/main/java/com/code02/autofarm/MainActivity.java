package com.code02.autofarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Màn hình chính: cấp quyền, mở game, chỉnh thông số. */
public class MainActivity extends Activity {

    private static final String COC_PACKAGE = "com.supercell.clashofclans";

    private TextView statusView;
    private final EditText[] inputs = new EditText[Config.ALL.length];

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

        col.addView(text("AutoFarm", 26, Color.BLACK));
        col.addView(text("Bot tự chạm chạy ngay trên máy tính bảng. Không cần root, không cần máy tính.",
                15, 0xFF444444));

        statusView = text("", 15, 0xFF000000);
        col.addView(statusView);

        col.addView(text("Bước 1 – Bật quyền", 19, 0xFF1565C0));
        col.addView(text("Mở Cài đặt → Trợ năng → Ứng dụng đã tải xuống → AutoFarm → Bật.\n"
                + "Android 13 trở lên: nếu nút bị mờ, vào Cài đặt → Ứng dụng → AutoFarm → ⋮ (góc phải) "
                + "→ \"Cho phép cài đặt bị hạn chế\", rồi bật lại.", 14, 0xFF333333));
        col.addView(button("Mở cài đặt Trợ năng", () ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        col.addView(button("Mở thông tin ứng dụng (cho phép cài đặt bị hạn chế)", () -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }));

        col.addView(text("Bước 2 – Cài vị trí nút", 19, 0xFF1565C0));
        col.addView(text("Mở Clash of Clans, ở làng chính. Trên bảng nổi bấm 📍, rồi lần lượt chạm vào từng nút "
                + "theo hướng dẫn hiện trên màn hình. Nút nào chưa thấy thì bấm \"Ẩn 5 giây\" để tự bấm trong game, "
                + "nút không dùng thì bấm \"Bỏ qua\". Chỉ cần làm một lần.\n"
                + "Đường thả quân: chọn 2 điểm ở vùng trống ngoài làng địch, quân sẽ được rải đều giữa 2 điểm.",
                14, 0xFF333333));
        col.addView(button("Mở Clash of Clans", () -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(COC_PACKAGE);
            if (i == null) {
                Toast.makeText(this, "Chưa cài Clash of Clans", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(i);
            }
        }));

        col.addView(text("Bước 3 – Chỉnh thông số", 19, 0xFF1565C0));
        for (int i = 0; i < Config.ALL.length; i++) {
            Config.Field f = Config.ALL[i];
            col.addView(text(f.label + "  (" + f.min + "–" + f.max + ")", 14, 0xFF333333));
            EditText e = new EditText(this);
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
            e.setText(String.valueOf(Config.get(this, f)));
            inputs[i] = e;
            col.addView(e);
        }
        col.addView(button("Lưu thông số", this::save));

        col.addView(text("Bước 4 – Chạy", 19, 0xFF1565C0));
        col.addView(text("Trong game bấm ▶ trên bảng nổi để bắt đầu, ■ để dừng, _ để thu gọn bảng. "
                + "Kéo ☰ để đưa bảng ra chỗ không che nút nào.\n\n"
                + "Lưu ý: Supercell cấm dùng bot trong Clash of Clans. Tài khoản có thể bị khóa. "
                + "Nên thử trên tài khoản phụ.", 14, 0xFF333333));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        boolean on = FarmService.isRunning();
        String missing = Points.missingRequired(this);
        StringBuilder sb = new StringBuilder();
        sb.append(on ? "✅ Trợ năng đã bật – bảng nổi đang hiện" : "❌ Chưa bật Trợ năng cho AutoFarm");
        sb.append("\n📍 Đã cài ").append(Points.countSet(this)).append("/").append(Points.ALL.length).append(" vị trí");
        if (!missing.isEmpty()) sb.append("\n⚠ Còn thiếu: ").append(missing);
        statusView.setText(sb.toString());
    }

    private void save() {
        for (int i = 0; i < Config.ALL.length; i++) {
            Config.Field f = Config.ALL[i];
            int v;
            try {
                v = Integer.parseInt(inputs[i].getText().toString().trim());
            } catch (NumberFormatException e) {
                v = f.def;
            }
            Config.set(this, f, v);
            inputs[i].setText(String.valueOf(Config.get(this, f)));
        }
        Toast.makeText(this, "Đã lưu. Áp dụng từ lần bấm ▶ tiếp theo.", Toast.LENGTH_SHORT).show();
    }
}
