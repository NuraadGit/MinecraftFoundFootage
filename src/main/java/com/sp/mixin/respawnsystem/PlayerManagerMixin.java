package com.sp.mixin.respawnsystem;

import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.init.BackroomsLevels;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow @Final private MinecraftServer server;

    @Unique
    ServerPlayerEntity targetPlayer;

    @Unique
    private boolean shouldForceOverworldRespawn() {
        if (this.targetPlayer == null) {
            return false;
        }

        PlayerComponent playerComponent = InitializeComponents.PLAYER.get(this.targetPlayer);
        return playerComponent.wasCaughtByWalker();
    }

    @Inject(method = "respawnPlayer", at = @At("HEAD"))
    private void setTargetPlayer(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir){
        this.targetPlayer = player;
    }

    @Redirect(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getSpawnPointPosition()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos setSpawnPointPos(ServerPlayerEntity instance){
        if (this.shouldForceOverworldRespawn()) {
            ServerWorld overworld = this.server.getWorld(World.OVERWORLD);
            return overworld != null ? overworld.getSpawnPos() : instance.getSpawnPointPosition();
        }

        if (BackroomsLevels.isInBackrooms(this.targetPlayer.getWorld().getRegistryKey())) {
            return this.targetPlayer.getLastDeathPos().isPresent() ? this.targetPlayer.getLastDeathPos().get().getPos() : instance.getSpawnPointPosition();
        }
        return instance.getSpawnPointPosition();
    }

    @Redirect(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"))
    private @Nullable ServerWorld getCurrentWorld(MinecraftServer instance, RegistryKey<World> key){
        if (this.shouldForceOverworldRespawn()) {
            return instance.getWorld(World.OVERWORLD);
        }

        if (BackroomsLevels.isInBackrooms(this.targetPlayer.getWorld().getRegistryKey())) {
            return instance.getWorld(this.targetPlayer.getWorld().getRegistryKey());
        }

        return instance.getWorld(key);
    }

    @Redirect(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;findRespawnPosition(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3d> respawn(ServerWorld world, BlockPos pos, float angle, boolean forced, boolean alive){
        if (this.shouldForceOverworldRespawn()) {
            ServerWorld overworld = this.server.getWorld(World.OVERWORLD);
            if (overworld != null) {
                return PlayerEntity.findRespawnPosition(overworld, overworld.getSpawnPos(), 0.0f, false, alive)
                        .or(() -> Optional.of(overworld.getSpawnPos().toCenterPos()));
            }
        }

        if (BackroomsLevels.isInBackrooms(this.targetPlayer.getWorld().getRegistryKey())) {
            if (this.targetPlayer.getLastDeathPos().isPresent()) {
                Vec3d lastDeathPos = this.targetPlayer.getLastDeathPos().get().getPos().toCenterPos();
                if(lastDeathPos.y < 0){
                    return Optional.of(BlockPos.ofFloored(BackroomsLevels.getCurrentLevelsOrigin(world.getRegistryKey())).toCenterPos());
                }
                return Optional.of(this.targetPlayer.getLastDeathPos().get().getPos().toCenterPos());
            }
        }
        return PlayerEntity.findRespawnPosition(world, pos, angle, forced, alive);
    }

    @ModifyArgs(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;refreshPositionAndAngles(DDDFF)V"))
    private void setSpawnAngle(Args args){
        if (this.shouldForceOverworldRespawn()) {
            return;
        }

        if (BackroomsLevels.isInBackrooms(this.targetPlayer.getWorld().getRegistryKey())) {
            args.set(3, this.targetPlayer.getYaw());
            args.set(4, this.targetPlayer.getPitch());
        }
    }

    @ModifyArgs(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSpawnPoint(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/math/BlockPos;FZZ)V"))
    private void respawn2(Args args){
        if (this.shouldForceOverworldRespawn()) {
            return;
        }

        if (BackroomsLevels.isInBackrooms(this.targetPlayer.getWorld().getRegistryKey())) {
            ServerWorld currentWorld = this.server.getWorld(this.targetPlayer.getWorld().getRegistryKey());
            Optional<GlobalPos> lastDeathPos = this.targetPlayer.getLastDeathPos();

            if (currentWorld != null && lastDeathPos.isPresent()) {
                BlockPos pos = lastDeathPos.get().getPos();

                if (pos.getY() < 0){
                    pos = BlockPos.ofFloored(BackroomsLevels.getCurrentLevelsOrigin(currentWorld.getRegistryKey()));
                }

                args.set(0, currentWorld.getRegistryKey());
                args.set(1, pos);
                args.set(2, args.get(2));
                args.set(3, args.get(3));
                args.set(4, args.get(4));
            }
        }
    }
}
