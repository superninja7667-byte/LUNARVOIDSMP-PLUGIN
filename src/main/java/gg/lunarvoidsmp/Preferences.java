package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Store;
import org.bukkit.configuration.ConfigurationSection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class Preferences {
    private final Store store;
    private final Map<String,Boolean> defaults=new HashMap<>();
    private final Map<UUID,Map<String,Boolean>> cache=new ConcurrentHashMap<>();
    static final List<String> KEYS=List.of("notifications","global_chat","death_messages","money_tag","money","discord_link","online","rank_tag","ping","tpa_request","tnt_particles","end_crystal_particles","auto_tpa","phantom_spawning","mob_spawning","server_tps","item_price","scoreboard","night_vision","instant_rtp","tpa_receiving");
    Preferences(Store store,ConfigurationSection config) {
        this.store=store; defaults.put("auto_tpa",false); defaults.put("rank_tag",false); defaults.put("item_price",false); defaults.put("night_vision",false); defaults.put("instant_rtp",false);
        if(config!=null) for(String key:KEYS) if(config.isBoolean(key)) defaults.put(key,config.getBoolean(key));
        defaults.put("rank_tag",false); defaults.put("item_price",false); defaults.put("night_vision",false); defaults.put("instant_rtp",false);
    }
    void load(UUID owner) throws SQLException {
        Map<String,Boolean> values=new HashMap<>(defaults);
        for(String key:KEYS) { Boolean v=store.setting(owner.toString(),key); if(v!=null) values.put(key,v); }
        cache.put(owner,Map.copyOf(values));
    }
    Boolean get(UUID owner,String key) { return cache.getOrDefault(owner,defaults).get(key); }
    boolean enabled(UUID owner,String key) { return !Boolean.FALSE.equals(get(owner,key)); }
    void set(UUID owner,String key,boolean value) throws SQLException {
        store.setting(owner.toString(),key,value);
        Map<String,Boolean> values=new HashMap<>(cache.getOrDefault(owner,defaults)); values.put(key,value); cache.put(owner,Map.copyOf(values));
    }
    void unload(UUID id) { cache.remove(id); }
}
