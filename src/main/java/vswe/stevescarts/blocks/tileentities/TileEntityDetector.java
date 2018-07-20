package vswe.stevescarts.blocks.tileentities;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.blocks.BlockDetector;
import vswe.stevescarts.blocks.ModBlocks;
import vswe.stevescarts.containers.ContainerBase;
import vswe.stevescarts.containers.ContainerDetector;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiBase;
import vswe.stevescarts.guis.GuiDetector;
import vswe.stevescarts.helpers.DetectorType;
import vswe.stevescarts.helpers.LogicObject;

import java.io.DataInput;
import java.io.IOException;

public class TileEntityDetector extends TileEntityBase {
	public LogicObject mainObj;
	private int activeTimer;
	private short oldData;
	private boolean hasOldData;

	@SideOnly(Side.CLIENT)
	@Override
	public GuiBase getGui(final InventoryPlayer inv) {
		return new GuiDetector(inv, this);
	}

	@Override
	public ContainerBase getContainer(final InventoryPlayer inv) {
		return new ContainerDetector(inv, this);
	}

	public TileEntityDetector() {
		activeTimer = 20;
		mainObj = new LogicObject((byte) 1, (byte) 0);
	}

	@Override
	public void readFromNBT(final NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		final byte count = nbttagcompound.getByte("LogicObjectCount");
		for (int i = 0; i < count; ++i) {
			loadLogicObjectFromInteger(nbttagcompound.getInteger("LogicObject" + i));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		final int count = saveLogicObject(nbttagcompound, mainObj, 0, false);
		nbttagcompound.setByte("LogicObjectCount", (byte) count);
		return nbttagcompound;
	}

	private int saveLogicObject(final NBTTagCompound nbttagcompound, final LogicObject obj, int id, final boolean saveMe) {
		if (saveMe) {
			nbttagcompound.setInteger("LogicObject" + id++, saveLogicObjectToInteger(obj));
		}
		for (final LogicObject child : obj.getChilds()) {
			id = saveLogicObject(nbttagcompound, child, id, true);
		}
		return id;
	}

	private int saveLogicObjectToInteger(final LogicObject obj) {
		int returnVal = 0;
		returnVal |= obj.getId() << 24;
		returnVal |= obj.getParent().getId() << 16;
		returnVal |= obj.getExtra() << 8;
		returnVal |= obj.getData() << 0;
		return returnVal;
	}

	private void loadLogicObjectFromInteger(final int val) {
		final byte id = (byte) (val >> 24 & 0xFF);
		final byte parent = (byte) (val >> 16 & 0xFF);
		final byte extra = (byte) (val >> 8 & 0xFF);
		final byte data = (byte) (val >> 0 & 0xFF);
		createObject(id, parent, extra, data);
	}

	@Override
	public void updateEntity() {
		if (activeTimer > 0 && --activeTimer == 0) {
			IBlockState blockState = world.getBlockState(pos);
			Block block = blockState.getBlock();
			if (block == ModBlocks.DETECTOR_UNIT.getBlock()) {
				DetectorType.getTypeFromSate(blockState).deactivate(this);
				world.setBlockState(pos, blockState.withProperty(BlockDetector.POWERED, false), 3);
			}
		}
	}

	@Override
	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0) {
			byte lowestId = -1;
			for (int i = 0; i < 128; ++i) {
				if (!isIdOccupied(mainObj, i)) {
					lowestId = (byte) i;
					break;
				}
			}
			if (lowestId == -1) {
				return;
			}
			createObject(lowestId, reader.readByte(), reader.readByte(), reader.readByte());
		} else if (id == 1) {
			removeObject(mainObj, reader.readByte());
		}
	}

	private void createObject(final byte id, final byte parentId, final byte extra, final byte data) {
		final LogicObject newObject = new LogicObject(id, extra, data);
		final LogicObject parent = getObjectFromId(mainObj, parentId);
		if (parent != null) {
			newObject.setParent(parent);
		}
	}

	private LogicObject getObjectFromId(final LogicObject object, final int id) {
		if (object.getId() == id) {
			return object;
		}
		for (final LogicObject child : object.getChilds()) {
			final LogicObject result = getObjectFromId(child, id);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private boolean removeObject(final LogicObject object, final int idToRemove) {
		if (object.getId() == idToRemove) {
			object.setParent(null);
			return true;
		}
		for (final LogicObject child : object.getChilds()) {
			if (removeObject(child, idToRemove)) {
				return true;
			}
		}
		return false;
	}

	private boolean isIdOccupied(final LogicObject object, final int id) {
		if (object.getId() == id) {
			return true;
		}
		for (final LogicObject child : object.getChilds()) {
			if (isIdOccupied(child, id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void initGuiData(final Container con, final IContainerListener crafting) {
	}

	@Override
	public void checkGuiData(final Container con, final IContainerListener crafting) {
		sendUpdatedLogicObjects(con, crafting, mainObj, ((ContainerDetector) con).mainObj);
	}

	private void sendUpdatedLogicObjects(final Container con, final IContainerListener crafting, final LogicObject real, LogicObject cache) {
		if (!real.equals(cache)) {
			final LogicObject parent = cache.getParent();
			cache.setParent(null);
			final LogicObject clone = real.copy(parent);
			removeLogicObject(con, crafting, cache);
			sendLogicObject(con, crafting, clone);
			cache = clone;
		}
		while (real.getChilds().size() > cache.getChilds().size()) {
			final int i = cache.getChilds().size();
			final LogicObject clone = real.getChilds().get(i).copy(cache);
			sendLogicObject(con, crafting, clone);
		}
		while (real.getChilds().size() < cache.getChilds().size()) {
			final int i = real.getChilds().size();
			final LogicObject toBeRemoved = cache.getChilds().get(i);
			toBeRemoved.setParent(null);
			removeLogicObject(con, crafting, toBeRemoved);
		}
		for (int i = 0; i < real.getChilds().size(); ++i) {
			sendUpdatedLogicObjects(con, crafting, real.getChilds().get(i), cache.getChilds().get(i));
		}
	}

	private void sendAllLogicObjects(final Container con, final IContainerListener crafting, final LogicObject obj) {
		sendLogicObject(con, crafting, obj);
		for (final LogicObject child : obj.getChilds()) {
			sendAllLogicObjects(con, crafting, child);
		}
	}

	private void sendLogicObject(final Container con, final IContainerListener crafting, final LogicObject obj) {
		if (obj.getParent() == null) {
			return;
		}
		final short data = (short) (obj.getId() << 8 | obj.getParent().getId());
		final short data2 = (short) (obj.getExtra() << 8 | obj.getData());
		updateGuiData(con, crafting, 0, data);
		updateGuiData(con, crafting, 1, data2);
	}

	private void removeLogicObject(final Container con, final IContainerListener crafting, final LogicObject obj) {
		updateGuiData(con, crafting, 2, obj.getId());
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			oldData = data;
			hasOldData = true;
		} else if (id == 1) {
			if (!hasOldData) {
				System.out.println("Doesn't have the other part of the data");
				return;
			}
			final byte logicid = (byte) ((oldData & 0xFF00) >> 8);
			final byte parent = (byte) (oldData & 0xFF);
			final byte extra = (byte) ((data & 0xFF00) >> 8);
			final byte logicdata = (byte) (data & 0xFF);
			createObject(logicid, parent, extra, logicdata);
			recalculateTree();
			hasOldData = false;
		} else if (id == 2) {
			removeObject(mainObj, data);
			recalculateTree();
		}
	}

	public void recalculateTree() {
		mainObj.generatePosition(5, 60, 245, 0);
	}

	public boolean evaluate(final EntityMinecartModular cart, final int depth) {
		return mainObj.evaluateLogicTree(this, cart, depth);
	}

	public void handleCart(final EntityMinecartModular cart) {
		final boolean truthValue = evaluate(cart, 0);
		IBlockState blockState = world.getBlockState(pos);
		boolean isOn = blockState.getValue(BlockDetector.POWERED);
		boolean power = false;
		if (truthValue) {
			DetectorType.getTypeFromSate(blockState).activate(this, cart);
			power = true;
		} else {
			DetectorType.getTypeFromSate(blockState).deactivate(this);
			power &= false;
		}
		if (power != isOn) {
			world.setBlockState(pos, blockState.withProperty(BlockDetector.POWERED, power), 3);
		}

		if (truthValue) {
			activeTimer = 20;
		}
	}

	@Override
	public boolean isUsableByPlayer(final EntityPlayer entityplayer) {
		return world.getTileEntity(pos) == this && entityplayer.getDistanceSqToCenter(pos) <= 64.0;
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
		return false;
	}

	public DetectorType getType() {
		return world.getBlockState(pos).getValue(BlockDetector.SATE);
	}
}
