package vswe.stevescarts.upgrades;

import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.StevesCarts;
import vswe.stevescarts.blocks.tileentities.TileEntityUpgrade;
import vswe.stevescarts.containers.ContainerUpgrade;
import vswe.stevescarts.containers.slots.SlotLiquidOutput;
import vswe.stevescarts.containers.slots.SlotLiquidUpgradeInput;
import vswe.stevescarts.guis.GuiUpgrade;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.helpers.storages.SCTank;
import vswe.stevescarts.network.message.MessageFluidSync;

public abstract class TankEffect extends InventoryEffect {
	private static final int tankInterfaceX = 35;
	private static final int tankInterfaceY = 20;
	@SideOnly(Side.CLIENT)
	private static ResourceLocation texture;

	public abstract int getTankSize();

	@Override
	public Class<? extends Slot> getSlot(final int id) {
		return SlotLiquidOutput.class;
	}

	@Override
	public Slot createSlot(final TileEntityUpgrade upgrade, final int id) {
		if (id == 0) {
			return new SlotLiquidUpgradeInput(upgrade, upgrade.tank, 16, id, getSlotX(id), getSlotY(id));
		}
		return super.createSlot(upgrade, id);
	}

	@Override
	public int getInventorySize() {
		return 2;
	}

	@Override
	public int getSlotX(final int id) {
		return 8;
	}

	@Override
	public int getSlotY(final int id) {
		return 24 * (id + 1);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final TileEntityUpgrade upgrade, final GuiUpgrade gui, final int x, final int y) {
		if (TankEffect.texture == null) {
			TankEffect.texture = ResourceHelper.getResource("/gui/tank.png");
		}
		upgrade.tank.drawFluid(gui, tankInterfaceX, tankInterfaceY);
		ResourceHelper.bindResource(TankEffect.texture);
		gui.drawTexturedModalRect(gui.getGuiLeft() + tankInterfaceX, gui.getGuiTop() + tankInterfaceY, 0, 0, 36, 51);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawMouseOver(final TileEntityUpgrade upgrade, final GuiUpgrade gui, final int x, final int y) {
		drawMouseOver(gui, upgrade.tank.getMouseOver(), x, y, new int[] { tankInterfaceX, tankInterfaceX, 36, 51 });
	}

	@Override
	public void checkGuiData(final TileEntityUpgrade upgrade, final ContainerUpgrade con, final IContainerListener crafting, final boolean isNew) {
		boolean changed = false;
		final int id = 0;
		final int amount1 = 1;
		final int amount2 = 2;
		final FluidStack oldfluid = (FluidStack) con.olddata;
		if ((isNew || oldfluid != null) && upgrade.tank.getFluid() == null) {
			upgrade.updateGuiData(con, crafting, id, (short) (-1));
			changed = true;
		} else if (upgrade.tank.getFluid() != null) {
			if (isNew || oldfluid == null) {
				upgrade.updateGuiData(con, crafting, id, (short) upgrade.tank.getFluid().amount);
				upgrade.updateGuiData(con, crafting, amount1, upgrade.getShortFromInt(true, upgrade.tank.getFluid().amount));
				upgrade.updateGuiData(con, crafting, amount2, upgrade.getShortFromInt(false, upgrade.tank.getFluid().amount));
				changed = true;
			} else {
				StevesCarts.NET.sendToDimension(new MessageFluidSync(upgrade.tank.getFluid(), upgrade.getPos(), upgrade.getWorld().provider.getDimension(), 0), upgrade.getWorld().provider.getDimension());
				if (oldfluid.amount != upgrade.tank.getFluid().amount) {
					upgrade.updateGuiData(con, crafting, amount1, upgrade.getShortFromInt(true, upgrade.tank.getFluid().amount));
					upgrade.updateGuiData(con, crafting, amount2, upgrade.getShortFromInt(false, upgrade.tank.getFluid().amount));
					changed = true;
				}
			}
		}
		if (changed) {
			if (upgrade.tank.getFluid() == null) {
				con.olddata = null;
			} else {
				con.olddata = upgrade.tank.getFluid().copy();
			}
		}
	}

	@Override
	public void receiveGuiData(final TileEntityUpgrade upgrade, final int id, final short data) {
		if (id == 0) {
			if (data == -1) {
				upgrade.tank.setFluid(null);
			}
		} else if (upgrade.tank.getFluid() != null) {
			upgrade.tank.getFluid().amount = upgrade.getIntFromShort(id == 1, upgrade.tank.getFluid().amount, data);
		}
	}

	@Override
	public void init(final TileEntityUpgrade upgrade) {
		upgrade.tank = new SCTank(upgrade, getTankSize(), 0);
		upgrade.getCompound().setByte("Tick", (byte) 0);
	}

	@Override
	public void update(final TileEntityUpgrade upgrade) {
		super.update(upgrade);
		upgrade.getCompound().setByte("Tick", (byte) (upgrade.getCompound().getByte("Tick") - 1));
		if (upgrade.getCompound().getByte("Tick") <= 0) {
			upgrade.getCompound().setByte("Tick", (byte) 5);
			if (!upgrade.getWorld().isRemote && slots != null && slots.size() >= 2) {
				upgrade.tank.containerTransfer();
			}
		}
	}

	@Override
	public void load(final TileEntityUpgrade upgrade, final NBTTagCompound compound) {
		if (compound.getByte("Exists") != 0) {
			upgrade.tank.setFluid(FluidStack.loadFluidStackFromNBT(compound));
		} else {
			upgrade.tank.setFluid(null);
		}
	}

	@Override
	public void save(final TileEntityUpgrade upgrade, final NBTTagCompound compound) {
		if (upgrade.tank.getFluid() == null) {
			compound.setByte("Exists", (byte) 0);
		} else {
			compound.setByte("Exists", (byte) 1);
			upgrade.tank.getFluid().writeToNBT(compound);
		}
	}
}
