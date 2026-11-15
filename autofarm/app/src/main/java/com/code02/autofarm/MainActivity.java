package com.code02.autofarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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

/** Màn hình chính: cấp quyền, mở game, chỉnh thông số. */
public class MainActivity extends Activity {

    private static final String COC_PACKAGE = "com.supercell.clashofclans";

    private TextView statusView;
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

        col.addView(text("AutoFarm 0.2", 26, Color.BLACK));
        col.addView(text("Bot đọc màn hình, tự xử lý tình huống và tự nâng cấp. Chạy ngay trên máy tính bảng, "
                + "không cần root, không cần máy tính.", 15, 0xFF444444));

        statusView = text("", 15, Color.BLACK);
        col.addView(statusView);

        col.addView(text("Bước 1 – Bật quyền", 19, 0xFF1565C0));
        col.addView(text("Cài đặt → Trợ năng → Ứng dụng đã tải xuống → AutoFarm → Bật.\n"
                + "Android 13+: nếu nút bị mờ, vào Thông tin ứng dụng → ⋮ → \"Cho phép cài đặt bị hạn chế\".\n"
                + "Vừa cập nhật app từ bản cũ: tắt rồi bật lại Trợ năng để có quyền chụp màn hình.",
                14, 0xFF333333));
        col.addView(button("Mở cài đặt Trợ năng", () ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        col.addView(button("Mở thông tin ứng dụng", () -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }));

        col.addView(text("Bước 2 – Dạy bot", 19, 0xFF1565C0));
        col.addView(text("Vào game, bấm 📍 trên bảng nổi, chọn nhóm rồi chạm vào từng thứ theo hướng dẫn. "
                + "Bot chụp lại hình quanh chỗ bạn chạm để sau này tự tìm trên màn hình.\n\n"
                + "• Đánh trận: Tấn công, Tìm trận, Tiếp theo, 2 vùng số tài nguyên, ô quân, đường thả quân, "
                + "Kết thúc trận, Về nhà.\n"
                + "• Thu tài nguyên: biểu tượng vàng/tiên dược nổi trên mỏ (dạy lúc mỏ đang đầy).\n"
                + "• Tự nâng cấp: biểu tượng thợ xây, dòng 1–2 trong danh sách, nút Nâng cấp, nút giá xác nhận.\n"
                + "• Đóng thông báo: mỗi khi gặp thông báo lạ, dạy nút X / OK / Thử lại của nó. "
                + "Tuyệt đối không dạy nút mua bằng ngọc.\n\n"
                + "Sau đó bấm 🔍 để kiểm tra bot nhìn thấy gì trên màn hình hiện tại.",
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
            if (f.bool) {
                CheckBox cb = new CheckBox(this);
                cb.setText(f.label);
                cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                cb.setChecked(Config.on(this, f));
                inputs[i] = cb;
                col.addView(cb);
            } else {
                col.addView(text(f.label + "  (" + f.min + "–" + f.max + ")", 14, 0xFF333333));
                EditText e = new EditText(this);
                e.setInputType(InputType.TYPE_CLASS_NUMBER);
                e.setText(String.valueOf(Config.get(this, f)));
                inputs[i] = e;
                col.addView(e);
            }
        }
        col.addView(button("Lưu thông số", this::save));

        col.addView(text("Bước 4 – Chạy", 19, 0xFF1565C0));
        col.addView(text("Ở làng chính bấm ▶. Bot tự: về làng nếu bị lạc → đóng thông báo → thu tài nguyên → "
                + "nâng cấp (nếu bật) → luyện quân → tìm trận, bỏ qua làng nghèo → thả quân → chờ trận xong → về nhà.\n"
                + "■ dừng, _ thu gọn, kéo ☰ để đưa bảng ra chỗ không che nút.\n\n"
                + "⚠ Supercell cấm dùng bot trong Clash of Clans, tài khoản có thể bị khóa. "
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
        StringBuilder sb = new StringBuilder();
        sb.append(FarmService.isRunning() ? "✅ Trợ năng đã bật – bảng nổi đang hiện" : "❌ Chưa bật Trợ năng cho AutoFarm");
        sb.append(FarmService.visionSupported()
                ? "\n✅ Đọc màn hình: được hỗ trợ"
                : "\n⚠ Android dưới 11: không đọc được màn hình, chỉ chạm theo tọa độ");
        for (String g : Points.GROUPS) {
            sb.append("\n📍 ").append(g).append(": ").append(Points.countSet(this, g))
                    .append("/").append(Points.countGroup(g));
        }
        String missing = Points.missingRequired(this);
        if (!missing.isEmpty()) sb.append("\n⚠ Còn thiếu: ").append(missing);
        statusView.setText(sb.toString());
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
        Toast.makeText(this, "Đã lưu. Áp dụng từ lần bấm ▶ tiếp theo.", Toast.LENGTH_SHORT).show();
    }
}
