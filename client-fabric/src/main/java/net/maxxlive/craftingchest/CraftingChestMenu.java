package net.maxxlive.craftingchest;

import net.minecraft.network.chat.Component;

/**
 * Implemented by {@link net.minecraft.world.inventory.CraftingMenu} through a mixin.
 */
public interface CraftingChestMenu {
	/** Index of the first chest slot; the vanilla crafting menu uses slots 0-45. */
	int CHEST_SLOT_START = 46;
	int PANEL_WIDTH = 176;

	int craftingchest$getChestRows();

	Component craftingchest$getChestTitle();

	boolean craftingchest$isPanelVisible();

	void craftingchest$setPanelVisible(boolean visible);

	default boolean craftingchest$hasChest() {
		return this.craftingchest$getChestRows() > 0;
	}

	default int craftingchest$getPanelHeight() {
		return this.craftingchest$getChestRows() * 18 + 24;
	}
}
