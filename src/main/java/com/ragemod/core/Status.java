package com.ragemod.core;

/** The mark a player can have. */
public enum Status {
	NONE("None"),
	TARGET("Target"),
	FRIEND("Friend"),
	ANNOYANCE("Annoyance"),
	ENEMY("Enemy");

	public final String label;

	Status(String label) {
		this.label = label;
	}

	/** RGB color from the config for this status (white for NONE). */
	public int rgb() {
		return switch (this) {
			case TARGET -> Config.TARGET_COLOR.rgb();
			case FRIEND -> Config.FRIEND_COLOR.rgb();
			case ANNOYANCE -> Config.ANNOYANCE_COLOR.rgb();
			case ENEMY -> Config.ENEMY_COLOR.rgb();
			default -> 0xFFFFFF;
		};
	}

	public int argb() {
		return 0xFF000000 | rgb();
	}
}
