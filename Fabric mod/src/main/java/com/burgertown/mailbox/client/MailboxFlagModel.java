package com.burgertown.mailbox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MailboxFlagModel {
	static final String FLAG_MODEL = "models/block/mailbox_flag.json";
	static final String RIVET_MODEL = "models/block/mailbox_flag_rivet.json";

	private static final Map<String, List<float[]>> CACHE = new HashMap<>();

	private MailboxFlagModel() {
	}

	static synchronized void emit(PoseStack.Pose pose, VertexConsumer consumer, int light, String model) {
		List<float[]> quads = CACHE.computeIfAbsent(model, key -> ModelQuads.build(MailboxResources.readJson(key)));
		for (float[] quad : quads) {
			float nx = quad[20];
			float ny = quad[21];
			float nz = quad[22];
			for (int i = 0; i < 4; i++) {
				int o = i * 5;
				consumer.addVertex(pose, quad[o], quad[o + 1], quad[o + 2])
					.setColor(-1)
					.setUv(quad[o + 3], quad[o + 4])
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, nx, ny, nz);
			}
		}
	}
}
