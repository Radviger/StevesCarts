package vswe.stevescarts.modules.workers.tools;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.SCConfig;
import vswe.stevescarts.blocks.ModBlocks;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.IActivatorModule;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.addons.*;
import vswe.stevescarts.modules.storages.chests.ModuleChest;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.util.List;

public abstract class ModuleDrill extends ModuleTool implements IActivatorModule {
	private ModuleDrillIntelligence intelligence;
	private ModuleLiquidSensors liquidsensors;
	private ModuleOreTracker tracker;
	private boolean hasHeightController;
	private byte sensorLight;
	private float drillRotation;
	private int miningCoolDown;
	private int[] buttonRect;
	private DataParameter<Boolean> IS_MINING;
	private DataParameter<Boolean> IS_ENABLED;

	public ModuleDrill(final EntityMinecartModular cart) {
		super(cart);
		sensorLight = 1;
		buttonRect = new int[] { 15, 30, 24, 12 };
	}

	@Override
	public byte getWorkPriority() {
		return 50;
	}

	@Override
	public void init() {
		super.init();
		for (final ModuleBase module : getCart().getModules()) {
			if (module instanceof ModuleDrillIntelligence) {
				intelligence = (ModuleDrillIntelligence) module;
			}
			if (module instanceof ModuleLiquidSensors) {
				liquidsensors = (ModuleLiquidSensors) module;
			}
			if (module instanceof ModuleOreTracker) {
				tracker = (ModuleOreTracker) module;
			}
			if (module instanceof ModuleHeightControl) {
				hasHeightController = true;
			}
		}
	}

	@Override
	public boolean work() {
		World world = getCart().world;
		if (!isDrillEnabled()) {
			stopDrill();
			stopWorking();
			return false;
		} else if (!doPreWork()) {
			stopDrill();
			stopWorking();
		}
		if (isBroken()) {
			return false;
		}
		BlockPos next = getNextblock();
		int[] range = mineRange();
 		for (int holeY = range[1]; holeY >= range[0]; holeY--) {
			for (int holeX = -blocksOnSide(); holeX <= blocksOnSide(); holeX++) {
				if (isMiningSpotAllowed(next, holeX, holeY, range)) {
					BlockPos mine = next.add(((getCart().z() != next.getZ()) ? holeX : 0), holeY, ((getCart().x() != next.getX()) ? holeX : 0));
					if (mineBlockAndRevive(world, mine, next, holeX, holeY)) {
						return true;
					}
				}
			}
		}
		BlockPos pos = next.add(0, range[0], 0);
		if (countsAsAir(pos) && !isValidForTrack(pos, true) && mineBlockAndRevive(world, pos.down(), next, 0, range[0] - 1)) {
			return true;
		}
		stopWorking();
		stopDrill();
		return false;
	}

	private boolean isMiningSpotAllowed(BlockPos next, int holeX, int holeY, int[] range) {
		int maxHeight = SCConfig.drillSize * 2 + 1 - (hasHeightController ? range[2] == 0 ? -1: 1: 0);
		if (Math.abs(holeX) <= SCConfig.drillSize && holeY <= maxHeight) {
			return intelligence == null || intelligence.isActive(holeX + blocksOnSide(), holeY, range[2], next.getX() > getCart().x() || next.getZ() < getCart().z());
		}
		return false;
	}

	private int[] mineRange() {
		BlockPos next = getNextblock();
		int yTarget = getCart().getYTarget();
		if (BlockRailBase.isRailBlock(getCart().world, next) || BlockRailBase.isRailBlock(getCart().world, next.down())) {
			return new int[] { 0, blocksOnTop() - 1, 1 };
		}
		if (next.getY() > yTarget) {
			return new int[] { -1, blocksOnTop() - 1, 1 };
		}
		if (next.getY() < yTarget) {
			return new int[] { 1, blocksOnTop() + 1, 0 };
		}
		return new int[] { 0, blocksOnTop() - 1, 1 };
	}

	protected abstract int blocksOnTop();

	protected abstract int blocksOnSide();

	public int getAreaWidth() {
		return blocksOnSide() * 2 + 1;
	}

	public int getAreaHeight() {
		return blocksOnTop();
	}

	private boolean mineBlockAndRevive(World world, BlockPos coord, BlockPos next, final int holeX, final int holeY) {
		if (mineBlock(world, coord, next, holeX, holeY, false)) {
			return true;
		} else if (isDead()) {
			revive();
			return true;
		}
		return false;
	}

	protected boolean mineBlock(World world, BlockPos coord, BlockPos next, final int holeX, final int holeY, final boolean flag) {
		if (tracker != null) {
			final BlockPos target = tracker.findBlockToMine(this, coord);
			if (target != null) {
				coord = target;
			}
		}
		final Object valid = isValidBlock(world, coord, holeX, holeY, flag);
		TileEntity storage = null;
		if (valid instanceof TileEntity) {
			storage = (TileEntity) valid;
		} else if (valid == null) {
			return false;
		}
		IBlockState blockState = world.getBlockState(coord);
		final Block block = blockState.getBlock();
		float h = blockState.getBlockHardness(world, coord);
		if (h < 0.0f) {
			h = 0.0f;
		}
		//TODO change to capabilities
		if (storage != null) {
			for (int i = 0; i < ((IInventory) storage).getSizeInventory(); ++i) {
				ItemStack iStack = ((IInventory) storage).getStackInSlot(i);
				if (!iStack.isEmpty()) {
					if (!minedItem(world, iStack, next)) {
						return false;
					}
					((IInventory) storage).setInventorySlotContents(i, ItemStack.EMPTY);
				}
			}
		}
		final int fortune = (enchanter != null) ? enchanter.getFortuneLevel() : 0;
		if (shouldSilkTouch(blockState, coord)) {
			ItemStack item = getSilkTouchedItem(blockState);
			if (!item.isEmpty() && !minedItem(world, item, next)) {
				return false;
			}
			world.setBlockToAir(coord);
		} else if (block.getDrops(world, coord, blockState, fortune).size() != 0) {
			List<ItemStack> stacks = block.getDrops(world, coord, blockState, fortune);
			boolean shouldRemove = false;
			for (int j = 0; j < stacks.size(); ++j) {
				if (!minedItem(world, stacks.get(j), next)) {
					return false;
				}
				shouldRemove = true;
			}
			if (shouldRemove) {
				world.setBlockToAir(coord);
			}
		} else {
			world.setBlockToAir(coord);
		}
		damageTool(1 + (int) h);
		startWorking(getTimeToMine(h));
		startDrill();
		return true;
	}

	protected boolean minedItem(World world, @Nonnull ItemStack iStack, BlockPos Coords) {
		if (iStack.isEmpty() || iStack.getCount() <= 0) {
			return true;
		}
		for (ModuleBase module : getCart().getModules()) {
			if (module instanceof ModuleIncinerator) {
				((ModuleIncinerator) module).incinerate(iStack);
				if (iStack.getCount() <= 0) {
					return true;
				}
				continue;
			}
		}
		int size = iStack.getCount();
		getCart().addItemToChest(iStack);
		if (iStack.getCount() == 0) {
			return true;
		}
		boolean hasChest = false;
		for (ModuleBase module2 : getCart().getModules()) {
			if (module2 instanceof ModuleChest) {
				hasChest = true;
				break;
			}
		}
		if (!hasChest) {
			final EntityItem entityitem = new EntityItem(world, getCart().posX, getCart().posY, getCart().posZ, iStack);
			entityitem.motionX = (getCart().x() - Coords.getX()) / 10.0f;
			entityitem.motionY = 0.15000000596046448;
			entityitem.motionZ = (getCart().z() - Coords.getZ()) / 10.0f;
			world.spawnEntity(entityitem);
			return true;
		}
		if (iStack.getCount() != size) {
			final EntityItem entityitem = new EntityItem(world, getCart().posX, getCart().posY, getCart().posZ, iStack);
			entityitem.motionX = (getCart().z() - Coords.getZ()) / 10.0f;
			entityitem.motionY = 0.15000000596046448;
			entityitem.motionZ = (getCart().x() - Coords.getX()) / 10.0f;
			world.spawnEntity(entityitem);
			return true;
		}
		return false;
	}

	private int getTimeToMine(final float hardness) {
		final int efficiency = (enchanter != null) ? enchanter.getEfficiencyLevel() : 0;
		return (int) (getTimeMult() * hardness / Math.pow(1.2999999523162842, efficiency)) + ((liquidsensors != null) ? 2 : 0);
	}

	protected abstract float getTimeMult();

	public Object isValidBlock(World world, BlockPos pos, final int holeX, final int holeY, final boolean flag) {
		if ((!flag && BlockRailBase.isRailBlock(world, pos)) || BlockRailBase.isRailBlock(world, pos.up())) {
			return null;
		}
		IBlockState blockState = world.getBlockState(pos);
		final Block block = blockState.getBlock();
		if (block == null) {
			return null;
		}
		if (block == Blocks.AIR) {
			return null;
		}
		if (block == Blocks.BEDROCK) {
			return null;
		}
		if (block instanceof BlockLiquid) {
			return null;
		}
		if (blockState.getBlockHardness(world, pos) < 0.0f) {
			return null;
		}
		if ((holeX != 0 || holeY > 0) && (block == Blocks.TORCH || block == Blocks.REDSTONE_WIRE || block == Blocks.REDSTONE_TORCH || block == Blocks.UNLIT_REDSTONE_TORCH || block == Blocks.POWERED_REPEATER || block == Blocks.UNPOWERED_REPEATER || block == Blocks.POWERED_COMPARATOR || block == Blocks.UNPOWERED_COMPARATOR || block == ModBlocks.MODULE_TOGGLER.getBlock())) {
			return null;
		}
		//TODO change to capabilities
		if (block instanceof BlockContainer) {
			final TileEntity tileentity = world.getTileEntity(pos);
			if (IInventory.class.isInstance(tileentity)) {
				if (holeX != 0 || holeY > 0) {
					return null;
				}
				return tileentity;
			}
		}
		if (liquidsensors != null) {
			if (liquidsensors.isDangerous(this, pos.add(0, 1, 0), true) || liquidsensors.isDangerous(this, pos.add(1, 0, 0), false) || liquidsensors.isDangerous(this, pos.add(-1, 0, 0), false) || liquidsensors.isDangerous(this, pos.add(0, 0, 1), false) || liquidsensors.isDangerous(this, pos.add(0, 0, -1), false)) {
				sensorLight = 3;
				return null;
			}
			sensorLight = 2;
		}
		return false;
	}

	@Override
	public void update() {
		super.update();
		if ((getCart().hasFuel() && isMining()) || miningCoolDown < 10) {
			drillRotation = (float) ((drillRotation + 0.03f * (10 - miningCoolDown)) % 6.283185307179586);
			if (isMining()) {
				miningCoolDown = 0;
			} else {
				++miningCoolDown;
			}
		}
		if (!getCart().world.isRemote && liquidsensors != null) {
			byte data = sensorLight;
			if (isDrillSpinning()) {
				data |= 0x4;
			}
			liquidsensors.getInfoFromDrill(data);
			sensorLight = 1;
		}
	}

	protected void startDrill() {
		updateDw(IS_MINING, true);
	}

	protected void stopDrill() {
		updateDw(IS_MINING, false);
	}

	protected boolean isMining() {
		if (isPlaceholder()) {
			return getSimInfo().getDrillSpinning();
		}
		return getDw(IS_MINING);
	}

	protected boolean isDrillSpinning() {
		return isMining() || miningCoolDown < 10;
	}

	@Override
	public void initDw() {
		IS_MINING = createDw(DataSerializers.BOOLEAN);
		IS_ENABLED = createDw(DataSerializers.BOOLEAN);
		registerDw(IS_MINING, false);
		registerDw(IS_ENABLED, true);
	}

	@Override
	public int numberOfDataWatchers() {
		return 2;
	}

	public float getDrillRotation() {
		return drillRotation;
	}

	private boolean isDrillEnabled() {
		return getDw(IS_ENABLED);
	}

	public void setDrillEnabled(final boolean val) {
		updateDw(IS_ENABLED, val);
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
			setDrillEnabled(!isDrillEnabled());
		}
	}

	@Override
	public int numberOfPackets() {
		return 1;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, Localization.MODULES.TOOLS.DRILL.translate(), 8, 6, 4210752);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		super.drawBackground(gui, x, y);
		ResourceHelper.bindResource("/gui/drill.png");
		final int imageID = isDrillEnabled() ? 1 : 0;
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
		super.drawMouseOver(gui, x, y);
		drawStringOnMouseOver(gui, getStateName(), x, y, buttonRect);
	}

	private String getStateName() {
		return Localization.MODULES.TOOLS.TOGGLE.translate(isDrillEnabled() ? "1" : "0");
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		super.Save(tagCompound, id);
		tagCompound.setBoolean(generateNBTName("DrillEnabled", id), isDrillEnabled());
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		super.Load(tagCompound, id);
		setDrillEnabled(tagCompound.getBoolean(generateNBTName("DrillEnabled", id)));
	}

	@Override
	public void doActivate(final int id) {
		setDrillEnabled(true);
	}

	@Override
	public void doDeActivate(final int id) {
		setDrillEnabled(false);
	}

	@Override
	public boolean isActive(final int id) {
		return isDrillEnabled();
	}
}
