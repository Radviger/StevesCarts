package vswe.stevescarts.modules.workers.tools;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks.EnumType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import vswe.stevescarts.api.farms.EnumHarvestResult;
import vswe.stevescarts.api.farms.ITreeModule;
import vswe.stevescarts.api.farms.ITreeProduceModule;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotFuel;
import vswe.stevescarts.containers.slots.SlotSapling;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.BlockPosHelpers;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.modules.ISuppliesModule;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.addons.plants.ModulePlantSize;
import vswe.stevescarts.plugins.APIHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ModuleWoodcutter extends ModuleTool implements ISuppliesModule {
	private ArrayList<ITreeModule> treeModules;
	private ModulePlantSize plantSize;
	private boolean isPlanting;
	private float cutterAngle;
	private DataParameter<Boolean> IS_CUTTING;

	public ModuleWoodcutter(final EntityMinecartModular cart) {
		super(cart);
		cutterAngle = 0.7853982f;
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
		drawString(gui, Localization.MODULES.TOOLS.CUTTER.translate(), 8, 6, 4210752);
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
		return new SlotSapling(getCart(), this, slotId, 8 + x * 18, 28 + y * 18);
	}

	@Override
	public boolean useDurability() {
		return true;
	}

	@Override
	public void init() {
		super.init();
		treeModules = new ArrayList<>();
		for (final ModuleBase module : getCart().getModules()) {
			if (module instanceof ITreeModule) {
				treeModules.add((ITreeModule) module);
			} else {
				if (!(module instanceof ModulePlantSize)) {
					continue;
				}
				plantSize = (ModulePlantSize) module;
			}
		}
		treeModules.addAll(APIHelper.treeModules);
	}

	public abstract int getPercentageDropChance();

	public NonNullList<ItemStack> getTierDrop(List<ItemStack> baseItems) {
		NonNullList<ItemStack> nerfedItems = NonNullList.create();
		for (@Nonnull ItemStack item : baseItems) {
			if (!item.isEmpty()) {
				dropItemByMultiplierChance(nerfedItems, item, getPercentageDropChance());
			}
		}
		return nerfedItems;
	}

	private void dropItemByMultiplierChance(List<ItemStack> items, @Nonnull ItemStack item, int percentage) {
		int drop = 0;
		while (percentage > 0) {
			if (getCart().rand.nextInt(100) < percentage) {
				items.add(item.copy());
				++drop;
			}
			percentage -= 100;
		}
	}

	@Override
	public WorkResult work() {
		World world = getCart().world;
		BlockPos next = getNextblock();
		final int size = getPlantSize();
		destroyLeaveBlockOnTrack(world, next);
		destroyLeaveBlockOnTrack(world, next.up());
		for (int i = -size; i <= size; ++i) {
			if (i != 0) {
				int i2 = i;
				if (i2 < 0) {
					i2 = -size - i2 - 1;
				}
				BlockPos plant = next.add(((getCart().z() != next.getZ()) ? i2 : 0), -1, ((getCart().x() != next.getX()) ? i2 : 0));
				WorkResult r = plant(size, plant, next.getX(), next.getZ());
				switch (r) {
					case SUCCESS:
						setCutting(false);
						return WorkResult.SUCCESS;
					case FAILURE:
						return r;
				}
			}
		}
		if (!isPlanting) {
			for (int i = -1; i <= 1; ++i) {
				for (int j = -1; j <= 1; ++j) {
					BlockPos farm = next.add(i, -1, j);
					WorkResult r = farm(world, farm);
					switch (r) {
						case SUCCESS:
							setCutting(true);
							return WorkResult.SUCCESS;
						case FAILURE:
							return r;
					}
				}
			}
		}
		setCutting(isPlanting = false);
		return WorkResult.SKIP;
	}

	private WorkResult plant(final int size, BlockPos pos, final int cx, final int cz) {
		if (size == 1) {
			if ((pos.getX() + pos.getZ()) % 2 == 0) {
				return WorkResult.SKIP;
			}
		} else if ((pos.getX() == cx && pos.getX() / size % 2 == 0) || (pos.getZ() == cz && pos.getZ() / size % 2 == 0)) {
			return WorkResult.SKIP;
		}
		int saplingSlotId = -1;
		ItemStack sapling = ItemStack.EMPTY;
		for (int i = 0; i < getInventorySize(); ++i) {
			final SlotBase slot = getSlots().get(i);
			if (slot.containsValidItem()) {
				saplingSlotId = i;
				sapling = getStack(i);
				break;
			}
		}
		if (!sapling.isEmpty()) {
			if (doPreWork()) {
				for (ITreeModule module : treeModules) {
					if (module.isSapling(sapling)) {
						if (module.plantSapling(getCart().world, pos, sapling, getCartOwner())) {
							sapling.shrink(1);
							if (sapling.getCount() == 0) {
								setStack(saplingSlotId, ItemStack.EMPTY);
							}
							startWorking(25);
							isPlanting = true;
							return WorkResult.SUCCESS;
						}
					}
				}
				stopWorking();
				isPlanting = false;
			} else {
				stopWorking();
				isPlanting = false;
			}
		}
		return WorkResult.SKIP;
	}

	private WorkResult farm(World world, BlockPos pos) {
		if (!isBroken()) {
			pos = pos.up();
			IBlockState state = world.getBlockState(pos);
			if (isWoodHandler(state, pos)) {
				NonNullList<ItemStack> drops = NonNullList.create();
				ITreeProduceModule produceModule = getProduceHandler(state, pos, drops, false); //TODO: ADD EVENTS FOR THIS
				if (produceModule != null) {
					for (ItemStack stack : drops) {
						getCart().addItemToChest(stack);
					}
					return drops.size() > 0 ? WorkResult.SUCCESS : WorkResult.SKIP;
				} else {
					final List<BlockPos> checked = new ArrayList<>();
					WorkResult r = removeAt(world, pos, checked);
					if (r != WorkResult.SKIP) {
						return r;
					}
				}
				stopWorking();
			}
		}
		return WorkResult.SKIP;
	}

	private WorkResult removeAt(World world, BlockPos here, final List<BlockPos> checked) {
		checked.add(here);
		IBlockState state = world.getBlockState(here);
		final Block block = state.getBlock();
		if (block == Blocks.AIR) {
			return WorkResult.SKIP;
		}
		if (checked.size() < 125 && BlockPosHelpers.getHorizontalDistToCartSquared(here, getCart()) < 175.0) {
			for (int type = 0; type < 2; ++type) {
				boolean hitWood = false;
				if (isLeavesHandler(state, here)) {
					type = 1;
				} else if (type == 1) {
					hitWood = true;
				}
				for (int x = -1; x <= 1; ++x) {
					for (int y = 1; y >= 0; --y) {
						for (int z = -1; z <= 1; ++z) {
							BlockPos pos = here.add(x, y, z);
							IBlockState currentState = world.getBlockState(pos);
                            if (hitWood) {
                                if (!isWoodHandler(currentState, pos)) {
                                    continue;
                                }
                            } else if (!isLeavesHandler(currentState, pos)) {
                                continue;
                            }
                            if (!checked.contains(pos)) {
                                return removeAt(world, pos, checked);
                            }
                        }
					}
				}
			}
		}
        FakePlayer player = getCartOwner();
        BlockEvent e = new BlockEvent.BreakEvent(world, here, state, player);
        if (!MinecraftForge.EVENT_BUS.post(e)) {
            NonNullList<ItemStack> stuff = NonNullList.create();
            if (shouldSilkTouch(state, here)) {
                ItemStack stack = getSilkTouchedItem(state);
                if (!stack.isEmpty()) {
                    stuff.add(stack);
                }
            } else {
                final int fortune = (enchanter != null) ? enchanter.getFortuneLevel() : 0;
                block.getDrops(stuff, world, here, state, fortune);
                List<ItemStack> drops = new ArrayList<>();
                BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(world, here, state, fortune, 1, drops, player, false);
                MinecraftForge.EVENT_BUS.post(event);
                for (ItemStack drop : drops) { //Here to filter out any bad itemstacks, the mod I was testing with returned stacks with a size of 0
                    if (!drop.isEmpty() && drop.getCount() > 0) {
                        stuff.add(drop);
                    }
                }

                int applerand = 200;
                if (fortune > 0) {
                    applerand -= 10 << fortune;
                    if (applerand < 40) {
                        applerand = 40;
                    }
                }
                if (block == Blocks.LEAVES && state.getValue(BlockOldLeaf.VARIANT) == EnumType.OAK && getCart().rand.nextInt(applerand) == 0) {
                    stuff.add(new ItemStack(Items.APPLE, 1, 0));
                }
            }
            List<ItemStack> nerfedstuff = getTierDrop(stuff);
            boolean first = true;
            for (ItemStack stack : nerfedstuff) {
                getCart().addItemToChest(stack, Slot.class, SlotFuel.class);
                if (stack.getCount() != 0) {
                    if (first) { //TODO: WTF?
                        return WorkResult.SKIP;
                    }
                    final EntityItem item = new EntityItem(world, getCart().posX, getCart().posY, getCart().posZ, stack);
                    item.motionX = (here.getX() - getCart().x()) / 10.0f;
                    item.motionY = 0.15000000596046448;
                    item.motionZ = (here.getZ() - getCart().z()) / 10.0f;
                    world.spawnEntity(item);
                }
                first = false;
            }
            world.playEvent(2001, here, Block.getStateId(state));
            world.setBlockToAir(here);
            int basetime;
            if (isLeavesHandler(state, here)) {
                basetime = 2;
                damageTool(1);
            } else {
                basetime = 25;
                damageTool(5);
            }
            final int efficiency = (enchanter != null) ? enchanter.getEfficiencyLevel() : 0;
            startWorking((int) (basetime / Math.pow(1.2999999523162842, efficiency)));
            return WorkResult.SUCCESS;
        } else {
            return WorkResult.FAILURE;
        }
	}

	@Override
	public void initDw() {
		IS_CUTTING = createDw(DataSerializers.BOOLEAN);
		registerDw(IS_CUTTING, false);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	private void setCutting(final boolean val) {
		updateDw(IS_CUTTING, val);
	}

	protected boolean isCutting() {
		if (isPlaceholder()) {
			return getSimInfo().getIsCutting();
		}
		return getDw(IS_CUTTING);
	}

	public float getCutterAngle() {
		return cutterAngle;
	}

	@Override
	public void update() {
		super.update();
		final boolean cuttingflag = isCutting();
		if (cuttingflag || cutterAngle != 0.7853982f) {
			boolean flag = false;
			if (!cuttingflag && cutterAngle < 0.7853982f) {
				flag = true;
			}
			cutterAngle = (float) ((cutterAngle + 0.9f) % 6.283185307179586);
			if (!cuttingflag && cutterAngle > 0.7853982f && flag) {
				cutterAngle = 0.7853982f;
			}
		}
	}

	@Override
	public boolean haveSupplies() {
		for (int i = 0; i < getInventorySize(); ++i) {
			if (getSlots().get(i).containsValidItem()) {
				return true;
			}
		}
		return false;
	}

	public boolean isLeavesHandler(IBlockState blockState, BlockPos pos) {
		for (final ITreeModule module : treeModules) {
			EnumHarvestResult result = module.isLeaves(blockState, pos, getCart());
			if (result == EnumHarvestResult.ALLOW) {
				return true;
			} else if (result == EnumHarvestResult.DISALLOW) {
				return false;
			}
		}
		return false;
	}

	public boolean isWoodHandler(IBlockState blockState, BlockPos pos) {
		for (final ITreeModule module : treeModules) {
			EnumHarvestResult result = module.isWood(blockState, pos, getCart());
			if (result == EnumHarvestResult.ALLOW) {
				return true;
			} else if (result == EnumHarvestResult.DISALLOW) {
				return false;
			}
		}
		return false;
	}

	@Nullable
	public ITreeProduceModule getProduceHandler(IBlockState blockState, BlockPos pos, NonNullList<ItemStack> drops, boolean simulate) {
		for (final ITreeModule module : treeModules) {
			if (module instanceof ITreeProduceModule) {
				if (((ITreeProduceModule) module).harvest(blockState, pos, getCart(), drops, simulate, this)) {
					return (ITreeProduceModule) module;
				}
			}
		}
		return null;
	}

	public boolean isSaplingHandler(@Nonnull ItemStack sapling) {
		for (final ITreeModule module : treeModules) {
			if (module.isSapling(sapling)) {
				return true;
			}
		}
		return false;
	}

	private int getPlantSize() {
		if (plantSize != null) {
			return plantSize.getSize();
		}
		return 1;
	}

	private void destroyLeaveBlockOnTrack(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (isLeavesHandler(state, pos)) {
			world.setBlockToAir(pos);
		}
	}
}
