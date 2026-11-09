package net.glacier.module.impl;

import net.glacier.cosmetics.Accessories;
import net.glacier.cosmetics.Capes;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;

/**
 * Client-side cosmetics. Only the local player is changed (see MixinAbstractClientPlayer); other players never see
 * these capes or accessories because nothing is sent to the server.
 */
public class CosmeticsModule extends Module {
	public final ChoiceSetting cape = add(new ChoiceSetting("Cape", Capes.NONE, Capes.OPTIONS));
	public final ToggleSetting showCape = add(new ToggleSetting("Show Cape", true));
	/** Velocity lean, sideways sway and flap on pack capes; off gives a fixed, calm cape. */
	public final ToggleSetting capePhysics = add(new ToggleSetting("Cape Physics", true));

	public final ChoiceSetting bandana = accessory(Accessories.Type.BANDANA);
	public final ChoiceSetting glasses = accessory(Accessories.Type.GLASSES);
	public final ChoiceSetting wings = accessory(Accessories.Type.WINGS);
	public final ChoiceSetting bracelet = accessory(Accessories.Type.BRACELET);
	public final ChoiceSetting halo = accessory(Accessories.Type.HALO);
	public final ChoiceSetting ears = accessory(Accessories.Type.EARS);
	public final ChoiceSetting backpack = accessory(Accessories.Type.BACKPACK);
	public final ChoiceSetting hat = accessory(Accessories.Type.HAT);
	public final ChoiceSetting tail = accessory(Accessories.Type.TAIL);
	public final ChoiceSetting pet = accessory(Accessories.Type.PET);
	public final ChoiceSetting scarf = accessory(Accessories.Type.SCARF);
	public final ChoiceSetting mask = accessory(Accessories.Type.MASK);

	public CosmeticsModule() {
		super("Cosmetics", "Client-side capes and accessories, visible only to you.", Category.SYSTEM);
	}

	@Override
	public void afterLoad() {
		setEnabled(true);
	}

	/** The cape to draw on the local player, or null when none is selected or capes are hidden. */
	public Capes.Cape activeCape() {
		if (previewCape != null) return Capes.byName(previewCape);
		if (!showCape.on()) return null;
		return Capes.byName(cape.get());
	}

	/**
	 * The design selected for an accessory type, looked up by its display name ("Bandana", "Glasses", "Wings",
	 * "Bracelet", "Halo", "Ears", "Backpack", "Hat", "Tail", "Pet", "Scarf", "Mask"). Returns "None" when nothing is selected, the name is unknown, or the module is off.
	 */
	public String accessory(String typeName) {
		for (Accessories.Type t : Accessories.Type.values()) {
			if (t.displayName.equals(typeName)) {
				if (tileActive) return t == tileType ? tileDesign : Accessories.NONE;
					String pv = previewAcc[t.ordinal()];
				if (pv != null) return pv;
				if (!isEnabled()) return Accessories.NONE;
				ChoiceSetting s = settingFor(t);
				return s == null ? Accessories.NONE : s.get();
			}
		}
		return Accessories.NONE;
	}

	// ---- try-on preview (Mod Menu cosmetics page only; cleared when that screen closes)

	/** Cape name to show instead of the equipped one, or null for no override. */
	private static volatile String previewCape;
	private static final String[] previewAcc = new String[Accessories.Type.values().length];

	public static void setPreviewCape(String name) {
		previewCape = name;
	}

	public static void setPreviewAccessory(Accessories.Type t, String design) {
		previewAcc[t.ordinal()] = design;
	}

	public static void clearPreview() {
		previewCape = null;
		java.util.Arrays.fill(previewAcc, null);
		clearTiles();
	}

	public static String previewCape() {
		return previewCape;
	}

	// ---- per-tile 3D previews: accessories are read while the GUI entity is rendered (end of frame), so each tile's
	// render state is registered here and AccessoryLayer calls bindRenderState(state) before reading selections.

	private static final java.util.IdentityHashMap<Object, Object[]> tileStates = new java.util.IdentityHashMap<>();
	private static volatile boolean tileActive;
	private static volatile Accessories.Type tileType;
	private static volatile String tileDesign;

	/** Registers a tile render state that shows only {@code design} of {@code type} (type null = no accessories). */
	public static void registerTile(Object state, Accessories.Type type, String design) {
		tileStates.put(state, new Object[]{type, design});
	}

	/** Called by AccessoryLayer for every rendered state: selects the tile override of that state, if any. */
	public static void bindRenderState(Object state) {
		Object[] o = tileStates.isEmpty() ? null : tileStates.get(state);
		if (o == null) {
			tileActive = false;
			return;
		}
		tileType = (Accessories.Type) o[0];
		tileDesign = (String) o[1];
		tileActive = true;
	}

	/** Drops every tile registration and the active binding (screen close, start of each page frame). */
	public static void clearTiles() {
		tileStates.clear();
		tileActive = false;
	}

	/** The setting holding the selected design for one accessory type. */
	public ChoiceSetting accessorySetting(Accessories.Type t) {
		return settingFor(t);
	}

	private ChoiceSetting settingFor(Accessories.Type t) {
		switch (t) {
			case BANDANA: return bandana;
			case GLASSES: return glasses;
			case WINGS: return wings;
			case BRACELET: return bracelet;
			case HALO: return halo;
			case EARS: return ears;
			case BACKPACK: return backpack;
			case HAT: return hat;
			case TAIL: return tail;
			case PET: return pet;
			case SCARF: return scarf;
			case MASK: return mask;
			default: return null;
		}
	}

	/** A None-first choice setting for one accessory type, named after the type. */
	private ChoiceSetting accessory(Accessories.Type t) {
		String[] options = new String[t.DESIGNS.length + 1];
		options[0] = Accessories.NONE;
		System.arraycopy(t.DESIGNS, 0, options, 1, t.DESIGNS.length);
		return add(new ChoiceSetting(t.displayName, Accessories.NONE, options));
	}
}
