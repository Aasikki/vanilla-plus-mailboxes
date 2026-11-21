package com.burgertown.mailbox;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class MailboxSupport {
	public static final String UNSUPPORTED_MESSAGE = "The Vanilla+ Mailboxes mod is not installed on this server, so the mailbox is disabled.";

	private static boolean serverHasMod;

	private MailboxSupport() {
	}

	public static boolean isSupported() {
		return serverHasMod;
	}

	public static void setServerHasMod(boolean supported) {
		serverHasMod = supported;
	}

	public static boolean isMailboxItem(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MailboxBlock;
	}
}
