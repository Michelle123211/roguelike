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
    val FLOOR = characterTile('.', GameColors.FLOOR_FOREGROUND, GameColors.FLOOR_BACKGROUND)

    // Wall tile is replaced by autotiling in Wall.kt
    val WALL = characterTile('#', GameColors.WALL_FOREGROUND, GameColors.WALL_BACKGROUND)

    val PLAYER = graphicalTile("Female")
    val CLOSED_DOOR = graphicalTile("Door closed")
    val OPEN_DOOR = graphicalTile("Door open")
    val STAIRS_DOWN = graphicalTile("Stairs down")
    val STAIRS_UP = graphicalTile("Stairs up")
    val BLACK = characterTile(' ', GameColors.BLACK, GameColors.BLACK)

    val RAT = graphicalTile("Animal 1")

    val SWORD = graphicalTile("Magic wand")

    // Autotiling tries to find a tile by whether similar tiles neighbor in some directions
    // It iterates through a list of Directional flags (which must be fulfilled) to tiles that should be used
    // It uses the first one found
    private val all8 = Direction.eightDirections.sumBy { it.flag }
    val wallTiling = Autotiling(
            // Default
            graphicalTile("Wall 2"),
            // Walls all around
            all8 to graphicalTile("Floor"),
            // Walls everywhere except one corner
            all8 - Direction.NORTH_WEST.flag to graphicalTile("Wall 7"),
            all8 - Direction.NORTH_EAST.flag to graphicalTile("Wall 6"),
            all8 - Direction.SOUTH_WEST.flag to graphicalTile("Wall 3"),
            all8 - Direction.SOUTH_EAST.flag to graphicalTile("Wall 1"),
            // Lines
            // TODO: Differenciate between left and right Wall 4, up and bottom Wall 2
            Direction.NORTH + Direction.SOUTH to graphicalTile("Wall 4"),
            Direction.WEST + Direction.EAST to graphicalTile("Wall 2"),
            // Corners
            Direction.NORTH + Direction.EAST to graphicalTile("Wall 6"),
            Direction.NORTH + Direction.WEST to graphicalTile("Wall 7"),
            Direction.SOUTH + Direction.EAST to graphicalTile("Wall 1"),
            Direction.SOUTH + Direction.WEST to graphicalTile("Wall 3"),
            // Single adjacent (horizontal ones fallback to default)
            Direction.NORTH.flag to graphicalTile("Wall 4"),
            Direction.SOUTH.flag to graphicalTile("Wall 4")
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