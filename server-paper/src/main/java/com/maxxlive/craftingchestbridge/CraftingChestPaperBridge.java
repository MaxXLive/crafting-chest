package com.maxxlive.craftingchestbridge;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replaces the crafting table menu of players running the MX Crafting Chest mod with a {@link ChestCraftingMenu}
 * whenever a chest or barrel stands next to the crafting table.
 */
public final class CraftingChestPaperBridge extends JavaPlugin implements Listener {
	/** Must match {@code ChestInfoPayload.TYPE} of the client mod. */
	private static final Identifier CHEST_INFO = Identifier.fromNamespaceAndPath("craftingchest", "chest_info");
	private static final Component CRAFTING_TITLE = Component.translatable("container.crafting");
	private static final int MAX_CHEST_SIZE = 54;

	private boolean respectProtection;

	private record AdjacentChest(Container container, Component title) {
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		respectProtection = getConfig().getBoolean("respect-protection", true);
		getServer().getMessenger().registerOutgoingPluginChannel(this, CHEST_INFO.toString());
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCraftingTableOpen(InventoryOpenEvent event) {
		if (!(event.getPlayer() instanceof Player bukkitPlayer)
				|| !(event.getView() instanceof CraftInventoryView<?, ?> view)
				// only the plain vanilla menu, not ours and not ones from other plugins
				|| view.getHandle().getClass() != CraftingMenu.class
				|| !bukkitPlayer.getListeningPluginChannels().contains(CHEST_INFO.toString())) {
			return;
		}

		CraftingMenu craftingMenu = (CraftingMenu) view.getHandle();
		ServerPlayer player = ((CraftPlayer) bukkitPlayer).getHandle();
		// evaluate() wraps the result with Optional.of, so "no crafting table" has to be an empty Optional instead of null
		BlockPos tablePos = craftingMenu.access.evaluate((level, pos) -> Optional.ofNullable(level.getBlockState(pos).is(Blocks.CRAFTING_TABLE) ? pos : null))
				.flatMap(posOptional -> posOptional).orElse(null);
		if (tablePos == null || findAdjacentChest(player, tablePos) == null) {
			return;
		}

		// The vanilla menu is already built at this point, so it is swapped for ours on the next tick.
		event.setCancelled(true);
		ServerLevel level = player.level();
		Bukkit.getScheduler().runTask(this, () -> {
			if (!bukkitPlayer.isOnline() || player.containerMenu != player.inventoryMenu || player.level() != level) {
				return;
			}
			AdjacentChest chest = findAdjacentChest(player, tablePos);
			if (chest == null) {
				return;
			}
			ContainerLevelAccess access = ContainerLevelAccess.create(level, tablePos);
			player.openMenu(new SimpleMenuProvider(
					(containerId, inventory, $) -> new ChestCraftingMenu(containerId, inventory, access, player, chest.container(), chest.title()),
					CRAFTING_TITLE));
		});
	}

	/**
	 * Runs after every other plugin had its say, right before the open screen packet is sent,
	 * so the client only learns about the chest slots if the menu really opens.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChestCraftingMenuOpen(InventoryOpenEvent event) {
		if (!(event.getView() instanceof CraftInventoryView<?, ?> view) || !(view.getHandle() instanceof ChestCraftingMenu menu)) {
			return;
		}
		ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
		sendChestInfo(player, menu.chest().getContainerSize(), menu.chestTitle());
	}

	private @Nullable AdjacentChest findAdjacentChest(ServerPlayer player, BlockPos tablePos) {
		ServerLevel level = player.level();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos pos = tablePos.relative(direction);
			BlockState state = level.getBlockState(pos);
			Container container = null;
			if (state.getBlock() instanceof ChestBlock chestBlock) {
				container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
			} else if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
				container = barrel;
			}

			if (container == null || container.getContainerSize() % 9 != 0 || container.getContainerSize() > MAX_CHEST_SIZE) {
				continue;
			}
			if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity && !blockEntity.canOpen(player)) {
				continue;
			}
			if (respectProtection && !mayOpen(player, pos)) {
				continue;
			}

			MenuProvider menuProvider = state.getMenuProvider(level, pos);
			Component title = menuProvider != null ? menuProvider.getDisplayName() : Component.translatable("container.chest");
			return new AdjacentChest(container, title);
		}
		return null;
	}

	/**
	 * Asks protection plugins whether the player may right-click the chest, without an item in hand
	 * so that item-specific listeners stay out of it.
	 */
	private boolean mayOpen(ServerPlayer player, BlockPos pos) {
		PlayerInteractEvent interact = new PlayerInteractEvent(player.getBukkitEntity(), Action.RIGHT_CLICK_BLOCK, null,
				CraftBlock.at(player.level(), pos), BlockFace.UP, EquipmentSlot.HAND);
		Bukkit.getPluginManager().callEvent(interact);
		return interact.useInteractedBlock() != Event.Result.DENY;
	}

	/**
	 * Sends the packet directly instead of using {@code Player#sendPluginMessage}, matching the
	 * {@code ChestInfoPayload} codec of the client mod: VarInt size followed by the title component.
	 */
	private static void sendChestInfo(ServerPlayer player, int size, Component title) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.level().registryAccess());
		try {
			ByteBufCodecs.VAR_INT.encode(buf, size);
			ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, title);
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			player.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(CHEST_INFO, bytes)));
		} finally {
			buf.release();
		}
	}
}
