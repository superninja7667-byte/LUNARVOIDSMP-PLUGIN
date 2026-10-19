package gg.lunarvoidsmp;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Bounded asynchronous chunk searches, one candidate at a time per player. */
final class RandomSpawns implements Listener {
    private final LunarPlugin plugin;
    private final Map<UUID,UUID> searches=new HashMap<>();
    private final Set<UUID> respawns=new HashSet<>();
    private static final Set<Material> HAZARDS=Set.of(Material.LAVA,Material.WATER,Material.MAGMA_BLOCK,Material.CACTUS,Material.FIRE,Material.SOUL_FIRE,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.POWDER_SNOW,Material.SWEET_BERRY_BUSH,Material.WITHER_ROSE);
    RandomSpawns(LunarPlugin plugin) { this.plugin=plugin; }
    void rtp(Player player,boolean immediate) {
        UUID token=UUID.randomUUID(); searches.put(player.getUniqueId(),token); Location origin=player.getLocation();
        if(!immediate) plugin.notice(player,"Teleporting in 5 seconds. Stay still.");
        Bukkit.getScheduler().runTaskLater(plugin,()-> {
            if(!player.isOnline() || !token.equals(searches.get(player.getUniqueId()))) return;
            if(!immediate && (!player.getWorld().equals(origin.getWorld()) || player.getLocation().distanceSquared(origin)>0.25)) { searches.remove(player.getUniqueId()); plugin.notice(player,"Random teleport cancelled because you moved."); return; }
            candidate(player,origin.getWorld(),token,0);
        },immediate?1L:100L);
    }
    private void candidate(Player player,World world,UUID token,int attempt) {
        if(!player.isOnline() || !world.equals(player.getWorld()) || !token.equals(searches.get(player.getUniqueId()))) return;
        if(attempt>=24) { searches.remove(player.getUniqueId()); plugin.notice(player,"No safe random destination found. Please try again."); return; }
        var random=ThreadLocalRandom.current(); Location spawn=world.getSpawnLocation();
        var border=world.getWorldBorder(); Location center=border.getCenter(); double half=border.getSize()/2-2;
        double minX=Math.max(spawn.getX()-20_000,center.getX()-half),maxX=Math.min(spawn.getX()+20_000,center.getX()+half);
        double minZ=Math.max(spawn.getZ()-20_000,center.getZ()-half),maxZ=Math.min(spawn.getZ()+20_000,center.getZ()+half);
        if(minX>=maxX || minZ>=maxZ) { searches.remove(player.getUniqueId()); plugin.notice(player,"The world border has no random destination within 20,000 blocks of spawn."); return; }
        int x=(int)Math.floor(random.nextDouble(minX,maxX)),z=(int)Math.floor(random.nextDouble(minZ,maxZ));
        if(Math.hypot(x+0.5-spawn.getX(),z+0.5-spawn.getZ())>20_000) { candidate(player,world,token,attempt+1); return; }
        if(!world.getWorldBorder().isInside(new Location(world,x+0.5,spawn.getY(),z+0.5))) { candidate(player,world,token,attempt+1); return; }
        world.getChunkAtAsync(x>>4,z>>4,true).whenComplete((chunk,error)-> {
            if(!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin,()-> {
                if(!player.isOnline() || !world.equals(player.getWorld()) || !token.equals(searches.get(player.getUniqueId()))) return;
                if(error==null) {
                    int top=world.getEnvironment()==World.Environment.NETHER?Math.min(119,world.getMaxHeight()-3):world.getHighestBlockYAt(x,z);
                    for(int y=top;y>world.getMinHeight();y--) {
                        Block ground=world.getBlockAt(x,y,z),feet=ground.getRelative(0,1,0),head=ground.getRelative(0,2,0);
                        if(ground.getType().isSolid() && !ground.getType().name().endsWith("LEAVES") && !HAZARDS.contains(ground.getType()) && feet.isEmpty() && head.isEmpty()) {
                            Location destination=new Location(world,x+0.5,y+1,z+0.5,player.getYaw(),player.getPitch());
                            searches.remove(player.getUniqueId());
                            player.teleportAsync(destination,PlayerTeleportEvent.TeleportCause.PLUGIN); return;
                        }
                        if(world.getEnvironment()!=World.Environment.NETHER) break;
                    }
                }
                candidate(player,world,token,attempt+1);
            });
        });
    }
    @EventHandler public void join(PlayerJoinEvent event) { if(!event.getPlayer().hasPlayedBefore()) rtp(event.getPlayer(),true); }
    @EventHandler(priority=EventPriority.MONITOR) public void respawn(PlayerRespawnEvent event) {
        if(event.getRespawnReason()==PlayerRespawnEvent.RespawnReason.DEATH && !event.isBedSpawn() && !event.isAnchorSpawn()) respawns.add(event.getPlayer().getUniqueId());
    }
    @EventHandler public void respawned(PlayerPostRespawnEvent event) { if(respawns.remove(event.getPlayer().getUniqueId())) rtp(event.getPlayer(),true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void command(PlayerCommandPreprocessEvent event) {
        if(!event.getMessage().strip().equalsIgnoreCase("/rtp")) return;
        event.setCancelled(true); rtp(event.getPlayer(),plugin.preferences.enabled(event.getPlayer().getUniqueId(),"instant_rtp"));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleported(PlayerTeleportEvent event) { searches.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void quit(PlayerQuitEvent event) { searches.remove(event.getPlayer().getUniqueId()); respawns.remove(event.getPlayer().getUniqueId()); }
}
