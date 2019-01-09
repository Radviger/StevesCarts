package vswe.stevescarts.modules.realtimers;

import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

import java.util.List;

public abstract class ModuleCommand extends ModuleBase implements ICommandSender {
	private String command;
	private int[] textbox;

	public ModuleCommand(final EntityMinecartModular cart) {
		super(cart);
		command = "say HI";
		textbox = new int[] { 10, 10, 145, 90 };
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		final List<String> lines = gui.getFontRenderer().listFormattedStringToWidth(command, textbox[2] - 4);
		for (int i = 0; i < lines.size(); ++i) {
			final String line = lines.get(i);
			drawString(gui, line, textbox[0] + 2, textbox[1] + 2 + i * 8, 16777215);
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
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/command.png");
		drawImage(gui, textbox, 0, 0);
	}

	public void keyPress(final char character, final int extraInformation) {
		if (extraInformation == 14) {
			if (command.length() > 0) {
				command = command.substring(0, command.length() - 1);
			}
		} else {
			command += Character.toString(character);
		}
	}

	public String getCommandSenderName() {
		return "@";
	}

	public void sendChatToPlayer(final String var1) {
	}

	@Override
	public boolean canUseCommand(final int level, final String command) {
		return level <= 2;
	}

	public String translateString(final String format, final Object... args) {
		return format;
	}

	public BlockPos getPlayerCoordinates() {
		return getCart().getPosition();
	}

	private void executeCommand() {
		if (!getCart().world.isRemote) {}
	}

	@Override
	public void moveMinecartOnRail(BlockPos pos) {
		if (getCart().world.getBlockState(pos).getBlock() == Blocks.DETECTOR_RAIL) {
			executeCommand();
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		tagCompound.setString(generateNBTName("Command", id), command);
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		command = tagCompound.getString(generateNBTName("Command", id));
	}
}
