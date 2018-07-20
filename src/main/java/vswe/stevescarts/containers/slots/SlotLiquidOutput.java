package vswe.stevescarts.containers.slots;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import vswe.stevescarts.helpers.storages.TransferHandler;

import javax.annotation.Nonnull;

public class SlotLiquidOutput extends SlotBase implements ISpecialItemTransferValidator {
	public SlotLiquidOutput(final IInventory iinventory, final int i, final int j, final int k) {
		super(iinventory, i, j, k);
	}

	@Override
	public boolean isItemValid(@Nonnull ItemStack itemstack) {
		return isItemStackValid(itemstack);
	}

	@Override
	public boolean isItemValidForTransfer(@Nonnull ItemStack item, final TransferHandler.TRANSFER_TYPE type) {
		return type == TransferHandler.TRANSFER_TYPE.OTHER && FluidUtil.getFluidHandler(item) != null;
	}

	public static boolean isItemStackValid(@Nonnull ItemStack itemstack) {
		return false;
	}
}
