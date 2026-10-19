package gg.lunarvoidsmp;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import gg.lunarvoidsmp.core.AutoTpa;
import net.ess3.api.events.TPARequestEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.*;

final class EssentialsTpa implements Listener {
    private final LunarPlugin plugin;
    private final Essentials essentials;
    private final AutoTpa auto=new AutoTpa();
    EssentialsTpa(LunarPlugin plugin) {
        this.plugin=plugin;
        essentials=(Essentials)Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Essentials"));
    }
    void apply(Player target) {
        Boolean value=plugin.preferences.get(target.getUniqueId(),"auto_tpa");
        if(value==null) return; // Unset must not enable auto acceptance or alter Essentials' preference.
        User user=essentials.getUser(target);
        // Essentials' native auto mode also accepts /tpahere, which has the opposite direction.
        // Lunar's explicit preference instead processes incoming /tpa through /tpaccept.
        if(user.isAutoTeleportEnabled()) user.setAutoTeleportEnabled(false);
        if(!value) { auto.forget(target.getUniqueId()); return; }
        List<AutoTpa.Request> requests=new ArrayList<>();
        for(String name:new ArrayList<>(user.getPendingTpaKeys())) {
            var request=user.getOutstandingTpaRequest(name,false); // honors expiry, without timeout messages
            if(request==null) continue;
            Player requester=Bukkit.getPlayer(request.getRequesterUuid());
            requests.add(new AutoTpa.Request(request.getRequesterUuid(),request.getTime(),request.getName(),request.isHere(),requester!=null && requester.isOnline()));
        }
        auto.accept(target.getUniqueId(),true,requests,name-> {
            // Dispatch as the target, not console. Essentials still checks permissions, world rules,
            // warmups, costs and teleport events. Failed attempts are not retried automatically.
            plugin.getServer().dispatchCommand(target,"essentials:tpaccept "+name);
        });
    }
    void forget(UUID player) { auto.forget(player); }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void request(TPARequestEvent event) {
        UUID targetId=event.getTarget().getUUID();
        Player target=Bukkit.getPlayer(targetId);
        if(target==null) return;
        if(plugin.notificationsAvailable() && Boolean.FALSE.equals(plugin.preferences.get(targetId,"tpa_request"))) {
            try {
                if(event.getRequester().getSender() instanceof Player sender) {
                User user=essentials.getUser(target),requester=essentials.getUser(sender);
                List<String> messages=new ArrayList<>();
                messages.add(plain(user,event.isTeleportHere()?"teleportHereRequest":"teleportRequest",requester.getDisplayName()));
                messages.add(plain(user,"typeTpaccept"));
                messages.add(plain(user,"typeTpdeny"));
                if(essentials.getSettings().getTpaAcceptCancellation()!=0)
                    messages.add(plain(user,"teleportRequestTimeoutInfo",essentials.getSettings().getTpaAcceptCancellation()));
                plugin.notifications.prepare(targetId,messages);
                }
            } catch(RuntimeException | LinkageError e) {
                plugin.disableTpaNotifications(e);
            }
        }
        if(!event.isTeleportHere()) plugin.getServer().getScheduler().runTask(plugin,()-> {
            if(target.isOnline()) plugin.applyTpa(target);
        });
    }
    private String plain(User user,String key,Object... arguments) {
        return EssentialsMessages.plain(user.tlComponent(key,arguments));
    }
}
