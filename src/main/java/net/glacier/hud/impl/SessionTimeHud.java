package net.glacier.hud.impl;

/** How long the game has been running this session, as H:MM:SS. */
public class SessionTimeHud extends LabelValueHud {
	private final long start = System.currentTimeMillis();

	public SessionTimeHud() {
		super("Session Time", "Shows how long you have been playing this session.", "Session", 26);
	}

	@Override
	protected String value() {
		long s = (System.currentTimeMillis() - start) / 1000;
		return String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
	}
}
