package vswe.stevescarts.modules.workers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.IFluidBlock;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.modules.ModuleBase;

public abstract class ModuleWorker extends ModuleBase {
	private boolean preWork;
	private boolean shouldDie;

	public ModuleWorker(final EntityMinecartModular cart) {
		super(cart);
		preWork = true;
	}

	public abstract byte getWorkPriority();

	public abstract WorkResult work();

	public void startWorking(final int time) {
		getCart().setWorkingTime(time);
		preWork = false;
		getCart().setWorker(this);
	}

	public void stopWorking() {
		if (getCart().getWorker() == this) {
			preWork = true;
			getCart().setWorker(null);
		}
	}

	public boolean preventAutoShutdown() {
		return false;
	}

	public void kill() {
		shouldDie = true;
	}

	public boolean isDead() {
		return shouldDie;
	}

	public void revive() {
		shouldDie = false;
	}

	protected boolean doPreWork() {
		return preWork;
	}

	public BlockPos getLastblock() {
		return getNextblock(false);
	}

	public BlockPos getNextblock() {
		return getNextblock(true);
	}

	private BlockPos getNextblock(final boolean flag) {
		BlockPos pos = getCart().getPosition();
		if (BlockRailBase.isRailBlock(getCart().world, pos.down())) {
			pos = pos.down();
		}
		IBlockState blockState = getCart().world.getBlockState(pos);
		if (BlockRailBase.isRailBlock(blockState)) {
			BlockRailBase.EnumRailDirection direction = ((BlockRailBase) blockState.getBlock()).getRailDirection(getCart().world, pos, blockState, getCart());
			if (direction.isAscending()) {
				pos = pos.up();
			}

			int[][] logic = EntityMinecartModular.railDirectionCoordinates[direction.getMetadata()];
			double pX = getCart().pushX;
			double pZ = getCart().pushZ;
			boolean xDir = (pX > 0.0 && logic[0][0] > 0) || pX == 0.0 || logic[0][0] == 0 || (pX < 0.0 && logic[0][0] < 0);
			boolean zDir = (pZ > 0.0 && logic[0][2] > 0) || pZ == 0.0 || logic[0][2] == 0 || (pZ < 0.0 && logic[0][2] < 0);
			int dir = ((xDir && zDir) != flag) ? 1 : 0;
			return pos.add(logic[dir][0], logic[dir][1], logic[dir][2]);
		}
		return pos;
	}

	@Override
	public float getMaxSpeed() {
		if (!doPreWork()) {
			return 0.0f;
		}
		return super.getMaxSpeed();
	}

	protected boolean isValidForTrack(BlockPos pos, boolean flag) {
		boolean result = countsAsAir(pos) && (!flag || getCart().world.isSideSolid(pos.down(), EnumFacing.UP));
		if (result) {
			int coordX = pos.getX() - (getCart().x() - pos.getX());
			int coordZ = pos.getZ() - (getCart().z() - pos.getZ());
			Block block = getCart().world.getBlockState(new BlockPos(coordX, pos.getY(), coordZ)).getBlock();
			boolean isWater = block == Blocks.WATER || block == Blocks.FLOWING_WATER || block == Blocks.ICE;
			boolean isLava = block == Blocks.LAVA || block == Blocks.FLOWING_LAVA;
			boolean isOther = block instanceof IFluidBlock;
			boolean isLiquid = isWater || isLava || isOther;
			result = !isLiquid;
		}
		return result;
	}

	public enum WorkResult {
		SUCCESS,
		FAILURE,
		SKIP
	}
}
