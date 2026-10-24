package net.maxxlive.craftingchest;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent right before the crafting screen opens, so the client can build the same extra chest slots as the server.
 * A size of 0 means there is no chest next to the crafting table.
 */
public record ChestInfoPayload(int size, Component title) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChestInfoPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CraftingChest.MOD_ID, "chest_info"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChestInfoPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ChestInfoPayload::size,
		ComponentSerialization.TRUSTED_STREAM_CODEC, ChestInfoPayload::title,
		ChestInfoPayload::new
	);
	public static final ChestInfoPayload NONE = new ChestInfoPayload(0, Component.empty());

	@Override
	public CustomPacketPayload.Type<ChestInfoPayload> type() {
		return TYPE;
	}
}
