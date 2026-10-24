package com.maxxlive.craftingchestbridge;

import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The vanilla crafting menu plus the slots of an adjacent chest, laid out exactly like the client mod expects:
 * slots 0-45 are the vanilla crafting menu, slots 46+ belong to the chest.
 */
public final class ChestCraftingMenu extends CraftingMenu {
	public static final int CHEST_SLOT_START = 46;
	private static final int PANEL_WIDTH = 176;
	private static final int INV_SLOT_START = 10;
	private static final int INV_SLOT_END = 46;

	private final ServerPlayer player;
	private final Container chest;
	private final Component chestTitle;
	private @Nullable CraftInventoryView<?, ?> view;

	public ChestCraftingMenu(int containerId, Inventory inventory, ContainerLevelAccess access, ServerPlayer player, Container chest, Component chestTitle) {
		super(containerId, inventory, access);
		this.player = player;
		this.chest = chest;
		this.chestTitle = chestTitle;
		int rows = chest.getContainerSize() / 9;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(chest, col + row * 9, 8 + col * 18 - PANEL_WIDTH, 18 + row * 18));
			}
		}
	}

	public Container chest() {
		return this.chest;
	}

	public Component chestTitle() {
		return this.chestTitle;
	}

	@Override
	public boolean stillValid(Player player) {
		return super.stillValid(player) && this.chest.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < CHEST_SLOT_START) {
			return super.quickMoveStack(player, slotIndex);
		}

		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, true)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (stack.getCount() == clicked.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, stack);
		}
		return clicked;
	}

	/**
	 * Paper builds click and drag events from this view, so it has to know about the chest slots,
	 * otherwise a click on them would throw and kick the player.
	 */
	@Override
	public CraftInventoryView getBukkitView() {
		if (this.view == null) {
			CraftInventoryCrafting crafting = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
			this.view = new ChestCraftingView(this.player.getBukkitEntity(), crafting, this, new CraftInventory(this.chest));
		}
		return this.view;
	}
}
