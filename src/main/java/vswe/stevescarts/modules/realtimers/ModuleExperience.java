package vswe.stevescarts.modules.realtimers;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

import java.io.DataInput;
import java.util.List;

public class ModuleExperience extends ModuleBase {
	private static final int MAX_EXPERIENCE_AMOUNT = 1500;
	private int experienceAmount;

	public ModuleExperience(final EntityMinecartModular cart) {
		super(cart);
	}

	@Override
	public void update() {
		if (!getCart().world.isRemote) {
			final List list = getCart().world.getEntitiesWithinAABBExcludingEntity(getCart(), getCart().getEntityBoundingBox().grow(3.0, 1.0, 3.0));
			for (int e = 0; e < list.size(); ++e) {
				if (list.get(e) instanceof EntityXPOrb) {
					experienceAmount += ((EntityXPOrb) list.get(e)).getXpValue();
					if (experienceAmount > 1500) {
						experienceAmount = 1500;
					} else {
						((EntityXPOrb) list.get(e)).setDead();
					}
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, Localization.MODULES.ATTACHMENTS.EXPERIENCE_LEVEL.translate(String.valueOf(experienceAmount), String.valueOf(1500)) + "\n" + Localization.MODULES.ATTACHMENTS.EXPERIENCE_EXTRACT.translate() + "\n" + Localization.MODULES.ATTACHMENTS.EXPERIENCE_PLAYER_LEVEL.translate(String.valueOf(getClientPlayer().experienceLevel)), x, y, getContainerRect());
	}

	@Override
	public int numberOfGuiData() {
		return 1;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		updateGuiData(info, 0, (short) experienceAmount);
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			experienceAmount = data;
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, Localization.MODULES.ATTACHMENTS.EXPERIENCE.translate(), 8, 6, 4210752);
	}

	private int[] getContainerRect() {
		return new int[] { 10, 15, 26, 65 };
	}

	private int[] getContentRect(final float part) {
		final int[] cont = getContainerRect();
		final int normalHeight = cont[3] - 4;
		final int currentHeight = (int) (normalHeight * part);
		return new int[] { cont[0] + 2, cont[1] + 2 + normalHeight - currentHeight, cont[2] - 4, currentHeight, normalHeight };
	}

	private void drawContent(final GuiMinecart gui, final int x, final int y, final int id) {
		final int lowerLevel = id * 1500 / 3;
		final int currentLevel = experienceAmount - lowerLevel;
		float part = 3.0f * currentLevel / 1500.0f;
		if (part > 1.0f) {
			part = 1.0f;
		}
		final int[] content = getContentRect(part);
		drawImage(gui, content, 4 + content[2] * (id + 1), content[4] - content[3]);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/experience.png");
		for (int i = 0; i < 3; ++i) {
			drawContent(gui, x, y, i);
		}
		drawImage(gui, getContainerRect(), 0, inRect(x, y, getContainerRect()) ? 65 : 0);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (inRect(x, y, getContainerRect())) {
			sendPacket(0);
		}
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public boolean hasSlots() {
		return false;
	}

	@Override
	public int guiWidth() {
		return 70;
	}

	@Override
	public int guiHeight() {
		return 84;
	}

	@Override
	protected int numberOfPackets() {
		return 1;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) {
		if (id == 0) {
			player.addExperience(Math.min(experienceAmount, 50));
			experienceAmount -= Math.min(experienceAmount, 50);
		}
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		experienceAmount = tagCompound.getShort(generateNBTName("Experience", id));
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setShort(generateNBTName("Experience", id), (short) experienceAmount);
	}
}
