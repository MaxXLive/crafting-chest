package net.maxxlive.craftingchest;

import org.jspecify.annotations.Nullable;

/**
 * Holds the chest info received from the server until the client creates the matching crafting menu.
 */
public final class PendingChestInfo {
	private static @Nullable ChestInfoPayload pending;

	private PendingChestInfo() {
	}

	public static void set(ChestInfoPayload info) {
		pending = info;
	}

	public static @Nullable ChestInfoPayload take() {
		ChestInfoPayload info = pending;
		pending = null;
		return info;
	}
}
