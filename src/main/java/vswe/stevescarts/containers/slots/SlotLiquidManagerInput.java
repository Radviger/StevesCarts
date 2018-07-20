package vswe.stevescarts.containers.slots;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import vswe.stevescarts.blocks.tileentities.TileEntityLiquid;
import vswe.stevescarts.helpers.storages.SCTank;

import javax.annotation.Nonnull;

public class SlotLiquidManagerInput extends SlotBase {
	private TileEntityLiquid manager;
	private int tankid;

	public SlotLiquidManagerInput(final TileEntityLiquid manager, final int tankid, final int i, final int j, final int k) {
		super(manager, i, j, k);
		this.manager = manager;
		this.tankid = tankid;
	}

	@Override
	public boolean isItemValid(@Nonnull ItemStack itemstack) {
		return isItemStackValid(itemstack, manager, tankid);
	}

	public static boolean isItemStackValid(@Nonnull ItemStack itemstack, final TileEntityLiquid manager, final int tankid) {
		IFluidHandler handler = FluidUtil.getFluidHandler(itemstack.copy());
		if (handler == null) return false;
		if (tankid < 0 || tankid >= 4) return true;

		final SCTank tank = manager.getTanks()[tankid];

		FluidStack fluidStack = handler.drain(Fluid.BUCKET_VOLUME, false);
		return ((fluidStack == null || fluidStack.amount <= 0) && tank.getFluid() != null) ||
				((fluidStack != null && fluidStack.amount > 0) && (tank.getFluid() == null || tank.getFluid().isFluidEqual(fluidStack)));
	}
}
