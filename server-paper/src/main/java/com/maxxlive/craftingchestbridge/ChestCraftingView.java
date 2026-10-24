package com.maxxlive.craftingchestbridge;

import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;

/**
 * Inventory view of a {@link ChestCraftingMenu}: raw slots below {@link ChestCraftingMenu#CHEST_SLOT_START}
 * behave like a normal crafting table, the slots after that map to the chest inventory.
 */
public final class ChestCraftingView extends CraftInventoryView<ChestCraftingMenu, CraftInventoryCrafting> {
	private static final int HOTBAR_START = 37;

	private final Inventory chestInventory;

	public ChestCraftingView(HumanEntity player, CraftInventoryCrafting crafting, ChestCraftingMenu menu, Inventory chestInventory) {
		super(player, crafting, menu);
		this.chestInventory = chestInventory;
	}

	private static boolean isChestSlot(int rawSlot) {
		return rawSlot >= ChestCraftingMenu.CHEST_SLOT_START;
	}

	@Override
	public int countSlots() {
		return ChestCraftingMenu.CHEST_SLOT_START + this.chestInventory.getSize();
	}

	@Override
	public Inventory mapValidSlotToInventory(int rawSlot) {
		return isChestSlot(rawSlot) ? this.chestInventory : super.mapValidSlotToInventory(rawSlot);
	}

	@Override
	public int convertSlot(int rawSlot) {
		return isChestSlot(rawSlot) ? rawSlot - ChestCraftingMenu.CHEST_SLOT_START : super.convertSlot(rawSlot);
	}

	@Override
	public SlotType getSlotType(int slot) {
		if (isChestSlot(slot)) {
			return SlotType.CONTAINER;
		}
		// The super implementation finds the hotbar via countSlots(), which now includes the chest.
		if (slot >= HOTBAR_START) {
			return SlotType.QUICKBAR;
		}
		return super.getSlotType(slot);
	}
}
