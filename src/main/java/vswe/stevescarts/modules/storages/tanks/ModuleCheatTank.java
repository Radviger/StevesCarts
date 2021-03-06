package vswe.stevescarts.modules.storages.tanks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.Localization;

import java.io.DataInput;
import java.io.IOException;

public class ModuleCheatTank extends ModuleTank {
	private static final TextFormatting[] colors = new TextFormatting[] { TextFormatting.YELLOW, TextFormatting.GREEN, TextFormatting.RED, TextFormatting.GOLD };
	private int mode;

	public ModuleCheatTank(final EntityMinecartModular cart) {
		super(cart);
	}

	@Override
	protected String getTankInfo() {
		String str = super.getTankInfo();
		str = str + "\n\n" + Localization.MODULES.TANKS.CREATIVE_MODE.translate(ModuleCheatTank.colors[mode].toString(), String.valueOf(mode)) + "\n" + Localization.MODULES.TANKS.CHANGE_MODE.translate();
		if (mode != 0) {
			str = str + "\n" + Localization.MODULES.TANKS.RESET_MODE.translate();
		}
		return str;
	}

	@Override
	protected int getTankSize() {
		return 2000000000;
	}

	@Override
	public boolean hasVisualTank() {
		return false;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		int m = reader.readByte();
		if (id == 0 && (m & 0x1) != 0x0) {
			if (mode != 0 && (m & 0x2) != 0x0) {
				mode = 0;
			} else if (++mode == ModuleCheatTank.colors.length) {
				mode = 1;
			}
			updateAmount();
			updateDw();
		}
	}

	@Override
	public int numberOfGuiData() {
		return super.numberOfGuiData() + 1;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		super.checkGuiData(info);
		updateGuiData(info, super.numberOfGuiData(), (short) mode);
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == super.numberOfGuiData()) {
			mode = data;
		} else {
			super.receiveGuiData(id, data);
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		super.Save(tagCompound, id);
		tagCompound.setByte(generateNBTName("mode", id), (byte) mode);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		super.Load(tagCompound, id);
		mode = tagCompound.getByte(generateNBTName("mode", id));
	}

	private void updateAmount() {
		if (tank.getFluid() != null) {
			if (mode == 1) {
				tank.getFluid().amount = getTankSize();
			} else if (mode == 2) {
				tank.getFluid().amount = 0;
				if (!tank.isLocked()) {
					tank.setFluid(null);
				}
			} else if (mode == 3) {
				tank.getFluid().amount = getTankSize() / 2;
			}
		}
	}

	@Override
	public void onFluidUpdated(final int tankid) {
		updateAmount();
		super.onFluidUpdated(tankid);
	}
}
