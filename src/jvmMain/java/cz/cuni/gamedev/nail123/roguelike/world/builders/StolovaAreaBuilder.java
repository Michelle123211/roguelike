package cz.cuni.gamedev.nail123.roguelike.world.builders;

import cz.cuni.gamedev.nail123.roguelike.GameConfig;
import cz.cuni.gamedev.nail123.roguelike.blocks.Floor;
import cz.cuni.gamedev.nail123.roguelike.blocks.GameBlock;
import cz.cuni.gamedev.nail123.roguelike.blocks.Wall;
import cz.cuni.gamedev.nail123.roguelike.entities.Player;
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Stairs;
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Door;
import cz.cuni.gamedev.nail123.roguelike.entities.unplacable.FogOfWar;
import cz.cuni.gamedev.nail123.roguelike.extensions.PositionExtensionsKt;
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding;
import cz.cuni.gamedev.nail123.roguelike.world.builders.AreaBuilder;
import cz.cuni.gamedev.nail123.utils.collections.ObservableMap;
import org.hexworks.zircon.api.data.Position;
import org.hexworks.zircon.api.data.Position3D;
import org.hexworks.zircon.api.data.Size3D;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;


public class StolovaAreaBuilder extends AreaBuilder {

    ObservableMap<Position3D, GameBlock> blocksTmp;

    int minSize = 7;

    Random random = new Random();

    ArrayList<Room> rooms;
    ArrayList<Room> corridors;

    public StolovaAreaBuilder(Size3D size, Size3D visibleSize) {
        super(size, visibleSize);
    }

    @NotNull
    @Override
    public AreaBuilder create() {
        return create(1);
    }

    public AreaBuilder create(int level) {
        this.blocksTmp = new ObservableMap<>();
        this.blocksTmp.put(Position3D.unknown(), new Floor());
        long seed = System.currentTimeMillis();
        System.out.println("SEED: " + seed);
        random.setSeed(seed);
        corridors = new ArrayList<>();
        rooms = generateRooms();
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

    private ArrayList<Room> generateRooms() {
        var res = generateRoomsRecursive(0, 0, getWidth(), getHeight(), 0);
        return res;
    }

    private ArrayList<Room> generateRoomsRecursive(int startX, int startY, int width, int height, int depth) {
        System.out.println("************************************");
        System.out.println("Depth = " + depth);
        // decide whether to continue dividing or not
        boolean shouldContinue = random.nextDouble() < (1.2 - depth * 0.2);
        var res = new ArrayList<Room>();
        if (!shouldContinue || (width < 2 * minSize + 4 && height < 2 * minSize + 4)) {
            // generate room inside of the current cell and return it
            Room room = new Room(startX, startY, width, height);
            // room must have some border of free space
            room.startX += 1;
            room.startY += 1;
            room.width -= 2;
            room.height -= 2;
            System.out.println("Making room smaller");
            System.out.println("Width = " + width);
            System.out.println("Height = " + height);
            System.out.println("StartX = " + startX);
            System.out.println("StartY = " + startY);
            int newWidth = room.width;
            if (newWidth > minSize) newWidth = minSize + random.nextInt(room.width - minSize);
            int newHeight = room.height;
            if (newHeight > minSize) newHeight = minSize + random.nextInt(room.height - minSize);
            int newX = room.startX;
            if (newWidth < room.width) newX += random.nextInt(room.width - newWidth + 1);
            int newY = room.startY;
            if (newHeight < room.height) newY += random.nextInt(room.height - newHeight + 1);
            System.out.println("New width = " + newWidth);
            System.out.println("New height = " + newHeight);
            System.out.println("NewX = " + newX);
            System.out.println("NewY = " + newY);
            room.startX = newX;
            room.startY = newY;
            room.width = newWidth;
            room.height = newHeight;
            res.add(room);
            System.out.println("------------------");
            return res;
        }
        ArrayList<Room> rooms1;
        ArrayList<Room> rooms2;
        // choose direction
        int i = random.nextInt(4);
        boolean isHorizontal = false;
        if (width >= 2 * minSize + 4 && ((height < 2 * minSize + 4) || (i < 3 && width >= height) || (i == 3 && width < height))) {
            // it needs to be divided vertically
            isHorizontal = true;
            int tmp = width;
            width = height;
            height = tmp;
            tmp = startX;
            startX = startY;
            startY = tmp;
        }
        // divide horizontally
        // choose random number so that each part has at least minimal size + 2
        System.out.println("startX = " + startX);
        System.out.println("startY = " + startY);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        int tmp = height - (2 * (minSize + 2));
        int cut = height / 2;
        if (tmp > 0) {
            cut = minSize + 2 + random.nextInt(height - (2 * (minSize + 2)));
        }
        System.out.println("Cut = " + cut);
        System.out.println("wasReversed = " + isHorizontal);
        System.out.println("=============================");
        // recursively call the rest for each part (use steps-1)
        rooms1 = generateRoomsRecursive(startX, startY, width, cut, depth + 1);
        rooms2 = generateRoomsRecursive(startX, startY + cut, width, height - cut, depth + 1);

        // from both parts filter only rooms which are right next to the division line
        ArrayList<Room> filteredRooms1 = new ArrayList<>();
        ArrayList<Room> filteredRooms2 = new ArrayList<>();
        for (Room room : rooms1)
            if (room.cellY + room.cellHeight == startY + cut && !room.isCorridor) filteredRooms1.add(room);
        for (Room room : rooms2)
            if (room.cellY == startY + cut && !room.isCorridor) filteredRooms2.add(room);
        // sort them according to the coordinate which will determine corridor (cellX/cellY)
        filteredRooms1.sort(Comparator.comparingInt(Room::getCellX));
        filteredRooms2.sort(Comparator.comparingInt(Room::getCellX));

        // find all interfaces of 2 rooms which are long enough, add them to the list for possible corridor locations
        int index1 = 0, index2 = 0;
        ArrayList<Integer> possibleLocations = new ArrayList<>();
        while (index1 < filteredRooms1.size() && index2 < filteredRooms2.size()) {
            Room room1 = filteredRooms1.get(index1);
            Room room2 = filteredRooms2.get(index2);
            int union = Math.abs(Math.min(room1.cellX, room2.cellX) - Math.max(room1.cellX + room1.cellWidth, room2.cellX + room2.cellWidth));
            int overlap = room1.cellWidth + room2.cellWidth - union;
            if (overlap >= 5)
                possibleLocations.add(Math.max(room1.cellX, room2.cellX));
            if (room1.cellX + room1.cellWidth <= room2.cellX + room2.cellWidth) ++index1;
            if (room2.cellX + room2.cellWidth <= room1.cellX + room1.cellWidth) ++ index2;
        }

        // choose randomly one of the interfaces
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
            //corridor.startX = (Math.max(room1.startX + room1.width, room2.startX + room2.width) - Math.min(room1.startX, room2.startX)) / 2 - 1;
            // if the coordinate is outside of the bound of a cell, then move it (there must be at least 2 free spaces around)
            while (corridor.startX < room1.cellX + 1 || corridor.startX < room2.cellX + 1)
                    ++corridor.startX;
            while (corridor.startX > room1.cellX + room1.cellWidth - corridor.width - 1 || corridor.startX > room2.cellX + room2.cellWidth - corridor.width - 1)
                    --corridor.startX;
            prolongRoom(room1, corridor.startX + 1);
            prolongRoom(room2, corridor.startX + 1);
        }
        corridor.startY = room1.startY + room1.height - 1;
        corridor.height = room2.startY - corridor.startY + 1;
        room1.corridors.add(corridor);
        room2.corridors.add(corridor);

        // add corridor to a list
        corridor.isCorridor = true;
        System.out.println("===============");
        System.out.println("Corridor chosen");
        logRoom(corridor);
        System.out.println("===============");
        rooms1.add(corridor);

        // add rooms from both branches into the result array
        if (isHorizontal) {
            for (Room room : rooms1) res.add(room.reversed());
            for (Room room : rooms2) res.add(room.reversed());
        } else {
            for (Room room : rooms1) res.add(room);
            for (Room room : rooms2) res.add(room);
        }
        // return result
        return res;
    }

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

    private void addFloor() {
        for (Room room : rooms) {
            if (!room.isCorridor) {
                System.out.println("Outputting room");
                for (int x = 1; x < room.width - 1; ++x) {
                    for (int y = 1; y < room.height - 1; ++y) {
                        Position3D pos = Position3D.create(room.startX + x, room.startY + y, 0);
                        blocksTmp.put(pos, new Floor());
                    }
                }
            } else {
                System.out.println("Outputting corridor");
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


            //if (room.isCorridor) {
            //    if (room.isVertical) {
            //        addEntity(new Door(), Position3D.create(room.startX + 1, room.startY, 0));
            //        addEntity(new Door(), Position3D.create(room.startX + 1, room.startY + room.height - 1, 0));
           //     } else {
            //        addEntity(new Door(), Position3D.create(room.startX, room.startY + 1, 0));
            //        addEntity(new Door(), Position3D.create(room.startX + room.width - 1, room.startY + 1, 0));
            //    }
            //}
        }
    }

    private Position3D addPlayer() {
        Room room = rooms.get(0);
        int playerX = room.startX + (room.width / 2);
        int playerY = room.startY + (room.height / 2);
        Position3D pos = Position3D.create(playerX, playerY, 0);

        System.out.println("Adding player on coordinates: [" + playerX + "," + playerY + "]");
        addEntity(getPlayer(), pos);

        return pos;
    }

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
        // randomly choose one position
        Position3D stairsPos = possiblePositions.get(random.nextInt(possiblePositions.size()));
        System.out.println("Adding stairs on coordinates: [" + stairsPos.getX() + "," + stairsPos.getY() + "]");
        addEntity(new Stairs(true), stairsPos);
    }

    private void fillEmptySpace() {
        for (int x = 0; x < getWidth(); ++x) {
            for (int y = 0; y < getHeight(); y++) {
                var pos = Position3D.create(x, y, 0);
                if (!blocksTmp.containsKey(pos))
                    blocksTmp.put(pos, new Wall());
            }
        }
    }

    private void prolongRoom(Room room, int corridorX) {
        if (corridorX >= room.startX + 1 && corridorX < room.startX + room.width - 1) {
            // the corridor is connected to the room, not necessary to change size
        } else if (corridorX < room.startX + 1) {
            int newX = corridorX - 1;
            if (newX > room.cellX + 2) newX -= random.nextInt(Math.min(2, corridorX - room.cellX - 1));
            room.width += room.startX - newX;
            room.startX = newX;
            System.out.println("Room prolonged 1");
            logRoom(room);
        } else {
            int newWidth = room.width + corridorX - (room.startX + room.width) + 2;
            if (room.startX + newWidth < room.cellX + room.cellWidth - 2)
                newWidth += random.nextInt(Math.min(2, room.cellX + room.cellWidth - room.startX - newWidth));
            room.width = newWidth;
            System.out.println("Room prolonged 2");
            logRoom(room);
        }
    }

    private void logRoom(Room room) {
        System.out.println("Room.startX = " + room.startX);
        System.out.println("Room.startY = " + room.startY);
        System.out.println("Room.width = " + room.width);
        System.out.println("Room.height = " + room.height);
        System.out.println("Room.cellX = " + room.cellX);
        System.out.println("Room.cellY = " + room.cellY);
        System.out.println("Room.cellWidth = " + room.cellWidth);
        System.out.println("Room.cellHeight = " + room.cellHeight);
        System.out.println("Is corridor = " + room.isCorridor);
        System.out.println("Is vertical = " + room.isVertical);
    }

    private void logRooms(ArrayList<Room> rooms) {
        System.out.println("LOGGING ROOMS");
        for (Room room : rooms) {
            System.out.println("Room.startX = " + room.startX);
            System.out.println("Room.startY = " + room.startY);
            System.out.println("Room.width = " + room.width);
            System.out.println("Room.height = " + room.height);
            System.out.println("--------------------------------");
        }
    }

    private class Dungeon {
        public ArrayList<Room> rooms;
        public ArrayList<Room> corridors;
    }

    private class Room {
        public int startX;
        public int startY;
        public int width;
        public int height;

        public int cellX;
        public int cellY;
        public int cellWidth;
        public int cellHeight;

        public ArrayList<Room> corridors;

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

        public int getCellY() {
            return cellY;
        }

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
