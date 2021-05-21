package cz.cuni.gamedev.nail123.roguelike.tiles

import cz.cuni.gamedev.nail123.roguelike.world.Direction
import cz.cuni.gamedev.nail123.roguelike.blocks.Floor
import org.hexworks.zircon.api.CP437TilesetResources
import org.hexworks.zircon.api.color.TileColor
import org.hexworks.zircon.api.data.Tile
import org.hexworks.zircon.api.resource.TilesetResource


object GameTiles {

    val defaultCharTileset = CP437TilesetResources.rogueYun16x16()
    val defaultGraphicalTileset = TilesetResources.stolova3_16x16

    val EMPTY: Tile = Tile.empty()

    // Allowed characters for tiles are https://en.wikipedia.org/wiki/Code_page_437
    val FLOOR = graphicalTile("Void")//graphicalTile("Room 46") //characterTile('.', GameColors.FLOOR_FOREGROUND, GameColors.FLOOR_BACKGROUND)

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
            graphicalTile("Void"),


            all8 to graphicalTile("Room 46"),

            // one missing
            all8 - Direction.NORTH_WEST.flag to graphicalTile("Room 45"),
            all8 - Direction.NORTH_EAST.flag to graphicalTile("Room 44"),
            all8 - Direction.SOUTH_WEST.flag to graphicalTile("Room 41"),
            all8 - Direction.SOUTH_EAST.flag to graphicalTile("Room 33"),

            // two missing
            all8 - Direction.NORTH_EAST.flag - Direction.SOUTH_WEST.flag to graphicalTile("Room 39"),
            all8 - Direction.SOUTH_EAST.flag - Direction.SOUTH_WEST.flag to graphicalTile("Room 25"),
            all8 - Direction.NORTH_WEST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 32"),
            all8 - Direction.NORTH_EAST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 31"),
            all8 - Direction.NORTH_WEST.flag - Direction.SOUTH_WEST.flag to graphicalTile("Room 40"),
            all8 - Direction.NORTH_WEST.flag - Direction.NORTH_EAST.flag to graphicalTile("Room 43"),

            // three missing
            all8 - Direction.SOUTH_WEST.flag - Direction.SOUTH.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 12"),
            all8 - Direction.SOUTH_WEST.flag - Direction.NORTH_WEST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 24"),
            all8 - Direction.NORTH_EAST.flag - Direction.EAST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 28"),
            all8 - Direction.NORTH_WEST.flag - Direction.WEST.flag - Direction.SOUTH_WEST.flag to graphicalTile("Room 36"),
            all8 - Direction.NORTH_WEST.flag - Direction.NORTH_EAST.flag - Direction.SOUTH_WEST.flag to graphicalTile("Room 38"),
            all8 - Direction.NORTH_WEST.flag - Direction.NORTH.flag - Direction.NORTH_EAST.flag to graphicalTile("Room 42"),
            all8 - Direction.NORTH_WEST.flag - Direction.NORTH_EAST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 30"),
            all8 - Direction.NORTH_EAST.flag - Direction.SOUTH_WEST.flag - Direction.SOUTH_EAST.flag to graphicalTile("Room 23"),

            // four neighbours
            Direction.NORTH_WEST.flag + Direction.NORTH.flag + Direction.WEST.flag + Direction.EAST.flag to graphicalTile("Room 10"),
            Direction.NORTH_EAST.flag + Direction.NORTH.flag + Direction.WEST.flag + Direction.EAST.flag to graphicalTile("Room 11"),
            Direction.NORTH_WEST.flag + Direction.NORTH.flag + Direction.SOUTH.flag + Direction.WEST.flag to graphicalTile("Room 17"),
            Direction.NORTH_EAST.flag + Direction.NORTH.flag + Direction.SOUTH.flag + Direction.EAST.flag to graphicalTile("Room 20"),
            Direction.NORTH.flag + Direction.SOUTH.flag + Direction.EAST.flag + Direction.WEST.flag to graphicalTile("Room 22"),
            Direction.NORTH.flag + Direction.WEST.flag + Direction.SOUTH.flag + Direction.SOUTH_WEST.flag to graphicalTile("Room 27"),
            Direction.SOUTH.flag + Direction.WEST.flag + Direction.EAST.flag + Direction.SOUTH_WEST.flag to graphicalTile("Room 29"),
            Direction.NORTH.flag + Direction.EAST.flag + Direction.SOUTH.flag + Direction.SOUTH_EAST.flag to graphicalTile("Room 35"),
            Direction.WEST.flag + Direction.EAST.flag + Direction.SOUTH.flag + Direction.SOUTH_EAST.flag to graphicalTile("Room 37"),

            // three neighbours
            Direction.WEST.flag + Direction.NORTH.flag + Direction.NORTH_WEST.flag to graphicalTile("Room 4"),
            Direction.EAST.flag + Direction.NORTH.flag + Direction.NORTH_EAST.flag to graphicalTile("Room 7"),
            Direction.NORTH.flag + Direction.SOUTH.flag + Direction.EAST.flag to graphicalTile("Room 19"),
            Direction.WEST.flag + Direction.SOUTH.flag + Direction.EAST.flag to graphicalTile("Room 21"),
            Direction.WEST.flag + Direction.SOUTH.flag + Direction.SOUTH_WEST.flag to graphicalTile("Room 26"),
            Direction.EAST.flag + Direction.SOUTH.flag + Direction.SOUTH_EAST.flag to graphicalTile("Room 34"),
            Direction.NORTH.flag + Direction.EAST.flag + Direction.WEST.flag to graphicalTile("Room 9"),
            Direction.NORTH.flag + Direction.WEST.flag + Direction.SOUTH.flag to graphicalTile("Room 16"),

            // two neighbours
            Direction.NORTH.flag + Direction.WEST.flag to graphicalTile("Room 3"),
            Direction.NORTH.flag + Direction.EAST.flag to graphicalTile("Room 6"),
            Direction.EAST.flag + Direction.WEST.flag to graphicalTile("Room 8"),
            Direction.NORTH.flag + Direction.SOUTH.flag to graphicalTile("Room 14"),
            Direction.WEST.flag + Direction.SOUTH.flag to graphicalTile("Room 15"),
            Direction.EAST.flag + Direction.SOUTH.flag to graphicalTile("Room 18"),


            // TODO: 39

            // one neighbour
            Direction.NORTH.flag to graphicalTile("Room 1"),
            Direction.WEST.flag to graphicalTile("Room 2"),
            Direction.EAST.flag to graphicalTile("Room 5"),
            Direction.SOUTH.flag to graphicalTile("Room 13"),

            // no neighbour
            0 to graphicalTile("Room 47"),

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