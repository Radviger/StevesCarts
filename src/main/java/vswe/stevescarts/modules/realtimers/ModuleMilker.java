package vswe.stevescarts.modules.realtimers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotMilker;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.modules.ModuleBase;

import javax.annotation.Nonnull;

public class ModuleMilker extends ModuleBase {
	int cooldown;
	int milkbuffer;

	public ModuleMilker(final EntityMinecartModular cart) {
		super(cart);
		cooldown = 0;
		milkbuffer = 0;
	}

	@Override
	public void update() {
		super.update();
		if (cooldown <= 0) {
			if (!getCart().world.isRemote && getCart().hasFuel()) {
				generateMilk();
				depositeMilk();
			}
			cooldown = 20;
		} else {
			--cooldown;
		}
	}

	private void depositeMilk() {
		if (milkbuffer > 0) {
			final FluidStack ret = FluidUtil.getFluidContained(new ItemStack(Items.MILK_BUCKET));
			if (ret != null) {
				ret.amount = milkbuffer;
				milkbuffer -= getCart().fill(ret, true);
			}
			if (milkbuffer == 1000) {
				for (int i = 0; i < getInventorySize(); ++i) {
					@Nonnull
					ItemStack bucket = getStack(i);
					if (!bucket.isEmpty() && bucket.getItem() == Items.BUCKET) {
						@Nonnull
						ItemStack milk = new ItemStack(Items.MILK_BUCKET);
						getCart().addItemToChest(milk);
						if (milk.getCount() <= 0) {
							milkbuffer = 0;
							@Nonnull
							ItemStack itemStack = bucket;
							itemStack.shrink(1);
							if (itemStack.getCount() <= 0) {
								setStack(i, ItemStack.EMPTY);
							}
						}
					}
				}
			}
		}
	}

	private void generateMilk() {
		if (milkbuffer < 1000) {
			if (!getCart().getPassengers().isEmpty()) {
				final Entity rider = getCart().getPassengers().get(0);
				if (rider != null && rider instanceof EntityCow) {
					milkbuffer = Math.min(milkbuffer + 75, 1000);
				}
			}
		}
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	protected int getInventoryWidth() {
		return 2;
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotMilker(getCart(), slotId, 8 + x * 18, 23 + y * 18);
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setShort(generateNBTName("Milk", id), (short) milkbuffer);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		milkbuffer = tagCompound.getShort(generateNBTName("Milk", id));
	}
}
