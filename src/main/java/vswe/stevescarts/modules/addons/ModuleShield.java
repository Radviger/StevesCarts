package vswe.stevescarts.modules.addons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.IActivatorModule;

import java.io.DataInput;

public class ModuleShield extends ModuleAddon implements IActivatorModule {
	private boolean shield;
	private float shieldDistance;
	private float shieldAngle;
	private int[] buttonRect;
	private DataParameter<Boolean> STATUS;

	public ModuleShield(final EntityMinecartModular cart) {
		super(cart);
		shield = true;
		shieldDistance = 18.0f;
		buttonRect = new int[] { 20, 20, 24, 12 };
	}

	protected boolean shieldSetting() {
		return getShieldStatus();
	}

	public float getShieldDistance() {
		return shieldDistance;
	}

	public float getShieldAngle() {
		return shieldAngle;
	}

	public boolean hasShield() {
		return shield;
	}

	@Override
	public void update() {
		super.update();
		if (hasShield() && !getCart().hasFuelForModule() && !getCart().world.isRemote) {
			setShieldStatus(false);
		}
		if (hasShield()) {
			getCart().extinguish();
		}
		if (!getShieldStatus() && shieldDistance > 0.0f) {
			shieldDistance -= 0.25f;
			if (shieldDistance <= 0.0f) {
				shield = false;
			}
		} else if (getShieldStatus() && shieldDistance < 18.0f) {
			shieldDistance += 0.25f;
			shield = true;
		}
		if (shield) {
			shieldAngle = (float) ((shieldAngle + 0.125f) % 314.1592653589793);
		}
	}

	@Override
	public boolean receiveDamage(DamageSource source, float val) {
		return !hasShield();
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
		return 75;
	}

	@Override
	public int guiHeight() {
		return 35;
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	public void setShieldStatus(final boolean val) {
		if (!isPlaceholder()) {
			updateDw(STATUS, val);
		}
	}

	private boolean getShieldStatus() {
		if (isPlaceholder()) {
			return getSimInfo().getShieldActive();
		}
		return getDw(STATUS);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/shield.png");
		final int imageID = getShieldStatus() ? 1 : 0;
		int borderID = 0;
		if (inRect(x, y, buttonRect)) {
			borderID = 1;
		}
		drawImage(gui, buttonRect, 0, buttonRect[3] * borderID);
		final int srcY = buttonRect[3] * 2 + imageID * (buttonRect[3] - 2);
		drawImage(gui, buttonRect[0] + 1, buttonRect[1] + 1, 0, srcY, buttonRect[2] - 2, buttonRect[3] - 2);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, getStateName(), x, y, buttonRect);
	}

	private String getStateName() {
		return Localization.MODULES.ADDONS.SHIELD.translate(getShieldStatus() ? "1" : "0");
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (button == 0 && inRect(x, y, buttonRect)) {
			sendPacket(0);
		}
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) {
		if (id == 0) {
			updateDw(STATUS, !getShieldStatus());
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
		STATUS = createDw(DataSerializers.BOOLEAN);
		registerDw(STATUS, false);
	}

	@Override
	public int getConsumption(final boolean isMoving) {
		return hasShield() ? 20 : super.getConsumption(isMoving);
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setBoolean(generateNBTName("Shield", id), getShieldStatus());
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		setShieldStatus(tagCompound.getBoolean(generateNBTName("Shield", id)));
	}

	@Override
	public void doActivate(final int id) {
		setShieldStatus(true);
	}

	@Override
	public void doDeActivate(final int id) {
		setShieldStatus(false);
	}

	@Override
	public boolean isActive(final int id) {
		return getShieldStatus();
	}
}
