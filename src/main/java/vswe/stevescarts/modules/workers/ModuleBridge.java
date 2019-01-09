package vswe.stevescarts.modules.workers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockRailBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotBridge;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.modules.ISuppliesModule;

import javax.annotation.Nonnull;

public class ModuleBridge extends ModuleWorker implements ISuppliesModule {
	private DataParameter<Boolean> BRIDGE;

	public ModuleBridge(final EntityMinecartModular cart) {
		super(cart);
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public int guiWidth() {
		return 80;
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotBridge(getCart(), slotId, 8 + x * 18, 23 + y * 18);
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public byte getWorkPriority() {
		return 98;
	}

	@Override
	public WorkResult work() {
		EntityMinecartModular cart = getCart();
		World world = cart.world;
		BlockPos next = getNextblock();
		if (cart.getYTarget() < next.getY()) {
			next = next.down(2);
		} else if (cart.getYTarget() == next.getY()){
			next = next.down(1);
		}
		if (!BlockRailBase.isRailBlock(world, next) && !BlockRailBase.isRailBlock(world, next.down())) {
			if (doPreWork()) {
				WorkResult r = tryBuildBridge(world, next, true);
				if (r == WorkResult.SUCCESS) {
					startWorking(22);
					setBridge(true);
					return WorkResult.SUCCESS;
				} else if (r == WorkResult.FAILURE) {
					return r;
				}
			} else {
				WorkResult r = tryBuildBridge(world, next, false);
				if (r != WorkResult.SKIP) { //TODO: SUCCESS ONLY?
					stopWorking();
				}
			}
		}
		setBridge(false);
		return WorkResult.SKIP;
	}

	private WorkResult tryBuildBridge(World world, BlockPos pos, final boolean simulate) {
		final Block b = world.getBlockState(pos).getBlock();
		if ((countsAsAir(pos) || b instanceof BlockLiquid) && isValidForTrack(pos.up(), false)) {
			FakePlayer player = getCartOwner();
			EntityMinecartModular cart = getCart();
			for (int m = 0; m < getInventorySize(); ++m) {
				ItemStack stack = getStack(m);
				if (!stack.isEmpty() && SlotBridge.isBridgeMaterial(stack)) {
					if (!simulate) {
						Block block = Block.getBlockFromItem(stack.getItem());
						//IBlockState state = block.getStateFromMeta(stack.getItemDamage());
						EnumActionResult e = ForgeHooks.onPlaceItemIntoWorld(stack, player, world, pos, EnumFacing.DOWN, 0F, 0F, 0F, EnumHand.MAIN_HAND);
						if (e != EnumActionResult.FAIL) {
							//world.setBlockState(pos, state);
							if (e == EnumActionResult.SUCCESS) {
								if (!cart.hasCreativeSupplies()) {
									/*stack.shrink(1);
									if (stack.getCount() == 0) {
										setStack(l, ItemStack.EMPTY);
									}*/
									cart.markDirty();
								}
							}
						} else {
							return WorkResult.FAILURE;
						}
					}
					return WorkResult.SUCCESS;
				}
			}
			if (isValidForTrack(pos, true) || isValidForTrack(pos.up(), true) || !isValidForTrack(pos.up(2), true)) {
			}
		}
		return WorkResult.SKIP;
	}

	@Override
	public void initDw() {
		BRIDGE = createDw(DataSerializers.BOOLEAN);
		registerDw(BRIDGE, false);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	private void setBridge(final boolean val) {
		updateDw(BRIDGE, val);
	}

	public boolean needBridge() {
		if (isPlaceholder()) {
			return getSimInfo().getNeedBridge();
		}
		return getDw(BRIDGE);
	}

	@Override
	public boolean haveSupplies() {
		for (int i = 0; i < getInventorySize(); ++i) {
			@Nonnull
			ItemStack item = getStack(i);
			if (!item.isEmpty() && SlotBridge.isBridgeMaterial(item)) {
				return true;
			}
		}
		return false;
	}
}
