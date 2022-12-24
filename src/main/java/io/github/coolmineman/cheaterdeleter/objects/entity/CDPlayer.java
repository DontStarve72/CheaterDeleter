package io.github.coolmineman.cheaterdeleter.objects.entity;

import java.util.Date;
import java.util.Locale;

import io.github.coolmineman.cheaterdeleter.util.BlockCollisionUtil;
import io.github.coolmineman.cheaterdeleter.util.BoxUtil;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import io.github.coolmineman.cheaterdeleter.LoggerThread;
import io.github.coolmineman.cheaterdeleter.compat.CompatManager;
import io.github.coolmineman.cheaterdeleter.compat.LuckoPermissionsCompat;
import io.github.coolmineman.cheaterdeleter.modules.CDModule;
import io.github.coolmineman.cheaterdeleter.objects.PlayerMoveC2SPacketView;
import io.github.coolmineman.cheaterdeleter.config.GlobalConfig;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public interface CDPlayer extends CDEntity {

    @Override
    default void _init() {
        CDEntity.super._init();
        CDPlayerEx ex = new CDPlayerEx(this);
        putData(CDPlayerEx.class, ex);
    }

    default void flag(int amount) {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        if (ex.flags > 0) {
            long timeDelta = System.currentTimeMillis() - ex.lastFlag;
            while (timeDelta > 1000) { //Runs once per second
                timeDelta -= 1000;
                //Max of 16 minor flags or 4 major per 1 min 
                ex.flags -= 1.0 / (16.0 * 5.0 * 60.0);
                if (ex.flags < 0) {
                    ex.flags = 0;
                }
            }
        }
        ex.flags += amount;
        ex.lastFlag = System.currentTimeMillis();
        if (ex.flags > 16) {
            kick(Text.literal("Flagged Too Much by AC"));
        }
    }

    default void minorFlag() {
        flag(1);
    }

    default void majorFlag() {
        flag(4);
    }

    /**
     * Call on failed Check
     * @return punish if true
     */
    default boolean flag(CDModule check, CDModule.FlagSeverity severity) {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        if (check.getFlagCoolDownMs() > System.currentTimeMillis() - ex.lastFlagsMap.getLong(check)) {
            return false;
        }
        ex.lastFlagsMap.put(check, System.currentTimeMillis());
        switch (severity) {
            case MAJOR:
                majorFlag();
            break;
            case MINOR:
                minorFlag();
            break;
        }
        return true;
    }

    @Nullable
    default ScreenHandler getCurrentScreenHandler() {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        return asMcPlayer().currentScreenHandler == asMcPlayer().playerScreenHandler && !ex.hasCurrentPlayerScreenHandler ? null : asMcPlayer().currentScreenHandler;
    }

    default void setHasCurrentPlayerScreenHandler(boolean hasCurrentPlayerScreenHandler) {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        ex.hasCurrentPlayerScreenHandler = hasCurrentPlayerScreenHandler;
    }

    default void rollback() {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        if (ex.hasLastGood) {
            teleportCd(ex.lastGoodX, ex.lastGoodY, ex.lastGoodZ);
            LoggerThread.info(String.format("Rolled %s back to %f %f %f", asString(), ex.lastGoodX, ex.lastGoodY, ex.lastGoodZ));
        }
    }

    default void tickRollback(double x, double y, double z, boolean isTeleport) {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        if (System.currentTimeMillis() - ex.lastFlag > 5000 || isTeleport || !ex.hasLastGood) {
            // Never let last good coordinates be in midair
            if (isOnGround()) {
                ex.lastGoodX = x;
                ex.lastGoodY = y;
                ex.lastGoodZ = z;
                ex.hasLastGood = true;
            }
        }
    }

    // TODO: Leaving this here for now to see if it's a good implementation
    default void rollbackAndGround() {
//        CDPlayerEx ex = getData(CDPlayerEx.class);
//        Box box = BoxUtil.withNewMinY(this.getBoxForPosition(ex.lastGoodX, this.getY(), ex.lastGoodZ), 0);
//        World world = this.getWorld();
//        // Calculate newY because lastGoodY might be in the air
//        double newY = BlockPos.stream(box).mapToDouble(pos -> BlockCollisionUtil.getHighestTopIntersection(world.getBlockState(pos).getCollisionShape(world, pos).offset(pos.getX(), pos.getY(), pos.getZ()), box, -100)).max().orElse(-100);
//        // Deal fall damage - Is this how you're supposed to do this?
//        asMcPlayer().fallDistance = (float) (this.getPacketY() - newY);
//        asMcPlayer().handleFall(0, true);
//        this.teleportCd(ex.lastGoodX, newY, ex.lastGoodZ);
        rollback();
    }

    default void groundBoat(CDEntity entity) {
        Box box = BoxUtil.withNewMinY(entity.getBox(), 0);
        World world = entity.getWorld();
        double newY = BlockPos.stream(box).mapToDouble(
                pos -> {
                    BlockState state = world.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) return pos.getY() + 1;
                    if (state.getMaterial().isLiquid()) return pos.getY() + 1;
                    return BlockCollisionUtil.getHighestTopIntersection(state.getCollisionShape(world, pos).offset(pos.getX(), pos.getY(), pos.getZ()), box, -100);
                }
        ).max().orElse(-100);
        entity.asMcEntity().setPos(entity.getX(), newY, entity.getZ());
        this.getNetworkHandler().sendPacket(new VehicleMoveS2CPacket(entity.asMcEntity()));
    }

    default TriState getPermission(String permission) {
        LuckoPermissionsCompat compat = CompatManager.getCompatHolder(LuckoPermissionsCompat.class).compat;
        return compat != null ? compat.get(this, permission) : TriState.DEFAULT;
    }

    default boolean shouldBypassAnticheat() {
        return getPermission("cheaterdeleter.bypassanticheat") == TriState.TRUE;
    }

    default boolean shouldSendMajorFlags() {
        return getPermission("cheaterdeleter.sendmajorflags") == TriState.TRUE;
    }

    default boolean shouldSendMinorFlags() {
        return getPermission("cheaterdeleter.cheaterdeleter.sendminorflags") == TriState.TRUE;
    }

    default void teleportCd(double x, double y, double z) {
        teleportCd(x, y, z, getYaw(), getPitch());
        CDPlayerEx ex = getData(CDPlayerEx.class);
        ex.lastGoodX = x;
        ex.lastGoodY = y;
        ex.lastGoodZ = z;
        ex.hasLastGood = true;
    }

    //TODO: Still breaks boats somehow
    default void teleportCd(double x, double y, double z, float yaw, float pitch) {
        asMcPlayer().stopRiding();
        asMcPlayer().teleport(getWorld(), x, y, z, yaw, pitch);
    }

    default void setPacketPos(PlayerMoveC2SPacketView packet) {
        CDPlayerEx ex = getData(CDPlayerEx.class);
        if (packet.isChangePosition()) {
            ex.lastPacketX = packet.getX();
            ex.lastPacketY = packet.getY();
            ex.lastPacketZ = packet.getZ();
        }
        if (packet.isChangeLook()) {
            ex.lastPacketYaw = packet.getYaw();
            ex.lastPacketPitch = packet.getPitch();
        }
    }

    default double getPacketX() {
        return getData(CDPlayerEx.class).lastPacketX;
    }

    default double getPacketY() {
        return getData(CDPlayerEx.class).lastPacketY;
    }

    default double getPacketZ() {
        return getData(CDPlayerEx.class).lastPacketZ;
    }

    default float getPacketYaw() {
        return getData(CDPlayerEx.class).lastPacketYaw;
    }

    default float getPacketPitch() {
        return getData(CDPlayerEx.class).lastPacketPitch;
    }

    /**
     * True if flying with an Elytra or similar
     */
    default boolean isFallFlying() {
        return asMcPlayer().isFallFlying();
    }

    default ServerPlayNetworkHandler getNetworkHandler() {
        return asMcPlayer().networkHandler;
    }

    default void kick(Text text) {
        if (GlobalConfig.getDebugMode() >= 2) {
            asMcPlayer().sendMessage(Text.literal("Kicked: ").append(text));
            CDPlayerEx ex = getData(CDPlayerEx.class);
            ex.flags = 0;
        } else {
            asMcPlayer().networkHandler.disconnect(text);
        }
    }

    default void ban(int hours, String reason) {
        if (GlobalConfig.getDebugMode() >= 2) {
            asMcPlayer().sendMessage(Text.literal("Banned: " + reason));
            CDPlayerEx ex = getData(CDPlayerEx.class);
            ex.flags = 0;
        } else {
            BannedPlayerList bannedPlayerList = asMcPlayer().server.getPlayerManager().getUserBanList();
            BannedPlayerEntry entry;
            long now = System.currentTimeMillis();
            
            if (hours > 0) {
                entry = new BannedPlayerEntry(asMcPlayer().getGameProfile(), new Date(now), "CheaterDeleter", new Date(now + (hours * 60 * 60 * 1000)), reason);
            } else {
                entry = new BannedPlayerEntry(asMcPlayer().getGameProfile(), new Date(now), "CheaterDeleter", null, reason);
            }

            bannedPlayerList.add(entry);
            asMcPlayer().networkHandler.disconnect(Text.translatable("multiplayer.disconnect.banned"));
        }
    }

    default boolean isCreative() {
        return asMcPlayer().isCreative();
    }

    default boolean isSpectator() {
        return asMcPlayer().isSpectator();
    }

    default String asString() {
        return String.format(Locale.ROOT, "Player['%s'/%s, w='%s', x=%.2f, y=%.2f, z=%.2f]", asMcPlayer().getName().getString(), this.getUuid().toString(), this.getWorld() == null ? "~NULL~" : this.getWorld().getRegistryKey().getValue().toString(), this.getX(), this.getY(), this.getZ());
    }

    default ServerPlayerEntity asMcPlayer() {
        return (ServerPlayerEntity)this;
    }

    public static CDPlayer of(ServerPlayerEntity mcPlayer) {
        return ((CDPlayer)mcPlayer);
    }
}
