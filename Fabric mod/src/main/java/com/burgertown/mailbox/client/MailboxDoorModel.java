package com.burgertown.mailbox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import java.util.List;

final class MailboxDoorModel {
	private static List<float[]> quads;

	private MailboxDoorModel() {
	}

	// Sprites are looked up on every draw, so the door keeps working after resource pack changes.
	static synchronized void emit(PoseStack.Pose pose, VertexConsumer consumer, int light, String woodTexture, String metalTexture) {
		if (quads == null) {
			quads = ModelQuads.build(MailboxResources.readJson("models/block/mailbox_door.json"));
		}
		TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite wood = atlas.getSprite(Identifier.withDefaultNamespace(woodTexture));
		TextureAtlasSprite metal = atlas.getSprite(Identifier.withDefaultNamespace(metalTexture));
		for (float[] quad : quads) {
			TextureAtlasSprite sprite = quad[23] == 1F ? metal : wood;
			float nx = quad[20];
			float ny = quad[21];
			float nz = quad[22];
			for (int i = 0; i < 4; i++) {
				int o = i * 5;
				consumer.addVertex(pose, quad[o], quad[o + 1], quad[o + 2])
					.setColor(-1)
					.setUv(sprite.getU(quad[o + 3]), sprite.getV(quad[o + 4]))
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, nx, ny, nz);
			}
		}
	}
}
