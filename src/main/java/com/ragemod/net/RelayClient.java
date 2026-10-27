package com.ragemod.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ragemod.core.Config;
import com.ragemod.core.Msg;
import com.ragemod.core.War;
import com.ragemod.core.WhitelistGate;
import com.ragemod.core.StatusStore;
import com.ragemod.core.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Talks to the RAGE relay server (relay/server.js): who is online with the mod, private messages,
 * help requests and war announcements. Everything here is off unless you are whitelisted.
 */
public final class RelayClient {
	public record Peer(String name, String uuid, String server) {}

	private static final HttpClient HTTP = HttpClient.newHttpClient();
	private static final Map<String, List<String>> CONVOS = new ConcurrentHashMap<>();
	private static final Set<String> UNREAD = ConcurrentHashMap.newKeySet();

	private static volatile WebSocket socket;
	private static volatile boolean connecting;
	private static volatile boolean connected;
	private static volatile List<Peer> peers = new ArrayList<>();
	private static volatile String status = "Offline";
	private static long nextTry;
	private static long lastPing;
	private static final StringBuilder BUF = new StringBuilder();

	private RelayClient() {}

	public static boolean connected() { return connected; }
	public static List<Peer> peers() { return peers; }
	public static String status() { return status; }

	public static List<String> convo(String name) {
		return CONVOS.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new CopyOnWriteArrayList<>());
	}

	public static boolean unread(String name) { return UNREAD.contains(name.toLowerCase(Locale.ROOT)); }
	public static void markRead(String name) { UNREAD.remove(name.toLowerCase(Locale.ROOT)); }
	public static boolean anyUnread() { return !UNREAD.isEmpty(); }

	public static boolean isOnline(String name) {
		for (Peer p : peers) {
			if (p.name().equalsIgnoreCase(name)) return true;
		}
		return false;
	}

	private static String url() {
		String u = Config.RELAY_URL.s();
		if (u == null || u.isBlank()) u = WhitelistGate.relayUrl();
		return u == null ? "" : u.trim();
	}

	/** Called every client tick on the main thread. Handles connect / reconnect / keep-alive. */
	public static void tick() {
		boolean want = Config.RELAY.b() && WhitelistGate.allowed();
		if (!want) {
			if (connected || socket != null) disconnect();
			status = Config.RELAY.b() ? "Not whitelisted" : "Disabled in config";
			return;
		}
		long now = System.currentTimeMillis();
		if (connected && now - lastPing > 30_000) {
			lastPing = now;
			JsonObject o = new JsonObject();
			o.addProperty("t", "ping");
			send(o);
		}
		if (!connected && !connecting && now >= nextTry) connect();
	}

	private static void connect() {
		String u = url();
		if (u.isEmpty()) {
			status = "No relay URL set";
			nextTry = System.currentTimeMillis() + 30_000;
			return;
		}
		connecting = true;
		status = "Connecting...";
		try {
			HTTP.newWebSocketBuilder()
				.connectTimeout(Duration.ofSeconds(8))
				.buildAsync(URI.create(u), new Listener())
				.whenComplete((ws, ex) -> {
					connecting = false;
					if (ex != null) {
						connected = false;
						status = "Offline (can't reach relay)";
						nextTry = System.currentTimeMillis() + 15_000;
					} else {
						socket = ws;
						connected = true;
						status = "Online";
						sendHello();
					}
				});
		} catch (Exception ex) {
			connecting = false;
			status = "Offline (bad relay URL)";
			nextTry = System.currentTimeMillis() + 60_000;
		}
	}

	public static void disconnect() {
		WebSocket w = socket;
		socket = null;
		connected = false;
		connecting = false;
		peers = new ArrayList<>();
		if (w != null) {
			try {
				w.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
			} catch (Exception ignored) {
			}
		}
	}

	private static void sendHello() {
		UUID id = WhitelistGate.selfUuid();
		JsonObject o = new JsonObject();
		o.addProperty("t", "hello");
		o.addProperty("uuid", id == null ? "" : id.toString());
		o.addProperty("name", Msg.me());
		o.addProperty("server", Msg.server());
		o.addProperty("v", "1.0");
		send(o);
	}

	/** Tell the relay which server you're on now. */
	public static void updateServer() {
		if (connected) sendHello();
	}

	private static void send(JsonObject o) {
		WebSocket w = socket;
		if (w == null || !connected) return;
		try {
			w.sendText(o.toString(), true);
		} catch (Exception ignored) {
		}
	}

	public static void sendPm(String to, String text) {
		if (text.isBlank()) return;
		JsonObject o = new JsonObject();
		o.addProperty("t", "pm");
		o.addProperty("to", to);
		o.addProperty("msg", text);
		send(o);
		convo(to).add("You: " + text);
	}

	public static void sendHelp(String text) {
		JsonObject o = new JsonObject();
		o.addProperty("t", "help");
		o.addProperty("msg", text);
		o.addProperty("server", Msg.server());
		send(o);
	}

	public static void sendWar(String action) {
		JsonObject o = new JsonObject();
		o.addProperty("t", "war");
		o.addProperty("action", action);
		o.addProperty("server", Msg.server());
		JsonArray arr = new JsonArray();
		for (String n : StatusStore.namesWith(Status.ENEMY)) arr.add(n);
		o.add("enemies", arr);
		send(o);
	}

	private static void ping(boolean urgent) {
		if (!Config.SOUNDS.b()) return;
		Minecraft mc = Minecraft.getInstance();
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), urgent ? 2.0f : 1.5f));
	}

	private static void handle(JsonObject o) {
		String t = o.has("t") ? o.get("t").getAsString() : "";
		switch (t) {
			case "users" -> {
				List<Peer> list = new ArrayList<>();
				UUID me = WhitelistGate.selfUuid();
				Set<String> before = new HashSet<>();
				for (Peer p : peers) before.add(p.name().toLowerCase(Locale.ROOT));
				for (JsonElement e : o.getAsJsonArray("list")) {
					JsonObject u = e.getAsJsonObject();
					String uuid = u.has("uuid") ? u.get("uuid").getAsString() : "";
					if (me != null && uuid.equalsIgnoreCase(me.toString())) continue;
					list.add(new Peer(u.get("name").getAsString(), uuid, u.has("server") ? u.get("server").getAsString() : "?"));
				}
				boolean first = before.isEmpty();
				peers = list;
				if (Config.JOIN_NOTICES.b() && !first) {
					for (Peer p : list) {
						if (!before.contains(p.name().toLowerCase(Locale.ROOT))) Msg.say(p.name() + " is now online with the mod.");
					}
				}
			}
			case "pm" -> {
				String from = o.get("from").getAsString();
				String msg = o.get("msg").getAsString();
				convo(from).add(from + ": " + msg);
				UNREAD.add(from.toLowerCase(Locale.ROOT));
				if (Config.PM_IN_CHAT.b()) Msg.say("[PM] " + from + ": " + msg, 0xFF55FF);
				ping(false);
			}
			case "help" -> {
				String from = o.get("from").getAsString();
				String msg = o.has("msg") ? o.get("msg").getAsString() : "";
				String srv = o.has("server") ? o.get("server").getAsString() : "?";
				Msg.say(from + " NEEDS HELP on " + srv + (msg.isBlank() ? "" : ": " + msg), 0xFF2A2A);
				ping(true);
			}
			case "war" -> {
				String action = o.has("action") ? o.get("action").getAsString() : "";
				String from = o.has("from") ? o.get("from").getAsString() : "?";
				if (action.equals("start")) {
					List<String> names = new ArrayList<>();
					if (o.has("enemies")) {
						for (JsonElement e : o.getAsJsonArray("enemies")) names.add(e.getAsString());
					}
					War.onRemoteStart(from, o.has("server") ? o.get("server").getAsString() : "?", names);
					ping(true);
				} else if (action.equals("end")) {
					War.onRemoteEnd(from);
				} else if (action.equals("join")) {
					Msg.say(from + " joined the war.");
				}
			}
			case "deny" -> {
				status = "Denied: " + (o.has("reason") ? o.get("reason").getAsString() : "not allowed");
				nextTry = System.currentTimeMillis() + 120_000;
				disconnect();
			}
			default -> { }
		}
	}

	private static final class Listener implements WebSocket.Listener {
		@Override
		public void onOpen(WebSocket webSocket) {
			webSocket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			BUF.append(data);
			if (last) {
				String text = BUF.toString();
				BUF.setLength(0);
				try {
					JsonObject o = JsonParser.parseString(text).getAsJsonObject();
					Minecraft.getInstance().execute(() -> {
						try {
							handle(o);
						} catch (Exception ex) {
							System.err.println("[RAGE] relay message error: " + ex);
						}
					});
				} catch (Exception ex) {
					System.err.println("[RAGE] bad relay message: " + ex);
				}
			}
			webSocket.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			connected = false;
			socket = null;
			peers = new ArrayList<>();
			status = "Offline";
			nextTry = System.currentTimeMillis() + 10_000;
			return null;
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			connected = false;
			socket = null;
			peers = new ArrayList<>();
			status = "Offline (error)";
			nextTry = System.currentTimeMillis() + 15_000;
		}
	}
}
