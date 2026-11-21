package com.burgertown.mailbox;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MailboxMod implements ModInitializer {
	public static final String MOD_ID = "vanilla_plus_mailboxes";

	public static final List<String> WOODS = List.of(
		"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped"
	);
	public static final List<String> COPPER_STAGES = List.of("copper", "exposed_copper", "weathered_copper", "oxidized_copper");
	public static final Map<String, String> SCRAPE_STEPS = Map.of(
		"exposed_copper", "copper", "weathered_copper", "exposed_copper", "oxidized_copper", "weathered_copper"
	);
	public static final List<String> METALS = List.of(
		"diamond", "gold", "iron", "netherite",
		"copper", "exposed_copper", "weathered_copper", "oxidized_copper",
		"waxed_copper", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper"
	);

	public static String textureFor(String metal) {
		return switch (metal) {
			case "copper", "waxed_copper" -> "block/copper_block";
			case "exposed_copper", "waxed_exposed_copper" -> "block/exposed_copper";
			case "weathered_copper", "waxed_weathered_copper" -> "block/weathered_copper";
			case "oxidized_copper", "waxed_oxidized_copper" -> "block/oxidized_copper";
			default -> "block/" + metal + "_block";
		};
	}

	public static final List<MailboxBlock> MAILBOX_BLOCKS = new ArrayList<>();
	public static final List<Item> MAILBOX_ITEMS = new ArrayList<>();

	static {
		for (String wood : WOODS) {
			for (String metal : METALS) {
				Identifier id = id(variantName(wood, metal));
				ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
				ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
				// Wooden chest hardness (2.5) so it breaks like wood and is sped up by axes via the mineable/axe tag.
				MailboxBlock block = Registry.register(
					BuiltInRegistries.BLOCK,
					blockKey,
					new MailboxBlock(BlockBehaviour.Properties.of()
						.setId(blockKey)
						.strength(2.5F)
						.sound(SoundType.WOOD)
						.noOcclusion(), wood, metal, textureFor(metal))
				);
				Item item = Registry.register(
					BuiltInRegistries.ITEM,
					itemKey,
					new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())
				);
				MAILBOX_BLOCKS.add(block);
				MAILBOX_ITEMS.add(item);
			}
		}
	}

	public static final BlockEntityType<MailboxBlockEntity> MAILBOX_BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("mailbox"),
		FabricBlockEntityTypeBuilder.create(MailboxBlockEntity::new, MAILBOX_BLOCKS.toArray(new Block[0])).build()
	);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static final SoundEvent MAILBOX_OPEN = Registry.register(
		BuiltInRegistries.SOUND_EVENT, id("mailbox.open"), SoundEvent.createVariableRangeEvent(id("mailbox.open"))
	);
	public static final SoundEvent MAILBOX_CLOSE = Registry.register(
		BuiltInRegistries.SOUND_EVENT, id("mailbox.close"), SoundEvent.createVariableRangeEvent(id("mailbox.close"))
	);

	public static String variantName(String wood, String metal) {
		return "mailbox_" + wood + "_" + metal;
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ServerPlayNetworking.send(handler.getPlayer(), new HandshakePayload()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(output -> MAILBOX_ITEMS.forEach(output::accept));

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			BlockPos pos = hit.getBlockPos();
			BlockState state = world.getBlockState(pos);
			if (!(state.getBlock() instanceof MailboxBlock block)) {
				return InteractionResult.PASS;
			}
			if (player.isShiftKeyDown() && stack.is(Items.HONEYCOMB) && COPPER_STAGES.contains(block.metal())) {
				MailboxBlock.replaceBlock(world, pos, state, variantBlock(block.wood(), "waxed_" + block.metal()));
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				world.levelEvent(3003, pos, 0);
				world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1F, 1F);
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown() && stack.is(item -> item.is(ItemTags.AXES)) && block.metal().startsWith("waxed_")) {
				MailboxBlock.replaceBlock(world, pos, state, variantBlock(block.wood(), block.metal().substring("waxed_".length())));
				stack.hurtAndBreak(1, player, hand);
				world.levelEvent(3004, pos, 0);
				world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1F, 1F);
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown() && stack.is(item -> item.is(ItemTags.AXES)) && SCRAPE_STEPS.containsKey(block.metal())) {
				MailboxBlock.replaceBlock(world, pos, state, variantBlock(block.wood(), SCRAPE_STEPS.get(block.metal())));
				stack.hurtAndBreak(1, player, hand);
				world.levelEvent(3005, pos, 0);
				world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1F, 1F);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static Block variantBlock(String wood, String metal) {
		return BuiltInRegistries.BLOCK.getValue(id(variantName(wood, metal)));
	}
}
