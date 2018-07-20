package vswe.stevescarts.modules.addons;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import vswe.stevescarts.entitys.CartDataSerializers;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;

import java.io.DataInput;
import java.io.IOException;

public class ModuleColorizer extends ModuleAddon {
	private int markerOffsetX;
	private int scrollWidth;
	private int markerMoving;
	private DataParameter<int[]> COLORS;

	public ModuleColorizer(final EntityMinecartModular cart) {
		super(cart);
		markerOffsetX = 10;
		scrollWidth = 64;
		markerMoving = -1;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public boolean hasSlots() {
		return false;
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public int guiWidth() {
		return 125;
	}

	@Override
	public int guiHeight() {
		return 75;
	}

	private int[] getMovableMarker(final int i) {
		return new int[] { markerOffsetX + (int) (scrollWidth * (getColorVal(i) / 255.0f)) - 2, 17 + i * 20, 4, 13 };
	}

	private int[] getArea(final int i) {
		return new int[] { markerOffsetX, 20 + i * 20, scrollWidth, 7 };
	}

	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/color.png");
		for (int i = 0; i < 3; ++i) {
			drawMarker(gui, x, y, i);
		}
		final float[] color = getColor();
		GlStateManager.color(color[0], color[1], color[2], 1.0f);
		drawImage(gui, scrollWidth + 25, 29, 4, 7, 28, 28);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		final String[] colorNames = { Localization.MODULES.ADDONS.COLOR_RED.translate(), Localization.MODULES.ADDONS.COLOR_GREEN.translate(),
			Localization.MODULES.ADDONS.COLOR_BLUE.translate() };
		for (int i = 0; i < 3; ++i) {
			drawStringOnMouseOver(gui, colorNames[i] + ": " + getColorVal(i), x, y, getArea(i));
		}
	}

	private void drawMarker(final GuiMinecart gui, final int x, final int y, final int id) {
		final float[] colorArea = new float[3];
		final float[] colorMarker = new float[3];
		for (int i = 0; i < 3; ++i) {
			if (i == id) {
				colorArea[i] = 0.7f;
				colorMarker[i] = 1.0f;
			} else {
				colorArea[i] = 0.2f;
				colorMarker[i] = 0.0f;
			}
		}
		GlStateManager.color(colorArea[0], colorArea[1], colorArea[2], 1.0f);
		drawImage(gui, getArea(id), 0, 0);
		GlStateManager.color(colorMarker[0], colorMarker[1], colorMarker[2], 1.0f);
		drawImage(gui, getMovableMarker(id), 0, 7);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (button == 0) {
			for (int i = 0; i < 3; ++i) {
				if (inRect(x, y, getMovableMarker(i))) {
					markerMoving = i;
				}
			}
		}
	}

	@Override
	public void mouseMovedOrUp(final GuiMinecart gui, final int x, final int y, final int button) {
		if (markerMoving != -1) {
			int tempColor = (int) ((x - markerOffsetX) / (scrollWidth / 255.0f));
			if (tempColor < 0) {
				tempColor = 0;
			} else if (tempColor > 255) {
				tempColor = 255;
			}
			sendPacket(markerMoving, (byte) tempColor);
		}
		if (button != -1) {
			markerMoving = -1;
		}
	}

	@Override
	public int numberOfDataWatchers() {
		return 3;
	}

	@Override
	public void initDw() {
		COLORS = createDw(CartDataSerializers.VARINT);
		registerDw(COLORS, new int[] { 255, 255, 255 });
	}

	@Override
	public int numberOfPackets() {
		return 3;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id >= 0 && id < 3) {
			setColorVal(id, reader.readByte());
		}
	}

	public int getColorVal(final int i) {
		if (isPlaceholder()) {
			return 255;
		}
		int tempVal = getDw(COLORS)[i];
		if (tempVal < 0) {
			tempVal += 256;
		}
		return tempVal;
	}

	public void setColorVal(final int id, final int val) {
		int[] colors = getDw(COLORS);
		colors[id] = val;
		updateDw(COLORS, colors);
	}

	private float getColorComponent(final int i) {
		return getColorVal(i) / 255.0f;
	}

	@Override
	public float[] getColor() {
		return new float[] { getColorComponent(0), getColorComponent(1), getColorComponent(2) };
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setByte(generateNBTName("Red", id), (byte) getColorVal(0));
		tagCompound.setByte(generateNBTName("Green", id), (byte) getColorVal(1));
		tagCompound.setByte(generateNBTName("Blue", id), (byte) getColorVal(2));
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		setColorVal(0, tagCompound.getByte(generateNBTName("Red", id)));
		setColorVal(1, tagCompound.getByte(generateNBTName("Green", id)));
		setColorVal(2, tagCompound.getByte(generateNBTName("Blue", id)));
	}
}
