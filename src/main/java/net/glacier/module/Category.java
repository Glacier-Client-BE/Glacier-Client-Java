package net.glacier.module;

public enum Category {
	HUD("HUD"),
	RENDER("Render"),
	MISC("Misc"),
	/** Backs the menu pages (Elements, Editors, Performance); never shown in the Modules grid. */
	SYSTEM("System");

	public final String display;

	Category(String display) {
		this.display = display;
	}
}
