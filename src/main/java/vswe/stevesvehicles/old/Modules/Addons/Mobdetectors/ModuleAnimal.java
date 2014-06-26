package vswe.stevesvehicles.old.Modules.Addons.Mobdetectors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import vswe.stevesvehicles.vehicles.entities.EntityModularCart;
import vswe.stevesvehicles.old.Helpers.Localization;

public class ModuleAnimal extends ModuleMobdetector {
	public ModuleAnimal(EntityModularCart cart) {
		super(cart);
	}

	public String getName() {
		return Localization.MODULES.ADDONS.DETECTOR_ANIMALS.translate();
	}
	public boolean isValidTarget(Entity target) {
		return
		target instanceof EntityAnimal
		&&
		(
			!(target instanceof EntityTameable)
			||
			!((EntityTameable)target).isTamed()
		)
		;
	}
}