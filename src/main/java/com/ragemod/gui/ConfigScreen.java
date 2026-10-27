package com.ragemod.gui;

import com.ragemod.core.Config;
import com.ragemod.core.Opt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Settings panel: search, color pickers, toggles, numbers and text options. */
public class ConfigScreen extends RageScreen {
	private static final int ROW_H = 32;
	private static final int CTRL_W = 150;

	private EditBox search;
	private String filter = "";
	private int offset;
	private int px;
	private int py;
	private int pw;
	private int ph;
	private int listY;
	private int perPage;
	private List<Opt> shown = new ArrayList<>();
	private final List<EditBox> rowBoxes = new ArrayList<>();
	private final List<Integer> swatchX = new ArrayList<>();
	private final List<Integer> swatchY = new ArrayList<>();
	private final List<Opt> swatchOpt = new ArrayList<>();

	public ConfigScreen(Screen parent) {
		super("Config", parent);
	}

	@Override
	protected void init() {
		px = 14;
		py = 8;
		pw = this.width - 28;
		ph = this.height - 16;
		listY = py + 54;
		perPage = Math.max(1, (ph - 54 - 34) / ROW_H);
		int sw = Math.min(pw - 40, 360);
		search = input(px + (pw - sw) / 2, py + 26, sw, 20, "Search options...", s -> {
			filter = s;
			offset = 0;
			layout();
		});
		search.setValue(filter);
		layout();
	}

	private void layout() {
		btns.clear();
		swatchX.clear();
		swatchY.clear();
		swatchOpt.clear();
		for (EditBox e : rowBoxes) this.removeWidget(e);
		rowBoxes.clear();

		String f = filter.toLowerCase(Locale.ROOT).trim();
		shown = new ArrayList<>();
		for (Opt o : Config.ALL) {
			if (f.isEmpty() || o.key.toLowerCase(Locale.ROOT).contains(f) || o.desc.toLowerCase(Locale.ROOT).contains(f)
				|| o.group.toLowerCase(Locale.ROOT).contains(f)) {
				shown.add(o);
			}
		}
		offset = clamp(offset, 0, Math.max(0, shown.size() - perPage));

		btn(px + pw / 2 - 90, py + ph - 26, 120, 20, "Reset to Defaults", Theme.BAD, b -> {
			Config.resetAll();
			layout();
		});
		btn(px + pw / 2 + 36, py + ph - 26, 54, 20, "Done", Theme.TEXT, b -> onClose());
		btn(px + pw / 2 - 150, py + ph - 26, 54, 20, "<", Theme.TEXT, b -> page(-perPage));
		btn(px + pw / 2 + 96, py + ph - 26, 54, 20, ">", Theme.TEXT, b -> page(perPage));

		int end = Math.min(shown.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			final Opt o = shown.get(i);
			int y = listY + (i - offset) * ROW_H;
			int x = px + pw - 12 - CTRL_W;
			if (o.kind == Opt.Kind.STRING) {
				EditBox eb = new EditBox(this.font, x, y + 6, CTRL_W, 20, Component.literal(o.key));
				eb.setMaxLength(120);
				eb.setValue(o.s());
				eb.setResponder(o::set);
				this.addRenderableWidget(eb);
				rowBoxes.add(eb);
			} else {
				Btn b = btn(x, y + 6, CTRL_W, 20, "", Theme.TEXT, m -> {
					o.cycle(m == 1 ? -1 : 1);
					Config.save();
					layout();
				});
				b.labelFn = o::display;
				if (o.kind == Opt.Kind.BOOL) b.color = o.b() ? Theme.GOOD : Theme.BAD;
				if (o.kind == Opt.Kind.COLOR) {
					swatchX.add(x + 4);
					swatchY.add(y + 10);
					swatchOpt.add(o);
				}
			}
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
	public void onClose() {
		Config.save();
		super.onClose();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		dim(g);
		Theme.panel(g, px, py, pw, ph);
		Theme.rect(g, px, py, pw, 3, Theme.ACCENT_DARK);
		Theme.textCentered(g, "RAGEMod Config", px + pw / 2, py + 10, Theme.ACCENT);

		int end = Math.min(shown.size(), offset + perPage);
		for (int i = offset; i < end; i++) {
			Opt o = shown.get(i);
			int y = listY + (i - offset) * ROW_H;
			Theme.rect(g, px + 8, y + 1, pw - 16, ROW_H - 3, (i & 1) == 0 ? Theme.ROW : Theme.ROW_ALT);
			Theme.text(g, o.key, px + 16, y + 6, Theme.TEXT);
			Theme.text(g, Theme.fit(o.desc, pw - CTRL_W - 60), px + 16, y + 18, Theme.TEXT_DIM);
		}
		if (shown.isEmpty()) Theme.text(g, "No options match.", px + 16, listY + 6, Theme.TEXT_DIM);

		drawBtns(g, mx, my);
		for (int i = 0; i < swatchOpt.size(); i++) {
			int sx = swatchX.get(i);
			int sy = swatchY.get(i);
			Theme.rect(g, sx, sy, 12, 12, swatchOpt.get(i).argb());
			Theme.border(g, sx, sy, 12, 12, 0xFF000000);
		}
		String foot = shown.isEmpty() ? "0 / 0" : (offset + 1) + "-" + end + " / " + shown.size();
		Theme.textRight(g, foot, px + pw - 12, py + ph - 20, Theme.TEXT_DIM);
		super.render(g, mx, my, pt);
	}
}
