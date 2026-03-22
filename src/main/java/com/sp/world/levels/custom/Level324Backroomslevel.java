package com.sp.world.levels.custom;

import com.sp.init.BackroomsLevels;
import com.sp.world.events.generic.lights.LightLevelFlicker;
import com.sp.world.generation.chunk_generator.Level324ChunkGenerator;
import com.sp.world.levels.BackroomsLevel;
import com.sp.world.levels.BackroomsLevelWithLights;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

public class Level324Backroomslevel extends BackroomsLevel implements BackroomsLevelWithLights {
    private Level0BackroomsLevel.LightState lightState = BackroomsLevelWithLights.LightState.ON;

    public Level324Backroomslevel() {
        super("level324", Level324ChunkGenerator.CODEC, new Vec3d(52,65,21), BackroomsLevels.LEVEL324_WORLD_KEY);

        this.registerEvent("flicker", LightLevelFlicker::new);
    }

    @Override
    public boolean rendersClouds() {
        return false;
    }

    @Override
    public boolean rendersSky() {
        return false;
    }

    @Override
    public int nextEventDelay() {
        return 0;
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        nbt.putString("lightState", lightState.name());
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        this.lightState = BackroomsLevelWithLights.LightState.valueOf(nbt.getString("lightState"));

    }

    @Override
    public void transitionOut(CrossDimensionTeleport crossDimensionTeleport) {

    }

    @Override
    public void transitionIn(CrossDimensionTeleport crossDimensionTeleport) {
        crossDimensionTeleport.playerComponent().player.fallDistance = 0;
    }

    public void setLightState(Level0BackroomsLevel.LightState lightState) {
        this.justChanged();
        this.lightState = lightState;
    }

    public Level0BackroomsLevel.LightState getLightState() {
        return this.lightState;
    }
}
