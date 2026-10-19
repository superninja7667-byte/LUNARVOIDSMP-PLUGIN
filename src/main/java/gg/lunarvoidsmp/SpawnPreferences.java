package gg.lunarvoidsmp;

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import gg.lunarvoidsmp.core.SpawnPolicy;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import java.util.ArrayList;
import java.util.List;

final class SpawnPreferences implements Listener {
    private final LunarPlugin plugin;
    SpawnPreferences(LunarPlugin plugin) { this.plugin=plugin; }

    private boolean blocked(Location location,boolean phantom) {
        List<SpawnPolicy.Area> areas=new ArrayList<>();
        // Only online players in this world; no chunks are loaded or permanently marked.
        for(var player:location.getWorld().getPlayers()) {
            boolean mobsOff=Boolean.FALSE.equals(plugin.preferences.get(player.getUniqueId(),"mob_spawning"));
            boolean phantomsOff=Boolean.FALSE.equals(plugin.preferences.get(player.getUniqueId(),"phantom_spawning"));
            if(!mobsOff && !phantomsOff) continue;
            Location at=player.getLocation();
            areas.add(new SpawnPolicy.Area(location.getWorld().getUID().toString(),SpawnPolicy.chunk(at.getBlockX()),SpawnPolicy.chunk(at.getBlockZ()),mobsOff,phantomsOff));
        }
        return SpawnPolicy.blocks(location.getWorld().getUID().toString(),location.getBlockX(),location.getBlockZ(),phantom,true,areas);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void spawn(CreatureSpawnEvent event) {
        if((event.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.NATURAL && event.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.CHUNK_GEN)
            || !(event.getEntity() instanceof Mob)) return;
        if(blocked(event.getLocation(),event.getEntityType()==org.bukkit.entity.EntityType.PHANTOM)) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void phantom(PhantomPreSpawnEvent event) {
        if(event.getReason()==CreatureSpawnEvent.SpawnReason.NATURAL && blocked(event.getSpawnLocation(),true)) event.setCancelled(true);
    }
}
