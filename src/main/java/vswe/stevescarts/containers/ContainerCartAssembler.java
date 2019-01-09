package vswe.stevescarts.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.blocks.tileentities.TileEntityBase;
import vswe.stevescarts.blocks.tileentities.TileEntityCartAssembler;
import vswe.stevescarts.containers.slots.SlotAssembler;
import vswe.stevescarts.containers.slots.SlotHull;

import javax.annotation.Nonnull;
import java.util.List;

public class ContainerCartAssembler extends ContainerBase {
	private TileEntityCartAssembler assembler;
	public int lastMaxAssemblingTime;
	public int lastCurrentAssemblingTime;
	public boolean lastIsAssembling;
	public int lastFuelLevel;

	@Override
	public IInventory getMyInventory() {
		return assembler;
	}

	@Override
	public TileEntityBase getTileEntity() {
		return assembler;
	}

	public ContainerCartAssembler(final IInventory invPlayer, final TileEntityCartAssembler assembler) {
		this.assembler = assembler;
		final List<SlotAssembler> slots = assembler.getSlots();
		for (final SlotAssembler slot : slots) {
			addSlotToContainer(slot);
		}
		for (int i = 0; i < 3; ++i) {
			for (int k = 0; k < 9; ++k) {
				addSlotToContainer(new Slot(invPlayer, k + i * 9 + 9, offsetX() + k * 18, i * 18 + offsetY()));
			}
		}
		for (int j = 0; j < 9; ++j) {
			addSlotToContainer(new Slot(invPlayer, j, offsetX() + j * 18, 58 + offsetY()));
		}
	}

	@Override
	public boolean canInteractWith(final EntityPlayer entityplayer) {
		return assembler.isUsableByPlayer(entityplayer);
	}

	@Override
	public void addListener(final IContainerListener par1ICrafting) {
		super.addListener(par1ICrafting);
		assembler.initGuiData(this, par1ICrafting);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void updateProgressBar(final int par1, int par2) {
		par2 &= 0xFFFF;
		assembler.receiveGuiData(par1, (short) par2);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		for (final IContainerListener var2 : listeners) {
			assembler.checkGuiData(this, var2);
		}
	}

	protected int offsetX() {
		return 176;
	}

	protected int offsetY() {
		return 174;
	}

	@Override
	@Nonnull
	public ItemStack slotClick(final int slotID, final int button, final ClickType keyflag, final EntityPlayer player) {
		if (slotID >= 0 && slotID < inventorySlots.size()) {
			final Slot hullSlot = inventorySlots.get(slotID);
			if (hullSlot != null && hullSlot instanceof SlotHull) {
				final InventoryPlayer playerInventory = player.inventory;
				@Nonnull
				ItemStack playerItem = playerInventory.getItemStack();
				@Nonnull
				ItemStack slotItem = hullSlot.getStack();
				final List<SlotAssembler> newSlots = assembler.getValidSlotFromHullItem(playerItem);
				final List<SlotAssembler> oldSlots = assembler.getValidSlotFromHullItem(slotItem);
				if (oldSlots != null) {
					if (newSlots != null) {
						for (final SlotAssembler slot : newSlots) {
							final int index = oldSlots.indexOf(slot);
							if (index != -1) {
								oldSlots.remove(index);
							}
						}
					}
					for (final SlotAssembler slot : oldSlots) {
						if (slot.getHasStack()) {
							return ItemStack.EMPTY;
						}
					}
				}
			}
		}
		return super.slotClick(slotID, button, keyflag, player);
	}
}
