package net.glacier.module.impl;

import com.mojang.blaze3d.platform.NativeImage;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.util.Optional;

/**
 * Recolours the enchantment glint, like the Bedrock "Enchantment Glint" extension (Red, Blue, Yellow, Orange and Rainbow
 * subpacks) plus a few more colours and a custom colour. Vanilla's glint render types bind the grey-purple glint texture
 * by Identifier at draw time (RenderSetup#getTextures), so MixinRenderSetup swaps that texture for a tinted
 * {@link DynamicTexture} built from the original's brightness. Speed and scale are applied to the glint texture matrix in
 * MixinTextureTransform. Both mixins are optional: if one fails to apply the module just does less.
 */
public class GlintColorizer extends Module {
	private static GlintColorizer instance;

	private final ChoiceSetting color = add(new ChoiceSetting("Color", "Blue", "Red", "Blue", "Yellow", "Orange",
			"Green", "Pink", "Cyan", "White", "Rainbow", "Custom"));
	private final ColorSetting custom = add(new ColorSetting("Custom Color", 0xFF00FFAA));
	private final SliderSetting speed = add(new SliderSetting("Speed", 1.0, 0.25, 4.0, 0.25));
	private final SliderSetting scale = add(new SliderSetting("Scale", 1.0, 0.5, 3.0, 0.25));

	private Tinted item;
	private Tinted armor;
	private int appliedRgb = -1;
	private int tickCount;

	public GlintColorizer() {
		super("Glint Colorizer", "Changes the color, speed and size of the enchantment glint.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		GlintColorizer g = instance;
		return g != null && g.isEnabled();
	}

	/** Called by MixinRenderSetup for every texture lookup of a render type; returns null to keep vanilla's texture. */
	public static AbstractTexture textureFor(Identifier id) {
		GlintColorizer g = instance;
		if (g == null || !g.isEnabled()) return null;
		try {
			return g.lookup(id);
		} catch (Throwable t) {
			return null;
		}
	}

	/** Applies speed and scale to a freshly created glint texture matrix (see MixinTextureTransform). */
	public static void adjust(Matrix4f m) {
		GlintColorizer g = instance;
		if (g == null) return;
		float sp = g.speed.f();
		m.m30(m.m30() * sp);
		m.m31(m.m31() * sp);
		float sc = 1f / g.scale.f();
		if (sc != 1f) m.scale(sc);
	}

	@Override
	protected void onDisable() {
		release();
	}

	@Override
	public void onTick() {
		tickCount++;
		if (color.is("Rainbow") && (tickCount & 1) != 0) return;
		int rgb = currentRgb();
		if (rgb != appliedRgb) retint(rgb);
	}

	private AbstractTexture lookup(Identifier id) {
		boolean isItem = id.equals(ItemRenderer.ENCHANTED_GLINT_ITEM);
		boolean isArmor = !isItem && id.equals(ItemRenderer.ENCHANTED_GLINT_ARMOR);
		if (!isItem && !isArmor) return null;
		Minecraft mc = Minecraft.getInstance();
		TextureManager tm = mc.getTextureManager();
		Tinted t = isItem ? item : armor;
		if (t == null || t.stale(tm)) {
			t = Tinted.create(mc, tm, id, isItem ? "glint_item" : "glint_armor");
			if (isItem) item = t;
			else armor = t;
			if (t != null) t.apply(currentRgb());
			appliedRgb = -1;
		}
		return t == null ? null : t.texture;
	}

	private int currentRgb() {
		return switch (color.get()) {
			case "Red" -> 0xFF2828;
			case "Blue" -> 0x3C6BFF;
			case "Yellow" -> 0xFFE628;
			case "Orange" -> 0xFF8C14;
			case "Green" -> 0x32E64B;
			case "Pink" -> 0xFF64C8;
			case "Cyan" -> 0x28E6E6;
			case "White" -> 0xFFFFFF;
			case "Custom" -> custom.argb() & 0xFFFFFF;
			default -> java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 6000L) / 6000f, 0.85f, 1f) & 0xFFFFFF;
		};
	}

	private void retint(int rgb) {
		appliedRgb = rgb;
		if (item != null) item.apply(rgb);
		if (armor != null) armor.apply(rgb);
	}

	private void release() {
		Minecraft mc = Minecraft.getInstance();
		TextureManager tm = mc.getTextureManager();
		if (item != null) item.release(tm);
		if (armor != null) armor.release(tm);
		item = null;
		armor = null;
		appliedRgb = -1;
	}

	/** One tinted copy of a vanilla glint texture: brightness + alpha of the original, recoloured on demand. */
	private static final class Tinted {
		final Identifier id;
		final DynamicTexture texture;
		final int w, h;
		final int[] lum;
		final int[] alpha;

		private Tinted(Identifier id, DynamicTexture texture, int w, int h, int[] lum, int[] alpha) {
			this.id = id;
			this.texture = texture;
			this.w = w;
			this.h = h;
			this.lum = lum;
			this.alpha = alpha;
		}

		static Tinted create(Minecraft mc, TextureManager tm, Identifier source, String name) {
			Optional<Resource> res = mc.getResourceManager().getResource(source);
			if (res.isEmpty()) return null;
			try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
				int w = img.getWidth();
				int h = img.getHeight();
				int[] px = img.getPixelsABGR();
				int[] lum = new int[px.length];
				int[] alpha = new int[px.length];
				for (int i = 0; i < px.length; i++) {
					int p = px[i];
					int r = p & 0xFF, g = (p >> 8) & 0xFF, b = (p >> 16) & 0xFF;
					lum[i] = Math.max(r, Math.max(g, b));
					alpha[i] = (p >>> 24) & 0xFF;
				}
				Identifier id = Identifier.fromNamespaceAndPath("glacier", name);
				DynamicTexture tex = new DynamicTexture(() -> "glacier_" + name, new NativeImage(w, h, false));
				tm.register(id, tex);
				return new Tinted(id, tex, w, h, lum, alpha);
			} catch (Exception e) {
				return null;
			}
		}

		/** True when the texture manager no longer holds our texture (resource reload) and it must be rebuilt. */
		boolean stale(TextureManager tm) {
			return tm.getTexture(id) != texture;
		}

		void apply(int rgb) {
			NativeImage img = texture.getPixels();
			if (img == null) return;
			int tr = (rgb >> 16) & 0xFF, tg = (rgb >> 8) & 0xFF, tb = rgb & 0xFF;
			int i = 0;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++, i++) {
					int l = lum[i];
					int r = l * tr / 255, g = l * tg / 255, b = l * tb / 255;
					img.setPixelABGR(x, y, (alpha[i] << 24) | (b << 16) | (g << 8) | r);
				}
			}
			texture.upload();
		}

		void release(TextureManager tm) {
			try {
				if (tm.getTexture(id) == texture) tm.release(id);
			} catch (Throwable ignored) {
			}
		}
	}
}
