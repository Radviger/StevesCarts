package vswe.stevescarts.modules.addons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.oredict.OreDictionary;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.modules.workers.tools.ModuleDrill;

import java.util.ArrayList;
import java.util.List;

public class ModuleOreTracker extends ModuleAddon {
	public ModuleOreTracker(final EntityMinecartModular cart) {
		super(cart);
	}

	public BlockPos findBlockToMine(final ModuleDrill drill, final BlockPos start) {
		return findBlockToMine(drill, new ArrayList<>(), start, true);
	}

	private BlockPos findBlockToMine(final ModuleDrill drill, final List<BlockPos> checked, final BlockPos current, final boolean first) {
		if (current == null || checked.contains(current) || (!first && !isOre(current))) {
			return null;
		}
		checked.add(current);
		if (checked.size() < 200) {
			for (int x = -1; x <= 1; ++x) {
				for (int y = -1; y <= 1; ++y) {
					for (int z = -1; z <= 1; ++z) {
						if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 1) {
							final BlockPos ret = findBlockToMine(drill, checked, current.add(x, y, z), false);
							if (ret != null) {
								return ret;
							}
						}
					}
				}
			}
		}
		if (first && !isOre(current)) {
			return null;
		}
		if (drill.isValidBlock(getCart().world, current, 0, 1, true) == null) {
			return null;
		}
		return current;
	}

	private boolean isOre(BlockPos pos) {
		IBlockState state = getCart().world.getBlockState(pos);
		Block b = state.getBlock();
		if (b == null || b == Blocks.AIR) {
			return false;
		}
		if (b instanceof BlockOre) {
			return true;
		}

		BlockPos fromPos = getCart().getPosition();
		int x = pos.getX() - fromPos.getX();
		int y = pos.getY() - fromPos.getY();
		int z = pos.getZ() - fromPos.getZ();
		EnumFacing facing = EnumFacing.getFacingFromVector(x, y, z);
		RayTraceResult hit = new RayTraceResult(new Vec3d(0,0,0), facing, pos);
		ItemStack stack = b.getPickBlock(state, hit, getCart().world, pos, getCartOwner());
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		final int[] oreIds = OreDictionary.getOreIDs(stack);
		if (oreIds.length == 0) {
			return false;
		}
		for (int oreId : oreIds) {
			final String oreName = OreDictionary.getOreName(oreId);
			if (oreName != null && oreName.startsWith("ore")) {
				return true;
			}
		}
		return false;
	}
}
