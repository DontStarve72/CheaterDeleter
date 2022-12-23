package io.github.coolmineman.cheaterdeleter.trackers;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.jetbrains.annotations.NotNull;

import io.github.coolmineman.cheaterdeleter.events.OutgoingTeleportListener;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDEntity;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import io.github.coolmineman.cheaterdeleter.trackers.data.PlayerLastTeleportData;

public class PlayerLastTeleportTracker extends Tracker<PlayerLastTeleportData> implements OutgoingTeleportListener {

    protected PlayerLastTeleportTracker() {
        super(PlayerLastTeleportData.class);
        OutgoingTeleportListener.EVENT.register(this);
    }

    @Override
    @NotNull
    public PlayerLastTeleportData get(CDEntity entity) {
        return entity.getOrCreateData(PlayerLastTeleportData.class, PlayerLastTeleportData::new);
    }

    @Override
    public void onOutgoingTeleport(CDPlayer player, PlayerPositionLookS2CPacket packet) {
        PlayerLastTeleportData data = get(player);
        data.lastTeleportX = packet.getX();
        data.lastTeleportY = packet.getY();
        data.lastTeleportZ = packet.getZ();
        data.lastTeleportYaw = packet.getYaw();
        data.lastTeleportPitch = packet.getPitch();
        data.lastTeleport = System.currentTimeMillis();
    }
    
}
