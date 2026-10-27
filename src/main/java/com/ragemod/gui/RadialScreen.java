package com.ragemod.gui;

import com.ragemod.core.KillTracker;
import com.ragemod.core.Config;
import com.ragemod.core.Msg;
import com.ragemod.core.Status;
import com.ragemod.core.StatusStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.List;
import java.util.Locale;

/** Middle-click menu on a player: Target, Friend, Annoyance, Info, Stats, Show, Ignore, Remove. */
public class RadialScreen extends RageScreen {
	private final AbstractClientPlayer target;
	private final String name;
	private int cx;
	private int cy;
	private int radius;

	public RadialScreen(AbstractClientPlayer target) {
		super("Radial", null);
		this.target = target;
		this.name = target.getName().getString();
	}

	@Override
	protected void init() {
		btns.clear();
		radius = Config.RADIAL_SIZE.i();
		radius = Math.min(radius, Math.min(this.width, this.height) / 2 - 6);
		cx = this.width / 2;
		cy = this.height / 2;
		double ring = radius * 0.80;
		String[] labels = {"Target", "Friend", "Annoyance", "Info", "Stats", "Show", "Ignore", "Remove"};
		for (int i = 0; i < labels.length; i++) {
			final String label = labels[i];
			double ang = Math.toRadians(i * 45.0);
			int bw = Math.max(54, Theme.width(label) + 16);
			int bh = 20;
			int bx = cx + (int) Math.round(Math.sin(ang) * ring) - bw / 2;
			int by = cy - (int) Math.round(Math.cos(ang) * ring) - bh / 2;
			Btn b = btn(bx, by, bw, bh, label, Theme.TEXT, m -> act(label));
			b.active = isActive(label);
			if (b.active) b.color = activeColor(label);
		}
	}

	private boolean isActive(String label) {
		return switch (label) {
			case "Target" -> StatusStore.status(name) == Status.TARGET;
			case "Friend" -> StatusStore.status(name) == Status.FRIEND;
			case "Annoyance" -> StatusStore.status(name) == Status.ANNOYANCE;
			case "Show" -> StatusStore.show(name);
			case "Ignore" -> StatusStore.ignored(name);
			default -> false;
		};
	}

	private int activeColor(String label) {
		return switch (label) {
			case "Target" -> Status.TARGET.argb();
			case "Friend" -> Status.FRIEND.argb();
			case "Annoyance" -> Status.ANNOYANCE.argb();
			case "Show" -> Config.HIGHLIGHT_COLOR.argb();
			default -> Theme.ACCENT;
		};
	}

	private void act(String label) {
		Minecraft mc = Minecraft.getInstance();
		switch (label) {
			case "Target" -> StatusStore.toggle(name, Status.TARGET);
			case "Friend" -> StatusStore.toggle(name, Status.FRIEND);
			case "Annoyance" -> StatusStore.toggle(name, Status.ANNOYANCE);
			case "Show" -> StatusStore.toggleShow(name);
			case "Ignore" -> StatusStore.toggleIgnore(name);
			case "Remove" -> StatusStore.clear(name);
			case "Info" -> info();
			case "Stats" -> {
				mc.setScreen(new StatsScreen(null, name));
				return;
			}
			default -> { }
		}
		mc.setScreen(null);
	}

	private void info() {
		Status s = StatusStore.status(name);
		KillTracker.Stats vsMe = KillTracker.stats(Msg.me(), List.of(name), false);
		Msg.say(name + "  |  status: " + s.label.toLowerCase(Locale.ROOT)
			+ (StatusStore.show(name) ? ", shown" : "") + (StatusStore.ignored(name) ? ", ignored" : ""));
		Msg.say("You vs " + name + ": " + vsMe.kills() + " kills, " + vsMe.deaths() + " deaths (K/D " + String.format("%.2f", vsMe.kd()) + ")");
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean handled = super.mouseClicked(event, doubleClick);
		if (handled) return true;
		// click outside the circle closes the menu
		double dx = event.x() - cx;
		double dy = event.y() - cy;
		if (dx * dx + dy * dy > (double) radius * radius) {
			Minecraft.getInstance().setScreen(null);
			return true;
		}
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(null);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		g.fill(0, 0, this.width, this.height, 0x60000000);
		Theme.circle(g, cx, cy, radius, 0xC0140608);
		Theme.circle(g, cx, cy, radius - 2, 0xC01C0A0D);
		int inner = (int) (radius * 0.34);
		Theme.circle(g, cx, cy, inner + 2, 0xFF5E1117);
		Theme.circle(g, cx, cy, inner, 0xFF2A1116);

		// head (flat square, like the skin face)
		int head = 32;
		drawHead(g, cx - head / 2, cy - head - 2, head);
		Theme.textCentered(g, name, cx, cy + 8, Theme.TEXT);
		Status s = StatusStore.status(name);
		if (s != Status.NONE) Theme.textCentered(g, s.label, cx, cy + 20, s.argb());

		drawBtns(g, mx, my);
	}

	private void drawHead(GuiGraphics g, int x, int y, int size) {
		Minecraft mc = Minecraft.getInstance();
		PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(target.getUUID());
		if (info != null) {
			PlayerFaceRenderer.draw(g, info.getSkin(), x, y, size);
		} else {
			g.fill(x, y, x + size, y + size, 0xFF3A1A1F);
		}
	}
}
