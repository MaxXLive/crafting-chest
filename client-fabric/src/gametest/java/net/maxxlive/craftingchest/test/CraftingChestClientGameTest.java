package net.maxxlive.craftingchest.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.maxxlive.craftingchest.CraftingChestMenu;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CraftingChestClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();

			BlockPos table = singleplayer.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.blockPosition().offset(2, 0, 0);
			});
			String t = table.getX() + " " + table.getY() + " " + table.getZ();
			String chest = table.getX() + " " + table.getY() + " " + (table.getZ() + 1);
			singleplayer.getServer().runCommand("setblock " + t + " crafting_table");
			singleplayer.getServer().runCommand("setblock " + chest + " chest");
			singleplayer.getServer().runCommand("item replace block " + chest + " container.0 with diamond 5");
			singleplayer.getServer().runCommand("item replace block " + chest + " container.13 with oak_planks 32");

			openCraftingTable(singleplayer, table);
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);

			int clientSlots = context.computeOnClient(mc -> mc.player.containerMenu.slots.size());
			if (clientSlots != CraftingChestMenu.CHEST_SLOT_START + 27) {
				throw new AssertionError("Expected 27 chest slots on client, menu has " + clientSlots + " slots");
			}
			boolean clientSeesDiamonds = context.computeOnClient(mc -> mc.player.containerMenu.slots.get(CraftingChestMenu.CHEST_SLOT_START).getItem().is(Items.DIAMOND));
			if (!clientSeesDiamonds) {
				throw new AssertionError("Client does not see diamonds in first chest slot");
			}
			context.takeScreenshot("crafting_chest_single");

			// Shift-click the diamonds out of the chest into the player inventory.
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.containerMenu.clicked(CraftingChestMenu.CHEST_SLOT_START, 0, ContainerInput.QUICK_MOVE, player);
				player.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			boolean moved = singleplayer.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.getInventory().countItem(Items.DIAMOND) == 5
					&& server.overworld().getBlockEntity(table.south()) instanceof net.minecraft.world.Container c && c.getItem(0).isEmpty();
			});
			if (!moved) {
				throw new AssertionError("Shift-click from chest did not move diamonds into the inventory");
			}

			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(2);

			// Double chest.
			String chest2 = (table.getX() + 1) + " " + table.getY() + " " + (table.getZ() + 1);
			// setblock does not connect chests on its own, so both halves get explicit states.
			singleplayer.getServer().runCommand("setblock " + chest + " chest[facing=north,type=left]");
			singleplayer.getServer().runCommand("setblock " + chest2 + " chest[facing=north,type=right]");
			singleplayer.getServer().runCommand("item replace block " + chest2 + " container.4 with iron_ingot 12");
			openCraftingTable(singleplayer, table);
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);
			int doubleSlots = context.computeOnClient(mc -> mc.player.containerMenu.slots.size());
			if (doubleSlots != CraftingChestMenu.CHEST_SLOT_START + 54) {
				throw new AssertionError("Expected 54 chest slots for a double chest, menu has " + doubleSlots + " slots");
			}
			context.takeScreenshot("crafting_chest_double");
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(2);

			// No chest: plain vanilla menu.
			singleplayer.getServer().runCommand("setblock " + chest + " air");
			singleplayer.getServer().runCommand("setblock " + chest2 + " air");
			openCraftingTable(singleplayer, table);
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);
			int vanillaSlots = context.computeOnClient(mc -> mc.player.containerMenu.slots.size());
			if (vanillaSlots != CraftingChestMenu.CHEST_SLOT_START || !(context.computeOnClient(mc -> mc.player.containerMenu) instanceof CraftingMenu)) {
				throw new AssertionError("Expected vanilla crafting menu without chest, got " + vanillaSlots + " slots");
			}
			context.takeScreenshot("crafting_no_chest");
		}
	}

	private static void openCraftingTable(TestSingleplayerContext singleplayer, BlockPos table) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(server.overworld(), table));
		});
	}
}
