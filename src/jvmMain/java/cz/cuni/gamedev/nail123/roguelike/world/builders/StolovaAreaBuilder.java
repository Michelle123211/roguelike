package cz.cuni.gamedev.nail123.roguelike.world.builders;

import cz.cuni.gamedev.nail123.roguelike.blocks.Floor;
import cz.cuni.gamedev.nail123.roguelike.blocks.GameBlock;
import cz.cuni.gamedev.nail123.roguelike.blocks.Wall;
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Stairs;
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Door;
import cz.cuni.gamedev.nail123.roguelike.entities.unplacable.FogOfWar;
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding;
import cz.cuni.gamedev.nail123.utils.collections.ObservableMap;
import org.hexworks.zircon.api.data.Position3D;
import org.hexworks.zircon.api.data.Size3D;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;


public class StolovaAreaBuilder extends AreaBuilder {

    ObservableMap<Position3D, GameBlock> blocksTmp;

    int minSize = 7; // minimum size of the room

    Random random = new Random();

    ArrayList<Room> rooms; // for storing generated rooms

    public StolovaAreaBuilder(Size3D size, Size3D visibleSize) {
        super(size, visibleSize);
    }

    @NotNull
    @Override
    public AreaBuilder create() {
        return create(1);
    }

    public AreaBuilder create(int level) {
        // initialization
        this.blocksTmp = new ObservableMap<>();
        this.blocksTmp.put(Position3D.unknown(), new Floor());
        long seed = System.currentTimeMillis();
        System.out.println("SEED: " + seed);
        random.setSeed(seed);

        // generate rooms
        rooms = generateRooms();

        // assemble all necessary parts
        addFloor();
        addWalls();
        fillEmptySpace();
        setBlocks(blocksTmp);
        addDoor();
        Position3D playerCoords = addPlayer();
        addStairs(level, playerCoords);
        //addEntity(new FogOfWar(), playerCoords);

        return this;
    }

    // uses binary division algorithm to divide the space into cells
    private ArrayList<Room> generateRooms() {
        var res = generateRoomsRecursive(0, 0, getWidth(), getHeight(), 0);
        return res;
    }

    // performs one layer of the binary division
    //      divides the space into 2 parts and then call recursively
    //      if the space is already too small to be divided, a room (not occupating the whole cell) is created
    private ArrayList<Room> generateRoomsRecursive(int startX, int startY, int width, int height, int depth) {
        // decide whether to continue dividing or not - uses probability based on the depth of recursion
        boolean shouldContinue = random.nextDouble() < (1.2 - depth * 0.2);
        var res = new ArrayList<Room>();
        if (!shouldContinue || (width < 2 * minSize + 4 && height < 2 * minSize + 4)) {
            Room room = createRoom(startX, startY, width, height);
            res.add(room);
            return res;
        }
        ArrayList<Room> rooms1;
        ArrayList<Room> rooms2;
        // choose which side will be divided - the longer one is preferred but not definite
        int i = random.nextInt(4);
        boolean isReversed = false;
        if (width >= 2 * minSize + 4 && ((height < 2 * minSize + 4) || (i < 3 && width >= height) || (i == 3 && width < height))) {
            // vertical direction was chosen - swap all coordinates and dimensions (so that the algorithm may be simpler and cut only horizontally)
            isReversed = true;
            int tmp = width;
            width = height;
            height = tmp;
            tmp = startX;
            startX = startY;
            startY = tmp;
        }
        // divide horizontally
        // choose random number so that each part has at least minSize + 2
        int tmp = height - (2 * (minSize + 2));
        int cut = height / 2;
        if (tmp > 0) {
            cut = minSize + 2 + random.nextInt(height - (2 * (minSize + 2)));
        }
        // recursively divide each new part
        rooms1 = generateRoomsRecursive(startX, startY, width, cut, depth + 1);
        rooms2 = generateRoomsRecursive(startX, startY + cut, width, height - cut, depth + 1);
        // choose 2 rooms (one from each part) which will be connected with corridor, then connect them
        RoomInterface roomInterface = getPossibleCorridorStart(rooms1, rooms2, startY, cut);
        Room corridor = createCorridor(roomInterface.room1, roomInterface.room2);
        // add corridor to a list
        corridor.isCorridor = true;
        rooms1.add(corridor);

        // add rooms from both branches into the result array
        if (isReversed) {
            // the area was reversed at the beginning, reverse it back
            for (Room room : rooms1) res.add(room.reversed());
            for (Room room : rooms2) res.add(room.reversed());
        } else {
            for (Room room : rooms1) res.add(room);
            for (Room room : rooms2) res.add(room);
        }

        return res;
    }

    // generates room inside of the given area and returns it
    private Room createRoom(int startX, int startY, int width, int height) {
        Room room = new Room(startX, startY, width, height);
        // room must have some border of free space - first start with subtracting 1 from each side
        room.startX += 1;
        room.startY += 1;
        room.width -= 2;
        room.height -= 2;
        // then randomly subtract even more if possible (but the room must have at least minSize)
        int newWidth = room.width;
        if (newWidth > minSize) newWidth = minSize + random.nextInt(room.width - minSize);
        int newHeight = room.height;
        if (newHeight > minSize) newHeight = minSize + random.nextInt(room.height - minSize);
        int newX = room.startX;
        if (newWidth < room.width) newX += random.nextInt(room.width - newWidth + 1);
        int newY = room.startY;
        if (newHeight < room.height) newY += random.nextInt(room.height - newHeight + 1);
        room.startX = newX;
        room.startY = newY;
        room.width = newWidth;
        room.height = newHeight;

        return room;
    }

    // randomly chooses an interface of two rooms (one from each part of the are) which will be then connected with corridor
    private RoomInterface getPossibleCorridorStart(ArrayList<Room> rooms1, ArrayList<Room> rooms2, int startY, int cut) {
        // from both parts filter only rooms which are right next to the division line
        ArrayList<Room> filteredRooms1 = new ArrayList<>();
        ArrayList<Room> filteredRooms2 = new ArrayList<>();
        for (Room room : rooms1)
            if (room.cellY + room.cellHeight == startY + cut && !room.isCorridor) filteredRooms1.add(room);
        for (Room room : rooms2)
            if (room.cellY == startY + cut && !room.isCorridor) filteredRooms2.add(room);
        // sort them according to the coordinate which will determine corridor
        filteredRooms1.sort(Comparator.comparingInt(Room::getCellX));
        filteredRooms2.sort(Comparator.comparingInt(Room::getCellX));
        // find all interfaces of 2 rooms which are long enough to accommodate a corridor, add them to the list of possible corridor locations
        int index1 = 0, index2 = 0;
        ArrayList<Integer> possibleLocations = new ArrayList<>();
        while (index1 < filteredRooms1.size() && index2 < filteredRooms2.size()) {
            Room room1 = filteredRooms1.get(index1);
            Room room2 = filteredRooms2.get(index2);
            int union = Math.abs(Math.min(room1.cellX, room2.cellX) - Math.max(room1.cellX + room1.cellWidth, room2.cellX + room2.cellWidth));
            int overlap = room1.cellWidth + room2.cellWidth - union;
            if (overlap >= 5) // overlap must be at least 5 (= corridor width (3) + 1 at each side so that the rooms are not right next to the border of cell)
                possibleLocations.add(Math.max(room1.cellX, room2.cellX));
            if (room1.cellX + room1.cellWidth <= room2.cellX + room2.cellWidth) ++index1;
            if (room2.cellX + room2.cellWidth <= room1.cellX + room1.cellWidth) ++ index2;
        }
        // choose randomly one of the interfaces (the corridor will run through it)
        int coord = possibleLocations.get(random.nextInt(possibleLocations.size()));
        // find 2 neighbouring rooms
        Room room1 = new Room(0, 0, 0, 0);
        Room room2 = new Room(0, 0, 0, 0);;
        for (Room room : filteredRooms1)
            if (coord >= room.cellX && coord < room.cellX + room.cellWidth)
                room1 = room;
        for (Room room : filteredRooms2)
            if (coord >= room.cellX && coord < room.cellX + room.cellWidth)
                room2 = room;
        // return the interface with the rooms
        return new RoomInterface(coord, room1, room2);
    }

    // creates a corridor between the two given rooms
    private Room createCorridor(Room room1, Room room2) {
        // choose exact corridor location and create the corridor
        Room corridor = new Room(0, 0, 3, 0);
        int union = Math.abs(Math.min(room1.startX, room2.startX) - Math.max(room1.startX + room1.width, room2.startX + room2.width));
        int overlap = room1.width + room2.width - union;
        if (overlap >= 3) {
            // if the rooms are overlapping enough, choose the middle of the overlap
            corridor.startX = Math.max(room1.startX, room2.startX) + overlap / 2 - 1;
        } else {
            // otherwise choose the middle between the maximal startX and minimal endX and prolong the rooms if necessary
            corridor.startX = (Math.min(room1.startX + room1.width, room2.startX + room2.width) + Math.max(room1.startX, room2.startX)) / 2 - 1;
            // if the coordinate is outside of the bound of a cell, then move it (there must be at least 2 free spaces around)
            while (corridor.startX < room1.cellX + 1 || corridor.startX < room2.cellX + 1)
                ++corridor.startX;
            while (corridor.startX > room1.cellX + room1.cellWidth - corridor.width - 1 || corridor.startX > room2.cellX + room2.cellWidth - corridor.width - 1)
                --corridor.startX;
            prolongRoom(room1, corridor.startX + 1);
            prolongRoom(room2, corridor.startX + 1);
        }
        // connect everything
        corridor.startY = room1.startY + room1.height - 1;
        corridor.height = room2.startY - corridor.startY + 1;
        room1.corridors.add(corridor);
        room2.corridors.add(corridor);
        return corridor;
    }

    // adds floor for all the rooms into this.blocksTmp
    private void addFloor() {
        for (Room room : rooms) {
            if (!room.isCorridor) {
                for (int x = 1; x < room.width - 1; ++x) { // +1 and -1 because floor is only inside
                    for (int y = 1; y < room.height - 1; ++y) { // +1 and -1 because floor is only inside
                        Position3D pos = Position3D.create(room.startX + x, room.startY + y, 0);
                        blocksTmp.put(pos, new Floor());
                    }
                }
            } else {
                // corridors are treated differently - they have walls only on 2 of 4 sides which also affects floor placing
                if (room.isVertical) {
                    for (int x = 1; x < room.width - 1; ++x) {
                        for (int y = 0; y < room.height; ++y) {
                            Position3D pos = Position3D.create(room.startX + x, room.startY + y, 0);
                            blocksTmp.put(pos, new Floor());
                        }
                    }
                } else {
                    for (int x = 0; x < room.width; ++x) {
                        for (int y = 1; y < room.height - 1; ++y) {
                            Position3D pos = Position3D.create(room.startX + x, room.startY + y, 0);
                            blocksTmp.put(pos, new Floor());
                        }
                    }
                }
            }
        }
    }

    // adds walls around all the rooms into this.blocksTmp
    //      must be called after addFloor()
    private void addWalls() {
        for (Room room : rooms) {
            // up and bottom
            for (int i = 0; i < room.width; ++i) {
                Position3D pos1 = Position3D.create(room.startX + i, room.startY, 0);
                Position3D pos2 = Position3D.create(room.startX + i, room.startY + room.height - 1, 0);
                if (!blocksTmp.containsKey(pos1))
                    blocksTmp.put(pos1, new Wall());
                if (!blocksTmp.containsKey(pos2))
                    blocksTmp.put(pos2, new Wall());
            }
            // left and right
            for (int i = 0; i < room.height; ++i) {
                Position3D pos1 = Position3D.create(room.startX, room.startY + i, 0);
                Position3D pos2 = Position3D.create(room.startX + room.width - 1, room.startY + i, 0);
                if (!blocksTmp.containsKey(pos1))
                    blocksTmp.put(pos1, new Wall());
                if (!blocksTmp.containsKey(pos2))
                    blocksTmp.put(pos2, new Wall());
            }
        }
    }

    // adds door between each room and corridor
    private void addDoor() {
        for (Room room : rooms) {
            if (!room.isCorridor) {
                for (Room corridor : room.corridors) {
                    if (corridor.startX < room.startX) {
                        // from left
                        addEntity(new Door(), Position3D.create(room.startX, corridor.startY + 1, 0));
                    } else if (corridor.startX >= room.startX + room.width - 1) {
                        // from right
                        addEntity(new Door(), Position3D.create(room.startX + room.width - 1, corridor.startY + 1, 0));
                    } else if (corridor.startY < room.startY) {
                        // from up
                        addEntity(new Door(), Position3D.create(corridor.startX + 1, room.startY, 0));
                    } else {
                        // from bottom
                        addEntity(new Door(), Position3D.create(corridor.startX + 1, room.startY + room.height - 1, 0));
                    }
                }
            }
        }
    }

    // adds player in the middle of some room
    private Position3D addPlayer() {
        Room room = rooms.get(0);
        int playerX = room.startX + (room.width / 2);
        int playerY = room.startY + (room.height / 2);
        Position3D pos = Position3D.create(playerX, playerY, 0);

        addEntity(getPlayer(), pos);

        return pos;
    }

    // adds stairs to the next level reasonably far away from the player
    //      level parameter is not important at the moment (it would be in case of enabling ascending to the previous level)
    //      must be called after addPlayer()
    private void addStairs(int level, Position3D playerPosition) {
        // add stairs up
        //if (level > 1) addEntity(new Stairs(false), playerPosition);

        // add stairs down
        var floodFill = Pathfinding.INSTANCE.floodFill(playerPosition, this, Pathfinding.INSTANCE.getEightDirectional(), Pathfinding.INSTANCE.getDoorOpening());
        int maxDist = 0;
        for (var position : floodFill.keySet()) {
            int dist = floodFill.get(position);
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        // get positions which are at most 75 % of the maximum distance away
        ArrayList<Position3D> possiblePositions = new ArrayList<>();
        for (var position : floodFill.keySet()) {
            int dist = floodFill.get(position);
            if (dist > 0.75 * (double)maxDist) {
                possiblePositions.add(position);
            }
        }
        // randomly choose one position and place stairs there
        Position3D stairsPos = possiblePositions.get(random.nextInt(possiblePositions.size()));
        addEntity(new Stairs(true), stairsPos);
    }

    // fills each space which is not occupied by rooms with wall
    private void fillEmptySpace() {
        for (int x = 0; x < getWidth(); ++x) {
            for (int y = 0; y < getHeight(); y++) {
                var pos = Position3D.create(x, y, 0);
                if (!blocksTmp.containsKey(pos))
                    blocksTmp.put(pos, new Wall());
            }
        }
    }

    // changes size of the given room in one direction so that it is connected to the corridor at the given coordinate
    private void prolongRoom(Room room, int corridorX) {
        if (corridorX >= room.startX + 1 && corridorX < room.startX + room.width - 1) {
            // the corridor is already connected to the room, not necessary to change size
        } else if (corridorX < room.startX + 1) {
            // the room must be prolonged in the direction to smaller x coordinate
            int newX = corridorX - 1;
            // if possible, prolong the room even more (not just exactly to the corridor)
            if (newX > room.cellX + 2)
                newX -= random.nextInt(Math.min(2, corridorX - room.cellX - 1));
            room.width += room.startX - newX;
            room.startX = newX;
        } else {
            // the room must be prolonged in the direction to larger x coordinate
            int newWidth = room.width + corridorX - (room.startX + room.width) + 2;
            // if possible, prolong the room even more (not just exactly to the corridor)
            if (room.startX + newWidth < room.cellX + room.cellWidth - 2)
                newWidth += random.nextInt(Math.min(2, room.cellX + room.cellWidth - room.startX - newWidth));
            room.width = newWidth;
        }
    }

    // represents interface of two rooms/cells - used to connect these rooms with a corridor
    private class RoomInterface {
        public int coord; // starting coordinate of the interface
        public Room room1;
        public Room room2;

        public RoomInterface(int coord, Room room1, Room room2) {
            this.coord = coord;
            this.room1 = room1;
            this.room2 = room2;
        }
    }

    // represents one room (either room or corridor)
    //      using coordinates of the upper left corner, width and height
    private class Room {
        // room's dimension
        public int startX;
        public int startY;
        public int width;
        public int height;
        // the surrounding cell
        public int cellX;
        public int cellY;
        public int cellWidth;
        public int cellHeight;
        // connected corridors
        public ArrayList<Room> corridors;
        // fields specific for corridors
        public boolean isCorridor;
        public boolean isVertical;

        public Room(int startX, int startY, int width, int height) {
            this.startX = startX;
            this.startY = startY;
            this.width = width;
            this.height = height;

            this.cellX = startX;
            this.cellY = startY;
            this.cellWidth = width;
            this.cellHeight = height;

            this.corridors = new ArrayList<>();

            this.isCorridor = false;
            this.isVertical = true;
        }

        public int getCellX() {
            return cellX;
        }

        // returns reversed room - swaps all coordinates, dimensions
        public Room reversed() {
            Room res = new Room(this.cellY, this.cellX, this.cellHeight, this.cellWidth);
            res.startX = this.startY;
            res.startY = this.startX;
            res.width = this.height;
            res.height = this.width;
            res.corridors = new ArrayList<>();
            for (Room corridor : this.corridors)
                res.corridors.add(corridor.reversed());
            res.isCorridor = this.isCorridor;
            res.isVertical = !this.isVertical;
            return res;
        }

    }
}
