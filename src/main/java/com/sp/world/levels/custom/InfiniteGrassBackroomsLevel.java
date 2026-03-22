package com.sp.world.levels.custom;

import com.sp.SPBRevamped;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModBlocks;
import com.sp.world.events.infinite_grass.InfiniteGrassAmbience;
import com.sp.world.generation.chunk_generator.InfGrassChunkGenerator;
import com.sp.world.levels.BackroomsLevel;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class InfiniteGrassBackroomsLevel extends BackroomsLevel {

    public InfiniteGrassBackroomsLevel() {
        super("inf_grass", InfGrassChunkGenerator.CODEC, new Vec3d(0, 31, 0), BackroomsLevels.INFINITE_FIELD_WORLD_KEY);
    }

    @Override
    public void register() {
        super.register();

        this.registerEvent("ambience", InfiniteGrassAmbience::new);

        this.registerTransition((world, playerComponent, from) -> {
            List<LevelTransition> playerList = new ArrayList<>();

            if (from instanceof InfiniteGrassBackroomsLevel
                    && playerComponent.player.isOnGround()
                    && playerComponent.player.getPos().y > 128
                    && playerComponent.player.getWorld().getBlockState(playerComponent.player.getBlockPos().down()).isOf(ModBlocks.CONCRETE_BLOCK_11)) {
                playerList.add(getLevel324Transition(playerComponent));
            }

            return playerList;
        }, this.getLevelId() + "->" + BackroomsLevels.LEVEL324_BACKROOMS_LEVEL.getLevelId());
    }

    private LevelTransition getLevel324Transition(PlayerComponent playerComponent) {
        return new LevelTransition(
                110,
                (teleport, tick) -> {
                    if (teleport.playerComponent().player.getWorld().isClient()) {
                        return;
                    }

                    if (tick == 20) {
                        teleport.playerComponent().setShouldNoClip(true);
                        teleport.playerComponent().sync();
                    }

                    if (tick == 14) {
                        SPBRevamped.sendBlackScreenPacket((ServerPlayerEntity) teleport.playerComponent().player, 20, true, false);
                    }

                    if (tick == 1) {
                        teleport.playerComponent().setShouldNoClip(false);
                        teleport.playerComponent().sync();
                    }
                },
                new CrossDimensionTeleport(
                        playerComponent,
                        BackroomsLevels.LEVEL324_BACKROOMS_LEVEL.getSpawnPos(),
                        this,
                        BackroomsLevels.LEVEL324_BACKROOMS_LEVEL),
                (teleport, tick) -> {
                    teleport.playerComponent().setShouldNoClip(false);
                    teleport.playerComponent().sync();
                });
    }

    @Override
    public int nextEventDelay() {
        return random.nextInt(1000, 1200);
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {

    }

    @Override
    public void readFromNbt(NbtCompound nbt) {

    }

    @Override
    public void transitionOut(CrossDimensionTeleport crossDimensionTeleport) {
    }

    @Override
    public void transitionIn(CrossDimensionTeleport crossDimensionTeleport) {
        crossDimensionTeleport.playerComponent().player.fallDistance = 0;
    }

    @Override
    public BoolTextPair allowsTorch() {
        return new BoolTextPair(false, Text.translatable("spb-revamped.flashlight.wet1").append(Text.translatable("spb-revamped.flashlight.wet2").formatted(Formatting.RED)));
    }

    @Override
    public boolean hasVanillaLighting() {
        return true;
    }
}
