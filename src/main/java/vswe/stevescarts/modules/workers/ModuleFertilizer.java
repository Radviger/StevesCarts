package vswe.stevescarts.modules.workers;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotFertilizer;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ISuppliesModule;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.workers.tools.ModuleFarmer;

import javax.annotation.Nonnull;
import java.util.Random;

public class ModuleFertilizer extends ModuleWorker implements ISuppliesModule {
	private int tankPosX;
	private int tankPosY;
	private int range;
	private int fert;
	private final int fertPerBonemeal = 4;
	private final int maxStacksOfBones = 1;
	private final Random random = new Random();

	public ModuleFertilizer(final EntityMinecartModular cart) {
		super(cart);
		tankPosX = guiWidth() - 21;
		tankPosY = 20;
		range = 1;
		fert = 0;
	}

	@Override
	public byte getWorkPriority() {
		return 127;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	protected int getInventoryWidth() {
		return 1;
	}

	@Override
	public void init() {
		super.init();
		for (final ModuleBase module : getCart().getModules()) {
			if (module instanceof ModuleFarmer) {
				range = ((ModuleFarmer) module).getExternalRange();
				break;
			}
		}
	}

	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/fertilize.png");
		drawImage(gui, tankPosX, tankPosY, 0, 0, 18, 27);
		final float percentage = fert / getMaxFert();
		final int size = (int) (percentage * 23.0f);
		drawImage(gui, tankPosX + 2, tankPosY + 2 + (23 - size), 18, 23 - size, 14, size);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, Localization.MODULES.ATTACHMENTS.FERTILIZERS.translate() + ": " + fert + " / " + getMaxFert(), x, y, tankPosX, tankPosY, 18, 27);
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public int guiWidth() {
		return super.guiWidth() + 25;
	}

	@Override
	public int guiHeight() {
		return Math.max(super.guiHeight(), 50);
	}

	@Override
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotFertilizer(getCart(), slotId, 8 + x * 18, 23 + y * 18);
	}

	@Override
	public WorkResult work() {
		World world = getCart().world;
		BlockPos next = getNextblock();
		for (int i = -range; i <= range; ++i) {
			label:
			for (int j = -range; j <= range; ++j) {
				WorkResult r = fertilize(world, next.add(i, 0, j));
				switch (r) {
					case SUCCESS:
						if (random.nextInt(25) == 0) {
							break label;
						}
						break;
					case FAILURE:
						return r;
				}
			}
		}
		return WorkResult.SKIP;
	}

	private WorkResult fertilize(World world, BlockPos pos) {
		IBlockState stateOfTopBlock = world.getBlockState(pos);
		Block blockTop = stateOfTopBlock.getBlock();
		if (fert > 0) {
			FakePlayer player = getCartOwner();
			if (blockTop instanceof IGrowable) {
				IGrowable growable = (IGrowable) blockTop;
				if (growable.canGrow(world, pos, stateOfTopBlock, false)) {
					if (growable.canUseBonemeal(world, getCart().rand, pos, stateOfTopBlock)) {
						PlayerInteractEvent e = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.UP, Vec3d.ZERO);
						if (!MinecraftForge.EVENT_BUS.post(e)) {
							growable.grow(world, getCart().rand, pos, stateOfTopBlock);
							fert -= 2;
							return WorkResult.SUCCESS;
						} else {
							return WorkResult.FAILURE;
						}
					}
				}
			}
		}
		return WorkResult.SKIP;
	}

	@Override
	public int numberOfGuiData() {
		return 1;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		updateGuiData(info, 0, (short) fert);
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			fert = data;
		}
	}

	@Override
	public void update() {
		super.update();
		loadSupplies();
	}

	private void loadSupplies() {
		if (getCart().world.isRemote) {
			return;
		}
		if (!getStack(0).isEmpty()) {
			final boolean isBone = getStack(0).getItem() == Items.BONE;
			final boolean isBoneMeal = getStack(0).getItem() == Items.DYE && getStack(0).getItemDamage() == 15;
			if (isBone || isBoneMeal) {
				int amount;
				if (isBoneMeal) {
					amount = 1;
				} else {
					amount = 3;
				}
				if (fert <= 4 * (192 - amount) && getStack(0).getCount() > 0) {
					@Nonnull
					ItemStack stack = getStack(0);
					stack.shrink(1);
					fert += amount * 4;
				}
				if (getStack(0).getCount() == 0) {
					setStack(0, ItemStack.EMPTY);
				}
			}
		}
	}

	private int getMaxFert() {
		return 768;
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setShort(generateNBTName("Fert", id), (short) fert);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		fert = tagCompound.getShort(generateNBTName("Fert", id));
	}

	@Override
	public boolean haveSupplies() {
		return fert > 0;
	}
}
