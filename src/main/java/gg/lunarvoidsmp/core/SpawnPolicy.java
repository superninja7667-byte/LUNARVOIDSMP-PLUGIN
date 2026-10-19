package gg.lunarvoidsmp.core;

import java.util.Collection;

/** A player's current chunk and its eight neighbors, without loading world chunks. */
public final class SpawnPolicy {
    private SpawnPolicy() {}
    public record Area(String world,int chunkX,int chunkZ,boolean mobsOff,boolean phantomsOff) {}

    public static int chunk(int blockCoordinate) { return Math.floorDiv(blockCoordinate,16); }

    public static boolean blocks(String world,int blockX,int blockZ,boolean phantom,boolean natural,Collection<Area> areas) {
        if(!natural) return false;
        int x=chunk(blockX),z=chunk(blockZ);
        for(Area area:areas) {
            if(area.world().equals(world) && Math.abs((long)x-area.chunkX())<=1 && Math.abs((long)z-area.chunkZ())<=1
                && (area.mobsOff() || (phantom && area.phantomsOff()))) return true;
        }
        return false;
    }
}
