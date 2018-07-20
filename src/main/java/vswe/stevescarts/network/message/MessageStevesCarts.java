package vswe.stevescarts.network.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.StevesCarts;
import vswe.stevescarts.api.network.AdvancedBuffer;
import vswe.stevescarts.api.network.AdvancedMessage;
import vswe.stevescarts.api.network.AdvancedMessageHandler;
import vswe.stevescarts.api.util.CheckedConsumer;
import vswe.stevescarts.blocks.BlockCartAssembler;
import vswe.stevescarts.blocks.ModBlocks;
import vswe.stevescarts.blocks.tileentities.TileEntityBase;
import vswe.stevescarts.containers.ContainerBase;
import vswe.stevescarts.containers.ContainerMinecart;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.modules.ModuleBase;

import java.io.*;

/**
 * A hack around the old packet system
 */
public class MessageStevesCarts extends AdvancedMessage {

	private byte[] bytes;

	public MessageStevesCarts(byte[] bytes) {
		this.bytes = bytes;
	}

	public MessageStevesCarts() {}

	@Override
	protected void read(AdvancedBuffer buf) throws IOException {
		bytes = buf.readByteArray();
	}

	@Override
	protected void write(AdvancedBuffer buf) throws IOException {
		buf.writeByteArray(bytes);
	}

	public static class Handler extends AdvancedMessageHandler<MessageStevesCarts, AdvancedMessage> {

		@Override
		protected AdvancedMessage handle(MessageStevesCarts message, MessageContext context) {
			switch (context.side) {
				case CLIENT:
					syncTask(context, () -> {
						try {
							handleClient(message, context);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					break;
				case SERVER:
					syncTask(context, () -> {
						try {
							handleServer(message, context);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					break;
			}
			return null;
		}

		private boolean handleServer(MessageStevesCarts message, MessageContext context) throws IOException {
			EntityPlayer player = context.getServerHandler().player;
			World world = player.world;
			final ByteArrayDataInput reader = ByteStreams.newDataInput(message.bytes);
			int id = reader.readByte();

			if (player.openContainer instanceof ContainerPlayer) {
				final int entityid = reader.readInt();
				final EntityMinecartModular cart = getCart(entityid, world);
				if (cart != null) {
					receivePacketAtCart(cart, id, reader, player);
				}
			} else {
				final Container con = player.openContainer;

				if (con instanceof ContainerMinecart) {
					final ContainerMinecart conMC = (ContainerMinecart) con;
					final EntityMinecartModular cart2 = conMC.cart;

					receivePacketAtCart(cart2, id, reader, player);
				} else if (con instanceof ContainerBase) {
					final ContainerBase conBase = (ContainerBase) con;
					final TileEntityBase base = conBase.getTileEntity();
					if (base != null) {
						base.receivePacket(id, reader, player);
					}
				}
			}
			return true;
		}

		@SideOnly(Side.CLIENT)
		private void handleClient(MessageStevesCarts message, MessageContext context) throws IOException {
			EntityPlayer player = FMLClientHandler.instance().getClient().player;
			final ByteArrayDataInput reader = ByteStreams.newDataInput(message.bytes);
			final World world = player.world;
			int id = reader.readByte();
			if (id == -1) {
				final int x = reader.readInt();
				final int y = reader.readInt();
				final int z = reader.readInt();
				((BlockCartAssembler) ModBlocks.CART_ASSEMBLER.getBlock()).updateMultiBlock(world, new BlockPos(x, y, z));
			} else {
				final int entityid = reader.readInt();
				final EntityMinecartModular cart = getCart(entityid, world);
				if (cart != null) {
					receivePacketAtCart(cart, id, reader, player);
				}
			}
		}

		private void receivePacketAtCart(final EntityMinecartModular cart, final int id, final DataInput reader, final EntityPlayer player) throws IOException {
			for (final ModuleBase module : cart.getModules()) {
				if (id >= module.getPacketStart() && id < module.getPacketStart() + module.totalNumberOfPackets()) {
					module.delegateReceivedPacket(id - module.getPacketStart(), reader, player);
					break;
				}
			}
		}

		private EntityMinecartModular getCart(final int ID, final World world) {
			for (final Object e : world.loadedEntityList) {
				if (e instanceof Entity && ((Entity) e).getEntityId() == ID && e instanceof EntityMinecartModular) {
					return (EntityMinecartModular) e;
				}
			}
			return null;
		}
	}

	public static void sendPacket(final int id, final CheckedConsumer<DataOutput, IOException> writer) {
		try (ByteArrayOutputStream bs = new ByteArrayOutputStream();
			 DataOutputStream ds = new DataOutputStream(bs)) {

			ds.writeByte((byte) id);
			writer.accept(ds);
			StevesCarts.NET.sendToServer(new MessageStevesCarts(bs.toByteArray()));
		} catch (IOException ignored) {}
	}

	public static void sendPacket(final EntityMinecartModular cart, final int id, CheckedConsumer<DataOutput, IOException> writer) {
		try (ByteArrayOutputStream bs = new ByteArrayOutputStream();
			 DataOutputStream ds = new DataOutputStream(bs)) {

			ds.writeByte((byte) id);
			ds.writeInt(cart.getEntityId());
			writer.accept(ds);
			StevesCarts.NET.sendToServer(new MessageStevesCarts(bs.toByteArray()));
		} catch (IOException ignored) {}
	}

	public static void sendPacketToPlayer(final int id, final CheckedConsumer<DataOutput, IOException> writer, final EntityPlayer player, final EntityMinecartModular cart) {
		try (ByteArrayOutputStream bs = new ByteArrayOutputStream();
			 DataOutputStream ds = new DataOutputStream(bs)) {

			ds.writeByte((byte) id);
			ds.writeInt(cart.getEntityId());
			writer.accept(ds);
			StevesCarts.NET.sendTo(new MessageStevesCarts(bs.toByteArray()), (EntityPlayerMP) player);
		} catch (IOException ignored) {}
	}

	public static void sendBlockInfoToClients(final World world, final CheckedConsumer<DataOutput, IOException> writer, final BlockPos pos) {
		final ByteArrayOutputStream bs = new ByteArrayOutputStream();
		final DataOutputStream ds = new DataOutputStream(bs);
		try {
			ds.writeByte(-1);
			ds.writeInt(pos.getX());
			ds.writeInt(pos.getY());
			ds.writeInt(pos.getZ());
			writer.accept(ds);
			sendToAllAround(new MessageStevesCarts(bs.toByteArray()), pos, world);
		} catch (IOException ignored) {}
	}

	// see https://github.com/MinecraftForge/MinecraftForge/issues/3677
	private static void sendToAllAround(AdvancedMessage message, BlockPos pos, World world) {
		if (!(world instanceof WorldServer)) {
			return;
		}

		WorldServer worldServer = (WorldServer) world;
		PlayerChunkMap playerManager = worldServer.getPlayerChunkMap();

		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;

		for (Object playerObj : world.playerEntities) {
			if (playerObj instanceof EntityPlayerMP) {
				EntityPlayerMP player = (EntityPlayerMP) playerObj;

				if (playerManager.isPlayerWatchingChunk(player, chunkX, chunkZ)) {
					StevesCarts.NET.sendTo(message, player);
				}
			}
		}
	}
}
