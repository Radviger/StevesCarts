package vswe.stevescarts.modules.storages.chests;

import net.minecraft.item.ItemStack;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.GiftItem;

import java.util.List;

public class ModuleGiftStorage extends ModuleChest {
	public ModuleGiftStorage(final EntityMinecartModular cart) {
		super(cart);
	}

	@Override
	protected int getInventoryWidth() {
		return 9;
	}

	@Override
	protected int getInventoryHeight() {
		return 4;
	}

	@Override
	public byte getExtraData() {
		return 0;
	}

	@Override
	public boolean hasExtraData() {
		return true;
	}

	@Override
	public void setExtraData(final byte b) {
		if (b == 0) {
			return;
		}
		final List<ItemStack> items = GiftItem.generateItems(getCart().rand, GiftItem.ChristmasList, 50 + getCart().rand.nextInt(700), 1 + getCart().rand.nextInt(5));
		for (int i = 0; i < items.size(); ++i) {
			setStack(i, items.get(i));
		}
	}
}
