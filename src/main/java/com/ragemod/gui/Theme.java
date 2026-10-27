package com.ragemod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** RAGE look: black / dark red with red accents. All colors are ARGB. */
public final class Theme {
	public static final int DIM = 0xB0070103;
	public static final int PANEL = 0xF0140709;
	public static final int PANEL_EDGE = 0xFF5E1117;
	public static final int ROW = 0xFF1F0B0E;
	public static final int ROW_ALT = 0xFF240D11;
	public static final int ROW_HOVER = 0xFF33131A;
	public static final int ROW_SEL = 0xFF55151D;
	public static final int BTN = 0xFF2A0E12;
	public static final int BTN_HOVER = 0xFF4A151D;
	public static final int BTN_EDGE = 0xFF7C1B24;
	public static final int ACCENT = 0xFFFF3B3B;
	public static final int ACCENT_DARK = 0xFFB01E26;
	public static final int TEXT = 0xFFF4E8E8;
	public static final int TEXT_DIM = 0xFFA88789;
	public static final int GOOD = 0xFF55FF55;
	public static final int BAD = 0xFFFF5555;

	private Theme() {}

	public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + h, color);
	}

	public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	public static void panel(GuiGraphics g, int x, int y, int w, int h) {
		rect(g, x, y, w, h, PANEL);
		border(g, x, y, w, h, PANEL_EDGE);
	}

	public static void circle(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int dx = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
			g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
		}
	}

	public static void text(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(Minecraft.getInstance().font, s, x, y, color);
	}

	public static void textCentered(GuiGraphics g, String s, int cx, int y, int color) {
		g.drawCenteredString(Minecraft.getInstance().font, s, cx, y, color);
	}

	public static void textRight(GuiGraphics g, String s, int rightX, int y, int color) {
		Minecraft mc = Minecraft.getInstance();
		g.drawString(mc.font, s, rightX - mc.font.width(s), y, color);
	}

	public static int width(String s) {
		return Minecraft.getInstance().font.width(s);
	}

	/** Cuts a string with "..." so it fits in maxW pixels. */
	public static String fit(String s, int maxW) {
		if (width(s) <= maxW) return s;
		String t = s;
		while (t.length() > 1 && width(t + "...") > maxW) t = t.substring(0, t.length() - 1);
		return t + "...";
	}

	/** Small red cross icon (used for war / enemies). */
	public static void warIcon(GuiGraphics g, int x, int y, int size, int color) {
		int t = Math.max(2, size / 3);
		int o = (size - t) / 2;
		g.fill(x + o, y, x + o + t, y + size, color);
		g.fill(x, y + o, x + size, y + o + t, color);
	}
}
