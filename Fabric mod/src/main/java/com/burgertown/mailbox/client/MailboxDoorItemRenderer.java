package com.burgertown.mailbox.client;

import com.burgertown.mailbox.MailboxBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class MailboxDoorItemRenderer implements SpecialModelRenderer<MailboxDoorItemRenderer.Textures> {
	public record Textures(String woodTexture, String metalTexture) {
	}

	@Override
	public void submit(Textures textures, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean glint, int color) {
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
			(pose, consumer) -> MailboxDoorModel.emit(pose, consumer, light, textures.woodTexture(), textures.metalTexture()));
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0F, 0F, 0F));
		output.accept(new Vector3f(1F, 1F, 1F));
	}

	@Override
	public Textures extractArgument(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MailboxBlock block) {
			return new Textures("block/" + block.wood() + "_planks", block.metalTexture());
		}
		return new Textures("block/oak_planks", "block/iron_block");
	}

	public static final class Unbaked implements SpecialModelRenderer.Unbaked<Textures> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public SpecialModelRenderer<Textures> bake(SpecialModelRenderer.BakingContext context) {
			return new MailboxDoorItemRenderer();
		}

		@Override
		public MapCodec<? extends SpecialModelRenderer.Unbaked<Textures>> type() {
			return MAP_CODEC;
		}
	}
}
