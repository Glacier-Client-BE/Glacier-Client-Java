package net.glacier.compat;

import net.minecraft.client.input.MouseButtonEvent;

/** 26.3 reports SDL mouse buttons (left 1, middle 2, right 3); the client code uses GLFW numbering (left 0, right 1, middle 2). */
public final class Mouse {
	private Mouse() {
	}

	public static int button(MouseButtonEvent e) {
		return switch (e.button()) {
			case 1 -> 0;
			case 3 -> 1;
			case 2 -> 2;
			default -> e.button();
		};
	}
}
