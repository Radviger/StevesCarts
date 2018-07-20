package vswe.stevescarts.guis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.Constants;
import vswe.stevescarts.blocks.tileentities.TileEntityCartAssembler;
import vswe.stevescarts.containers.ContainerCartAssembler;
import vswe.stevescarts.containers.slots.SlotAssembler;
import vswe.stevescarts.helpers.*;
import vswe.stevescarts.items.ModItems;
import vswe.stevescarts.modules.data.ModuleData;
import vswe.stevescarts.modules.data.ModuleDataHull;
import vswe.stevescarts.network.message.MessageStevesCarts;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiCartAssembler extends GuiBase {
	private ArrayList<TextWithColor> statusLog;
	private boolean hasErrors;
	private boolean firstLoad;
	private static ResourceLocation[] backgrounds;
	private static final ResourceLocation textureLeft;
	private static final ResourceLocation textureRight;
	private static final ResourceLocation textureExtra;
	private int[] assembleRect;
	private String validChars;
	private int dropdownX;
	private int dropdownY;
	private int scrollingX;
	private int scrollingY;
	private boolean isScrolling;
	private int[] blackBackground;
	private TileEntityCartAssembler assembler;

	private final int[] assemblingProgRect = { 375, 180, 115, 11 };
	private final int[] fuelProgRect = { 375, 200, 115, 11 };

	public GuiCartAssembler(final InventoryPlayer invPlayer, final TileEntityCartAssembler assembler) {
		super(new ContainerCartAssembler(invPlayer, assembler));
		firstLoad = true;
		assembleRect = new int[] { 390, 160, 80, 11 };
		validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		dropdownX = -1;
		dropdownY = -1;
		blackBackground = new int[] { 145, 15, 222, 148 };
		this.assembler = assembler;
		setXSize(512);
		setYSize(256);
	}

	@Override
	public void drawGuiForeground(final int x, final int y) {
		getFontRenderer().drawString(Localization.GUI.ASSEMBLER.TITLE.translate(), 8, 6, 4210752);
		if (assembler.isErrorListOutdated) {
			updateErrorList();
			assembler.isErrorListOutdated = false;
		}
		final ArrayList<TextWithColor> lines = statusLog;
		if (lines != null) {
			int lineCount = lines.size();
			boolean dotdotdot = false;
			if (lineCount > 11) {
				lineCount = 10;
				dotdotdot = true;
			}
			for (int i = 0; i < lineCount; ++i) {
				final TextWithColor info = lines.get(i);
				if (info != null) {
					getFontRenderer().drawString(info.getText(), 375, 40 + i * 10, info.getColor());
				}
			}
			if (dotdotdot) {
				getFontRenderer().drawString("...", 375, 40 + lineCount * 10, 4210752);
			}
		}
		float assemblingProgress;
		String assemblingInfo;
		if (assembler.getIsAssembling()) {
			assemblingProgress = (float) assembler.getAssemblingTime() / (float) assembler.getMaxAssemblingTime();
			assemblingInfo = Localization.GUI.ASSEMBLER.ASSEMBLE_PROGRESS.translate() + ": " + formatProgress(assemblingProgress);
			assemblingInfo = assemblingInfo + "\n" + Localization.GUI.ASSEMBLER.TIME_LEFT.translate() + ": " + formatTime((int) ((assembler.getMaxAssemblingTime() - assembler.getAssemblingTime()) / assembler.getEfficiency()));
		} else {
			assemblingInfo = Localization.GUI.ASSEMBLER.IDLE_MESSAGE.translate();
		}
		if (!hasErrors) {
			if (assembler.getIsDisassembling()) {
				drawProgressBarInfo(assembleRect, x, y, Localization.GUI.ASSEMBLER.MODIFY_CART.translate());
			} else {
				drawProgressBarInfo(assembleRect, x, y, Localization.GUI.ASSEMBLER.ASSEMBLE_CART.translate());
			}
		}
		drawProgressBarInfo(assemblingProgRect, x, y, assemblingInfo);
		drawProgressBarInfo(fuelProgRect, x, y, Localization.GUI.ASSEMBLER.FUEL_LEVEL.translate() + ": " + assembler.getFuelLevel() + "/" + assembler.getMaxFuelLevel());
	}

	private void updateErrorList() {
		final ArrayList<TextWithColor> lines = new ArrayList<>();
		if (this.assembler.getStackInSlot(0).isEmpty()) {
			this.addText(lines, Localization.GUI.ASSEMBLER.ASSEMBLE_INSTRUCTION.translate());
			this.hasErrors = true;
		} else {
			final ModuleData hulldata = ModItems.MODULES.getModuleData(this.assembler.getStackInSlot(0));
			if (hulldata == null || !(hulldata instanceof ModuleDataHull)) {
				this.addText(lines, Localization.GUI.ASSEMBLER.INVALID_HULL.translate(), 10357518);
				this.hasErrors = true;
			} else {
				final ModuleDataHull hull = (ModuleDataHull) hulldata;
				this.addText(lines, Localization.GUI.ASSEMBLER.HULL_CAPACITY.translate() + ": " + hull.getCapacity());
				this.addText(lines, Localization.GUI.ASSEMBLER.COMPLEXITY_CAP.translate() + ": " + hull.getComplexityMax());
				this.addText(lines, Localization.GUI.ASSEMBLER.TOTAL_COST.translate() + ": " + this.assembler.getTotalCost());
				this.addText(lines, Localization.GUI.ASSEMBLER.TOTAl_TIME.translate() + ": " + this.formatTime((int) (this.assembler.generateAssemblingTime() / this.assembler.getEfficiency())));
				this.addNewLine(lines);
				final ArrayList<String> errors = this.assembler.getErrors();
				this.hasErrors = (errors.size() > 0);
				if (errors.size() == 0) {
					this.addText(lines, Localization.GUI.ASSEMBLER.NO_ERROR.translate(), 22566);
				} else {
					for (final String error : errors) {
						this.addText(lines, error, 10357518);
					}
				}
			}
		}
		this.statusLog = lines;
	}

	private void addText(final ArrayList<TextWithColor> lines, final String text) {
		addText(lines, text, 4210752);
	}

	private void addText(final ArrayList<TextWithColor> lines, final String text, final int color) {
		final List newlines = getFontRenderer().listFormattedStringToWidth(text, 130);
		for (final Object line : newlines) {
			lines.add(new TextWithColor(line.toString(), color));
		}
	}

	private void addNewLine(final ArrayList<TextWithColor> lines) {
		lines.add(null);
	}

	@Override
	public void drawGuiBackground(final float f, final int x, final int y) {
		if (firstLoad) {
			updateErrorList();
			firstLoad = false;
		}
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		final int j = getGuiLeft();
		final int k = getGuiTop();
		ResourceHelper.bindResource(GuiCartAssembler.backgrounds[assembler.getSimulationInfo().getBackground()]);
		drawTexturedModalRect(j + 143, k + 15, 0, 0, 220, 148);
		ResourceHelper.bindResource(GuiCartAssembler.textureLeft);
		drawTexturedModalRect(j, k, 0, 0, 256, ySize);
		ResourceHelper.bindResource(GuiCartAssembler.textureRight);
		drawTexturedModalRect(j + 256, k, 0, 0, xSize - 256, ySize);
		drawTexturedModalRect(j + 256, k, 0, 0, xSize - 256, ySize);
		ResourceHelper.bindResource(GuiCartAssembler.textureExtra);
		final ArrayList<SlotAssembler> slots = assembler.getSlots();
		for (final SlotAssembler slot : slots) {
			int targetX = slot.getX() - 1;
			int targetY = slot.getY() - 1;
			int size;
			int srcX;
			int srcY;
			if (slot.useLargeInterface()) {
				targetX -= 3;
				targetY -= 3;
				size = 24;
				srcX = 0;
				srcY = 0;
			} else {
				size = 18;
				if (!slot.getStack().isEmpty() && TileEntityCartAssembler.getSlotStatus(slot.getStack()) <= 0) {
					if (TileEntityCartAssembler.getSlotStatus(slot.getStack()) == TileEntityCartAssembler.getRemovedSize()) {
						srcX = 140;
					} else {
						srcX = 122;
					}
					srcY = 40;
				} else {
					srcX = 24;
					srcY = 0;
				}
			}
			drawTexturedModalRect(j + targetX, k + targetY, srcX, srcY, size, size);
			int animationTick = slot.getAnimationTick();
			if (animationTick < 0) {
				animationTick = 0;
			}
			if (animationTick < 8 && !slot.useLargeInterface()) {
				drawTexturedModalRect(j + targetX + 1, k + targetY + 1, 0, 24 + animationTick, 16, 8 - animationTick);
				drawTexturedModalRect(j + targetX + 1, k + targetY + 1 + 8 + animationTick, 0, 32, 16, 8 - animationTick);
			}
		}
		for (final TitleBox box : assembler.getTitleBoxes()) {
			final int targetY2 = box.getY() - 12;
			final int targetX2 = box.getX();
			drawTexturedModalRect(j + targetX2, k + targetY2, 0, 40, 115, 11);
			GlStateManager.color((box.getColor() >> 16) / 255.0f, (box.getColor() >> 8 & 0xFF) / 255.0f, (box.getColor() & 0xFF) / 255.0f, 1.0f);
			drawTexturedModalRect(j + targetX2 + 8, k + targetY2 + 2, 0, 51 + box.getID() * 7, 115, 7);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		}
		int srcX2 = 42;
		int srcY2 = 0;
		if (assembler.getIsDisassembling()) {
			srcX2 = 158;
			srcY2 = 40;
		}
		if (hasErrors) {
			srcY2 += 22;
		} else if (inRect(x - j, y - k, assembleRect)) {
			srcY2 += 11;
		}
		drawTexturedModalRect(j + assembleRect[0], k + assembleRect[1], srcX2, srcY2, assembleRect[2], assembleRect[3]);
		float assemblingProgress = 0.0f;
		if (assembler.getIsAssembling()) {
			assemblingProgress = (float) assembler.getAssemblingTime() / (float) assembler.getMaxAssemblingTime();
		}
		drawProgressBar(assemblingProgRect, assemblingProgress, 22, x, y);
		drawProgressBar(fuelProgRect, (float) assembler.getFuelLevel() / (float) assembler.getMaxFuelLevel(), 31, x, y);
		renderDropDownMenu(x, y);
		render3DCart();
	}

	private String formatProgress(final float progress) {
		final float percentage = (int) (progress * 10000.0f) / 100.0f;
		return String.format("%05.2f%%", percentage);
	}

	private String formatTime(int ticks) {
		int seconds = ticks / 20;
		ticks -= seconds * 20;
		int minutes = seconds / 60;
		seconds -= minutes * 60;
		final int hours = minutes / 60;
		minutes -= hours * 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	private void drawProgressBarInfo(final int[] rect, final int x, final int y, final String str) {
		if (inRect(x - getGuiLeft(), y - getGuiTop(), rect)) {
			drawMouseOver(str, x - getGuiLeft(), y - getGuiTop());
		}
	}

	private void drawProgressBar(final int[] rect, float progress, final int barSrcY, final int x, final int y) {
		final int j = getGuiLeft();
		final int k = getGuiTop();
		int boxSrcY = 0;
		if (inRect(x - j, y - k, rect)) {
			boxSrcY = 11;
		}
		drawTexturedModalRect(j + rect[0], k + rect[1], 122, boxSrcY, rect[2], rect[3]);
		if (progress != 0.0f) {
			if (progress > 1.0f) {
				progress = 1.0f;
			}
			drawTexturedModalRect(j + rect[0] + 1, k + rect[1] + 1, 122, barSrcY, (int) (rect[2] * progress), rect[3] - 2);
		}
	}

	private void render3DCart() {
		assembler.createPlaceholder();
		int left = guiLeft;
		int top = guiTop;
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		GlStateManager.pushMatrix();
		float n = left + 256;
		int n2 = top;
		GlStateManager.translate(n, n2 + (Constants.renderSteve ? 50 : 100), 100.0f);
		float scale = 50.0f;
		GlStateManager.scale(-scale, scale, scale);
		GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.rotate(135.0f, 0.0f, 1.0f, 0.0f);
		RenderHelper.enableStandardItemLighting();
		GlStateManager.rotate(-135.0f, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(assembler.getRoll(), 1.0f, 0.0f, 0.0f);
		GlStateManager.rotate(assembler.getYaw(), 0.0f, 1.0f, 0.0f);
		Minecraft.getMinecraft().getRenderManager().playerViewY = 180.0f;
		if (Constants.renderSteve) {
			final EntityPlayer player = Minecraft.getMinecraft().player;
			@Nonnull
			ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
			player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, assembler.getCartFromModules(true));
			float temp = player.rotationPitch;
			player.rotationPitch = 0.7853982f;
			Minecraft.getMinecraft().getRenderManager().renderEntity(player, 0.0, 0.0, 0.0, 0.0f, 1.0f, false);
			player.rotationPitch = temp;
			player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
		} else {
			Minecraft.getMinecraft().getRenderManager().renderEntity(assembler.getPlaceholder(), 0.0, 0.0, 0.0, 0.0f, 1.0f, false);
		}
		GlStateManager.popMatrix();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();
		assembler.getPlaceholder().keepAlive = 0;
	}

	private void renderDropDownMenu(final int x, final int y) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0f, 0.0f, 200.0f);
		final int j = getGuiLeft();
		final int k = getGuiTop();
		if (dropdownX != -1 && dropdownY != -1) {
			final ArrayList<DropDownMenuItem> items = assembler.getDropDown();
			for (int i = 0; i < items.size(); ++i) {
				final DropDownMenuItem item = items.get(i);
				final int[] rect = item.getRect(dropdownX, dropdownY, i);
				int[] subrect = new int[0];
				int srcX = 0;
				int srcY = item.getIsLarge() ? 113 : 93;
				drawTexturedModalRect(j + rect[0], k + rect[1], srcX, srcY, rect[2], rect[3]);
				if (item.getIsLarge()) {
					drawString(item.getName(), j + rect[0] + 55, k + rect[1] + 7);
				}
				drawTexturedModalRect(j + rect[0] + 34, k + rect[1] + 2, item.getImageID() % 16 * 16, 179 + item.getImageID() / 16 * 16, 16, 16);
				if (item.hasSubmenu()) {
					subrect = item.getSubRect(dropdownX, dropdownY, i);
					srcX = (item.getIsSubMenuOpen() ? 0 : 43);
					srcY = 133;
					drawTexturedModalRect(j + subrect[0], k + subrect[1], srcX, srcY, subrect[2], subrect[3]);
				}
				switch (item.getType()) {
					case BOOL: {
						drawBooleanBox(x, y, 5 + rect[0], 5 + rect[1], item.getBOOL());
						break;
					}
					case INT: {
						if (item.getIsSubMenuOpen()) {
							drawIncreamentBox(x, y, getOffSetXForSubMenuBox(0, 2) + subrect[0], 3 + subrect[1]);
							drawDecreamentBox(x, y, getOffSetXForSubMenuBox(1, 2) + subrect[0], 3 + subrect[1]);
						}
						final int targetX = rect[0] + 16;
						final int targetY = rect[1] + 7;
						final int valueToWrite = item.getINT();
						if (valueToWrite >= 10) {
							drawDigit(valueToWrite / 10, -1, targetX, targetY);
							drawDigit(valueToWrite % 10, 1, targetX, targetY);
							break;
						}
						drawDigit(valueToWrite, 0, targetX, targetY);
						break;
					}
					case MULTIBOOL: {
						if (item.getIsSubMenuOpen()) {
							for (int count = item.getMULTIBOOLCount(), bool = 0; bool < count; ++bool) {
								drawBooleanBox(x, y, subrect[0] + getOffSetXForSubMenuBox(bool, count), subrect[1] + 3, item.getMULTIBOOL(bool));
							}
							break;
						}
						break;
					}
				}
			}
		}
		GlStateManager.popMatrix();
	}

	private void drawString(String str, final int x, final int y) {
		str = str.toUpperCase();
		for (int i = 0; i < str.length(); ++i) {
			final char c = str.charAt(i);
			final int index = validChars.indexOf(c);
			if (index != -1) {
				drawTexturedModalRect(x + 7 * i, y, 8 * index, 165, 6, 7);
			}
		}
	}

	private int getOffSetXForSubMenuBox(final int id, final int count) {
		return 2 + (int) (20.0f + (id - count / 2.0f) * 10.0f);
	}

	private void drawDigit(final int digit, final int offset, int targetX, final int targetY) {
		final int srcX = digit * 8;
		final int srcY = 172;
		targetX += offset * 4;
		drawTexturedModalRect(getGuiLeft() + targetX, getGuiTop() + targetY, srcX, srcY, 6, 7);
	}

	private void drawIncreamentBox(final int mouseX, final int mouseY, final int x, final int y) {
		drawStandardBox(mouseX, mouseY, x, y, 10);
	}

	private void drawDecreamentBox(final int mouseX, final int mouseY, final int x, final int y) {
		drawStandardBox(mouseX, mouseY, x, y, 20);
	}

	private void drawBooleanBox(final int mouseX, final int mouseY, final int x, final int y, final boolean itemvalue) {
		drawStandardBox(mouseX, mouseY, x, y, 0);
		if (itemvalue) {
			drawTexturedModalRect(getGuiLeft() + x + 2, getGuiTop() + y + 2, 0, 159, 6, 6);
		}
	}

	private void drawStandardBox(final int mouseX, final int mouseY, final int x, final int y, final int srcX) {
		final int targetX = getGuiLeft() + x;
		final int targetY = getGuiTop() + y;
		final int srcY = 149;
		drawTexturedModalRect(targetX, targetY, srcX, srcY, 10, 10);
		if (inRect(mouseX, mouseY, new int[] { targetX, targetY, 10, 10 })) {
			drawTexturedModalRect(targetX, targetY, 30, srcY, 10, 10);
		}
	}

	private boolean clickBox(final int mouseX, final int mouseY, final int x, final int y) {
		return inRect(mouseX, mouseY, new int[] { x, y, 10, 10 });
	}

	@Override
	public void mouseMoved(final int x0, final int y0, final int button) {
		super.mouseMoved(x0, y0, button);
		final int x = x0 - getGuiLeft();
		final int y = y0 - getGuiTop();
		if (dropdownX != -1 && dropdownY != -1) {
			final ArrayList<DropDownMenuItem> items = assembler.getDropDown();
			for (int i = 0; i < items.size(); ++i) {
				final DropDownMenuItem item = items.get(i);
				boolean insideSubRect = false;
				if (item.hasSubmenu()) {
					insideSubRect = inRect(x, y, item.getSubRect(dropdownX, dropdownY, i));
					if (!insideSubRect && item.getIsSubMenuOpen()) {
						item.setIsSubMenuOpen(false);
					} else if (insideSubRect && !item.getIsSubMenuOpen()) {
						item.setIsSubMenuOpen(true);
					}
				}
				final boolean insideRect = insideSubRect || inRect(x, y, item.getRect(dropdownX, dropdownY, i));
				if (!insideRect && item.getIsLarge()) {
					item.setIsLarge(false);
				} else if (insideRect && !item.getIsLarge()) {
					item.setIsLarge(true);
				}
			}
		}
		if (isScrolling) {
			if (button != -1) {
				isScrolling = false;
				assembler.setSpinning(true);
			} else {
				assembler.setYaw(assembler.getYaw() + x - scrollingX);
				assembler.setRoll(assembler.getRoll() + y - scrollingY);
				scrollingX = x;
				scrollingY = y;
			}
		}
	}

	@Override
	public void mouseClick(final int x0, final int y0, final int button) {
		super.mouseClick(x0, y0, button);
		final int x = x0 - getGuiLeft();
		final int y = y0 - getGuiTop();
		if (inRect(x, y, assembleRect)) {
			MessageStevesCarts.sendPacket(0, o -> o.writeByte(0));
		} else if (inRect(x, y, blackBackground)) {
			if (button == 0) {
				if (!isScrolling) {
					scrollingX = x;
					scrollingY = y;
					isScrolling = true;
					assembler.setSpinning(false);
				}
			} else if (button == 1) {
				dropdownX = x;
				dropdownY = y;
				if (dropdownY + assembler.getDropDown().size() * 20 > 164) {
					dropdownY = 164 - assembler.getDropDown().size() * 20;
				}
			}
		} else {
			final ArrayList<SlotAssembler> slots = assembler.getSlots();
			for (int i = 1; i < slots.size(); ++i) {
				final SlotAssembler slot = slots.get(i);
				final int targetX = slot.getX() - 1;
				final int targetY = slot.getY() - 1;
				final int size = 18;
				if (inRect(x, y, new int[] { targetX, targetY, size, size }) && !slot.getStack().isEmpty() && TileEntityCartAssembler.getSlotStatus(slot.getStack()) <= 0) {
					final int idx = i;
					MessageStevesCarts.sendPacket(1, o -> o.writeByte((byte) idx));
				}
			}
		}
		if (button == 0 && dropdownX != -1 && dropdownY != -1) {
			boolean anyLargeItem = false;
			final ArrayList<DropDownMenuItem> items = assembler.getDropDown();
			for (int j = 0; j < items.size(); ++j) {
				final DropDownMenuItem item = items.get(j);
				if (item.getIsLarge()) {
					anyLargeItem = true;
					final int[] rect = item.getRect(dropdownX, dropdownY, j);
					int[] subrect = new int[0];
					if (item.hasSubmenu() && item.getIsSubMenuOpen()) {
						subrect = item.getSubRect(dropdownX, dropdownY, j);
					}
					switch (item.getType()) {
						case BOOL: {
							if (clickBox(x, y, 5 + rect[0], 5 + rect[1])) {
								item.setBOOL(!item.getBOOL());
								break;
							}
							break;
						}
						case INT: {
							if (!item.getIsSubMenuOpen()) {
								break;
							}
							if (clickBox(x, y, getOffSetXForSubMenuBox(0, 2) + subrect[0], 3 + subrect[1])) {
								item.setINT(item.getINT() + 1);
							}
							if (clickBox(x, y, getOffSetXForSubMenuBox(1, 2) + subrect[0], 3 + subrect[1])) {
								item.setINT(item.getINT() - 1);
								break;
							}
							break;
						}
						case MULTIBOOL: {
							if (item.getIsSubMenuOpen()) {
								for (int count = item.getMULTIBOOLCount(), bool = 0; bool < count; ++bool) {
									if (clickBox(x, y, subrect[0] + getOffSetXForSubMenuBox(bool, count), subrect[1] + 3)) {
										item.setMULTIBOOL(bool, !item.getMULTIBOOL(bool));
										break;
									}
								}
								break;
							}
							break;
						}
					}
				}
			}
			if (!anyLargeItem) {
				final int n = -1;
				dropdownY = n;
				dropdownX = n;
			}
		}
	}

	static {
		GuiCartAssembler.backgrounds = new ResourceLocation[4];
		for (int i = 0; i < GuiCartAssembler.backgrounds.length; ++i) {
			GuiCartAssembler.backgrounds[i] = ResourceHelper.getResource("/gui/garageBackground" + i + ".png");
		}
		textureLeft = ResourceHelper.getResource("/gui/garagePart1.png");
		textureRight = ResourceHelper.getResource("/gui/garagePart2.png");
		textureExtra = ResourceHelper.getResource("/gui/garageExtra.png");
	}

	private class TextWithColor {
		private String text;
		private int color;

		public TextWithColor(final String text, final int color) {
			this.text = text;
			this.color = color;
		}

		public String getText() {
			return text;
		}

		public int getColor() {
			return color;
		}
	}
}
