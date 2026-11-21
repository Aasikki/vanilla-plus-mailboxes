package com.burgertown.mailbox.mixin;

import com.burgertown.mailbox.MailboxSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Servers without the mod can't decode mailbox items, so the client must not send them.
@Mixin(Connection.class)
public abstract class ConnectionMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void burgertown$blockUnsupportedMailboxItems(Packet<?> packet, CallbackInfo ci) {
		if (packet instanceof ServerboundSetCreativeModeSlotPacket slot
			&& !MailboxSupport.isSupported()
			&& MailboxSupport.isMailboxItem(slot.itemStack())) {
			ci.cancel();
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null) {
				minecraft.player.sendSystemMessage(Component.literal(MailboxSupport.UNSUPPORTED_MESSAGE));
			}
		}
	}
}
