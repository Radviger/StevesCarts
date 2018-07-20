package vswe.stevescarts.arcade.invaders;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import vswe.stevescarts.arcade.ArcadeGame;
import vswe.stevescarts.arcade.tracks.TrackStory;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.handlers.SoundHandler;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.realtimers.ModuleArcade;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

public class ArcadeInvaders extends ArcadeGame {
	protected ArrayList<Unit> invaders;
	private ArrayList<Player> lives;
	private ArrayList<Unit> buildings;
	protected ArrayList<Projectile> projectiles;
	private Player player;
	protected int moveDirection;
	protected int moveSpeed;
	protected int moveDown;
	private int fireDelay;
	private int score;
	private int highscore;
	protected boolean hasPahighast;
	protected boolean canSpawnPahighast;
	private boolean newHighscore;
	private int gameoverCounter;
	private static String texture;
	private static final String[][] numbers;

	public ArcadeInvaders(final ModuleArcade module) {
		super(module, Localization.ARCADE.GHAST);
		invaders = new ArrayList<>();
		buildings = new ArrayList<>();
		lives = new ArrayList<>();
		projectiles = new ArrayList<>();
		start();
	}

	private void start() {
		buildings.clear();
		lives.clear();
		projectiles.clear();
		player = new Player(this);
		for (int i = 0; i < 3; ++i) {
			lives.add(new Player(this, 10 + i * 20, 190));
		}
		for (int i = 0; i < 4; ++i) {
			for (int j = 0; j < 3; ++j) {
				buildings.add(new Building(this, 48 + i * 96 + j * 16, 120));
			}
		}
		moveSpeed = 0;
		fireDelay = 0;
		score = 0;
		canSpawnPahighast = false;
		newHighscore = false;
		spawnInvaders();
	}

	private void spawnInvaders() {
		invaders.clear();
		hasPahighast = false;
		for (int j = 0; j < 3; ++j) {
			for (int i = 0; i < 14; ++i) {
				invaders.add(new InvaderGhast(this, 20 + i * 20, 10 + 25 * j));
			}
		}
		++moveSpeed;
		moveDirection = 1;
		moveDown = 0;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void update() {
		super.update();
		if (player != null) {
			if (player.ready) {
				boolean flag = false;
				boolean flag2 = false;
				for (int i = invaders.size() - 1; i >= 0; --i) {
					final Unit invader = invaders.get(i);
					final Unit.UPDATE_RESULT result = invader.update();
					if (result == Unit.UPDATE_RESULT.DEAD) {
						if (((InvaderGhast) invader).isPahighast) {
							hasPahighast = false;
						}
						ArcadeGame.playSound(SoundEvents.ENTITY_GHAST_HURT, 0.15f, 1.0f);
						invaders.remove(i);
						++score;
					} else if (result == Unit.UPDATE_RESULT.TURN_BACK) {
						flag = true;
					} else if (result == Unit.UPDATE_RESULT.GAME_OVER) {
						flag2 = true;
					}
				}
				if (moveDown > 0) {
					--moveDown;
				}
				if (flag) {
					moveDirection *= -1;
					moveDown = 5;
				}
				if (invaders.size() == 0 || (hasPahighast && invaders.size() == 1)) {
					score += (hasPahighast ? 200 : 50);
					canSpawnPahighast = true;
					spawnInvaders();
				}
				if (flag2) {
					lives.clear();
					projectiles.clear();
					player = null;
					newHighScore();
					return;
				}
				for (int i = buildings.size() - 1; i >= 0; --i) {
					if (buildings.get(i).update() == Unit.UPDATE_RESULT.DEAD) {
						buildings.remove(i);
					}
				}
				for (int i = projectiles.size() - 1; i >= 0; --i) {
					if (projectiles.get(i).update() == Unit.UPDATE_RESULT.DEAD) {
						projectiles.remove(i);
					}
				}
				if (Keyboard.isKeyDown(30)) {
					player.move(-1);
				} else if (Keyboard.isKeyDown(32)) {
					player.move(1);
				}
				if (fireDelay == 0 && Keyboard.isKeyDown(17)) {
					projectiles.add(new Projectile(this, player.x + 8 - 2, player.y - 15, true));
					ArcadeGame.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 0.8f, 1.0f / (getModule().getCart().rand.nextFloat() * 0.4f + 1.2f) + 0.5f);
					fireDelay = 10;
				} else if (fireDelay > 0) {
					--fireDelay;
				}
			}
			if (player.update() == Unit.UPDATE_RESULT.DEAD) {
				projectiles.clear();
				ArcadeGame.playSound(SoundHandler.HIT, 1.0f, 1.0f);
				if (lives.size() != 0) {
					lives.get(0).setTarget(player.x, player.y);
					player = lives.get(0);
					lives.remove(0);
				} else {
					player = null;
					newHighScore();
				}
			}
		} else if (gameoverCounter == 0) {
			boolean flag = false;
			for (int j = invaders.size() - 1; j >= 0; --j) {
				final Unit invader2 = invaders.get(j);
				if (invader2.update() == Unit.UPDATE_RESULT.TARGET) {
					flag = true;
				}
			}
			if (!flag) {
				gameoverCounter = 1;
			}
		} else if (newHighscore && gameoverCounter < 5) {
			++gameoverCounter;
			if (gameoverCounter == 5) {
				ArcadeGame.playSound(SoundHandler.HIGH_SCORE, 1.0f, 1.0f);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource(ArcadeInvaders.texture);
		for (int i = 0; i < 27; ++i) {
			getModule().drawImage(gui, 5 + i * 16, 150, 16, 32, 16, 16);
		}
		for (int i = 0; i < 5; ++i) {
			getModule().drawImage(gui, 3 + i * 16, 190, 16, 32, 16, 16);
		}
		for (final Unit invader : invaders) {
			invader.draw(gui);
		}
		if (player != null) {
			player.draw(gui);
		}
		for (final Unit player : lives) {
			player.draw(gui);
		}
		for (final Unit projectile : projectiles) {
			projectile.draw(gui);
		}
		for (final Unit building : buildings) {
			building.draw(gui);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawForeground(final GuiMinecart gui) {
		getModule().drawString(gui, Localization.ARCADE.EXTRA_LIVES.translate() + ":", 10, 180, 4210752);
		getModule().drawString(gui, Localization.ARCADE.HIGH_SCORE.translate(String.valueOf(highscore)), 10, 210, 4210752);
		getModule().drawString(gui, Localization.ARCADE.SCORE.translate(String.valueOf(score)), 10, 220, 4210752);
		getModule().drawString(gui, "W - " + Localization.ARCADE.INSTRUCTION_SHOOT.translate(), 330, 180, 4210752);
		getModule().drawString(gui, "A - " + Localization.ARCADE.INSTRUCTION_LEFT.translate(), 330, 190, 4210752);
		getModule().drawString(gui, "D - " + Localization.ARCADE.INSTRUCTION_RIGHT.translate(), 330, 200, 4210752);
		getModule().drawString(gui, "R - " + Localization.ARCADE.INSTRUCTION_RESTART.translate(), 330, 220, 4210752);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void keyPress(final GuiMinecart gui, final char character, final int extraInformation) {
		if (Character.toLowerCase(character) == 'r') {
			start();
		}
	}

	private void newHighScore() {
		buildings.clear();
		int digits;
		if (score == 0) {
			digits = 1;
		} else {
			digits = (int) Math.floor(Math.log10(score)) + 1;
		}
		canSpawnPahighast = false;
		int currentGhast = 0;
		for (int i = 0; i < digits; ++i) {
			final int digit = score / (int) Math.pow(10.0, digits - i - 1) % 10;
			final String[] number = ArcadeInvaders.numbers[digit];
			for (int j = 0; j < number.length; ++j) {
				final String line = number[j];
				for (int k = 0; k < line.length(); ++k) {
					if (line.charAt(k) == 'X') {
						final int x = (443 - (digits * 90 - 10)) / 2 + i * 90 + k * 20;
						final int y = 5 + j * 20;
						InvaderGhast ghast;
						if (currentGhast >= invaders.size()) {
							invaders.add(ghast = new InvaderGhast(this, x, -20));
							++currentGhast;
						} else {
							ghast = (InvaderGhast) invaders.get(currentGhast++);
						}
						ghast.setTarget(x, y);
					}
				}
			}
		}
		for (int i = currentGhast; i < invaders.size(); ++i) {
			final InvaderGhast ghast2 = (InvaderGhast) invaders.get(i);
			ghast2.setTarget(ghast2.x, -25);
		}
		gameoverCounter = 0;
		if (score > highscore) {
			newHighscore = true;
			getModule().sendPacket(2, o -> o.writeInt(score));
		}
	}

	@Override
	public void receivePacket(final int id, final DataInput reader, final EntityPlayer player) throws IOException {
		if (id == 2) {
			highscore = reader.readInt();
		}
	}

	@Override
	public void checkGuiData(final Object[] info) {
		getModule().updateGuiData(info, TrackStory.stories.size() + 1, (short) highscore);
	}

	@Override
	public void receiveGuiData(final int id, final short data) {
		if (id == TrackStory.stories.size() + 1) {
			highscore = data;
		}
	}

	@Override
	public void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setShort(getModule().generateNBTName("HighscoreGhast", id), (short) highscore);
	}

	@Override
	public void Load(final NBTTagCompound tagCompound, final int id) {
		highscore = tagCompound.getShort(getModule().generateNBTName("HighscoreGhast", id));
	}

	static {
		ArcadeInvaders.texture = "/gui/invaders.png";
		numbers = new String[][] { { "XXXX", "X  X", "X  X", "X  X", "X  X", "X  X", "XXXX" }, { "   X", "   X", "   X", "   X", "   X", "   X", "   X" },
			{ "XXXX", "   X", "   X", "XXXX", "X   ", "X   ", "XXXX" }, { "XXXX", "   X", "   X", "XXXX", "   X", "   X", "XXXX" }, { "X  X", "X  X", "X  X", "XXXX", "   X", "   X", "   X" },
			{ "XXXX", "X   ", "X   ", "XXXX", "   X", "   X", "XXXX" }, { "XXXX", "X   ", "X   ", "XXXX", "X  X", "X  X", "XXXX" }, { "XXXX", "   X", "   X", "   X", "   X", "   X", "   X" },
			{ "XXXX", "X  X", "X  X", "XXXX", "X  X", "X  X", "XXXX" }, { "XXXX", "X  X", "X  X", "XXXX", "   X", "   X", "XXXX" } };
	}
}
