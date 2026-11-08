package net.glacier.ui;

/** Central colour and radius tokens. Every widget style resolves through here. */
public final class Theme {
	private Theme() {
	}

	public static final int ACCENT = 0xFF7289DA;
	public static final int ACCENT_HOVER = 0xFF8EA0E0;
	public static final int ACCENT_BG = 0x297289DA;
	public static final int BG = 0xFF23272A;
	public static final int PANEL = 0xFF222326;
	public static final int CARD = 0xFF2C2F33;
	public static final int CARD_BORDER = 0xFF393C41;
	public static final int INSET = 0xFF1F2227;
	public static final int TRACK_OFF = 0xFF383B40;
	public static final int ITEM = 0x0AFFFFFF;
	public static final int ITEM_HOVER = 0x13FFFFFF;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int DIM = 0xFF99AAB5;
	public static final int GREEN = 0xFF43B581;
	public static final int RED = 0xFFF04747;
	public static final int ORANGE = 0xFFFAA61A;
	public static final int BORDER = 0x1AFFFFFF;

	/** HUD tokens: vanilla-style flat black box, status colours and key caps. */
	public static final int HUD_BOX = 0xFF000000;
	public static final int HUD_GOOD = 0xFF55FF55;
	public static final int HUD_WARN = 0xFFFFFF55;
	public static final int HUD_BAD = 0xFFFF5555;
	public static final int HUD_BIOME = 0xFF8FE08F;
	public static final int HUD_MUTED = 0xFFAAAAAA;
	public static final int HUD_TICK = 0x99FFFFFF;
	public static final int HUD_KEY_IDLE = 0x59000000;
	public static final int HUD_KEY_PRESSED = 0x8CFFFFFF;
	public static final int HUD_INK = 0xFF000000;

	/** Glacier HUD design tokens (used when the Elements "Glacier Design" toggle is on). */
	public static final int GLACIER_PANEL = 0xFF0D0E12;
	public static final int GLACIER_EDGE = 0x14FFFFFF;
	public static final int GLACIER_KEY_IDLE = 0x8C0B0F18;
	public static final int GLACIER_KEY_PRESSED = 0xE6FFFFFF;

	public static final int R_SM = 6;
	public static final int R_MD = 9;
	public static final int R_PILL = 999;
}
