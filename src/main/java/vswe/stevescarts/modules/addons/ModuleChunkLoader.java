package vswe.stevescarts.modules.addons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.IActivatorModule;

import java.io.DataInput;

public class ModuleChunkLoader extends ModuleAddon implements IActivatorModule {
	private boolean rdyToInit;
	private int[] buttonRect;
	private DataParameter<Boolean> LOADING_CHUNK;

	public ModuleChunkLoader(final EntityMinecartModular cart) {
		super(cart);
		buttonRect = new int[] { 20, 20, 24, 12 };
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
		drawString(gui, "Chunk Loader", 8, 6, 4210752);
	}

	@Override
	public void update() {
		super.update();
		if (!rdyToInit) {
			rdyToInit = true;
		}
		if (isLoadingChunk() && !getCart().hasFuelForModule() && !getCart().world.isRemote) {
			setChunkLoading(false);
		}
	}

	public void setChunkLoading(final boolean val) {
		if (!isPlaceholder()) {
			updateDw(LOADING_CHUNK, val);
			if (!getCart().world.isRemote && rdyToInit) {
				if (val) {
					getCart().initChunkLoading();
				} else {
					getCart().dropChunkLoading();
				}
			}
		}
	}

	private boolean isLoadingChunk() {
		return !isPlaceholder() && getDw(LOADING_CHUNK);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/chunk.png");
		final int imageID = isLoadingChunk() ? 1 : 0;
		int borderID = 0;
		if (inRect(x, y, buttonRect)) {
			borderID = 1;
		}
		drawImage(gui, buttonRect, 0, buttonRect[3] * borderID);
		final int srcY = buttonRect[3] * 2 + imageID * (buttonRect[3] - 2);
		drawImage(gui, buttonRect[0] + 1, buttonRect[1] + 1, 0, srcY, buttonRect[2] - 2, buttonRect[3] - 2);
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		drawStringOnMouseOver(gui, getStateName(), x, y, buttonRect);
	}

	private String getStateName() {
		if (!isLoadingChunk()) {
			return "Activate chunk loading";
		}
		return "Deactivate chunk loading";
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (button == 0 && inRect(x, y, buttonRect)) {
			sendPacket(0);
		}
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) {
		if (id == 0) {
			setChunkLoading(!isLoadingChunk());
		}
	}

	@Override
	public int numberOfPackets() {
		return 1;
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	public void initDw() {
		LOADING_CHUNK = createDw(DataSerializers.BOOLEAN);
		registerDw(LOADING_CHUNK, false);
	}

	@Override
	public int getConsumption(final boolean isMoving) {
		return isLoadingChunk() ? 5 : super.getConsumption(isMoving);
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setBoolean(generateNBTName("ChunkLoading", id), isLoadingChunk());
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		setChunkLoading(tagCompound.getBoolean(generateNBTName("ChunkLoading", id)));
	}

	@Override
	public void doActivate(final int id) {
		setChunkLoading(true);
	}

	@Override
	public void doDeActivate(final int id) {
		setChunkLoading(false);
	}

	@Override
	public boolean isActive(final int id) {
		return isLoadingChunk();
	}
}
