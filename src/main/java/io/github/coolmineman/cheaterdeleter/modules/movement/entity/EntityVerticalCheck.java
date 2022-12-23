package io.github.coolmineman.cheaterdeleter.modules.movement.entity;

import org.jetbrains.annotations.Nullable;

import io.github.coolmineman.cheaterdeleter.events.VehicleMoveListener;
import io.github.coolmineman.cheaterdeleter.modules.CDModule;
import io.github.coolmineman.cheaterdeleter.objects.PlayerMoveC2SPacketView;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDEntity;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import io.github.coolmineman.cheaterdeleter.util.BlockCollisionUtil;
import io.github.coolmineman.cheaterdeleter.util.CollisionUtil;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Box;

public class EntityVerticalCheck extends CDModule implements VehicleMoveListener {
    public EntityVerticalCheck() {
        super("entity_vertical_check");
        VehicleMoveListener.EVENT.register(this);
    }

    @Override
    public long getFlagCoolDownMs() {
        return 100;
    }

    private class EntityVerticalCheckData {
        volatile double maxY = 0.0;
        boolean isActive = false;
    }

    @Override
    public void onVehicleMove(CDPlayer player, CDEntity vehicle, PlayerMoveC2SPacketView playerLook, PlayerInputC2SPacket playerInput, VehicleMoveC2SPacket vehicleMoveC2SPacket, @Nullable VehicleMoveC2SPacket lastVehicleMoveC2SPacket) {
            if (!enabledFor(player)) return;
            if (vehicle == null) return;
            EntityVerticalCheckData verticalCheckData = player.getOrCreateData(EntityVerticalCheckData.class,
                    EntityVerticalCheckData::new);
            if (vehicle.asMcEntity().isSwimming()
                    || BlockCollisionUtil.isNearby(player, 2.0, 4.0, BlockCollisionUtil.NON_SOLID_COLLISION)) {
                verticalCheckData.isActive = false;
                return;
            }
            Box vehicleBox = vehicle.getBoxForPosition(vehicleMoveC2SPacket.getX(), vehicleMoveC2SPacket.getY(), vehicleMoveC2SPacket.getZ()).expand(0.01);
            Box scanBox = vehicleBox.expand(0.6);
            boolean vehicleOnGround = CollisionUtil.isTouching(new CDEntity[] { player, vehicle }, scanBox,
                    vehicle.getWorld(), CollisionUtil.touchingRigidTopPredicates(vehicleBox));
            if (player.getWorld().getTime() - vehicle.getPistonMovementTick() < 1000) {
                verticalCheckData.isActive = false;
            }
            else if (vehicle.isOnGround() && !vehicleOnGround && vehicle.getVelocity().getY() < 0.45) {
                verticalCheckData.maxY = vehicle.getY() + vehicle.getMaxJumpHeight();
                verticalCheckData.isActive = true;
            } else if (vehicleOnGround) {
                if (verticalCheckData != null && verticalCheckData.isActive) {
                    verticalCheckData.isActive = false;
                }
            } else { // Packet off ground
                if (verticalCheckData.isActive && vehicleMoveC2SPacket.getY() > verticalCheckData.maxY) {
                    if (flag(player, FlagSeverity.MAJOR,
                            "Failed Entity Vertical Movement Check " + (verticalCheckData.maxY - vehicleMoveC2SPacket.getY())))
                        player.groundBoat(vehicle);
                }
                if (!verticalCheckData.isActive && vehicle.getVelocity().getY() < 0.45) {
                    verticalCheckData.maxY = vehicle.getY() + vehicle.getMaxJumpHeight();
                    verticalCheckData.isActive = true;
                }
    
            }
    }
}
