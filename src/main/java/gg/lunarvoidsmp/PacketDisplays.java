package gg.lunarvoidsmp;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.util.WeightedList;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import java.util.*;

/** Recipient-only packet copies. No Bukkit inventory access on network threads. */
final class PacketDisplays extends PacketListenerAbstract implements PriceDisplay {
    private final LunarPlugin plugin;
    private volatile boolean healthy=true;
    PacketDisplays(LunarPlugin plugin) {
        super(PacketListenerPriority.HIGH); this.plugin=plugin;
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }
    @Override public boolean healthy() { return healthy; }
    @Override public void onPacketSend(PacketSendEvent event) {
        if(!healthy || event.isCancelled() || event.getUser()==null) return;
        UUID id=event.getUser().getUUID(); if(id==null) return;
        try {
            if(event.getPacketType()==PacketType.Play.Server.EXPLOSION && plugin.explosions.hide(id,new WrapperPlayServerExplosion(event).getPosition())) {
                var packet=new WrapperPlayServerExplosion(event);
                // Keep sound and knockback. Remove the explosion emitter and block particles only.
                packet.setParticle(new Particle<>(ParticleTypes.BLOCK_MARKER,new com.github.retrooper.packetevents.protocol.particle.data.ParticleBlockStateData(com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState.getByGlobalId(0))));
                packet.setBlockParticles(new WeightedList<>());
            } else if(event.getPacketType()==PacketType.Play.Server.PARTICLE) {
                var packet=new WrapperPlayServerParticle(event); var type=packet.getParticle().getType();
                if((type==ParticleTypes.EXPLOSION || type==ParticleTypes.EXPLOSION_EMITTER) && plugin.explosions.hide(id,packet.getPosition())) event.setCancelled(true);
            }
        } catch(RuntimeException e) { healthy=false; plugin.getLogger().warning("Packet display stopped after an adapter error: "+e.getClass().getSimpleName()); }
    }
    @Override public void forget(UUID id) {}
    void disconnect(UUID id) {}
    @Override public void close() { healthy=false; PacketEvents.getAPI().getEventManager().unregisterListener(this); }
}
