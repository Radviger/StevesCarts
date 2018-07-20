package vswe.stevescarts.helpers.storages;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.guis.GuiBase;
import vswe.stevescarts.helpers.Localization;

import javax.annotation.Nullable;
import java.text.NumberFormat;

public class SCTank extends FluidTank {
	private ITankHolder owner;
	private int tankid;
	private boolean isLocked;

	public SCTank(final ITankHolder owner, final int tankSize, final int tankid) {
		super(tankSize);
		this.owner = owner;
		this.tankid = tankid;
	}

	public SCTank copy() {
		final SCTank tank = new SCTank(owner, capacity, tankid);
		if (getFluid() != null) {
			tank.setFluid(getFluid().copy());
		}
		return tank;
	}

	public void containerTransfer() {
		ItemStack item = owner.getInputContainer(tankid);
		if (!item.isEmpty()) {
			IFluidHandler handler = FluidUtil.getFluidHandler(item.copy());
			if (handler != null && (this.fluid == null || this.capacity - this.fluid.amount >= Fluid.BUCKET_VOLUME)) {
				FluidStack fluidStack = handler.drain(Fluid.BUCKET_VOLUME, false);
				if (fluidStack != null && fluidStack.amount >= Fluid.BUCKET_VOLUME) {
					FluidActionResult result = FluidUtil.tryEmptyContainer(item, this, Fluid.BUCKET_VOLUME, null, false);
					if (result.isSuccess()) {
						ItemStack container = result.getResult();
						handler = FluidUtil.getFluidHandler(container.copy());
						if (handler != null) {
							fluidStack = handler.drain(Fluid.BUCKET_VOLUME, false);
							if (fluidStack != null && fluidStack.amount == Fluid.BUCKET_VOLUME) {
								FluidUtil.tryEmptyContainer(item, this, Fluid.BUCKET_VOLUME, null, true);
								owner.setInputContainer(tankid, container);
								return;
							}
						}
						if (!container.isEmpty()) {
							owner.addToOutputContainer(tankid, container);
						}
						if (container.getCount() == 0) {
							FluidUtil.tryEmptyContainer(item, this, Fluid.BUCKET_VOLUME, null, true);

							item.shrink(1);
							if (item.isEmpty() || item.getCount() <= 0)
								owner.setInputContainer(tankid, ItemStack.EMPTY);
						}
					}
				} else {
					FluidActionResult result = FluidUtil.tryFillContainer(item, this, Fluid.BUCKET_VOLUME, null, false);
					if (result.isSuccess()) {
						ItemStack container = result.getResult();
						if (!container.isEmpty()) {
							owner.addToOutputContainer(tankid, container);
							if (container.getCount() == 0) {
								FluidUtil.tryFillContainer(item, this, Fluid.BUCKET_VOLUME, null, true);

								item.shrink(1);
								if (item.isEmpty() || item.getCount() <= 0)
									owner.setInputContainer(tankid, ItemStack.EMPTY);
							}
						}
					}
				}
			}
		}
	}

	@Nullable
	@Override
	public FluidStack drainInternal(int maxDrain, boolean doDrain) {
		if (fluid == null || maxDrain <= 0) {
			return null;
		}

		int drained = maxDrain;
		if (fluid.amount < drained) {
			drained = fluid.amount;
		}

		FluidStack stack = new FluidStack(fluid, drained);
		if (doDrain) {
			fluid.amount -= drained;
			if (fluid.amount <= 0 && !isLocked) {
				fluid = null;
			}

			onContentsChanged();

			if (tile != null) {
				FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(fluid, tile.getWorld(), tile.getPos(), this, drained));
			}
		}
		return stack;
	}

	public void setLocked(final boolean val) {
		isLocked = val;
	}

	public boolean isLocked() {
		return isLocked;
	}

	public String getMouseOver() {
		String name = Localization.MODULES.TANKS.EMPTY.translate();
		int amount = 0;
		if (fluid != null) {
			name = fluid.getFluid().getLocalizedName(fluid);
			if (name.indexOf(".") != -1) {
				name = FluidRegistry.getFluidName(fluid);
			}
			if (name != null && !name.equals("")) {
				name = fluid.getFluid().getRarity().rarityColor.toString() + Character.toUpperCase(name.charAt(0)) + name.substring(1) + TextFormatting.WHITE;
			} else {
				name = Localization.MODULES.TANKS.INVALID.translate();
			}
			amount = fluid.amount;
		}
		NumberFormat format = NumberFormat.getInstance();
		return name + "\n" + format.format(amount) + " / " + format.format(capacity);
	}

	@Override
	protected void onContentsChanged() {
		owner.onFluidUpdated(tankid);
	}

	private static float getColorComponent(final int color, final int id) {
		return ((color & 255 << id * 8) >> id * 8) / 255.0f;
	}

	public static void applyColorFilter(FluidStack fluid) {
		int color = fluid.getFluid().getColor(fluid);
		GlStateManager.color(getColorComponent(color, 2), getColorComponent(color, 1), getColorComponent(color, 0), 1F);
	}

	@SideOnly(Side.CLIENT)
	public void drawFluid(final GuiBase gui, final int startX, final int startY) {
		if (fluid != null) {
			if (fluid.getFluid() == null) {
				return;
			}
			int fluidLevel = (int) (48 * ((float)fluid.amount / (float) capacity));
			ResourceLocation still = fluid.getFluid().getStill();
			TextureAtlasSprite sprite = gui.mc.getTextureMapBlocks().getTextureExtry(still.toString());
			if (sprite == null) {
				return;
			}

			gui.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			applyColorFilter(fluid);

			GlStateManager.enableBlend();
			for (int y = 0; y < 3; y++) {
				int pixels = fluidLevel - (2 - y) * 16;

				if (pixels <= 0) {
					continue;
				} else if (pixels > 16) {
					pixels = 16;
				}

				for (int x = 0; x < 2; x++) {
					owner.drawImage(tankid, gui, sprite, startX + 2 + 16 * x, startY + 1 + 16 * y + (16 - pixels), 0, (16 - pixels), 16, pixels);
				}
			}
			GlStateManager.enableBlend();
			GlStateManager.color(1F, 1F, 1F, 1F);
		}
	}

	@Override
	public FluidTankInfo getInfo() {
		return new FluidTankInfo(fluid, getCapacity());
	}
}
