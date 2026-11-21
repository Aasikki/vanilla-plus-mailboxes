package com.burgertown.mailbox.mixin;

import com.burgertown.mailbox.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {
	@Inject(method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
	private static void burgertown$shouldConnectTo(BlockState state, BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (!(state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock)) {
			return;
		}
		if (!(level.getBlockEntity(pos.above()) instanceof MailboxBlockEntity)) {
			cir.setReturnValue(false);
		}
	}
}
