package vswe.stevescarts.modules.workers.tools;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotRepair;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.EnchantmentInfo;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.addons.ModuleEnchants;
import vswe.stevescarts.modules.workers.ModuleWorker;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class ModuleTool extends ModuleWorker {
	private static final Method getSilkTouchDrop = ReflectionHelper.findMethod(Block.class, "getSilkTouchDrop", "func_180643_i", IBlockState.class);
	static {
		getSilkTouchDrop.setAccessible(true);
	}

	private int currentDurability;
	private int remainingRepairUnits;
	private int maximumRepairUnits;
	protected ModuleEnchants enchanter;
	private int[] durabilityRect;

	public ModuleTool(final EntityMinecartModular cart) {
		super(cart);
		maximumRepairUnits = 1;
		durabilityRect = new int[] { 10, 15, 52, 8 };
		currentDurability = getMaxDurability();
	}

	public abstract int getMaxDurability();

	public abstract String getRepairItemName();

	public abstract int getRepairItemUnits(@Nonnull ItemStack p0);

	public abstract int getRepairSpeed();

	public abstract boolean useDurability();

	@Override
	public void init() {
		super.init();
		for (final ModuleBase module : getCart().getModules()) {
			if (module instanceof ModuleEnchants) {
				(enchanter = (ModuleEnchants) module).addType(EnchantmentInfo.ENCHANTMENT_TYPE.TOOL);
				break;
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/tool.png");
		drawBox(gui, 0, 0, 1.0f);
		drawBox(gui, 0, 8, useDurability() ? (currentDurability / getMaxDurability()) : 1.0f);
		drawBox(gui, 0, 16, remainingRepairUnits / maximumRepairUnits);
		if (inRect(x, y, durabilityRect)) {
			drawBox(gui, 0, 24, 1.0f);
		}
	}

	private void drawBox(final GuiMinecart gui, final int u, final int v, final float mult) {
		final int w = (int) (durabilityRect[2] * mult);
		if (w > 0) {
			drawImage(gui, durabilityRect[0], durabilityRect[1], u, v, w, durabilityRect[3]);
		}
	}

	public boolean isValidRepairMaterial(@Nonnull ItemStack item) {
		return getRepairItemUnits(item) > 0;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotRepair(this, getCart(), slotId, 76, 8);
	}

	@Override
	protected int getInventoryWidth() {
		return 1;
	}

	@Override
	public int guiWidth() {
		return 100;
	}

	@Override
	public int guiHeight() {
		return 50;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		String str;
		if (useDurability()) {
			str = Localization.MODULES.TOOLS.DURABILITY.translate() + ": " + currentDurability + "/" + getMaxDurability();
			if (isBroken()) {
				str = str + " [" + Localization.MODULES.TOOLS.BROKEN.translate() + "]";
			} else {
				str = str + " [" + 100 * currentDurability / getMaxDurability() + "%]";
			}
			str += "\n";
			if (isRepairing()) {
				if (isActuallyRepairing()) {
					str = str + " [" + getRepairPercentage() + "%]";
				} else {
					str += Localization.MODULES.TOOLS.DECENT.translate();
				}
			} else {
				str += Localization.MODULES.TOOLS.INSTRUCTION.translate(getRepairItemName());
			}
		} else {
			str = Localization.MODULES.TOOLS.UNBREAKABLE.translate();
			if (isRepairing() && !isActuallyRepairing()) {
				str = str + " " + Localization.MODULES.TOOLS.UNBREAKABLE_REPAIR.translate();
			}
		}
		drawStringOnMouseOver(gui, str, x, y, durabilityRect);
	}

	@Override
	public void update() {
		super.update();
		if (!getCart().world.isRemote && useDurability()) {
			if (isActuallyRepairing()) {
				final int dif = 1;
				remainingRepairUnits -= dif;
				currentDurability += dif * getRepairSpeed();
				if (currentDurability > getMaxDurability()) {
					currentDurability = getMaxDurability();
				}
			}
			if (!isActuallyRepairing()) {
				final int units = getRepairItemUnits(getStack(0));
				if (units > 0 && units <= getMaxDurability() - currentDurability) {
					final int n = units / getRepairSpeed();
					remainingRepairUnits = n;
					maximumRepairUnits = n;
					@Nonnull
					ItemStack stack = getStack(0);
					stack.shrink(1);
					if (getStack(0).getCount() <= 0) {
						setStack(0, ItemStack.EMPTY);
					}
				}
			}
		}
	}

	@Override
	public boolean stopEngines() {
		return isRepairing();
	}

	public boolean isRepairing() {
		return !getStack(0).isEmpty() || isActuallyRepairing();
	}

	public boolean isActuallyRepairing() {
		return remainingRepairUnits > 0;
	}

	public boolean isBroken() {
		return currentDurability == 0 && useDurability();
	}

	public void damageTool(final int val) {
		final int unbreaking = (enchanter != null) ? enchanter.getUnbreakingLevel() : 0;
		if (getCart().rand.nextInt(100) < 100 / (unbreaking + 1)) {
			currentDurability -= val;
			if (currentDurability < 0) {
				currentDurability = 0;
			}
		}
		if (enchanter != null) {
			enchanter.damageEnchant(EnchantmentInfo.ENCHANTMENT_TYPE.TOOL, val);
		}
	}

	@Override
	public int numberOfGuiData() {
		return 4;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		updateGuiData(info, 0, (short) (currentDurability & 0xFFFF));
		updateGuiData(info, 1, (short) (currentDurability >> 16 & 0xFFFF));
		updateGuiData(info, 2, (short) remainingRepairUnits);
		updateGuiData(info, 3, (short) maximumRepairUnits);
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		int dataint = data;
		if (dataint < 0) {
			dataint += 65536;
		}
		if (id == 0) {
			currentDurability = ((currentDurability & 0xFFFF0000) | dataint);
		} else if (id == 1) {
			currentDurability = ((currentDurability & 0xFFFF) | dataint << 16);
		} else if (id == 2) {
			remainingRepairUnits = data;
		} else if (id == 3) {
			maximumRepairUnits = data;
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setInteger(generateNBTName("Durability", id), currentDurability);
		tagCompound.setShort(generateNBTName("Repair", id), (short) remainingRepairUnits);
		tagCompound.setShort(generateNBTName("MaxRepair", id), (short) maximumRepairUnits);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		currentDurability = tagCompound.getInteger(generateNBTName("Durability", id));
		remainingRepairUnits = tagCompound.getShort(generateNBTName("Repair", id));
		maximumRepairUnits = tagCompound.getShort(generateNBTName("MaxRepair", id));
	}

	@Override
	public boolean hasExtraData() {
		return true;
	}

	@Override
	public byte getExtraData() {
		return (byte) (100 * currentDurability / getMaxDurability());
	}

	@Override
	public void setExtraData(final byte b) {
		currentDurability = b * getMaxDurability() / 100;
	}

	public boolean shouldSilkTouch(IBlockState blockState, BlockPos pos) {
		return enchanter != null && enchanter.useSilkTouch() && blockState.getBlock().canSilkHarvest(getCart().world, pos, blockState, getCartOwner());
	}

	@Nonnull
	public ItemStack getSilkTouchedItem(IBlockState state) {
		Block block = state.getBlock();
		try {
			return (ItemStack) getSilkTouchDrop.invoke(block, state);
		} catch (IllegalAccessException | InvocationTargetException e) {
			return new ItemStack(block);
		}
	}

	public int getCurrentDurability() {
		return currentDurability;
	}

	public int getRepairPercentage() {
		return 100 - 100 * remainingRepairUnits / maximumRepairUnits;
	}
}
