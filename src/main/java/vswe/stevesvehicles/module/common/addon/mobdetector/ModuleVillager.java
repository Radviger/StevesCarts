package vswe.stevesvehicles.module.common.addon.mobdetector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.passive.EntityVillager;

import vswe.stevesvehicles.localization.entry.module.LocalizationShooter;
import vswe.stevesvehicles.vehicle.VehicleBase;

public class ModuleVillager extends ModuleEntityDetector {
	public ModuleVillager(VehicleBase vehicleBase) {
		super(vehicleBase);
	}

	@Override
	public String getName() {
		return LocalizationShooter.VILLAGER_TITLE.translate();
	}
	@Override
	public boolean isValidTarget(Entity target) {
		return
				(
						target instanceof EntityGolem
						||
						target instanceof EntityVillager
						)
				;
	}
}