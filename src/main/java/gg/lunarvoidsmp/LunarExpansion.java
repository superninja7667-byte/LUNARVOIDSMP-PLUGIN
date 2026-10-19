package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.PlaceholderValues;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Internal expansion: registered by LunarVoidSMP, never installed through eCloud. */
final class LunarExpansion extends PlaceholderExpansion {
    private static final Set<String> LINES=Set.of("money","discord_link","online","rank_tag","ping","server_tps");
    private final String version;
    private final PlaceholderValues values;
    private final BiFunction<UUID,String,Boolean> preference;
    private final Function<String,String> template;
    private final BiFunction<OfflinePlayer,String,String> parser;
    private final ThreadLocal<Boolean> rendering=ThreadLocal.withInitial(()->false);
    LunarExpansion(LunarPlugin plugin) {
        this(plugin.getPluginMeta().getVersion(),plugin.placeholderValues,plugin.preferences::get,
            key->plugin.getConfig().getString("tab.lines."+key,""),PlaceholderAPI::setPlaceholders);
    }
    LunarExpansion(String version,PlaceholderValues values,BiFunction<UUID,String,Boolean> preference,
                   Function<String,String> template,BiFunction<OfflinePlayer,String,String> parser) {
        this.version=version; this.values=values; this.preference=preference; this.template=template; this.parser=parser;
    }
    @Override public String getIdentifier() { return "lunar"; }
    @Override public String getAuthor() { return "LUNARVOIDSMP"; }
    @Override public String getVersion() { return version; }
    @Override public boolean persist() { return true; }
    @Override public List<String> getPlaceholders() {
        var names=new ArrayList<>(PlaceholderValues.KEYS);
        Preferences.KEYS.forEach(key->names.add("setting_"+key));
        LINES.stream().sorted().forEach(key->names.add("line_"+key));
        return names.stream().map(key->"%lunar_"+key+"%").toList();
    }
    @Override public String onRequest(OfflinePlayer player,String params) {
        if(params==null) return null;
        String key=params.toLowerCase(Locale.ROOT);
        UUID id=player==null?null:player.getUniqueId();
        if(key.startsWith("setting_")) {
            String setting=key.substring(8);
            if(!Preferences.KEYS.contains(setting)) return null;
            if(!values.contains(id)) return ""; // Never invent an offline player's saved choice.
            Boolean value=preference.apply(id,setting); return value==null?"unset":value.toString();
        }
        if(key.startsWith("line_")) {
            String line=key.substring(5);
            if(!LINES.contains(line)) return null;
            if(!values.contains(id) || Boolean.FALSE.equals(preference.apply(id,line)) || rendering.get()) return "";
            rendering.set(true);
            try { return parser.apply(player,template.apply(line)); }
            finally { rendering.remove(); }
        }
        return values.resolve(id,key);
    }
}
