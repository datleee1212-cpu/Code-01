package sim;

import com.code02.autofarm.core.Img;
import com.code02.autofarm.core.Vision;

import java.util.List;
import java.util.Random;

/**
 * Kiểm thử nhận diện ở độ phân giải Galaxy Tab S10+ (2800×1752, 16:10):
 * đúng vị trí, không nhận nhầm, và đủ nhanh.
 */
public final class TabS10Tests {

    static final int W = 2800;
    static final int H = 1752;
    private static int failures = 0;

    private static void check(boolean ok, String what) {
        System.out.println((ok ? "   ✓ " : "   ✗ ") + what);
        if (!ok) failures++;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Nền giống địa hình game + nhiễu, độ sáng lệch theo từng khung hình. */
    static Img screen(long seed, int[][] buttons, int size) {
        Random r = new Random(seed);
        int shift = r.nextInt(11) - 5;
        int[] px = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int g = 90 + (int) (40 * Math.sin(x / 90.0) * Math.cos(y / 125.0)) + ((x / 350 + y / 350) % 2) * 15
                        + shift + r.nextInt(9) - 4;
                px[y * W + x] = 0xFF000000 | (clamp(g - 20) << 16) | (clamp(g + 25) << 8) | clamp(g - 35);
            }
        }
        Img img = new Img(W, H, px);
        for (int[] b : buttons) drawButton(img, b[0], b[1], size, b[2]);
        return img;
    }

    static void drawButton(Img img, int cx, int cy, int size, int id) {
        Random pr = new Random(id * 7919L);
        int cells = 6;
        int cw = size / cells;
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                int c = 0xFF000000 | (pr.nextInt(256) << 16) | (pr.nextInt(256) << 8) | pr.nextInt(256);
                for (int y = 0; y < cw; y++) {
                    for (int x = 0; x < cw; x++) {
                        int px = cx - size / 2 + i * cw + x;
                        int py = cy - size / 2 + j * cw + y;
                        if (px >= 0 && py >= 0 && px < W && py < H) img.px[py * W + px] = c;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=============== KIỂM THỬ GALAXY TAB S10+ (2800×1752) ===============");
        int btn = 180; // nút trong game ở màn hình này cỡ ~170–200 điểm ảnh
        int[][] buttons = {{200, 1580, 1}, {2600, 1380, 2}, {1400, 100, 3}, {1400, 876, 4}, {700, 1200, 5}};

        // Dạy: cắt mẫu 8% cạnh ngắn (≈140 px) từ một khung hình
        Img teach = screen(1, buttons, btn);
        int side = H * 8 / 100;
        Vision.Tpl[] tpl = new Vision.Tpl[buttons.length];
        for (int i = 0; i < buttons.length; i++) tpl[i] = new Vision.Tpl(teach.cropSquare(buttons[i][0], buttons[i][1], side));

        // Khung hình khác (nhiễu + độ sáng khác) để tìm
        Img shot = screen(2, buttons, btn);
        long t0 = System.nanoTime();
        Vision.Frame f = new Vision.Frame(shot);
        long tFrame = System.nanoTime() - t0;

        int ok = 0;
        long tFind = 0;
        for (int i = 0; i < buttons.length; i++) {
            long a = System.nanoTime();
            Vision.Match m = Vision.find(f, tpl[i], 0, 0, W, H, 0.82f);
            tFind += System.nanoTime() - a;
            boolean hit = m != null && Math.abs(m.x - buttons[i][0]) <= 3 && Math.abs(m.y - buttons[i][1]) <= 3;
            if (hit) ok++;
            else System.out.println("      nút " + (i + 1) + " → " + (m == null ? "không thấy" : m.x + "," + m.y + " (" + m.score + ")"));
        }
        check(ok == buttons.length, "tìm đúng vị trí (±3 px) " + ok + "/" + buttons.length + " nút trên cả màn hình");

        double perFind = tFind / 1e6 / buttons.length;
        System.out.printf("   chuẩn bị khung hình: %.0f ms | tìm 1 mẫu trên cả màn hình: %.0f ms%n", tFrame / 1e6, perFind);
        // Máy tính bảng chậm hơn máy chủ khoảng 3–5 lần: cần ≤ 60 ms ở đây để ≤ ~300 ms trên máy.
        check(perFind <= 60, "tìm 1 mẫu đủ nhanh (≤ 60 ms trên máy chủ)");

        // Nút không có trên màn hình → không được nhận nhầm
        Img empty = screen(3, new int[][]{{200, 1580, 1}}, btn);
        Vision.Frame fe = new Vision.Frame(empty);
        int fp = 0;
        for (int i = 1; i < buttons.length; i++) if (Vision.find(fe, tpl[i], 0, 0, W, H, 0.82f) != null) fp++;
        check(fp == 0, "không nhận nhầm khi nút không có trên màn hình (" + fp + " lần nhầm)");

        // Tìm nhiều (biểu tượng thu tài nguyên): 4 cái giống nhau
        int[][] bubbles = {{500, 500, 9}, {1200, 700, 9}, {1900, 1000, 9}, {2300, 400, 9}};
        Img col = screen(4, bubbles, 120);
        Vision.Tpl bt = new Vision.Tpl(screen(5, bubbles, 120).cropSquare(500, 500, 100));
        List<Vision.Match> all = Vision.findAll(new Vision.Frame(col), bt, 0, 0, W, H, 0.82f, 15);
        check(all.size() == 4, "tìm đủ 4 biểu tượng thu tài nguyên giống nhau (" + all.size() + ")");

        // Mẫu nhỏ (cỡ 3%) vẫn dùng đường cũ và vẫn đúng
        Vision.Tpl small = new Vision.Tpl(teach.cropSquare(1400, 876, H * 3 / 100));
        Vision.Match ms = Vision.find(f, small, 0, 0, W, H, 0.82f);
        check(ms != null && Math.abs(ms.x - 1400) <= 3 && Math.abs(ms.y - 876) <= 3, "mẫu nhỏ (3%) vẫn tìm đúng");

        // Tìm quanh chỗ đã dạy (nút cố định) phải rất nhanh
        long a = System.nanoTime();
        for (int k = 0; k < 10; k++) Vision.find(f, tpl[0], 200 - 300, 1580 - 300, 200 + 300, 1580 + 300, 0.82f);
        double near = (System.nanoTime() - a) / 1e6 / 10;
        System.out.printf("   tìm quanh chỗ đã dạy: %.1f ms%n", near);
        check(near <= 15, "tìm quanh chỗ đã dạy ≤ 15 ms");

        System.out.println(failures == 0 ? "KẾT QUẢ: TẤT CẢ ĐẠT ✓" : "KẾT QUẢ: " + failures + " MỤC KHÔNG ĐẠT ✗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
