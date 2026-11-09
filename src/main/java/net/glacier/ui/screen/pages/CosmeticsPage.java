package net.glacier.ui.screen.pages;

import net.glacier.config.Config;
import net.glacier.cosmetics.Accessories;
import net.glacier.cosmetics.Capes;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.CosmeticsModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.ui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cosmetics tab (pages/cosmetics.js, .cos-* rules in pages-extra.css): the PREVIEW stage card on the left and, on the
 * right, six tabs (Capes, Head, Face, Back, Body, Equipped) over a 3 column tile grid. All numbers css px.
 * Cape art shows only the front and back faces (22x17 of the 64x32 sheet).
 */
public final class CosmeticsPage {
	private static final String NONE = Capes.NONE;
	private static final int INSET = 0xFF1F2127, MUTED = 0xFF99AAB5, ACCENT_UI = 0xFF6B7FE0;
	private static final int DEFAULT_ACCENT = 0xFF6B7FE0;
	private static final int ICON_NONE = 0xF05E, ICON_CHECK = 0xF00C;

	private record Tab(String id, String label, Accessories.Type... types) {
	}

	private static final Tab[] TABS = {
			new Tab("capes", "Capes"),
			new Tab("head", "Head", Accessories.Type.HAT, Accessories.Type.BANDANA, Accessories.Type.HALO, Accessories.Type.EARS),
			new Tab("face", "Face", Accessories.Type.MASK, Accessories.Type.GLASSES),
			new Tab("back", "Back", Accessories.Type.WINGS, Accessories.Type.BACKPACK, Accessories.Type.TAIL),
			new Tab("body", "Body", Accessories.Type.SCARF, Accessories.Type.PET, Accessories.Type.BRACELET),
			new Tab("equipped", "Equipped")};

	private final Random random = new Random();
	private String tab = "capes";

	private interface Art {
		void paint(float x, float y, float w, float h);
	}

	// ------------------------------------------------------------------- page

	public float draw(MenuKit k, float x, float y, float w) {
		kitRef = k;
		beginTiles();
		pvCape = null;
		pvType = null;
		pvAcc = null;
		float mx = x + 232 + 12, mw = w - 244;
		float my = y;

		float tw = (mw - 4 * (TABS.length - 1)) / TABS.length;
		for (int i = 0; i < TABS.length; i++) {
			float tx = mx + i * (tw + 4);
			boolean on = TABS[i].id().equals(tab);
			boolean hov = k.hover(tx, my, tw, 32);
			k.box(tx, my, tw, 32, 16, on ? ACCENT_UI : hov ? 0xFF33373C : 0xFF2C2F33, on ? ACCENT_UI : 0xFF393C41);
			int n = count(TABS[i]);
			String label = TABS[i].label() + (n > 0 ? " · " + n : "");
			k.textCenter(MenuKit.fit(label, tw - 14, 12, false), tx + tw / 2f, my + 16, 12,
					on || hov ? 0xFFFFFFFF : MUTED, false);
			final String id = TABS[i].id();
			k.hit(tx, my, tw, 32, () -> tab = id);
		}
		float by = my + 32 + 10;
		float end;
		if (tab.equals("equipped")) {
			end = equipped(k, mx, by, mw);
		} else if (tab.equals("capes")) {
			end = capesGrid(k, mx, by, mw);
		} else {
			end = by;
			for (Tab t : TABS) {
				if (!t.id().equals(tab)) continue;
				for (Accessories.Type type : t.types()) end = group(k, type, mx, end, mw) + 10;
				end -= 10;
			}
		}
		applyPreview();
		float stageH = stage(k, x, y);
		return Math.max(y + stageH, end);
	}

	private static int count(Tab t) {
		int n = 0;
		boolean cape = !NONE.equals(selectedCape());
		if (t.id().equals("capes")) return cape ? 1 : 0;
		if (t.id().equals("equipped")) {
			n = cape ? 1 : 0;
			for (Accessories.Type type : Accessories.Type.values()) if (!NONE.equals(selectedAccessory(type))) n++;
			return n;
		}
		for (Accessories.Type type : t.types()) if (!NONE.equals(selectedAccessory(type))) n++;
		return n;
	}

	private static String tabFor(Accessories.Type type) {
		for (Tab t : TABS) for (Accessories.Type tt : t.types()) if (tt == type) return t.id();
		return "capes";
	}

	// -------------------------------------------------------------- 3D model

	private static final float STAGE_H = 200;
	private static final float AUTO_SPEED = 24f, DRAG_DEG = 0.9f, PITCH = 0.14f, EYE_OFFSET = 0.0625f;
	private static final int FULL_BRIGHT = 15728880;
	private static final Vector2f TMP = new Vector2f();

	/** Hovered tile (try-on); applied to the module once per frame before the model is drawn. */
	private String pvCape;
	private Accessories.Type pvType;
	private String pvAcc;

	private float yaw = 200f, vel = AUTO_SPEED, zoom = 1f, zoomShown = 1f, lastX = Float.NaN;
	private boolean dragging;
	private long frameNanos, dragNanos;
	private float sx, sy, sw, sh;

	private void applyPreview() {
		CosmeticsModule.setPreviewCape(pvCape);
		for (Accessories.Type t : Accessories.Type.values()) CosmeticsModule.setPreviewAccessory(t, null);
		if (pvType != null) CosmeticsModule.setPreviewAccessory(pvType, pvAcc);
	}

	/** Mouse wheel over the stage zooms the model. Coordinates are css px. Returns true when consumed. */
	public boolean scrollZoom(double lx, double ly, double dy) {
		if (lx < sx || ly < sy || lx >= sx + sw || ly >= sy + sh) return false;
		zoom = Math.max(0.6f, Math.min(2.2f, zoom * (1f + (float) dy * 0.12f)));
		return true;
	}

	private void dragPress(double lx) {
		float x = (float) lx;
		long now = System.nanoTime();
		if (!dragging || Float.isNaN(lastX)) {
			dragging = true;
			lastX = x;
			dragNanos = now;
			return;
		}
		float dx = x - lastX;
		lastX = x;
		if (dx == 0) return;
		float deg = dx * DRAG_DEG;
		yaw += deg;
		vel = Math.max(-540f, Math.min(540f, deg / Math.max(1f / 240f, (now - dragNanos) / 1e9f)));
		dragNanos = now;
	}

	private void dragRelease() {
		dragging = false;
		lastX = Float.NaN;
	}

	/** The stage: backdrop, the 3D player (or a 2D fallback without a world), drag area and zoom buttons. */
	private void model(MenuKit k, float x, float y, float w, float h, Capes.Cape cape) {
		sx = x;
		sy = y;
		sw = w;
		sh = h;
		long now = System.nanoTime();
		float dt = frameNanos == 0 ? 0.016f : Math.min(0.1f, (now - frameNanos) / 1e9f);
		frameNanos = now;
		if (!dragging) {
			vel += (AUTO_SPEED - vel) * Math.min(1f, 2.5f * dt);
			yaw += vel * dt;
		}
		zoomShown += (zoom - zoomShown) * Math.min(1f, 12f * dt);
		if (yaw > 720f || yaw < -720f) yaw %= 360f;

		k.gradient(x, y, w, h, 14, 0xFF2A2D3A, 0xFF1B1D24);
		k.ring(x, y, w, h, 14, 0x1AFFFFFF, 1);
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p != null && mc.level != null) {
			drawPlayer(k, mc, p, x + 1, y + 1, w - 2, h - 2);
		} else {
			if (cape != null) region(k, cape, x, y + 10, w, h - 40, 8);
			else k.icon(ICON_NONE, x + w / 2f, y + h / 2f - 10, 22, MUTED);
			k.textCenter("Join a world for the 3D preview", x + w / 2f, y + h - 16, 10, MUTED, false);
		}
		k.drag(x, y, w, h, this::dragPress, this::dragRelease);
		zoomButton(k, 0xF067, x + w - 8 - 22, y + 8, 1.2f);
		zoomButton(k, 0xF068, x + w - 8 - 22, y + 8 + 26, 1f / 1.2f);
	}

	private void zoomButton(MenuKit k, int icon, float x, float y, float factor) {
		boolean hov = k.hover(x, y, 22, 22);
		k.box(x, y, 22, 22, 11, hov ? 0xFF33373C : 0x992C2F33, 0xFF393C41);
		k.icon(icon, x + 11, y + 11, 9, hov ? 0xFFFFFFFF : 0xFFD6DAE0);
		k.hit(x, y, 22, 22, () -> zoom = Math.max(0.6f, Math.min(2.2f, zoom * factor)));
	}

	/** Submits vanilla's GUI entity render for the local player in the css rectangle, using the render state rotation. */
	private void drawPlayer(MenuKit k, Minecraft mc, LocalPlayer p, float x, float y, float w, float h) {
		var pose = k.g.pose();
		pose.transformPosition(x, y, TMP);
		int x0 = Math.round(TMP.x), y0 = Math.round(TMP.y);
		pose.transformPosition(x + w, y + h, TMP);
		int x1 = Math.round(TMP.x), y1 = Math.round(TMP.y);
		if (x1 <= x0 || y1 <= y0) return;

		EntityRenderState st = mc.getEntityRenderDispatcher().extractEntity(p, 1f);
		st.lightCoords = FULL_BRIGHT;
		st.shadowPieces.clear();
		st.outlineColor = 0;
		st.nameTag = null;
		st.displayFireAnimation = false;
		float entityScale = 1f;
		if (st instanceof LivingEntityRenderState l) {
			l.bodyRot = 180f + yaw;
			l.yRot = 0f;
			l.xRot = 0f;
			l.pose = Pose.STANDING;
			l.walkAnimationSpeed = 0f;
			entityScale = l.scale;
			l.boundingBoxWidth /= l.scale;
			l.boundingBoxHeight /= l.scale;
			l.scale = 1f;
		}
		float pixPerBlock = (y1 - y0) / 2.15f * zoomShown / entityScale;
		Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf cam = new Quaternionf().rotateX(PITCH);
		rot.mul(cam);
		k.g.entity(st, pixPerBlock, new Vector3f(0f, st.boundingBoxHeight / 2f + EYE_OFFSET, 0f), rot, cam,
				x0, y0, x1, y1);
	}

	// ------------------------------------------------------------------ stage

	private float stage(MenuKit k, float x, float y) {
		Capes.Cape cape = Capes.byName(pvCape != null ? pvCape : selectedCape());
		List<Accessories.Type> worn = new ArrayList<>();
		for (Accessories.Type t : Accessories.Type.values()) if (!NONE.equals(selectedAccessory(t))) worn.add(t);
		float wornH = worn.isEmpty() ? 12.5f : worn.size() * 50 + (worn.size() - 1) * 6;
		float h = 2 + 28 + 12.5f + 10 + STAGE_H + 10 + 35.5f + 10 + 26 + 10 + 26 + 10 + 12.5f + 10 + wornH;
		k.box(x, y, 232, h, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 15, iw = 202, cy = y + 15;
		k.text("PREVIEW", ix, cy + 6.25f, 10, 0xFF8EA0E0);
		cy += 12.5f + 10;
		model(k, ix, cy, iw, STAGE_H, cape);
		cy += STAGE_H + 10;
		boolean trying = pvCape != null || pvType != null;
		k.text(MenuKit.fit(cape == null ? NONE : cape.name(), iw, 16, true), ix, cy + 10, 16, 0xFFFFFFFF, true);
		k.text(trying ? "Trying on, click to equip" : cape != null ? "Cape equipped" : "No cape equipped", ix,
				cy + 20 + 3 + 6.25f, 10, trying ? 0xFF8EA0E0 : MUTED);
		cy += 35.5f + 10;
		button(k, "Equip Glacier Set", ix, cy, iw, this::equipGlacier);
		cy += 26 + 10;
		button(k, "Randomize outfit", ix, cy, iw, this::randomize);
		cy += 26 + 10;
		k.text("WORN", ix, cy + 6.25f, 10, MUTED);
		cy += 12.5f + 10;
		if (worn.isEmpty()) {
			k.text("No accessories equipped", ix, cy + 6.25f, 10, MUTED);
		}
		for (Accessories.Type t : worn) {
			row(k, ix, cy, iw, t.displayName, selectedAccessory(t), accArt(t, selectedAccessory(t), 56, 30), tabFor(t), null);
			cy += 56;
		}
		return h;
	}

	/** .cos-btn: 26px pill. Width is given, or sized to the label when w <= 0. Returns its width. */
	private static float button(MenuKit k, String label, float x, float y, float w, Runnable act) {
		float bw = w > 0 ? w : MenuKit.width(label, 11, false) + 22;
		boolean hov = k.hover(x, y, bw, 26);
		k.box(x, y, bw, 26, 13, hov ? 0xFF33373C : 0xFF2C2F33, 0xFF393C41);
		k.textCenter(label, x + bw / 2f, y + 13, 11, hov ? 0xFFFFFFFF : 0xFFD6DAE0, false);
		k.hit(x, y, bw, 26, act);
		return bw;
	}

	/**
	 * .cos-row: 60x36 art, eyebrow label over name, optional Remove button; clicking the row switches tab. {@code clear}
	 * is the Remove action (null = no button).
	 */
	private void row(MenuKit k, float x, float y, float w, String label, String name, Art art, String goTab, Runnable clear) {
		boolean hov = k.hover(x, y, w, 50);
		k.box(x, y, w, 50, 12, hov ? 0x12FFFFFF : 0x08FFFFFF, 0x0DFFFFFF);
		k.round(x + 9, y + 7, 60, 36, 8, INSET);
		art.paint(x + 9, y + 7, 60, 36);
		float tx = x + 9 + 60 + 10;
		float right = x + w - 9;
		if (clear != null) {
			float bw = MenuKit.width("Remove", 11, false) + 22;
			right = x + w - 9 - bw - 10;
		}
		k.text(label.toUpperCase(java.util.Locale.ROOT), tx, y + 25 - 15.25f + 6.25f, 10, MUTED);
		k.text(MenuKit.fit(name, right - tx, 12, false), tx, y + 25 - 15.25f + 12.5f + 3 + 7.5f, 12, 0xFFFFFFFF);
		k.hit(x, y, w, 50, () -> tab = goTab);
		if (clear != null) {
			float bw = MenuKit.width("Remove", 11, false) + 22;
			button(k, "Remove", x + w - 9 - bw, y + 12, 0, clear);
		}
	}

	// ------------------------------------------------------------------ tiles

	private static float tileH() {
		return 151.5f;
	}

	// ------------------------------------------------------- per-tile 3D previews

	/** Cached render state of one tile; only yaw / age are mutated per frame. */
	private static final class TileModel {
		EntityRenderState state;
		float phase;
		float lastY = Float.NaN;
	}

	private final java.util.HashMap<String, TileModel> tileModels = new java.util.HashMap<>();
	private LocalPlayer tilePlayer;
	private float tileDt = 0.016f;
	private long tileNanos;
	private int extractBudget, tilesDrawn;
	/** Cards allowed to render in 3D this frame besides the hovered one; shrinks when the frame rate drops. */
	private int tileCap = 6;

	/** Per-frame bookkeeping: frame delta, extraction budget, and dropping stale tile registrations. */
	private void beginTiles() {
		long now = System.nanoTime();
		tileDt = tileNanos == 0 ? 0.016f : Math.min(0.1f, (now - tileNanos) / 1e9f);
		tileNanos = now;
		extractBudget = 2;
		tilesDrawn = 0;
		CosmeticsModule.clearTiles();
		LocalPlayer p = Minecraft.getInstance().player;
		if (p != tilePlayer) {
			tileModels.clear();
			tilePlayer = p;
		}
	}

	/**
	 * Draws the 3D preview of one tile into the art rect. Returns false when the flat fallback must be used (no world,
	 * None tile, scrolling fast, extraction budget used up, or a failure).
	 */
	private boolean model3d(MenuKit k, TileSpec s, boolean hov, float x, float y, float w, float h, float baseY) {
		if (s.value == null || NONE.equals(s.value) || (!hov && tilesDrawn >= tileCap)) return false;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null || !k.visibleY(y, h)) return false;
		String key = (s.type == null ? "cape:" : s.type.name() + ":") + s.value;
		TileModel m = tileModels.get(key);
		boolean fast = m != null && !Float.isNaN(m.lastY) && Math.abs(baseY - m.lastY) > 14f;
		if (m != null) m.lastY = baseY;
		if (fast) return false;
		if (m == null) {
			if (extractBudget <= 0) return false;
			extractBudget--;
			m = new TileModel();
			m.lastY = baseY;
			m.phase = (key.hashCode() & 0xFF) / 40f;
			String prevCape = CosmeticsModule.previewCape();
			try {
				// Capes are read from the skin while the state is extracted; accessories later, through registerTile.
				CosmeticsModule.setPreviewCape(s.type == null ? s.value : NONE);
				EntityRenderState st = mc.getEntityRenderDispatcher().extractEntity(p, 1f);
				st.lightCoords = FULL_BRIGHT;
				st.shadowPieces.clear();
				st.outlineColor = 0;
				st.nameTag = null;
				st.displayFireAnimation = false;
				hideBody(st);
				if (st instanceof LivingEntityRenderState l) {
					l.yRot = 0f;
					l.xRot = 0f;
					l.pose = Pose.STANDING;
					l.walkAnimationSpeed = 0f;
					l.boundingBoxWidth /= l.scale;
					l.boundingBoxHeight /= l.scale;
					l.scale = 1f;
				}
				m.state = st;
			} catch (RuntimeException e) {
				return false;
			} finally {
				CosmeticsModule.setPreviewCape(prevCape);
			}
			tileModels.put(key, m);
		}
		EntityRenderState st = m.state;
		if (st == null) return false;
		var pose = k.g.pose();
		float ix = x + 2, iy = y + 2, iw = w - 4, ih = h - 4;
		pose.transformPosition(ix, iy, TMP);
		int x0 = Math.round(TMP.x), y0 = Math.round(TMP.y);
		pose.transformPosition(ix + iw, iy + ih, TMP);
		int x1 = Math.round(TMP.x), y1 = Math.round(TMP.y);
		if (x1 <= x0 || y1 <= y0) return false;

		m.phase += tileDt * (hov ? 3.4f : 0.9f);
		// Per-type framing, centred on the accessory (focus = height in blocks above the feet, ppb = pixels per block
		// at a 96px art box). The player body is invisible, so only the accessory is on screen.
		float base, amp, focus, ppb;
		Accessories.Type ty = s.type == null ? Accessories.Type.WINGS : s.type;
		switch (ty) {
			case WINGS -> { base = 180f; amp = 30f; focus = 1.08f; ppb = 58f; } // from behind
			case BACKPACK -> { base = 180f; amp = 28f; focus = 1.02f; ppb = 100f; } // from behind
			case TAIL -> { base = 160f; amp = 30f; focus = 0.78f; ppb = 90f; } // behind, slightly aside
			case PET -> { base = 30f; amp = 28f; focus = 1.3f; ppb = 150f; } // three-quarter
			case SCARF -> { base = 25f; amp = 30f; focus = 1.2f; ppb = 120f; } // three-quarter
			case BRACELET -> { base = 35f; amp = 28f; focus = 0.62f; ppb = 150f; } // three-quarter
			case HAT -> { base = 0f; amp = 32f; focus = 1.86f; ppb = 105f; }
			case MASK -> { base = 0f; amp = 26f; focus = 1.68f; ppb = 150f; }
			case GLASSES -> { base = 0f; amp = 28f; focus = 1.7f; ppb = 165f; }
			case BANDANA -> { base = 0f; amp = 32f; focus = 1.78f; ppb = 125f; }
			case EARS -> { base = 0f; amp = 32f; focus = 1.92f; ppb = 120f; }
			case HALO -> { base = 0f; amp = 32f; focus = 1.98f; ppb = 150f; }
			default -> { base = 0f; amp = 32f; focus = 1.7f; ppb = 120f; }
		}
		float ang = base + amp * (float) Math.sin(m.phase) + (hov ? 40f * (float) Math.sin(m.phase * 0.5f) : 0f);
		float scale = ppb * (ih / 96f);
		if (st instanceof LivingEntityRenderState l) l.bodyRot = ang;
		st.ageInTicks = tileNanos / 5e7f;
		CosmeticsModule.registerTile(st, s.type, s.type == null ? null : s.value);
		Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf cam = new Quaternionf().rotateX(PITCH);
		rot.mul(cam);
		k.scissor(ix, iy, iw, ih);
		try {
			k.g.entity(st, scale, new Vector3f(0f, focus + EYE_OFFSET, 0f), rot, cam, x0, y0, x1, y1);
		} finally {
			k.endScissor();
		}
		tilesDrawn++;
		return true;
	}

	private static final Identifier BLANK = Identifier.fromNamespaceAndPath("glacier", "textures/cosmetics/blank.png");

	/**
	 * Makes the player render nothing but the accessory layer: the skin is swapped for a fully transparent texture
	 * (so body, outer layers and vanilla cape vanish while isInvisible stays false and AccessoryLayer still runs), and
	 * armor, held items, head items, arrows, parrots, name and score text are cleared.
	 */
	private static void hideBody(EntityRenderState st) {
		if (!(st instanceof AvatarRenderState a)) return;
		if (a.skin != null) {
			a.skin = new PlayerSkin(new ClientAsset.ResourceTexture(BLANK, BLANK), null, null, a.skin.model(), true);
		}
		a.showCape = false;
		a.showExtraEars = false;
		a.arrowCount = 0;
		a.stingerCount = 0;
		a.scoreText = null;
		a.parrotOnLeftShoulder = null;
		a.parrotOnRightShoulder = null;
		a.isFallFlying = false;
		a.isCrouching = false;
		a.isPassenger = false;
		a.isUsingItem = false;
		a.headEquipment = ItemStack.EMPTY;
		a.chestEquipment = ItemStack.EMPTY;
		a.legsEquipment = ItemStack.EMPTY;
		a.feetEquipment = ItemStack.EMPTY;
		a.rightHandItemStack = ItemStack.EMPTY;
		a.leftHandItemStack = ItemStack.EMPTY;
		a.rightHandItemState.clear();
		a.leftHandItemState.clear();
		a.headItem.clear();
		a.heldOnHead.clear();
		a.rightArmPose = HumanoidModel.ArmPose.EMPTY;
		a.leftArmPose = HumanoidModel.ArmPose.EMPTY;
	}

	/** Per-cape rotation phase, advanced only while the tile is drawn. */
	private final java.util.HashMap<String, float[]> capePhase = new java.util.HashMap<>();

	/**
	 * A cape as a slowly turning flat 3D card: the outer face (u 1..11, v 1..17 of the 64x32 sheet) squeezed
	 * horizontally by cos(angle), the inner face (u 12..22) shown past 90 degrees, with a little vertical shading and
	 * a darkening towards edge-on. Faster while hovered.
	 */
	private void capeCard(MenuKit k, Capes.Cape cape, boolean hov, float x, float y, float w, float h) {
		float[] ph = capePhase.computeIfAbsent(cape.name(), n -> new float[]{(n.hashCode() & 0xFF) / 40f});
		ph[0] += tileDt * (hov ? 2.6f : 0.55f);
		float c = (float) Math.cos(ph[0]);
		float ac = Math.max(0.05f, Math.abs(c));
		int u = c >= 0 ? 1 : 12;
		boolean pack = cape.pack();
		float s = pack ? (h - 12f) / 466f : Math.max(1f, (float) Math.floor((h - 10f) / 16f));
		float cx = x + w / 2f, cy = y + h / 2f + (float) Math.sin(ph[0] * 1.7f) * 1.5f;
		int shade = 0xFF000000 | ((int) (150 + 105 * ac) * 0x010101);
		var m = k.g.pose();
		k.scissor(x, y, w, h);
		m.pushMatrix();
		m.translate(cx, cy);
		m.scale(ac * s, s);
		if (pack) {
			// Pack cape (640x670 sheet, faces from cape_glacier.mesh.json): outer 29..320 x 29..495, inner 349..640 x 37..494.
			if (c >= 0) k.g.blit(RenderPipelines.GUI_TEXTURED, cape.texture(), -145, -233, 29f, 29f, 291, 466, 291, 466, 640, 670, shade);
			else k.g.blit(RenderPipelines.GUI_TEXTURED, cape.texture(), -145, -233, 349f, 37f, 291, 466, 291, 457, 640, 670, shade);
			k.g.fillGradient(-145, -233, 146, 233, 0x22FFFFFF, 0x55000000);
		} else {
			k.g.blit(RenderPipelines.GUI_TEXTURED, cape.texture(), -5, -8, (float) u, 1f, 10, 16, 10, 16, 64, 32, shade);
			k.g.fillGradient(-5, -8, 5, 8, 0x22FFFFFF, 0x55000000);
		}
		m.popMatrix();
		k.endScissor();
	}

	private boolean canModel(MenuKit k, float ty) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.level != null && k.visibleY(ty, tileH());
	}

	private boolean tile(MenuKit k, float x, float y, float w, String name, String sub, int acc, boolean on, Art art, Runnable pick,
						 TileSpec spec) {
		boolean hov = k.hover(x, y, w, tileH());
		float ty = hov ? y - 3 : y;
		if (on) k.shadow(x, ty, w, tileH(), 16, Draw.alpha(acc, 0.45f), 6);
		k.shadow(x, ty + (hov ? 8 : 0), w, tileH(), 16, hov ? 0x61000000 : 0x40000000, hov ? 5 : 2);
		k.box(x, ty, w, tileH(), 16, on || hov ? 0xFF303237 : 0xFF2A2B2F, on ? acc : Draw.alpha(acc, 0.45f));
		float aw = w - 18;
		if (spec != null && spec.type == null && spec.value != null && !NONE.equals(spec.value) && k.visibleY(ty, tileH())
				&& Capes.byName(spec.value) != null) {
			k.round(x + 9, ty + 9, aw, 96, 12, INSET);
			k.gradient(x + 9, ty + 9, aw, 96, 12, Draw.alpha(acc, 0.30f), Draw.alpha(acc, 0.04f));
			capeCard(k, Capes.byName(spec.value), hov, x + 9, ty + 9, aw, 96);
		} else if (spec != null && spec.value != null && !NONE.equals(spec.value) && spec.type != null && canModel(k, ty)) {
			k.round(x + 9, ty + 9, aw, 96, 12, INSET);
			k.gradient(x + 9, ty + 9, aw, 96, 12, Draw.alpha(acc, 0.30f), Draw.alpha(acc, 0.04f));
			if (!model3d(k, spec, hov, x + 9, ty + 9, aw, 96, y)) art.paint(x + 9, ty + 9, aw, 96);
		} else {
			art.paint(x + 9, ty + 9, aw, 96);
		}
		float ix = x + 1 + 8 + 2;
		k.text(MenuKit.fit(name, w - 24, 12, false), ix, ty + 1 + 8 + 96 + 8 + 7.5f, 12, 0xFFFFFFFF);
		k.text(MenuKit.fit(sub, w - 24, 10, false), ix, ty + 1 + 8 + 96 + 8 + 15 + 2 + 6.25f, 10, MUTED);
		if (on) {
			float bx = x + w - 1 - 10 - 22, by = ty + 1 + 10;
			k.round(bx, by, 22, 22, 11, acc);
			k.icon(ICON_CHECK, bx + 11, by + 11, 10, 0xFFFFFFFF);
		}
		k.hit(x, y, w, tileH(), pick);
		return hov;
	}

	private float grid(MenuKit k, float x, float y, float w, List<Runnable> unused, TileSpec[] specs) {
		float tw = (w - 20) / 3f;
		for (int i = 0; i < specs.length; i++) {
			TileSpec s = specs[i];
			if (tile(k, x + (i % 3) * (tw + 10), y + (i / 3) * (tileH() + 10), tw, s.name, s.sub, s.acc, s.on, s.art, s.pick, s)
					&& s.value != null) {
				if (s.type == null) pvCape = s.value;
				else {
					pvType = s.type;
					pvAcc = s.value;
				}
			}
		}
		int rows = (specs.length + 2) / 3;
		return y + rows * tileH() + Math.max(0, rows - 1) * 10;
	}

	private record TileSpec(String name, String sub, int acc, boolean on, Art art, Runnable pick,
							Accessories.Type type, String value) {
	}

	private float capesGrid(MenuKit k, float x, float y, float w) {
		String sel = selectedCape();
		List<TileSpec> specs = new ArrayList<>();
		specs.add(new TileSpec(NONE, "No cape", DEFAULT_ACCENT, NONE.equals(sel), (ax, ay, aw, ah) -> noneArt(k, ax, ay, aw, ah),
				() -> setCape(NONE), null, NONE));
		for (Capes.Cape cape : Capes.ALL) {
			specs.add(new TileSpec(cape.name(), "Cape", cape.accent(), cape.name().equals(sel), (ax, ay, aw, ah) -> {
				k.gradient(ax, ay, aw, ah, 12, cape.base(), cape.accent());
				region(k, cape, ax, ay, aw, ah, 5);
			}, () -> setCape(cape.name()), null, cape.name()));
		}
		return grid(k, x, y, w, null, specs.toArray(new TileSpec[0]));
	}

	private float group(MenuKit k, Accessories.Type t, float x, float y, float w) {
		String sel = selectedAccessory(t);
		int acc = accent(t);
		List<TileSpec> specs = new ArrayList<>();
		specs.add(new TileSpec(NONE, "Nothing", acc, NONE.equals(sel), (ax, ay, aw, ah) -> noneArt(k, ax, ay, aw, ah),
				() -> setAccessory(t, NONE), t, NONE));
		for (String design : t.DESIGNS) {
			specs.add(new TileSpec(design, t.displayName, acc, design.equals(sel), (ax, ay, aw, ah) -> {
				k.gradient(ax, ay, aw, ah, 12, Draw.alpha(acc, 0.24f), Draw.alpha(acc, 0.03f));
				sheet(k, Accessories.texture(t, design), sheetSize(t), ax, ay, aw, ah, 120, 56, 4);
			}, () -> setAccessory(t, design), t, design));
		}
		int rows = (specs.size() + 2) / 3;
		float h = 2 + 24 + 16.25f + 10 + rows * tileH() + (rows - 1) * 10;
		k.box(x, y, w, h, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 15, iw = w - 30;
		k.text(t.displayName, ix, y + 13 + 8.125f, 13, 0xFFFFFFFF, true);
		k.textRight(NONE.equals(sel) ? "Nothing equipped" : sel, ix + iw, y + 13 + 8.125f, 10, MUTED, false);
		grid(k, ix, y + 13 + 16.25f + 10, iw, null, specs.toArray(new TileSpec[0]));
		return y + h;
	}

	private float equipped(MenuKit k, float x, float y, float w) {
		int rows = 1 + Accessories.Type.values().length;
		float h = 2 + 24 + 26 + 10 + rows * 50 + (rows - 1) * 10;
		k.box(x, y, w, h, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 15, iw = w - 30;
		k.text("Equipped", ix, y + 13 + 13, 13, 0xFFFFFFFF, true);
		float bw = MenuKit.width("Unequip all", 11, false) + 22;
		button(k, "Unequip all", ix + iw - bw, y + 13, 0, this::unequipAll);
		float ry = y + 13 + 26 + 10;
		Capes.Cape cape = Capes.byName(selectedCape());
		row(k, ix, ry, iw, "Cape", cape == null ? NONE : cape.name(),
				cape == null ? (ax, ay, aw, ah) -> k.icon(ICON_NONE, ax + aw / 2f, ay + ah / 2f, 22, MUTED)
						: (ax, ay, aw, ah) -> region(k, cape, ax, ay, aw, ah, 1.64f),
				"capes", cape == null ? null : () -> setCape(NONE));
		ry += 60;
		for (Accessories.Type t : Accessories.Type.values()) {
			String sel = selectedAccessory(t);
			boolean on = !NONE.equals(sel);
			row(k, ix, ry, iw, t.displayName, on ? sel : NONE, on ? accArt(t, sel, 56, 30)
					: (ax, ay, aw, ah) -> k.icon(ICON_NONE, ax + aw / 2f, ay + ah / 2f, 22, MUTED),
					tabFor(t), on ? () -> setAccessory(t, NONE) : null);
			ry += 60;
		}
		return y + h;
	}

	// -------------------------------------------------------------------- art

	private void noneArt(MenuKit k, float x, float y, float w, float h) {
		k.round(x, y, w, h, 12, INSET);
		k.icon(ICON_NONE, x + w / 2f, y + h / 2f, 20, MUTED);
	}

	private Art accArt(Accessories.Type t, String design, float maxW, float maxH) {
		return (ax, ay, aw, ah) -> {
			MenuKit kk = kitRef;
			sheet(kk, Accessories.texture(t, design), sheetSize(t), ax, ay, aw, ah, maxW, maxH, 3);
		};
	}

	/** The kit of the frame being drawn, for Art lambdas created before it is in scope. */
	private MenuKit kitRef;

	/** Draws only the top-left 22x17 part of a 64x32 cape sheet (front and back faces), integer scaled up to maxScale. */
	private static void region(MenuKit k, Capes.Cape cape, float x, float y, float w, float h, float maxScale) {
		Identifier id = cape.texture();
		if (cape.pack()) { // native 640x670 sheet: outer face 291x466 at (29,29), drawn at its real texture size
			float ps = Math.min(w / 291f, h / 466f);
			float dw = 291 * ps, dh = 466 * ps;
			k.image(id, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh, 29, 29, 291, 466, 640, 670);
			return;
		}
		float s = Math.min(Math.min(w / 22f, h / 17f), maxScale);
		if (s >= 1f) s = maxScale >= 2 ? (float) Math.floor(s) : s;
		float dw = 22 * s, dh = 17 * s;
		k.image(id, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh, 0, 0, 22, 17, 64, 32);
	}

	/** A whole texture of size[0] x size[1], scaled to fit maxW x maxH (up to maxScale) and centred in the box. */
	private static void sheet(MenuKit k, Identifier id, int[] size, float x, float y, float w, float h, float maxW,
							  float maxH, float maxScale) {
		float s = Math.min(Math.min(maxW / size[0], maxH / size[1]), maxScale);
		float dw = (float) Math.floor(size[0] * s), dh = (float) Math.floor(size[1] * s);
		k.image(id, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh, 0, 0, size[0], size[1], size[0], size[1]);
	}

	/** Accent per accessory type, as in cosmetics.js (the API carries no colours for accessories). */
	private static int accent(Accessories.Type t) {
		return switch (t) {
			case BANDANA -> 0xFFE5484D;
			case GLASSES -> 0xFFF5B83D;
			case WINGS -> 0xFF9D8CFF;
			case BRACELET -> 0xFFE0B15A;
			case HALO -> 0xFF7FD6FF;
			case EARS -> 0xFFFF8FB1;
			case BACKPACK -> 0xFF5B87EA;
			case HAT -> 0xFFB48CFF;
			case TAIL -> 0xFFF08A3C;
			case PET -> 0xFF5AD6A0;
			case SCARF -> 0xFFE86A6A;
			case MASK -> 0xFFC8D0DC;
		};
	}

	/** Pixel size of each type's design textures, read from the PNGs under textures/cosmetics. */
	private static int[] sheetSize(Accessories.Type t) {
		return switch (t) {
			case BANDANA -> new int[]{36, 11};
			case GLASSES -> new int[]{24, 10};
			case WINGS -> new int[]{64, 32};
			case BRACELET -> new int[]{20, 7};
			case HALO, EARS -> new int[]{16, 8};
			case BACKPACK, HAT, TAIL, PET, SCARF, MASK -> new int[]{64, 32};
		};
	}

	// ---------------------------------------------------------------- settings

	private static CosmeticsModule module() {
		return ModuleManager.get(CosmeticsModule.class);
	}

	private static String selectedCape() {
		CosmeticsModule cm = module();
		return cm == null ? NONE : cm.cape.get();
	}

	private static String selectedAccessory(Accessories.Type t) {
		CosmeticsModule cm = module();
		ChoiceSetting cs = cm == null ? null : cm.accessorySetting(t);
		String s = cs == null ? null : cs.get();
		return s == null ? NONE : s;
	}

	private static boolean storeCape(String name) {
		CosmeticsModule cm = module();
		if (cm == null || !cm.cape.options().contains(name)) return false;
		cm.cape.set(name);
		return true;
	}

	private static boolean storeAccessory(Accessories.Type t, String name) {
		CosmeticsModule cm = module();
		if (cm == null) return false;
		ChoiceSetting s = cm.accessorySetting(t);
		if (s == null || !s.options().contains(name)) return false;
		s.set(name);
		return true;
	}

	private static void setCape(String name) {
		if (storeCape(name)) Config.save();
	}

	private static void setAccessory(Accessories.Type t, String name) {
		if (storeAccessory(t, name)) Config.save();
	}

	private void randomize() {
		if (module() == null) return;
		storeCape(Capes.ALL.get(random.nextInt(Capes.ALL.size())).name());
		for (Accessories.Type t : Accessories.Type.values()) {
			int pick = random.nextInt(t.DESIGNS.length + 1);
			storeAccessory(t, pick == 0 ? NONE : t.DESIGNS[pick - 1]);
		}
		Config.save();
	}

	/** One click: the Glacier cape plus the Glacier design (first entry) of every accessory type. */
	private void equipGlacier() {
		if (module() == null) return;
		storeCape(Capes.GLACIER_SET);
		for (Accessories.Type t : Accessories.Type.values()) storeAccessory(t, t.glacier());
		Config.save();
	}

	private void unequipAll() {
		if (module() == null) return;
		storeCape(NONE);
		for (Accessories.Type t : Accessories.Type.values()) storeAccessory(t, NONE);
		Config.save();
	}
}
