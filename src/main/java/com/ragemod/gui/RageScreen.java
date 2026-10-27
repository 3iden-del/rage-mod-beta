package com.ragemod.gui;

import com.ragemod.core.WhitelistGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Base screen: RAGE styling, simple custom buttons, search boxes, paging. */
public abstract class RageScreen extends Screen {
	/** A flat custom button. action gets the mouse button (0 = left, 1 = right). */
	public static final class Btn {
		public int x;
		public int y;
		public int w;
		public int h;
		public String label = "";
		public Supplier<String> labelFn;
		public int color = Theme.TEXT;
		public boolean active;
		public boolean enabled = true;
		/** Flat list-row style (no border, left aligned text with a colored dot). */
		public boolean flat;
		public IntConsumer action;

		boolean hit(double mx, double my) {
			return enabled && mx >= x && mx < x + w && my >= y && my < y + h;
		}

		String text() {
			return labelFn != null ? labelFn.get() : label;
		}
	}

	protected final Screen parent;
	protected final List<Btn> btns = new ArrayList<>();

	protected RageScreen(String title, Screen parent) {
		super(Component.literal(title));
		this.parent = parent;
	}

	// ---- helpers ----

	protected Btn btn(int x, int y, int w, int h, String label, int color, IntConsumer action) {
		Btn b = new Btn();
		b.x = x;
		b.y = y;
		b.w = w;
		b.h = h;
		b.label = label;
		b.color = color;
		b.action = action;
		btns.add(b);
		return b;
	}

	protected EditBox input(int x, int y, int w, int h, String hint, Consumer<String> onChange) {
		EditBox eb = new EditBox(this.font, x, y, w, h, Component.literal(hint));
		eb.setHint(Component.literal(hint));
		eb.setMaxLength(64);
		eb.setResponder(onChange);
		this.addRenderableWidget(eb);
		return eb;
	}

	protected void drawBtn(GuiGraphics g, Btn b, int mx, int my) {
		boolean hover = b.hit(mx, my);
		if (b.flat) {
			int rowBg = b.active ? Theme.ROW_SEL : (hover ? Theme.ROW_HOVER : Theme.ROW);
			Theme.rect(g, b.x, b.y, b.w, b.h, rowBg);
			Theme.rect(g, b.x + 6, b.y + (b.h - 4) / 2, 4, 4, b.color);
			Theme.text(g, Theme.fit(b.text(), b.w - 22), b.x + 15, b.y + (b.h - 8) / 2, b.active ? Theme.TEXT : b.color);
			return;
		}
		int bg = hover ? Theme.BTN_HOVER : Theme.BTN;
		Theme.rect(g, b.x, b.y, b.w, b.h, bg);
		Theme.border(g, b.x, b.y, b.w, b.h, b.active ? b.color : Theme.BTN_EDGE);
		if (b.active) Theme.rect(g, b.x + 1, b.y + 1, b.w - 2, 1, b.color);
		String t = Theme.fit(b.text(), b.w - 6);
		int color = b.enabled ? b.color : Theme.TEXT_DIM;
		Theme.textCentered(g, t, b.x + b.w / 2, b.y + (b.h - 8) / 2, color);
	}

	protected void drawBtns(GuiGraphics g, int mx, int my) {
		for (Btn b : btns) drawBtn(g, b, mx, my);
	}

	protected boolean over(int x, int y, int w, int h, int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	protected static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	/** Draws the dim background. Call first in render(). */
	protected void dim(GuiGraphics g) {
		g.fill(0, 0, this.width, this.height, Theme.DIM);
	}

	/** Title in accent red with a thin underline. */
	protected void title(GuiGraphics g, String text, int x, int y) {
		Theme.text(g, text, x, y, Theme.ACCENT);
	}

	protected void back() {
		Minecraft.getInstance().setScreen(parent);
	}

	// ---- Screen overrides ----

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// we draw our own dim, no vanilla blur
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!WhitelistGate.allowed()) return super.mouseClicked(event, doubleClick);
		if (super.mouseClicked(event, doubleClick)) return true;
		for (int i = btns.size() - 1; i >= 0; i--) {
			Btn b = btns.get(i);
			if (b.hit(event.x(), event.y())) {
				if (b.action != null) b.action.accept(event.button());
				return true;
			}
		}
		return false;
	}
}
