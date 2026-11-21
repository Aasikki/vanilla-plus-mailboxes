package com.burgertown.mailbox.client;

import com.burgertown.mailbox.AnimationState;
import com.google.gson.JsonObject;

final class MailboxAnimation {
	private static AnimationClip doorOpen;
	private static AnimationClip doorClose;
	private static AnimationClip flagUp;
	private static AnimationClip flagDown;

	private MailboxAnimation() {
	}

	static float door(AnimationState state, boolean open, double nowSeconds) {
		load();
		return advance(state, doorOpen, doorClose, open, nowSeconds);
	}

	static float flag(AnimationState state, boolean raised, double nowSeconds) {
		load();
		return advance(state, flagUp, flagDown, raised, nowSeconds);
	}

	// Plays the clip for the current target. If the target flips mid-animation, playback continues
	// from the same angle on the opposite clip, so the motion reverses instead of jumping.
	// Time is real-time seconds, so server tick corrections can't make the motion stutter.
	private static float advance(AnimationState state, AnimationClip forward, AnimationClip backward, boolean target, double nowSeconds) {
		if (!state.initialized) {
			state.initialized = true;
			state.target = target;
			AnimationClip clip = target ? forward : backward;
			state.seconds = clip.length();
			state.angle = clip.sample(state.seconds);
			state.lastSeconds = nowSeconds;
			return state.angle;
		}

		float dt = (float) Math.max(0D, nowSeconds - state.lastSeconds);
		state.lastSeconds = nowSeconds;

		AnimationClip current = state.target ? forward : backward;
		state.seconds = Math.min(state.seconds + dt, current.length());

		if (target != state.target) {
			float angleNow = current.sample(state.seconds);
			state.target = target;
			AnimationClip next = target ? forward : backward;
			state.seconds = next.timeFor(angleNow);
		}

		AnimationClip active = state.target ? forward : backward;
		state.angle = active.sample(state.seconds);
		return state.angle;
	}

	private static synchronized void load() {
		if (doorOpen != null) {
			return;
		}
		JsonObject animations = MailboxResources.readJson("animations/mailbox_animation.json").getAsJsonObject("animations");
		doorOpen = AnimationClip.parse(animations.getAsJsonObject("open"), "door");
		doorClose = AnimationClip.parse(animations.getAsJsonObject("close"), "door");
		flagUp = AnimationClip.parse(animations.getAsJsonObject("flag_up"), "flag");
		flagDown = AnimationClip.parse(animations.getAsJsonObject("flag_down"), "flag");
	}
}
