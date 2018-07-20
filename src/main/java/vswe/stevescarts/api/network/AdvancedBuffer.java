package vswe.stevescarts.api.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;

public class AdvancedBuffer extends PacketBuffer {
    public AdvancedBuffer(ByteBuf buf) {
        super(buf);
    }

    public FluidStack readFluidStack() throws IOException {
        return FluidStack.loadFluidStackFromNBT(readCompoundTag());
    }

    public void writeFluidStack(FluidStack fluid) {
        NBTTagCompound compound = new NBTTagCompound();
        fluid.writeToNBT(compound);
        writeCompoundTag(compound);
    }
}
