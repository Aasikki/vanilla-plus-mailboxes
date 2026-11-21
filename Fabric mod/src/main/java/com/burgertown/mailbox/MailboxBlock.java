package com.burgertown.mailbox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

public class MailboxBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	// Vanilla copper oxidation chance per random tick.
	private static final float OXIDATION_CHANCE = 0.05688889F;
	private final String wood;
	private final String metal;
	private final String metalTexture;

	public MailboxBlock(BlockBehaviour.Properties properties, String wood, String metal, String metalTexture) {
		super(properties);
		this.wood = wood;
		this.metal = metal;
		this.metalTexture = metalTexture;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public String wood() {
		return this.wood;
	}

	public String metal() {
		return this.metal;
	}

	public String metalTexture() {
		return this.metalTexture;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox && mailbox.hasItems() ? 15 : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.getSignal(state, level, pos, direction);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		refreshRedstoneAroundFence(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		refreshRedstoneAroundFence(level, pos);
	}

	// Redstone dust next to the fence only re-checks its connection when the fence's neighbours change shape,
	// so tell the dust directly when a mailbox appears or disappears above the fence.
	private static void refreshRedstoneAroundFence(Level level, BlockPos mailboxPos) {
		BlockPos fencePos = mailboxPos.below();
		level.updateNeighborsAt(fencePos, level.getBlockState(fencePos).getBlock());
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos wirePos = fencePos.relative(direction);
			BlockState wire = level.getBlockState(wirePos);
			if (wire.getBlock() instanceof RedstoneWireBlock) {
				BlockState updated = Block.updateFromNeighbourShapes(wire, level, wirePos);
				Block.updateOrDestroy(wire, updated, level, wirePos, 3);
			}
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return this.nextOxidation() != null;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		String next = this.nextOxidation();
		if (next == null || random.nextFloat() >= OXIDATION_CHANCE) {
			return;
		}
		Block nextBlock = BuiltInRegistries.BLOCK.getValue(MailboxMod.id(MailboxMod.variantName(this.wood, next)));
		replaceBlock(level, pos, state, nextBlock);
	}

	// Swaps the block and carries the stored items over to the new block entity.
	public static void replaceBlock(Level level, BlockPos pos, BlockState oldState, Block newBlock) {
		NonNullList<ItemStack> saved = null;
		if (level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
			saved = mailbox.takeItems();
		}
		level.setBlockAndUpdate(pos, newBlock.defaultBlockState().setValue(FACING, oldState.getValue(FACING)));
		if (saved != null && level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
			mailbox.restoreItems(saved);
		}
	}

	private String nextOxidation() {
		return switch (this.metal) {
			case "copper" -> "exposed_copper";
			case "exposed_copper" -> "weathered_copper";
			case "weathered_copper" -> "oxidized_copper";
			default -> null;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MailboxBlockEntity(pos, state);
	}

	// Only valid on top of a fence or wall.
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(BlockTags.FENCES) || below.is(BlockTags.WALLS);
	}

	// Break the mailbox when the fence/wall under it is removed.
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		if (!this.canSurvive(state, level, pos)) {
			level.destroyBlock(pos, true);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
			player.openMenu(mailbox);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int type, int data) {
		if (level.isClientSide() && level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
			boolean wasOpen = mailbox.isOpen();
			mailbox.setOpenStateFromServer(data >>> 1, (data & 1) == 1);
			if (wasOpen != mailbox.isOpen()) {
				SoundEvent sound = mailbox.isOpen() ? MailboxMod.MAILBOX_OPEN : MailboxMod.MAILBOX_CLOSE;
				level.playLocalSound(pos, sound, SoundSource.BLOCKS, 0.5F, 1F, false);
			}
		}
		return true;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getMailboxShape(state.getValue(FACING).getOpposite());
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getMailboxShape(state.getValue(FACING).getOpposite());
	}

	// Hitbox from hitbox.json (Shaper export, converted from 0-1 to 0-16 units).
	private static final double[][] MODEL_BOXES = {
		{4, 0, 2, 12, 7.2, 14}, {4.8, 7.2, 2, 11.2, 8.8, 14}, {5.6, 8.8, 2, 10.4, 9.6, 14}, {6.4, 9.6, 2, 9.6, 10.4, 14}
	};

	private static VoxelShape getMailboxShape(Direction facing) {
		VoxelShape shape = Shapes.empty();
		for (double[] b : MODEL_BOXES) {
			shape = Shapes.or(shape, part(facing, b[0], b[1], b[2], b[3], b[4], b[5]));
		}
		return shape;
	}

	// Rotates a box in the model's south-facing space to the given facing.
	private static VoxelShape part(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
		double[] a = rotate(facing, x1, z1);
		double[] b = rotate(facing, x2, z2);
		return Block.box(
			Math.min(a[0], b[0]), y1, Math.min(a[1], b[1]),
			Math.max(a[0], b[0]), y2, Math.max(a[1], b[1])
		);
	}

	private static double[] rotate(Direction facing, double x, double z) {
		return switch (facing) {
			case WEST -> new double[]{16 - z, x};
			case NORTH -> new double[]{16 - x, 16 - z};
			case EAST -> new double[]{z, 16 - x};
			default -> new double[]{x, z};
		};
	}
}
