package io.github.coolmineman.cheaterdeleter.modules.movement;

import java.util.Objects;

import com.mojang.logging.LogUtils;
import io.github.coolmineman.cheaterdeleter.events.OutgoingTeleportListener;
import io.github.coolmineman.cheaterdeleter.events.TeleportConfirmListener;
import io.github.coolmineman.cheaterdeleter.modules.CDModule;
import io.github.coolmineman.cheaterdeleter.objects.PlayerMoveC2SPacketView;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.Flag;

public class TeleportVerifierCheck extends CDModule implements TeleportConfirmListener, OutgoingTeleportListener {

    public TeleportVerifierCheck() {
        super("teleport_verifier_check");
        TeleportConfirmListener.EVENT.register(this);
        OutgoingTeleportListener.EVENT.register(this);
    }

    private static class TeleportVerifierCheckData {
        public Int2ObjectMap<TeleportInfo> teleports = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    }

    private static class TeleportInfo {
        public final double x;
        public final double y;
        public final double z;
        public final boolean exactX;
        public final boolean exactY;
        public final boolean exactZ;

        public TeleportInfo(CDPlayer player, PlayerPositionLookS2CPacket packet) {
            if (packet.getFlags().contains(Flag.X)) {
                exactX = false;
                x = packet.getX() + player.getPacketX();
            } else {
                exactX = true;
                x = packet.getX();
            }
            if (packet.getFlags().contains(Flag.Y)) {
                exactY = false;
                y = packet.getY() + player.getY();
            } else {
                exactY = true;
                y = packet.getY();
            }
            if (packet.getFlags().contains(Flag.Z)) {
                exactZ = false;
                z = packet.getZ() + player.getZ();
            } else {
                exactZ = true;
                z = packet.getZ();
            }
        }
    }

    @Override
    public void onTeleportConfirm(CDPlayer player, TeleportConfirmC2SPacket teleportPacket, PlayerMoveC2SPacketView movePacket) {
        TeleportInfo teleport = player.getData(TeleportVerifierCheckData.class).teleports.get(teleportPacket.getTeleportId());
        player.getData(TeleportVerifierCheckData.class).teleports.remove(teleportPacket.getTeleportId());
        if (enabledFor(player)) {
            Objects.requireNonNull(teleport, "If this is null and player is not cheating you should panic");
            assertAxis(player, "x", teleport.exactX, movePacket.getX(), teleport.x);
            assertAxis(player, "y", teleport.exactY, movePacket.getY(), teleport.y);
            assertAxis(player, "z", teleport.exactZ, movePacket.getZ(), teleport.z);
        }
    }

    private void assertAxis(CDPlayer player, String axis, boolean exact, double obtained, double target) {
        if (exact) {
            assertOrKick(obtained == target, player, "Bad Teleport " + axis + " expected: " + target + " got: " + obtained);
        } else {
            assertOrKick(Math.abs(target - obtained) < 10, player, "Bad Teleport " + axis + " expected: " + target + " got: " + obtained); // TODO this is bad
        }
    }

    @Override
    public void onOutgoingTeleport(CDPlayer player, PlayerPositionLookS2CPacket packet) {
        TeleportVerifierCheckData data = player.getOrCreateData(TeleportVerifierCheckData.class, TeleportVerifierCheckData::new);
        data.teleports.put(packet.getTeleportId(), new TeleportInfo(player, packet));
    }
}
