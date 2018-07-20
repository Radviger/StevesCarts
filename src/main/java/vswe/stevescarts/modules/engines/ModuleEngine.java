package vswe.stevescarts.modules.engines;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

import java.io.DataInput;
import java.io.IOException;

public abstract class ModuleEngine extends ModuleBase {
	private int fuel;
	protected int[] priorityButton;

	public ModuleEngine(final EntityMinecartModular cart) {
		super(cart);
		initPriorityButton();
	}

	protected void initPriorityButton() {
		priorityButton = new int[] { 78, 7, 16, 16 };
	}

	@Override
	public void update() {
		super.update();
		loadFuel();
	}

	@Override
	public boolean hasFuel(final int comsumption) {
		return getFuelLevel() >= comsumption && !isDisabled();
	}

	public int getFuelLevel() {
		return fuel;
	}

	public void setFuelLevel(final int val) {
		fuel = val;
	}

	protected boolean isDisabled() {
		return getPriority() >= 3 || getPriority() < 0;
	}

	protected abstract DataParameter<Integer> getPriorityDw();

	public int getPriority() {
		if (isPlaceholder()) {
			return 0;
		}
		int temp = getDw(getPriorityDw());
		if (temp < 0 || temp > 3) {
			temp = 3;
		}
		return temp;
	}

	private void setPriority(int data) {
		if (data < 0) {
			data = 0;
		} else if (data > 3) {
			data = 3;
		}
		updateDw(getPriorityDw(), data);
	}

	public void consumeFuel(final int comsumption) {
		setFuelLevel(getFuelLevel() - comsumption);
	}

	protected abstract void loadFuel();

	public void smoke() {
	}

	public abstract int getTotalFuel();

	public abstract float[] getGuiBarColor();

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public int guiWidth() {
		return 100;
	}

	@Override
	public int guiHeight() {
		return 50;
	}

	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/engine.png");
		final int sourceX = 16 * getPriority();
		int sourceY = 0;
		if (inRect(x, y, priorityButton)) {
			sourceY = 16;
		}
		drawImage(gui, priorityButton, sourceX, sourceY);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, getPriorityText(), x, y, priorityButton);
	}

	private String getPriorityText() {
		if (isDisabled()) {
			return Localization.MODULES.ENGINES.ENGINE_DISABLED.translate();
		}
		return Localization.MODULES.ENGINES.ENGINE_PRIORITY.translate(String.valueOf(getPriority()));
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (inRect(x, y, priorityButton) && (button == 0 || button == 1)) {
			sendPacket(0, (byte) button);
		}
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0) {
			int prio = getPriority();
			prio += ((reader.readByte() == 0) ? 1 : -1);
			prio %= 4;
			if (prio < 0) {
				prio += 4;
			}
			setPriority(prio);
		}
	}

	@Override
	public int numberOfPackets() {
		return 1;
	}

	@Override
	public void initDw() {
		registerDw(getPriorityDw(), 0);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setByte(generateNBTName("Priority", id), (byte) getPriority());
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		setPriority(tagCompound.getByte(generateNBTName("Priority", id)));
	}
}
