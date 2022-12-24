package io.github.coolmineman.cheaterdeleter.modules.movement;

import io.github.coolmineman.cheaterdeleter.events.PlayerMovementListener;
import io.github.coolmineman.cheaterdeleter.events.PlayerDamageListener;
import io.github.coolmineman.cheaterdeleter.modules.CDModule;
import io.github.coolmineman.cheaterdeleter.objects.PlayerMoveC2SPacketView;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import io.github.coolmineman.cheaterdeleter.util.BlockCollisionUtil;
import net.minecraft.entity.damage.DamageSource;

public class VerticalCheck extends CDModule implements PlayerMovementListener, PlayerDamageListener {
    public VerticalCheck() {
        super("vertical_check");
        PlayerMovementListener.EVENT.register(this);
        PlayerDamageListener.EVENT.register(this);
    }

    //TODO: Smarter Bounce Handling
    @Override
    public void onMovement(CDPlayer player, PlayerMoveC2SPacketView packet, MoveCause cause) {
        if (!enabledFor(player) || !packet.isChangePosition() || player.isSpectator()) return;
        VerticalCheckData verticalCheckData = player.getOrCreateData(VerticalCheckData.class, VerticalCheckData::new);
        if (player.isCreative() || player.asMcPlayer().isSwimming() || player.asMcPlayer().isClimbing() || player.isFallFlying() || BlockCollisionUtil.isNearby(player, 2.0, 4.0, BlockCollisionUtil.NON_SOLID_COLLISION)) {
            verticalCheckData.isActive = false;
            return;
        }
        if (player.isOnGround() && !packet.isOnGround() && player.getVelocity().getY() < 0.45) {
            verticalCheckData.maxY = player.getY() + player.getMaxJumpHeight();
            verticalCheckData.isActive = true;
        } else if (packet.isOnGround()) {
            if (verticalCheckData != null && verticalCheckData.isActive) {
                verticalCheckData.isActive = false;
            }
        } else { //Packet off ground
            if (verticalCheckData.isActive && packet.isChangePosition() && packet.getY() > verticalCheckData.maxY) {
                // if (flag(player, FlagSeverity.MINOR, "Failed Vertical Movement Check " + (verticalCheckData.maxY - packet.getY()))) PunishUtil.groundPlayer(player);
                if (flag(player, FlagSeverity.MINOR, "Failed Vertical Movement Check " + (verticalCheckData.maxY - packet.getY()))) player.rollbackAndGround();
            }
            if (!verticalCheckData.isActive && player.getVelocity().getY() < 0.45) {
                verticalCheckData.maxY = player.getY() + player.getMaxJumpHeight();
                verticalCheckData.isActive = true;
            }

        }
    }

    private class VerticalCheckData {
        volatile double maxY = 0.0;
        boolean isActive = false;
    }

	@Override
	public void onPlayerDamage(CDPlayer player, DamageSource source, float amount) {
        if (!enabledFor(player)) return;
		VerticalCheckData verticalCheckData = player.getData(VerticalCheckData.class);
		if (verticalCheckData != null) {
            verticalCheckData.maxY += 0.6;
        }
    }
}
