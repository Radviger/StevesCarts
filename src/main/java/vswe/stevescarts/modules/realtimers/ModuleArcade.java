package vswe.stevescarts.modules.realtimers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.arcade.ArcadeGame;
import vswe.stevescarts.arcade.invaders.ArcadeInvaders;
import vswe.stevescarts.arcade.sweeper.ArcadeSweeper;
import vswe.stevescarts.arcade.tetris.ArcadeTetris;
import vswe.stevescarts.arcade.tracks.ArcadeTracks;
import vswe.stevescarts.arcade.tracks.TrackStory;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

public class ModuleArcade extends ModuleBase {
	private ArrayList<ArcadeGame> games;
	private ArcadeGame currentGame;
	private int afkTimer;

	public ModuleArcade(final EntityMinecartModular cart) {
		super(cart);
		(games = new ArrayList<>()).add(new ArcadeTracks(this));
		games.add(new ArcadeTetris(this));
		games.add(new ArcadeInvaders(this));
		games.add(new ArcadeSweeper(this));
	}

	private boolean isGameActive() {
		return getCart().world.isRemote && currentGame != null;
	}

	@Override
	public boolean doStealInterface() {
		return isGameActive();
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
		return 190;
	}

	@Override
	public int guiHeight() {
		return 115;
	}

	@Override
	public void update() {
		if (isGameActive() && afkTimer < 10) {
			currentGame.update();
			++afkTimer;
		}
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		if (isGameActive()) {
			currentGame.drawForeground(gui);
		} else {
			drawString(gui, getModuleName(), 8, 6, 4210752);
			for (int i = 0; i < games.size(); ++i) {
				final int[] text = getButtonTextArea(i);
				if (text[3] == 8) {
					drawString(gui, games.get(i).getName(), text[0], text[1], 4210752);
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/arcade.png");
		afkTimer = 0;
		if (isGameActive()) {
			final int[] rect = getExitArea();
			final int srcX = 0;
			final int srcY = 104 + (inRect(x, y, rect) ? 16 : 0);
			drawImage(gui, rect, srcX, srcY);
			currentGame.drawBackground(gui, x, y);
		} else {
			final int[] rect = getListArea();
			drawImage(gui, rect, 0, 0);
			for (int i = 0; i < games.size(); ++i) {
				final int[] button = getButtonGraphicArea(i);
				final int srcX2 = 0;
				final int srcY2 = 136 + (inRect(x, y, getButtonBoundsArea(i)) ? button[3] : 0);
				if (button[3] > 0) {
					drawImage(gui, button, srcX2, srcY2);
					final int[] icon = getButtonIconArea(i);
					drawImage(gui, icon, i * 16, rect[3]);
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		if (isGameActive()) {
			drawStringOnMouseOver(gui, "Exit", x, y, getExitArea());
			currentGame.drawMouseOver(gui, x, y);
		}
	}

	private int[] getExitArea() {
		return new int[] { 455, 6, 16, 16 };
	}

	private int[] getListArea() {
		return new int[] { 15, 20, 170, 88 };
	}

	private int[] getButtonBoundsArea(final int i) {
		return getButtonArea(i, false);
	}

	private int[] getButtonGraphicArea(final int i) {
		return getButtonArea(i, true);
	}

	private int[] getButtonArea(final int i, final boolean graphic) {
		final int[] list = getListArea();
		return new int[] { list[0] + 2, list[1] + 2 + i * 21, 166, graphic ? 21 : 20 };
	}

	private int[] getButtonTextArea(final int i) {
		final int[] button = getButtonGraphicArea(i);
		return new int[] { button[0] + 24, button[1] + 6, button[2], 8 };
	}

	private int[] getButtonIconArea(final int i) {
		final int[] button = getButtonGraphicArea(i);
		return new int[] { button[0] + 2, button[1] + 2, 16, 16 };
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int button) {
		if (isGameActive()) {
			if (button == 0 && inRect(x, y, getExitArea())) {
				currentGame.unload(gui);
				currentGame = null;
			} else {
				currentGame.mouseClicked(gui, x, y, button);
			}
		} else if (button == 0) {
			for (int i = 0; i < games.size(); ++i) {
				if (inRect(x, y, getButtonBoundsArea(i))) {
					(currentGame = games.get(i)).load(gui);
					break;
				}
			}
		}
	}

	@Override
	public void mouseMovedOrUp(final GuiMinecart gui, final int x, final int y, final int button) {
		if (isGameActive()) {
			currentGame.mouseMovedOrUp(gui, x, y, button);
		}
	}

	@Override
	public void keyPress(final GuiMinecart gui, final char character, final int extraInformation) {
		if (isGameActive()) {
			currentGame.keyPress(gui, character, extraInformation);
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		for (final ArcadeGame game : games) {
			game.Save(tagCompound, id);
		}
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		for (final ArcadeGame game : games) {
			game.Load(tagCompound, id);
		}
	}

	@Override
	public int numberOfPackets() {
		return 4;
	}

	@Override
	protected void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		for (final ArcadeGame game : games) {
			game.receivePacket(id, reader, player);
		}
	}

	@Override
	public int numberOfGuiData() {
		return TrackStory.stories.size() + 5;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		for (final ArcadeGame game : games) {
			game.checkGuiData(info);
		}
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		for (final ArcadeGame game : games) {
			game.receiveGuiData(id, data);
		}
	}

	@Override
	public boolean disableStandardKeyFunctionality() {
		return currentGame != null && currentGame.disableStandardKeyFunctionality();
	}
}
