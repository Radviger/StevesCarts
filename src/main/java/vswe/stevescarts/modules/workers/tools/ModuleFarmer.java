package vswe.stevescarts.modules.workers.tools;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import vswe.stevescarts.api.farms.ICropModule;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotSeed;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.modules.ISuppliesModule;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.plugins.APIHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public abstract class ModuleFarmer extends ModuleTool implements ISuppliesModule {
	private ArrayList<ICropModule> plantModules;
	private int farming;
	private float farmAngle;
	private float rigAngle;
	private DataParameter<Boolean> IS_FARMING;

	public ModuleFarmer(final EntityMinecartModular cart) {
		super(cart);
		rigAngle = -3.926991f;
	}

	protected abstract int getRange();

	public int getExternalRange() {
		return getRange();
	}

	@Override
	public void init() {
		super.init();
		plantModules = new ArrayList<>();
		for (final ModuleBase module : getCart().getModules()) {
			if (module instanceof ICropModule) {
				plantModules.add((ICropModule) module);
			}
		}
		for (ICropModule cropModule : APIHelper.cropModules) {
			plantModules.add(cropModule);
		}
	}

	@Override
	public byte getWorkPriority() {
		return 80;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, Localization.MODULES.TOOLS.FARMER.translate(), 8, 6, 4210752);
	}

	@Override
	protected int getInventoryWidth() {
		return super.getInventoryWidth() + 3;
	}

	@Override
	protected SlotBase getSlot(final int slotId, int x, final int y) {
		if (x == 0) {
			return super.getSlot(slotId, x, y);
		}
		--x;
		return new SlotSeed(getCart(), this, slotId, 8 + x * 18, 28 + y * 18);
	}

	@Override
	public WorkResult work() {
		World world = getCart().world;
		BlockPos next = getNextblock();
		int range = getRange();
		for (int i = -range; i <= range; ++i) {
			for (int j = -range; j <= range; ++j) {
				BlockPos coord = next.add(i, -1, j);
				WorkResult f = farm(world, coord);
				if (f != WorkResult.SKIP) {
					return f;
				}
				WorkResult t = till(world, coord);
				if (t != WorkResult.SKIP) {
					return t;
				}
				WorkResult p = plant(world, coord);
				if (p != WorkResult.SKIP) {
					return p;
				}
			}
		}
		return WorkResult.SKIP;
	}

	protected WorkResult till(World world, BlockPos pos) {
		Block block = world.getBlockState(pos).getBlock();
		if (world.isAirBlock(pos.up()) && (block == Blocks.GRASS || block == Blocks.DIRT)) {
			if (doPreWork()) {
				startWorking(10);
				return WorkResult.SUCCESS;
			}
			stopWorking();
			PlayerInteractEvent e = new PlayerInteractEvent.RightClickBlock(getCartOwner(), EnumHand.MAIN_HAND, pos, EnumFacing.UP, Vec3d.ZERO);
			if (!MinecraftForge.EVENT_BUS.post(e)) {
				world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1F, 1F);
				world.setBlockState(pos, Blocks.FARMLAND.getDefaultState());
			} else {
				return WorkResult.FAILURE;
			}
		}
		return WorkResult.SKIP;
	}

	protected WorkResult plant(World world, BlockPos pos) {
		int hasSeeds = -1;
		IBlockState soilState = world.getBlockState(pos);
		Block soilblock = soilState.getBlock();
		if (soilblock != Blocks.AIR) {
			for (int i = 0; i < getInventorySize(); ++i) {
				if (!getStack(i).isEmpty() && isSeedValidHandler(getStack(i))) {
					IBlockState cropblock = getCropFromSeedHandler(getStack(i), world, pos);
					if (cropblock != null && cropblock.getBlock() instanceof IPlantable && world.isAirBlock(pos.up()) && soilblock.canSustainPlant(soilState, world, pos, EnumFacing.UP, (IPlantable) cropblock.getBlock())) {
						hasSeeds = i;
						break;
					}
				}
			}
			if (hasSeeds != -1) {
				if (doPreWork()) {
					startWorking(25);
					return WorkResult.SUCCESS;
				}
				stopWorking();
				IBlockState cropblock2 = getCropFromSeedHandler(getStack(hasSeeds), world, pos);
				world.setBlockState(pos.up(), cropblock2);
				ItemStack stack = getStack(hasSeeds);
				stack.shrink(1);
				if (getStack(hasSeeds).getCount() <= 0) {
					setStack(hasSeeds, ItemStack.EMPTY);
				}
			}
		}
		return WorkResult.SKIP;
	}

	protected WorkResult farm(World world, BlockPos pos) {
		EntityMinecartModular cart = getCart();
		if (!isBroken()) {
			pos = pos.up();
			IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (isReadyToHarvestHandler(world, pos)) {
				if (doPreWork()) {
					final int efficiency = (enchanter != null) ? enchanter.getEfficiencyLevel() : 0;
					final int workingtime = (int) (getBaseFarmingTime() / Math.pow(1.2999999523162842, efficiency));
					setFarming(workingtime * 4);
					startWorking(workingtime);
					return WorkResult.SUCCESS;
				}
				stopWorking();
				FakePlayer player = getCartOwner();
				BlockEvent e = new BlockEvent.BreakEvent(world, pos, state, player);
				if (!MinecraftForge.EVENT_BUS.post(e)) {
					NonNullList<ItemStack> stuff = NonNullList.create();
					if (shouldSilkTouch(state, pos)) {
						ItemStack stack = getSilkTouchedItem(state);
						if (!stack.isEmpty()) {
							stuff.add(stack);
						}
					} else {
						final int fortune = (enchanter != null) ? enchanter.getFortuneLevel() : 0;
						block.getDrops(stuff, world, pos, state, fortune);
					}
					for (ItemStack s : stuff) {
						cart.addItemToChest(s);
						if (s.getCount() != 0) {
							final EntityItem item = new EntityItem(world, cart.posX, cart.posY, cart.posZ, s);
							item.motionX = (pos.getX() - cart.x()) / 10.0f;
							item.motionY = 0.15000000596046448;
							item.motionZ = (pos.getZ() - cart.z()) / 10.0f;
							world.spawnEntity(item);
						}
					}
					world.playEvent(2001, pos, Block.getStateId(state));
					world.setBlockToAir(pos);
					damageTool(3);
				} else {
					return WorkResult.FAILURE;
				}
			}
		}
		return WorkResult.SKIP;
	}

	protected int getBaseFarmingTime() {
		return 25;
	}

	public boolean isSeedValidHandler(
		@Nonnull
			ItemStack seed) {
		for (final ICropModule module : plantModules) {
			if (module.isSeedValid(seed)) {
				return true;
			}
		}
		return false;
	}

	protected IBlockState getCropFromSeedHandler(
		@Nonnull
			ItemStack seed, World world, BlockPos pos) {
		for (final ICropModule module : plantModules) {
			if (module.isSeedValid(seed)) {
				return module.getCropFromSeed(seed, world, pos);
			}
		}
		return null;
	}

	protected boolean isReadyToHarvestHandler(World world, BlockPos pos) {
		for (final ICropModule module : plantModules) {
			if (module.isReadyToHarvest(world, pos)) {
				return true;
			}
		}
		return false;
	}

	public float getFarmAngle() {
		return farmAngle;
	}

	public float getRigAngle() {
		return rigAngle;
	}

	@Override
	public void initDw() {
		IS_FARMING = createDw(DataSerializers.BOOLEAN);
		registerDw(IS_FARMING, false);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	private void setFarming(final int val) {
		farming = val;
		updateDw(IS_FARMING, val > 0);
	}

	protected boolean isFarming() {
		if (isPlaceholder()) {
			return getSimInfo().getIsFarming();
		}
		return getCart().isEngineBurning() && getDw(IS_FARMING);
	}

	@Override
	public void update() {
		super.update();
		if (!getCart().world.isRemote) {
			setFarming(farming - 1);
		} else {
			final float up = -3.926991f;
			final float down = -3.1415927f;
			final boolean flag = isFarming();
			if (flag) {
				if (rigAngle < down) {
					rigAngle += 0.1f;
					if (rigAngle > down) {
						rigAngle = down;
					}
				} else {
					farmAngle = (float) ((farmAngle + 0.15f) % 6.283185307179586);
				}
			} else if (rigAngle > up) {
				rigAngle -= 0.075f;
				if (rigAngle < up) {
					rigAngle = up;
				}
			}
		}
	}

	@Override
	public boolean haveSupplies() {
		for (int i = 0; i < getInventorySize(); ++i) {
			@Nonnull
			ItemStack item = getStack(i);
			if (!item.isEmpty() && isSeedValidHandler(item)) {
				return true;
			}
		}
		return false;
	}
}
