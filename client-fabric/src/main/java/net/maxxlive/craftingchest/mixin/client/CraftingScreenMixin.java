package net.maxxlive.craftingchest.mixin.client;

import net.maxxlive.craftingchest.CraftingChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> {
	@Unique
	private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	@Unique
	private static final int LABEL_COLOR = -12566464;

	private CraftingScreenMixin(CraftingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void craftingchest$drawChestPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (!(this.menu instanceof CraftingChestMenu chestMenu) || !chestMenu.craftingchest$hasChest() || !chestMenu.craftingchest$isPanelVisible()) {
			return;
		}

		int rows = chestMenu.craftingchest$getChestRows();
		int x = this.leftPos - CraftingChestMenu.PANEL_WIDTH;
		int y = this.topPos;
		int topHeight = rows * 18 + 17;
		// Top border, title bar and chest rows, followed by the 7px bottom border of the chest texture.
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y, 0.0F, 0.0F, CraftingChestMenu.PANEL_WIDTH, topHeight, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + topHeight, 0.0F, 215.0F, CraftingChestMenu.PANEL_WIDTH, 7, 256, 256);
		graphics.text(this.font, chestMenu.craftingchest$getChestTitle(), x + 8, y + 6, LABEL_COLOR, false);
	}
}
