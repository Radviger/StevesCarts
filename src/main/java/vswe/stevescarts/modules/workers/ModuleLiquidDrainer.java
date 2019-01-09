package vswe.stevescarts.modules.workers;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.BlockPosHelpers;
import vswe.stevescarts.modules.workers.tools.ModuleDrill;

import java.util.ArrayList;
import java.util.List;

public class ModuleLiquidDrainer extends ModuleWorker {
	public ModuleLiquidDrainer(final EntityMinecartModular cart) {
		super(cart);
	}

	@Override
	public byte getWorkPriority() {
		return 0;
	}

	@Override
	public WorkResult work() {
		return WorkResult.SKIP;
	}

	public void handleLiquid(final ModuleDrill drill, BlockPos pos) {
		final List<BlockPos> checked = new ArrayList<>();
		final int result = drainAt(getCart().world, drill, checked, pos, 0);
		if (result > 0 && doPreWork()) {
			drill.kill();
			startWorking((int) (2.5f * result));
		} else {
			stopWorking();
		}
	}

	@Override
	public boolean preventAutoShutdown() {
		return true;
	}

	private int drainAt(World world, final ModuleDrill drill, final List<BlockPos> checked, final BlockPos pos, int buckets) {
		int drained = 0;
		IBlockState blockState = world.getBlockState(pos);
		final Block b = blockState.getBlock();
		if (!isLiquid(b)) {
			return 0;
		}
		final int meta = b.getMetaFromState(blockState);
		final FluidStack liquid = getFluidStack(b, pos, !doPreWork());
		if (liquid != null) {
			if (doPreWork()) {
				liquid.amount += buckets * 1000;
			}
			final int amount = getCart().fill(liquid, false);
			if (amount == liquid.amount) {
				final boolean isDrainable = meta == 0;
				if (!doPreWork()) {
					if (isDrainable) {
						getCart().fill(liquid, true);
					}
					world.setBlockToAir(pos);
				}
				drained += (isDrainable ? 40 : 3);
				buckets += (isDrainable ? 1 : 0);
			}
		}
		checked.add(pos);
		if (checked.size() < 100 && BlockPosHelpers.getHorizontalDistToCartSquared(pos, getCart()) < 200.0) {
			for (int y = 1; y >= 0; --y) {
				for (int x = -1; x <= 1; ++x) {
					for (int z = -1; z <= 1; ++z) {
						if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 1) {
							BlockPos next = pos.add(x, y, z);
							if (!checked.contains(next)) {
								drained += drainAt(world, drill, checked, next, buckets);
							}
						}
					}
				}
			}
		}
		return drained;
	}

	private boolean isLiquid(final Block b) {
		final boolean isWater = b == Blocks.WATER || b == Blocks.FLOWING_WATER || b == Blocks.ICE;
		final boolean isLava = b == Blocks.LAVA || b == Blocks.FLOWING_LAVA;
		final boolean isOther = b instanceof IFluidBlock;
		return isWater || isLava || isOther;
	}

	private FluidStack getFluidStack(final Block b, BlockPos pos, final boolean doDrain) {
		if (b == Blocks.WATER || b == Blocks.FLOWING_WATER) {
			return new FluidStack(FluidRegistry.WATER, 1000);
		}
		if (b == Blocks.LAVA || b == Blocks.FLOWING_LAVA) {
			return new FluidStack(FluidRegistry.LAVA, 1000);
		}
		if (b instanceof IFluidBlock) {
			final IFluidBlock liquid = (IFluidBlock) b;
			return liquid.drain(getCart().world, pos, doDrain);
		}
		return null;
	}
}
