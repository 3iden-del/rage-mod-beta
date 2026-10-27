package com.ragemod.render;

import com.ragemod.Features;
import com.ragemod.core.Config;
import com.ragemod.core.War;
import com.ragemod.core.WhitelistGate;
import com.ragemod.net.RelayClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Tiny on-screen indicators in the top-left corner. */
public final class Hud {
	private Hud() {}

	public static void render(GuiGraphics g, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || !Config.HUD.b() || !WhitelistGate.allowed()) return;
		int y = 4;
		if (Features.afk) y = line(g, mc, "RAGE: Anti-AFK ON", y);
		if (Features.autoJoin) y = line(g, mc, "RAGE: Auto join " + Features.autoJoinAttempts + "/" + Config.AJ_MAX.i(), y);
		if (War.active) y = line(g, mc, "RAGE: WAR" + (War.joined ? "" : " (not joined)"), y);
		if (RelayClient.anyUnread()) line(g, mc, "RAGE: new private message", y);
	}

	private static int line(GuiGraphics g, Minecraft mc, String text, int y) {
		g.drawString(mc.font, text, 5, y, 0xFFFF5555);
		return y + 11;
	}
}
