package com.ragemod.gui;

import com.ragemod.core.KillTracker;
import com.ragemod.core.Lobby;
import com.ragemod.core.Msg;
import com.ragemod.core.Status;
import com.ragemod.core.StatusStore;
import com.ragemod.core.War;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** War module: status on the left, pick enemies from the server players on the right. */
public class WarScreen extends RageScreen {
	private static final int ROW_H = 22;
	private static final int GAP = 3;

	private EditBox search;
	private String filter = "";
	private int offset;
	private int lx;
	private int ly;
	private int lw;
	private int lh;
	private int rx;
	private int rw;
	private int listY;
	private int perPage;
	private List<String> shown = new ArrayList<>();

	public WarScreen(Screen parent) {
		super("War", parent);
	}

	@Override
	protected void init() {
		lx = 12;
		ly = 10;
		lw = Math.min(280, this.width * 38 / 100);
		lh = this.height - 20;
		rx = lx + lw + 8;
		rw = this.width - rx - 12;
		listY = ly + 56;
		perPage = Math.max(1, (lh - 56 - 30) / (ROW_H + GAP));
		search = input(rx + 10, ly + 28, rw - 20, 20, "Search current server nicks...", s -> {
			filter = s;
			offset = 0;
			layout();
		});
		search.setValue(filter);
		layout();
	}

	private void layout() {
		btns.clear();
		String f = filter.toLowerCase(Locale.ROOT).trim();
		shown = new ArrayList<>();
		for (String n : Lobby.names(false)) {
			if (f.isEmpty() || n.toLowerCase(Locale.ROOT).contains(f)) shown.add(n);
		}
		// enemies first so you see your picks at the top
		shown.sort((a, b) -> {
			boolean ea = StatusStore.status(a) == Status.ENEMY;
			boolean eb = StatusStore.status(b) == Status.ENEMY;
			if (ea != eb) return ea ? -1 : 1;
			return a.compareToIgnoreCase(b);
		});
		offset = clamp(offset, 0, Math.max(0, shown.size() - perPage));

		btn(rx + rw - 62, ly + 6, 52, 16, "Back", Theme.TEXT, b -> back());
		btn(rx + rw / 2 - 40, ly + lh - 24, 24, 18, "<", Theme.TEXT, b -> page(-perPage));
		btn(rx + rw / 2 + 16, ly + lh - 24, 24, 18, ">", Theme.TEXT, b -> page(perPage));

		int by = ly + 36 + 8 * 14 + 8;
		if (!War.active) {
			Btn b = btn(lx + 10, by, lw - 20, 22, "Start War", Theme.GOOD, m -> {
				War.start();
				layout();
			});
			b.active = false;
		} else {
			if (!War.joined) {
				btn(lx + 10, by, lw - 20, 22, "Join War", Theme.GOOD, m -> {
					War.join();
					layout();
				});
				by += 28;
			}
			btn(lx + 10, by, lw - 20, 22, "End War", Theme.TEXT, m -> {
				War.end();
				layout();
			});
		}

		int end = Math.min(shown.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			final String name = shown.get(i);
			int y = listY + (i - offset) * (ROW_H + GAP);
			boolean enemy = StatusStore.status(name) == Status.ENEMY;
			btn(rx + rw - 10 - 84, y + 2, 84, ROW_H - 4, enemy ? "Deselect" : "Enemy", enemy ? Theme.BAD : Theme.TEXT, m -> {
				StatusStore.toggle(name, Status.ENEMY);
				layout();
			});
		}
	}

	private void page(int delta) {
		offset = clamp(offset + delta, 0, Math.max(0, shown.size() - perPage));
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

		// ---- left: war status ----
		Theme.panel(g, lx, ly, lw, lh);
		Theme.rect(g, lx, ly, lw, 3, Theme.ACCENT_DARK);
		Theme.warIcon(g, lx + 10, ly + 12, 14, Theme.ACCENT);
		Theme.text(g, "War", lx + 32, ly + 14, Theme.ACCENT);
		int y = ly + 36;
		Theme.text(g, "Active: " + War.active, lx + 10, y, Theme.TEXT);
		Theme.text(g, "Joined: " + War.joined, lx + 10, y + 14, Theme.TEXT);
		Theme.text(g, "Leading: " + War.leading, lx + 10, y + 28, Theme.TEXT);
		Theme.text(g, "Server: " + Theme.fit(War.active ? War.server : Msg.server(), lw - 80), lx + 10, y + 42, Theme.TEXT);
		Theme.text(g, "Leader: " + Theme.fit(War.leader, lw - 80), lx + 10, y + 56, Theme.TEXT);
		Theme.text(g, "Enemies: " + War.enemies(), lx + 10, y + 70, Theme.TEXT_DIM);
		KillTracker.Stats st = KillTracker.stats(Msg.me(), StatusStore.namesWith(Status.ENEMY), false);
		String vs = (st.kills() + st.deaths() == 0)
			? "no fights"
			: st.kills() + "-" + st.deaths() + " (" + String.format("%.2f", st.kd()) + ")";
		Theme.text(g, Theme.fit("Your K/D vs enemies: " + vs, lw - 20), lx + 10, y + 84, Theme.TEXT_DIM);
		if (!War.note.isEmpty()) Theme.text(g, Theme.fit(War.note, lw - 20), lx + 10, ly + lh - 18, Theme.GOOD);

		// ---- right: server players ----
		Theme.panel(g, rx, ly, rw, lh);
		Theme.rect(g, rx, ly, rw, 3, Theme.ACCENT_DARK);
		title(g, "Current Server Players", rx + 10, ly + 12);

		int end = Math.min(shown.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			String name = shown.get(i);
			int ry = listY + (i - offset) * (ROW_H + GAP);
			boolean enemy = StatusStore.status(name) == Status.ENEMY;
			boolean hover = over(rx + 8, ry, rw - 16, ROW_H, mx, my);
			Theme.rect(g, rx + 8, ry, rw - 16, ROW_H, hover ? Theme.ROW_HOVER : Theme.ROW);
			int nameX = rx + 16;
			if (enemy) {
				Theme.warIcon(g, nameX, ry + 4, 14, Theme.ACCENT);
				nameX += 22;
			}
			Theme.text(g, name, nameX, ry + (ROW_H - 8) / 2, enemy ? Status.ENEMY.argb() : Theme.TEXT);
		}
		if (shown.isEmpty()) Theme.text(g, "No players found.", rx + 16, listY + 6, Theme.TEXT_DIM);
		String foot = shown.isEmpty() ? "0 / 0" : (offset + 1) + "-" + end + " / " + shown.size();
		Theme.textRight(g, foot, rx + rw - 12, ly + lh - 20, Theme.TEXT_DIM);

		drawBtns(g, mx, my);
		super.render(g, mx, my, pt);
	}
}
