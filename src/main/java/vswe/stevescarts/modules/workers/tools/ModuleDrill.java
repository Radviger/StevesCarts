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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
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
	public WorkResult work() {
		World world = getCart().world;
		if (!isDrillEnabled()) {
			stopDrill();
			stopWorking();
			return WorkResult.SKIP;
		} else if (!doPreWork()) {
			stopDrill();
			stopWorking();
		}
		if (isBroken()) {
			return WorkResult.SKIP;
		}
		BlockPos next = getNextblock();
		int[] range = mineRange();
 		for (int holeY = range[1]; holeY >= range[0]; holeY--) {
			for (int holeX = -blocksOnSide(); holeX <= blocksOnSide(); holeX++) {
				if (isMiningSpotAllowed(next, holeX, holeY, range)) {
					BlockPos mine = next.add(((getCart().z() != next.getZ()) ? holeX : 0), holeY, ((getCart().x() != next.getX()) ? holeX : 0));
					WorkResult m = mineBlockAndRevive(world, mine, next, holeX, holeY);
					if (m != WorkResult.SKIP) {
						return m;
					}
				}
			}
		}
		BlockPos pos = next.add(0, range[0], 0);
		if (countsAsAir(pos) && !isValidForTrack(pos, true)) {
			WorkResult m = mineBlockAndRevive(world, pos.down(), next, 0, range[0] - 1);
			if (m != WorkResult.SKIP) {
				return m;
			}
		}
		stopWorking();
		stopDrill();
		return WorkResult.SKIP;
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

	private WorkResult mineBlockAndRevive(World world, BlockPos coord, BlockPos next, final int holeX, final int holeY) {
		WorkResult m = mineBlock(world, coord, next, holeX, holeY, false);
		if (m != WorkResult.SKIP) {
			return m;
		} else if (isDead()) {
			revive();
			return WorkResult.SUCCESS;
		}
		return WorkResult.SKIP;
	}

	protected WorkResult mineBlock(World world, BlockPos pos, BlockPos next, final int holeX, final int holeY, final boolean flag) {
		if (tracker != null) {
			final BlockPos target = tracker.findBlockToMine(this, pos);
			if (target != null) {
				pos = target;
			}
		}
		final Object valid = isValidBlock(world, pos, holeX, holeY, flag);
		TileEntity storage = null;
		if (valid instanceof TileEntity) {
			storage = (TileEntity) valid;
		} else if (valid == null) {
			return WorkResult.SKIP;
		}
		IBlockState state = world.getBlockState(pos);
		BlockEvent e = new BlockEvent.BreakEvent(world, pos, state, getCartOwner());
		if (!MinecraftForge.EVENT_BUS.post(e)) {
			final Block block = state.getBlock();
			float h = state.getBlockHardness(world, pos);
			if (h < 0.0f) {
				h = 0.0f;
			}
			//TODO change to capabilities
			if (storage != null) {
				for (int i = 0; i < ((IInventory) storage).getSizeInventory(); ++i) {
					ItemStack stack = ((IInventory) storage).getStackInSlot(i);
					if (!stack.isEmpty()) {
						if (!minedItem(world, stack, next)) {
							return WorkResult.SKIP;
						}
						((IInventory) storage).setInventorySlotContents(i, ItemStack.EMPTY);
					}
				}
			}
			final int fortune = (enchanter != null) ? enchanter.getFortuneLevel() : 0;
			boolean shouldRemove = false;
			if (shouldSilkTouch(state, pos)) {
				ItemStack item = getSilkTouchedItem(state);
				if (!item.isEmpty() && !minedItem(world, item, next)) {
					return WorkResult.SKIP;
				}
			} else if (block.getDrops(world, pos, state, fortune).size() != 0) {
				List<ItemStack> stacks = block.getDrops(world, pos, state, fortune);
				shouldRemove = false;
				for (ItemStack stack : stacks) {
					if (!minedItem(world, stack, next)) {
						return WorkResult.SKIP;
					}
					shouldRemove = true;
				}
			}
			if (shouldRemove) {
				world.playEvent(2001, pos, Block.getStateId(state));
				world.setBlockToAir(pos);
			}
			damageTool(1 + (int) h);
			startWorking(getTimeToMine(h));
			startDrill();
			return WorkResult.SUCCESS;
		} else {
			return WorkResult.FAILURE;
		}
	}

	protected boolean minedItem(World world, @Nonnull ItemStack stack, BlockPos pos) {
		if (stack.isEmpty() || stack.getCount() <= 0) {
			return true;
		}
		for (ModuleBase module : getCart().getModules()) {
			if (module instanceof ModuleIncinerator) {
				((ModuleIncinerator) module).incinerate(stack);
				if (stack.getCount() <= 0) {
					return true;
				}
			}
		}
		int size = stack.getCount();
		getCart().addItemToChest(stack);
		if (stack.getCount() == 0) {
			return true;
		}
		boolean hasChest = false;
		for (ModuleBase m : getCart().getModules()) {
			if (m instanceof ModuleChest) {
				hasChest = true;
				break;
			}
		}
		if (!hasChest) {
			final EntityItem item = new EntityItem(world, getCart().posX, getCart().posY, getCart().posZ, stack);
			item.motionX = (getCart().x() - pos.getX()) / 10.0f;
			item.motionY = 0.15000000596046448;
			item.motionZ = (getCart().z() - pos.getZ()) / 10.0f;
			world.spawnEntity(item);
			return true;
		}
		if (stack.getCount() != size) {
			final EntityItem item = new EntityItem(world, getCart().posX, getCart().posY, getCart().posZ, stack);
			item.motionX = (getCart().z() - pos.getZ()) / 10.0f;
			item.motionY = 0.15000000596046448;
			item.motionZ = (getCart().x() - pos.getX()) / 10.0f;
			world.spawnEntity(item);
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
			if (tileentity instanceof IInventory) {
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
