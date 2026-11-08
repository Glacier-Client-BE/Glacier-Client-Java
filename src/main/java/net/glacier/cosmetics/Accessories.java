package net.glacier.cosmetics;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.CosmeticsModule;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Client-side accessories worn by the local player: bandanas, glasses, wings, bracelets, halos and ears. Each design
 * is a PNG under assets/glacier/textures/cosmetics/&lt;folder&gt;/&lt;slug&gt;.png, where the slug is the lowercase design
 * name with spaces replaced by underscores. Geometry lives in {@link AccessoryModels}, drawing in {@link AccessoryLayer}.
 */
public final class Accessories {
	private Accessories() {
	}

	public static final String NONE = "None";
	private static final String NAMESPACE = "glacier";

	public enum Type {
		BANDANA("Bandana", "bandana", "Red Classic", "Glacier Blue", "Black Skull", "Navy Stripes"),
		GLASSES("Glasses", "glasses", "Classic", "Shades", "Gold Round", "Hearts", "Pixel"),
		WINGS("Wings", "wings", "Angel", "Bat", "Dragon", "Ice", "Butterfly"),
		BRACELET("Bracelet", "bracelet", "Gold Cuff", "Silver Gem", "Rainbow", "Leather", "Neon"),
		HALO("Halo", "halo", "Gold Halo", "Ice Halo", "Neon Halo"),
		EARS("Ears", "ears", "Cat", "Fox", "Bear");

		/** Name used for this type in the cosmetics settings. */
		public final String displayName;
		/** Folder under textures/cosmetics. */
		public final String folder;
		/** Design display names, exactly as shown in the design board. */
		public final String[] DESIGNS;

		private Identifier[] textures;

		Type(String displayName, String folder, String... designs) {
			this.displayName = displayName;
			this.folder = folder;
			this.DESIGNS = designs;
		}

		/** Index of the design with this name, or -1. */
		int indexOf(String design) {
			if (design == null) return -1;
			for (int i = 0; i < DESIGNS.length; i++) {
				if (DESIGNS[i].equals(design)) return i;
			}
			return -1;
		}

		/** Texture ids for every design, built once per type. */
		Identifier[] textures() {
			if (textures == null) {
				Identifier[] built = new Identifier[DESIGNS.length];
				for (int i = 0; i < DESIGNS.length; i++) {
					built[i] = path(this, DESIGNS[i]);
				}
				textures = built;
			}
			return textures;
		}
	}

	/** Texture of one design, e.g. glacier:textures/cosmetics/halo/gold_halo.png. */
	public static Identifier texture(Type t, String design) {
		int i = t.indexOf(design);
		if (i >= 0) return t.textures()[i];
		return path(t, design);
	}

	/**
	 * The design the local player picked for this type, or null for None, unknown names, or when the cosmetics
	 * module is missing.
	 */
	public static String selected(Type t) {
		CosmeticsModule cosmetics = ModuleManager.get(CosmeticsModule.class);
		if (cosmetics == null) return null;
		String name = cosmetics.accessory(t.displayName);
		if (name == null || NONE.equals(name) || t.indexOf(name) < 0) return null;
		return name;
	}

	private static Identifier path(Type t, String design) {
		String slug = design.toLowerCase(Locale.ROOT).replace(' ', '_');
		return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/cosmetics/" + t.folder + "/" + slug + ".png");
	}
}
