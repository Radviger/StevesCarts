package vswe.stevescarts.guis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import vswe.stevescarts.modules.data.ModuleData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class GuiBase extends GuiContainer {
	private int myOwnEventButton;
	private long myOwnTimeyWhineyThingy;
	private int myOwnTouchpadTimeWhineyThingy;

	public GuiBase(final Container container) {
		super(container);
		myOwnEventButton = 0;
		myOwnTimeyWhineyThingy = 0L;
		myOwnTouchpadTimeWhineyThingy = 0;
	}

	public void drawMouseOver(final String str, final int x, final int y) {
		final String[] split = str.split("\n");
		final List<String> text = new ArrayList<>(Arrays.asList(split));
        drawHoveringText(text, x, y);
	}

	@Override
	protected void renderToolTip(ItemStack stack, int x, int y) {
		FontRenderer font = stack.getItem().getFontRenderer(stack);
		net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(stack);
		this.drawHoveringText(this.getItemToolTip(stack), x, y, (font == null ? fontRenderer : font));
		net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
	}

	public boolean inRect(final int x, final int y, final int[] coords) {
		return coords != null && x >= coords[0] && x < coords[0] + coords[2] && y >= coords[1] && y < coords[1] + coords[3];
	}

	public Minecraft getMinecraft() {
		return mc;
	}

	public FontRenderer getFontRenderer() {
		return fontRenderer;
	}

	public void setXSize(final int val) {
		xSize = val;
		guiLeft = (width - xSize) / 2;
	}

	public void setYSize(final int val) {
		ySize = val;
		guiTop = (height - ySize) / 2;
	}

	public int getXSize() {
		return xSize;
	}

	public int getYSize() {
		return ySize;
	}

	public int getGuiLeft() {
		return guiLeft;
	}

	public int getGuiTop() {
		return guiTop;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(final int x, final int y) {
		drawGuiForeground(x, y);
	}

	public void drawGuiForeground(final int x, final int y) {
	}

	@Override
	public void drawDefaultBackground() {
		super.drawDefaultBackground();
		startScaling();
	}

	private int scaleX(float x) {
		final float scale = getScale();
		x /= scale;
		x += getGuiLeft();
		x -= (width - xSize * scale) / (2.0f * scale);
		return (int) x;
	}

	private int scaleY(float y) {
		final float scale = getScale();
		y /= scale;
		y += getGuiTop();
		y -= (height - ySize * scale) / (2.0f * scale);
		return (int) y;
	}

	@Override
	public void drawScreen(final int x, final int y, final float f) {
		this.drawDefaultBackground();
		super.drawScreen(scaleX(x), scaleY(y), f);
		this.renderHoveredToolTip(scaleX(x), scaleY(y));
		GlStateManager.popMatrix();
	}

	protected float getScale() {
		final ScaledResolution scaledresolution = new ScaledResolution(mc);
		final float w = scaledresolution.getScaledWidth() * 0.9f;
		final float h = scaledresolution.getScaledHeight() * 0.9f;
		final float multX = w / getXSize();
		final float multY = h / getYSize();
		float mult = Math.min(multX, multY);
		if (mult > 1.0f) {
			mult = 1.0f;
		}
		return mult;
	}

	private void startScaling() {
		GlStateManager.pushMatrix();
		final float scale = getScale();
		GlStateManager.scale(scale, scale, 1.0f);
		GlStateManager.translate((-guiLeft), (-guiTop), 0.0f);
		GlStateManager.translate((width - xSize * scale) / (2.0f * scale), (height - ySize * scale) / (2.0f * scale), 0.0f);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(final float f, final int x, final int y) {
		drawGuiBackground(f, x, y);
	}

	public void drawGuiBackground(final float f, final int x, final int y) {
	}

	@Override
	protected void mouseClicked(int x, int y, final int button) throws IOException {
		x = scaleX(x);
		y = scaleY(y);
		super.mouseClicked(x, y, button);
		mouseClick(x, y, button);
	}

	public void mouseClick(final int x, final int y, final int button) {
	}

	protected void mouseMovedOrUp(int x, int y, final int button) {
		x = scaleX(x);
		y = scaleY(y);
		//super.mouseMovedOrUp(x, y, button);
		mouseMoved(x, y, button);
		mouseDraged(x, y, button);
	}

	@Override
	public void handleMouseInput() throws IOException {
		final int mouseX = Mouse.getEventX() * width / mc.displayWidth;
		final int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
		if (Mouse.getEventButtonState()) {
			if (mc.gameSettings.touchscreen && myOwnTouchpadTimeWhineyThingy++ > 0) {
				return;
			}
			myOwnEventButton = Mouse.getEventButton();
			myOwnTimeyWhineyThingy = Minecraft.getSystemTime();
			mouseClicked(mouseX, mouseY, myOwnEventButton);
		} else if (Mouse.getEventButton() != -1) {
			if (mc.gameSettings.touchscreen && --myOwnTouchpadTimeWhineyThingy > 0) {
				return;
			}
			myOwnEventButton = -1;
			mouseReleased(mouseX, mouseY, Mouse.getEventButton());
			mouseMovedOrUp(mouseX, mouseY, Mouse.getEventButton());
		} else if (myOwnEventButton != -1 && myOwnTimeyWhineyThingy > 0L) {
			final long k = Minecraft.getSystemTime() - myOwnTimeyWhineyThingy;
			mouseClickMove(mouseX, mouseY, myOwnEventButton, k);
		} else {
			mouseMovedOrUp(mouseX, mouseY, -1);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		mouseX = scaleX(mouseX);
		mouseY = scaleY(mouseY);
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	protected void mouseClickMove(int x, int y, final int button, final long timeSinceClick) {
		x = scaleX(x);
		y = scaleY(y);
		super.mouseClickMove(x, y, button, timeSinceClick);
		mouseMoved(x, y, -1);
		mouseDraged(x, y, button);
	}

	public void mouseMoved(final int x, final int y, final int button) {
	}

	public void mouseDraged(final int x, final int y, final int button) {
	}

	@Override
	protected void keyTyped(final char character, final int extraInformation) throws IOException {
		if (extraInformation == 1 || !disableStandardKeyFunctionality()) {
			super.keyTyped(character, extraInformation);
		}
		keyPress(character, extraInformation);
	}

	public boolean disableStandardKeyFunctionality() {
		return false;
	}

	public void keyPress(final char character, final int extraInformation) {
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	public void enableKeyRepeat(final boolean val) {
		Keyboard.enableRepeatEvents(val);
	}

	public float getZLevel() {
		return zLevel;
	}

	public void drawIcon(final TextureAtlasSprite icon, final int targetX, final int targetY, final float sizeX, final float sizeY, final float offsetX, final float offsetY) {
		final Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(7, DefaultVertexFormats.POSITION_TEX);

		float x = icon.getMinU() + offsetX * (icon.getMaxU() - icon.getMinU());
		float y = icon.getMinV() + offsetY * (icon.getMaxV() - icon.getMinV());
		float width = (icon.getMaxU() - icon.getMinU()) * sizeX;
		float height = (icon.getMaxV() - icon.getMinV()) * sizeY;

		buf.pos(targetX + 0, 			targetY + 16 * sizeY, 	this.getZLevel()).tex(x + 0, 			y + height).endVertex();
		buf.pos(targetX + 16 * sizeX, 	targetY + 16 * sizeY, 	this.getZLevel()).tex(x + width, 		y + height).endVertex();
		buf.pos(targetX + 16 * sizeX, 	targetY + 0, 			this.getZLevel()).tex(x + width, 		y + 0).endVertex();
		buf.pos(targetX + 0, 			targetY + 0, 			this.getZLevel()).tex(x + 0, 			y + 0).endVertex();
		tessellator.draw();
	}

	public void drawModuleIcon(ModuleData icon, final int targetX, final int targetY, final float sizeX, final float sizeY, final float offsetX, final float offsetY) {
		RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
		IBakedModel bakedModel = itemRenderer.getItemModelWithOverrides(icon.getItemStack(), null, Minecraft.getMinecraft().player);

		this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		List<BakedQuad> quads = new LinkedList<>();
		for (EnumFacing enumfacing : EnumFacing.values())
		{
			quads.addAll(bakedModel.getQuads(null, enumfacing, 0L));
		}
		quads.addAll(bakedModel.getQuads(null, null, 0));
		for (BakedQuad quad : quads) {
			drawIcon(quad.getSprite(), targetX, targetY, sizeX, sizeY, offsetX, offsetY);
		}
	}

	public void drawTexturedModalRect(final int x, final int y, final int u, final int v, final int w, final int h, final RENDER_ROTATION rotation) {
		final float fw = 0.00390625f;
		final float fy = 0.00390625f;

		final double a = (u + 0) * fw;
		final double b = (u + w) * fw;
		final double c = (v + h) * fy;
		final double d = (v + 0) * fy;

		final double[] ptA = { a, c };
		final double[] ptB = { b, c };
		final double[] ptC = { b, d };
		final double[] ptD = { a, d };

		double [] pt1, pt2, pt3, pt4;

		switch (rotation) {
			default: {
				pt1 = ptA;
				pt2 = ptB;
				pt3 = ptC;
				pt4 = ptD;
				break;
			}
			case ROTATE_90: {
				pt1 = ptB;
				pt2 = ptC;
				pt3 = ptD;
				pt4 = ptA;
				break;
			}
			case ROTATE_180: {
				pt1 = ptC;
				pt2 = ptD;
				pt3 = ptA;
				pt4 = ptB;
				break;
			}
			case ROTATE_270: {
				pt1 = ptD;
				pt2 = ptA;
				pt3 = ptB;
				pt4 = ptC;
				break;
			}
			case FLIP_HORIZONTAL: {
				pt1 = ptB;
				pt2 = ptA;
				pt3 = ptD;
				pt4 = ptC;
				break;
			}
			case ROTATE_90_FLIP: {
				pt1 = ptA;
				pt2 = ptD;
				pt3 = ptC;
				pt4 = ptB;
				break;
			}
			case FLIP_VERTICAL: {
				pt1 = ptD;
				pt2 = ptC;
				pt3 = ptB;
				pt4 = ptA;
				break;
			}
			case ROTATE_270_FLIP: {
				pt1 = ptC;
				pt2 = ptB;
				pt3 = ptA;
				pt4 = ptD;
				break;
			}
		}

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buff = tessellator.getBuffer();
		buff.begin(7, DefaultVertexFormats.POSITION_TEX);
		buff.pos((x + 0), y + h, zLevel).tex(pt1[0], pt1[1]).endVertex();
		buff.pos((x + w), y + h, zLevel).tex(pt2[0], pt2[1]).endVertex();
		buff.pos((x + w), y + 0, zLevel).tex(pt3[0], pt3[1]).endVertex();
		buff.pos((x + 0), y + 0, zLevel).tex(pt4[0], pt4[1]).endVertex();
		tessellator.draw();
	}

	public enum RENDER_ROTATION {
		NORMAL,
		ROTATE_90,
		ROTATE_180,
		ROTATE_270,
		FLIP_HORIZONTAL,
		ROTATE_90_FLIP,
		FLIP_VERTICAL,
		ROTATE_270_FLIP;

		public RENDER_ROTATION getNextRotation() {
			switch (this) {
				default: {
					return RENDER_ROTATION.ROTATE_90;
				}
				case ROTATE_90: {
					return RENDER_ROTATION.ROTATE_180;
				}
				case ROTATE_180: {
					return RENDER_ROTATION.ROTATE_270;
				}
				case ROTATE_270: {
					return RENDER_ROTATION.NORMAL;
				}
				case FLIP_HORIZONTAL: {
					return RENDER_ROTATION.ROTATE_90_FLIP;
				}
				case ROTATE_90_FLIP: {
					return RENDER_ROTATION.FLIP_VERTICAL;
				}
				case FLIP_VERTICAL: {
					return RENDER_ROTATION.ROTATE_270_FLIP;
				}
				case ROTATE_270_FLIP: {
					return RENDER_ROTATION.FLIP_HORIZONTAL;
				}
			}
		}

		public RENDER_ROTATION getFlippedRotation() {
			switch (this) {
				default: {
					return RENDER_ROTATION.FLIP_HORIZONTAL;
				}
				case ROTATE_90: {
					return RENDER_ROTATION.ROTATE_90_FLIP;
				}
				case ROTATE_180: {
					return RENDER_ROTATION.FLIP_VERTICAL;
				}
				case ROTATE_270: {
					return RENDER_ROTATION.ROTATE_270_FLIP;
				}
				case FLIP_HORIZONTAL: {
					return RENDER_ROTATION.NORMAL;
				}
				case ROTATE_90_FLIP: {
					return RENDER_ROTATION.ROTATE_90;
				}
				case FLIP_VERTICAL: {
					return RENDER_ROTATION.ROTATE_180;
				}
				case ROTATE_270_FLIP: {
					return RENDER_ROTATION.ROTATE_270;
				}
			}
		}
	}
}
