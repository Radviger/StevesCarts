package vswe.stevescarts.network.message;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import vswe.stevescarts.api.network.AdvancedBuffer;
import vswe.stevescarts.api.network.AdvancedMessage;
import vswe.stevescarts.api.network.AdvancedMessageHandler;
import vswe.stevescarts.blocks.tileentities.TileEntityLiquid;
import vswe.stevescarts.blocks.tileentities.TileEntityUpgrade;

import java.io.IOException;

public class MessageFluidSync extends AdvancedMessage {
	private FluidStack fluidStack;
	private BlockPos pos;
	private int worldID;
	private int tankID;

	public MessageFluidSync(FluidStack fluidStack, BlockPos pos, int worldID, int tankID) {
		this.fluidStack = fluidStack;
		this.pos = pos;
		this.worldID = worldID;
		this.tankID = tankID;
	}

	public MessageFluidSync() {}

	@Override
	protected void read(AdvancedBuffer buf) throws IOException {
		fluidStack = buf.readFluidStack();
		pos = buf.readBlockPos();
		worldID = buf.readInt();
		tankID = buf.readInt();
	}

	@Override
	protected void write(AdvancedBuffer buf) throws IOException {
		buf.writeFluidStack(fluidStack);
		buf.writeBlockPos(pos);
		buf.writeInt(worldID);
		buf.writeInt(tankID);
	}

	public static class Handler extends AdvancedMessageHandler<MessageFluidSync, AdvancedMessage> {
		@Override
		protected AdvancedMessage handle(MessageFluidSync message, MessageContext context) {
			if (context.side == Side.CLIENT){
				syncTask(context, () -> {
					EntityPlayer player = getPlayer(context);
					TileEntity tile = player.world.getTileEntity(message.pos);
					if(tile instanceof TileEntityLiquid){
						((TileEntityLiquid)tile).tanks[message.tankID].setFluid(message.fluidStack);
					} else if (tile instanceof TileEntityUpgrade) {
						((TileEntityUpgrade) tile).tank.setFluid(message.fluidStack);
					}
				});
			}
			return null;
		}
	}
}
