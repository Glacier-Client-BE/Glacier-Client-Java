/*
 * Research notes (1.21.11, Mojang names, verified with javap against the merged Minecraft jar and fabric-rendering-v1 16.2.10):
 *
 * - Player renderer: net.minecraft.client.renderer.entity.player.AvatarRenderer<AvatarlikeEntity>
 *   extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel>, which implements
 *   RenderLayerParent<S, M> (getModel()). LivingEntityRenderer.layers is a protected list, addLayer(RenderLayer) is protected.
 * - Render state: net.minecraft.client.renderer.entity.state.AvatarRenderState extends HumanoidRenderState extends
 *   LivingEntityRenderState extends EntityRenderState. Useful fields: int id (set from Avatar.getId()), float ageInTicks,
 *   boolean isInvisible, Pose pose (Pose.CROUCHING), PlayerSkin skin.
 * - Layer base: net.minecraft.client.renderer.entity.layers.RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>>
 *   ctor RenderLayer(RenderLayerParent<S,M>); abstract void submit(com.mojang.blaze3d.vertex.PoseStack,
 *   net.minecraft.client.renderer.SubmitNodeCollector, int light, S state, float, float); M getParentModel().
 *   CapeLayer (AvatarRenderState, PlayerModel) is the template: pushPose, parent body.translateAndRotate(pose),
 *   submit, popPose.
 * - Submitting: SubmitNodeCollector extends OrderedSubmitNodeCollector, method
 *   submitModelPart(ModelPart, PoseStack, RenderType, int light, int overlay, TextureAtlasSprite sprite, boolean, boolean)
 *   (sprite null is what CapeLayer effectively passes; the two booleans are passed false,false).
 * - RenderType: net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(Identifier) (memoized
 *   internally). OverlayTexture.NO_OVERLAY in net.minecraft.client.renderer.texture.
 * - PlayerModel: net.minecraft.client.model.player.PlayerModel extends HumanoidModel<AvatarRenderState>, public final
 *   fields head, body, rightArm, leftArm (ModelPart). ModelPart.translateAndRotate(PoseStack) applies the part's pose.
 * - Baking without a layer definition: net.minecraft.client.model.geom.builders.MeshDefinition.getRoot()
 *   .addOrReplaceChild(String, CubeListBuilder, PartPose), then LayerDefinition.create(mesh, texW, texH).bakeRoot().
 *   CubeListBuilder: create(), texOffs(int,int), mirror(), addBox(x,y,z,w,h,d).
 * - PoseStack (com.mojang.blaze3d.vertex): translate(float,float,float), mulPose(Quaternionfc), pushPose, popPose.
 *   Quaternionf from org.joml (joml 1.10.8): rotationX(float), rotationY(float).
 * - Registration: fabric-rendering-v1 16.2.10 has
 *   net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT, callback
 *   (EntityType, LivingEntityRenderer, RegistrationHelper, EntityRendererProvider.Context); helper.register(RenderLayer<T, ? extends EntityModel<T>>).
 *   Used instead of a mixin, so glacier.mixins.json is untouched.
 *
 * Not verified in-game: the exact coordinate placement of the accessories, the slim-arm bracelet fit, and whether
 * the Fabric callback fires for the player renderer before the first frame (it is documented to).
 */
package net.glacier.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.glacier.cosmetics.Accessories.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the local player's selected accessories. Other players never get this layer's output, because the selection
 * lives only in the client-side cosmetics settings. The layer runs on the render thread, so the shared scratch
 * quaternion is safe.
 */
public final class AccessoryLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float PX = 1.0F / 16.0F;
	private static final float WING_YAW = 0.5236F;
	private static final float CROUCH_TILT = -0.25F;
	private static final Quaternionf ROT = new Quaternionf();
	/** Per-segment pitch of the tail; poses accumulate, so it droops from the back and curls up at the tip. */
	private static final float[] TAIL_PITCH = {-0.55F, -0.15F, 0.35F, 0.3F};

	private final Map<Identifier, RenderType> renderTypes = new HashMap<>();

	public AccessoryLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	/** Adds the layer to every player renderer. Call once from the client initializer. */
	public static void register() {
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
			if (entityRenderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new AccessoryLayer(avatar));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
			float limbSwing, float limbAmount) {
		net.glacier.module.impl.CosmeticsModule.bindRenderState(state);
		if (state.isInvisible || !isLocalPlayer(state)) return;

		PlayerModel model = getParentModel();
		float age = state.ageInTicks;

		String design = Accessories.selected(Type.EARS);
		if (design != null) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			float phase = age % 100.0F;
			float twitch = phase < 6.0F ? Mth.sin(phase * 1.0472F) * 0.12F : 0.0F;
			ROT.rotationZ(twitch);
			pose.mulPose(ROT);
			draw(collector, pose, light, AccessoryModels.ears(), Accessories.texture(Type.EARS, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.BANDANA);
		if (design != null) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.bandana(), Accessories.texture(Type.BANDANA, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.GLASSES);
		if (design != null) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.glasses(), Accessories.texture(Type.GLASSES, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.HALO);
		if (design != null) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			pose.translate(0.0F, Mth.sin(age * 0.1D) * 0.5F * PX, 0.0F);
			ROT.rotationY(age * 0.03F);
			pose.mulPose(ROT);
			draw(collector, pose, FULL_BRIGHT, AccessoryModels.halo(), Accessories.texture(Type.HALO, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.BRACELET);
		if (design != null) {
			Identifier texture = Accessories.texture(Type.BRACELET, design);
			pose.pushPose();
			model.rightArm.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.braceletRight(), texture);
			pose.popPose();
			pose.pushPose();
			model.leftArm.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.braceletLeft(), texture);
			pose.popPose();
		}

		design = Accessories.selected(Type.WINGS);
		if (design != null) {
			Identifier texture = Accessories.texture(Type.WINGS, design);
			boolean crouching = state.pose == Pose.CROUCHING;
			float speed = Mth.clamp(limbAmount, 0.0F, 1.0F);
			float flap = Mth.sin(age * (0.12D + 0.2D * speed)) * (0.12F + 0.18F * speed);
			flap += Mth.sin(age * 0.05D) * 0.04F;
			pose.pushPose();
			model.body.translateAndRotate(pose);
			pose.translate(0.0F, 2.0F * PX, 2.5F * PX);

			pose.pushPose();
			applyTilt(pose, crouching);
			ROT.rotationY(WING_YAW + flap);
			pose.mulPose(ROT);
			draw(collector, pose, light, AccessoryModels.wingRight(), texture);
			pose.popPose();

			pose.pushPose();
			applyTilt(pose, crouching);
			ROT.rotationY(-WING_YAW - flap);
			pose.mulPose(ROT);
			draw(collector, pose, light, AccessoryModels.wingLeft(), texture);
			pose.popPose();

			pose.popPose();
		}

		design = Accessories.selected(Type.HAT);
		if (design != null) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.hat(), Accessories.texture(Type.HAT, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.MASK);
		if (design != null) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.mask(), Accessories.texture(Type.MASK, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.BACKPACK);
		if (design != null) {
			pose.pushPose();
			model.body.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.backpack(), Accessories.texture(Type.BACKPACK, design));
			pose.popPose();
		}

		design = Accessories.selected(Type.SCARF);
		if (design != null) {
			Identifier texture = Accessories.texture(Type.SCARF, design);
			float speed = Mth.clamp(limbAmount, 0.0F, 1.0F);
			float lean = -0.05F - 0.35F * speed + Mth.sin(age * 0.09F) * 0.05F;
			pose.pushPose();
			model.body.translateAndRotate(pose);
			draw(collector, pose, light, AccessoryModels.scarfCollar(), texture);
			hang(collector, pose, light, AccessoryModels.scarfFlapA(), texture, 2.0F, 2.0F, -3.0F, lean);
			hang(collector, pose, light, AccessoryModels.scarfFlapB(), texture, -1.0F, 2.0F, -3.0F,
					lean * 0.8F + Mth.sin(age * 0.11F + 1.3F) * 0.04F);
			pose.popPose();
		}

		design = Accessories.selected(Type.TAIL);
		if (design != null) {
			Identifier texture = Accessories.texture(Type.TAIL, design);
			float speed = Mth.clamp(limbAmount, 0.0F, 1.0F);
			float wagAmp = 0.22F + 0.2F * speed;
			float wagT = age * (0.1F + 0.12F * speed);
			pose.pushPose();
			model.body.translateAndRotate(pose);
			pose.translate(0.0F, 10.0F * PX, 2.0F * PX);
			for (int i = 0; i < TAIL_PITCH.length; i++) {
				ROT.rotationX(TAIL_PITCH[i]).rotateY(Mth.sin(wagT - i * 0.7F) * wagAmp * (0.5F + 0.2F * i));
				pose.mulPose(ROT);
				draw(collector, pose, light, AccessoryModels.tailSegment(i), texture);
				pose.translate(0.0F, 0.0F, AccessoryModels.tailLength(i) * PX);
			}
			pose.popPose();
		}

		design = Accessories.selected(Type.PET);
		if (design != null) {
			Identifier texture = Accessories.texture(Type.PET, design);
			pose.pushPose();
			model.body.translateAndRotate(pose);
			pose.translate(-6.0F * PX, Mth.sin(age * 0.1F) * 0.35F * PX, 0.0F);
			draw(collector, pose, light, AccessoryModels.petBody(), texture);

			pose.pushPose();
			pose.translate(0.0F, -3.0F * PX, -2.5F * PX);
			ROT.rotationY(Mth.sin(age * 0.05F) * 0.45F).rotateX(Mth.sin(age * 0.07F + 1.0F) * 0.12F);
			pose.mulPose(ROT);
			draw(collector, pose, light, AccessoryModels.petHead(), texture);
			pose.popPose();

			pose.pushPose();
			pose.translate(0.0F, -1.5F * PX, 2.5F * PX);
			ROT.rotationY(Mth.sin(age * 0.2F) * 0.5F);
			pose.mulPose(ROT);
			draw(collector, pose, light, AccessoryModels.petTail(), texture);
			pose.popPose();

			pose.popPose();
		}
	}

	/** Draws a hanging part (scarf end) pivoting about (x, y, z) in model pixels, swung about X by {@code swing}. */
	private void hang(SubmitNodeCollector collector, PoseStack pose, int light, ModelPart part, Identifier texture,
			float x, float y, float z, float swing) {
		pose.pushPose();
		pose.translate(x * PX, y * PX, z * PX);
		ROT.rotationX(swing);
		pose.mulPose(ROT);
		draw(collector, pose, light, part, texture);
		pose.popPose();
	}

	private static void applyTilt(PoseStack pose, boolean crouching) {
		if (!crouching) return;
		ROT.rotationX(CROUCH_TILT);
		pose.mulPose(ROT);
	}

	private void draw(SubmitNodeCollector collector, PoseStack pose, int light, ModelPart part, Identifier texture) {
		collector.submitModelPart(part, pose, renderType(texture), light, OverlayTexture.NO_OVERLAY, null, false, false);
	}

	private RenderType renderType(Identifier texture) {
		RenderType type = renderTypes.get(texture);
		if (type == null) {
			type = RenderTypes.entityTranslucent(texture);
			renderTypes.put(texture, type);
		}
		return type;
	}

	private static boolean isLocalPlayer(AvatarRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && state.id == mc.player.getId();
	}
}
