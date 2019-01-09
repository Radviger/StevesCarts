package vswe.stevescarts.modules.addons;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotCartCrafter;
import vswe.stevescarts.containers.slots.SlotCartCrafterResult;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ModuleCrafter extends ModuleRecipe {
	private CraftingDummy dummy;
	private int cooldown;

	public ModuleCrafter(final EntityMinecartModular cart) {
		super(cart);
		cooldown = 0;
		dummy = new CraftingDummy(this);
	}

	@Override
	public void update() {
		if (cooldown <= 0) {
			if (!getCart().world.isRemote && getValidSlot() != null) {
				@Nonnull
				ItemStack result = dummy.getResult();
				if (!result.isEmpty() && getCart().getModules() != null) {
					if (result.getCount() == 0) {
						result.setCount(1);
					}
					prepareLists();
					if (canCraftMoreOfResult(result)) {
						final NonNullList<ItemStack> originals = NonNullList.create();
						for (SlotBase slot : allTheSlots) {
							ItemStack item = slot.getStack();
							originals.add((item.isEmpty()) ? ItemStack.EMPTY : item.copy());
						}
						final List<ItemStack> containers = new ArrayList<>();
						boolean valid = true;
						boolean edited = false;
						for (int j = 0; j < 9; ++j) {
							@Nonnull
							ItemStack recipe = getStack(j);
							if (!recipe.isEmpty()) {
								valid = false;
								for (SlotBase slot : inputSlots) {
									ItemStack item2 = slot.getStack();
									if (!item2.isEmpty() && item2.isItemEqual(recipe) && ItemStack.areItemStackTagsEqual(item2, recipe)) {
										edited = true;
										if (item2.getItem().hasContainerItem(item2)) {
											containers.add(item2.getItem().getContainerItem(item2));
										}
										@Nonnull
										ItemStack itemStack = item2;
										itemStack.shrink(1);
										if (item2.getCount() <= 0) {
											slot.putStack(ItemStack.EMPTY);
										}
										valid = true;
										break;
									}
								}
								if (!valid) {
									break;
								}
							}
						}
						if (valid) {
							getCart().addItemToChest(result, getValidSlot(), null);
							if (result.getCount() > 0) {
								valid = false;
							} else {
								edited = true;
								for (int j = 0; j < containers.size(); ++j) {
									@Nonnull
									ItemStack container = containers.get(j);
									if (container != null) {
										getCart().addItemToChest(container, getValidSlot(), null);
										if (container.getCount() > 0) {
											valid = false;
											break;
										}
									}
								}
							}
						}
						if (!valid && edited) {
							for (int j = 0; j < allTheSlots.size(); ++j) {
								allTheSlots.get(j).putStack(originals.get(j));
							}
						}
					}
				}
			}
			cooldown = 40;
		} else {
			--cooldown;
		}
	}

	@Override
	protected int[] getArea() {
		return new int[] { 68, 44, 16, 16 };
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public int getInventorySize() {
		return 10;
	}

	@Override
	public int generateSlots(int slotCount) {
		slotGlobalStart = slotCount;
		slotList = new ArrayList<>();
		for (int y = 0; y < 3; ++y) {
			for (int x = 0; x < 3; ++x) {
				slotList.add(new SlotCartCrafter(getCart(), slotCount++, 10 + 18 * x, 15 + 18 * y));
			}
		}
		slotList.add(new SlotCartCrafterResult(getCart(), slotCount++, 67, canUseAdvancedFeatures() ? 20 : 33));
		return slotCount;
	}

	@Override
	public void onInventoryChanged() {
		if (getCart().world.isRemote) {
			dummy.update();
		}
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		super.drawForeground(gui);
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public int guiWidth() {
		return canUseAdvancedFeatures() ? 120 : 95;
	}

	@Override
	public int guiHeight() {
		return 75;
	}

	@Override
	protected boolean canUseAdvancedFeatures() {
		return false;
	}

	@Override
	protected int getLimitStartX() {
		return 90;
	}

	@Override
	protected int getLimitStartY() {
		return 23;
	}
}
