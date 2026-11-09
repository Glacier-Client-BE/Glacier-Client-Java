package net.glacier.cosmetics;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.CosmeticsModule;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Map;

/**
 * Client-side accessories worn by the local player: bandanas, glasses, wings, bracelets, halos, ears, backpacks, hats, tails, shoulder pets, scarves and masks.
 * The first design of every type belongs to the Glacier set. Each design
 * is a PNG under assets/glacier/textures/cosmetics/&lt;folder&gt;/&lt;slug&gt;.png, where the slug is the lowercase design
 * name with spaces replaced by underscores. Geometry lives in {@link AccessoryModels}, drawing in {@link AccessoryLayer}.
 */
public final class Accessories {
	private Accessories() {
	}

	public static final String NONE = "None";
	private static final String NAMESPACE = "glacier";

	/**
	 * Designs ported from the Bedrock pack: their PNG is the pack's own art under textures/cosmetics/pack, not a generated
	 * one. Key is {@code folder/Design name}. tools/gen_cosmetics.py leaves these files alone and does not require them in the
	 * type folder.
	 */
	private static final Map<String, String> EXTERNAL = Map.of(
			"wings/Glacier Wings", "pack/wings_glacier.png",
			"halo/Glacier Halo", "pack/halo_glacier.png",
			"mask/Glacier Mask", "pack/mask_glacier.png");

	/**
	 * Geometry per design. Key is {@code folder/Design name}, value a model id that {@link AccessoryModels} understands for that
	 * type; designs not listed use the type's default model ({@link #model}). Wings: glacier (pack), angel, bat, dragon,
	 * butterfly, mecha. Halo: glacier (pack) or the default eight-bar ring. Mask: glacier (pack mesh) or the default plate.
	 */
	private static final Map<String, String> MODELS = Map.ofEntries(
			Map.entry("wings/Glacier Wings", "glacier"), Map.entry("halo/Glacier Halo", "glacier"), Map.entry("mask/Glacier Mask", "glacier"),
			Map.entry("wings/Angel", "angel"), Map.entry("wings/Seraph", "angel"), Map.entry("wings/Ice", "angel"),
			Map.entry("wings/Phoenix", "angel"), Map.entry("wings/Gold Plated", "angel"), Map.entry("wings/Glacier", "angel"),
			Map.entry("wings/Candy", "angel"), Map.entry("wings/Sakura", "angel"), Map.entry("wings/Autumn", "angel"),
			Map.entry("wings/Raven", "angel"),
			Map.entry("wings/Bat", "bat"), Map.entry("wings/Void", "bat"), Map.entry("wings/Shadow", "bat"),
			Map.entry("wings/Skeleton", "bat"), Map.entry("wings/Toxic", "bat"),
			Map.entry("wings/Dragon", "dragon"), Map.entry("wings/Lava", "dragon"), Map.entry("wings/Ice Shard", "dragon"),
			Map.entry("wings/Storm", "dragon"),
			Map.entry("wings/Butterfly", "butterfly"), Map.entry("wings/Moth", "butterfly"), Map.entry("wings/Fairy", "butterfly"),
			Map.entry("wings/Dragonfly", "butterfly"), Map.entry("wings/Galaxy", "butterfly"),
			Map.entry("wings/Mecha", "mecha"), Map.entry("wings/Cyber Grid", "mecha"), Map.entry("wings/Steampunk", "mecha"),
			Map.entry("wings/Crystal", "mecha"), Map.entry("wings/Pixel Rainbow", "mecha"));

	public enum Type {
		BANDANA("Bandana", "bandana",
				"Glacier Frost", "Red Classic", "Glacier Blue", "Black Skull", "Navy Stripes", "Camo", "Flame",
				"Tie Dye", "Bandit", "Stars", "Plaid", "Frost", "Paisley",
				"Sakura", "Rainbow", "Cyber Grid", "Hazard", "Checker Pop", "Galaxy",
				"Pirate", "Toxic", "Hachimaki", "Royal Gold"),
		GLASSES("Glasses", "glasses",
				"Glacier Lens", "Classic", "Shades", "Gold Round", "Hearts", "Pixel", "Aviator",
				"3D Glasses", "Cyber Visor", "Star Shades", "Round Neon", "Welding", "Sunset Tint",
				"Ski Goggles", "Wayfarer", "Matrix", "Steampunk", "Nerd", "Rainbow Lens",
				"Ice Lens", "Toxic Goggles", "Emerald"),
		WINGS("Wings", "wings",
				"Glacier Wings", "Angel", "Bat", "Dragon", "Ice", "Butterfly", "Phoenix",
				"Crystal", "Mecha", "Void", "Fairy", "Moth", "Raven",
				"Seraph", "Cyber Grid", "Lava", "Storm", "Candy", "Pixel Rainbow",
				"Skeleton", "Steampunk", "Glacier", "Galaxy", "Autumn", "Sakura",
				"Toxic", "Gold Plated", "Shadow", "Ice Shard", "Dragonfly"),
		BRACELET("Bracelet", "bracelet",
				"Glacier Cuff", "Gold Cuff", "Silver Gem", "Rainbow", "Leather", "Neon", "Gold Chain",
				"Spiked", "Friendship", "Tech Watch", "Ruby Band", "Diamond", "Obsidian",
				"Jade", "Cyber Cuff", "Pearl Strand", "Bead Pop", "Runic"),
		HALO("Halo", "halo",
				"Glacier Halo", "Gold Halo", "Ice Halo", "Neon Halo", "Thorn", "Laurel", "Rainbow",
				"Neon Lime", "Eclipse", "Storm", "Flame", "Crystal", "Void",
				"Candy Cane", "Sakura", "Aurora", "Starlight", "Holy Light"),
		EARS("Ears", "ears",
				"Glacier Bear", "Cat", "Fox", "Bear", "Wolf", "Panda", "Bunny",
				"Deer", "Devil", "Elf", "Robot", "Mouse", "Tiger",
				"Frog", "Husky", "Raccoon", "Snow Leopard", "Neon Cat", "Pug",
				"Dragon"),
		BACKPACK("Backpack", "backpack",
				"Glacier Bear", "Hiker Green", "Leather Brown", "School Red", "Camo Pack", "Cyber Pack",
				"Rainbow Pack", "Star Pack", "Galaxy Pack", "Candy Pack", "Treasure"),
		HAT("Hat", "hat",
				"Glacier Beanie", "Top Hat", "Baseball Cap", "Royal Crown", "Cozy Beanie", "Wizard",
				"Cowboy", "Santa", "Chef", "Captain", "Party Hat"),
		TAIL("Tail", "tail",
				"Glacier Frost", "Fox", "Tabby", "Wolf", "Dragon", "Devil",
				"Bunny Puff", "Raccoon", "Mermaid", "Skunk", "Neon Pulse"),
		PET("Pet", "pet",
				"Glacier Bear", "Tabby Cat", "Red Fox", "Snowy Owl", "Penguin", "Bunny",
				"Frog", "Panda", "Parrot", "Hatchling", "Raven"),
		SCARF("Scarf", "scarf",
				"Glacier Knit", "Red Wool", "Candy Stripe", "Rainbow Knit", "Cozy Plaid", "Midnight Silk",
				"Autumn Wrap", "Starry Night", "Sakura Wrap", "Frost Fluff", "Ocean Blue"),
		MASK("Mask", "mask",
				"Glacier Mask", "Oni", "Kitsune", "Surgical", "Ninja", "Domino",
				"Hockey", "Skull Mask", "Venetian", "Wolf Mask", "Robo Plate");

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

		/** The Glacier set piece of this type: always the first design. */
		public String glacier() {
			return DESIGNS[0];
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

	/** Model id of one design (see {@link #MODELS}); "default" when the design has no geometry of its own. */
	public static String model(Type t, String design) {
		String id = MODELS.get(t.folder + "/" + design);
		if (id != null) return id;
		return t == Type.WINGS ? "angel" : "default";
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
		String external = EXTERNAL.get(t.folder + "/" + design);
		if (external != null) return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/cosmetics/" + external);
		String slug = design.toLowerCase(Locale.ROOT).replace(' ', '_');
		return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/cosmetics/" + t.folder + "/" + slug + ".png");
	}
}
