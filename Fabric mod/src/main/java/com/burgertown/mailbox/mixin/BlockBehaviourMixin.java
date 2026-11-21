package com.burgertown.mailbox.mixin;

import com.burgertown.mailbox.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	@Inject(method = "isSignalSource", at = @At("HEAD"), cancellable = true)
	private void burgertown$isSignalSource(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (isFenceOrWall()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
	private void burgertown$getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (!isFenceOrWall()) {
			return;
		}
		cir.setReturnValue(mailboxPower(level, pos));
	}

	@Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true)
	private void burgertown$getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (!isFenceOrWall()) {
			return;
		}
		cir.setReturnValue(mailboxPower(level, pos));
	}

	private static int mailboxPower(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos.above()) instanceof MailboxBlockEntity mailbox && mailbox.hasItems() ? 15 : 0;
	}

	private boolean isFenceOrWall() {
		Object self = this;
		return self instanceof FenceBlock || self instanceof WallBlock;
	}
}
