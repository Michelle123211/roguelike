package cz.cuni.gamedev.nail123.roguelike.world.worlds;

import cz.cuni.gamedev.nail123.roguelike.GameConfig;
import cz.cuni.gamedev.nail123.roguelike.entities.enemies.Rat;
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Stairs;
import cz.cuni.gamedev.nail123.roguelike.entities.unplacable.FogOfWar;
import cz.cuni.gamedev.nail123.roguelike.events.LoggedEvent;
import cz.cuni.gamedev.nail123.roguelike.world.Area;
import cz.cuni.gamedev.nail123.roguelike.world.World;
import cz.cuni.gamedev.nail123.roguelike.world.builders.AreaBuilder;
import cz.cuni.gamedev.nail123.roguelike.world.builders.StolovaAreaBuilder;
import org.hexworks.zircon.api.data.Position3D;
import org.hexworks.zircon.api.data.Size3D;
import org.jetbrains.annotations.NotNull;
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding;

import java.util.ArrayList;
import java.util.Random;

public class StolovaJavaWorld extends World {
    int currentLevel = 0;

    AreaBuilder areaBuilder;

    Random random;

    public StolovaJavaWorld() {
    }

    @NotNull
    @Override
    public Area buildStartingArea() {
        return buildLevel();
    }

    Area buildLevel() {
        this.random = new Random();
        random.setSeed(System.currentTimeMillis());

        // Start with an empty area
        this.areaBuilder = (new StolovaAreaBuilder(GameConfig.INSTANCE.getAREA_SIZE(), GameConfig.INSTANCE.getVISIBLE_SIZE())).create();

        for (int i = 0; i <= currentLevel; ++i) {
            areaBuilder.addAtEmptyPosition(new Rat(), Position3D.defaultPosition(), areaBuilder.getSize());
        }

        // Build it into a full Area
        return areaBuilder.build();
    }

    /**
     * Moving down - goes to a brand new level.
     */
    @Override
    public void moveDown() {
        ++currentLevel;
        (new LoggedEvent(this, "Descended to level " + (currentLevel + 1))).emit();
        if (currentLevel >= getAreas().getSize()) getAreas().add(buildLevel());
        goToArea(getAreas().get(currentLevel));
    }

    /**
     * Moving up would be for revisiting past levels, we do not need that. Check [DungeonWorld] for an implementation.
     */
    @Override
    public void moveUp() {
        // Not implemented
    }
}
