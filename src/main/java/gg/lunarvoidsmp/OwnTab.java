package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.*;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/** Optional replacement. Never shares teams or objectives with another sidebar owner. */
final class OwnTab implements AutoCloseable {
    private record View(Scoreboard previous,Scoreboard board,Objective sidebar,Objective money,Objective ping) {}
    private final LunarPlugin plugin;
    private final Map<UUID,View> views=new HashMap<>();
    private final Map<UUID,String> prefixes=new HashMap<>(),suffixes=new HashMap<>(),groups=new HashMap<>();
    private final Map<UUID,BigDecimal> balances=new HashMap<>();
    private final YamlConfiguration animations;
    private long ticks;
    private static final List<String> ORDER=List.of("smp_owner","smp_manager","admin","mod","trusted","vip1","member","default");
    private static final LegacyComponentSerializer LEGACY=LegacyComponentSerializer.builder().character('&').hexColors().build();
    OwnTab(LunarPlugin plugin) {
        this.plugin=plugin;
        animations=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource("tab-animations.yml")),java.nio.charset.StandardCharsets.UTF_8));
        Bukkit.getScheduler().runTaskTimer(plugin,this::update,1,2);
    }
    boolean active() { return plugin.getConfig().getBoolean("own-tab.enabled",false); }
    private Component component(String input) { return LEGACY.deserialize(input); }
    private String frame(String name) {
        var values=animations.getStringList(name+".texts"); if(values.isEmpty()) return "";
        long interval=Math.max(100,animations.getLong(name+".change-interval",100));
        return values.get((int)((ticks*50/interval)%values.size()));
    }
    private String replace(String raw,Player player) {
        return raw.replace("%player%",player.getName()).replace("%server_online%",Integer.toString(Bukkit.getOnlinePlayers().size())).replace("%server_max_players%",Integer.toString(Bukkit.getMaxPlayers()));
    }
    private void sample() {
        var chatReg=Bukkit.getServicesManager().getRegistration(Chat.class); Chat chat=chatReg==null?null:chatReg.getProvider();
        var permissionReg=Bukkit.getServicesManager().getRegistration(Permission.class); Permission perms=permissionReg==null?null:permissionReg.getProvider();
        for(Player player:Bukkit.getOnlinePlayers()) {
            UUID id=player.getUniqueId();
            prefixes.put(id,chat==null?"":Objects.toString(chat.getPlayerPrefix(player),"")); suffixes.put(id,chat==null?"":Objects.toString(chat.getPlayerSuffix(player),""));
            groups.put(id,perms==null?"default":perms.getPrimaryGroup(player));
            try { balances.put(id,plugin.balanceValue(player)); } catch(RuntimeException e) { balances.remove(id); }
        }
    }
    private void update() {
        if(!active()) { if(!views.isEmpty()) close(); return; }
        // TAB must be removed/disabled before the replacement is activated.
        if(Bukkit.getPluginManager().isPluginEnabled("TAB")) { if(!views.isEmpty()) close(); return; }
        ticks+=2; if(ticks%20==2) sample();
        List<Player> players=new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.<Player>comparingInt(p->{ int index=ORDER.indexOf(groups.getOrDefault(p.getUniqueId(),"default")); return index<0?ORDER.size():index; }).thenComparing(Player::getName));
        for(int i=0;i<players.size();i++) players.get(i).setPlayerListOrder(i);
        for(Player viewer:players) {
            UUID id=viewer.getUniqueId();
            View view=views.computeIfAbsent(id,key->{
                Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
                Objective sidebar=board.registerNewObjective("lunar_side",Criteria.DUMMY,Component.empty()); sidebar.numberFormat(NumberFormat.blank());
                Objective money=board.registerNewObjective("lunar_money",Criteria.DUMMY,Component.empty());
                Objective ping=board.registerNewObjective("lunar_ping",Criteria.DUMMY,Component.text("TAB")); ping.setDisplaySlot(DisplaySlot.PLAYER_LIST);
                View created=new View(viewer.getScoreboard(),board,sidebar,money,ping); viewer.setScoreboard(board); return created;
            });
            if(viewer.getScoreboard()!=view.board()) continue;
            viewer.sendPlayerListHeaderAndFooter(component(frame("lunarServer")),component(frame("lunarDiscord")));
            viewer.playerListName(component(prefixes.getOrDefault(id,"")+viewer.getName()+suffixes.getOrDefault(id,"")));
            boolean visible=plugin.preferences.enabled(id,"scoreboard");
            if(visible) view.sidebar().setDisplaySlot(DisplaySlot.SIDEBAR); else if(view.sidebar().getDisplaySlot()!=null) view.sidebar().setDisplaySlot(null);
            view.sidebar().displayName(component(frame("lunarServer")));
            for(String entry:new HashSet<>(view.board().getEntries())) if(entry.startsWith("§")) view.board().resetScores(entry);
            List<String> lines=new ArrayList<>(); lines.add(frame("lunarDivider")); lines.add("&f&lPlayer: &f"+viewer.getName());
            if(plugin.preferences.enabled(id,"rank_tag")) lines.add("&f&lRank: &f&l"+prefixes.getOrDefault(id,""));
            if(plugin.preferences.enabled(id,"online")) lines.add(replace(frame("lunarOnline"),viewer));
            if(plugin.preferences.enabled(id,"money")) lines.add("&a&lMoney: &a&l$&f&l"+formatBalance(id));
            if(plugin.preferences.enabled(id,"ping")) lines.add("&1&lPing: &f&l"+viewer.getPing()+" ms");
            if(plugin.preferences.enabled(id,"server_tps")) lines.add("&4&lSERVER TPS: &f&l"+String.format(Locale.US,"%.1f",Bukkit.getTPS()[0]));
            lines.add(frame("lunarDivider")); if(plugin.preferences.enabled(id,"discord_link")) lines.add(frame("lunarDiscord"));
            for(int i=0;i<lines.size();i++) { var score=view.sidebar().getScore("§"+Integer.toHexString(i)); score.setScore(lines.size()-i); score.customName(component(lines.get(i))); }
            if(plugin.preferences.enabled(id,"money_tag")) view.money().setDisplaySlot(DisplaySlot.BELOW_NAME); else if(view.money().getDisplaySlot()!=null) view.money().setDisplaySlot(null);
            Set<String> live=new HashSet<>();
            for(Player target:players) {
                String key=target.getUniqueId().toString().replace("-","").substring(0,16); live.add(key);
                Team team=view.board().getTeam(key); if(team==null) team=view.board().registerNewTeam(key);
                if(!viewer.canSee(target)) { team.removeEntry(target.getName()); continue; }
                team.addEntry(target.getName()); team.setOption(Team.Option.COLLISION_RULE,Team.OptionStatus.ALWAYS); team.setCanSeeFriendlyInvisibles(false);
                team.prefix(component(plugin.preferences.enabled(id,"rank_tag")?prefixes.getOrDefault(target.getUniqueId(),""):""));
                team.suffix(component(plugin.preferences.enabled(id,"rank_tag")?suffixes.getOrDefault(target.getUniqueId(),""):""));
                BigDecimal value=balances.get(target.getUniqueId()); var money=view.money().getScore(target.getName());
                money.setScore(value==null?0:value.min(BigDecimal.valueOf(Integer.MAX_VALUE)).max(BigDecimal.ZERO).intValue());
                money.numberFormat(NumberFormat.fixed(component("&a$&f"+formatBalance(target.getUniqueId()))));
                view.ping().getScore(target.getName()).setScore(Math.max(0,target.getPing()));
            }
            for(Team team:new HashSet<>(view.board().getTeams())) if(!live.contains(team.getName())) { for(String entry:team.getEntries()) view.board().resetScores(entry); team.unregister(); }
        }
        views.keySet().removeIf(id->Bukkit.getPlayer(id)==null); prefixes.keySet().retainAll(views.keySet());suffixes.keySet().retainAll(views.keySet());groups.keySet().retainAll(views.keySet());balances.keySet().retainAll(views.keySet());
    }
    private String formatBalance(UUID id) { BigDecimal balance=balances.get(id); return balance==null?"":NumberFormatter.shorten(balance); }
    @Override public void close() {
        for(var entry:views.entrySet()) { Player player=Bukkit.getPlayer(entry.getKey()); if(player!=null && player.getScoreboard()==entry.getValue().board()) { player.setScoreboard(entry.getValue().previous()); player.sendPlayerListHeaderAndFooter(Component.empty(),Component.empty()); player.playerListName(null); } }
        views.clear();
    }
}
