package vswe.stevescarts.modules.addons;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.HeightControlOre;
import vswe.stevescarts.helpers.ResourceHelper;

import java.io.DataInput;
import java.io.IOException;

public class ModuleHeightControl extends ModuleAddon {
	private int levelNumberBoxX;
	private int levelNumberBoxY;
	private int[] arrowUp;
	private int[] arrowMiddle;
	private int[] arrowDown;
	private int oreMapX;
	private int oreMapY;
	private DataParameter<Integer> Y_TARGET;

	public ModuleHeightControl(EntityMinecartModular cart) {
		super(cart);
		levelNumberBoxX = 8;
		levelNumberBoxY = 18;
		arrowUp = new int[] { 9, 36, 17, 9 };
		arrowMiddle = new int[] { 9, 46, 17, 6 };
		arrowDown = new int[] { 9, 53, 17, 9 };
		oreMapX = 40;
		oreMapY = 18;
	}

	@Override
	public boolean hasSlots() {
		return false;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public int guiWidth() {
		return Math.max(100, oreMapX + 5 + HeightControlOre.ores.size() * 4);
	}

	@Override
	public int guiHeight() {
		return 65;
	}

	@Override
	public void drawForeground(GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
		final String s = String.valueOf(getYTarget());
		int x = levelNumberBoxX + 6;
		int color = 16777215;
		if (getYTarget() >= 100) {
			x -= 4;
		} else if (getYTarget() < 10) {
			x += 3;
			if (getYTarget() < 5) {
				color = 16711680;
			}
		}
		drawString(gui, s, x, levelNumberBoxY + 5, color);
	}

	@Override
	public void drawBackground(GuiMinecart gui, int x, int y) {
		ResourceHelper.bindResource("/gui/heightcontrol.png");
		drawImage(gui, levelNumberBoxX, levelNumberBoxY, 4, 36, 21, 15);
		drawImage(gui, arrowUp, 4, 12);
		drawImage(gui, arrowMiddle, 4, 21);
		drawImage(gui, arrowDown, 4, 27);
		for (int i = 0; i < HeightControlOre.ores.size(); ++i) {
			final HeightControlOre ore = HeightControlOre.ores.get(i);
			for (int j = 0; j < 11; ++j) {
				int altitude = getYTarget() - j + 5;
				boolean empty = ore.spanLowest > altitude || altitude > ore.spanHighest;
				boolean high = ore.bestLowest <= altitude && altitude <= ore.bestHighest;
				int srcY;
				int srcX;
				if (empty) {
					srcY = 0;
					srcX = 0;
				} else {
					srcX = ore.srcX;
					srcY = ore.srcY;
					if (high) {
						srcY += 4;
					}
				}
				drawImage(gui, oreMapX + i * 4, oreMapY + j * 4, srcX, srcY, 4, 4);
			}
		}
		if (getYTarget() != (int) getCart().posY) {
			drawMarker(gui, 5, false);
		}
		int pos = getYTarget() + 5 - (int) getCart().posY;
		if (pos >= 0 && pos < 11) {
			drawMarker(gui, pos, true);
		}
	}

	private void drawMarker(GuiMinecart gui, int pos, boolean isTargetLevel) {
		int srcX = 4;
		int srcY = isTargetLevel ? 6 : 0;
		drawImage(gui, oreMapX - 1, oreMapY + pos * 4 - 1, srcX, srcY, 1, 6);
		for (int i = 0; i < HeightControlOre.ores.size(); ++i) {
			drawImage(gui, oreMapX + i * 4, oreMapY + pos * 4 - 1, srcX + 1, srcY, 4, 6);
		}
		drawImage(gui, oreMapX + HeightControlOre.ores.size() * 4, oreMapY + pos * 4 - 1, srcX + 5, srcY, 1, 6);
	}

	@Override
	public void mouseClicked(GuiMinecart gui, int x, int y, int button) {
		if (button == 0) {
			byte packetData = 0;
			if (inRect(x, y, arrowMiddle)) {
				packetData |= 0x1;
			} else {
				if (!inRect(x, y, arrowUp)) {
					if (!inRect(x, y, arrowDown)) {
						return;
					}
					packetData |= 0x2;
				}
				if (GuiScreen.isShiftKeyDown()) {
					packetData |= 0x4;
				}
			}
			sendPacket(0, packetData);
		}
	}

	@Override
	protected void receivePacket(int id, DataInput reader, EntityPlayer player) throws IOException {
		if (id == 0) {
			byte info = reader.readByte();
			if ((info & 0x1) != 0x0) {
				setYTarget((int) getCart().posY);
			} else {
				int mult;
				if ((info & 0x2) == 0x0) {
					mult = 1;
				} else {
					mult = -1;
				}
				int dif;
				if ((info & 0x4) == 0x0) {
					dif = 1;
				} else {
					dif = 10;
				}
				int targetY = getYTarget();
				targetY += mult * dif;
				if (targetY < 0) {
					targetY = 0;
				} else if (targetY > 255) {
					targetY = 255;
				}
				setYTarget(targetY);
			}
		}
	}

	@Override
	public int numberOfPackets() {
		return 1;
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	public void initDw() {
		Y_TARGET = createDw(DataSerializers.VARINT);
		registerDw(Y_TARGET, (int) getCart().posY);
	}

	public void setYTarget(int val) {
		updateDw(Y_TARGET, val);
	}

	@Override
	public int getYTarget() {
		if (isPlaceholder()) {
			return 64;
		}
		int data = getDw(Y_TARGET);
		if (data < 0) {
			data += 256;
		}
		return data;
	}

	@Override
	protected void Save(NBTTagCompound tagCompound, int id) {
		tagCompound.setShort(generateNBTName("Height", id), (short) getYTarget());
	}

	@Override
	protected void Load(NBTTagCompound tagCompound, int id) {
		setYTarget(tagCompound.getShort(generateNBTName("Height", id)));
	}
}
