package vswe.stevescarts.blocks.tileentities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import vswe.stevescarts.api.util.CheckedConsumer;
import vswe.stevescarts.containers.ContainerManager;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.NBTHelper;
import vswe.stevescarts.helpers.storages.TransferManager;
import vswe.stevescarts.network.message.MessageStevesCarts;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class TileEntityManager extends TileEntityBase implements IInventory {
	private TransferManager standardTransferHandler;
	private NonNullList<ItemStack> cargoItemStacks;
	public int layoutType;
	public int moveTime;
	public boolean[] toCart;
	public boolean[] doReturn;
	public int[] amount;
	public int[] color;

	public TileEntityManager() {
		toCart = new boolean[] { true, true, true, true };
		doReturn = new boolean[] { false, false, false, false };
		amount = new int[] { 0, 0, 0, 0 };
		color = new int[] { 1, 2, 3, 4 };
		cargoItemStacks = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
		moveTime = 0;
		standardTransferHandler = new TransferManager();
	}

	@Override
	@Nonnull
	public ItemStack getStackInSlot(final int i) {
		return cargoItemStacks.get(i);
	}

	@Override
	@Nonnull
	public ItemStack decrStackSize(int index, int count) {
		ItemStack itemstack = ItemStackHelper.getAndSplit(cargoItemStacks, index, count);
		if (!itemstack.isEmpty()) {
			markDirty();
		}
		return itemstack;
	}

	@Override
	public void setInventorySlotContents(final int i, @Nonnull ItemStack itemstack) {
		cargoItemStacks.set(i, itemstack);
		if (!itemstack.isEmpty() && itemstack.getCount() > getInventoryStackLimit()) {
			itemstack.setCount(getInventoryStackLimit());
		}
		markDirty();
	}

	@Override
	public void readFromNBT(final NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		final NBTTagList nbttaglist = nbttagcompound.getTagList("Items", NBTHelper.COMPOUND.getId());
		cargoItemStacks = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
		for (int i = 0; i < nbttaglist.tagCount(); ++i) {
			final NBTTagCompound nbttagcompound2 = nbttaglist.getCompoundTagAt(i);
			final byte byte0 = nbttagcompound2.getByte("Slot");
			if (byte0 >= 0 && byte0 < cargoItemStacks.size()) {
				cargoItemStacks.set(byte0, new ItemStack(nbttagcompound2));
			}
		}
		moveTime = nbttagcompound.getByte("movetime");
		setLowestSetting(nbttagcompound.getByte("lowestNumber"));
		layoutType = nbttagcompound.getByte("layout");
		final byte temp = nbttagcompound.getByte("tocart");
		final byte temp2 = nbttagcompound.getByte("doReturn");
		for (int j = 0; j < 4; ++j) {
			amount[j] = nbttagcompound.getByte("amount" + j);
			color[j] = nbttagcompound.getByte("color" + j);
			if (color[j] == 0) {
				color[j] = j + 1;
			}
			toCart[j] = ((temp & 1 << j) != 0x0);
			doReturn[j] = ((temp2 & 1 << j) != 0x0);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setByte("movetime", (byte) moveTime);
		nbttagcompound.setByte("lowestNumber", (byte) getLowestSetting());
		nbttagcompound.setByte("layout", (byte) layoutType);
		byte temp = 0;
		byte temp2 = 0;
		for (int i = 0; i < 4; ++i) {
			nbttagcompound.setByte("amount" + i, (byte) amount[i]);
			nbttagcompound.setByte("color" + i, (byte) color[i]);
			if (toCart[i]) {
				temp |= (byte) (1 << i);
			}
			if (doReturn[i]) {
				temp2 |= (byte) (1 << i);
			}
		}
		nbttagcompound.setByte("tocart", temp);
		nbttagcompound.setByte("doReturn", temp2);
		final NBTTagList nbttaglist = new NBTTagList();
		for (int j = 0; j < cargoItemStacks.size(); ++j) {
			if (!cargoItemStacks.get(j).isEmpty()) {
				final NBTTagCompound nbttagcompound2 = new NBTTagCompound();
				nbttagcompound2.setByte("Slot", (byte) j);
				cargoItemStacks.get(j).writeToNBT(nbttagcompound2);
				nbttaglist.appendTag(nbttagcompound2);
			}
		}
		nbttagcompound.setTag("Items", nbttaglist);
		return nbttagcompound;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	public EntityMinecartModular getCart() {
		return standardTransferHandler.getCart();
	}

	public void setCart(final EntityMinecartModular cart) {
		standardTransferHandler.setCart(cart);
	}

	public int getSetting() {
		return standardTransferHandler.getSetting();
	}

	public void setSetting(final int val) {
		standardTransferHandler.setSetting(val);
	}

	public int getSide() {
		return standardTransferHandler.getSide();
	}

	public void setSide(final int val) {
		standardTransferHandler.setSide(val);
	}

	public int getLastSetting() {
		return standardTransferHandler.getLastSetting();
	}

	public void setLastSetting(final int val) {
		standardTransferHandler.setLastSetting(val);
	}

	public int getLowestSetting() {
		return standardTransferHandler.getLowestSetting();
	}

	public void setLowestSetting(final int val) {
		standardTransferHandler.setLowestSetting(val);
	}

	public int getWorkload() {
		return standardTransferHandler.getWorkload();
	}

	public void setWorkload(final int val) {
		standardTransferHandler.setWorkload(val);
	}

	@Override
	public void updateEntity() {
		if (world.isRemote) {
			updateLayout();
			return;
		}
		if (getCart() == null || getCart().isDead || getSide() < 0 || getSide() > 3 || !getCart().isDisabled()) {
			standardTransferHandler.reset();
			return;
		}
		++moveTime;
		if (moveTime >= 24 || (getSetting() == -1) && color[0] - 1 != getSide()) {
			moveTime = 0;
			if (!exchangeItems(standardTransferHandler)) {
				getCart().releaseCart();
				if (doReturn[getSide()]) {
					getCart().turnback();
				}
				standardTransferHandler.reset();
			}
		}
	}

	public boolean exchangeItems(final TransferManager transfer) {
		for (transfer.setSetting(transfer.getLowestSetting()); transfer.getSetting() < 4; transfer.setSetting(transfer.getSetting() + 1)) {
			if (color[transfer.getSetting()] - 1 != transfer.getSide()) {
				continue;
			}

			transfer.setLowestSetting(transfer.getSetting());
			if (transfer.getLastSetting() != transfer.getSetting()) {
				transfer.setWorkload(0);
				transfer.setLastSetting(transfer.getSetting());
				return true;
			}

			if (!(toCart[transfer.getSetting()] ? transfer.getToCartEnabled() : transfer.getFromCartEnabled()) || !isTargetValid(transfer)) {
				transfer.setLowestSetting(transfer.getSetting() + 1);
				return true;
			}

			if (doTransfer(transfer)) {
				return true;
			}
		}

		return false;
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
		switch (id) {
			case 0: {
				final int railID = reader.readByte();
				toCart[railID] = !toCart[railID];
				if (color[railID] - 1 == getSide()) {
					reset();
				}
				break;
			}
			case 4: {
				final int railID = reader.readByte();
				if (color[railID] != 5) {
					doReturn[color[railID] - 1] = !doReturn[color[railID] - 1];
				}
				break;
			}
			case 5:
				final int difference = reader.readByte();
				layoutType += difference;
				if (layoutType > 2) {
					layoutType = 0;
				} else if (layoutType < 0) {
					layoutType = 2;
				}
				reset();
				break;
			default: {
				final byte railsAndDifferenceCombined = reader.readByte();
				final int railID = railsAndDifferenceCombined & 0x3;
				final int k = (railsAndDifferenceCombined & 0x4) >> 2;
				int difference2 = k == 0 ? 1 : -1;
				if (id == 2) {
					final int[] amount = this.amount;
					amount[railID] += difference2;
					if (this.amount[railID] >= getAmountCount()) {
						this.amount[railID] = 0;
					} else if (this.amount[railID] < 0) {
						this.amount[railID] = getAmountCount() - 1;
					}
					if (color[railID] - 1 == getSide()) {
						reset();
					}
				} else if (id == 3) {
					if (color[railID] != 5) {
						boolean willStillExist = false;
						for (int side = 0; side < 4; ++side) {
							if (side != railID && color[railID] == color[side]) {
								willStillExist = true;
								break;
							}
						}
						if (!willStillExist) {
							doReturn[color[railID] - 1] = false;
						}
					}
					final int[] color = this.color;
					color[railID] += difference2;
					if (this.color[railID] > 5) {
						this.color[railID] = 1;
					} else if (this.color[railID] < 1) {
						this.color[railID] = 5;
					}
					if (this.color[railID] - 1 == getSide()) {
						reset();
					}
				} else {
					receiveClickData(id, railID, difference2);
				}
				break;
			}
		}
	}

	@Override
	public void initGuiData(final Container con, final IContainerListener crafting) {
		checkGuiData((ContainerManager) con, crafting, true);
	}

	@Override
	public void checkGuiData(final Container con, final IContainerListener crafting) {
		checkGuiData((ContainerManager) con, crafting, false);
	}

	public void checkGuiData(final ContainerManager con, final IContainerListener crafting, final boolean isNew) {
		short header = (short) (moveTime & 0x1F);
		header |= (short) ((layoutType & 0x3) << 5);
		for (int i = 0; i < 4; ++i) {
			header |= (short) ((toCart[i] ? 1 : 0) << 7 + i);
		}
		for (int i = 0; i < 4; ++i) {
			header |= (short) ((doReturn[i] ? 1 : 0) << 11 + i);
		}
		if (isNew || con.lastHeader != header) {
			updateGuiData(con, crafting, 0, header);
			con.lastHeader = header;
		}
		short colorShort = 0;
		for (int j = 0; j < 4; ++j) {
			colorShort |= (short) ((color[j] & 0x7) << j * 3);
		}
		colorShort |= (short) ((getLastSetting() & 0x7) << 12);
		if (isNew || con.lastColor != colorShort) {
			updateGuiData(con, crafting, 1, colorShort);
			con.lastColor = colorShort;
		}
		short amountShort = 0;
		for (int k = 0; k < 4; ++k) {
			amountShort |= (short) ((amount[k] & 0xF) << k * 4);
		}
		if (isNew || con.lastAmount != amountShort) {
			updateGuiData(con, crafting, 3, amountShort);
			con.lastAmount = amountShort;
		}
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			moveTime = (data & 0x1F);
			layoutType = (data & 0x60) >> 5;
			updateLayout();
			for (int i = 0; i < 4; ++i) {
				toCart[i] = ((data & 1 << 7 + i) != 0x0);
			}
			for (int i = 0; i < 4; ++i) {
				doReturn[i] = ((data & 1 << 11 + i) != 0x0);
			}
		} else if (id == 1) {
			for (int i = 0; i < 4; ++i) {
				color[i] = (data & 7 << i * 3) >> i * 3;
			}
			setLastSetting((data & 0x7000) >> 12);
		} else if (id == 3) {
			for (int i = 0; i < 4; ++i) {
				amount[i] = (data & 15 << i * 4) >> i * 4;
			}
		}
	}

	public int moveProgressScaled(final int i) {
		return moveTime * i / 24;
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public boolean isUsableByPlayer(final EntityPlayer entityplayer) {
		return world.getTileEntity(pos) == this && entityplayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}

	@Nonnull
	public ItemStack getStackInSlotOnClosing(final int par1) {
		if (!cargoItemStacks.get(par1).isEmpty()) {
			@Nonnull
			ItemStack var2 = cargoItemStacks.get(par1);
			cargoItemStacks.set(par1, ItemStack.EMPTY);
			return var2;
		}
		return ItemStack.EMPTY;
	}

	protected void updateLayout() {
	}

	protected void receiveClickData(final int packetid, final int id, final int dif) {
	}

	protected abstract boolean isTargetValid(final TransferManager p0);

	protected abstract boolean doTransfer(final TransferManager p0);

	public abstract int getAmountCount();

	protected void reset() {
		setWorkload(moveTime = 0);
	}

	protected int getAmountId(final int id) {
		return amount[id];
	}

	@Override
	public int getSizeInventory() {
		return cargoItemStacks.size();
	}

	@Nullable
	@Override
	@Nonnull
	public ItemStack removeStackFromSlot(int index) {
		return ItemStackHelper.getAndRemove(cargoItemStacks, index);
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {

	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {

	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : cargoItemStacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getName() {
		return "container.cargomanager";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return (T) new InvWrapper(this);
		}
		return super.getCapability(capability, facing);
	}
}
