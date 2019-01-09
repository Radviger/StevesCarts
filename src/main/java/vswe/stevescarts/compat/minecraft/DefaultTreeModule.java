package vswe.stevescarts.compat.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.oredict.OreDictionary;
import vswe.stevescarts.api.farms.EnumHarvestResult;
import vswe.stevescarts.api.farms.ITreeModule;
import vswe.stevescarts.entitys.EntityMinecartModular;

import javax.annotation.Nonnull;

public class DefaultTreeModule implements ITreeModule {

	@Override
	public EnumHarvestResult isLeaves(IBlockState blockState, BlockPos pos, EntityMinecartModular cart) {
		return blockState.getBlock().isLeaves(blockState, cart.world, pos) ? EnumHarvestResult.ALLOW : EnumHarvestResult.SKIP;
	}

	@Override
	public EnumHarvestResult isWood(IBlockState blockState, BlockPos pos, EntityMinecartModular cart) {
		return blockState.getBlock().isWood(cart.world, pos) ? EnumHarvestResult.ALLOW : EnumHarvestResult.SKIP;
	}

	@Override
	public boolean isSapling(@Nonnull ItemStack sapling) {
		if (!sapling.isEmpty()) {
			if (isStackSapling(sapling)) {
				return true;
			}
			if (sapling.getItem() instanceof ItemBlock) {
				final Block b = Block.getBlockFromItem(sapling.getItem());
				return b instanceof BlockSapling || isStackSapling(new ItemStack(b, 1, 32767));
			}
		}
		return false;
	}

	@Override
	public boolean plantSapling(World world, BlockPos pos, ItemStack stack, FakePlayer fakePlayer) {
		if (stack.isEmpty()) {
			return false;
		}
		Block block = Block.getBlockFromItem(stack.getItem());
		if (block instanceof BlockSapling) {
			BlockSapling blockSapling = (BlockSapling) block;
			if (blockSapling.canPlaceBlockAt(world, pos.up())) {
				world.setBlockState(pos.up(), blockSapling.getStateFromMeta(stack.getItemDamage()));
				return true;
			}
		}
		return false;
	}

	private boolean isStackSapling(@Nonnull ItemStack sapling) {
		final int[] ids = OreDictionary.getOreIDs(sapling);
		for (int id : ids) {
			final String name = OreDictionary.getOreName(id);
			if (name != null && name.startsWith("treeSapling")) {
				return true;
			}
		}
		return false;
	}
}
