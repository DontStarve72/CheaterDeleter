package io.github.coolmineman.cheaterdeleter.modules.packetcount;

import io.github.coolmineman.cheaterdeleter.events.PacketCallback;
import io.github.coolmineman.cheaterdeleter.modules.CDModule;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class CraftRequestC2SPacketLimiterModule extends CDModule implements PacketCallback {
    private static final long INTERVAL = 1;
    private static final long MAX_PACKETS_PER_SECOND = 20;

    public CraftRequestC2SPacketLimiterModule() {
        super("craft_request_c2s_packet_limiter");
        PacketCallback.EVENT.register(this);
    }

    @Override
    public ActionResult onPacket(CDPlayer player, Packet<ServerPlayPacketListener> packet) {
        if (!enabledFor(player) || !(packet instanceof CraftRequestC2SPacket)) return ActionResult.PASS;
        PacketVolumeData data = player.getOrCreateData(PacketVolumeData.class, PacketVolumeData::new);
        data.packetCount++;
        if (data.packetCount > MAX_PACKETS_PER_SECOND * INTERVAL) {
            player.getNetworkHandler().disconnect(Text.literal("Too Many CraftRequestC2SPacket Packets"));
            player.getNetworkHandler().connection.disconnect(Text.literal("Too Many CraftRequestC2SPacket Packets")); // Why is this needed?
            return ActionResult.FAIL;
        } else if (System.currentTimeMillis() - data.lastCheck >= INTERVAL * 1000) {
            data.packetCount = 0;
            data.lastCheck = System.currentTimeMillis();
        }
        return ActionResult.PASS;
    }

    public static class PacketVolumeData {
        public long packetCount = 0;
        public long lastCheck = System.currentTimeMillis();
    }
}
