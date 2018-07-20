package vswe.stevescarts.blocks.tileentities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.containers.ContainerActivator;
import vswe.stevescarts.containers.ContainerBase;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiActivator;
import vswe.stevescarts.guis.GuiBase;
import vswe.stevescarts.helpers.ActivatorOption;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.modules.addons.ModuleChunkLoader;
import vswe.stevescarts.modules.addons.ModuleInvisible;
import vswe.stevescarts.modules.addons.ModuleShield;
import vswe.stevescarts.modules.realtimers.ModuleCage;
import vswe.stevescarts.modules.workers.ModuleRemover;
import vswe.stevescarts.modules.workers.tools.ModuleDrill;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

public class TileEntityActivator extends TileEntityBase {
	private ArrayList<ActivatorOption> options;

	@SideOnly(Side.CLIENT)
	@Override
	public GuiBase getGui(final InventoryPlayer inv) {
		return new GuiActivator(inv, this);
	}

	@Override
	public ContainerBase getContainer(final InventoryPlayer inv) {
		return new ContainerActivator(inv, this);
	}

	public TileEntityActivator() {
		loadOptions();
	}

	private void loadOptions() {
		(options = new ArrayList<>()).add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_DRILL, ModuleDrill.class));
		options.add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_SHIELD, ModuleShield.class));
		options.add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_INVISIBILITY, ModuleInvisible.class));
		options.add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_CHUNK, ModuleChunkLoader.class));
		options.add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_CAGE_AUTO, ModuleCage.class, 0));
		options.add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_CAGE, ModuleCage.class, 1));
		options.add(new ActivatorOption(Localization.GUI.TOGGLER.OPTION_REMOVER, ModuleRemover.class));
	}

	public ArrayList<ActivatorOption> getOptions() {
		return options;
	}

	@Override
	public void readFromNBT(final NBTTagCompound compound) {
		super.readFromNBT(compound);
		for (final ActivatorOption option : options) {
			option.setOption(compound.getByte(option.getName()));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
		super.writeToNBT(compound);
		for (final ActivatorOption option : options) {
			compound.setByte(option.getName(), (byte) option.getOption());
		}
		return compound;
	}

	@Override
	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0) {
			final byte data = reader.readByte();
			final boolean leftClick = (data & 0x1) == 0x0;
			final int optionId = (data & 0xFFFFFFFE) >> 1;
			if (optionId >= 0 && optionId < options.size()) {
				options.get(optionId).changeOption(leftClick);
			}
		}
	}

	@Override
	public void initGuiData(final Container con, final IContainerListener crafting) {
		for (int i = 0; i < options.size(); ++i) {
			updateGuiData(con, crafting, i, (short) options.get(i).getOption());
		}
	}

	@Override
	public void checkGuiData(final Container con, final IContainerListener crafting) {
		for (int i = 0; i < options.size(); ++i) {
			final int option = options.get(i).getOption();
			final int lastoption = ((ContainerActivator) con).lastOptions.get(i);
			if (option != lastoption) {
				updateGuiData(con, crafting, i, (short) option);
				((ContainerActivator) con).lastOptions.set(i, option);
			}
		}
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id >= 0 && id < options.size()) {
			options.get(id).setOption(data);
		}
	}

	public void handleCart(final EntityMinecartModular cart, final boolean isOrange) {
		for (final ActivatorOption option : options) {
			if (!option.isDisabled()) {
				cart.handleActivator(option, isOrange);
			}
		}
	}
}
