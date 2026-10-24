package net.maxxlive.craftingchest.mixin;

import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.maxxlive.craftingchest.AdjacentChest;
import net.maxxlive.craftingchest.ChestInfoPayload;
import net.maxxlive.craftingchest.ChestPanelSlot;
import net.maxxlive.craftingchest.CraftingChestMenu;
import net.maxxlive.craftingchest.PendingChestInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin extends AbstractCraftingMenu implements CraftingChestMenu {
	@Unique
	private static final int INV_SLOT_START = 10;
	@Unique
	private static final int INV_SLOT_END = 46;

	@Shadow
	@Final
	private Player player;

	@Unique
	private @Nullable Container craftingchest$chest;
	@Unique
	private int craftingchest$chestRows;
	@Unique
	private Component craftingchest$chestTitle = Component.empty();
	@Unique
	private boolean craftingchest$panelVisible = true;

	private CraftingMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
		super(menuType, containerId, width, height);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void craftingchest$addChestSlots(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		Container chest;
		Component title;
		if (this.player instanceof ServerPlayer serverPlayer) {
			// Clients without this mod would get out of sync with the extra slots, so they keep the vanilla menu.
			if (!ServerPlayNetworking.canSend(serverPlayer, ChestInfoPayload.TYPE)) {
				return;
			}
			// evaluate() wraps the result with Optional.of, so a missing chest has to be an empty Optional instead of null.
			AdjacentChest found = access.evaluate((level, pos) -> Optional.ofNullable(AdjacentChest.find(level, pos, serverPlayer))).flatMap(chestOptional -> chestOptional).orElse(null);
			ServerPlayNetworking.send(serverPlayer, found == null ? ChestInfoPayload.NONE : new ChestInfoPayload(found.container().getContainerSize(), found.title()));
			if (found == null) {
				return;
			}
			chest = found.container();
			title = found.title();
			this.craftingchest$chest = chest;
		} else {
			ChestInfoPayload info = PendingChestInfo.take();
			if (info == null || info.size() <= 0) {
				return;
			}
			chest = new SimpleContainer(info.size());
			title = info.title();
		}

		this.craftingchest$chestRows = chest.getContainerSize() / 9;
		this.craftingchest$chestTitle = title;
		for (int row = 0; row < this.craftingchest$chestRows; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new ChestPanelSlot(this, chest, col + row * 9, 8 + col * 18 - PANEL_WIDTH, 18 + row * 18));
			}
		}
	}

	@Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
	private void craftingchest$checkChestStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && this.craftingchest$chest != null && !this.craftingchest$chest.stillValid(player)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void craftingchest$quickMoveFromChest(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		if (slotIndex < CHEST_SLOT_START) {
			return;
		}

		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, true)) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (stack.getCount() == clicked.getCount()) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}

			slot.onTake(player, stack);
		}
		cir.setReturnValue(clicked);
	}

	@Override
	public int craftingchest$getChestRows() {
		return this.craftingchest$chestRows;
	}

	@Override
	public Component craftingchest$getChestTitle() {
		return this.craftingchest$chestTitle;
	}

	@Override
	public boolean craftingchest$isPanelVisible() {
		return this.craftingchest$panelVisible;
	}

	@Override
	public void craftingchest$setPanelVisible(boolean visible) {
		this.craftingchest$panelVisible = visible;
	}
}
