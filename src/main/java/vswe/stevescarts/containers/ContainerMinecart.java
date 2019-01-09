package vswe.stevescarts.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.blocks.tileentities.TileEntityBase;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotFake;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.modules.ModuleBase;

import java.util.HashMap;
import java.util.List;

public class ContainerMinecart extends ContainerBase {
	private IInventory player;
	public HashMap<Short, Short> cache;
	public EntityMinecartModular cart;

	public ContainerMinecart(final IInventory player, final EntityMinecartModular cart) {
		cartInv(cart);
		playerInv(player);
	}

	@Override
	public IInventory getMyInventory() {
		return cart;
	}

	@Override
	public TileEntityBase getTileEntity() {
		return null;
	}

	protected void cartInv(final EntityMinecartModular cart) {
		this.cart = cart;
		if (cart.getModules() != null) {
			for (final ModuleBase module : cart.getModules()) {
				if (module.hasSlots()) {
					final List<SlotBase> slotsList = module.getSlots();
					for (final SlotBase slot : slotsList) {
						slot.xPos = slot.getX() + module.getX() + 1;
						slot.yPos = slot.getY() + module.getY() + 1;
						addSlotToContainer(slot);
					}
				}
			}
		} else {
			for (int i = 0; i < 100; ++i) {
				addSlotToContainer(new Slot(cart, i, -1000, -1000));
			}
		}
	}

	protected void playerInv(final IInventory player) {
		this.player = player;
		for (int i = 0; i < 3; ++i) {
			for (int k = 0; k < 9; ++k) {
				addSlotToContainer(new Slot(player, k + i * 9 + 9, offsetX() + k * 18, i * 18 + offsetY()));
			}
		}
		for (int j = 0; j < 9; ++j) {
			addSlotToContainer(new Slot(player, j, offsetX() + j * 18, 58 + offsetY()));
		}
	}

	protected int offsetX() {
		return 159;
	}

	protected int offsetY() {
		return 174;
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		ItemStack stack = super.slotClick(slotId, dragType, clickTypeIn, player);
		if (slotId > 0) {
			Slot slot = this.inventorySlots.get(slotId);
			if (slot instanceof SlotFake) {
				return ItemStack.EMPTY;
			}
		}
		return stack;
	}

	@Override
	public boolean canInteractWith(final EntityPlayer entityplayer) {
		return cart.isUsableByPlayer(entityplayer);
	}

	@Override
	public void onContainerClosed(final EntityPlayer par1EntityPlayer) {
		super.onContainerClosed(par1EntityPlayer);
		cart.closeInventory(par1EntityPlayer);
	}

	@Override
	public void addListener(final IContainerListener par1ICrafting) {
		super.addListener(par1ICrafting);
		if (cart.getModules() != null) {
			for (ModuleBase module : cart.getModules()) {}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void updateProgressBar(final int par1, int par2) {
		par2 &= 0xFFFF;
		if (cart.getModules() != null) {
			for (final ModuleBase module : cart.getModules()) {
				if (par1 >= module.getGuiDataStart() && par1 < module.getGuiDataStart() + module.numberOfGuiData()) {
					module.receiveGuiData(par1 - module.getGuiDataStart(), (short) par2);
					break;
				}
			}
		}
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (cart.getModules() != null && listeners.size() > 0) {
			for (final ModuleBase module : cart.getModules()) {
				module.checkGuiData(this, listeners, false);
			}
		}
	}
}
