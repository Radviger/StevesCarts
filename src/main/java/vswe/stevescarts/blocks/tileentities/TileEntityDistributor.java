package vswe.stevescarts.blocks.tileentities;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import vswe.stevescarts.api.util.CheckedConsumer;
import vswe.stevescarts.containers.ContainerBase;
import vswe.stevescarts.containers.ContainerDistributor;
import vswe.stevescarts.guis.GuiBase;
import vswe.stevescarts.guis.GuiDistributor;
import vswe.stevescarts.helpers.DistributorSetting;
import vswe.stevescarts.helpers.DistributorSide;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.storages.SCTank;
import vswe.stevescarts.network.message.MessageStevesCarts;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TileEntityDistributor extends TileEntityBase implements IInventory, ISidedInventory {
	private ArrayList<DistributorSide> sides;
	private boolean dirty;

	private TileEntityManager[] inventories;
	public boolean hasTop;
	public boolean hasBot;

	private Map<EnumFacing, IFluidHandler> fluidHandlerMap;

	public TileEntityDistributor() {
		dirty = true;
		(sides = new ArrayList<>()).add(new DistributorSide(0, Localization.GUI.DISTRIBUTOR.SIDE_ORANGE, EnumFacing.UP));
		sides.add(new DistributorSide(1, Localization.GUI.DISTRIBUTOR.SIDE_PURPLE, EnumFacing.DOWN));
		sides.add(new DistributorSide(2, Localization.GUI.DISTRIBUTOR.SIDE_YELLOW, EnumFacing.NORTH));
		sides.add(new DistributorSide(3, Localization.GUI.DISTRIBUTOR.SIDE_GREEN, EnumFacing.WEST));
		sides.add(new DistributorSide(4, Localization.GUI.DISTRIBUTOR.SIDE_BLUE, EnumFacing.SOUTH));
		sides.add(new DistributorSide(5, Localization.GUI.DISTRIBUTOR.SIDE_RED, EnumFacing.EAST));
		fluidHandlerMap = new HashMap<>();
		for (EnumFacing facing: EnumFacing.values()) {
			fluidHandlerMap.put(facing, new IFluidHandler() {
				@Override
				public IFluidTankProperties[] getTankProperties() {
					final IFluidTank[] tanks = getTanks(facing);
					final IFluidTankProperties[] infos = new IFluidTankProperties[tanks.length];
					for (int i = 0; i < infos.length; ++i) {
						infos[i] = new FluidTankProperties(tanks[i].getFluid(), tanks[i].getCapacity());
					}
					return infos;
				}

				@Override
				public int fill(FluidStack resource, boolean doFill) {
					final IFluidTank[] tanks = getTanks(facing);
					int amount = 0;
					for (final IFluidTank tank : tanks) {
						amount += tank.fill(resource, doFill);
					}
					return amount;
				}

				@Nullable
				@Override
				public FluidStack drain(FluidStack resource, boolean doDrain) {
					return TileEntityDistributor.this.drain(facing, resource, (resource == null) ? 0 : resource.amount, doDrain);
				}

				@Nullable
				@Override
				public FluidStack drain(int maxDrain, boolean doDrain) {
					return TileEntityDistributor.this.drain(facing, null, maxDrain, doDrain);
				}
			});
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public GuiBase getGui(final InventoryPlayer inv) {
		return new GuiDistributor(inv, this);
	}

	@Override
	public ContainerBase getContainer(final InventoryPlayer inv) {
		return new ContainerDistributor(inv, this);
	}

	public ArrayList<DistributorSide> getSides() {
		return sides;
	}

	@Override
	public void readFromNBT(final NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		for (final DistributorSide side : getSides()) {
			side.setData(nbttagcompound.getInteger("Side" + side.getId()));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		for (final DistributorSide side : getSides()) {
			nbttagcompound.setInteger("Side" + side.getId(), side.getData());
		}
		return nbttagcompound;
	}

	@Override
	public void updateEntity() {
		dirty = true;
	}

	public void sendPacket(final int id) {
		sendPacket(id, o -> o.writeByte(0));
	}

	public void sendPacket(final int id, final byte data) {
		sendPacket(id, o -> o.writeByte(data));
	}

	public void sendPacket(final int id, final CheckedConsumer<DataOutput, IOException> writer) {
		MessageStevesCarts.sendPacket(id, writer);
	}

	@Override
	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0 || id == 1) {
			final byte settingId = reader.readByte();
			final byte sideId = reader.readByte();
			if (settingId >= 0 && settingId < DistributorSetting.settings.size() && sideId >= 0 && sideId < getSides().size()) {
				if (id == 0) {
					getSides().get(sideId).set(settingId);
				} else {
					getSides().get(sideId).reset(settingId);
				}
			}
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
		}
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, -1, getUpdateTag());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		handleUpdateTag(pkt.getNbtCompound());
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 3);
	}

	@Override
	public void initGuiData(final Container con, final IContainerListener crafting) { }

	@Override
	public void checkGuiData(final Container con, final IContainerListener crafting) {
		final ContainerDistributor condist = (ContainerDistributor) con;
		for (int i = 0; i < getSides().size(); ++i) {
			final DistributorSide side = getSides().get(i);
			if (side.getLowShortData() != condist.cachedValues.get(i * 2)) {
				updateGuiData(con, crafting, i * 2, side.getLowShortData());
				condist.cachedValues.set(i * 2, side.getLowShortData());
			}
			if (side.getHighShortData() != condist.cachedValues.get(i * 2 + 1)) {
				updateGuiData(con, crafting, i * 2 + 1, side.getHighShortData());
				condist.cachedValues.set(i * 2 + 1, side.getHighShortData());
			}
		}
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		final int sideId = id / 2;
		final boolean isHigh = id % 2 == 1;
		final DistributorSide side = getSides().get(sideId);
		if (isHigh) {
			side.setHighShortData(data);
		} else {
			side.setLowShortData(data);
		}
	}

	public TileEntityManager[] getInventories() {
		if (dirty) {
			generateInventories();
			dirty = false;
		}
		return inventories;
	}

	private void generateInventories() {
		final TileEntityManager bot = generateManager(-1);
		final TileEntityManager top = generateManager(1);
		hasTop = (top != null);
		hasBot = (bot != null);
		inventories = populateManagers(top, bot, hasTop, hasBot);
	}

	private TileEntityManager[] populateManagers(TileEntityManager topElement, TileEntityManager botElement, boolean hasTopElement, boolean hasBotElement) {
		if (!hasTopElement && !hasBotElement) {
			return new TileEntityManager[0];
		}
		if (!hasBotElement) {
			return new TileEntityManager[] { topElement };
		}
		if (!hasTopElement) {
			return new TileEntityManager[] { botElement };
		}
		return new TileEntityManager[] { botElement, topElement };
	}

	private TileEntityManager generateManager(final int y) {
		final TileEntity te = world.getTileEntity(pos.add(0, y, 0));
		if (te != null && te instanceof TileEntityManager) {
			return (TileEntityManager) te;
		}
		return null;
	}

	@Override
	public boolean isUsableByPlayer(final EntityPlayer entityplayer) {
		return world.getTileEntity(pos) == this && entityplayer.getDistanceSqToCenter(pos) <= 64.0;
	}

	private int translateSlotId(final int slot) {
		return slot % 60;
	}

	private TileEntityManager getManagerFromSlotId(final int slot) {
		final TileEntityManager[] invs = getInventories();
		int id = slot / 60;
		if (!hasTop || !hasBot) {
			id = 0;
		}
		if (id < 0 || id >= invs.length) {
			return null;
		}
		return invs[id];
	}

	@Override
	public int getSizeInventory() {
		return 120;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	@Nonnull
	public ItemStack getStackInSlot(final int slot) {
		final TileEntityManager manager = getManagerFromSlotId(slot);
		if (manager != null) {
			return manager.getStackInSlot(translateSlotId(slot));
		}
		return ItemStack.EMPTY;
	}

	@Override
	@Nonnull
	public ItemStack decrStackSize(final int slot, final int count) {
		final TileEntityManager manager = getManagerFromSlotId(slot);
		if (manager != null) {
			return manager.decrStackSize(translateSlotId(slot), count);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setInventorySlotContents(final int slot, @Nonnull ItemStack itemstack) {
		final TileEntityManager manager = getManagerFromSlotId(slot);
		if (manager != null) {
			manager.setInventorySlotContents(translateSlotId(slot), itemstack);
		}
	}

	@Override
	public String getName() {
		return "container.cargodistributor";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	@Nonnull
	public ItemStack removeStackFromSlot(final int slot) {
		final TileEntityManager manager = getManagerFromSlotId(slot);
		if (manager != null) {
			return manager.getStackInSlotOnClosing(translateSlotId(slot));
		}
		return ItemStack.EMPTY;
	}

	private boolean isChunkValid(final DistributorSide side, final TileEntityManager manager, final int chunkId, final boolean top) {
		for (final DistributorSetting setting : DistributorSetting.settings) {
			if (setting.isEnabled(this) && side.isSet(setting.getId()) && setting.isValid(manager, chunkId, top)) {
				return true;
			}
		}
		return false;
	}

	private FluidStack drain(final EnumFacing from, final FluidStack resource, int maxDrain, final boolean doDrain) {
		FluidStack ret = resource;
		if (ret != null) {
			ret = ret.copy();
			ret.amount = 0;
		}
		final IFluidTank[] tanks = getTanks(from);
		for (final IFluidTank tank : tanks) {
			FluidStack temp = tank.drain(maxDrain, doDrain);
			if (temp != null && (ret == null || ret.isFluidEqual(temp))) {
				if (ret == null) {
					ret = temp;
				} else {
					final FluidStack fluidStack = ret;
					fluidStack.amount += temp.amount;
				}
				maxDrain -= temp.amount;
				if (maxDrain <= 0) {
					break;
				}
			}
		}
		if (ret != null && ret.amount == 0) {
			return null;
		}
		return ret;
	}

	private boolean hasAnyTank(EnumFacing facing) {
		return facing != null && getInventories().length > 0 && getTanks(facing).length > 0;
	}

	private SCTank[] getTanks(final EnumFacing direction) {
		final TileEntityManager[] invs = getInventories();
		if (invs.length > 0) {
			for (final DistributorSide side : getSides()) {
				if (side.getSide() == direction) {
					final ArrayList<SCTank> tanks = new ArrayList<>();
					if (hasTop && hasBot) {
						populateTanks(tanks, side, invs[0], false);
						populateTanks(tanks, side, invs[1], true);
					} else if (hasTop) {
						populateTanks(tanks, side, invs[0], true);
					} else if (hasBot) {
						populateTanks(tanks, side, invs[0], false);
					}
					return tanks.toArray(new SCTank[tanks.size()]);
				}
			}
		}
		return new SCTank[0];
	}

	private void populateTanks(final ArrayList<SCTank> tanks, final DistributorSide side, final TileEntityManager manager, final boolean top) {
		if (manager != null && manager instanceof TileEntityLiquid) {
			final TileEntityLiquid fluid = (TileEntityLiquid) manager;
			final SCTank[] managerTanks = fluid.getTanks();
			for (int i = 0; i < 4; ++i) {
				if (isChunkValid(side, manager, i, top) && !tanks.contains(managerTanks[i])) {
					tanks.add(managerTanks[i]);
				}
			}
		}
	}

	private void populateSlots(final ArrayList<Integer> slotchunks, final DistributorSide side, final TileEntityManager manager, final boolean top) {
		if (manager != null && manager instanceof TileEntityCargo) {
			for (int i = 0; i < 4; ++i) {
				if (isChunkValid(side, manager, i, top)) {
					final int chunkid = i + (top ? 4 : 0);
					if (!slotchunks.contains(chunkid)) {
						slotchunks.add(chunkid);
					}
				}
			}
		}
	}

	@Override
	public boolean canInsertItem(final int slot, @Nonnull ItemStack item, EnumFacing side) {
		return true;
	}

	@Override
	public boolean canExtractItem(final int slot, @Nonnull ItemStack item, EnumFacing side) {
		return true;
	}

	@Override
	public boolean isItemValidForSlot(final int slotId, @Nonnull ItemStack item) {
		return true;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		final TileEntityManager[] invs = getInventories();
		if (invs.length > 0) {
			for (final DistributorSide otherSide : getSides()) {
				if (otherSide.getFacing() == side) {
					final ArrayList<Integer> slotchunks = new ArrayList<>();
					if (hasTop && hasBot) {
						populateSlots(slotchunks, otherSide, invs[0], false);
						populateSlots(slotchunks, otherSide, invs[1], true);
					} else if (hasTop) {
						populateSlots(slotchunks, otherSide, invs[0], true);
					} else if (hasBot) {
						populateSlots(slotchunks, otherSide, invs[0], false);
					}
					final int[] ret = new int[slotchunks.size() * 15];
					int id = 0;
					for (final int chunkid : slotchunks) {
						for (int i = 0; i < 15; ++i) {
							ret[id] = chunkid * 15 + i;
							++id;
						}
					}
					return ret;
				}
			}
		}
		return new int[0];
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && hasAnyTank(facing)) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return (T) new SidedInvWrapper(this, facing);
		}
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && hasAnyTank(facing)) {
			return (T) fluidHandlerMap.get(facing);
		}
		return super.getCapability(capability, facing);
	}
}
