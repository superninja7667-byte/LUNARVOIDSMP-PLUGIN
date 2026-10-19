package gg.lunarvoidsmp;

import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.EntityType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Snapshot explosion ownership on the server thread; packet callbacks read immutable data. */
final class ExplosionViews implements Listener {
    private record Blast(UUID world,double x,double y,double z,String key,long until,Set<UUID> viewers) {}
    private final LunarPlugin plugin;
    private final Map<UUID,Blast> blasts=new ConcurrentHashMap<>();
    ExplosionViews(LunarPlugin plugin) { this.plugin=plugin; }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void explode(EntityExplodeEvent event) {
        String key=event.getEntityType()==EntityType.TNT?"tnt_particles":event.getEntityType()==EntityType.END_CRYSTAL?"end_crystal_particles":null;
        if(key==null) return;
        long now=System.currentTimeMillis(); blasts.values().removeIf(blast->blast.until()<now);
        var location=event.getLocation(); Set<UUID> viewers=new HashSet<>(); for(var player:location.getWorld().getPlayers()) viewers.add(player.getUniqueId());
        blasts.put(event.getEntity().getUniqueId(),new Blast(location.getWorld().getUID(),location.getX(),location.getY(),location.getZ(),key,now+1500,Set.copyOf(viewers)));
    }
    boolean hide(UUID viewer,Vector3d at) {
        long now=System.currentTimeMillis();
        return blasts.values().stream().anyMatch(blast->blast.until()>=now && blast.viewers().contains(viewer) && Math.abs(blast.x()-at.x)<2 && Math.abs(blast.y()-at.y)<2 && Math.abs(blast.z()-at.z)<2 && !plugin.preferences.enabled(viewer,blast.key()));
    }
}
