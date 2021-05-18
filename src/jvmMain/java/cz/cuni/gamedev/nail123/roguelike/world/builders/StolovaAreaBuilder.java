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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;


public class StolovaAreaBuilder extends AreaBuilder {

    ObservableMap<Position3D, GameBlock> blocksTmp = new ObservableMap<>();

    int minSize = 7;

    Random random = new Random();

    public StolovaAreaBuilder(Size3D size, Size3D visibleSize) {
        super(size, visibleSize);
    }

    @NotNull
    @Override
    public AreaBuilder create() {
        return create(-1, -1);
    }

    public AreaBuilder create(int playerX, int playerY) {
        ArrayList<Room> rooms = generateRooms();
        addWalls(rooms);
        addFloor(rooms);
        Position3D playerCoords = addPlayer(rooms);
        Position3D stairsCoords = addStairs(rooms);
        setBlocks(blocksTmp);
        return this;
    }

    private ArrayList<Room> generateRooms() {
        var res = generateRoomsRecursive(0, 0, getWidth(), getHeight(), 10);
        return res;
    }

    private ArrayList<Room> generateRoomsRecursive(int startX, int startY, int width, int height, int steps) {
        var res = new ArrayList<Room>();
        if (steps == 0 || (width <= 2 * minSize && height <= 2 * minSize)) {
            // generate room inside of the current cell and return it
            Room room = new Room(startX, startY, width, height);
            res.add(room);
            return res;
        }
        ArrayList<Room> rooms1;// = new ArrayList<>();
        ArrayList<Room> rooms2;// = new ArrayList<>();
        // choose direction
        int i = random.nextInt(4);
        //if (width >= height) {
        if (width > 2 * minSize && ((height <= 2 * minSize) || (i < 3 && width >= height) || (i == 3 && width < height))) {
            // divide vertically
            // choose random number so that each part has at least minimal size
            int cut = minSize + random.nextInt(width - (2 * minSize));
            // recursively call the rest for each part (use steps-1)
            rooms1 = generateRoomsRecursive(startX, startY, cut, height, steps - 1);
            rooms2 = generateRoomsRecursive(startX + cut, startY, width - cut, height, steps - 1);
            // choose where the corridor will be (from all the coordinates which are inside of a room)
            //      v opacnem smeru, nez ve kterem se rezalo
            //      projdu mistnosti obou seznamu, pro kazdou moznou souradnici vezmu tu mistnost, ktera je bliz delici hranici, a souradnice vnitrku pridam do seznamu
            //      pak vybiram jen z tech souradnic, ktere jsou pro obe strany
            // find rooms to join (must be possible to join them with 3 squares wide corridor)
            //      if not possible, make some room longer
            //          pokud neni zadna spolecna souradnice
            // add the corridor as another room
        } else {
            // divide horizontally
            // choose random number so that each part has at least minimal size
            int cut = minSize + random.nextInt(height - (2 * minSize));
            // recursively call the rest for each part (use steps-1)
            rooms1 = generateRoomsRecursive(startX, startY, width, cut, steps - 1);
            rooms2 = generateRoomsRecursive(startX, startY + cut, width, height - cut, steps - 1);
            // choose where the corridor will be (from all the coordinates which are inside of a room)
            //      v opacnem smeru, nez ve kterem se rezalo
            //      projdu mistnosti obou seznamu, pro kazdou moznou souradnici vezmu tu mistnost, ktera je bliz delici hranici, a souradnice vnitrku pridam do seznamu
            //      pak vybiram jen z tech souradnic, ktere jsou pro obe strany
            // find rooms to join (must be possible to join them with 3 squares wide corridor)
            //      if not possible, make some room longer or move it
            //          pokud neni zadna spolecna souradnice
            //      or start with determining position of the corridor
            //          then choose coordinates for rooms accordingly
            // add the corridor as another room
        }
        // add rooms from both branches into the result array
        for (Room room : rooms1) res.add(room);
        for (Room room : rooms2) res.add(room);
        // return result
        return res;
    }

    private void addWalls(ArrayList<Room> rooms) {
        for (Room room : rooms) {
            // up and bottom
            for (int i = 1; i < room.width - 1; ++i) {
                blocksTmp.put(Position3D.create(room.startX + i, room.startY + 1, 0), new Wall());
                blocksTmp.put(Position3D.create(room.startX + i, room.startY + room.height - 2, 0), new Wall());
            }
            // left and right
            for (int i = 1; i < room.height - 1; ++i) {
                blocksTmp.put(Position3D.create(room.startX + 1, room.startY + i, 0), new Wall());
                blocksTmp.put(Position3D.create(room.startX + room.width - 2, room.startY + i, 0), new Wall());
            }
        }

    }

    private void addFloor(ArrayList<Room> rooms) {
        for (Room room : rooms) {
            for (int x = 2; x < room.width - 2; ++x) {
                for (int y = 2; y < room.height - 2; ++y) {
                    Position3D pos = Position3D.create(room.startX + x, room.startY + y, 0);
                    blocksTmp.put(pos, new Floor());
                }
            }
        }
    }

    private Position3D addPlayer(ArrayList<Room> rooms) {
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

    private Position3D addStairs(ArrayList<Room> rooms) {
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

    private class Room {
        public int startX;
        public int startY;
        public int width;
        public int height;

        public Room(int startX, int startY, int width, int height) {
            this.startX = startX;
            this.startY = startY;
            this.width = width;
            this.height = height;
        }

    }
}
