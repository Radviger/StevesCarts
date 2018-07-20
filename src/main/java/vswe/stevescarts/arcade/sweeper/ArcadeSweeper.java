package vswe.stevescarts.arcade.sweeper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.arcade.ArcadeGame;
import vswe.stevescarts.arcade.tracks.TrackStory;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.handlers.SoundHandler;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.realtimers.ModuleArcade;

import java.io.DataInput;
import java.io.IOException;

public class ArcadeSweeper extends ArcadeGame {
	private Tile[][] tiles;
	protected boolean isPlaying;
	protected boolean hasFinished;
	private int currentGameType;
	private int ticks;
	protected int creepersLeft;
	protected int emptyLeft;
	private boolean hasStarted;
	private int[] highscore;
	private int highscoreTicks;
	private static String textureMenu = "/gui/sweeper.png";

	public ArcadeSweeper(final ModuleArcade module) {
		super(module, Localization.ARCADE.CREEPER);
		highscore = new int[] { 999, 999, 999 };
		newGame(currentGameType);
	}

	private void newGame(final int size) {
		switch (size) {
			case 0: {
				newGame(9, 9, 10);
				break;
			}
			case 1: {
				newGame(16, 16, 40);
				break;
			}
			case 2: {
				newGame(30, 16, 99);
				break;
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void update() {
		super.update();
		if (hasStarted && isPlaying && !hasFinished && ticks < 19980) {
			++ticks;
		}
		if (highscoreTicks > 0) {
			++highscoreTicks;
			if (highscoreTicks == 78) {
				highscoreTicks = 0;
				ArcadeGame.playSound(SoundHandler.HIGH_SCORE, 1.0f, 1.0f);
			}
		}
	}

	private void newGame(final int width, final int height, final int totalCreepers) {
		isPlaying = true;
		ticks = 0;
		creepersLeft = totalCreepers;
		emptyLeft = width * height - totalCreepers;
		hasStarted = false;
		hasFinished = false;
		highscoreTicks = 0;
		tiles = new Tile[width][height];
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				tiles[x][y] = new Tile(this);
			}
		}
		for (int creepers = 0; creepers < totalCreepers; ++creepers) {
			final int x2 = getModule().getCart().rand.nextInt(width);
			final int y2 = getModule().getCart().rand.nextInt(height);
			if (!tiles[x2][y2].isCreeper()) {
				tiles[x2][y2].setCreeper();
			}
		}
		for (int x2 = 0; x2 < width; ++x2) {
			for (int y2 = 0; y2 < height; ++y2) {
				if (!tiles[x2][y2].isCreeper()) {
					int count = 0;
					for (int i = -1; i <= 1; ++i) {
						for (int j = -1; j <= 1; ++j) {
							if (i != 0 || j != 0) {
								final int x3 = x2 + i;
								final int y3 = y2 + j;
								if (x3 >= 0 && y3 >= 0 && x3 < width && y3 < height && tiles[x3][y3].isCreeper()) {
									++count;
								}
							}
						}
					}
					tiles[x2][y2].setNearbyCreepers(count);
				}
			}
		}
	}

	private int getMarginLeft() {
		return (443 - tiles.length * 10) / 2;
	}

	private int getMarginTop() {
		return (168 - tiles[0].length * 10) / 2;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource(ArcadeSweeper.textureMenu);
		for (int i = 0; i < tiles.length; ++i) {
			for (int j = 0; j < tiles[0].length; ++j) {
				tiles[i][j].draw(this, gui, getMarginLeft() + i * 10, getMarginTop() + j * 10, x, y);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void mouseClicked(final GuiMinecart gui, int x, int y, final int button) {
		if (!isPlaying) {
			return;
		}
		x -= getMarginLeft();
		y -= getMarginTop();
		final int xc = x / 10;
		final int yc = y / 10;
		if (button == 0) {
			openTile(xc, yc, true);
		} else if (button == 1 && isValidCoordinate(xc, yc)) {
			hasStarted = true;
			ArcadeGame.playSound(SoundHandler.FLAG_CLICK, 1.0f, 1.0f);
			tiles[xc][yc].mark();
		} else if (button == 2 && isValidCoordinate(xc, yc) && tiles[xc][yc].getState() == Tile.TILE_STATE.OPENED) {
			ArcadeGame.playSound(SoundHandler.CLICK, 1.0f, 1.0f);
			int nearby = tiles[xc][yc].getNearbyCreepers();
			if (nearby != 0) {
				for (int i = -1; i <= 1; ++i) {
					for (int j = -1; j <= 1; ++j) {
						if ((i != 0 || j != 0) && isValidCoordinate(xc + i, yc + j) && tiles[xc + i][yc + j].getState() == Tile.TILE_STATE.FLAGGED) {
							--nearby;
						}
					}
				}
				if (nearby == 0) {
					for (int i = -1; i <= 1; ++i) {
						for (int j = -1; j <= 1; ++j) {
							if (i != 0 || j != 0) {
								openTile(xc + i, yc + j, false);
							}
						}
					}
				}
			}
		}
	}

	private boolean isValidCoordinate(final int x, final int y) {
		return x >= 0 && y >= 0 && x < tiles.length && y < tiles[0].length;
	}

	private void openTile(final int x, final int y, final boolean first) {
		if (isValidCoordinate(x, y)) {
			hasStarted = true;
			final Tile.TILE_OPEN_RESULT result = tiles[x][y].open();
			if (emptyLeft == 0) {
				hasFinished = true;
				isPlaying = false;
				ArcadeGame.playSound(SoundHandler.GOOD_JOB, 1.0f, 1.0f);
				if (highscore[currentGameType] > ticks / 20) {
					highscoreTicks = 1;
					final int val = ticks / 20;
					getModule().sendPacket(3, o -> {
						o.writeByte((byte)currentGameType);
						o.writeInt(val);
					});
				}
			} else if (result == Tile.TILE_OPEN_RESULT.BLOB) {
				if (first) {
					ArcadeGame.playSound(SoundHandler.BLOB_CLICK, 1.0f, 1.0f);
				}
				for (int i = -1; i <= 1; ++i) {
					for (int j = -1; j <= 1; ++j) {
						openTile(x + i, y + j, false);
					}
				}
			} else if (result == Tile.TILE_OPEN_RESULT.DEAD) {
				isPlaying = false;
				ArcadeGame.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, (1.0f + (getModule().getCart().rand.nextFloat() - getModule().getCart().rand.nextFloat()) * 0.2f) * 0.7f);
			} else if (result == Tile.TILE_OPEN_RESULT.OK && first) {
				ArcadeGame.playSound(SoundHandler.CLICK, 1.0f, 1.0f);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void keyPress(final GuiMinecart gui, final char character, final int extraInformation) {
		if (Character.toLowerCase(character) == 'r') {
			newGame(currentGameType);
		} else if (Character.toLowerCase(character) == 't') {
			newGame(currentGameType = (currentGameType + 1) % 3);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawForeground(final GuiMinecart gui) {
		final String[] mapnames = { Localization.ARCADE.MAP_1.translate(), Localization.ARCADE.MAP_2.translate(), Localization.ARCADE.MAP_3.translate() };
		getModule().drawString(gui, Localization.ARCADE.LEFT.translate(String.valueOf(creepersLeft)), 10, 180, 4210752);
		getModule().drawString(gui, Localization.ARCADE.TIME.translate(String.valueOf(ticks / 20)), 10, 190, 4210752);
		getModule().drawString(gui, "R - " + Localization.ARCADE.INSTRUCTION_RESTART.translate(), 10, 210, 4210752);
		getModule().drawString(gui, "T - " + Localization.ARCADE.INSTRUCTION_CHANGE_MAP.translate(), 10, 230, 4210752);
		getModule().drawString(gui, Localization.ARCADE.MAP.translate(mapnames[currentGameType]), 10, 240, 4210752);
		getModule().drawString(gui, Localization.ARCADE.HIGH_SCORES.translate(), 330, 180, 4210752);
		for (int i = 0; i < 3; ++i) {
			getModule().drawString(gui, Localization.ARCADE.HIGH_SCORE_ENTRY.translate(mapnames[i], String.valueOf(highscore[i])), 330, 190 + i * 10, 4210752);
		}
	}

	@Override
	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 3) {
			highscore[reader.readByte()] = reader.readInt();
		}
	}

	@Override
	public void checkGuiData(final Object[] info) {
		for (int i = 0; i < 3; ++i) {
			getModule().updateGuiData(info, TrackStory.stories.size() + 2 + i, (short) highscore[i]);
		}
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id >= TrackStory.stories.size() + 2 && id < TrackStory.stories.size() + 5) {
			highscore[id - (TrackStory.stories.size() + 2)] = data;
		}
	}

	@Override
	public void Save(final NBTTagCompound tagCompound, final int id) {
		for (int i = 0; i < 3; ++i) {
			tagCompound.setShort(getModule().generateNBTName("HighscoreSweeper" + i, id), (short) highscore[i]);
		}
	}

	@Override
	public void Load(final NBTTagCompound tagCompound, final int id) {
		for (int i = 0; i < 3; ++i) {
			highscore[i] = tagCompound.getShort(getModule().generateNBTName("HighscoreSweeper" + i, id));
		}
	}
}
