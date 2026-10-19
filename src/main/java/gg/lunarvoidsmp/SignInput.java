package gg.lunarvoidsmp;

import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.function.Consumer;

/** Client-only signs: never replace a world block or teleport the player. */
final class SignInput implements Listener {
    private record Pending(World world,BlockPosition position,Consumer<String> callback,BukkitTask timeout) {}
    private final LunarPlugin plugin;
    private final Map<UUID,Pending> pending=new HashMap<>();
    SignInput(LunarPlugin plugin) { this.plugin=plugin; }
    void open(Player player,String label,Consumer<String> callback) {
        cancel(player); player.closeInventory();
        var location=player.getLocation();
        BlockPosition position=Position.block(location.getBlockX(),Math.max(player.getWorld().getMinHeight(),location.getBlockY()-3),location.getBlockZ());
        player.sendBlockChange(position.toLocation(player.getWorld()),Material.OAK_SIGN.createBlockData());
        player.sendSignChange(position.toLocation(player.getWorld()),List.of(Component.empty(),Component.text("^^^^^^^^^^^^^^^"),Component.text(label),Component.empty()));
        int seconds=Math.max(10,Math.min(300,plugin.getConfig().getInt("integration.sign-timeout-seconds",60)));
        BukkitTask timeout=plugin.getServer().getScheduler().runTaskLater(plugin,()->cancel(player),seconds*20L);
        Pending input=new Pending(player.getWorld(),position,callback,timeout);
        pending.put(player.getUniqueId(),input);
        plugin.getServer().getScheduler().runTaskLater(plugin,()-> {
            if(pending.get(player.getUniqueId())==input && player.isOnline()) player.openVirtualSign(position,Side.FRONT);
        },2L);
    }
    @EventHandler public void change(UncheckedSignChangeEvent event) {
        Player player=event.getPlayer(); Pending input=pending.get(player.getUniqueId());
        if(input==null || !input.world().equals(player.getWorld()) || !input.position().equals(event.getEditedBlockPosition()) || event.getSide()!=Side.FRONT) return;
        event.setCancelled(true);
        String value=PlainTextComponentSerializer.plainText().serialize(event.lines().getFirst()).trim();
        cancel(player);
        if(value.isEmpty()) { plugin.notice(player,"No value entered; nothing changed."); return; }
        if(value.length()>64) { player.sendMessage(Component.text("Please use at most 64 characters.")); return; }
        plugin.getServer().getScheduler().runTask(plugin,()-> {
            if(player.isOnline()) plugin.attempt(player,()->input.callback().accept(value));
        });
    }
    void cancel(Player player) {
        Pending input=pending.remove(player.getUniqueId()); if(input==null) return;
        input.timeout().cancel();
        if(player.isOnline() && player.getWorld().equals(input.world())) {
            var loc=input.position().toLocation(input.world()); player.sendBlockChange(loc,loc.getBlock().getBlockData());
        }
    }
    void close() { for(Player player:plugin.getServer().getOnlinePlayers()) cancel(player); }
}
