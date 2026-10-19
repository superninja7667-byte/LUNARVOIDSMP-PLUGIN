package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.NumberFormatter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import java.math.BigDecimal;

final class FormatterExpansion extends PlaceholderExpansion {
    @Override public String getIdentifier() { return "formatter"; }
    @Override public String getAuthor() { return "LUNARVOIDSMP"; }
    @Override public String getVersion() { return "0.6.1-test"; }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player,String params) {
        if(params==null || !params.startsWith("number_shorten_")) return null;
        String raw=params.substring("number_shorten_".length());
        if(raw.contains("{formatter_") || raw.length()>128) return "";
        raw=PlaceholderAPI.setBracketPlaceholders(player,raw);
        try { return NumberFormatter.shorten(new BigDecimal(raw.replace(",",""))); }
        catch(NumberFormatException e) { return ""; }
    }
}
