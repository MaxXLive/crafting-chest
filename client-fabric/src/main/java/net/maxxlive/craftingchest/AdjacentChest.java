package net.maxxlive.craftingchest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record AdjacentChest(Container container, Component title) {
	private static final int MAX_SIZE = 54;

	/**
	 * Finds a chest (single or double, incl. trapped and copper chests) or barrel directly next to the crafting table.
	 */
	public static @Nullable AdjacentChest find(Level level, BlockPos craftingTablePos, Player player) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos pos = craftingTablePos.relative(direction);
			BlockState state = level.getBlockState(pos);
			Container container = null;
			if (state.getBlock() instanceof ChestBlock chestBlock) {
				container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
			} else if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
				container = barrel;
			}

			if (container == null || container.getContainerSize() % 9 != 0 || container.getContainerSize() > MAX_SIZE) {
				continue;
			}
			if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity && !blockEntity.canOpen(player)) {
				continue;
			}

			MenuProvider menuProvider = state.getMenuProvider(level, pos);
			Component title = menuProvider != null ? menuProvider.getDisplayName() : Component.translatable("container.chest");
			return new AdjacentChest(container, title);
		}
		return null;
	}
}
