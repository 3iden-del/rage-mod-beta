package com.ragemod.gui;

import com.ragemod.core.WhitelistGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** "RAGEMod - Open a module" menu. */
public class MainMenuScreen extends RageScreen {
	private int px;
	private int py;
	private int pw;
	private int ph;

	public MainMenuScreen() {
		super("RAGE Mod", null);
	}

	@Override
	protected void init() {
		btns.clear();
		pw = 300;
		int rowH = 22;
		String[] names = {"Config", "War", "Stats", "Players", "Mod Users", "Clan"};
		ph = 70 + names.length * (rowH + 6) + 8;
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		int y = py + 62;
		for (String n : names) {
			final String name = n;
			btn(px + 14, y, pw - 28, rowH, name, Theme.TEXT, b -> open(name));
			y += rowH + 6;
		}
	}

	private void open(String name) {
		Minecraft mc = Minecraft.getInstance();
		switch (name) {
			case "Config" -> mc.setScreen(new ConfigScreen(this));
			case "War" -> mc.setScreen(new WarScreen(this));
			case "Stats" -> mc.setScreen(new StatsScreen(this, null));
			case "Players" -> mc.setScreen(new PlayersScreen(this));
			case "Mod Users" -> mc.setScreen(new NetworkScreen(this, null));
			case "Clan" -> mc.setScreen(new ClanScreen(this));
			default -> { }
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		dim(g);
		Theme.panel(g, px, py, pw, ph);
		Theme.rect(g, px, py, pw, 3, Theme.ACCENT_DARK);
		Theme.textCentered(g, WhitelistGate.clan().toUpperCase() + " MOD", px + pw / 2, py + 16, Theme.ACCENT);
		Theme.textCentered(g, "Open a module", px + pw / 2, py + 32, Theme.TEXT_DIM);
		drawBtns(g, mx, my);
		super.render(g, mx, my, pt);
	}
}
