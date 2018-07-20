package vswe.stevescarts.modules.workers;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import vswe.stevescarts.containers.slots.SlotBase;
import vswe.stevescarts.containers.slots.SlotTorch;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ISuppliesModule;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.IOException;

public class ModuleTorch extends ModuleWorker implements ISuppliesModule {
	private int light;
	private int lightLimit;
	private int[] boxRect;
	boolean markerMoving;
	private DataParameter<Integer> TORCHES;

	public ModuleTorch(final EntityMinecartModular cart) {
		super(cart);
		lightLimit = 8;
		boxRect = new int[] { 12, guiHeight() - 10, 46, 9 };
		markerMoving = false;
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
	protected SlotBase getSlot(final int slotId, final int x, final int y) {
		return new SlotTorch(getCart(), slotId, 8 + x * 18, 23 + y * 18);
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
	}

	@Override
	public byte getWorkPriority() {
		return 95;
	}

	@Override
	public boolean work() {
		final BlockPos next = getLastblock();
		final EntityMinecartModular cart = getCart();
		final World world = cart.world;
		final int x = next.getX();
		final int y = next.getY();
		final int z = next.getZ();
		final int cartX = cart.x();
		final int cartZ = cart.z();
		if (light <= lightLimit) {
			for (int side = -1; side <= 1; side += 2) {
				final int xTorch = x + ((cartZ != z) ? side : 0);
				final int zTorch = z + ((cartX != x) ? side : 0);
				for (int level = 2; level >= -2; level--) {
					BlockPos pos = new BlockPos(xTorch, y + level, zTorch);
					if (world.isAirBlock(pos) && Blocks.TORCH.canPlaceBlockAt(world, pos)) {
						int i = 0;
						while (i < getInventorySize()) {
							if (!getStack(i).isEmpty() && Block.getBlockFromItem(getStack(i).getItem()) == Blocks.TORCH) {
								if (doPreWork()) {
									startWorking(3);
									return true;
								}
								IBlockState state = Blocks.TORCH.getStateForPlacement(world, pos, EnumFacing.DOWN, 0, 0, 0, 0, getFakePlayer(), EnumHand.MAIN_HAND);
								world.setBlockState(new BlockPos(xTorch, y + level, zTorch), state);
								if (!cart.hasCreativeSupplies()) {
									@Nonnull
									ItemStack stack = getStack(i);
									stack.shrink(1);
									if (getStack(i).getCount() == 0) {
										setStack(i, ItemStack.EMPTY);
									}
									onInventoryChanged();
									break;
								}
								break;
							} else {
								++i;
							}
						}
						break;
					} else if (world.getBlockState(pos).getBlock() == Blocks.TORCH) {
						break;
					}
				}
			}
		}
		stopWorking();
		return false;
	}

	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/torch.png");
		int barLength = 3 * light;
		if (light == 15) {
			--barLength;
		}
		int srcX = 0;
		if (inRect(x, y, boxRect)) {
			srcX += boxRect[2];
		}
		drawImage(gui, boxRect, srcX, 0);
		drawImage(gui, 13, guiHeight() - 10 + 1, 0, 9, barLength, 7);
		drawImage(gui, 12 + 3 * lightLimit, guiHeight() - 10, 0, 16, 1, 9);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, "Threshold: " + lightLimit + " Current: " + light, x, y, boxRect);
	}

	@Override
	public int guiHeight() {
		return super.guiHeight() + 10;
	}

	@Override
	public int numberOfGuiData() {
		return 2;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		short data = (short) (light & 0xF);
		data |= (short) ((lightLimit & 0xF) << 4);
		updateGuiData(info, 0, data);
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == 0) {
			light = (data & 0xF);
			lightLimit = (data & 0xF0) >> 4;
		}
	}

	@Override
	public int numberOfPackets() {
		return 1;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 0) {
			lightLimit = reader.readByte();
			if (lightLimit < 0) {
				lightLimit = 0;
			} else if (lightLimit > 15) {
				lightLimit = 15;
			}
		}
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (button == 0 && inRect(x, y, boxRect)) {
			generatePacket(x, y);
			markerMoving = true;
		}
	}

	@Override
	public void mouseMovedOrUp(final GuiMinecart gui, final int x, final int y, final int button) {
		if (markerMoving) {
			generatePacket(x, y);
		}
		if (button != -1) {
			markerMoving = false;
		}
	}

	private void generatePacket(final int x, final int y) {
		final int xInBox = x - boxRect[0];
		int val = xInBox / 3;
		if (val < 0) {
			val = 0;
		} else if (val > 15) {
			val = 15;
		}
		sendPacket(0, (byte) val);
	}

	public void setThreshold(final byte val) {
		lightLimit = val;
	}

	public int getThreshold() {
		return lightLimit;
	}

	public int getLightLevel() {
		return light;
	}

	@Override
	public void update() {
		super.update();
		light = getCart().world.getLightFor(EnumSkyBlock.BLOCK, new BlockPos(getCart().x(), getCart().y() + 1, getCart().z()));
	}

	@Override
	public void initDw() {
		TORCHES = createDw(DataSerializers.VARINT);
		registerDw(TORCHES, 0);
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	public void onInventoryChanged() {
		super.onInventoryChanged();
		calculateTorches();
	}

	private void calculateTorches() {
		if (getCart().world.isRemote) {
			return;
		}
		int val = 0;
		for (int i = 0; i < 3; ++i) {
			val |= (!(getStack(i).isEmpty()) ? 1 : 0) << i;
		}
		updateDw(TORCHES, val);
	}

	public int getTorches() {
		if (isPlaceholder()) {
			return getSimInfo().getTorchInfo();
		}
		return getDw(TORCHES);
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setByte(generateNBTName("lightLimit", id), (byte) lightLimit);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		lightLimit = tagCompound.getByte(generateNBTName("lightLimit", id));
		calculateTorches();
	}

	@Override
	public boolean haveSupplies() {
		for (int i = 0; i < getInventorySize(); ++i) {
			@Nonnull
			ItemStack item = getStack(i);
			if (item != null && Block.getBlockFromItem(item.getItem()) == Blocks.TORCH) {
				return true;
			}
		}
		return false;
	}
}
