package com.burgertown.mailbox;

import com.burgertown.mailbox.client.MailboxDoorItemRenderer;
import com.burgertown.mailbox.client.MailboxRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class MailboxClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRendererRegistry.register(MailboxMod.MAILBOX_BLOCK_ENTITY, context -> new MailboxRenderer());
		SpecialModelRenderers.ID_MAPPER.put(MailboxMod.id("mailbox_door"), MailboxDoorItemRenderer.Unbaked.MAP_CODEC);

		ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> MailboxSupport.setServerHasMod(true));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MailboxSupport.setServerHasMod(false));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MailboxSupport.setServerHasMod(false));

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!world.isClientSide() || MailboxSupport.isSupported()) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (MailboxSupport.isMailboxItem(stack) || state.getBlock() instanceof MailboxBlock) {
				player.sendSystemMessage(Component.literal(MailboxSupport.UNSUPPORTED_MESSAGE));
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}
}
