package net.maxxlive.craftingchest.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.maxxlive.craftingchest.ChestInfoPayload;
import net.maxxlive.craftingchest.PendingChestInfo;

public class CraftingChestClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ChestInfoPayload.TYPE, (payload, context) -> PendingChestInfo.set(payload));
	}
}
