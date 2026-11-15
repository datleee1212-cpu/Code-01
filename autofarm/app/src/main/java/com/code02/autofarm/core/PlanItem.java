package com.code02.autofarm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Công trình chi tiết dùng cho lộ trình TH16 → TH17 (lớp nhận biết "mịn" nằm trên Roadmap.Cat).
 * Nhận từ tên tiếng Anh hoặc tiếng Việt (không phân biệt dấu, chữ hoa/thường), chịu được
 * lỗi đọc chữ nhẹ (thiếu/sai 1 ký tự, dính chữ).
 * Tên tiếng Việt trong game chưa chắc chắn → ghi nhiều cách gọi có thể gặp.
 */
public enum PlanItem {
    // ---- công trình tấn công
    LAB("Phòng thí nghiệm", Roadmap.Cat.LAB, false,
            "laboratory", "lab", "phong thi nghiem", "thi nghiem", "phong nghien cuu"),
    ARMY_CAMP("Trại quân", Roadmap.Cat.ARMY_CAMP, false,
            "army camp", "trai quan", "trai linh", "doanh trai"),
    CLAN_CASTLE("Lâu đài bang hội", Roadmap.Cat.CLAN_CASTLE, false,
            "clan castle", "lau dai bang hoi", "lau dai", "thanh bang hoi", "lau dai clan"),
    HERO_HALL("Sảnh anh hùng", Roadmap.Cat.ARMY_BUILDING, false,
            "hero hall", "sanh anh hung", "dai anh hung", "sanh tuong", "nha anh hung", "lau anh hung",
            "dien anh hung"),
    BLACKSMITH("Lò rèn", Roadmap.Cat.ARMY_BUILDING, false,
            "blacksmith", "lo ren", "tho ren", "xuong ren"),
    BARRACKS("Nhà lính", Roadmap.Cat.ARMY_BUILDING, false,
            "barracks", "barrack", "nha linh", "trai huan luyen"),
    DARK_BARRACKS("Nhà lính đen", Roadmap.Cat.ARMY_BUILDING, false,
            "dark barracks", "dark barrack", "nha linh den", "nha linh bong toi", "doanh trai den"),
    SPELL_FACTORY("Xưởng phép", Roadmap.Cat.ARMY_BUILDING, false,
            "spell factory", "xuong phep", "nha may phep", "xuong che phep"),
    DARK_SPELL_FACTORY("Xưởng phép đen", Roadmap.Cat.ARMY_BUILDING, false,
            "dark spell factory", "xuong phep den", "xuong phep bong toi", "nha may phep den"),
    WORKSHOP("Xưởng công thành", Roadmap.Cat.ARMY_BUILDING, false,
            "workshop", "siege workshop", "xuong", "xuong cong thanh", "xuong co khi"),
    PET_HOUSE("Nhà thú cưng", Roadmap.Cat.ARMY_BUILDING, false,
            "pet house", "nha thu cung", "nha thu nuoi", "chuong thu cung"),
    HELPER_HUT("Nhà trợ thủ", Roadmap.Cat.UNKNOWN, false,
            "helper hut", "nha tro thu", "nha tro ly", "leu tro thu", "choi tro ly"),

    // ---- tướng
    ARCHER_QUEEN("Nữ hoàng cung thủ", Roadmap.Cat.HERO, true,
            "archer queen", "queen", "nu hoang cung thu", "nu hoang"),
    GRAND_WARDEN("Cận vệ (Grand Warden)", Roadmap.Cat.HERO, true,
            "grand warden", "warden", "hiep si", "can ve", "can phong", "giam ho", "nguoi giam ho",
            "dai giam ho", "hiep si vi dai"),
    ROYAL_CHAMPION("Nhà vô địch hoàng gia", Roadmap.Cat.HERO, true,
            "royal champion", "champion", "nha vo dich", "nu vo dich", "vo dich", "nha vo dich hoang gia",
            "chien binh hoang gia"),
    BARBARIAN_KING("Vua man rợ", Roadmap.Cat.HERO, true,
            "barbarian king", "vua man ro", "vua"),
    DRAGON_DUKE("Công tước rồng", Roadmap.Cat.HERO, true,
            "dragon duke", "duke", "cong tuoc rong", "cong tuoc"),
    MINION_PRINCE("Hoàng tử Minion", Roadmap.Cat.HERO, true,
            "minion prince", "prince", "hoang tu", "hoang tu minion", "hoang tu tieu quy"),

    // ---- phòng thủ
    EAGLE_ARTILLERY("Pháo đại bàng", Roadmap.Cat.SPLASH_DEF, false,
            "eagle artillery", "eagle", "phao dai bang", "dai bang", "phao binh dai bang"),
    MONOLITH("Nguyên khối (Monolith)", Roadmap.Cat.SPLASH_DEF, false,
            "monolith", "nguyen khoi", "khoi nguyen khoi", "khoi da", "da nguyen khoi"),
    SPELL_TOWER("Tháp phép", Roadmap.Cat.SPLASH_DEF, false,
            "spell tower", "thap phep", "thap phep thuat"),
    MULTI_GEAR_TOWER("Tháp đa năng (Multi-Gear)", Roadmap.Cat.SPLASH_DEF, false,
            "multi gear tower", "multi gear", "multigear tower", "multigear", "thap da nang",
            "thap banh rang", "thap nhieu banh rang", "thap da che do"),
    SCATTERSHOT("Súng tán xạ (Scattershot)", Roadmap.Cat.SPLASH_DEF, false,
            "scattershot", "scatter shot", "scatter", "sung tan xa", "phao tan xa", "sung phan tan", "tan xa"),
    INFERNO_TOWER("Tháp lửa (Inferno)", Roadmap.Cat.AIR_DEF, false,
            "inferno tower", "inferno", "thap lua", "thap hoa nguc", "thap dia nguc"),
    X_BOW("Nỏ lớn (X-Bow)", Roadmap.Cat.AIR_DEF, false,
            "x bow", "xbow", "no lon", "no than", "no khong lo"),
    FIRESPITTER("Súng phun lửa (Firespitter)", Roadmap.Cat.SPLASH_DEF, false,
            "firespitter", "fire spitter", "sung phun lua", "phun lua", "thap phun lua", "rong phun lua",
            "may phun lua"),
    INFERNO_ARTILLERY("Pháo hỏa ngục (vũ khí Nhà chính)", Roadmap.Cat.SPLASH_DEF, false,
            "inferno artillery", "town hall weapon", "th weapon", "phao hoa nguc", "phao dia nguc",
            "phao binh hoa nguc", "vu khi nha chinh", "vu khi thi chinh"),
    CRAFTING_STATION("Phòng thủ chế tạo", Roadmap.Cat.POINT_DEF, false,
            "crafting station", "crafted defense", "crafted defence", "tram che tao", "phong thu che tao",
            "xuong che tao", "che tao"),
    WIZARD_TOWER("Tháp phù thủy", Roadmap.Cat.SPLASH_DEF, false,
            "wizard tower", "thap phu thuy", "thap phap su"),
    BOMB_TOWER("Tháp bom", Roadmap.Cat.SPLASH_DEF, false,
            "bomb tower", "bomber tower", "thap bom", "thap nem bom"),
    AIR_DEFENSE("Phòng không", Roadmap.Cat.AIR_DEF, false,
            "air defense", "air defence", "phong khong", "thap phong khong"),
    MULTI_ARCHER_TOWER("Tháp đa cung thủ", Roadmap.Cat.POINT_DEF, false,
            "multi archer tower", "multi archer", "multiarcher", "thap da cung thu", "thap nhieu cung thu",
            "thap cung thu kep"),
    RICOCHET_CANNON("Đại bác nảy (Ricochet)", Roadmap.Cat.POINT_DEF, false,
            "ricochet cannon", "ricochet", "dai bac nay", "dai bac bat nay", "dai bac doi", "dai bac kep"),
    ARCHER_TOWER("Tháp cung thủ", Roadmap.Cat.POINT_DEF, false,
            "archer tower", "thap cung", "thap cung thu"),
    CANNON("Đại bác", Roadmap.Cat.POINT_DEF, false,
            "cannon", "dai bac"),
    MORTAR("Súng cối", Roadmap.Cat.SPLASH_DEF, false,
            "mortar", "sung coi", "phao coi"),
    AIR_SWEEPER("Máy thổi khí", Roadmap.Cat.AIR_DEF, false,
            "air sweeper", "may thoi khi", "may thoi", "quat gio"),
    HIDDEN_TESLA("Tesla ẩn", Roadmap.Cat.AIR_DEF, false,
            "hidden tesla", "tesla", "thap tesla", "tesla an"),
    BUILDERS_HUT("Nhà thợ xây", Roadmap.Cat.POINT_DEF, false,
            "builder s hut", "builders hut", "builder hut", "nha tho xay", "chong tho xay", "leu tho xay",
            "choi tho xay"),

    // ---- bẫy
    GIGA_BOMB("Bom Giga", Roadmap.Cat.TRAP, false,
            "giga bomb", "gigabomb", "bom giga", "sieu bom"),
    AIR_SEEKING_MINE("Mìn đuổi máy bay", Roadmap.Cat.TRAP, false,
            "seeking air mine", "air seeking mine", "air mine", "min tim may bay", "min duoi may bay",
            "min truy duoi", "min phong khong", "min tren khong", "min khong quan", "min"),
    AIR_BOMB("Bom trên không", Roadmap.Cat.TRAP, false,
            "air bomb", "bom tren khong", "bom khong quan", "bom phong khong", "bom bay"),
    GIANT_BOMB("Bom lớn", Roadmap.Cat.TRAP, false,
            "giant bomb", "bom lon", "bom khong lo"),
    BOMB("Bom", Roadmap.Cat.TRAP, false,
            "bomb", "bom"),
    SPRING_TRAP("Bẫy lò xo", Roadmap.Cat.TRAP, false,
            "spring trap", "bay lo xo"),
    SKELETON_TRAP("Bẫy xương", Roadmap.Cat.TRAP, false,
            "skeleton trap", "bay xuong", "bay bo xuong"),
    TORNADO_TRAP("Bẫy lốc xoáy", Roadmap.Cat.TRAP, false,
            "tornado trap", "bay loc xoay", "bay loc"),
    OTHER_TRAP("Bẫy khác", Roadmap.Cat.TRAP, false,
            "trap", "bay"),

    // ---- kinh tế
    STORAGE("Kho", Roadmap.Cat.STORAGE, false,
            "storage", "gold storage", "elixir storage", "dark elixir storage", "kho", "kho vang",
            "kho tien duoc", "kho tien duoc den"),
    COLLECTOR("Mỏ / bơm / khoan", Roadmap.Cat.COLLECTOR, false,
            "gold mine", "mo vang", "elixir collector", "collector", "bom tien duoc", "may bom tien duoc",
            "dark elixir drill", "drill", "khoan", "khoan tien duoc den", "may khoan"),

    WALL("Tường", Roadmap.Cat.WALL, false,
            "wall", "walls", "tuong", "tuong thanh", "doan tuong"),
    TOWN_HALL("Nhà chính", Roadmap.Cat.TOWN_HALL, false,
            "town hall", "nha chinh", "toa thi chinh", "thi chinh", "toa nha chinh"),

    // ---- nhóm chung (khi chỉ biết loại thô Roadmap.Cat)
    GENERIC_ARMY("Công trình quân khác", Roadmap.Cat.ARMY_BUILDING, false),
    HERO_OTHER("Tướng khác", Roadmap.Cat.HERO, true),
    GENERIC_SPLASH("Phòng thủ diện rộng khác", Roadmap.Cat.SPLASH_DEF, false),
    GENERIC_AIR("Phòng không / thủ mạnh khác", Roadmap.Cat.AIR_DEF, false),
    GENERIC_POINT("Phòng thủ đơn khác", Roadmap.Cat.POINT_DEF, false),
    UNKNOWN("Không rõ", Roadmap.Cat.UNKNOWN, false);

    /** Tên hiển thị tiếng Việt. */
    public final String vi;
    /** Loại thô tương ứng (dùng cho các lộ trình cũ). */
    public final Roadmap.Cat cat;
    /** Là tướng (tính vào giới hạn số tướng nâng cùng lúc). */
    public final boolean hero;
    private final String[] phrases;

    PlanItem(String vi, Roadmap.Cat cat, boolean hero, String... phrases) {
        this.vi = vi;
        this.cat = cat;
        this.hero = hero;
        this.phrases = phrases;
    }

    private static final class Kw {
        final String phrase;
        final String joined;
        final PlanItem item;

        Kw(String phrase, PlanItem item) {
            this.phrase = phrase;
            this.joined = phrase.replace(" ", "");
            this.item = item;
        }
    }

    private static final List<Kw> KEYWORDS = new ArrayList<>();

    static {
        for (PlanItem it : values()) for (String p : it.phrases) KEYWORDS.add(new Kw(p, it));
        // Cụm dài khớp trước: "inferno artillery" trước "inferno", "multi archer tower" trước "archer tower",
        // "giga bomb" trước "bomb", "bom tien duoc" (bơm) trước "bom".
        Collections.sort(KEYWORDS, (a, b) -> b.phrase.length() - a.phrase.length());
    }

    /** Nhận công trình từ tên (tiếng Anh / tiếng Việt). UNKNOWN nếu không nhận ra. */
    public static PlanItem of(String name) {
        String n = Roadmap.normalize(name);
        if (n.trim().isEmpty()) return UNKNOWN;
        // 1) Khớp nguyên cụm từ.
        for (Kw k : KEYWORDS) {
            if (n.contains(" " + k.phrase + " ")) return k.item;
        }
        // 2) Chữ bị dính (vd. "ArcherQueen", "Multi-GearTower"): so khi bỏ hết dấu cách, chỉ với cụm dài.
        String joined = n.replace(" ", "");
        for (Kw k : KEYWORDS) {
            if (k.joined.length() >= 8 && joined.contains(k.joined)) return k.item;
        }
        // 3) Đọc sai nhẹ (vd. "Arcer Queen", "Mortor"): sai tối đa 1 ký tự (2 ký tự với cụm ≥ 11 ký tự).
        String[] words = n.trim().split(" ");
        PlanItem best = UNKNOWN;
        int bestLen = 0;
        for (Kw k : KEYWORDS) {
            if (k.phrase.length() < 6 || k.phrase.length() <= bestLen) continue;
            int kWords = k.phrase.split(" ").length;
            int maxDist = k.phrase.length() >= 11 ? 2 : 1;
            for (int i = 0; i + kWords <= words.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < i + kWords; j++) {
                    if (j > i) sb.append(' ');
                    sb.append(words[j]);
                }
                if (distance(sb.toString(), k.phrase, maxDist) <= maxDist) {
                    best = k.item;
                    bestLen = k.phrase.length();
                    break;
                }
            }
        }
        return best;
    }

    /** Công trình chung theo loại thô (khi tên không nhận ra nhưng đã biết loại). */
    public static PlanItem fromCat(Roadmap.Cat c) {
        if (c == null) return UNKNOWN;
        switch (c) {
            case LAB:
                return LAB;
            case CLAN_CASTLE:
                return CLAN_CASTLE;
            case ARMY_CAMP:
                return ARMY_CAMP;
            case ARMY_BUILDING:
                return GENERIC_ARMY;
            case HERO:
                return HERO_OTHER;
            case STORAGE:
                return STORAGE;
            case SPLASH_DEF:
                return GENERIC_SPLASH;
            case AIR_DEF:
                return GENERIC_AIR;
            case POINT_DEF:
                return GENERIC_POINT;
            case COLLECTOR:
                return COLLECTOR;
            case TRAP:
                return OTHER_TRAP;
            case WALL:
                return WALL;
            case TOWN_HALL:
                return TOWN_HALL;
            default:
                return UNKNOWN;
        }
    }

    /** Công trình của một dòng danh sách thợ xây: theo tên, nếu không rõ thì theo loại thô. */
    public static PlanItem of(Roadmap.Row r) {
        if (r == null) return UNKNOWN;
        if (r.cat == Roadmap.Cat.WALL) return WALL;
        PlanItem it = of(r.name);
        return it == UNKNOWN ? fromCat(r.cat) : it;
    }

    /** Khoảng cách Levenshtein, dừng sớm khi chắc chắn vượt ngưỡng. */
    private static int distance(String a, String b, int max) {
        if (Math.abs(a.length() - b.length()) > max) return max + 1;
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            int rowMin = cur[0];
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                rowMin = Math.min(rowMin, cur[j]);
            }
            if (rowMin > max) return max + 1;
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
