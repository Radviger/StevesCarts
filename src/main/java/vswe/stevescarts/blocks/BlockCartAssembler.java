package vswe.stevescarts.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import vswe.stevescarts.StevesCarts;
import vswe.stevescarts.blocks.tileentities.TileEntityCartAssembler;
import vswe.stevescarts.blocks.tileentities.TileEntityUpgrade;
import vswe.stevescarts.helpers.Pair;
import vswe.stevescarts.packet.PacketStevesCarts;

import java.util.ArrayList;

public class BlockCartAssembler extends BlockContainerBase {

	public BlockCartAssembler() {
		super(Material.ROCK);
		setCreativeTab(StevesCarts.tabsSC2Blocks);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (entityplayer.isSneaking()) {
			return false;
		}
		final TileEntityCartAssembler assembler = (TileEntityCartAssembler) world.getTileEntity(pos);
		if (assembler != null) {
			if (!world.isRemote) {
				entityplayer.openGui(StevesCarts.instance, 3, world, pos.getX(), pos.getY(), pos.getZ());
			}
			return true;
		}
		return false;
	}

	public void updateMultiBlock(final World world, final BlockPos pos) {
		if(!(world.getTileEntity(pos) instanceof TileEntityCartAssembler)){
			return;
		}
		final TileEntityCartAssembler master = (TileEntityCartAssembler) world.getTileEntity(pos);
		if (master != null) {
			master.clearUpgrades();
		}
		checkForUpgrades(world, pos);
		if (!world.isRemote) {
			PacketStevesCarts.sendBlockInfoToClients(world, new byte[0], pos);
		}
		if (master != null) {
			master.onUpgradeUpdate();
		}
	}

	private void checkForUpgrades(final World world, final BlockPos pos) {
		BlockPos.PooledMutableBlockPos blockPos = BlockPos.PooledMutableBlockPos.retain(pos.getX(), pos.getY(), pos.getZ());
		for (EnumFacing facing : EnumFacing.VALUES) {
			blockPos.setPos(pos.getX(), pos.getY(), pos.getZ());
			checkForUpgrade(world, blockPos.move(facing));
		}
		blockPos.release();
	}

	private TileEntityCartAssembler checkForUpgrade(final World world, final BlockPos pos) {
		final TileEntity tile = world.getTileEntity(pos);
		if (tile != null && tile instanceof TileEntityUpgrade) {
			final TileEntityUpgrade upgrade = (TileEntityUpgrade) tile;
			final ArrayList<Pair<TileEntityCartAssembler, EnumFacing>> masters = getMasters(world, pos);
			if (masters.size() == 1) {
				Pair<TileEntityCartAssembler, EnumFacing> pair = masters.get(0);
				TileEntityCartAssembler master = pair.first();
				master.addUpgrade(upgrade);
				upgrade.setMaster(master, pair.second().getOpposite());
				return master;
			} else {
				world.markChunkDirty(pos, tile);
			}
			for (final Pair<TileEntityCartAssembler, EnumFacing> master2 : masters) {
				master2.first().removeUpgrade(upgrade);
			}
			upgrade.setMaster(null, null);
		}
		return null;
	}

	private ArrayList<Pair<TileEntityCartAssembler, EnumFacing>> getMasters(final World world, final BlockPos pos) {
		final ArrayList<Pair<TileEntityCartAssembler, EnumFacing>> masters = new ArrayList<>();
		for (EnumFacing facing: EnumFacing.VALUES) {
			final TileEntityCartAssembler temp = getMaster(world, pos.offset(facing));
			if (temp != null) {
				masters.add(Pair.of(temp, facing));
			}
		}
		return masters;
	}

	private TileEntityCartAssembler getValidMaster(final World world, final BlockPos pos) {
		TileEntityCartAssembler master = null;
		for (EnumFacing facing: EnumFacing.VALUES) {
			final TileEntityCartAssembler temp = getMaster(world, pos.offset(facing));
			if (temp != null) {
				if (master != null) {
					return null;
				}
				master = temp;
			}
		}
		return master;
	}

	private TileEntityCartAssembler getMaster(final World world, final BlockPos pos) {
		final TileEntity tile = world.getTileEntity(pos);
		if (tile != null && tile instanceof TileEntityCartAssembler) {
			final TileEntityCartAssembler master = (TileEntityCartAssembler) tile;
			if (!master.isDead) {
				return master;
			}
		}
		return null;
	}

	public void addUpgrade(final World world, final BlockPos pos) {
		final TileEntityCartAssembler master = getValidMaster(world, pos);
		if (master != null) {
			updateMultiBlock(world, master.getPos());
		}
	}

	public void removeUpgrade(final World world, final BlockPos pos) {
		final TileEntityCartAssembler master = getValidMaster(world, pos);
		if (master != null) {
			updateMultiBlock(world, pos);
		}
	}

	@Override
	public TileEntity createNewTileEntity(final World world, final int var2) {
		return new TileEntityCartAssembler();
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		super.onBlockAdded(worldIn, pos, state);
		updateMultiBlock(worldIn, pos);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		final TileEntityCartAssembler tile = (TileEntityCartAssembler) world.getTileEntity(pos);
		tile.isDead = true;
		updateMultiBlock(world, pos);
		if (!tile.isEmpty()) {
			InventoryHelper.dropInventoryItems(world, pos, tile);
			ItemStack outputItem = tile.getOutputOnInterupt();
			if (!outputItem.isEmpty()) {
				final EntityItem eItem = new EntityItem(world, pos.getX() + 0.20000000298023224, pos.getY() + 0.20000000298023224, pos.getZ() + 0.2f, outputItem);
				eItem.motionX = (float) world.rand.nextGaussian() * 0.05f;
				eItem.motionY = (float) world.rand.nextGaussian() * 0.25f;
				eItem.motionZ = (float) world.rand.nextGaussian() * 0.05f;
				if (outputItem.hasTagCompound()) {
					eItem.getItem().setTagCompound(outputItem.getTagCompound().copy());
				}
				world.spawnEntity(eItem);
			}
		}
		super.breakBlock(world, pos, getDefaultState());
	}

}
