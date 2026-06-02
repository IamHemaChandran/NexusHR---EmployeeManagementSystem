package employee.management;
import java.awt.*;

public class Theme {
    public static boolean isDark = true;

    // Dynamic getters — always use these, never static fields directly
    public static Color BG_DEEP()   { return isDark ? new Color(0x0f,0x11,0x17) : new Color(0xf1,0xf5,0xf9); }
    public static Color BG_PANEL()  { return isDark ? new Color(0x16,0x1b,0x27) : new Color(0xff,0xff,0xff); }
    public static Color BG_CARD()   { return isDark ? new Color(0x1e,0x25,0x35) : new Color(0xe8,0xf0,0xfe); }
    public static Color BG_INPUT()  { return isDark ? new Color(0x0f,0x11,0x17) : new Color(0xf8,0xfa,0xff); }
    public static Color BG_HOVER()  { return isDark ? new Color(0x23,0x2d,0x42) : new Color(0xe2,0xea,0xfa); }
    public static Color BORDER()    { return isDark ? new Color(0x2a,0x2f,0x3d) : new Color(0xcc,0xd9,0xee); }
    public static Color TEXT_PRI()  { return isDark ? new Color(0xe2,0xe8,0xf0) : new Color(0x0f,0x17,0x2a); }
    public static Color TEXT_SEC()  { return isDark ? new Color(0x94,0xa3,0xb8) : new Color(0x33,0x4d,0x6b); }
    public static Color TEXT_MUT()  { return isDark ? new Color(0x64,0x74,0x8b) : new Color(0x88,0x99,0xbb); }
    public static Color SIDEBAR_ACTIVE(){ return isDark ? new Color(0x1e,0x25,0x35) : new Color(0xd6,0xe4,0xff); }

    // Static accents stay the same in both modes
    public static final Color ACCENT_BLU = new Color(0x7c,0x9e,0xf5);
    public static final Color ACCENT_GRN = new Color(0x4a,0xde,0x80);
    public static final Color ACCENT_RED = new Color(0xf8,0x71,0x71);
    public static final Color ACCENT_YLW = new Color(0xfb,0xbf,0x24);
    public static final Color ACCENT_PUR = new Color(0xa7,0x8b,0xfa);
    public static final Color BTN_BLU_BG = new Color(0x1e,0x3a,0x5f);
    public static final Color BTN_RED_BG = new Color(0x2a,0x0e,0x0e);
    public static final Color BTN_GRN_BG = new Color(0x0a,0x1f,0x18);

    public static final Color[][] AVATAR_COLORS = {
        {new Color(0x1e,0x1a,0x3d), new Color(0xaf,0xa9,0xec)},
        {new Color(0x2a,0x10,0x20), new Color(0xed,0x93,0xb1)},
        {new Color(0x0a,0x1f,0x18), new Color(0x5d,0xca,0xa5)},
        {new Color(0x1a,0x25,0x35), new Color(0x7c,0xb9,0xf5)},
        {new Color(0x1f,0x1a,0x10), new Color(0xfb,0xbf,0x24)},
    };

    public static final Font FONT_LARGE = new Font("Segoe UI",Font.BOLD,18);
    public static final Font FONT_MED   = new Font("Segoe UI",Font.PLAIN,13);
    public static final Font FONT_SMALL = new Font("Segoe UI",Font.PLAIN,11);
    public static final Font FONT_MONO  = new Font("Consolas",Font.PLAIN,12);
    public static final Font FONT_TITLE = new Font("Segoe UI",Font.BOLD,14);
    public static final Font FONT_STAT  = new Font("Segoe UI",Font.BOLD,22);

    // Legacy compat — many files reference Theme.BG_DEEP etc as fields
    public static Color BG_DEEP    = BG_DEEP();
    public static Color BG_PANEL   = BG_PANEL();
    public static Color BG_CARD    = BG_CARD();
    public static Color BG_INPUT   = BG_INPUT();
    public static Color BG_HOVER   = BG_HOVER();
    public static Color BORDER     = BORDER();
    public static Color TEXT_PRI   = TEXT_PRI();
    public static Color TEXT_SEC   = TEXT_SEC();
    public static Color TEXT_MUT   = TEXT_MUT();
    public static Color SIDEBAR_ACTIVE = SIDEBAR_ACTIVE();

    /** Call this after toggling isDark to refresh static fields */
    public static void refresh() {
        BG_DEEP    = BG_DEEP();
        BG_PANEL   = BG_PANEL();
        BG_CARD    = BG_CARD();
        BG_INPUT   = BG_INPUT();
        BG_HOVER   = BG_HOVER();
        BORDER     = BORDER();
        TEXT_PRI   = TEXT_PRI();
        TEXT_SEC   = TEXT_SEC();
        TEXT_MUT   = TEXT_MUT();
        SIDEBAR_ACTIVE = SIDEBAR_ACTIVE();
    }
}
