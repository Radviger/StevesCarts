package vswe.stevescarts.items;

import net.minecraft.block.BlockRailBase;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.StevesCarts;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.helpers.CartVersion;
import vswe.stevescarts.helpers.ModuleCountPair;
import vswe.stevescarts.modules.ModuleBase;
import vswe.stevescarts.modules.data.ModuleData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ItemCarts extends ItemMinecart {
	public ItemCarts() {
		super(EntityMinecart.Type.RIDEABLE);
		setHasSubtypes(true);
		setMaxDamage(0);
		setCreativeTab(null);
		setUnlocalizedName("SC2:ModularCart");
	}

	public String getName() {
		return "Modular cart";
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		@Nonnull
		ItemStack stack = player.getHeldItem(hand);
		CartVersion.updateItemStack(stack);
		if (!world.isRemote) {
			if (BlockRailBase.isRailBlock(world, pos)) {
				try {
					final NBTTagCompound info = stack.getTagCompound();
					if (info != null && !info.hasKey("maxTime")) {
						try {
							final EntityMinecartModular cart = new EntityMinecartModular(world, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, info, stack.getDisplayName(), player.getGameProfile());
							world.spawnEntity(cart);
						} catch (Exception e) {
							e.printStackTrace();
							player.sendMessage(new TextComponentString("The cart failed to be placed into the world, this is due to an issue with one or more modules. "
								+ "Please post your log on the issue tracker here: " + TextFormatting.BLUE + " https://github.com/modmuss50/SC2/issues"));
							StevesCarts.logger.error(" --------------- Broken cart info --------------- ");
							StevesCarts.logger.error(info);
							NBTTagByteArray moduleIDTag = (NBTTagByteArray) info.getTag("Modules");
							for (final byte id : moduleIDTag.getByteArray()) {
								try {
									final Class<? extends ModuleBase> moduleClass = ModuleData.getList().get(id).getModuleClass();
									StevesCarts.logger.error("--- " + moduleClass.getCanonicalName());
								} catch (Exception ex) {
									StevesCarts.logger.error("Failed to load module with ID " + id + "! More info below.");
									e.printStackTrace();
								}
							}
							StevesCarts.logger.error(" --------------- Broken cart info --------------- ");
							return EnumActionResult.FAIL;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					return EnumActionResult.FAIL;
				}
				stack.shrink(1);
				return EnumActionResult.SUCCESS;
			}
		}
		return EnumActionResult.PASS;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull ItemStack item, final World world, final List<String> list, final ITooltipFlag useExtraInfo) {
		CartVersion.updateItemStack(item);
		final NBTTagCompound info = item.getTagCompound();
		if (info != null) {
			final NBTTagByteArray moduleIDTag = (NBTTagByteArray) info.getTag("Modules");
			final byte[] bytes = moduleIDTag.getByteArray();
			final List<ModuleCountPair> counts = new ArrayList<>();
			for (int i = 0; i < bytes.length; ++i) {
				final byte id = bytes[i];
				final ModuleData module = ModuleData.getList().get(id);
				if (module != null) {
					boolean found = false;
					if (!info.hasKey("Data" + i)) {
						for (final ModuleCountPair count : counts) {
							if (count.isContainingData(module)) {
								count.increase();
								found = true;
								break;
							}
						}
					}
					if (!found) {
						final ModuleCountPair count2 = new ModuleCountPair(module);
						if (info.hasKey("Data" + i)) {
							count2.setExtraData(info.getByte("Data" + i));
						}
						counts.add(count2);
					}
				}
			}
			for (final ModuleCountPair count3 : counts) {
				list.add(count3.toString());
			}
			if (info.hasKey("Spares")) {
				final byte[] spares = info.getByteArray("Spares");
				for (int j = 0; j < spares.length; ++j) {
					final byte id2 = spares[j];
					final ModuleData module2 = ModuleData.getList().get(id2);
					if (module2 != null) {
						String name = module2.getName();
						if (info.hasKey("Data" + (bytes.length + j))) {
							name = module2.getCartInfoText(name, info.getByte("Data" + (bytes.length + j)));
						}
						list.add(TextFormatting.GOLD + name);
					}
				}
			}
			if (info.hasKey("maxTime")) {
				list.add(TextFormatting.RED + "Incomplete cart!");
				final int maxTime = info.getInteger("maxTime");
				final int currentTime = info.getInteger("currentTime");
				final int timeLeft = maxTime - currentTime;
				list.add(TextFormatting.RED + "Time left: " + formatTime(timeLeft));
			}
		} else {
			list.add("No modules loaded");
		}
	}

	private String formatTime(int ticks) {
		int seconds = ticks / 20;
		ticks -= seconds * 20;
		int minutes = seconds / 60;
		seconds -= minutes * 60;
		final int hours = minutes / 60;
		minutes -= hours * 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	@Override
	public boolean getShareTag() {
		return true;
	}
}
