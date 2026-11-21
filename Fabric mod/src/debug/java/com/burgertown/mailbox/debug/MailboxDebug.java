package com.burgertown.mailbox.debug;

import com.burgertown.mailbox.MailboxBlock;
import com.burgertown.mailbox.MailboxBlockEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class MailboxDebug {
	private static boolean chestUiDisabled;
	private static final Set<BlockPos> debugOpen = new HashSet<>();

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("mailboxdebug")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("chestui")
					.then(Commands.literal("off").executes(ctx -> setChestUi(ctx.getSource(), false)))
					.then(Commands.literal("on").executes(ctx -> setChestUi(ctx.getSource(), true))))));

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide() || !chestUiDisabled) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = world.getBlockState(pos);
			if (!(world.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) || !(state.getBlock() instanceof MailboxBlock)) {
				return InteractionResult.PASS;
			}
			boolean open = debugOpen.add(pos);
			if (!open) {
				debugOpen.remove(pos);
			}
			world.blockEvent(pos, state.getBlock(), 1, ((open ? 1 : 0) << 1) | (mailbox.isFlagRaised() ? 1 : 0));
			return InteractionResult.SUCCESS;
		});
	}

	private static int setChestUi(CommandSourceStack source, boolean enabled) {
		chestUiDisabled = !enabled;
		source.sendSuccess(() -> Component.literal("Mailbox chest UI " + (enabled ? "enabled" : "disabled (right-click toggles door only)")), true);
		return 1;
	}
}
