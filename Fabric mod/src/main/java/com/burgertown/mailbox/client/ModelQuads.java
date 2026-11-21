package com.burgertown.mailbox.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Builds quads from a Blockbench block model. Each quad is 4 vertices of (x, y, z, u, v) followed by the normal,
// then the texture key from the model (for example 0 or 1) so the caller can pick the texture at draw time.
// UVs are stored as 0-1 fractions of the texture, so they can be mapped onto any atlas sprite.
final class ModelQuads {
	static final int QUAD_SIZE = 24;

	private ModelQuads() {
	}

	static List<float[]> build(JsonObject root) {
		List<float[]> result = new ArrayList<>();
		for (JsonElement elementJson : root.getAsJsonArray("elements")) {
			JsonObject element = elementJson.getAsJsonObject();
			float[] from = vector(element.getAsJsonArray("from"));
			float[] to = vector(element.getAsJsonArray("to"));
			for (Map.Entry<String, JsonElement> faceEntry : element.getAsJsonObject("faces").entrySet()) {
				JsonObject face = faceEntry.getValue().getAsJsonObject();
				String textureKey = face.get("texture").getAsString().substring(1);
				float[] uv = vector(face.getAsJsonArray("uv"));
				result.add(quad(faceEntry.getKey(), from, to, uv, Float.parseFloat(textureKey)));
			}
		}
		return result;
	}

	private static float[] quad(String face, float[] from, float[] to, float[] uv, float textureKey) {
		float x1 = from[0] / 16F, y1 = from[1] / 16F, z1 = from[2] / 16F;
		float x2 = to[0] / 16F, y2 = to[1] / 16F, z2 = to[2] / 16F;
		float[][] pos;
		float nx = 0, ny = 0, nz = 0;
		switch (face) {
			case "north" -> {
				pos = new float[][]{{x2, y1, z1}, {x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}};
				nz = -1;
			}
			case "south" -> {
				pos = new float[][]{{x1, y1, z2}, {x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}};
				nz = 1;
			}
			case "east" -> {
				pos = new float[][]{{x2, y1, z2}, {x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}};
				nx = 1;
			}
			case "west" -> {
				pos = new float[][]{{x1, y1, z1}, {x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}};
				nx = -1;
			}
			case "up" -> {
				pos = new float[][]{{x1, y2, z2}, {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}};
				ny = 1;
			}
			default -> {
				pos = new float[][]{{x1, y1, z1}, {x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}};
				ny = -1;
			}
		}
		float[][] corners = {
			{uv[0] / 16F, uv[3] / 16F},
			{uv[0] / 16F, uv[1] / 16F},
			{uv[2] / 16F, uv[1] / 16F},
			{uv[2] / 16F, uv[3] / 16F}
		};
		float[] out = new float[QUAD_SIZE];
		for (int i = 0; i < 4; i++) {
			int o = i * 5;
			out[o] = pos[i][0];
			out[o + 1] = pos[i][1];
			out[o + 2] = pos[i][2];
			out[o + 3] = corners[i][0];
			out[o + 4] = corners[i][1];
		}
		out[20] = nx;
		out[21] = ny;
		out[22] = nz;
		out[23] = textureKey;
		return out;
	}

	static float[] vector(JsonArray array) {
		float[] values = new float[array.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = array.get(i).getAsFloat();
		}
		return values;
	}
}
