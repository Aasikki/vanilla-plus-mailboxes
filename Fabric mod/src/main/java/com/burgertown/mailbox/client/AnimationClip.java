package com.burgertown.mailbox.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeMap;

final class AnimationClip {
	private final float length;
	private final TreeMap<Float, Key> keys;

	private AnimationClip(float length, TreeMap<Float, Key> keys) {
		this.length = length;
		this.keys = keys;
	}

	static AnimationClip parse(JsonObject animation, String bone) {
		float length = animation.get("animation_length").getAsFloat();
		TreeMap<Float, Key> keys = new TreeMap<>();
		JsonObject rotation = animation.getAsJsonObject("bones").getAsJsonObject(bone).getAsJsonObject("rotation");
		for (Map.Entry<String, JsonElement> entry : rotation.entrySet()) {
			keys.put(Float.parseFloat(entry.getKey()), Key.parse(entry.getValue()));
		}
		return new AnimationClip(length, keys);
	}

	float length() {
		return this.length;
	}

	float sample(float seconds) {
		float t = Math.min(Math.max(seconds, 0F), this.length);
		Map.Entry<Float, Key> before = this.keys.floorEntry(t);
		Map.Entry<Float, Key> after = this.keys.higherEntry(t);
		if (before == null) {
			return after == null ? 0.0F : after.getValue().value;
		}
		if (after == null) {
			return before.getValue().value;
		}
		float span = after.getKey() - before.getKey();
		float progress = (t - before.getKey()) / span;
		float eased = Easing.apply(before.getValue().easing, progress);
		return before.getValue().value + (after.getValue().value - before.getValue().value) * eased;
	}

	// Finds the time at which this clip reaches the given angle, so playback can continue from there.
	float timeFor(float angle) {
		boolean increasing = sample(this.length) >= sample(0F);
		float low = 0F;
		float high = this.length;
		for (int i = 0; i < 32; i++) {
			float mid = (low + high) / 2F;
			if ((sample(mid) < angle) == increasing) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return (low + high) / 2F;
	}

	private record Key(float value, String easing) {
		static Key parse(JsonElement element) {
			if (element.isJsonArray()) {
				return new Key(element.getAsJsonArray().get(0).getAsFloat(), "linear");
			}
			JsonObject object = element.getAsJsonObject();
			JsonElement post = object.has("post") ? object.get("post") : object.get("vector");
			String easing = object.has("easing") ? object.get("easing").getAsString() : "linear";
			return new Key(post.getAsJsonArray().get(0).getAsFloat(), easing);
		}
	}

	private static final class Easing {
		private Easing() {
		}

		static float apply(String name, float x) {
			return switch (name) {
				case "easeInSine" -> 1 - (float) Math.cos(x * Math.PI / 2);
				case "easeOutSine" -> (float) Math.sin(x * Math.PI / 2);
				case "easeInOutSine" -> -((float) Math.cos(Math.PI * x) - 1) / 2;
				case "easeInQuad" -> x * x;
				case "easeOutQuad" -> 1 - (1 - x) * (1 - x);
				case "easeInOutQuad" -> x < 0.5F ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
				case "easeInCubic" -> x * x * x;
				case "easeOutCubic" -> 1 - (float) Math.pow(1 - x, 3);
				case "easeInOutCubic" -> x < 0.5F ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
				default -> x;
			};
		}
	}
}
