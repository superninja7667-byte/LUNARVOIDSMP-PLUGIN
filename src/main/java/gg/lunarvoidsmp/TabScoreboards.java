package gg.lunarvoidsmp;

import me.neznamy.tab.api.TabAPI;
import org.bukkit.entity.Player;

/** TAB owns the scoreboard contents. This adapter changes sidebar visibility only. */
final class TabScoreboards {
    boolean available(Player player) {
        return TabAPI.getInstance().getScoreboardManager()!=null && TabAPI.getInstance().getPlayer(player.getUniqueId())!=null;
    }
    boolean visible(Player player) {
        var api=TabAPI.getInstance();
        if(!available(player)) throw new IllegalArgumentException("TAB's scoreboard is disabled or your TAB data is still loading.");
        return api.getScoreboardManager().hasScoreboardVisible(api.getPlayer(player.getUniqueId()));
    }
    void apply(Player player,boolean visible) {
        var api=TabAPI.getInstance();
        var manager=api.getScoreboardManager();
        var tabPlayer=api.getPlayer(player.getUniqueId());
        if(manager!=null && tabPlayer!=null) manager.setScoreboardVisible(tabPlayer,visible,false);
    }
}
