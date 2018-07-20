package vswe.stevescarts.network.message;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import vswe.stevescarts.api.network.AdvancedBuffer;
import vswe.stevescarts.api.network.AdvancedMessage;
import vswe.stevescarts.api.network.AdvancedMessageHandler;
import vswe.stevescarts.containers.ContainerMinecart;
import vswe.stevescarts.entitys.EntityMinecartModular;

import java.io.IOException;

public class MessageReturnCart extends AdvancedMessage {

    public MessageReturnCart() {}

    @Override
    protected void read(AdvancedBuffer buf) throws IOException {}

    @Override
    protected void write(AdvancedBuffer buf) throws IOException {}

    public static class Handler extends AdvancedMessageHandler<MessageReturnCart, AdvancedMessage> {
        @Override
        protected AdvancedMessage handle(MessageReturnCart message, MessageContext context) {
            EntityPlayer player = getPlayer(context);
            Container container = player.openContainer;
            if (container instanceof ContainerMinecart) {
                EntityMinecartModular cart = ((ContainerMinecart) container).cart;
                cart.turnback();
            }
            return null;
        }
    }
}
