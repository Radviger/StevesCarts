package vswe.stevescarts.guis;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import vswe.stevescarts.StevesCarts;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ModuleCountPair;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.network.message.MessageReturnCart;

import java.io.IOException;
import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class GuiMinecart extends GuiBase {
	private static ResourceLocation textureLeft;
	private static ResourceLocation textureRight;
	private static ResourceLocation textureReturn;
	private boolean isScrolling;
	private int[] scrollBox;
	private EntityMinecartModular cart;

	private int[] returnButton;

	public GuiMinecart(final InventoryPlayer invPlayer, final EntityMinecartModular cart) {
		super(cart.getCon(invPlayer));
		scrollBox = new int[] { 450, 15, 18, 225 };
		returnButton = new int[] { 324, 173, 24, 12 };
		setup(cart);
	}

	protected void setup(final EntityMinecartModular cart) {
		this.cart = cart;
		setXSize(478);
		setYSize(256);
	}

	@Override
	public void drawGuiForeground(final int x, final int y) {
		GlStateManager.disableLighting();
		if (cart.getModules() != null) {
			final ModuleBase thief = cart.getInterfaceThief();
			if (thief != null) {
				drawModuleForeground(thief);
				drawModuleMouseOver(thief, x, y);
			} else {
				for (final ModuleBase module : cart.getModules()) {
					drawModuleForeground(module);
				}
				renderModuleListText(x, y);
				for (final ModuleBase module : cart.getModules()) {
					drawModuleMouseOver(module, x, y);
				}
				renderModuleListMouseOver(x, y);
				renderReturnMouseOver(x, y);
			}
		}
		GlStateManager.enableLighting();
	}

	@Override
	public void drawGuiBackground(final float f, final int x, final int y) {
		GlStateManager.disableLighting();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		final int j = getGuiLeft();
		final int k = getGuiTop();
		ResourceHelper.bindResource(GuiMinecart.textureLeft);
		drawTexturedModalRect(j, k, 0, 0, 256, ySize);
		ResourceHelper.bindResource(GuiMinecart.textureRight);
		drawTexturedModalRect(j + 256, k, 0, 0, xSize - 256, ySize);
		final ModuleBase thief = cart.getInterfaceThief();
		if (thief != null) {
			drawModuleSlots(thief);
			drawModuleBackground(thief, x, y);
			drawModuleBackgroundItems(thief, x, y);
			for (final ModuleBase module : cart.getModules()) {
				if (module.hasGui() && module.hasSlots()) {
					final ArrayList<SlotBase> slotsList = module.getSlots();
					for (final SlotBase slot : slotsList) {
						resetSlot(slot);
					}
				}
			}
		} else if (cart.getModules() != null) {
			drawTexturedModalRect(j + scrollBox[0], k + scrollBox[1], 222, 24, scrollBox[2], scrollBox[3]);
			drawTexturedModalRect(j + scrollBox[0] + 2, k + scrollBox[1] + 2 + cart.getScrollY(), 240, 26 + (cart.canScrollModules ? 0 : 25), 14, 25);
			for (final ModuleBase module : cart.getModules()) {
				drawModuleSlots(module);
			}
			for (final ModuleBase module : cart.getModules()) {
				drawModuleBackground(module, x, y);
			}
			renderModuleList(x, y);
			renderReturnButton(x, y);
			for (final ModuleBase module : cart.getModules()) {
				drawModuleBackgroundItems(module, x, y);
			}
		}
		GlStateManager.enableLighting();
	}

	private void renderModuleList(int x, int y) {
		x -= getGuiLeft();
		y -= getGuiTop();
		ArrayList<ModuleCountPair> moduleCounts = cart.getModuleCounts();

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		for (int i = 0; i < moduleCounts.size(); ++i) {
			ModuleCountPair count = moduleCounts.get(i);
			float alpha = inRect(x, y, getModuleDisplayX(i), getModuleDisplayY(i), 16, 16) ? 1.0f : 0.1f;

			GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
			drawModuleIcon(count.getData(), getGuiLeft() + getModuleDisplayX(i), getGuiTop() + getModuleDisplayY(i), 1.0f, 1.0f, 0.0f, 0.0f);
		}
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.popMatrix();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private void renderReturnButton(int x, int y) {
		x -= getGuiLeft();
		y -= getGuiTop();
		ResourceHelper.bindResource(GuiMinecart.textureReturn);
		int uy = inRect(x, y, returnButton) ? 12: 0;
		drawTexturedModalRect(returnButton[0] + getGuiLeft(), returnButton[1] + getGuiTop(), 0, uy, returnButton[2], returnButton[3]);
	}

	private void renderModuleListText(int x, int y) {
		x -= getGuiLeft();
		y -= getGuiTop();
		ArrayList<ModuleCountPair> moduleCounts = cart.getModuleCounts();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		getFontRenderer().drawString(cart.getCartName(), 5, 172, 4210752);
		GlStateManager.enableBlend();
		for (int i = 0; i < moduleCounts.size(); ++i) {
			ModuleCountPair count = moduleCounts.get(i);
			if (count.getCount() != 1) {
				int alpha = (int) ((inRect(x, y, getModuleDisplayX(i), getModuleDisplayY(i), 16, 16) ? 1.0f : 0.75f) * 256.0f);
				String str = String.valueOf(count.getCount());
				getFontRenderer().drawStringWithShadow(str, getModuleDisplayX(i) + 16 - getFontRenderer().getStringWidth(str), getModuleDisplayY(i) + 8, 0xFFFFFF | alpha << 24);
			}
		}
		GlStateManager.disableBlend();
	}

	private void renderModuleListMouseOver(int x, int y) {
		x -= getGuiLeft();
		y -= getGuiTop();
		ArrayList<ModuleCountPair> moduleCounts = cart.getModuleCounts();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		for (int i = 0; i < moduleCounts.size(); ++i) {
			final ModuleCountPair count = moduleCounts.get(i);
			if (inRect(x, y, getModuleDisplayX(i), getModuleDisplayY(i), 16, 16)) {
				drawMouseOver(count.toString(), x, y);
			}
		}
	}

	private void renderReturnMouseOver(int x, int y) {
		x -= getGuiLeft();
		y -= getGuiTop();

		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (inRect(x, y, returnButton)) {
			drawMouseOver(Localization.GUI.CART.RETURN.translate(), x, y);
		}
	}

	private int getModuleDisplayX(int id) {
		return id % 8 * 18 + 7;
	}

	private int getModuleDisplayY(int id) {
		return id / 8 * 18 + 182;
	}

	@Override
	public void mouseClick(int x, int y, int button) {
		super.mouseClick(x, y, button);
		ModuleBase thief = cart.getInterfaceThief();
		if (thief != null) {
			handleModuleMouseClicked(thief, x, y, button);
		} else if (cart.getModules() != null) {
			if (inRect(x - getGuiLeft(), y - getGuiTop(), scrollBox[0], scrollBox[1], scrollBox[2], scrollBox[3])) {
				isScrolling = true;
			}
			for (ModuleBase module : cart.getModules()) {
				handleModuleMouseClicked(module, x, y, button);
			}
			if (inRect(x - getGuiLeft(), y - getGuiTop(), returnButton)) {
				StevesCarts.NET.sendToServer(new MessageReturnCart());
			}
		}
	}

	protected boolean inRect(final int x, final int y, final int x1, final int y1, final int sizeX, final int sizeY) {
		return x >= x1 && x <= x1 + sizeX && y >= y1 && y <= y1 + sizeY;
	}

	@Override
	public void mouseMoved(final int x, final int y, final int button) {
		super.mouseMoved(x, y, button);
		if (isScrolling) {
			int temp = y - getGuiTop() - 12 - (scrollBox[1] + 2);
			if (temp < 0) {
				temp = 0;
			} else if (temp > 198) {
				temp = 198;
			}
			cart.setScrollY(temp);
		}
		if (button != -1) {
			isScrolling = false;
		}
		if (cart.getModules() != null) {
			final ModuleBase thief = cart.getInterfaceThief();
			if (thief != null) {
				handleModuleMouseMoved(thief, x, y, button);
			} else {
				for (final ModuleBase module : cart.getModules()) {
					handleModuleMouseMoved(module, x, y, button);
				}
			}
		}
	}

	@Override
	public void keyPress(final char character, final int extraInformation) {
		super.keyPress(character, extraInformation);
		if (cart.getModules() != null) {
			final ModuleBase thief = cart.getInterfaceThief();
			if (thief != null) {
				handleModuleKeyPress(thief, character, extraInformation);
			} else {
				for (final ModuleBase module : cart.getModules()) {
					handleModuleKeyPress(module, character, extraInformation);
				}
			}
		}
	}

	@Override
	public boolean disableStandardKeyFunctionality() {
		if (cart.getModules() != null) {
			final ModuleBase thief = cart.getInterfaceThief();
			if (thief != null) {
				return thief.disableStandardKeyFunctionality();
			}
			for (final ModuleBase module : cart.getModules()) {
				if (module.disableStandardKeyFunctionality()) {
					return true;
				}
			}
		}
		return false;
	}

	private void drawModuleForeground(final ModuleBase module) {
		if (module.hasGui()) {
			module.drawForeground(this);
			if (module.useButtons()) {
				module.drawButtonText(this);
			}
		}
	}

	private void drawModuleMouseOver(final ModuleBase module, final int x, final int y) {
		if (module.hasGui()) {
			module.drawMouseOver(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY());
			if (module.useButtons()) {
				module.drawButtonOverlays(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY());
			}
		}
	}

	private void drawModuleSlots(final ModuleBase module) {
		if (module.hasGui() && module.hasSlots()) {
			final ArrayList<SlotBase> slotsList = module.getSlots();
			for (final SlotBase slot : slotsList) {
				final int[] rect = { slot.getX() + 1, slot.getY() + 1, 16, 16 };
				module.handleScroll(rect);
				final boolean drawAll = rect[3] == 16;
				if (drawAll) {
					slot.xPos = slot.getX() + module.getX() + 1;
					slot.yPos = slot.getY() + module.getY() + 1 - cart.getRealScrollY();
				} else {
					resetSlot(slot);
				}
				module.drawImage(this, slot.getX(), slot.getY(), xSize - 256, 0, 18, 18);
				if (!drawAll) {
					module.drawImage(this, slot.getX() + 1, slot.getY() + 1, xSize - 256 + 18, 1, 16, 16);
				}
			}
		}
	}

	private void resetSlot(final SlotBase slot) {
		slot.xPos = -9001;
		slot.yPos = -9001;
	}

	private void drawModuleBackground(final ModuleBase module, final int x, final int y) {
		if (module.hasGui()) {
			module.drawBackground(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY());
			if (module.useButtons()) {
				module.drawButtons(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY());
			}
		}
	}

	private void drawModuleBackgroundItems(final ModuleBase module, final int x, final int y) {
		if (module.hasGui()) {
			module.drawBackgroundItems(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY());
		}
	}

	private void handleModuleMouseClicked(final ModuleBase module, final int x, final int y, final int button) {
		module.mouseClicked(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY(), button);
		if (module.useButtons()) {
			module.mouseClickedButton(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY(), button);
		}
	}

	private void handleModuleMouseMoved(final ModuleBase module, final int x, final int y, final int button) {
		module.mouseMovedOrUp(this, x - getGuiLeft() - module.getX(), y - getGuiTop() - module.getY(), button);
	}

	private void handleModuleKeyPress(final ModuleBase module, final char character, final int extraInformation) {
		module.keyPress(this, character, extraInformation);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int d = Mouse.getDWheel();
		if (d != -1) {
			int x = Mouse.getEventX() * width / mc.displayWidth;
			int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;

			if (d < 0) {
				d = -1;
			}
			if(d > 0)
			{
				d = 1;
			}

			if (this.inRect(x - getGuiLeft(), y - getGuiTop(),  0, 0, xSize, ySize))
			{
				int moduleSize = this.cart.modularSpaceHeight;
				int scroll = cart.getScrollY() + (-d * 7500) / (moduleSize - EntityMinecartModular.MODULAR_SPACE_HEIGHT);
				scroll = MathHelper.clamp(scroll, 0, 198);
				cart.setScrollY(scroll);
			}
		}
	}

	static {
		GuiMinecart.textureLeft = ResourceHelper.getResource("/gui/guiBase1.png");
		GuiMinecart.textureRight = ResourceHelper.getResource("/gui/guiBase2.png");
		GuiMinecart.textureReturn = ResourceHelper.getResource("/gui/return.png");
	}
}
