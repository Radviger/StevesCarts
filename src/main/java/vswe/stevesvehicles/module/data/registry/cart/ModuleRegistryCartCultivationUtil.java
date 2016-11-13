package vswe.stevesvehicles.module.data.registry.cart;


import static vswe.stevesvehicles.item.ComponentTypes.ADVANCED_PCB;
import static vswe.stevesvehicles.item.ComponentTypes.BLADE_ARM;
import static vswe.stevesvehicles.item.ComponentTypes.EMPTY_DISK;
import static vswe.stevesvehicles.item.ComponentTypes.SIMPLE_PCB;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import vswe.stevesvehicles.client.rendering.models.cart.ModelLawnMower;
import vswe.stevesvehicles.module.cart.addon.cultivation.ModuleModTrees;
import vswe.stevesvehicles.module.cart.addon.cultivation.ModuleNetherWart;
import vswe.stevesvehicles.module.cart.addon.cultivation.ModulePlantSize;
import vswe.stevesvehicles.module.cart.attachment.ModuleFertilizer;
import vswe.stevesvehicles.module.cart.attachment.ModuleFlowerRemover;
import vswe.stevesvehicles.module.cart.attachment.ModuleHydrater;
import vswe.stevesvehicles.module.data.ModuleData;
import vswe.stevesvehicles.module.data.ModuleDataGroup;
import vswe.stevesvehicles.module.data.ModuleSide;
import vswe.stevesvehicles.module.data.registry.ModuleRegistry;
import vswe.stevesvehicles.module.data.registry.ModuleRegistryTanks;
import vswe.stevesvehicles.vehicle.VehicleRegistry;

public class ModuleRegistryCartCultivationUtil extends ModuleRegistry {
	public ModuleRegistryCartCultivationUtil() {
		super("cart.cultivation");

		loadFarmingUtil();
		loadWoodCuttingUtil();
	}

	private void loadFarmingUtil() {
		ModuleDataGroup farmers = ModuleDataGroup.getGroup(ModuleRegistryCartTools.FARM_KEY);

		ModuleData netherWart = new ModuleData("crop_nether_wart", ModuleNetherWart.class, 20);
		netherWart.addShapedRecipeWithSize(1, 2,
				Items.nether_wart,
				EMPTY_DISK);


		netherWart.addVehicles(VehicleRegistry.CART);
		netherWart.addRequirement(farmers);
		register(netherWart);



		ModuleData hydrator = new ModuleData("hydrator", ModuleHydrater.class, 6);
		hydrator.addShapedRecipeWithSize(3, 2,
				Items.iron_ingot,       Items.glass_bottle,         Items.iron_ingot,
				null,                   Blocks.iron_bars,           null);


		hydrator.addVehicles(VehicleRegistry.CART);
		hydrator.addRequirement(ModuleDataGroup.getGroup(ModuleRegistryTanks.TANK_KEY));
		register(hydrator);



		ModuleData fertilizer = new ModuleData("fertilizer", ModuleFertilizer.class, 10);
		fertilizer.addShapedRecipe(     new ItemStack(Items.dye, 1, 15),        null,                   new ItemStack(Items.dye, 1, 15),
				Items.glass_bottle,                     Items.leather,          Items.glass_bottle,
				Items.leather,                          SIMPLE_PCB,             Items.leather);


		fertilizer.addVehicles(VehicleRegistry.CART);
		register(fertilizer);




		ModuleData mower = new ModuleData("lawn_mower", ModuleFlowerRemover.class, 38) {
			@Override
			@SideOnly(Side.CLIENT)
			public void loadModels() {
				addModel("LawnMower", new ModelLawnMower());
				setModelMultiplier(0.4F);
			}
		};
		mower.addShapedRecipe(      BLADE_ARM,      null,           BLADE_ARM,
				null,           SIMPLE_PCB,     null,
				BLADE_ARM,      null,           BLADE_ARM);


		mower.addSides(ModuleSide.RIGHT, ModuleSide.LEFT);
		mower.addVehicles(VehicleRegistry.CART);
		register(mower);
	}

	private void loadWoodCuttingUtil() {
		ModuleDataGroup cutters = ModuleDataGroup.getGroup(ModuleRegistryCartTools.WOOD_KEY);

		ModuleData exotic = new ModuleData("tree_exotic", ModuleModTrees.class, 30);
		exotic.addShapedRecipeWithSize(1, 2,
				Blocks.sapling,
				EMPTY_DISK);


		exotic.addVehicles(VehicleRegistry.CART);
		exotic.addRequirement(cutters);
		register(exotic);


		ModuleData range = new ModuleData("planter_range_extender", ModulePlantSize.class, 20);
		range.addShapedRecipe(Items.redstone, ADVANCED_PCB, Items.redstone,
				null, Blocks.sapling, null,
				SIMPLE_PCB, Blocks.sapling, SIMPLE_PCB);


		range.addVehicles(VehicleRegistry.CART);
		range.addRequirement(cutters);
		register(range);
	}

}
