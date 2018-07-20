package vswe.stevescarts.blocks.tileentities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.containers.ContainerBase;
import vswe.stevescarts.guis.GuiBase;

import java.io.DataInput;
import java.io.IOException;

public abstract class TileEntityBase extends TileEntity implements ITickable {

	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
	}

	@SideOnly(Side.CLIENT)
	public abstract GuiBase getGui(final InventoryPlayer p0);

	public abstract ContainerBase getContainer(final InventoryPlayer p0);

	public void updateGuiData(final Container con, final IContainerListener crafting, final int id, final short data) {
		crafting.sendWindowProperty(con, id, data);
	}

	public void initGuiData(final Container con, final IContainerListener crafting) {
	}

	public void checkGuiData(final Container con, final IContainerListener crafting) {
	}

	public void receiveGuiData(final int id, final short data) {
	}

	public boolean isUsableByPlayer(final EntityPlayer entityplayer) {
		return world.getTileEntity(pos) == this && entityplayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}

	public short getShortFromInt(final boolean first, final int val) {
		if (first) {
			return (short) (val & 0xFFFF);
		}
		return (short) (val >> 16 & 0xFFFF);
	}

	public int getIntFromShort(final boolean first, int oldVal, final short val) {
		if (first) {
			oldVal = ((oldVal & 0xFFFF0000) | val);
		} else {
			oldVal = ((oldVal & 0xFFFF) | val << 16);
		}
		return oldVal;
	}

	@Override
	public void update() {
		updateEntity();
	}

	public void updateEntity() {

	}
}
