package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.ManagedEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Infinite personal Night Vision; ownership marker also survives an interrupted session. */
final class NightVision implements Listener,AutoCloseable {
    private final LunarPlugin plugin;
    private final NamespacedKey marker;
    private final Set<UUID> changing=new HashSet<>();
    private final PotionEffect effect=new PotionEffect(PotionEffectType.NIGHT_VISION,PotionEffect.INFINITE_DURATION,0,false,false,true);
    private boolean closed;
    NightVision(LunarPlugin plugin) { this.plugin=plugin; marker=new NamespacedKey(plugin,"night_vision_effect"); }
    private ManagedEffect.Effect target(Player player) {
        return new ManagedEffect.Effect() {
            public boolean present() { return player.hasPotionEffect(PotionEffectType.NIGHT_VISION); }
            public boolean matches() { return effect.equals(player.getPotionEffect(PotionEffectType.NIGHT_VISION)); }
            public boolean owned() { return player.getPersistentDataContainer().has(marker,PersistentDataType.BYTE); }
            public void owned(boolean value) {
                if(value) player.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);
                else player.getPersistentDataContainer().remove(marker);
            }
            public boolean grant() {
                changing.add(player.getUniqueId());
                try { return player.addPotionEffect(effect) && matches(); }
                finally { changing.remove(player.getUniqueId()); }
            }
            public void remove() {
                changing.add(player.getUniqueId());
                try { player.removePotionEffect(PotionEffectType.NIGHT_VISION); }
                finally { changing.remove(player.getUniqueId()); }
            }
        };
    }
    void apply(Player player) {
        if(closed || !player.isOnline() || player.isDead()) return;
        ManagedEffect.apply(Boolean.TRUE.equals(plugin.preferences.get(player.getUniqueId(),"night_vision")),target(player));
    }
    void release(Player player) { ManagedEffect.release(target(player)); }
    private void later(Player player,Runnable action) {
        if(!closed && plugin.isEnabled()) plugin.getServer().getScheduler().runTask(plugin,()-> {
            if(!closed && player.isOnline() && !player.isDead()) action.run();
        });
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void potion(EntityPotionEffectEvent event) {
        if(!(event.getEntity() instanceof Player player) || event.getModifiedType()!=PotionEffectType.NIGHT_VISION
            || changing.contains(player.getUniqueId()) || closed) return;
        var state=target(player);
        var incoming=event.getNewEffect();
        if(state.owned() && state.matches()) {
            if(event.getAction()==EntityPotionEffectEvent.Action.CHANGED && !event.isOverride() && incoming!=null) {
                // A finite potion may be hidden underneath our infinite effect. Yield to it;
                // do not discard it when the player later switches the setting OFF.
                later(player,()-> {
                    var current=target(player);
                    if(!current.owned() || !current.matches()) return;
                    ManagedEffect.release(current);
                    if(current.owned()) return; // Removal was cancelled; do not replace another plugin's decision.
                    int duration=incoming.isInfinite()?PotionEffect.INFINITE_DURATION:incoming.getDuration()-1;
                    if(duration>0 || incoming.isInfinite()) {
                        changing.add(player.getUniqueId());
                        try { player.addPotionEffect(new PotionEffect(incoming.getType(),duration,incoming.getAmplifier(),incoming.isAmbient(),incoming.hasParticles(),incoming.hasIcon())); }
                        finally { changing.remove(player.getUniqueId()); }
                    }
                    apply(player);
                });
            } else state.owned(false); // Removal or replacement belongs to the external source.
        }
        later(player,()->apply(player));
    }
    @EventHandler public void respawn(PlayerRespawnEvent event) { later(event.getPlayer(),()->apply(event.getPlayer())); }
    @Override public void close() {
        closed=true;
        for(Player player:plugin.getServer().getOnlinePlayers()) release(player);
        HandlerList.unregisterAll(this);
    }
}
