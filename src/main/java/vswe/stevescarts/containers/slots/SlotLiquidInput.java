package vswe.stevescarts.containers.slots;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import vswe.stevescarts.helpers.storages.SCTank;

import javax.annotation.Nonnull;

public class SlotLiquidInput extends SlotBase {
	private SCTank tank;
	private int maxsize;

	public SlotLiquidInput(final IInventory iinventory, final SCTank tank, final int maxsize, final int i, final int j, final int k) {
		super(iinventory, i, j, k);
		this.tank = tank;
		this.maxsize = maxsize;
	}

	@Override
	public int getSlotStackLimit() {
		if (maxsize != -1) {
			return maxsize;
		}
		return Math.min(8, tank.getCapacity() / Fluid.BUCKET_VOLUME);
	}

	@Override
	public boolean isItemValid(@Nonnull ItemStack itemstack) {
		IFluidHandler handler = FluidUtil.getFluidHandler(itemstack.copy());
		if (handler == null) return false;

		FluidStack fluidStack = handler.drain(Fluid.BUCKET_VOLUME, false);
		return ((fluidStack == null || fluidStack.amount <= 0) && tank.getFluid() != null) ||
				((fluidStack != null && fluidStack.amount > 0) && (tank.getFluid() == null || tank.getFluid().isFluidEqual(fluidStack)));
	}
}
