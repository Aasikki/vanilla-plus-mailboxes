package com.burgertown.mailbox;

// Client-side playback position for one moving part. Kept free of client classes so block entities can hold it on any side.
public final class AnimationState {
	public boolean initialized;
	public boolean target;
	public float seconds;
	public float angle;
	public double lastSeconds;
}
