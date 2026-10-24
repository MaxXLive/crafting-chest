package net.maxxlive.craftingchest.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.maxxlive.craftingchest.CraftingChestMenu;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Connects to a running Paper server with CraftingChestPaperBridge. Only runs when CRAFTINGCHEST_PAPER_DIR points
 * to the server directory, whose console reads commands from cmds.txt.
 */
public class PaperBridgeClientGameTest implements FabricClientGameTest {
	private static final String ADDRESS = "localhost:25599";

	@Override
	public void runTest(ClientGameTestContext context) {
		String serverDir = System.getenv("CRAFTINGCHEST_PAPER_DIR");
		if (serverDir == null) {
			return;
		}
		Path commands = Path.of(serverDir, "cmds.txt");

		context.runOnClient(mc -> ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(ADDRESS),
			new ServerData("Paper", ADDRESS, ServerData.Type.OTHER), false, null));
		context.waitFor(mc -> mc.player != null && mc.level != null && mc.gui.screen() == null || mc.gui.screen() instanceof DisconnectedScreen, 20 * 120);
		if (context.computeOnClient(mc -> mc.gui.screen() instanceof DisconnectedScreen)) {
			throw new AssertionError("Paper server refused the connection");
		}
		context.waitTicks(60);

		BlockPos table = context.computeOnClient(mc -> mc.player.blockPosition().offset(2, 0, 0));
		String chest = table.getX() + " " + table.getY() + " " + (table.getZ() + 1);
		command(commands, "clear @a");
		command(commands, "setblock " + table.getX() + " " + table.getY() + " " + table.getZ() + " crafting_table");
		command(commands, "setblock " + chest + " chest");
		command(commands, "item replace block " + chest + " container.0 with diamond 5");
		command(commands, "item replace block " + chest + " container.13 with oak_planks 32");
		context.waitTicks(40);

		useBlock(context, table);
		context.waitForScreen(CraftingScreen.class);
		context.waitTicks(10);
		int slots = context.computeOnClient(mc -> mc.player.containerMenu.slots.size());
		if (slots != CraftingChestMenu.CHEST_SLOT_START + 27) {
			throw new AssertionError("Expected 27 chest slots from Paper, menu has " + slots + " slots");
		}
		if (!context.computeOnClient(mc -> mc.player.containerMenu.slots.get(CraftingChestMenu.CHEST_SLOT_START).getItem().is(Items.DIAMOND))) {
			throw new AssertionError("Client does not see the diamonds from the Paper chest");
		}
		context.takeScreenshot("paper_crafting_chest");

		// A real click packet: shift-click the diamonds, then put one plank from the chest into the crafting grid.
		context.runOnClient(mc -> {
			int id = mc.player.containerMenu.containerId;
			mc.gameMode.handleContainerInput(id, CraftingChestMenu.CHEST_SLOT_START, 0, ContainerInput.QUICK_MOVE, mc.player);
			mc.gameMode.handleContainerInput(id, CraftingChestMenu.CHEST_SLOT_START + 13, 0, ContainerInput.PICKUP, mc.player);
			mc.gameMode.handleContainerInput(id, 1, 1, ContainerInput.PICKUP, mc.player);
			mc.gameMode.handleContainerInput(id, CraftingChestMenu.CHEST_SLOT_START + 13, 0, ContainerInput.PICKUP, mc.player);
		});
		context.waitTicks(20);
		context.takeScreenshot("paper_after_clicks");

		// Reopen, so everything shown comes fresh from the server.
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(10);
		useBlock(context, table);
		context.waitForScreen(CraftingScreen.class);
		context.waitTicks(10);
		boolean chestEmptied = context.computeOnClient(mc -> mc.player.containerMenu.slots.get(CraftingChestMenu.CHEST_SLOT_START).getItem().isEmpty());
		int diamonds = context.computeOnClient(mc -> mc.player.getInventory().countItem(Items.DIAMOND));
		int planksInChest = context.computeOnClient(mc -> mc.player.containerMenu.slots.get(CraftingChestMenu.CHEST_SLOT_START + 13).getItem().getCount());
		int planksInInventory = context.computeOnClient(mc -> mc.player.getInventory().countItem(Items.OAK_PLANKS));
		if (!chestEmptied || diamonds != 5) {
			throw new AssertionError("Shift-click on Paper failed: chest emptied=" + chestEmptied + ", diamonds in inventory=" + diamonds);
		}
		// The plank in the crafting grid goes back to the inventory when the table is closed.
		if (planksInChest != 31 || planksInInventory != 1) {
			throw new AssertionError("Expected 31 planks in chest and 1 in inventory, got " + planksInChest + " and " + planksInInventory);
		}
		context.takeScreenshot("paper_reopened");

		context.runOnClient(mc -> mc.player.closeContainer());
		context.runOnClient(mc -> mc.disconnect(new TitleScreen(), false));
		context.waitForScreen(TitleScreen.class);
	}

	private static void useBlock(ClientGameTestContext context, BlockPos pos) {
		context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
	}

	private static void command(Path commands, String command) {
		try {
			Files.writeString(commands, command + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
