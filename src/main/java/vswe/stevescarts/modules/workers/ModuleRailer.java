package vswe.stevescarts.modules.workers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotBuilder;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.modules.ISuppliesModule;

import java.util.ArrayList;
import java.util.List;

public class ModuleRailer extends ModuleWorker implements ISuppliesModule {
	private boolean hasGeneratedAngles;
	private float[] railAngles;
	private DataParameter<Byte> RAILS;

	public ModuleRailer(final EntityMinecartModular cart) {
		super(cart);
		hasGeneratedAngles = false;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotBuilder(getCart(), slotId, 8 + x * 18, 23 + y * 18);
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, Localization.MODULES.ATTACHMENTS.RAILER.translate(), 8, 6, 4210752);
	}

	@Override
	public byte getWorkPriority() {
		return 100;
	}

	@Override
	public WorkResult work() {
		World world = getCart().world;
		BlockPos next = getNextblock();
		int x = next.getX();
		int y = next.getY();
		int z = next.getZ();
		final List<BlockPos> positions = getValidRailPositions(x, y, z);
		if (doPreWork()) {
			boolean valid = false;
			loop:
			for (BlockPos po : positions) {
				WorkResult r = tryPlaceTrack(po, true);
				switch (r) {
					case SUCCESS:
						valid = true;
						break loop;
					case FAILURE:
						return r;
				}
			}
			if (valid) {
				startWorking(12);
			} else {
				boolean front = false;
				for (BlockPos po : positions) {
					if (BlockRailBase.isRailBlock(world, po)) {
						front = true;
						break;
					}
				}
				if (!front) {
					turnback();
				}
			}
			return WorkResult.SUCCESS;
		}
		stopWorking();
		for (BlockPos p : positions) {
			WorkResult r = tryPlaceTrack(p, false);
			if (r == WorkResult.SUCCESS) {
				break;
			} else if (r == WorkResult.FAILURE) {
				return r;
			}
		}
		return WorkResult.SKIP;
	}

	protected List<BlockPos> getValidRailPositions(int x, int y, int z) {
		List<BlockPos> lst = new ArrayList<>();
		if (y >= getCart().y()) {
			lst.add(new BlockPos(x, y + 1, z));
		}
		lst.add(new BlockPos(x, y, z));
		lst.add(new BlockPos(x, y - 1, z));
		return lst;
	}

	protected static boolean isRail(Item item) {
		return Block.getBlockFromItem(item) instanceof BlockRailBase;
	}

	private WorkResult tryPlaceTrack(BlockPos pos, boolean simulate) {
		if (isValidForTrack(pos, true)) {
			EntityMinecartModular cart = getCart();
			FakePlayer player = getCartOwner();
			World world = cart.world;
			for (int l = 0; l < getInventorySize(); ++l) {
				ItemStack stack = getStack(l);
				Block block = Block.getBlockFromItem(stack.getItem());
				if (!stack.isEmpty() && isRail(stack.getItem())) {
					if (!simulate) {
						player.setHeldItem(EnumHand.MAIN_HAND, stack);
						player.capabilities.isCreativeMode = cart.hasCreativeSupplies();
						IBlockState state = block.getStateForPlacement(world, pos, EnumFacing.UP, 0, 0, 0, 0, player, EnumHand.MAIN_HAND);
						BlockSnapshot snapshot = new BlockSnapshot(player.world, pos, state);
						BlockEvent.PlaceEvent e = ForgeEventFactory.onPlayerBlockPlace(player, snapshot, EnumFacing.UP, EnumHand.MAIN_HAND);
						player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
						if (!e.isCanceled()) {
							world.setBlockState(pos, state);
							world.playEvent(2001, pos, Block.getStateId(state));
							if (!player.capabilities.isCreativeMode) {
								stack.shrink(1);
								if (stack.getCount() == 0) {
									setStack(l, ItemStack.EMPTY);
								}
								cart.markDirty();
							} else {
								player.capabilities.isCreativeMode = false;
							}
						} else {
							return WorkResult.FAILURE;
						}
					}
					return WorkResult.SUCCESS;
				}
			}
			turnback();
			return WorkResult.SUCCESS;
		}
		return WorkResult.SKIP;
	}

	@Override
	public void initDw() {
		RAILS = createDw(DataSerializers.BYTE);
		registerDw(RAILS, (byte) 0);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	public void onInventoryChanged() {
		super.onInventoryChanged();
		calculateRails();
	}

	private void calculateRails() {
		if (getCart().world.isRemote) {
			return;
		}
		byte valid = 0;
		for (int i = 0; i < getInventorySize(); ++i) {
			if (!getStack(i).isEmpty() && isRail(getStack(i).getItem())) {
				++valid;
			}
		}
		updateDw(RAILS, valid);
	}

	public int getRails() {
		if (isPlaceholder()) {
			return getSimInfo().getRailCount();
		}
		return getDw(RAILS);
	}

	public float getRailAngle(final int i) {
		if (!hasGeneratedAngles) {
			railAngles = new float[getInventorySize()];
			for (int j = 0; j < getInventorySize(); ++j) {
				railAngles[j] = getCart().rand.nextFloat() / 2.0f - 0.25f;
			}
			hasGeneratedAngles = true;
		}
		return railAngles[i];
	}

	@Override
	protected void Load(NBTTagCompound tagCompound, final int id) {
		calculateRails();
	}

	@Override
	public boolean haveSupplies() {
		for (int i = 0; i < getInventorySize(); ++i) {
			ItemStack item = getStack(i);
			if (!item.isEmpty() && isRail(item.getItem())) {
				return true;
			}
		}
		return false;
	}
}
