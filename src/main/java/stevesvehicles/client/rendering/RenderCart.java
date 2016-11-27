package stevesvehicles.client.rendering;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import stevesvehicles.common.vehicles.entitys.EntityModularCart;

public class RenderCart extends RenderVehicle {
	public RenderCart(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	protected void applyMatrixUpdates(stevesvehicles.common.vehicles.VehicleBase vehicle, MatrixObject matrix, float partialTickTime) {
		EntityModularCart cart = (EntityModularCart) vehicle.getEntity();
		// calculate the current positions and the current pitch(since the cart
		// should still be rendered between ticks)
		double partialPosX = cart.lastTickPosX + (cart.posX - cart.lastTickPosX) * partialTickTime;
		double partialPosY = cart.lastTickPosY + (cart.posY - cart.lastTickPosY) * partialTickTime;
		double partialPosZ = cart.lastTickPosZ + (cart.posZ - cart.lastTickPosZ) * partialTickTime;
		matrix.pitch = cart.prevRotationPitch + (cart.rotationPitch - cart.prevRotationPitch) * partialTickTime;
		/*
		 * Vec3 rotations =
		 * engine.getRenderRotation((float)partialPosX,(float)partialPosY,(float
		 * )partialPosZ,partialTickTime); if (rotations != null) { yaw =
		 * (float)rotations.xCoord * 180F / (float)Math.PI; }
		 */
		Vec3d posFromRail = cart.getPos(partialPosX, partialPosY, partialPosZ);
		// if cart is on a rail the yaw and the pitch should be calculated
		// accordingly(instead of just use given values)
		if (posFromRail != null && cart.canUseRail()) {
			// predict the last and next position of the cart with the given
			// prediction time span
			double predictionLength = 0.30000001192092896D;
			Vec3d lastPos = cart.getPosOffset(partialPosX, partialPosY, partialPosZ, predictionLength);
			Vec3d nextPos = cart.getPosOffset(partialPosX, partialPosY, partialPosZ, -predictionLength);
			// if the last pos wasn't on the rail
			if (lastPos == null) {
				lastPos = posFromRail;
			}
			// if the next pos won't be on the rail
			if (nextPos == null) {
				nextPos = posFromRail;
			}
			// fix the coordinates accordingly to the rail
			matrix.x += posFromRail.xCoord - partialPosX;
			matrix.y += (lastPos.yCoord + nextPos.yCoord) / 2.0D - partialPosY;
			matrix.z += posFromRail.zCoord - partialPosZ;
			// get the difference beetween the next and the last pos
			Vec3d difference = nextPos.addVector(-lastPos.xCoord, -lastPos.yCoord, -lastPos.zCoord);
			// if there exist any difference
			if (difference.lengthVector() != 0.0D) {
				difference = difference.normalize();
				// calculate the yaw and the pitch
				matrix.yaw = (float) (Math.atan2(difference.zCoord, difference.xCoord) * 180.0D / Math.PI);
				matrix.pitch = (float) (Math.atan(difference.yCoord) * 73.0D);
			}
		}
		matrix.yaw = 180F - matrix.yaw;
		matrix.pitch *= -1;
		// calculate and apply the rotation caused by the cart being damaged
		float damageRot = cart.getRollingAmplitude() - partialTickTime;
		float damageTime = cart.getDamage() - partialTickTime;
		float damageDir = cart.getRollingDirection();
		if (damageTime < 0.0F) {
			damageTime = 0.0F;
		}
		matrix.flip = (cart.motionX > 0) == (cart.motionZ > 0);
		if (cart.cornerFlip) {
			matrix.flip = !matrix.flip;
		}
		if (cart.getRenderFlippedYaw(matrix.yaw + (matrix.flip ? 0F : 180F))) {
			matrix.flip = !matrix.flip;
		}
		if (damageRot > 0.0F) {
			matrix.roll = MathHelper.sin(damageRot) * damageRot * damageTime / 10.0F * damageDir;
		}
		matrix.y += 0.375F;
	}
}