package io.github.coolmineman.cheaterdeleter.mixin;

import com.mojang.datafixers.util.Pair;

import com.mojang.logging.LogUtils;
import net.minecraft.network.PacketCallbacks;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.coolmineman.cheaterdeleter.CheaterDeleterThread;
import io.github.coolmineman.cheaterdeleter.events.OutgoingPacketListener;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Shadow
    private PacketListener packetListener;
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet, CallbackInfo cb) {
        if (packetListener instanceof ServerPlayNetworkHandler) {
            CheaterDeleterThread.PACKET_QUEUE.add(new Pair<>(CDPlayer.of(((ServerPlayNetworkHandler)packetListener).player), packet));
        }
    }

    @Inject(method = "sendImmediately", at = @At("HEAD"))
    private void sendImmediately(Packet packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        if (packetListener instanceof ServerPlayNetworkHandler) {
            OutgoingPacketListener.EVENT.invoker().onOutgoingPacket(CDPlayer.of(((ServerPlayNetworkHandler)packetListener).player), packet);
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo cb) {
        LOGGER.warn(ExceptionUtils.getStackTrace(throwable));
    }
}
