package com.ragemod.gui;

import com.ragemod.core.Msg;
import com.ragemod.net.RelayClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Online mod users, private chat, and the "ask everyone for help" button. */
public class NetworkScreen extends RageScreen {
	private static final int ROW_H = 20;

	private String selected;
	private EditBox input;
	private int lx;
	private int lw;
	private int rx;
	private int rw;
	private int top;
	private int bottom;
	private int perPage;
	private int offset;
	private List<RelayClient.Peer> lastPeers;

	public NetworkScreen(Screen parent, String select) {
		super("Mod Users", parent);
		this.selected = select;
	}

	@Override
	protected void init() {
		lx = 12;
		lw = Math.max(140, this.width * 30 / 100);
		rx = lx + lw + 8;
		rw = this.width - rx - 12;
		top = 30;
		bottom = this.height - 10;
		perPage = Math.max(1, (bottom - top - 54) / (ROW_H + 2));
		input = input(rx + 8, bottom - 28, rw - 16 - 66, 20, "Message...", s -> { });
		input.setMaxLength(200);
		layout();
	}

	private void layout() {
		btns.clear();
		lastPeers = RelayClient.peers();
		offset = clamp(offset, 0, Math.max(0, lastPeers.size() - perPage));

		btn(this.width - 64, 6, 52, 16, "Back", Theme.TEXT, m -> back());
		btn(lx, 6, 130, 18, "ASK FOR HELP", Theme.ACCENT, m -> askHelp());
		btn(rx + rw - 8 - 58, bottom - 28, 58, 20, "Send", Theme.GOOD, m -> send());

		int end = Math.min(lastPeers.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			final RelayClient.Peer p = lastPeers.get(i);
			int y = top + 40 + (i - offset) * (ROW_H + 2);
			String label = p.name() + (RelayClient.unread(p.name()) ? "  *" : "");
			Btn b = btn(lx + 4, y, lw - 8, ROW_H, label, RelayClient.unread(p.name()) ? Theme.ACCENT : 0xFF5AB8FF, m -> {
				selected = p.name();
				RelayClient.markRead(p.name());
				layout();
			});
			b.flat = true;
			b.active = p.name().equalsIgnoreCase(selected);
		}
	}

	private void askHelp() {
		if (!RelayClient.connected()) {
			Msg.say("Not connected to the relay, can't ask for help.");
			return;
		}
		RelayClient.sendHelp("");
		Msg.say("Help request sent to " + RelayClient.peers().size() + " mod user(s).");
	}

	private void send() {
		if (selected == null || input == null) return;
		String t = input.getValue().trim();
		if (t.isEmpty()) return;
		if (!RelayClient.connected()) {
			Msg.say("Not connected to the relay.");
			return;
		}
		RelayClient.sendPm(selected, t);
		input.setValue("");
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && input != null && input.isFocused()) {
			send();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (sy != 0 && mx < rx) {
			offset = clamp(offset + (sy > 0 ? -2 : 2), 0, Math.max(0, RelayClient.peers().size() - perPage));
			layout();
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy);
	}

	private List<String> wrap(String text, int maxW) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = line.length() == 0 ? word : line + " " + word;
			if (Theme.width(test) > maxW && line.length() > 0) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(test);
			}
		}
		if (line.length() > 0) out.add(line.toString());
		return out;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		// refresh the list when the relay sends a new user list
		if (lastPeers != RelayClient.peers()) layout();

		dim(g);
		// left: users
		Theme.panel(g, lx, top, lw, bottom - top);
		Theme.text(g, "Mod users online (" + RelayClient.peers().size() + ")", lx + 8, top + 8, Theme.TEXT);
		boolean on = RelayClient.connected();
		Theme.text(g, "Relay: " + Theme.fit(RelayClient.status(), lw - 60), lx + 8, top + 22, on ? Theme.GOOD : Theme.BAD);
		if (RelayClient.peers().isEmpty()) Theme.text(g, "Nobody else online.", lx + 8, top + 44, Theme.TEXT_DIM);

		// right: chat
		Theme.panel(g, rx, top, rw, bottom - top);
		Theme.rect(g, rx, top, rw, 3, Theme.ACCENT_DARK);
		if (selected == null) {
			Theme.text(g, "Select a user to chat privately.", rx + 10, top + 12, Theme.TEXT_DIM);
		} else {
			Theme.text(g, "Chat with " + selected + (RelayClient.isOnline(selected) ? "" : " (offline)"), rx + 10, top + 10, Theme.ACCENT);
			List<String> lines = new ArrayList<>(RelayClient.convo(selected));
			List<String> wrapped = new ArrayList<>();
			for (String l : lines) wrapped.addAll(wrap(l, rw - 24));
			int maxLines = Math.max(1, (bottom - top - 24 - 40) / 11);
			int start = Math.max(0, wrapped.size() - maxLines);
			int y = top + 26;
			for (int i = start; i < wrapped.size(); i++) {
				String l = wrapped.get(i);
				Theme.text(g, l, rx + 10, y, l.startsWith("You:") ? Theme.TEXT_DIM : Theme.TEXT);
				y += 11;
			}
			RelayClient.markRead(selected);
		}

		drawBtns(g, mx, my);
		super.render(g, mx, my, pt);
	}
}
