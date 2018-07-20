package vswe.stevescarts.renders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.storages.SCTank;
import vswe.stevescarts.models.ModelCartbase;
import vswe.stevescarts.modules.ModuleBase;

import java.util.ArrayList;

public class RendererCart<T extends EntityMinecartModular> extends Render<T> {
	public RendererCart(RenderManager renderManager) {
		super(renderManager);
		shadowSize = 0.5f;
	}

	@Override
	protected ResourceLocation getEntityTexture(T entity) {
		return null;
	}

	public void renderCart(final EntityMinecartModular cart, double x, double y, double z, float yaw, final float partialTickTime) {
		if (cart.getModules() != null) {
			for (final ModuleBase module : cart.getModules()) {
				if (!module.shouldCartRender()) {
					return;
				}
			}
		}
		GlStateManager.pushMatrix();
		final double partialPosX = cart.lastTickPosX + (cart.posX - cart.lastTickPosX) * partialTickTime;
		final double partialPosY = cart.lastTickPosY + (cart.posY - cart.lastTickPosY) * partialTickTime;
		final double partialPosZ = cart.lastTickPosZ + (cart.posZ - cart.lastTickPosZ) * partialTickTime;
		float partialRotPitch = cart.prevRotationPitch + (cart.rotationPitch - cart.prevRotationPitch) * partialTickTime;
		final Vec3d posFromRail = cart.getPos(partialPosX, partialPosY, partialPosZ);
		if (posFromRail != null && cart.canUseRail()) {
			final double predictionLength = 0.30000001192092896;
			Vec3d lastPos = cart.getPosOffset(partialPosX, partialPosY, partialPosZ, predictionLength);
			Vec3d nextPos = cart.getPosOffset(partialPosX, partialPosY, partialPosZ, -predictionLength);
			if (lastPos == null) {
				lastPos = posFromRail;
			}
			if (nextPos == null) {
				nextPos = posFromRail;
			}
			x += posFromRail.x - partialPosX;
			y += (lastPos.y + nextPos.y) / 2.0 - partialPosY;
			z += posFromRail.z - partialPosZ;
			Vec3d difference = nextPos.addVector(-lastPos.x, -lastPos.y, -lastPos.z);
			if (difference.lengthVector() != 0.0) {
				difference = difference.normalize();
				yaw = (float) (Math.atan2(difference.z, difference.x) * 180.0 / 3.141592653589793);
				partialRotPitch = (float) (Math.atan(difference.y) * 73.0);
			}
		}
		yaw = 180.0f - yaw;
		partialRotPitch *= -1.0f;
		float damageRot = cart.getRollingAmplitude() - partialTickTime;
		float damageTime = cart.getDamage() - partialTickTime;
		final float damageDir = cart.getRollingDirection();
		if (damageTime < 0.0f) {
			damageTime = 0.0f;
		}
		boolean flip = cart.motionX > 0.0 != cart.motionZ > 0.0;
		if (cart.cornerFlip) {
			flip = !flip;
		}
		if (cart.getRenderFlippedYaw(yaw + (flip ? 0.0f : 180.0f))) {
			flip = !flip;
		}
		GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
		GlStateManager.rotate(yaw, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(partialRotPitch, 0.0f, 0.0f, 1.0f);
		if (damageRot > 0.0f) {
			damageRot = MathHelper.sin(damageRot) * damageRot * damageTime / 10.0f * damageDir;
			GlStateManager.rotate(damageRot, 1.0f, 0.0f, 0.0f);
		}
		yaw += (flip ? 0.0f : 180.0f);
		GlStateManager.rotate(flip ? 0.0f : 180.0f, 0.0f, 1.0f, 0.0f);
		GlStateManager.scale(-1.0f, -1.0f, 1.0f);
		renderModels(cart, (float) (3.141592653589793 * yaw / 180.0), partialRotPitch, damageRot, 0.0625f, partialTickTime);
		GlStateManager.popMatrix();
		renderLabel(cart, x, y, z);
	}

	public void renderModels(final EntityMinecartModular cart, final float yaw, final float pitch, final float roll, final float mult, final float partialtime) {
		if (cart.getModules() != null) {
			for (final ModuleBase module : cart.getModules()) {
				if (module.haveModels()) {
					for (final ModelCartbase model : module.getModels()) {
						model.render(this, module, yaw, pitch, roll, mult, partialtime);
					}
				}
			}
		}
	}

	public void renderLiquidCuboid(final FluidStack fluid, final int tankSize, final float x, final float y, final float z, final float sizeX, final float sizeY, final float sizeZ, float mult) {
		if (fluid == null || fluid.getFluid() == null) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		ResourceLocation still = fluid.getFluid().getStill();
		TextureAtlasSprite sprite = mc.getTextureMapBlocks().getTextureExtry(still.toString());
		if (sprite == null) {
			return;
		}
		if (fluid.amount > 0) {
			float filled = fluid.amount / (float)tankSize;
			GlStateManager.pushMatrix();
			GlStateManager.translate(x * mult, (y + sizeY * (1.0f - filled) / 2.0f) * mult, z * mult);

			bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			SCTank.applyColorFilter(fluid);
			final float scale = 0.5f;
			GlStateManager.scale(scale, scale, scale);
			GlStateManager.disableLighting();
			mult /= scale;
			renderCuboid(sprite, sizeX * mult, sizeY * mult * filled, sizeZ * mult);
			GlStateManager.enableLighting();
			GlStateManager.disableBlend();
			GlStateManager.disableRescaleNormal();
			GlStateManager.popMatrix();

			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	private void renderCuboid(final TextureAtlasSprite icon, final double sizeX, final double sizeY, final double sizeZ) {
		renderFace(icon, sizeX, sizeZ, 0, 		90F, 	0F, 					-(float)(sizeY / 2),	0F					);
		renderFace(icon, sizeX, sizeZ, 0, 		-90F, 	0F, 					(float)(sizeY / 2), 	0F					);
		renderFace(icon, sizeX, sizeY, 0, 		0, 		0F, 					0F, 					(float)(sizeZ / 2)	);
		renderFace(icon, sizeX, sizeY, 180F, 	0F, 	0F, 					0F, 					-(float)(sizeZ / 2)	);
		renderFace(icon, sizeZ, sizeY, 90F, 	0, 		(float)(sizeX / 2), 	0F, 					0F					);
		renderFace(icon, sizeZ, sizeY, -90F, 	0F, 	-(float)(sizeX / 2), 	0F, 					0F					);
	}

	private void renderFace(final TextureAtlasSprite icon, final double totalTargetW, final double totalTargetH, final float yaw, final float roll, final float offX, final float offY, final float offZ) {
		GlStateManager.pushMatrix();

		GlStateManager.translate(offX, offY, offZ);
		GlStateManager.rotate(yaw, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(roll, 1.0f, 0.0f, 0.0f);

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buff = tess.getBuffer();
		double srcX = icon.getMinU();
		double srcY = icon.getMinV();

		double srcW = icon.getMaxU() - srcX;
		double srcH = icon.getMaxV() - srcY;

		double d = 0.001D;
		double currentTargetX = 0D;
		while (totalTargetW - currentTargetX > d * 2) {
			double currentTargetW = Math.min(totalTargetW - currentTargetX, 1D);
			double currentTargetY = 0D;
			while (totalTargetH - currentTargetY > d * 2) {
				double currentTargetH = Math.min(totalTargetH - currentTargetY, 1D);

				buff.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
				buff.pos(currentTargetX - totalTargetW / 2.0, 					currentTargetY - totalTargetH / 2.0, 0.0)					.tex(srcX, 									srcY)							.normal(0.0f, 1.0f, 0.0f).endVertex();
				buff.pos(currentTargetX + currentTargetW - totalTargetW / 2.0, 	currentTargetY - totalTargetH / 2.0, 0.0)					.tex(srcX + srcW * currentTargetW,		srcY)							.normal(0.0f, 1.0f, 0.0f).endVertex();
				buff.pos(currentTargetX + currentTargetW - totalTargetW / 2.0, 	currentTargetY + currentTargetH - totalTargetH / 2.0, 0.0).tex(srcX + srcW * currentTargetW,		srcY + srcH * currentTargetH)	.normal(0.0f, 1.0f, 0.0f).endVertex();
				buff.pos(currentTargetX - totalTargetW / 2.0, 					currentTargetY + currentTargetH - totalTargetH / 2.0, 0.0).tex(srcX,									srcY + srcH * currentTargetH)	.normal(0.0f, 1.0f, 0.0f).endVertex();
				tess.draw();
				currentTargetY += currentTargetH - d;
			}
			currentTargetX += currentTargetW  - d;
		}
		GlStateManager.popMatrix();
	}

	protected void renderLabel(final EntityMinecartModular cart, final double x, final double y, final double z) {
		final ArrayList<String> labels = cart.getLabel();
		if (labels != null && labels.size() > 0) {
			final float distance = cart.getDistance(renderManager.renderViewEntity);
			if (distance <= 64.0f) {
				final FontRenderer frend = getFontRendererFromRenderManager();
				final float var12 = 1.6f;
				final float var13 = 0.016666668f * var12;
				GlStateManager.pushMatrix();
				GlStateManager.translate((float) x + 0.0f, (float) y + 1.0f + (labels.size() - 1) * 0.12f, (float) z);
				GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f);
				GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
				GlStateManager.rotate(renderManager.playerViewX, 1.0f, 0.0f, 0.0f);
				GlStateManager.scale(-var13, -var13, var13);
				GlStateManager.disableLighting();
				GlStateManager.depthMask(false);
				GlStateManager.disableDepth();
				GlStateManager.enableBlend();
				GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				int boxwidth = 0;
				int boxheight = 0;
				for (final String label : labels) {
					boxwidth = Math.max(boxwidth, frend.getStringWidth(label));
					boxheight += frend.FONT_HEIGHT;
				}
				final int halfW = boxwidth / 2;
				final int halfH = boxheight / 2;
				final Tessellator tes = Tessellator.getInstance();
				BufferBuilder buffer = tes.getBuffer();
				GlStateManager.disableTexture2D();
				buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				buffer.pos(-halfW - 1, -halfH - 1, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
				buffer.pos(-halfW - 1, halfH + 1, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
				buffer.pos(halfW + 1, halfH + 1, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
				buffer.pos(halfW + 1, -halfH - 1, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex();
				tes.draw();
				GlStateManager.enableTexture2D();
				int yPos = -halfH;
				for (final String label2 : labels) {
					frend.drawString(label2, -frend.getStringWidth(label2) / 2, yPos, 553648127);
					yPos += frend.FONT_HEIGHT;
				}
				GlStateManager.enableDepth();
				GlStateManager.depthMask(true);
				yPos = -halfH;
				for (final String label2 : labels) {
					frend.drawString(label2, -frend.getStringWidth(label2) / 2, yPos, -1);
					yPos += frend.FONT_HEIGHT;
				}
				GlStateManager.enableLighting();
				GlStateManager.disableBlend();
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				GlStateManager.popMatrix();
			}
		}
	}

	@Override
	public void doRender(final EntityMinecartModular cart, final double x, final double y, final double z, final float yaw, final float partialTickTime) {
		renderCart(cart, x, y, z, yaw, partialTickTime);
	}
}
