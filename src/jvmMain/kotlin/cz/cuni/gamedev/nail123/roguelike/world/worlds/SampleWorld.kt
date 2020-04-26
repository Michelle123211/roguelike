package cz.cuni.gamedev.nail123.roguelike.world.worlds

import cz.cuni.gamedev.nail123.roguelike.entities.Stairs
import cz.cuni.gamedev.nail123.roguelike.events.logMessage
import cz.cuni.gamedev.nail123.roguelike.world.Area
import cz.cuni.gamedev.nail123.roguelike.world.World
import cz.cuni.gamedev.nail123.roguelike.world.builders.EmptyAreaBuilder
import org.hexworks.zircon.api.data.Position3D
import org.hexworks.zircon.api.data.Size3D

/**
 * Sample world, made as a starting point for creating custom worlds.
 * It consists of separate levels - each one has one staircase, and it leads infinitely deep.
 */
class SampleWorld: World() {
    var currentLevel = 0

    init {
        // We need to create the first area and enter it
        val level = buildLevel()
        areas.add(level)
        goToArea(level)
    }

    /**
     * Builds one of the levels.
     */
    fun buildLevel(): Area {
        // Start with an empty area
        val areaBuilder = EmptyAreaBuilder().create()

        // Place the player at an empty location in the top-left quarter
        areaBuilder.addAtEmptyPosition(
                areaBuilder.player,
                Position3D.create(1, 1, 0),
                Size3D.create(areaBuilder.width / 2 - 2, areaBuilder.height / 2 - 2, 1)
        )
        // Place the stairs at an empty location in the top-right quarter
        areaBuilder.addAtEmptyPosition(
                Stairs(),
                Position3D.create(areaBuilder.width / 2, areaBuilder.height / 2, 0),
                Size3D.create(areaBuilder.width / 2 - 2, areaBuilder.height / 2 - 2, 1)
        )

        // Build it into a full Area
        return areaBuilder.build()
    }

    /**
     * Moving down - goes to a brand new level.
     */
    override fun moveDown() {
        ++currentLevel
        this.logMessage("Descended to level ${currentLevel + 1}")
        if (currentLevel >= areas.size) areas.add(buildLevel())
        goToArea(areas[currentLevel])
    }

    /**
     * Moving up would be for revisiting past levels, we do not need that. Check [DungeonWorld] for an implementation.
     */
    override fun moveUp() {
        // Not implemented
    }
}