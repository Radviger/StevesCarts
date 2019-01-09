package vswe.stevescarts;

import com.google.gson.JsonObject;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.Logger;
import vswe.stevescarts.blocks.ModBlocks;
import vswe.stevescarts.blocks.tileentities.TileEntityCargo;
import vswe.stevescarts.entitys.CartDataSerializers;
import vswe.stevescarts.entitys.EntityCake;
import vswe.stevescarts.entitys.EntityEasterEgg;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.handlers.EventHandler;
import vswe.stevescarts.handlers.EventHandlerChristmas;
import vswe.stevescarts.handlers.TradeHandler;
import vswe.stevescarts.helpers.ComponentTypes;
import vswe.stevescarts.helpers.CreativeTabSC2;
import vswe.stevescarts.helpers.GiftItem;
import vswe.stevescarts.items.ModItems;
import vswe.stevescarts.modules.data.ModuleData;
import vswe.stevescarts.network.message.MessageFluidSync;
import vswe.stevescarts.network.message.MessageReturnCart;
import vswe.stevescarts.network.message.MessageStevesCarts;
import vswe.stevescarts.plugins.PluginLoader;
import vswe.stevescarts.upgrades.AssemblerUpgrade;

import java.util.function.BooleanSupplier;

@Mod(modid = Constants.MOD_ID, name = Constants.NAME, version = Constants.VERSION, dependencies = "required-after:forge@[14.21.0.2373,);", acceptedMinecraftVersions = "[1.12,1.12.2]")
public class StevesCarts {

	@SidedProxy(clientSide = "vswe.stevescarts.ClientProxy", serverSide = "vswe.stevescarts.CommonProxy")
	public static CommonProxy proxy;

	@Mod.Instance(Constants.MOD_ID)
	public static StevesCarts instance;

	public static CreativeTabSC2 tabsSC2 = new CreativeTabSC2("SC2Modules");
	public static CreativeTabSC2 tabsSC2Components = new CreativeTabSC2("SC2Items");
	public static CreativeTabSC2 tabsSC2Blocks = new CreativeTabSC2("SC2Blocks");

	public static SimpleNetworkWrapper NET;

	public static Logger logger;
	public TradeHandler tradeHandler;

	public StevesCarts() {}

	@Mod.EventHandler
	public void preInit(final FMLPreInitializationEvent event) {
		StevesCarts.logger = event.getModLog();

		ModuleData.init();
		SCConfig.INSTANCE.load(event);

		ModItems.preInit();
		ModBlocks.preInit();

		AssemblerUpgrade.init();
		initCart(0, EntityMinecartModular.class);
		EntityRegistry.registerModEntity(new ResourceLocation(Constants.MOD_ID, "egg"), EntityEasterEgg.class, "Egg.Vswe", 2, StevesCarts.instance, 80, 3, true);
		EntityRegistry.registerModEntity(new ResourceLocation(Constants.MOD_ID, "cake"), EntityCake.class, "Cake.Vswe", 3, StevesCarts.instance, 80, 3, true);

		StevesCarts.proxy.preInit();
		registerFenceOreDict();

		PluginLoader.preInit(event);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void load(final FMLInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(new EventHandler());

		if (Constants.isChristmas) {
			tradeHandler = new TradeHandler();
			MinecraftForge.EVENT_BUS.register(new EventHandlerChristmas());
		}

		GiftItem.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(StevesCarts.instance, StevesCarts.proxy);
		NET = NetworkRegistry.INSTANCE.newSimpleChannel(Constants.MOD_ID);
		NET.registerMessage(MessageStevesCarts.Handler.class, MessageStevesCarts.class, 0, Side.CLIENT);
		NET.registerMessage(MessageStevesCarts.Handler.class, MessageStevesCarts.class, 1, Side.SERVER);
		NET.registerMessage(MessageFluidSync.Handler.class, MessageFluidSync.class, 2, Side.CLIENT);
		NET.registerMessage(MessageReturnCart.Handler.class, MessageReturnCart.class, 3, Side.SERVER);

		StevesCarts.proxy.init();

		StevesCarts.tabsSC2.setIcon(ModuleData.getList().get((byte)39).getItemStack());
		StevesCarts.tabsSC2Components.setIcon(ComponentTypes.REINFORCED_WHEELS.getItemStack());
		StevesCarts.tabsSC2Blocks.setIcon(new ItemStack(ModBlocks.CART_ASSEMBLER.getBlock(), 1));
		TileEntityCargo.loadSelectionSettings();
		ModItems.addRecipes();

		CartDataSerializers.init();

		PluginLoader.init(evt);
	}

	@Mod.EventHandler
	public void loadComplete(FMLLoadCompleteEvent event) {
		proxy.loadComplete();
	}

	private void initCart(final int ID, final Class<? extends EntityMinecartModular> cart) {
		EntityRegistry.registerModEntity(new ResourceLocation(Constants.MOD_ID, "cart." + ID), cart, "Minecart.Vswe." + ID, ID, StevesCarts.instance, 80, 3, true);
	}

	private static void registerFenceOreDict() {
		OreDictionary.registerOre("fenceWood", Blocks.OAK_FENCE);
		OreDictionary.registerOre("fenceWood", Blocks.SPRUCE_FENCE);
		OreDictionary.registerOre("fenceWood", Blocks.BIRCH_FENCE);
		OreDictionary.registerOre("fenceWood", Blocks.JUNGLE_FENCE);
		OreDictionary.registerOre("fenceWood", Blocks.DARK_OAK_FENCE);
		OreDictionary.registerOre("fenceWood", Blocks.ACACIA_FENCE);
	}

	public static class IsEvent implements IConditionFactory {

		@Override
		public BooleanSupplier parse(JsonContext context, JsonObject json) {
			String name = JsonUtils.getString(json, "event");
			if ("christmas".equals(name) && Constants.isChristmas || "easter".equals(name) && Constants.isEaster || "halloween".equals(name) && Constants.isHalloween)
				return () -> true;
			return () -> false;
		}
	}

	public static class ModuleEnabled implements IConditionFactory {

		@Override
		public BooleanSupplier parse(JsonContext context, JsonObject json) {
			int id = JsonUtils.getInt(json, "id");
			ModuleData data = ModuleData.getList().get((byte)id);
			if (!data.getIsLocked() && SCConfig.validModules.get(data.getID()))
				return () -> true;
			return () -> false;
		}
	}

	public static boolean hasOreDictKey(ItemStack item, String key) {
		if (!item.isEmpty()) {
			int i = OreDictionary.getOreID(key);
			for (int oreID : OreDictionary.getOreIDs(item)) {
				if (oreID == i) {
					return true;
				}
			}
		}
		return false;
	}
}
