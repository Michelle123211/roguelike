package cz.cuni.gamedev.nail123.roguelike.tiles

import cz.cuni.gamedev.nail123.roguelike.world.Direction
import org.hexworks.zircon.api.CP437TilesetResources
import org.hexworks.zircon.api.color.TileColor
import org.hexworks.zircon.api.data.Tile
import org.hexworks.zircon.api.resource.TilesetResource


object GameTiles {

    val defaultCharTileset = CP437TilesetResources.rogueYun16x16()
    val defaultGraphicalTileset = TilesetResources.stolova16x16

    val EMPTY: Tile = Tile.empty()

    // Allowed characters for tiles are https://en.wikipedia.org/wiki/Code_page_437
    val FLOOR = graphicalTile("Floor")

    // Wall tile is replaced by autotiling in Wall.kt
    val WALL = characterTile('#', GameColors.WALL_FOREGROUND, GameColors.WALL_BACKGROUND)

    val PLAYER = graphicalTile("Female")
    val CLOSED_DOOR = graphicalTile("Door closed")
    val OPEN_DOOR = graphicalTile("Door open")
    val STAIRS_DOWN = graphicalTile("Stairs down")
    val STAIRS_UP = graphicalTile("Stairs up")
    val BLACK = characterTile(' ', GameColors.BLACK, GameColors.BLACK)

    val RAT = graphicalTile("Animal")

    val SWORD = graphicalTile("Magic wand")

    // Autotiling tries to find a tile by whether similar tiles neighbor in some directions
    // It iterates through a list of Directional flags (which must be fulfilled) to tiles that should be used
    // It uses the first one found
    private val all8 = Direction.eightDirections.sumBy { it.flag }
    val wallTiling = Autotiling(
            // Default
            graphicalTile("Void"),


            all8 to graphicalTile("Void"),

            // outer corners of the rooms
            all8 - Direction.NORTH_WEST.flag to graphicalTile("Wall outer N W"),
            all8 - Direction.NORTH_EAST.flag to graphicalTile("Wall outer N E"),
            all8 - Direction.SOUTH_WEST.flag to graphicalTile("Wall outer S W"),
            all8 - Direction.SOUTH_EAST.flag to graphicalTile("Wall outer S E"),

            // straight walls around rooms
            all8 - Direction.NORTH_WEST.flag - Direction.NORTH.flag - Direction.NORTH_EAST.flag to graphicalTile("Wall down"),
            all8 - Direction.NORTH_WEST.flag - Direction.WEST.flag - Direction.SOUTH_WEST.flag to graphicalTile("Wall right"),
            all8 - Direction.NORTH_EAST.flag - Direction.EAST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Wall left"),
            all8 - Direction.SOUTH_WEST.flag - Direction.SOUTH.flag - Direction.SOUTH_EAST.flag to graphicalTile("Wall up"),

            // inner corners to the corridors
            Direction.SOUTH.flag + Direction.EAST.flag + Direction.SOUTH_EAST.flag to graphicalTile("Wall inner S E"),
            Direction.SOUTH.flag + Direction.WEST.flag + Direction.SOUTH_WEST.flag to graphicalTile("Wall inner S W"),
            Direction.NORTH.flag + Direction.EAST.flag + Direction.NORTH_EAST.flag to graphicalTile("Wall inner N E"),
            Direction.NORTH.flag + Direction.WEST.flag + Direction.NORTH_WEST.flag to graphicalTile("Wall inner N W"),
    )

    fun characterTile(char: Char,
                      foreground: TileColor = GameColors.OBJECT_FOREGROUND,
                      background: TileColor = TileColor.transparent()): Tile {

        return Tile.newBuilder()
                   .withCharacter(char)
                   .withForegroundColor(foreground)
                   .withBackgroundColor(background)
                   .build()
    }

    fun graphicalTile(tag: String, tileset: TilesetResource = defaultGraphicalTileset): Tile {
        return Tile.newBuilder()
                   .withName(tag)
                   .withTags(tag.split(' ').toSet())
                   .withTileset(tileset)
                   .buildGraphicalTile()
    }
}