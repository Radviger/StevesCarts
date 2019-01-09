package vswe.stevescarts.modules.realtimers;

import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotFirework;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.modules.ModuleBase;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ModuleFirework extends ModuleBase {
	private int fireCooldown;

	public ModuleFirework(final EntityMinecartModular cart) {
		super(cart);
	}

	@Override
	public void update() {
		if (fireCooldown > 0) {
			--fireCooldown;
		}
	}

	@Override
	public void activatedByRail(final int x, final int y, final int z, final boolean active) {
		if (active && fireCooldown == 0 && getCart().hasFuel()) {
			fire();
			fireCooldown = 20;
		}
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotFirework(getCart(), slotId, 8 + x * 18, 16 + y * 18);
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public int guiWidth() {
		return 15 + getInventoryWidth() * 18;
	}

	@Override
	public int guiHeight() {
		return 20 + getInventoryHeight() * 18;
	}

	@Override
	protected int getInventoryWidth() {
		return 8;
	}

	@Override
	protected int getInventoryHeight() {
		return 3;
	}

	public void fire() {
		if (getCart().world.isRemote) {
			return;
		}
		@Nonnull
		ItemStack firework = getFirework();
		if (!firework.isEmpty()) {
			launchFirework(firework);
		}
	}

	@Nonnull
	private ItemStack getFirework() {
		boolean hasGunpowder = false;
		boolean hasPaper = false;
		for (int i = 0; i < getInventorySize(); ++i) {
			@Nonnull
			ItemStack item = getStack(i);
			if (!item.isEmpty()) {
				if (item.getItem() == Items.FIREWORKS) {
					@Nonnull
					ItemStack firework = item.copy();
					firework.setCount(1);
					removeItemStack(item, firework.getCount(), i);
					return firework;
				}
				if (item.getItem() == Items.PAPER) {
					hasPaper = true;
				} else if (item.getItem() == Items.GUNPOWDER) {
					hasGunpowder = true;
				}
			}
		}
		if (hasPaper && hasGunpowder) {
			@Nonnull
			ItemStack firework2 = new ItemStack(Items.FIREWORKS);
			final int maxGunpowder = getCart().rand.nextInt(3) + 1;
			int countGunpowder = 0;
			boolean removedPaper = false;
			for (int j = 0; j < getInventorySize(); ++j) {
				@Nonnull
				ItemStack item2 = getStack(j);
				if (!item2.isEmpty()) {
					if (item2.getItem() == Items.PAPER && !removedPaper) {
						removeItemStack(item2, 1, j);
						removedPaper = true;
					} else if (item2.getItem() == Items.GUNPOWDER && countGunpowder < maxGunpowder) {
						while (item2.getCount() > 0 && countGunpowder < maxGunpowder) {
							++countGunpowder;
							removeItemStack(item2, 1, j);
						}
					}
				}
			}
			int chargeCount;
			for (chargeCount = 1; chargeCount < 7 && getCart().rand.nextInt(3 + chargeCount / 3) == 0; ++chargeCount) {}
			final NBTTagCompound itemstackNBT = new NBTTagCompound();
			final NBTTagCompound fireworksNBT = new NBTTagCompound();
			final NBTTagList explosionsNBT = new NBTTagList();
			for (int k = 0; k < chargeCount; ++k) {
				@Nonnull
				ItemStack charge = getCharge();
				if (charge.isEmpty()) {
					break;
				}
				if (charge.hasTagCompound() && charge.getTagCompound().hasKey("Explosion")) {
					explosionsNBT.appendTag(charge.getTagCompound().getCompoundTag("Explosion"));
				}
			}
			fireworksNBT.setTag("Explosions", explosionsNBT);
			fireworksNBT.setByte("Flight", (byte) countGunpowder);
			itemstackNBT.setTag("Fireworks", fireworksNBT);
			firework2.setTagCompound(itemstackNBT);
			return firework2;
		}
		return ItemStack.EMPTY;
	}

	@Nonnull
	private ItemStack getCharge() {
		for (int i = 0; i < getInventorySize(); ++i) {
			@Nonnull
			ItemStack item = getStack(i);
			if (!item.isEmpty() && item.getItem() == Items.FIREWORK_CHARGE) {
				@Nonnull
				ItemStack charge = item.copy();
				charge.setCount(1);
				removeItemStack(item, charge.getCount(), i);
				return charge;
			}
		}
		@Nonnull
		ItemStack charge2 = new ItemStack(Items.FIREWORK_CHARGE);
		final NBTTagCompound itemNBT = new NBTTagCompound();
		final NBTTagCompound explosionNBT = new NBTTagCompound();
		byte type = 0;
		boolean removedGunpowder = false;
		final boolean canHasTrail = getCart().rand.nextInt(16) == 0;
		final boolean canHasFlicker = getCart().rand.nextInt(8) == 0;
		final boolean canHasModifier = getCart().rand.nextInt(4) == 0;
		final byte modifierType = (byte) (getCart().rand.nextInt(4) + 1);
		boolean removedModifier = false;
		boolean removedDiamond = false;
		boolean removedGlow = false;
		for (int j = 0; j < getInventorySize(); ++j) {
			@Nonnull
			ItemStack item2 = getStack(j);
			if (item2 != null) {
				if (item2.getItem() == Items.GUNPOWDER && !removedGunpowder) {
					removeItemStack(item2, 1, j);
					removedGunpowder = true;
				} else if (item2.getItem() == Items.GLOWSTONE_DUST && canHasFlicker && !removedGlow) {
					removeItemStack(item2, 1, j);
					removedGlow = true;
					explosionNBT.setBoolean("Flicker", true);
				} else if (item2.getItem() == Items.DIAMOND && canHasTrail && !removedDiamond) {
					removeItemStack(item2, 1, j);
					removedDiamond = true;
					explosionNBT.setBoolean("Trail", true);
				} else if (canHasModifier && !removedModifier && ((item2.getItem() == Items.FIREWORK_CHARGE && modifierType == 1) || (item2.getItem() == Items.GOLD_NUGGET && modifierType == 2) || (item2.getItem() == Items.SKULL && modifierType == 3) || (item2.getItem() == Items.FEATHER && modifierType == 4))) {
					removeItemStack(item2, 1, j);
					removedModifier = true;
					type = modifierType;
				}
			}
		}
		final int[] colors = generateColors((type != 0) ? 7 : 8);
		if (colors == null) {
			return null;
		}
		explosionNBT.setIntArray("Colors", colors);
		if (getCart().rand.nextInt(4) == 0) {
			final int[] fade = generateColors(8);
			if (fade != null) {
				explosionNBT.setIntArray("FadeColors", fade);
			}
		}
		explosionNBT.setByte("Type", type);
		itemNBT.setTag("Explosion", explosionNBT);
		charge2.setTagCompound(itemNBT);
		return charge2;
	}

	private int[] generateColors(final int maxColorCount) {
		final int[] maxColors = new int[16];
		final int[] currentColors = new int[16];
		for (int i = 0; i < getInventorySize(); ++i) {
			@Nonnull
			ItemStack item = getStack(i);
			if (!item.isEmpty() && item.getItem() == Items.DYE) {
				final int itemDamage = item.getItemDamage();
				maxColors[itemDamage] += item.getCount();
			}
		}
		int colorCount = getCart().rand.nextInt(2) + 1;
		while (colorCount <= maxColorCount - 2 && getCart().rand.nextInt(2) == 0) {
			colorCount += 2;
		}
		final List<Integer> colorPointers = new ArrayList<>();
		for (int j = 0; j < 16; ++j) {
			if (maxColors[j] > 0) {
				colorPointers.add(j);
			}
		}
		if (colorPointers.size() == 0) {
			return null;
		}
		final List<Integer> usedColors = new ArrayList<>();
		while (colorCount > 0 && colorPointers.size() > 0) {
			final int pointerId = getCart().rand.nextInt(colorPointers.size());
			final int colorId = colorPointers.get(pointerId);
			++currentColors[colorId];
			if (--maxColors[colorId] <= 0) {
				colorPointers.remove(pointerId);
			}
			usedColors.add(colorId);
			--colorCount;
		}
		final int[] colors = new int[usedColors.size()];
		for (int k = 0; k < colors.length; ++k) {
			colors[k] = ItemDye.DYE_COLORS[usedColors.get(k)];
		}
		for (int k = 0; k < getInventorySize(); ++k) {
			@Nonnull
			ItemStack item2 = getStack(k);
			if (!item2.isEmpty() && item2.getItem() == Items.DYE && currentColors[item2.getItemDamage()] > 0) {
				final int count = Math.min(currentColors[item2.getItemDamage()], item2.getCount());
				final int itemDamage2 = item2.getItemDamage();
				currentColors[itemDamage2] -= count;
			}
		}
		return colors;
	}

	private void removeItemStack(
		@Nonnull
			ItemStack item, final int count, final int id) {
		if (!getCart().hasCreativeSupplies()) {
			item.shrink(count);
			if (item.getCount() <= 0) {
				setStack(id, ItemStack.EMPTY);
			}
		}
	}

	private void launchFirework(
		@Nonnull
			ItemStack firework) {
		final EntityFireworkRocket rocket = new EntityFireworkRocket(getCart().world, getCart().posX, getCart().posY + 1.0, getCart().posZ, firework);
		getCart().world.spawnEntity(rocket);
	}
}
