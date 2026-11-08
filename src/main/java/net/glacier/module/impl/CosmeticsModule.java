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

	public final ChoiceSetting bandana = accessory(Accessories.Type.BANDANA);
	public final ChoiceSetting glasses = accessory(Accessories.Type.GLASSES);
	public final ChoiceSetting wings = accessory(Accessories.Type.WINGS);
	public final ChoiceSetting bracelet = accessory(Accessories.Type.BRACELET);
	public final ChoiceSetting halo = accessory(Accessories.Type.HALO);
	public final ChoiceSetting ears = accessory(Accessories.Type.EARS);

	public CosmeticsModule() {
		super("Cosmetics", "Client-side capes and accessories, visible only to you.", Category.SYSTEM);
	}

	@Override
	public void afterLoad() {
		setEnabled(true);
	}

	/** The cape to draw on the local player, or null when none is selected or capes are hidden. */
	public Capes.Cape activeCape() {
		if (!showCape.on()) return null;
		return Capes.byName(cape.get());
	}

	/**
	 * The design selected for an accessory type, looked up by its display name ("Bandana", "Glasses", "Wings",
	 * "Bracelet", "Halo", "Ears"). Returns "None" when nothing is selected, the name is unknown, or the module is off.
	 */
	public String accessory(String typeName) {
		if (!isEnabled()) return Accessories.NONE;
		for (Accessories.Type t : Accessories.Type.values()) {
			if (t.displayName.equals(typeName)) {
				ChoiceSetting s = settingFor(t);
				return s == null ? Accessories.NONE : s.get();
			}
		}
		return Accessories.NONE;
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
