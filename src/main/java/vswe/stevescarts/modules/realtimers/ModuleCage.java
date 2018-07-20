package vswe.stevescarts.modules.realtimers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.IActivatorModule;
import vswe.stevescarts.modules.ModuleBase;

import java.io.DataInput;
import java.util.Comparator;
import java.util.List;

public class ModuleCage extends ModuleBase implements IActivatorModule {
	private int[] autoRect;
	private int[] manualRect;
	private EntityNearestTarget sorter;
	private int cooldown;
	private boolean disablePickup;

	public ModuleCage(final EntityMinecartModular cart) {
		super(cart);
		autoRect = new int[] { 15, 20, 24, 12 };
		manualRect = new int[] { autoRect[0] + autoRect[2] + 5, autoRect[1], autoRect[2], autoRect[3] };
		sorter = new EntityNearestTarget(getCart());
		cooldown = 0;
	}

	@Override
	public boolean hasSlots() {
		return false;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public int guiWidth() {
		return 80;
	}

	@Override
	public int guiHeight() {
		return 35;
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/cage.png");
		drawButton(gui, x, y, autoRect, disablePickup ? 2 : 3);
		drawButton(gui, x, y, manualRect, isCageEmpty() ? 0 : 1);
	}

	private void drawButton(final GuiMinecart gui, final int x, final int y, final int[] coords, final int imageID) {
		if (inRect(x, y, coords)) {
			drawImage(gui, coords, 0, coords[3]);
		} else {
			drawImage(gui, coords, 0, 0);
		}
		final int srcY = coords[3] * 2 + imageID * (coords[3] - 2);
		drawImage(gui, coords[0] + 1, coords[1] + 1, 0, srcY, coords[2] - 2, coords[3] - 2);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, Localization.MODULES.ATTACHMENTS.CAGE_AUTO.translate(disablePickup ? "0" : "1"), x, y, autoRect);
		drawStringOnMouseOver(gui, Localization.MODULES.ATTACHMENTS.CAGE.translate(isCageEmpty() ? "0" : "1"), x, y, manualRect);
	}

	private boolean isCageEmpty() {
		return getCart().getRidingEntity() == null;
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (button == 0) {
			if (inRect(x, y, autoRect)) {
				sendPacket(0);
			} else if (inRect(x, y, manualRect)) {
				sendPacket(1);
			}
		}
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) {
		if (id == 0) {
			disablePickup = !disablePickup;
		} else if (id == 1) {
			if (!isCageEmpty()) {
				manualDrop();
			} else {
				manualPickUp();
			}
		}
	}

	@Override
	public int numberOfPackets() {
		return 2;
	}

	@Override
	public void update() {
		super.update();
		if (cooldown > 0) {
			--cooldown;
		} else if (!disablePickup) {
			pickUpCreature(2);
			cooldown = 20;
		}
	}

	private void manualDrop() {
		if (!isCageEmpty()) {
			getCart().startRiding((Entity) null);
			cooldown = 20;
		}
	}

	private void manualPickUp() {
		pickUpCreature(5);
	}

	private void pickUpCreature(final int searchDistance) {
		if (getCart().world.isRemote || !isCageEmpty()) {
			return;
		}
		final List<EntityLivingBase> entities = getCart().world.getEntitiesWithinAABB(EntityLivingBase.class, getCart().getEntityBoundingBox().grow(searchDistance, 4.0, searchDistance));
		entities.sort(sorter);
		for (EntityLivingBase target : entities) {
			if (!(target instanceof EntityPlayer) && !(target instanceof EntityIronGolem) && !(target instanceof EntityDragon) && !(target instanceof EntitySlime) && !(target instanceof EntityWaterMob) && !(target instanceof EntityWither) && !(target instanceof EntityEnderman) && (!(target instanceof EntitySpider) || target instanceof EntityCaveSpider) && !(target instanceof EntityGiantZombie) && !(target instanceof EntityFlying)) {
				if (target.getRidingEntity() == null) {
					target.startRiding(getCart());
					return;
				}
				continue;
			}
		}
	}

	@Override
	public float mountedOffset(final Entity rider) {
		if (rider instanceof EntityBat) {
			return 0.5f;
		}
		if (rider instanceof EntityZombie || rider instanceof EntitySkeleton) {
			return -0.75f;
		}
		return super.mountedOffset(rider);
	}

	@Override
	public int numberOfGuiData() {
		return 1;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		updateGuiData(info, 0, (byte) (disablePickup ? 1 : 0));
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			disablePickup = (data != 0);
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setBoolean(generateNBTName("disablePickup", id), disablePickup);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		disablePickup = tagCompound.getBoolean(generateNBTName("disablePickup", id));
	}

	@Override
	public boolean isActive(final int id) {
		if (id == 0) {
			return !disablePickup;
		}
		return !isCageEmpty();
	}

	@Override
	public void doActivate(final int id) {
		if (id == 0) {
			disablePickup = false;
		} else {
			manualPickUp();
		}
	}

	@Override
	public void doDeActivate(final int id) {
		if (id == 0) {
			disablePickup = true;
		} else {
			manualDrop();
		}
	}

	private static class EntityNearestTarget implements Comparator {
		private Entity entity;

		public EntityNearestTarget(final Entity entity) {
			this.entity = entity;
		}

		public int compareDistanceSq(final Entity entity1, final Entity entity2) {
			final double distance1 = entity.getDistanceSq(entity1);
			final double distance2 = entity.getDistanceSq(entity2);
			return (distance1 < distance2) ? -1 : ((distance1 > distance2) ? 1 : 0);
		}

		@Override
		public int compare(final Object obj1, final Object obj2) {
			return compareDistanceSq((Entity) obj1, (Entity) obj2);
		}
	}
}
