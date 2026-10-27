package com.ragemod.core;

import com.ragemod.net.RelayClient;

/** War mode state. Enemies are players with Status.ENEMY. */
public final class War {
	public static volatile boolean active;
	public static volatile boolean joined;
	public static volatile boolean leading;
	public static volatile String leader = "-";
	public static volatile String server = "-";
	public static volatile String note = "";

	private War() {}

	public static int enemies() {
		return StatusStore.count(Status.ENEMY);
	}

	public static void start() {
		active = true;
		joined = true;
		leading = true;
		leader = Msg.me();
		server = Msg.server();
		note = "War intent sent.";
		RelayClient.sendWar("start");
	}

	public static void end() {
		boolean wasLeading = leading;
		active = false;
		joined = false;
		leading = false;
		note = "War ended.";
		if (wasLeading) RelayClient.sendWar("end");
	}

	public static void join() {
		if (!active) return;
		joined = true;
		note = "Joined the war.";
		RelayClient.sendWar("join");
	}

	/** Called when the leader announces a war over the relay. */
	public static void onRemoteStart(String from, String srv, java.util.List<String> enemyNames) {
		active = true;
		joined = false;
		leading = false;
		leader = from;
		server = srv;
		for (String n : enemyNames) StatusStore.set(n, Status.ENEMY);
		note = from + " started a war. Open the War menu to join.";
		Msg.say(from + " started a war on " + srv + " with " + enemyNames.size() + " enemies. Open the War menu to join.");
	}

	public static void onRemoteEnd(String from) {
		if (!leader.equalsIgnoreCase(from)) return;
		active = false;
		joined = false;
		leading = false;
		note = "War ended by " + from + ".";
		Msg.say("War ended by " + from + ".");
	}
}
