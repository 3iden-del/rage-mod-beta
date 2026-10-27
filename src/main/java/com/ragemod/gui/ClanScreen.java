package com.ragemod.gui;

import com.ragemod.core.Lobby;
import com.ragemod.core.WhitelistGate;
import com.ragemod.net.RelayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/** Clan members (from the whitelist file) with an online dot. */
public class ClanScreen extends RageScreen {
	private static final int ROW_H = 22;
	private static final int GAP = 3;

	private int offset;
	private int px;
	private int py;
	private int pw;
	private int ph;
	private int listY;
	private int perPage;
	private List<WhitelistGate.Member> members = new ArrayList<>();

	public ClanScreen(Screen parent) {
		super("Clan", parent);
	}

	@Override
	protected void init() {
		px = 14;
		py = 10;
		pw = this.width - 28;
		ph = this.height - 20;
		listY = py + 34;
		perPage = Math.max(1, (ph - 34 - 30) / (ROW_H + GAP));
		layout();
	}

	private boolean online(String name) {
		return RelayClient.isOnline(name) || Lobby.contains(name);
	}

	private void layout() {
		btns.clear();
		members = new ArrayList<>(WhitelistGate.members());
		// online first, then alphabetical
		members.sort((a, b) -> {
			boolean oa = online(a.name());
			boolean ob = online(b.name());
			if (oa != ob) return oa ? -1 : 1;
			return a.name().compareToIgnoreCase(b.name());
		});
		offset = clamp(offset, 0, Math.max(0, members.size() - perPage));

		btn(px + pw - 62, py + 6, 52, 16, "Back", Theme.TEXT, b -> back());
		btn(px + pw / 2 - 40, py + ph - 24, 24, 18, "<", Theme.TEXT, b -> page(-perPage));
		btn(px + pw / 2 + 16, py + ph - 24, 24, 18, ">", Theme.TEXT, b -> page(perPage));

		int end = Math.min(members.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			final String name = members.get(i).name();
			int y = listY + (i - offset) * (ROW_H + GAP);
			if (RelayClient.isOnline(name)) {
				btn(px + pw - 10 - 70, y + 2, 70, ROW_H - 4, "Message", Theme.TEXT,
					m -> Minecraft.getInstance().setScreen(new NetworkScreen(this, name)));
			}
		}
	}

	private void page(int delta) {
		offset = clamp(offset + delta, 0, Math.max(0, members.size() - perPage));
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
		int on = 0;
		for (WhitelistGate.Member m : members) {
			if (online(m.name())) on++;
		}
		title(g, WhitelistGate.clan() + " clan members  (" + on + " online / " + members.size() + ")", px + 10, py + 12);

		int end = Math.min(members.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			WhitelistGate.Member m = members.get(i);
			int y = listY + (i - offset) * (ROW_H + GAP);
			boolean hover = over(px + 8, y, pw - 16, ROW_H, mx, my);
			Theme.rect(g, px + 8, y, pw - 16, ROW_H, hover ? Theme.ROW_HOVER : ((i & 1) == 0 ? Theme.ROW : Theme.ROW_ALT));
			boolean on2 = online(m.name());
			Theme.rect(g, px + 16, y + (ROW_H - 6) / 2, 6, 6, on2 ? Theme.GOOD : 0xFF5A3A3C);
			Theme.text(g, m.name(), px + 30, y + (ROW_H - 8) / 2, on2 ? Theme.TEXT : Theme.TEXT_DIM);
			int roleColor = m.role().equalsIgnoreCase("owner") || m.role().equalsIgnoreCase("leader") ? Theme.ACCENT : Theme.TEXT_DIM;
			Theme.text(g, m.role(), px + 30 + Theme.width(m.name()) + 10, y + (ROW_H - 8) / 2, roleColor);
		}
		if (members.isEmpty()) Theme.text(g, "No members loaded yet (whitelist not fetched).", px + 16, listY + 6, Theme.TEXT_DIM);
		String foot = members.isEmpty() ? "0 / 0" : (offset + 1) + "-" + end + " / " + members.size();
		Theme.textRight(g, foot, px + pw - 12, py + ph - 20, Theme.TEXT_DIM);

		drawBtns(g, mx, my);
		super.render(g, mx, my, pt);
	}
}
