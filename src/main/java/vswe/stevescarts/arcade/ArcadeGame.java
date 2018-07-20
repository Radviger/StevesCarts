package vswe.stevescarts.arcade;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.SCConfig;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.handlers.SoundHandler;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.modules.realtimers.ModuleArcade;

import java.io.DataInput;
import java.io.IOException;

public abstract class ArcadeGame {
	private ModuleArcade module;
	private Localization.ARCADE name;

	public ArcadeGame(final ModuleArcade module, final Localization.ARCADE name) {
		this.name = name;
		this.module = module;
	}

	public String getName() {
		return name.translate();
	}

	public ModuleArcade getModule() {
		return module;
	}

	@SideOnly(Side.CLIENT)
	public void update() {
		if (SCConfig.useArcadeSounds) {
			getModule().getCart().silent();
		}
	}

	@SideOnly(Side.CLIENT)
	public void drawForeground(final GuiMinecart gui) {
	}

	@SideOnly(Side.CLIENT)
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
	}

	@SideOnly(Side.CLIENT)
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
	}

	@SideOnly(Side.CLIENT)
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
	}

	@SideOnly(Side.CLIENT)
	public void mouseMovedOrUp(final GuiMinecart gui, final int x, final int y, final int button) {
	}

	@SideOnly(Side.CLIENT)
	public void keyPress(final GuiMinecart gui, final char character, final int extraInformation) {
	}

	public void Save(final NBTTagCompound tagCompound, final int id) {
	}

	public void Load(final NBTTagCompound tagCompound, final int id) {
	}

	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
	}

	public void checkGuiData(final Object[] info) {
	}

	public void receiveGuiData(final int id, final short data) {
	}

	public boolean disableStandardKeyFunctionality() {
		return false;
	}

	@SideOnly(Side.CLIENT)
	public static void playSound(SoundEvent sound, float volume, float pitch) {
		if (SCConfig.useArcadeSounds && sound != null) {
			SoundHandler.playSound(sound, SoundCategory.BLOCKS, volume, pitch);
		}
	}

	public boolean allowKeyRepeat() {
		return false;
	}

	public void load(final GuiMinecart gui) {
		gui.enableKeyRepeat(allowKeyRepeat());
	}

	public void unload(final GuiMinecart gui) {
		if (allowKeyRepeat()) {
			gui.enableKeyRepeat(false);
		}
	}

	public void drawImageInArea(final GuiMinecart gui, final int x, final int y, final int u, final int v, final int w, final int h) {
		drawImageInArea(gui, x, y, u, v, w, h, 5, 4, 443, 168);
	}

	public void drawImageInArea(final GuiMinecart gui, int x, int y, int u, int v, int w, int h, final int x1, final int y1, final int x2, final int y2) {
		if (x < x1) {
			w -= x1 - x;
			u += x1 - x;
			x = x1;
		} else if (x + w > x2) {
			w = x2 - x;
		}
		if (y < y1) {
			h -= y1 - y;
			v += y1 - y;
			y = y1;
		} else if (y + h > y2) {
			h = y2 - y;
		}
		if (w > 0 && h > 0) {
			getModule().drawImage(gui, x, y, u, v, w, h);
		}
	}
}
