package vswe.stevescarts.modules.workers;

import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.workers.tools.ModuleFarmer;

public class ModuleHydrater extends ModuleWorker {
	private int range;

	public ModuleHydrater(final EntityMinecartModular cart) {
		super(cart);
		range = 1;
	}

	@Override
	public byte getWorkPriority() {
		return 82;
	}

	@Override
	public void init() {
		super.init();
		for (final ModuleBase module : getCart().getModules()) {
			if (module instanceof ModuleFarmer) {
				range = ((ModuleFarmer) module).getExternalRange();
				break;
			}
		}
	}

	@Override
	public WorkResult work() {
		World world = getCart().world;
		BlockPos next = getNextblock();
		for (int i = -range; i <= range; ++i) {
			for (int j = -range; j <= range; ++j) {
				WorkResult r = hydrate(world, next.add(i, -1, j));
				if (r != WorkResult.SKIP) {
					return r;
				}
			}
		}
		return WorkResult.SKIP;
	}

	private WorkResult hydrate(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock() == Blocks.FARMLAND) {
			int moisture = state.getValue(BlockFarmland.MOISTURE);
			if (moisture != 7) {
				int waterCost = 7 - moisture;
				waterCost = getCart().drain(FluidRegistry.WATER, waterCost, false);
				if (waterCost > 0) {
					if (doPreWork()) {
						startWorking(2 + waterCost);
						return WorkResult.SUCCESS;
					}
					stopWorking();
					getCart().drain(FluidRegistry.WATER, waterCost, true);
					world.setBlockState(pos, state.withProperty(BlockFarmland.MOISTURE, moisture + waterCost), 3);
				}
			}
		}
		return WorkResult.SKIP;
	}
}
