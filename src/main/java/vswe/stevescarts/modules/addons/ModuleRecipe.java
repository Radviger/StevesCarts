package vswe.stevescarts.modules.addons;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.blocks.tileentities.TileEntityCargo;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotChest;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

public abstract class ModuleRecipe extends ModuleAddon {
	private int target;
	protected boolean dirty;
	protected ArrayList<SlotBase> inputSlots;
	protected ArrayList<SlotBase> outputSlots;
	protected ArrayList<SlotBase> allTheSlots;
	private int maxItemCount;
	private int mode;

	public ModuleRecipe(final EntityMinecartModular cart) {
		super(cart);
		maxItemCount = 1;
		target = 3;
		dirty = true;
		allTheSlots = new ArrayList<>();
		outputSlots = new ArrayList<>();
	}

	protected abstract int getLimitStartX();

	protected abstract int getLimitStartY();

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		if (canUseAdvancedFeatures()) {
			final int[] area = getArea();
			ResourceHelper.bindResource("/gui/recipe.png");
			drawImage(gui, area[0] - 2, area[1] - 2, 0, 0, 20, 20);
			if (mode == 1) {
				for (int i = 0; i < 3; ++i) {
					drawControlRect(gui, x, y, i);
				}
			} else {
				drawControlRect(gui, x, y, 1);
			}
		}
	}

	private void drawControlRect(final GuiMinecart gui, final int x, final int y, final int i) {
		final int v = i * 11;
		final int[] rect = getControlRect(i);
		drawImage(gui, rect, 20 + (inRect(x, y, rect) ? 22 : 0), v);
	}

	private int[] getControlRect(final int i) {
		return new int[] { getLimitStartX(), getLimitStartY() + 12 * i, 22, 11 };
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawForeground(final GuiMinecart gui) {
		if (canUseAdvancedFeatures()) {
			String str = null;
			switch (mode) {
				case 0: {
					str = "Inf";
					break;
				}
				case 1: {
					str = String.valueOf(maxItemCount);
					break;
				}
				default: {
					str = "X";
					break;
				}
			}
			drawString(gui, str, getControlRect(1), 4210752);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackgroundItems(final GuiMinecart gui, final int x, final int y) {
		if (canUseAdvancedFeatures()) {
			ItemStack icon;
			if (isTargetInvalid()) {
				icon = new ItemStack(Items.MINECART, 1);
			} else {
				icon = TileEntityCargo.itemSelections.get(target).getIcon();
			}
			final int[] area = getArea();
			drawItemInInterface(gui, icon, area[0], area[1]);
		}
	}

	private boolean isTargetInvalid() {
		return target < 0 || target >= TileEntityCargo.itemSelections.size() || TileEntityCargo.itemSelections.get(target).getValidSlot() == null;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		if (canUseAdvancedFeatures()) {
			String str = Localization.MODULES.ADDONS.RECIPE_OUTPUT.translate() + "\n" + Localization.MODULES.ADDONS.CURRENT.translate() + ": ";
			if (isTargetInvalid()) {
				str += Localization.MODULES.ADDONS.INVALID_OUTPUT.translate();
			} else {
				str += TileEntityCargo.itemSelections.get(target).getName();
			}
			drawStringOnMouseOver(gui, str, x, y, getArea());
			for (int i = 0; i < 3; ++i) {
				if (i == 1) {
					str = Localization.MODULES.ADDONS.RECIPE_MODE.translate() + "\n" + Localization.MODULES.ADDONS.CURRENT.translate() + ": ";
					switch (mode) {
						case 0: {
							str += Localization.MODULES.ADDONS.RECIPE_NO_LIMIT.translate();
							break;
						}
						case 1: {
							str += Localization.MODULES.ADDONS.RECIPE_LIMIT.translate();
							break;
						}
						default: {
							str += Localization.MODULES.ADDONS.RECIPE_DISABLED.translate();
							break;
						}
					}
				} else if (mode != 1) {
					str = null;
				} else {
					str = Localization.MODULES.ADDONS.RECIPE_CHANGE_AMOUNT.translate((i == 0) ? "0"
					                                                                          : "1") + "\n" + Localization.MODULES.ADDONS.RECIPE_CHANGE_AMOUNT_10.translate() + "\n" + Localization.MODULES.ADDONS.RECIPE_CHANGE_AMOUNT_64.translate();
				}
				if (str != null) {
					drawStringOnMouseOver(gui, str, x, y, getControlRect(i));
				}
			}
		}
	}

	protected abstract int[] getArea();

	@Override
	public int numberOfGuiData() {
		return canUseAdvancedFeatures() ? 3 : 0;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		if (canUseAdvancedFeatures()) {
			updateGuiData(info, 0, (short) target);
			updateGuiData(info, 1, (short) mode);
			updateGuiData(info, 2, (short) maxItemCount);
		}
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (canUseAdvancedFeatures()) {
			if (id == 0) {
				target = data;
			} else if (id == 1) {
				mode = data;
			} else if (id == 2) {
				maxItemCount = data;
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (canUseAdvancedFeatures()) {
			if (inRect(x, y, getArea())) {
				sendPacket(0, (byte) button);
			}
			int i = 0;
			while (i < 3) {
				if ((mode == 1 || i == 1) && inRect(x, y, getControlRect(i))) {
					if (i == 1) {
						sendPacket(1, (byte) button);
						break;
					}
					byte encodedData = (byte) ((i != 0) ? 1 : 0);
					if (GuiScreen.isCtrlKeyDown()) {
						encodedData |= 0x2;
					} else if (GuiScreen.isShiftKeyDown()) {
						encodedData |= 0x4;
					}
					sendPacket(2, encodedData);
					break;
				} else {
					++i;
				}
			}
		}
	}

	@Override
	protected int numberOfPackets() {
		return canUseAdvancedFeatures() ? 3 : 0;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (canUseAdvancedFeatures()) {
			if (id == 0) {
				dirty = true;
				changeTarget(reader.readByte() == 0);
			} else if (id == 1) {
				if (reader.readByte() == 0) {
					if (++mode > 2) {
						mode = 0;
					}
				} else if (--mode < 0) {
					mode = 2;
				}
			} else if (id == 2) {
				int m = reader.readByte();
				int dif = ((m & 0x1) == 0x0) ? 1 : -1;
				if ((m & 0x2) != 0x0) {
					dif *= 64;
				} else if ((m & 0x4) != 0x0) {
					dif *= 10;
				}
				maxItemCount = Math.min(Math.max(1, maxItemCount + dif), 999);
			}
		}
	}

	private void changeTarget(final boolean up) {
		if (!up) {
			if (--target < 0) {
				target = TileEntityCargo.itemSelections.size() - 1;
			}
		} else if (++target >= TileEntityCargo.itemSelections.size()) {
			target = 0;
		}
		if (isTargetInvalid()) {
			changeTarget(up);
		}
	}

	protected abstract boolean canUseAdvancedFeatures();

	protected Class getValidSlot() {
		if (isTargetInvalid()) {
			return null;
		}
		return TileEntityCargo.itemSelections.get(target).getValidSlot();
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		if (canUseAdvancedFeatures()) {
			target = tagCompound.getByte(generateNBTName("Target", id));
			mode = tagCompound.getByte(generateNBTName("Mode", id));
			maxItemCount = tagCompound.getShort(generateNBTName("MaxItems", id));
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		if (canUseAdvancedFeatures()) {
			tagCompound.setByte(generateNBTName("Target", id), (byte) target);
			tagCompound.setByte(generateNBTName("Mode", id), (byte) mode);
			tagCompound.setShort(generateNBTName("MaxItems", id), (short) maxItemCount);
		}
	}

	protected void prepareLists() {
		if (inputSlots == null) {
			inputSlots = new ArrayList<>();
			for (final ModuleBase module : getCart().getModules()) {
				if (module.getSlots() != null) {
					for (final SlotBase slot : module.getSlots()) {
						if (slot instanceof SlotChest) {
							inputSlots.add(slot);
						}
					}
				}
			}
		}
		if (dirty) {
			allTheSlots.clear();
			outputSlots.clear();
			final Class validSlot = getValidSlot();
			for (final ModuleBase module2 : getCart().getModules()) {
				if (module2.getSlots() != null) {
					for (final SlotBase slot2 : module2.getSlots()) {
						if (validSlot.isInstance(slot2)) {
							outputSlots.add(slot2);
							allTheSlots.add(slot2);
						} else {
							if (!(slot2 instanceof SlotChest)) {
								continue;
							}
							allTheSlots.add(slot2);
						}
					}
				}
			}
			dirty = false;
		}
	}

	protected boolean canCraftMoreOfResult(@Nonnull ItemStack result) {
		if (mode == 0) {
			return true;
		}
		if (mode == 2) {
			return false;
		}
		int count = 0;
		for (int i = 0; i < outputSlots.size(); ++i) {
			@Nonnull
			ItemStack item = outputSlots.get(i).getStack();
			if (!item.isEmpty() && item.isItemEqual(result) && ItemStack.areItemStackTagsEqual(item, result)) {
				count += item.getCount();
				if (count >= maxItemCount) {
					return false;
				}
			}
		}
		return true;
	}
}
