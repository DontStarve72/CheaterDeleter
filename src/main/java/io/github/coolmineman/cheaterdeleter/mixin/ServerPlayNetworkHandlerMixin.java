package io.github.coolmineman.cheaterdeleter.mixin;

import io.github.coolmineman.cheaterdeleter.util.CollisionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.coolmineman.cheaterdeleter.events.PlayerInteractBlockCallback;
import io.github.coolmineman.cheaterdeleter.objects.entity.CDPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.BlockPos;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;
    private boolean isTouching;

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo cb) {
        CDPlayer cdPlayer = CDPlayer.of(player);
        BlockPos pos = packet.getBlockHitResult().getBlockPos();
        if (!PlayerInteractBlockCallback.EVENT.invoker().onPlayerInteractBlock(cdPlayer, packet)) {
            cdPlayer.getNetworkHandler().sendPacket(new BlockUpdateS2CPacket(cdPlayer.getWorld(), pos));
            cdPlayer.getNetworkHandler().sendPacket(new BlockUpdateS2CPacket(cdPlayer.getWorld(), pos.offset(packet.getBlockHitResult().getSide())));
            cb.cancel();
        } 
    }
}
