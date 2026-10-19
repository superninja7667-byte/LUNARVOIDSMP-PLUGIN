package gg.lunarvoidsmp;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

/** Own maintenance commands; configuration and UUID whitelist survive restarts. */
final class MaintenanceMode implements Listener {
    private final LunarPlugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Set<String> whitelist;
    private boolean enabled;
    MaintenanceMode(LunarPlugin plugin) {
        this.plugin=plugin; file=new File(plugin.getDataFolder(),"maintenance.yml");
        if(!file.exists()) plugin.saveResource("maintenance.yml",false);
        config=YamlConfiguration.loadConfiguration(file); enabled=config.getBoolean("maintenance-enabled");
        whitelist=new HashSet<>(config.getStringList("whitelist"));
    }
    private Component message(String text) { return MiniMessage.miniMessage().deserialize(text.replace("%REASON%",config.getString("active-reason","")).replace("#fffff>","#ffffff>")); }
    private Component kickMessage() { return message(config.getString("kick-message","<red>LUNARVOIDSMP is currently under maintenance.<newline><gray>Please come back soon.")); }
    private boolean allowed(Player player) { return player.hasPermission("lunar.admin") || whitelist.contains(player.getUniqueId().toString()); }
    private void save() throws java.io.IOException { config.set("maintenance-enabled",enabled); config.set("whitelist",new ArrayList<>(whitelist)); config.save(file); }
    void command(CommandSender sender,String[] args) throws Exception {
        if(!sender.hasPermission("lunar.admin")) { sender.sendMessage("Administrator permission required."); return; }
        if(args.length==0 || args[0].equalsIgnoreCase("status")) { sender.sendMessage("Maintenance: "+(enabled?"ON":"OFF")); return; }
        switch(args[0].toLowerCase(Locale.ROOT)) {
            case "on","off" -> {
                enabled=args[0].equalsIgnoreCase("on"); save();
                if(enabled && config.getBoolean("kick-online-players",true)) for(Player player:Bukkit.getOnlinePlayers()) if(!allowed(player)) player.kick(kickMessage());
                sender.sendMessage("Maintenance: "+(enabled?"ON":"OFF"));
            }
            case "setreason" -> { config.set("active-reason",String.join(" ",Arrays.copyOfRange(args,1,args.length))); save(); sender.sendMessage("Maintenance reason saved."); }
            case "whitelist" -> {
                if(args.length==1 || args[1].equalsIgnoreCase("list")) { sender.sendMessage("Whitelisted UUIDs: "+String.join(", ",whitelist)); return; }
                if(args.length!=3 || !Set.of("add","remove").contains(args[1].toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("/maintenance whitelist add|remove <online-player-or-uuid>");
                Player target=Bukkit.getPlayerExact(args[2]); String id=target==null?UUID.fromString(args[2]).toString():target.getUniqueId().toString();
                if(args[1].equalsIgnoreCase("add")) whitelist.add(id); else whitelist.remove(id);
                save(); sender.sendMessage("Maintenance whitelist saved.");
            }
            default -> throw new IllegalArgumentException("/maintenance on|off|status|setreason|whitelist");
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void login(PlayerLoginEvent event) {
        if(enabled && event.getResult()==PlayerLoginEvent.Result.ALLOWED && !allowed(event.getPlayer())) event.disallow(PlayerLoginEvent.Result.KICK_OTHER,kickMessage());
    }
    @EventHandler public void ping(PaperServerListPingEvent event) {
        if(!enabled) return;
        var motds=config.getStringList("ping-message.messages.default");
        if(config.getBoolean("ping-message.enabled",true) && !motds.isEmpty()) event.motd(message(motds.getFirst()));
        if(config.getBoolean("player-count-message.enabled",true)) { event.setVersion("§4Maintenance"); event.setProtocolVersion(-1); }
        if(config.getBoolean("player-list-hover-message.enabled",true)) {
            event.getPlayerSample().clear();
            event.getPlayerSample().add(Bukkit.createProfile(UUID.nameUUIDFromBytes("lunar-maintenance".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"Maintenance"));
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void shortcut(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        String[] parts=event.getMessage().substring(1).strip().split("\\s+");
        if(!parts[0].equalsIgnoreCase("maintenance")) return;
        event.setCancelled(true); plugin.attempt(event.getPlayer(),()->command(event.getPlayer(),Arrays.copyOfRange(parts,1,parts.length)));
    }
}
