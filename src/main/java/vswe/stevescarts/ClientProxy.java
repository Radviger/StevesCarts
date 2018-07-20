package vswe.stevescarts;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.handlers.SoundHandler;
import vswe.stevescarts.helpers.MinecartSoundMuter;
import vswe.stevescarts.items.ModItems;
import vswe.stevescarts.modules.data.ModuleData;
import vswe.stevescarts.renders.ItemStackRenderer;
import vswe.stevescarts.renders.RendererCart;

import java.util.HashMap;
import java.util.Map;

public class ClientProxy extends CommonProxy {

	@Override
	public void init() {
		ModuleData.initModels();

		/*
		ModelLoader.setCustomStateMapper(ModBlocks.UPGRADE.getBlock(), new StateMapperBase() {
			@Override
			protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
				String resourceDomain = Block.REGISTRY.getNameForObject(state.getBlock()).getResourceDomain();
				String propertyString = getPropertyString(state.getProperties());
				return new ModelResourceLocation(resourceDomain + ":upgrade", propertyString);
			}
		});*/
	}

	@Override
	public void preInit() {
		RenderingRegistry.registerEntityRenderingHandler(EntityMinecartModular.class, RendererCart::new); //Needs to be done after the mc ones have been done
		new SoundHandler();
		new MinecartSoundMuter();
	}

	@Override
	public void loadComplete() {
		super.loadComplete();
		//Done here to try and load after all other mods, as some mods override this
		TileEntityItemStackRenderer.instance = new ItemStackRenderer(TileEntityItemStackRenderer.instance);
	}

	@Override
	public void registerDefaultItemRenderer(Item item) {
		Map<Integer, ResourceLocation> map = this.getItemModelMap(item);
		for(Map.Entry<Integer, ResourceLocation> entry : map.entrySet()) {
			ModelLoader.setCustomModelResourceLocation(item, entry.getKey(), (ModelResourceLocation) entry.getValue());
		}
	}

	@Override
	public Map<Integer, ResourceLocation> getItemModelMap(Item item) {
		Map<Integer, ResourceLocation> map = new HashMap<>();
		if (item instanceof ModItems.IMultipleItemModelDefinition) {
			for (Map.Entry<Integer, ResourceLocation> model : ((ModItems.IMultipleItemModelDefinition) item).getModels().entrySet()) {
				map.put(model.getKey(), new ModelResourceLocation(model.getValue(), "inventory"));
			}
		} else {
			map.put(0, new ModelResourceLocation(item.getRegistryName().toString(), "inventory"));
		}
		return map;
	}
}
