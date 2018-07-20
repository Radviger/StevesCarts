package vswe.stevescarts.modules.realtimers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ILeverModule;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.engines.ModuleEngine;
import vswe.stevescarts.network.message.MessageStevesCarts;

import java.io.DataInput;
import java.io.IOException;

public class ModuleAdvControl extends ModuleBase implements ILeverModule {
	private int upperBarLength = -1, lowerBarLength = -1;
	private int tripPacketTimer;
	private int enginePacketTimer;
	private byte keyinformation;
	private double lastPosX;
	private double lastPosY;
	private double lastPosZ;
	private boolean first;
	private int speedChangeCooldown;
	private boolean lastBackKey;
	private double odo;
	private double trip;
	private int[] buttonRect;
	private DataParameter<Integer> SPEED;

	public ModuleAdvControl(final EntityMinecartModular cart) {
		super(cart);
		first = true;
		buttonRect = new int[] { 15, 20, 24, 12 };
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
		return 90;
	}

	@Override
	public int guiHeight() {
		return 35;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void renderOverlay(final Minecraft minecraft) {
		ResourceHelper.bindResource("/gui/drive.png");
		if (upperBarLength != -1 && lowerBarLength != -1) {
			for (int i = 0; i < getCart().getEngines().size(); ++i) {
				drawImage(5, i * 15, 0, 0, 66, 15);
				final ModuleEngine engine = getCart().getEngines().get(i);
				final float[] rgb = engine.getGuiBarColor();
				GlStateManager.color(rgb[0], rgb[1], rgb[2], 1.0f);
				drawImage(7, i * 15 + 2, 66, 0, upperBarLength, 5);
				drawImage(7, i * 15 + 2 + 6, 66, 6, lowerBarLength, 5);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawImage(5, i * 15, 66 + engine.getPriority() * 7, 11, 7, 15);
			}
		}
		final int enginesEndAt = getCart().getEngines().size() * 15;
		drawImage(5, enginesEndAt, 0, 15, 32, 32);
		if (minecraft.gameSettings.keyBindForward.isKeyDown()) {
			drawImage(15, enginesEndAt + 5, 42, 20, 12, 6);
		} else if (minecraft.gameSettings.keyBindLeft.isKeyDown()) {
			drawImage(7, enginesEndAt + 13, 34, 28, 6, 12);
		} else if (minecraft.gameSettings.keyBindRight.isKeyDown()) {
			drawImage(29, enginesEndAt + 13, 56, 28, 6, 12);
		}
		final int speedGraphicHeight = getSpeedSetting() * 2;
		drawImage(14, enginesEndAt + 13 + 12 - speedGraphicHeight, 41, 40 - speedGraphicHeight, 14, speedGraphicHeight);
		drawImage(0, 0, 0, 67, 5, 130);
		drawImage(1, 1 + (256 - getCart().y()) / 2, 5, 67, 5, 1);
		drawImage(5, enginesEndAt + 32, 0, 47, 32, 20);
		drawImage(5, enginesEndAt + 52, 0, 47, 32, 20);
		drawImage(5, enginesEndAt + 72, 0, 47, 32, 20);
		minecraft.fontRenderer.drawString(Localization.MODULES.ATTACHMENTS.ODO.translate(), 7, enginesEndAt + 52 + 2, 4210752);
		minecraft.fontRenderer.drawString(distToString(odo), 7, enginesEndAt + 52 + 11, 4210752);
		minecraft.fontRenderer.drawString(Localization.MODULES.ATTACHMENTS.TRIP.translate(), 7, enginesEndAt + 52 + 22, 4210752);
		minecraft.fontRenderer.drawString(distToString(trip), 7, enginesEndAt + 52 + 31, 4210752);
		drawItem(new ItemStack(Items.CLOCK, 1), 5, enginesEndAt + 32 + 3);
		drawItem(new ItemStack(Items.COMPASS, 1), 21, enginesEndAt + 32 + 3);
	}

	@SideOnly(Side.CLIENT)
	public void drawItem(ItemStack icon, final int targetX, final int targetY) {
		RenderHelper.enableGUIStandardItemLighting();
		RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
		itemRenderer.renderItemAndEffectIntoGUI(icon, targetX, targetY);

	}

	private String distToString(double dist) {
		int i;
		for (i = 0; dist >= 1000.0; dist /= 1000.0, ++i) {}
		int val;
		if (dist >= 100.0) {
			val = 1;
		} else if (dist >= 10.0) {
			val = 10;
		} else {
			val = 100;
		}
		final double d = Math.round(dist * val) / val;
		StringBuilder s;
		if (d == (int) d) {
			s = new StringBuilder(String.valueOf((int) d));
		} else {
			s = new StringBuilder(String.valueOf(d));
		}
		while (s.length() < ((s.toString().indexOf(46) != -1) ? 4 : 3)) {
			if (s.toString().indexOf(46) != -1) {
				s.append("0");
			} else {
				s.append(".0");
			}
		}
		s.append(Localization.MODULES.ATTACHMENTS.DISTANCES.translate(String.valueOf(i)));
		return s.toString();
	}

	@Override
	public RAILDIRECTION getSpecialRailDirection(BlockPos pos) {
		if (this.isForwardKeyDown()) {
			return RAILDIRECTION.FORWARD;
		}
		if (this.isLeftKeyDown()) {
			return RAILDIRECTION.LEFT;
		}
		if (this.isRightKeyDown()) {
			return RAILDIRECTION.RIGHT;
		}
		return RAILDIRECTION.DEFAULT;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0) {
			upperBarLength = reader.readInt();
			lowerBarLength = reader.readInt();
		} else if (id == 1) {
			if (getCart().getCartRider() != null && getCart().getCartRider() instanceof EntityPlayer && getCart().getCartRider() == player) {
				keyinformation = reader.readByte();
				getCart().resetRailDirection();
			}
		} else if (id == 2) {
			odo = reader.readInt();
			trip = reader.readInt();
		} else if (id == 3) {
			trip = 0.0;
			tripPacketTimer = 0;
		}
	}

	@Override
	public void update() {
		super.update();
		if (!getCart().world.isRemote && getCart().getCartRider() != null && getCart().getCartRider() instanceof EntityPlayer) {
			if (enginePacketTimer == 0) {
				sendEnginePacket((EntityPlayer) getCart().getCartRider());
				enginePacketTimer = 15;
			} else {
				enginePacketTimer--;
			}
			if (tripPacketTimer == 0) {
				sendTripPacket((EntityPlayer) getCart().getCartRider());
				tripPacketTimer = 500;
			} else {
				tripPacketTimer--;
			}
		} else {
			enginePacketTimer = 0;
			tripPacketTimer = 0;
		}

		if (getCart().world.isRemote) {
			encodeKeys();
		}
		if (!lastBackKey && isBackKeyDown()) {
			turnback();
		}
		lastBackKey = isBackKeyDown();

		if (!getCart().world.isRemote) {
			if (speedChangeCooldown == 0) {
				if (!isJumpKeyDown() || !isControlKeyDown()) {
					if (isJumpKeyDown()) {
						setSpeedSetting(getSpeedSetting() + 1);
						speedChangeCooldown = 8;
					} else if (isControlKeyDown()) {
						setSpeedSetting(getSpeedSetting() - 1);
						speedChangeCooldown = 8;
					} else {
						speedChangeCooldown = 0;
					}
				}
			} else {
				--speedChangeCooldown;
			}
			if (isForwardKeyDown() && isLeftKeyDown() && isRightKeyDown() && getCart().getCartRider() != null && getCart().getCartRider() instanceof EntityPlayer) {
				getCart().getCartRider().startRiding(getCart());
				keyinformation = 0;
			}
		}
		final double x = getCart().posX - lastPosX;
		final double y = getCart().posY - lastPosY;
		final double z = getCart().posZ - lastPosZ;
		lastPosX = getCart().posX;
		lastPosY = getCart().posY;
		lastPosZ = getCart().posZ;
		final double dist = Math.sqrt(x * x + y * y + z * z);
		if (!first) {
			odo += dist;
			trip += dist;
		} else {
			first = false;
		}
	}

	@Override
	public double getPushFactor() {
		switch (getSpeedSetting()) {
			case 1: {
				return 0.01;
			}
			case 2: {
				return 0.03;
			}
			case 3: {
				return 0.05;
			}
			case 4: {
				return 0.07;
			}
			case 5: {
				return 0.09;
			}
			case 6: {
				return 0.11;
			}
			default: {
				return super.getPushFactor();
			}
		}
	}

	private void encodeKeys() {
		if (getCart().getCartRider() != null && getCart().getCartRider() instanceof EntityPlayer && getCart().getCartRider() == getClientPlayer()) {
			final Minecraft minecraft = Minecraft.getMinecraft();
            GameSettings settings = minecraft.gameSettings;
            final byte oldVal = keyinformation;
			keyinformation = 0;
			keyinformation |= (byte) ((settings.keyBindForward.isKeyDown() ? 1 : 0) << 0);
			keyinformation |= (byte) ((settings.keyBindLeft.isKeyDown() ? 1 : 0) << 1);
			keyinformation |= (byte) ((settings.keyBindRight.isKeyDown() ? 1 : 0) << 2);
			keyinformation |= (byte) ((settings.keyBindBack.isKeyDown() ? 1 : 0) << 3);
			keyinformation |= (byte) ((settings.keyBindJump.isKeyDown() ? 1 : 0) << 4);
			keyinformation |= (byte) ((settings.keyBindSprint.isKeyDown() ? 1 : 0) << 5);
			if (oldVal != keyinformation) {
				MessageStevesCarts.sendPacket(getCart(), 1 + getPacketStart(), o -> o.writeByte(keyinformation));
			}
		}
	}

	private boolean isForwardKeyDown() {
		return (keyinformation & 0x1) != 0x0;
	}

	private boolean isLeftKeyDown() {
		return (keyinformation & 0x2) != 0x0;
	}

	private boolean isRightKeyDown() {
		return (keyinformation & 0x4) != 0x0;
	}

	private boolean isBackKeyDown() {
		return (keyinformation & 0x8) != 0x0;
	}

	private boolean isJumpKeyDown() {
		return (keyinformation & 0x10) != 0x0;
	}

	private boolean isControlKeyDown() {
		return (keyinformation & 0x20) != 0x0;
	}

	private void sendTripPacket(final EntityPlayer player) {
		final int intOdo = (int) odo;
		final int intTrip = (int) trip;
		sendPacket(2, o -> {
			o.writeInt(intOdo);
			o.writeInt(intTrip);
		}, player);
	}

	private void sendEnginePacket(final EntityPlayer player) {
		sendPacket(0, o -> {
			for (ModuleEngine engine : getCart().getEngines()) {
				final int totalFuel = engine.getTotalFuel();
				final int fuelInTopBar = 20000;
				final int maxBarLength = 62;
				final float percentage = totalFuel % fuelInTopBar / fuelInTopBar;
				final int upperBarLength = (int) (maxBarLength * percentage);
				int lowerBarLength = totalFuel / fuelInTopBar;
				if (lowerBarLength > maxBarLength) {
					lowerBarLength = maxBarLength;
				}
				o.writeInt(upperBarLength);
				o.writeInt(lowerBarLength);
			}
		}, player);
	}

	@Override
	public int numberOfPackets() {
		return 4;
	}

	private void setSpeedSetting(final int val) {
		if (val < 0 || val > 6) {
			return;
		}
		updateDw(SPEED, val);
	}

	private int getSpeedSetting() {
		if (isPlaceholder()) {
			return 1;
		}
		return getDw(SPEED);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	public void initDw() {
		SPEED = createDw(DataSerializers.VARINT);
		registerDw(SPEED, 0);
	}

	@Override
	public boolean stopEngines() {
		return getSpeedSetting() == 0;
	}

	@Override
	public int getConsumption(final boolean isMoving) {
		if (!isMoving) {
			return super.getConsumption(isMoving);
		}
		switch (getSpeedSetting()) {
			case 4: {
				return 1;
			}
			case 5: {
				return 3;
			}
			case 6: {
				return 5;
			}
			default: {
				return super.getConsumption(isMoving);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/advlever.png");
		if (inRect(x, y, buttonRect)) {
			drawImage(gui, buttonRect, 0, buttonRect[3]);
		} else {
			drawImage(gui, buttonRect, 0, 0);
		}
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, Localization.MODULES.ATTACHMENTS.CONTROL_RESET.translate(), x, y, buttonRect);
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (button == 0 && inRect(x, y, buttonRect)) {
			sendPacket(3);
		}
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, Localization.MODULES.ATTACHMENTS.CONTROL_SYSTEM.translate(), 8, 6, 4210752);
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setByte(generateNBTName("Speed", id), (byte) getSpeedSetting());
		tagCompound.setDouble(generateNBTName("ODO", id), odo);
		tagCompound.setDouble(generateNBTName("TRIP", id), trip);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		setSpeedSetting(tagCompound.getByte(generateNBTName("Speed", id)));
		odo = tagCompound.getDouble(generateNBTName("ODO", id));
		trip = tagCompound.getDouble(generateNBTName("TRIP", id));
	}

	public float getWheelAngle() {
		if (!isForwardKeyDown()) {
			if (isLeftKeyDown()) {
				return 0.3926991f;
			}
			if (isRightKeyDown()) {
				return -0.3926991f;
			}
		}
		return 0.0f;
	}

	@Override
	public float getLeverState() {
		if (isPlaceholder()) {
			return 0.0f;
		}
		return getSpeedSetting() / 6.0f;
	}

	@Override
	public void postUpdate() {
		if (this.getCart().world.isRemote && this.getCart().getCartRider() != null && this.getCart().getCartRider() instanceof EntityPlayer && this.getCart().getCartRider() == this.getClientPlayer()) {
			KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode(), false);
			KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode(), false);
		}
	}
}
