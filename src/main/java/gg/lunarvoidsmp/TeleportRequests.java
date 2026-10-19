package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.TpaRequests;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.util.*;

final class TeleportRequests implements Listener {
    private final LunarPlugin plugin;
    private final TpaRequests requests=new TpaRequests();
    private static final Set<String> COMMANDS=Set.of("tpa","tpahere","tpaccept","tpdeny","tpacancel","tpatoggle");
    TeleportRequests(LunarPlugin plugin) { this.plugin=plugin; }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void command(PlayerCommandPreprocessEvent event) {
        String[] parts=event.getMessage().substring(1).strip().split("\\s+");
        String cmd=parts[0].toLowerCase(Locale.ROOT); if(!COMMANDS.contains(cmd)) return;
        event.setCancelled(true);
        plugin.attempt(event.getPlayer(),()->handle(event.getPlayer(),cmd,Arrays.copyOfRange(parts,1,parts.length)));
    }
    void handle(Player player,String cmd,String[] args) throws Exception {
        long now=System.currentTimeMillis(); UUID id=player.getUniqueId();
        switch(cmd) {
            case "tpa","tpahere" -> {
                if(args.length!=1) throw new IllegalArgumentException("Usage: /"+cmd+" <player>");
                Player target=Bukkit.getPlayerExact(args[0]);
                if(target==null || !player.canSee(target)) throw new IllegalArgumentException("That player is not online.");
                if(!plugin.preferences.enabled(target.getUniqueId(),"tpa_receiving")) throw new IllegalArgumentException("That player is not receiving teleport requests.");
                var request=requests.create(id,target.getUniqueId(),cmd.equals("tpahere"),now);
                if(!request.here() && Boolean.TRUE.equals(plugin.preferences.get(target.getUniqueId(),"auto_tpa"))) { accept(target,request); return; }
                plugin.notice(player,"Teleport request sent to "+target.getName()+".");
                if(plugin.preferences.enabled(target.getUniqueId(),"notifications") && plugin.preferences.enabled(target.getUniqueId(),"tpa_request")) {
                    target.sendMessage(Component.text(player.getName()+" sent you a "+(request.here()?"TPA-Here":"TPA")+"-Request! ",NamedTextColor.WHITE)
                        .append(Component.text("/tpaccept",NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/tpaccept "+player.getName())))
                        .append(Component.text(" or ",NamedTextColor.WHITE))
                        .append(Component.text("/tpdeny",NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/tpdeny "+player.getName()))));
                }
            }
            case "tpaccept","tpdeny" -> {
                var incoming=requests.incoming(id,now);
                var chosen=incoming.stream().filter(r->args.length==0 || Optional.ofNullable(Bukkit.getPlayer(r.sender())).map(p->p.getName().equalsIgnoreCase(args[0])).orElse(false)).reduce((a,b)->b).orElseThrow(()->new IllegalArgumentException("No matching teleport request."));
                if(cmd.equals("tpaccept")) accept(player,chosen);
                else { requests.remove(chosen.sender()); plugin.notice(player,"Teleport request denied."); Player sender=Bukkit.getPlayer(chosen.sender()); if(sender!=null) plugin.notice(sender,"Your teleport request was denied."); }
            }
            case "tpacancel" -> { requests.remove(id); plugin.notice(player,"Your outgoing teleport request was cancelled."); }
            case "tpatoggle" -> { boolean enabled=!plugin.preferences.enabled(id,"tpa_receiving"); plugin.preferences.set(id,"tpa_receiving",enabled); plugin.notice(player,"Receiving TPA requests: "+(enabled?"ON":"OFF")); }
            default -> throw new IllegalArgumentException("Unknown teleport command.");
        }
    }
    void autoAccept(Player player) {
        if(!Boolean.TRUE.equals(plugin.preferences.get(player.getUniqueId(),"auto_tpa"))) return;
        for(var request:requests.incoming(player.getUniqueId(),System.currentTimeMillis())) if(!request.here()) accept(player,request);
    }
    private void accept(Player target,TpaRequests.Request request) {
        requests.remove(request.sender()); Player sender=Bukkit.getPlayer(request.sender());
        if(sender==null || !sender.isOnline() || !target.isOnline()) { plugin.notice(target,"The requesting player went offline."); return; }
        Player moving=request.here()?target:sender; Player destination=request.here()?sender:target;
        moving.teleportAsync(destination.getLocation(),PlayerTeleportEvent.TeleportCause.COMMAND).whenComplete((ok,error)-> {
            if(plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin,()-> { if(moving.isOnline()) plugin.notice(moving,error==null && Boolean.TRUE.equals(ok)?"Teleport complete.":"Teleport was cancelled or could not complete."); });
        });
    }
    @EventHandler public void quit(PlayerQuitEvent event) { requests.forget(event.getPlayer().getUniqueId()); }
}
