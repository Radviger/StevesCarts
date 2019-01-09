package vswe.stevescarts.upgrades;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import vswe.stevescarts.blocks.tileentities.TileEntityUpgrade;
import vswe.stevescarts.containers.slots.SlotCart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.modules.data.ModuleData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Blueprint extends SimpleInventoryEffect {
	public Blueprint() {
		super(1, 1);
	}

	@Override
	public Class<? extends Slot> getSlot(final int i) {
		return SlotCart.class;
	}

	@Override
	public String getName() {
		return Localization.UPGRADES.BLUEPRINT.translate();
	}

	public boolean isValidForBluePrint(final TileEntityUpgrade upgrade, final List<ModuleData> modules, final ModuleData module) {
		@Nonnull
		ItemStack blueprint = upgrade.getStackInSlot(0);
		if (blueprint.isEmpty()) {
			return false;
		}
		final NBTTagCompound info = blueprint.getTagCompound();
		if (info == null) {
			return false;
		}
		final NBTTagByteArray moduleIDTag = (NBTTagByteArray) info.getTag("Modules");
		if (moduleIDTag == null) {
			return false;
		}
		final byte[] IDs = moduleIDTag.getByteArray();
		final List<ModuleData> missing = new ArrayList<>();
		for (final byte id : IDs) {
			final ModuleData blueprintModule = ModuleData.getList().get(id);
			final int index = modules.indexOf(blueprintModule);
			if (index != -1) {
				modules.remove(index);
			} else {
				missing.add(blueprintModule);
			}
		}
		return missing.contains(module);
	}
}
