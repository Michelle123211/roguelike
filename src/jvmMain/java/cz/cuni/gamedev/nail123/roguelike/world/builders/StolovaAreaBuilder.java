package cz.cuni.gamedev.nail123.roguelike.world.builders;

import cz.cuni.gamedev.nail123.roguelike.GameConfig;
import cz.cuni.gamedev.nail123.roguelike.blocks.Floor;
import cz.cuni.gamedev.nail123.roguelike.blocks.GameBlock;
import cz.cuni.gamedev.nail123.roguelike.blocks.Wall;
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Stairs;
import cz.cuni.gamedev.nail123.roguelike.extensions.PositionExtensionsKt;
import cz.cuni.gamedev.nail123.roguelike.world.builders.AreaBuilder;
import cz.cuni.gamedev.nail123.utils.collections.ObservableMap;
import org.hexworks.zircon.api.data.Position3D;
import org.hexworks.zircon.api.data.Size3D;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;


public class StolovaAreaBuilder extends AreaBuilder {

    ObservableMap<Position3D, GameBlock> blocksTmp = new ObservableMap<>();

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
        return create(-1, -1);
    }

    public AreaBuilder create(int playerX, int playerY) {
        random.setSeed(System.currentTimeMillis());
        corridors = new ArrayList<>();
        rooms = generateRooms();
        //addWalls();
        addFloor();
        Position3D playerCoords = addPlayer();
        Position3D stairsCoords = addStairs();
        setBlocks(blocksTmp);
        return this;
    }

    private ArrayList<Room> generateRooms() {
        var res = generateRoomsRecursive(0, 0, getWidth(), getHeight(), 5);
        return res;
    }

    private ArrayList<Room> generateRoomsRecursive(int startX, int startY, int width, int height, int steps) {
        System.out.println("************************************");
        System.out.println("Steps = " + steps);
        var res = new ArrayList<Room>();
        if (steps == 0 || (width <= 2 * minSize && height <= 2 * minSize)) {
            // generate room inside of the current cell and return it
            Room room = new Room(startX, startY, width, height);
            System.out.println("Making room smaller");
            System.out.println("Width = " + width);
            System.out.println("Height = " + height);
            System.out.println("StartX = " + startX);
            System.out.println("StartY = " + startY);
            int newWidth = width;
            if (newWidth > minSize) newWidth = minSize + random.nextInt(width - minSize);
            int newHeight = height;
            if (newHeight > minSize) newHeight = minSize + random.nextInt(height - minSize);
            int newX = startX;
            if (newWidth < width) newX += random.nextInt(width - newWidth);
            int newY = startY;
            if (newHeight < height) newY += random.nextInt(height - newHeight);
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
        if (width > 2 * minSize && ((height <= 2 * minSize) || (i < 3 && width >= height) || (i == 3 && width < height))) {
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
        // choose random number so that each part has at least minimal size
        int cut = minSize + random.nextInt(height - (2 * minSize));
        System.out.println("startX = " + startX);
        System.out.println("startY = " + startY);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("Cut = " + cut);
        System.out.println("wasReversed = " + isHorizontal);
        System.out.println("=============================");
        // recursively call the rest for each part (use steps-1)
        rooms1 = generateRoomsRecursive(startX, startY, width, cut, steps - 1);
        rooms2 = generateRoomsRecursive(startX, startY + cut, width, height - cut, steps - 1);

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
        // go through both lists at the same time, remember somewhere all possible coordinates for corridor
        ArrayList<Integer> possibleCoordinates = new ArrayList<>();
        logRooms(filteredRooms1);
        logRooms(filteredRooms2);
        System.out.println("Determining corridor...");
        for (int coord = 0; coord < height; ++coord) {
            boolean isRoom1 = false;
            for (Room room : filteredRooms1) {
                if (startX + coord >= room.cellX + 2 && startX + coord < room.cellX + room.cellWidth - 2) {
                    isRoom1 = true;
                    break;
                }
            }
            boolean isRoom2 = false;
            for (Room room : filteredRooms2) {
                if (startX + coord >= room.cellX + 2 && startX + coord < room.cellX + room.cellWidth - 2) {
                    isRoom2 = true;
                    break;
                }
            }
            if (isRoom1 && isRoom2) possibleCoordinates.add(startX + coord);
        }
        // randomly choose a coordinate
        int corridorX = possibleCoordinates.get(random.nextInt(possibleCoordinates.size()));
        // rooms, which will be joined have to be enlarged so that the corridor goes into them
        Room corridor = new Room(corridorX - 1, 0, 3, 0);
        for (Room room : filteredRooms1) {
            if (corridorX >= room.cellX + 2 && corridorX < room.cellX + room.cellWidth - 2) {
                logRoom(room);
                if (corridorX >= room.startX + 1 && corridorX < room.startX + room.width - 1) {
                    // the corridor is connected to the room
                    System.out.println("Not necessary to change size.");
                } else if (corridorX < room.startX + 1) {
                    room.width += room.startX - corridorX + 1;
                    room.startX = corridorX - 1; // TODO: Change randomly
                    System.out.println("Room's size changed 1.");
                    logRoom(room);
                } else {
                    room.width += corridorX - (room.startX + room.width) + 2; // TODO: Change randomly
                    System.out.println("Room's size changed 2.");
                    logRoom(room);
                }
                corridor.startY = room.startY + room.height - 1;
                break;
            }
        }
        for (Room room : filteredRooms2) {
            if (corridorX >= room.cellX + 2 && corridorX < room.cellX + room.cellWidth - 2) {
                logRoom(room);
                if (corridorX >= room.startX + 1 && corridorX < room.startX + room.width - 1) {
                    // the corridor is connected to the room
                    System.out.println("Not necessary to change size.");
                } else if (corridorX < room.startX + 1) {
                    room.width += room.startX - corridorX + 1;
                    room.startX = corridorX - 1; // TODO: Change randomly
                    System.out.println("Room's size changed 1.");
                    logRoom(room);
                } else {
                    room.width += corridorX - (room.startX + room.width) + 2; // TODO: Change randomly
                    System.out.println("Room's size changed 2.");
                    logRoom(room);
                }
                corridor.height = room.startY - corridor.startY + 1;
                break;
            }
        }
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
                blocksTmp.put(Position3D.create(room.startX + i, room.startY + 1, 0), new Wall());
                blocksTmp.put(Position3D.create(room.startX + i, room.startY + room.height - 2, 0), new Wall());
            }
            // left and right
            for (int i = 0; i < room.height; ++i) {
                blocksTmp.put(Position3D.create(room.startX + 1, room.startY + i, 0), new Wall());
                blocksTmp.put(Position3D.create(room.startX + room.width - 2, room.startY + i, 0), new Wall());
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

    private Position3D addPlayer() {
        Room room = rooms.get(0);
        int playerX = room.startX + 2 + random.nextInt(room.width - 3);
        int playerY = room.startY + 2 + random.nextInt(room.height - 3);
        Position3D pos = Position3D.create(playerX, playerY, 0);

        addAtEmptyPosition(
                getPlayer(),
                pos,
                Size3D.create(getWidth() / 2 - 2, getHeight() / 2 - 2, 1)
        );

        return pos;
    }

    private Position3D addStairs() {
        Room room = rooms.get(rooms.size() - 1);
        int stairsX = room.startX + 2 + random.nextInt(room.width - 3);
        int stairsY = room.startY + 2 + random.nextInt(room.height - 3);
        Position3D pos = Position3D.create(stairsX, stairsY, 0);

        addAtEmptyPosition(
                new Stairs(),
                pos,
                Size3D.create(getWidth() / 2 - 2, getHeight() / 2 - 2, 1)
        );

        //var floodFill = Pathfinding.INSTANCE.floodFill(areaBuilder.getPlayer().getPosition(), areaBuilder, Pathfinding.INSTANCE.getEightDirectional(), Pathfinding.INSTANCE.getDoorOpening());
        //var staircasePosition = floodFill.keys.random();

        return pos;
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
            res.isCorridor = this.isCorridor;
            res.isVertical = !this.isVertical;
            return res;
        }

    }
}
