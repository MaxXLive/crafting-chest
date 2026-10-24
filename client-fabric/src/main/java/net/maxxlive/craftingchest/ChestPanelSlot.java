package net.maxxlive.craftingchest;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class ChestPanelSlot extends Slot {
	private final CraftingChestMenu menu;

	public ChestPanelSlot(CraftingChestMenu menu, Container container, int slot, int x, int y) {
		super(container, slot, x, y);
		this.menu = menu;
	}

	@Override
	public boolean isActive() {
		return this.menu.craftingchest$isPanelVisible();
	}
}
