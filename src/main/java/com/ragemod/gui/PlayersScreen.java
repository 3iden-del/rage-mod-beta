package com.ragemod.gui;

import com.ragemod.core.Lobby;
import com.ragemod.core.Status;
import com.ragemod.core.StatusStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** "Player Statuses": everyone in the lobby (plus saved marks) with Target / Friend / Annoyance buttons. */
public class PlayersScreen extends RageScreen {
	private static final int ROW_H = 22;
	private static final int GAP = 3;

	private EditBox search;
	private String filter = "";
	private int offset;
	private int px;
	private int py;
	private int pw;
	private int ph;
	private int listY;
	private int perPage;
	private List<String> all = new ArrayList<>();

	public PlayersScreen(Screen parent) {
		super("Player Statuses", parent);
	}

	@Override
	protected void init() {
		px = 14;
		py = 10;
		pw = this.width - 28;
		ph = this.height - 20;
		listY = py + 54;
		perPage = Math.max(1, (ph - 54 - 30) / (ROW_H + GAP));
		search = input(px + 10, py + 26, pw - 20, 20, "Search current map or saved statuses...", s -> {
			filter = s;
			offset = 0;
			layout();
		});
		search.setValue(filter);
		layout();
	}

	private void collect() {
		Map<String, String> names = new LinkedHashMap<>();
		for (String n : Lobby.names(false)) names.put(n.toLowerCase(Locale.ROOT), n);
		for (StatusStore.Entry e : StatusStore.all()) names.putIfAbsent(e.name.toLowerCase(Locale.ROOT), e.name);
		String f = filter.toLowerCase(Locale.ROOT).trim();
		all = new ArrayList<>();
		for (Map.Entry<String, String> e : names.entrySet()) {
			if (f.isEmpty() || e.getKey().contains(f)) all.add(e.getValue());
		}
		all.sort(String.CASE_INSENSITIVE_ORDER);
	}

	private void layout() {
		btns.clear();
		collect();
		offset = clamp(offset, 0, Math.max(0, all.size() - perPage));

		btn(px + pw - 62, py + 6, 52, 16, "Back", Theme.TEXT, b -> back());
		btn(px + pw / 2 - 40, py + ph - 24, 24, 18, "<", Theme.TEXT, b -> page(-perPage));
		btn(px + pw / 2 + 16, py + ph - 24, 24, 18, ">", Theme.TEXT, b -> page(perPage));

		int bw = 78;
		int end = Math.min(all.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			final String name = all.get(i);
			int y = listY + (i - offset) * (ROW_H + GAP);
			int x = px + pw - 10 - bw * 3 - 4;
			statusBtn(x, y, bw, name, Status.TARGET);
			statusBtn(x + bw + 2, y, bw, name, Status.FRIEND);
			statusBtn(x + (bw + 2) * 2, y, bw, name, Status.ANNOYANCE);
		}
	}

	private void statusBtn(int x, int y, int w, String name, Status s) {
		Btn b = btn(x, y + 2, w, ROW_H - 4, s.label, s.argb(), m -> {
			StatusStore.toggle(name, s);
			layout();
		});
		b.active = StatusStore.status(name) == s;
	}

	private void page(int delta) {
		offset = clamp(offset + delta, 0, Math.max(0, all.size() - perPage));
		layout();
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (sy != 0) {
			page(sy > 0 ? -2 : 2);
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		dim(g);
		Theme.panel(g, px, py, pw, ph);
		Theme.rect(g, px, py, pw, 3, Theme.ACCENT_DARK);
		title(g, "Player Statuses", px + 10, py + 10);

		int end = Math.min(all.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			String name = all.get(i);
			int y = listY + (i - offset) * (ROW_H + GAP);
			boolean hover = over(px + 8, y, pw - 16, ROW_H, mx, my);
			Theme.rect(g, px + 8, y, pw - 16, ROW_H, hover ? Theme.ROW_HOVER : ((i & 1) == 0 ? Theme.ROW : Theme.ROW_ALT));
			Status s = StatusStore.status(name);
			int color = s == Status.NONE ? Theme.TEXT : s.argb();
			Theme.text(g, name, px + 16, y + (ROW_H - 8) / 2, color);
			if (!Lobby.contains(name)) Theme.text(g, "(offline)", px + 16 + Theme.width(name) + 8, y + (ROW_H - 8) / 2, Theme.TEXT_DIM);
		}
		if (all.isEmpty()) Theme.text(g, "No players found.", px + 16, listY + 6, Theme.TEXT_DIM);

		String foot = all.isEmpty() ? "0 / 0" : (offset + 1) + "-" + end + " / " + all.size();
		Theme.textRight(g, foot, px + pw - 12, py + ph - 20, Theme.TEXT_DIM);

		drawBtns(g, mx, my);
		super.render(g, mx, my, pt);
	}
}
