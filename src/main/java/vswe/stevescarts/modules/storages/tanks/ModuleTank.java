package vswe.stevescarts.modules.storages.tanks;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotLiquidInput;
import vswe.stevescarts.containers.slots.SlotLiquidOutput;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiBase;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.helpers.storages.ITankHolder;
import vswe.stevescarts.helpers.storages.SCTank;
import vswe.stevescarts.modules.storages.ModuleStorage;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.IOException;

public abstract class ModuleTank extends ModuleStorage implements IFluidTank, ITankHolder {
	protected SCTank tank;
	private int tick;
	protected int[] tankBounds;
	private DataParameter<String> FLUID_NAME;
	private DataParameter<Integer> FLUID_AMOUNT;

	public ModuleTank(final EntityMinecartModular cart) {
		super(cart);
		tankBounds = new int[] { 35, 20, 36, 51 };
		tank = new SCTank(this, getTankSize(), 0);
	}

	protected abstract int getTankSize();

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		if (y == 0) {
			return new SlotLiquidInput(getCart(), tank, -1, slotId, 8 + x * 18, 24 + y * 24);
		}
		return new SlotLiquidOutput(getCart(), slotId, 8 + x * 18, 24 + y * 24);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public int getInventoryWidth() {
		return 1;
	}

	@Override
	public int getInventoryHeight() {
		return 2;
	}

	@Override
	public int guiWidth() {
		return 100;
	}

	@Override
	public int guiHeight() {
		return 80;
	}

	public boolean hasVisualTank() {
		return true;
	}

	@Override
	public void update() {
		super.update();
		if (tick-- <= 0) {
			tick = 5;
			if (!getCart().world.isRemote) {
				tank.containerTransfer();
			} else if (!isPlaceholder()) {
				if (getDw(FLUID_NAME).isEmpty()) {
					tank.setFluid(null);
				} else {
					tank.setFluid(new FluidStack(FluidRegistry.getFluid(getDw(FLUID_NAME)), getDw(FLUID_AMOUNT)));
				}
			}
		}
	}

	@Override
	@Nonnull
	public ItemStack getInputContainer(final int tankid) {
		return getStack(0);
	}

	@Override
	public void setInputContainer(final int tankid, ItemStack stack) {
		setStack(0, stack);
	}

	@Override
	public void addToOutputContainer(final int tankid, @Nonnull ItemStack item) {
		addStack(1, item);
	}

	@Override
	public void onFluidUpdated(final int tankid) {
		if (getCart().world.isRemote) {
			return;
		}
		updateDw();
	}

	//	@SideOnly(Side.CLIENT)
	//	public void drawFluid(final int tankid, final GuiBase gui, final IIcon icon, final int targetX, final int targetY, final int srcX, final int srcY, final int sizeX, final int sizeY) {
	//		this.drawFluid((GuiMinecart) gui, icon, targetX, targetY, srcX, srcY, sizeX, sizeY);
	//	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		tank.drawFluid(gui, tankBounds[0], tankBounds[1]);
		ResourceHelper.bindResource("/gui/tank.png");
		drawImage(gui, tankBounds, 0, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, getTankInfo(), x, y, tankBounds);
	}

	protected String getTankInfo() {
		String str = tank.getMouseOver();
		if (tank.isLocked()) {
			str = str + "\n\n" + TextFormatting.GREEN + Localization.MODULES.TANKS.LOCKED.translate() + "\n" + Localization.MODULES.TANKS.UNLOCK.translate();
		} else if (tank.getFluid() != null) {
			str = str + "\n\n" + Localization.MODULES.TANKS.LOCK.translate();
		}
		return str;
	}

	@Override
	public FluidStack getFluid() {
		return (tank.getFluid() == null) ? null : tank.getFluid().copy();
	}

	@Override
	public int getCapacity() {
		return getTankSize();
	}

	@Override
	public int fill(final FluidStack resource, final boolean doFill) {
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(final int maxDrain, final boolean doDrain) {
		return tank.drain(maxDrain, doDrain);
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		if (tank.getFluid() != null) {
			final NBTTagCompound compound = new NBTTagCompound();
			tank.getFluid().writeToNBT(compound);
			tagCompound.setTag(generateNBTName("Fluid", id), compound);
		}
		tagCompound.setBoolean(generateNBTName("Locked", id), tank.isLocked());
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		tank.setFluid(FluidStack.loadFluidStackFromNBT(tagCompound.getCompoundTag(generateNBTName("Fluid", id))));
		tank.setLocked(tagCompound.getBoolean(generateNBTName("Locked", id)));
		updateDw();
	}

	@Override
	public int numberOfDataWatchers() {
		return 2;
	}

	protected void updateDw() {
		updateDw(FLUID_NAME, (tank.getFluid() == null) ? "" : tank.getFluid().getFluid().getName());
		updateDw(FLUID_AMOUNT, (tank.getFluid() == null) ? -1 : tank.getFluid().amount);
	}

	@Override
	public void initDw() {
		FLUID_NAME = createDw(DataSerializers.STRING);
		FLUID_AMOUNT = createDw(DataSerializers.VARINT);
		registerDw(FLUID_NAME, (tank.getFluid() == null) ? "" : tank.getFluid().getFluid().getName());
		registerDw(FLUID_AMOUNT, (tank.getFluid() == null) ? -1 : tank.getFluid().amount);
	}

	public float getFluidRenderHeight() {
		if (tank.getFluid() == null) {
			return 0.0f;
		}
		return tank.getFluid().amount / getTankSize();
	}

	public boolean isCompletelyFilled() {
		return getFluid() != null && getFluid().amount >= getTankSize();
	}

	public boolean isCompletelyEmpty() {
		return getFluid() == null || getFluid().amount == 0;
	}

	@Override
	public int getFluidAmount() {
		return (getFluid() == null) ? 0 : getFluid().amount;
	}

	@Override
	public FluidTankInfo getInfo() {
		return new FluidTankInfo(getFluid(), getCapacity());
	}

	@Override
	protected int numberOfPackets() {
		return 1;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0 && (getFluid() != null || tank.isLocked())) {
			tank.setLocked(!tank.isLocked());
			if (!tank.isLocked() && tank.getFluid() != null && tank.getFluid().amount <= 0) {
				tank.setFluid(null);
				updateDw();
			}
		}
	}

	@Override
	public int numberOfGuiData() {
		return 1;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		updateGuiData(info, 0, (short) (tank.isLocked() ? 1 : 0));
		updateDw();
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			tank.setLocked(data != 0);
		}
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (inRect(x, y, tankBounds)) {
			byte data = (byte) button;
			if (GuiScreen.isShiftKeyDown()) {
				data |= 0x2;
			}
			sendPacket(0, data);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawImage(int tankid, GuiBase gui, TextureAtlasSprite sprite, int targetX, int targetY, int srcX, int srcY, int width, int height) {
		drawImage((GuiMinecart)gui, sprite, targetX, targetY, srcX, srcY, width, height);
	}
}
