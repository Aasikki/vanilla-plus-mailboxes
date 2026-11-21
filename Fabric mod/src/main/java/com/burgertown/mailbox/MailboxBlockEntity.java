package com.burgertown.mailbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MailboxBlockEntity extends BaseContainerBlockEntity {
	// Hopper-sized inventory: 5 slots, noticeably smaller than a chest.
	private static final int SLOTS = 5;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private int openCount;
	private boolean flagRaised;
	public final AnimationState doorState = new AnimationState();
	public final AnimationState flagState = new AnimationState();

	public MailboxBlockEntity(BlockPos pos, BlockState state) {
		super(MailboxMod.MAILBOX_BLOCK_ENTITY, pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.vanilla_plus_mailboxes.mailbox");
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (this.getLevel() != null) {
			Containers.dropContents(this.getLevel(), pos, this);
		}
		super.preRemoveSideEffects(pos, state);
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new HopperMenu(containerId, inventory, this);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		output.putBoolean("FlagRaised", this.flagRaised);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.flagRaised = input.getBooleanOr("FlagRaised", false);
	}

	@Override
	public void startOpen(ContainerUser user) {
		this.openCount++;
		this.signalOpenCount();
	}

	@Override
	public void stopOpen(ContainerUser user) {
		this.openCount = Math.max(0, this.openCount - 1);
		if (this.openCount == 0) {
			// The flag only changes when the mailbox is closed.
			this.flagRaised = this.hasItems();
			this.setChanged();
		}
		this.signalOpenCount();
	}

	// Sends the open count and flag state to clients: (openCount << 1) | flagBit.
	private void signalOpenCount() {
		if (this.level != null && !this.level.isClientSide()) {
			int data = (this.openCount << 1) | (this.flagRaised ? 1 : 0);
			this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, data);
		}
	}

	public NonNullList<ItemStack> takeItems() {
		NonNullList<ItemStack> taken = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < SLOTS; i++) {
			taken.set(i, this.items.get(i).copy());
		}
		this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		return taken;
	}

	public void restoreItems(NonNullList<ItemStack> saved) {
		this.items = saved;
		this.flagRaised = this.hasItems();
		this.setChanged();
	}

	public void setOpenStateFromServer(int openCount, boolean flagRaised) {
		this.openCount = openCount;
		this.flagRaised = flagRaised;
	}

	public boolean isOpen() {
		return this.openCount > 0;
	}

	public boolean isFlagRaised() {
		return this.flagRaised;
	}

	public boolean hasItems() {
		for (ItemStack stack : this.items) {
			if (!stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.updateNeighborsAt(this.worldPosition.below(), this.getBlockState().getBlock());
			this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
