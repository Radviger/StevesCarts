package vswe.stevescarts.arcade.tracks;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.helpers.Localization;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TrackLevel {
	public static final TrackLevel editor;
	private static String MAP_FOLDER_PATH;
	private Localization.STORIES.THE_BEGINNING name;
	private int playerX;
	private int playerY;
	private TrackOrientation.DIRECTION playerDir;
	private int itemX;
	private int itemY;
	private List<Track> tracks;
	private List<LevelMessage> messages;

	private static byte getFileVersion() {
		return 0;
	}

	@SideOnly(Side.CLIENT)
	public static List<TrackLevel> loadMapsFromFolder() {
		final List<TrackLevel> maps = new ArrayList<>();
		try {
			final File dir = new File(Minecraft.getMinecraft().mcDataDir, TrackLevel.MAP_FOLDER_PATH);
			final File[] children = dir.listFiles();
			if (children != null) {
				for (final File child : children) {
					if (child.isFile()) {
						final String name = child.getName();
						final TrackLevel map = loadMap(name);
						if (map != null) {
							maps.add(map);
						}
					}
				}
			}
		} catch (Exception exception) {
			System.out.println("Failed to load the maps");
		}
		return maps;
	}

	@SideOnly(Side.CLIENT)
	public static TrackLevel loadMap(final String filename) {
		try {
			final byte[] bytes = readFromFile(new File(Minecraft.getMinecraft().mcDataDir, TrackLevel.MAP_FOLDER_PATH + filename));
			return loadMapData(bytes);
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public static TrackLevel loadMap(final byte[] bytes) {
		try {
			return loadMapData(bytes);
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public static TrackLevel loadMapData(final byte[] bytes) throws IOException {
		final ByteArrayInputStream data = new ByteArrayInputStream(bytes);
		final int version = data.read();
		final int namelength = data.read();
		final byte[] namebytes = new byte[namelength];
		data.read(namebytes, 0, namelength);
		final String name = new String(namebytes, Charset.forName("UTF-8"));
		final int header = data.read() << 24 | data.read() << 16 | data.read() << 8 | data.read() << 0;
		final int playerX = header & 0x1F;
		final int playerY = header >> 5 & 0xF;
		final TrackOrientation.DIRECTION playerDir = TrackOrientation.DIRECTION.fromInteger(header >> 9 & 0x3);
		final int itemX = header >> 11 & 0x1F;
		final int itemY = header >> 16 & 0xF;
		final int tracksize = header >> 20 & 0x1FF;
		final TrackLevel map = new TrackLevel(null, playerX, playerY, playerDir, itemX, itemY);
		for (int i = 0; i < tracksize; ++i) {
			final int trackdata = data.read() << 16 | data.read() << 8 | data.read() << 0;
			final int trackX = trackdata & 0x1F;
			final int trackY = trackdata >> 5 & 0xF;
			final int type = trackdata >> 9 & 0x7;
			final TrackOrientation orientation = TrackOrientation.ALL.get(trackdata >> 12 & 0x3F);
			final int extraLength = trackdata >> 18 & 0x3F;
			final Track track = TrackEditor.getRealTrack(trackX, trackY, type, orientation);
			final byte[] extraData = new byte[extraLength];
			data.read(extraData);
			track.setExtraInfo(extraData);
			map.getTracks().add(track);
		}
		return map;
	}

	@SideOnly(Side.CLIENT)
	public static boolean saveMap(final String name,
	                              final int playerX,
	                              final int playerY,
	                              final TrackOrientation.DIRECTION playerDir,
	                              final int itemX,
	                              final int itemY,
	                              final List<Track> tracks) {
		try {
			final byte[] bytes = saveMapData(name, playerX, playerY, playerDir, itemX, itemY, tracks);
			writeToFile(new File(Minecraft.getMinecraft().mcDataDir, "sc2/arcade/trackoperator/" + name.replace(" ", "_") + ".dat"), bytes);
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	@SideOnly(Side.CLIENT)
	public static String saveMapToString(final String name,
	                                     final int playerX,
	                                     final int playerY,
	                                     final TrackOrientation.DIRECTION playerDir,
	                                     final int itemX,
	                                     final int itemY,
	                                     final List<Track> tracks) {
		try {
			final byte[] bytes = saveMapData(name, playerX, playerY, playerDir, itemX, itemY, tracks);
			String str = "TrackLevel.loadMap(new byte[] {";
			for (int i = 0; i < bytes.length; ++i) {
				if (i != 0) {
					str += ",";
				}
				str += bytes[i];
			}
			str += "});";
			return str;
		} catch (IOException ex) {
			return "";
		}
	}

	@SideOnly(Side.CLIENT)
	public static byte[] saveMapData(final String name,
	                                 final int playerX,
	                                 final int playerY,
	                                 final TrackOrientation.DIRECTION playerDir,
	                                 final int itemX,
	                                 final int itemY,
	                                 final List<Track> tracks) throws IOException {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		final DataOutputStream data = new DataOutputStream(stream);
		data.writeByte(getFileVersion());
		data.writeByte(name.length());
		data.writeBytes(name);
		int header = 0;
		header |= playerX;
		header |= playerY << 5;
		header |= playerDir.toInteger() << 9;
		header |= itemX << 11;
		header |= itemY << 16;
		header |= tracks.size() << 20;
		data.writeInt(header);
		for (final Track track : tracks) {
			int trackdata = 0;
			final byte[] extraData = track.getExtraInfo();
			trackdata |= track.getX();
			trackdata |= track.getY() << 5;
			trackdata |= track.getU() << 9;
			trackdata |= track.getOrientation().toInteger() << 12;
			trackdata |= extraData.length << 18;
			data.write((trackdata & 0xFF0000) >> 16);
			data.write((trackdata & 0xFF00) >> 8);
			data.write(trackdata & 0xFF);
			data.write(extraData);
		}
		return stream.toByteArray();
	}

	@SideOnly(Side.CLIENT)
	private static void writeToFile(final File file, final byte[] bytes) throws IOException {
		createFolder(file.getParentFile());
		final FileOutputStream writer = new FileOutputStream(file);
		writer.write(bytes);
		writer.close();
	}

	@SideOnly(Side.CLIENT)
	private static byte[] readFromFile(final File file) throws IOException {
		createFolder(file.getParentFile());
		final FileInputStream reader = new FileInputStream(file);
		final byte[] bytes = new byte[(int) file.length()];
		reader.read(bytes);
		reader.close();
		return bytes;
	}

	@SideOnly(Side.CLIENT)
	private static void createFolder(final File dir) throws IOException {
		if (dir == null) {
			return;
		}
		final File parent = dir.getParentFile();
		createFolder(parent);
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
	}

	public TrackLevel(final Localization.STORIES.THE_BEGINNING name, final int playerX, final int playerY, final TrackOrientation.DIRECTION playerDir, final int itemX, final int itemY) {
		this.name = name;
		this.playerX = playerX;
		this.playerY = playerY;
		this.playerDir = playerDir;
		this.itemX = itemX;
		this.itemY = itemY;
		tracks = new ArrayList<>();
		messages = new ArrayList<>();
	}

	public String getName() {
		return name.translate();
	}

	public void setName(final Localization.STORIES.THE_BEGINNING name) {
		this.name = name;
	}

	public int getPlayerStartX() {
		return playerX;
	}

	public int getPlayerStartY() {
		return playerY;
	}

	public TrackOrientation.DIRECTION getPlayerStartDirection() {
		return playerDir;
	}

	public int getItemX() {
		return itemX;
	}

	public int getItemY() {
		return itemY;
	}

	public List<Track> getTracks() {
		return tracks;
	}

	public List<LevelMessage> getMessages() {
		return messages;
	}

	public void addMessage(final LevelMessage levelMessage) {
		messages.add(levelMessage);
	}

	static {
		editor = new TrackLevel(Localization.STORIES.THE_BEGINNING.MAP_EDITOR, 0, 0, TrackOrientation.DIRECTION.RIGHT, 26, 9);
		TrackLevel.MAP_FOLDER_PATH = "sc2/arcade/trackoperator/";
	}
}
