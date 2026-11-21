package com.burgertown.mailbox.client;

import com.burgertown.mailbox.MailboxBlock;
import com.burgertown.mailbox.MailboxBlockEntity;
import com.burgertown.mailbox.MailboxMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class MailboxRenderer implements BlockEntityRenderer<MailboxBlockEntity, MailboxRenderState> {
	private static final float DOOR_PIVOT_X = 8F / 16F;
	private static final float DOOR_PIVOT_Y = 0.8F / 16F;
	private static final float DOOR_PIVOT_Z = 2.4F / 16F;
	private static final float FLAG_PIVOT_X = 3.8F / 16F;
	private static final float FLAG_PIVOT_Y = 6F / 16F;
	private static final float FLAG_PIVOT_Z = 3.2F / 16F;
	private static final Identifier FLAG_TEXTURE = MailboxMod.id("textures/block/mailbox_flag.png");

	@Override
	public MailboxRenderState createRenderState() {
		return new MailboxRenderState();
	}

	@Override
	public void extractRenderState(MailboxBlockEntity blockEntity, MailboxRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
		state.facingYaw = yaw(blockEntity.getBlockState().getValue(MailboxBlock.FACING));
		MailboxBlock block = (MailboxBlock) blockEntity.getBlockState().getBlock();
		state.woodTexture = "block/" + block.wood() + "_planks";
		state.metalTexture = block.metalTexture();
		double now = System.nanoTime() / 1_000_000_000D;
		state.doorAngle = MailboxAnimation.door(blockEntity.doorState, blockEntity.isOpen(), now);
		state.flagAngle = MailboxAnimation.flag(blockEntity.flagState, blockEntity.isFlagRaised(), now);
	}

	@Override
	public void submit(MailboxRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		int light = state.lightCoords;
		String woodTexture = state.woodTexture;
		String metalTexture = state.metalTexture;

		poseStack.pushPose();
		applyFacing(poseStack, state.facingYaw);
		poseStack.translate(DOOR_PIVOT_X, DOOR_PIVOT_Y, DOOR_PIVOT_Z);
		poseStack.rotateDegrees(Axis.XP, -state.doorAngle);
		poseStack.translate(-DOOR_PIVOT_X, -DOOR_PIVOT_Y, -DOOR_PIVOT_Z);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
			(pose, consumer) -> MailboxDoorModel.emit(pose, consumer, light, woodTexture, metalTexture));
		poseStack.popPose();

		poseStack.pushPose();
		applyFacing(poseStack, state.facingYaw);
		poseStack.translate(FLAG_PIVOT_X, FLAG_PIVOT_Y, FLAG_PIVOT_Z);
		poseStack.rotateDegrees(Axis.XP, -state.flagAngle);
		poseStack.translate(-FLAG_PIVOT_X, -FLAG_PIVOT_Y, -FLAG_PIVOT_Z);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(FLAG_TEXTURE),
			(pose, consumer) -> MailboxFlagModel.emit(pose, consumer, light, MailboxFlagModel.FLAG_MODEL));
		poseStack.popPose();

		poseStack.pushPose();
		applyFacing(poseStack, state.facingYaw);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(FLAG_TEXTURE),
			(pose, consumer) -> MailboxFlagModel.emit(pose, consumer, light, MailboxFlagModel.RIVET_MODEL));
		poseStack.popPose();
	}

	private static void applyFacing(PoseStack poseStack, float yaw) {
		poseStack.translate(0.5F, 0F, 0.5F);
		poseStack.rotateDegrees(Axis.YP, -yaw);
		poseStack.translate(-0.5F, 0F, -0.5F);
	}

	private static float yaw(Direction facing) {
		return switch (facing) {
			case EAST -> 90F;
			case SOUTH -> 180F;
			case WEST -> 270F;
			default -> 0F;
		};
	}
}
