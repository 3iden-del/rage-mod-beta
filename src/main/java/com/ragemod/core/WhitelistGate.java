package com.ragemod.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Whitelist gate. The mod only works for players whose Minecraft UUID is listed in the
 * whitelist.json you host on GitHub. Edit that file to add/remove people, no new jar needed.
 */
public final class WhitelistGate {
	// ================= CONFIG: edit these two lines =================
	/** Raw URL of your whitelist.json (GitHub -> open file -> Raw -> copy the link). */
	private static final String WHITELIST_URL = "https://raw.githubusercontent.com/YOUR_USER/YOUR_REPO/main/whitelist.json";
	/** Set to false while testing to skip the whitelist completely. Set to true for the release build. */
	private static final boolean ENFORCE = true;
	// ================================================================

	private static final long REFRESH_MINUTES = 5;
	private static final long OFFLINE_GRACE_MINUTES = 60;

	public record Member(String uuid, String name, String role) {}

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

	private static volatile boolean allowed = false;
	private static volatile boolean started = false;
	private static volatile long lastSuccessMs = 0;
	private static volatile String reason = "Checking whitelist...";
	private static volatile String clanName = "RAGE";
	private static volatile String relayUrl = "";
	private static volatile List<Member> members = Collections.emptyList();
	private static long lastNoticeMs = 0;

	private WhitelistGate() {}

	public static synchronized void start() {
		if (started) return;
		started = true;
		if (!ENFORCE) {
			allowed = true;
			return;
		}
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "rage-whitelist");
			t.setDaemon(true);
			return t;
		});
		ses.scheduleWithFixedDelay(WhitelistGate::refresh, 0, REFRESH_MINUTES, TimeUnit.MINUTES);
	}

	/** True if this player is whitelisted right now. Safe to call every frame. */
	public static boolean allowed() {
		return allowed;
	}

	public static String clan() { return clanName; }
	public static String relayUrl() { return relayUrl; }
	public static List<Member> members() { return members; }

	public static UUID selfUuid() {
		try {
			return Minecraft.getInstance().getUser().getProfileId();
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Use in keybind handlers / before opening screens. Returns true if allowed, otherwise tells the
	 * player why, shows their UUID (and copies it) so they can send it to you, and returns false.
	 */
	public static boolean ensureAllowed() {
		if (allowed) return true;
		long now = System.currentTimeMillis();
		if (now - lastNoticeMs > 3000) {
			lastNoticeMs = now;
			Minecraft mc = Minecraft.getInstance();
			UUID id = selfUuid();
			String idText = id == null ? "unknown" : id.toString();
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.literal("[RAGE] Not whitelisted: " + reason), false);
				mc.player.displayClientMessage(Component.literal("[RAGE] Your ID: " + idText + " (copied to clipboard)"), false);
			}
			if (id != null) mc.keyboardHandler.setClipboard(idText);
		}
		return false;
	}

	private static void refresh() {
		try {
			UUID me = selfUuid();
			if (me == null) {
				allowed = false;
				reason = "Couldn't read your Minecraft account.";
				return;
			}
			HttpRequest req = HttpRequest.newBuilder(URI.create(WHITELIST_URL))
				.timeout(Duration.ofSeconds(10))
				.header("Cache-Control", "no-cache")
				.GET()
				.build();
			HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());

			JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
			if (root.has("clan")) clanName = root.get("clan").getAsString();
			if (root.has("relay")) relayUrl = root.get("relay").getAsString();

			List<Member> list = new ArrayList<>();
			JsonArray arr = root.getAsJsonArray("players");
			for (JsonElement e : arr) {
				JsonObject p = e.getAsJsonObject();
				String uuid = p.get("uuid").getAsString().toLowerCase(Locale.ROOT);
				String name = p.has("name") ? p.get("name").getAsString() : uuid;
				String role = p.has("role") ? p.get("role").getAsString() : "member";
				list.add(new Member(uuid, name, role));
			}
			members = list;
			lastSuccessMs = System.currentTimeMillis();

			// "enabled": false locks the mod for everyone (kill switch).
			if (root.has("enabled") && !root.get("enabled").getAsBoolean()) {
				allowed = false;
				reason = "The mod is currently disabled.";
				return;
			}
			boolean ok = false;
			String mine = me.toString().toLowerCase(Locale.ROOT);
			for (Member m : list) {
				if (m.uuid().equals(mine)) {
					ok = true;
					break;
				}
			}
			allowed = ok;
			reason = ok ? "" : "Ask the owner to whitelist you.";
		} catch (Exception ex) {
			long age = System.currentTimeMillis() - lastSuccessMs;
			if (lastSuccessMs == 0 || age > TimeUnit.MINUTES.toMillis(OFFLINE_GRACE_MINUTES)) {
				allowed = false;
				reason = "Couldn't reach the whitelist (" + ex.getClass().getSimpleName() + ").";
			}
		}
	}
}
