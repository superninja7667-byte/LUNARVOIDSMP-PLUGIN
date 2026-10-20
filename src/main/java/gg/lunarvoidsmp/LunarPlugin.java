package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class LunarPlugin extends JavaPlugin implements Listener {
    Store store;
    Items items;
    Prices prices;
    Preferences preferences;
    TradeService trades;
    SignInput signs;
    Menus menus;
    EssentialsTpa tpa;
    TeleportRequests ownTpa;
    RandomSpawns randomSpawns;
    OwnTab ownTab;
    ExplosionViews explosions;
    private boolean packetsLoaded;
    @Override public void onLoad() {
        try {
            com.github.retrooper.packetevents.PacketEvents.setAPI(io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder.build(this));
            com.github.retrooper.packetevents.PacketEvents.getAPI().getSettings().reEncodeByDefault(true).bStats(false).checkForUpdates(false).kickOnPacketException(false);
            com.github.retrooper.packetevents.PacketEvents.getAPI().load(); packetsLoaded=true;
        } catch(RuntimeException | LinkageError e) { getLogger().warning("Packet display could not load: "+e.getClass().getSimpleName()); }
    }
    TabScoreboards scoreboards;
    RequestNotifications notifications;
    PriceDisplay priceDisplay;
    NightVision nightVision;
    private Economy economy;
    private LunarExpansion expansion;
    private FormatterExpansion formatter;
    private MaintenanceMode maintenance;
    final PlaceholderValues placeholderValues=new PlaceholderValues();
    private boolean placeholderFailureLogged;

    @Override public void onEnable() {
        try {
            saveDefaultConfig();
            configureApprovedNavigation();
            installSellPrices();
            installTabLines();
            if(!getDataFolder().isDirectory() && !getDataFolder().mkdirs()) throw new IllegalStateException("Cannot create plugin data folder");
            store=new Store(getDataFolder().toPath().resolve("lunar.db")); store.recover();
            prices=new Prices(getConfig().getConfigurationSection("prices"));
            preferences=new Preferences(store,getConfig().getConfigurationSection("settings.defaults"));
            items=new Items(store); signs=new SignInput(this); menus=new Menus(this);
            trades=new TradeService(store,new TradeService.Economy() {
                public boolean has(String owner,long cents) { requireEconomy(); return economy.has(account(owner),Money.vault(cents)); }
                public boolean withdraw(String owner,long cents) { requireEconomy(); return economy.withdrawPlayer(account(owner),Money.vault(cents)).transactionSuccess(); }
                public boolean deposit(String owner,long cents) { requireEconomy(); return economy.depositPlayer(account(owner),Money.vault(cents)).transactionSuccess(); }
            });
            getServer().getPluginManager().registerEvents(this,this);
            getServer().getPluginManager().registerEvents(signs,this);
            getServer().getPluginManager().registerEvents(menus,this);
            getServer().getPluginManager().registerEvents(new SpawnPreferences(this),this);
            for(Player player:getServer().getOnlinePlayers()) preferences.load(player.getUniqueId());
            registerExpansion();
            nightVision=new NightVision(this);
            getServer().getPluginManager().registerEvents(nightVision,this);
            explosions=new ExplosionViews(this); getServer().getPluginManager().registerEvents(explosions,this);
            if(packetsLoaded) {
                try { com.github.retrooper.packetevents.PacketEvents.getAPI().init(); priceDisplay=new PacketDisplays(this); }
                catch(RuntimeException | LinkageError e) { getLogger().warning("Packet display initialization failed: "+e.getClass().getSimpleName()); }
            }
            initializeSettingsIntegrations();
            maintenance=new MaintenanceMode(this); getServer().getPluginManager().registerEvents(maintenance,this);
            ownTab=new OwnTab(this);
            ownTpa=new TeleportRequests(this); randomSpawns=new RandomSpawns(this);
            getServer().getPluginManager().registerEvents(ownTpa,this);
            getServer().getPluginManager().registerEvents(randomSpawns,this);
            getServer().getScheduler().runTaskTimer(this,()-> {
                refreshPlaceholderValues();
                for(Player player:getServer().getOnlinePlayers()) {
                    applyTpa(player);
                    nightVision.apply(player);
                    Boolean visible=preferences.get(player.getUniqueId(),"scoreboard");
                    if(visible!=null && scoreboardAvailable(player)) applyScoreboard(player,visible);
                }
                if(notifications!=null) notifications.expire();
            },1L,20L);
            getLogger().info("LunarVoidSMP 0.6.2-test loaded. Sell prices imported when empty. Auction listings unlimited; listing fee 0%. Check /lunar status.");
            for(String review:store.reviews()) getLogger().warning("Needs reconciliation: "+review);
        } catch(Exception e) {
            getLogger().log(Level.SEVERE,"Could not start safely",e); getServer().getPluginManager().disablePlugin(this);
        }
    }
    private void installTabLines() {
        var templates=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(Objects.requireNonNull(getResource("tab-lines.yml")),java.nio.charset.StandardCharsets.UTF_8));
        for(String key:templates.getConfigurationSection("lines").getKeys(false)) if(!getConfig().isSet("tab.lines."+key)) getConfig().set("tab.lines."+key,templates.getString("lines."+key));
        saveConfig();
    }
    private void installSellPrices() {
        var defaults=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(Objects.requireNonNull(getResource("sell-prices.yml")),java.nio.charset.StandardCharsets.UTF_8));
        if(SellPriceDefaults.install(getConfig(),defaults.getConfigurationSection("prices"))) saveConfig();
    }
    void notice(Player player,String message) {
        if(preferences.enabled(player.getUniqueId(),"notifications")) player.sendMessage(Menus.text(message));
    }
    private void registerExpansion() {
        if(!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) return;
        try {
            if(expansion!=null && expansion.isRegistered()) return;
            expansion=new LunarExpansion(this);
            if(!expansion.register()) {
                getLogger().warning("Could not register %lunar_*% placeholders; check for another expansion using identifier lunar.");
                expansion=null; return;
            }
            formatter=new FormatterExpansion();
            if(!formatter.register()) { getLogger().warning("Formatter identifier already registered; keeping the existing expansion."); formatter=null; }
            refreshPlaceholderValues();
        } catch(RuntimeException | LinkageError e) {
            expansion=null;
            getLogger().log(Level.WARNING,"Lunar placeholders unavailable; other plugin features remain active",e);
        }
    }
    private void refreshPlaceholderValues() {
        if(expansion==null) return;
        try {
            collectPlaceholderValues(); placeholderFailureLogged=false;
        } catch(RuntimeException | LinkageError e) {
            placeholderValues.clear();
            if(!placeholderFailureLogged) getLogger().log(Level.WARNING,"Could not refresh Lunar placeholder values; settings and transfers remain independent",e);
            placeholderFailureLogged=true;
        }
    }
    private void collectPlaceholderValues() {
        var online=getServer().getOnlinePlayers();
        placeholderValues.server(online.size(),getServer().getTPS()[0]);
        Economy provider=null;
        try { requireEconomy(); provider=economy; } catch(RuntimeException ignored) { /* Blank balance while provider is unavailable. */ }
        for(Player player:online) {
            Long balance=null;
            if(provider!=null) {
                try { balance=PlaceholderValues.cents(provider.getBalance(player)); }
                catch(RuntimeException ignored) { /* A failed balance lookup is not a zero balance. */ }
            }
            placeholderValues.player(player.getUniqueId(),new PlaceholderValues.PlayerValues(player.getName(),balance,
                player.getPing(),prices.sell(player.getInventory().getItemInMainHand())));
        }
    }
    @EventHandler public void providerEnabled(PluginEnableEvent event) {
        if(event.getPlugin().getName().equals("PlaceholderAPI")) registerExpansion();
    }
    @EventHandler public void providerDisabled(PluginDisableEvent event) {
        if(event.getPlugin().getName().equals("PlaceholderAPI")) {
            // PAPI owns its registry teardown. Stop using that registry until it is enabled again.
            expansion=null; placeholderValues.clear();
        }
    }
    private void configureApprovedNavigation() {
        // Upgrade earlier gated defaults while preserving explicitly configured positions.
        boolean changed=false;
        for(var entry:Map.of("auction",48,"search",49,"settings",50,"pay",53).entrySet()) {
            String path="menus.navigation."+entry.getKey();
            if(!getConfig().isSet(path) || getConfig().getInt(path)==-1) {
                getConfig().set(path,entry.getValue()); changed=true;
            }
        }
        if(getConfig().getInt("auction.max-listings-per-player",10)!=0) { getConfig().set("auction.max-listings-per-player",0); changed=true; }
        if(getConfig().getDouble("auction.fee-percent",0)!=0.0) { getConfig().set("auction.fee-percent",0); changed=true; }
        if(changed) saveConfig();
    }
    private void initializeSettingsIntegrations() {
        if(getServer().getPluginManager().isPluginEnabled("TAB")) {
            try { scoreboards=new TabScoreboards(); }
            catch(RuntimeException | LinkageError e) { getLogger().log(Level.WARNING,"TAB scoreboard settings unavailable",e); }
        }
        // Inventory price display and TPA prompt filtering need a replacement adapter.
        // PlaceholderAPI supplies text values; it does not intercept inventory/message traffic.
    }
    boolean itemPricesAvailable() {
        return false; // Awaiting a verified 26.3 item-prediction reconciliation adapter.
    }
    boolean packetDisplaysAvailable() { return priceDisplay!=null && priceDisplay.healthy(); }
    boolean tpaAvailable() { return ownTpa!=null; }
    boolean notificationsAvailable() {
        return ownTpa!=null;
    }
    void disableTpaNotifications(Throwable cause) {
        getLogger().log(Level.WARNING,"TPA notification adapter stopped; requests remain functional",cause);
        if(notifications!=null) {
            try { notifications.close(); } catch(RuntimeException | LinkageError e) { cause.addSuppressed(e); }
            notifications=null;
        }
    }
    void applyTpa(Player player) {
        if(ownTpa!=null) { ownTpa.autoAccept(player); return; }
        if(tpa==null) return;
        try { tpa.apply(player); }
        catch(RuntimeException | LinkageError e) {
            getLogger().log(Level.WARNING,"Essentials TPA adapter stopped; no automatic retries. Check Essentials compatibility.",e);
            org.bukkit.event.HandlerList.unregisterAll(tpa); tpa=null;
        }
    }
    boolean scoreboardAvailable(Player player) {
        if(ownTabActive()) return true;
        if(scoreboards==null || !getServer().getPluginManager().isPluginEnabled("TAB")) return false;
        try { return scoreboards.available(player); }
        catch(RuntimeException | LinkageError e) { getLogger().log(Level.WARNING,"TAB scoreboard adapter stopped",e); scoreboards=null; return false; }
    }
    private void applyScoreboard(Player player,boolean visible) {
        if(ownTabActive()) return;
        try { scoreboards.apply(player,visible); }
        catch(RuntimeException | LinkageError e) { getLogger().log(Level.WARNING,"TAB scoreboard adapter stopped",e); scoreboards=null; }
    }
    void changePreference(Player player,String key) throws SQLException {
        boolean next;
        if(key.equals("scoreboard")) {
            if(!scoreboardAvailable(player)) throw new IllegalArgumentException("TAB scoreboard is unavailable or still loading.");
            next=ownTabActive()?!preferences.enabled(player.getUniqueId(),"scoreboard"):!scoreboards.visible(player);
        } else if(key.equals("auto_tpa")) {
            if(!tpaAvailable()) throw new IllegalArgumentException("The custom TPA service is unavailable.");
            next=!Boolean.TRUE.equals(preferences.get(player.getUniqueId(),key));
        } else next=!preferences.enabled(player.getUniqueId(),key);
        if(key.equals("tpa_request") && !notificationsAvailable()) throw new IllegalArgumentException("TPA notification visibility is awaiting the replacement message adapter.");
        if(key.equals("item_price") && !itemPricesAvailable()) throw new IllegalArgumentException("Item Price is awaiting the replacement inventory display adapter.");
        preferences.set(player.getUniqueId(),key,next);
        if(key.equals("item_price")) {
            priceDisplay.forget(player.getUniqueId());
            player.updateInventory();
        }
        if(key.equals("night_vision")) nightVision.apply(player);
        if(key.equals("auto_tpa")) applyTpa(player);
        if(key.equals("scoreboard")) applyScoreboard(player,next);
    }
    private OfflinePlayer account(String owner) { return Bukkit.getOfflinePlayer(UUID.fromString(owner)); }
    void requireEconomy() {
        var registration=getServer().getServicesManager().getRegistration(Economy.class);
        if(registration==null || !registration.getProvider().isEnabled()) throw new IllegalArgumentException("Vault has no enabled economy provider. Check Essentials and Vault.");
        economy=registration.getProvider();
        int digits=economy.fractionalDigits();
        if(digits>=0 && digits<2) throw new IllegalArgumentException("The economy provider must support cent precision.");
    }
    BigDecimal balanceValue(Player player) { requireEconomy(); return BigDecimal.valueOf(economy.getBalance(player)); }
    boolean ownTabActive() { return ownTab!=null && ownTab.active() && !getServer().getPluginManager().isPluginEnabled("TAB"); }
    String balance(Player player) {
        try { requireEconomy(); return economy.format(economy.getBalance(player)); }
        catch(IllegalArgumentException e) { return "Unavailable"; }
    }
    String playerName(String owner) { OfflinePlayer player=account(owner); return player.getName()==null?owner:player.getName(); }
    boolean auctionsConfigured() {
        try {
            Object limit=getConfig().get("auction.max-listings-per-player"),fee=getConfig().get("auction.fee-percent");
            return limit instanceof Number && ((Number)limit).longValue()>0 && ((Number)limit).longValue()<=Integer.MAX_VALUE
                && fee!=null && new BigDecimal(fee.toString()).signum()>=0 && new BigDecimal(fee.toString()).compareTo(BigDecimal.valueOf(100))<0;
        } catch(ArithmeticException | NumberFormatException e) { return false; }
    }
    boolean payLayoutConfigured() { /* Legacy diagnostic; payment uses sign submission. */
        Set<Integer> slots=new HashSet<>();
        for(String key:List.of("cancel","item","confirm")) { int slot=getConfig().getInt("menus.pay-confirmation."+key,-1); if(slot<0 || slot>=27 || !slots.add(slot)) return false; }
        return true;
    }
    boolean lineConfigured(String key) {
        return ownTabActive() || getServer().getPluginManager().isPluginEnabled("TAB") && expansion!=null && expansion.isRegistered() && getConfig().isString("tab.lines."+key) && !getConfig().getString("tab.lines."+key,"").isBlank();
    }
    void attempt(Player player,Menus.Action action) {
        try { action.run(); }
        catch(IllegalArgumentException e) { player.closeInventory(); notice(player,e.getMessage()); }
        catch(Exception e) {
            getLogger().log(Level.SEVERE,"Operation for "+player.getUniqueId()+" stopped; inspect /lunar review before retrying interrupted transfers",e);
            player.closeInventory(); notice(player,"The operation stopped safely. Ask staff to inspect /lunar review before retrying.");
        }
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(command.getName().equals("maintenance")) { try { maintenance.command(sender,args); } catch(Exception e) { sender.sendMessage(e.getMessage()==null?"Maintenance command failed.":e.getMessage()); } return true; }
        if(command.getName().equals("lunar") && args.length>0 && Set.of("status","review","reconcile").contains(args[0].toLowerCase(Locale.ROOT))) {
            if(!sender.hasPermission("lunar.admin")) { sender.sendMessage("Administrator permission required."); return true; }
            try {
                if(args[0].equalsIgnoreCase("reconcile")) {
                    if(args.length<5) throw new IllegalArgumentException("Usage: /lunar reconcile trade|item <id> complete|failed|return|delivered <reason-after-checking-logs>");
                    String reason=String.join(" ",Arrays.copyOfRange(args,4,args.length));
                    if(args[1].equals("trade")) {
                        Store.Trade t=store.trade(args[2]); if(t==null) throw new IllegalArgumentException("Unknown transaction.");
                        offline(t.buyer()); offline(t.seller());
                        if(!Set.of("complete","failed").contains(args[3])) throw new IllegalArgumentException("Trade decision must be complete or failed.");
                        store.reconcileTrade(t.id(),args[3].equals("complete"),sender.getName(),reason);
                    } else if(args[1].equals("item")) {
                        Store.Item i=store.item(args[2]); if(i==null) throw new IllegalArgumentException("Unknown item."); offline(i.owner());
                        if(!Set.of("return","delivered").contains(args[3])) throw new IllegalArgumentException("Item decision must be return or delivered.");
                        store.reconcileItem(i.id(),args[3].equals("return"),sender.getName(),reason);
                    } else throw new IllegalArgumentException("Choose trade or item.");
                    getLogger().warning("Reconciliation by "+sender.getName()+": "+args[1]+" "+args[2]+" "+args[3]+" reason="+reason);
                    sender.sendMessage("Recorded. No money was moved by reconciliation. Item claims follow the decision.");
                } else if(args[0].equalsIgnoreCase("review")) {
                    List<String> records=store.reviews(); sender.sendMessage(records.isEmpty()?"No interrupted records awaiting review.":String.join("\n",records));
                } else {
                    sender.sendMessage("LunarVoidSMP 0.6.1-test; Paper API 26.3 build 205; Java "+System.getProperty("java.version"));
                    sender.sendMessage("Priced materials: "+prices.materials().size()+"; auction rules: "+auctionsConfigured()+"; Pay: sign amount sends immediately");
                    try { requireEconomy(); sender.sendMessage("Economy: "+economy.getName()); } catch(IllegalArgumentException e) { sender.sendMessage(e.getMessage()); }
                    for(String name:List.of("TAB","PlaceholderAPI","LuckPerms","Essentials","BetterRTP","Geyser-Spigot","floodgate")) {
                        var dependency=getServer().getPluginManager().getPlugin(name); sender.sendMessage(name+": "+(dependency==null?"not installed":dependency.getPluginMeta().getVersion()+" enabled="+dependency.isEnabled()));
                    }
                    sender.sendMessage("Built-in Lunar placeholders: "+(expansion!=null && expansion.isRegistered())+"; identifier lunar; no separate expansion JAR needed.");
                    sender.sendMessage("Auto TPA adapter: "+tpaAvailable()+"; TPA notification filter: "+notificationsAvailable()+"; natural spawn controls: enabled; scoreboard: existing TAB or optional own TAB.");
                    sender.sendMessage("Item Price packet display: "+itemPricesAvailable()+"; default OFF; /sell unit cap $30,000.00; stack totals multiply quantity.");
                    sender.sendMessage("Pending: inventory tooltip, skins, exact TAB visual parity and live Paper/Geyser/Quest verification. Own TAB is off by default.");
                }
            } catch(IllegalArgumentException e) { sender.sendMessage(e.getMessage()); }
            catch(SQLException e) { getLogger().log(Level.SEVERE,"Diagnostics failed",e); sender.sendMessage("Cannot read journal; see server log."); }
            return true;
        }
        if(!(sender instanceof Player player)) { sender.sendMessage("Use this command in game."); return true; }
        attempt(player,()-> {
            switch(command.getName()) {
                case "shop" -> menus.main(player);
                case "sell" -> menus.sell(player);
                case "settings" -> menus.settings(player);
                case "pay" -> {
                    if(args.length!=0) throw new IllegalArgumentException("Use /pay to choose a player and gift money.");
                    menus.payPlayers(player,0);
                }
                case "ah" -> auctionCommand(player,args);
                case "tpa","tpahere","tpaccept","tpdeny","tpacancel","tpatoggle" -> ownTpa.handle(player,command.getName(),args);
                case "rtp" -> randomSpawns.rtp(player,preferences.enabled(player.getUniqueId(),"instant_rtp"));
                case "lunar" -> {
                    if(args.length==0) { notice(player,"/lunar pay | search | catalog [page] | claim"); return; }
                    switch(args[0].toLowerCase(Locale.ROOT)) {
                        case "pay" -> menus.payPlayers(player,args.length>1?Integer.parseInt(args[1])-1:0);
                        case "search" -> menus.search(player);
                        case "catalog" -> menus.catalog(player,args.length>1?Integer.parseInt(args[1])-1:0);
                        case "claim" -> { items.claim(player); notice(player,"Claim checked. Items that do not fit remain stored."); }
                        default -> notice(player,"Unknown option.");
                    }
                }
            }
        }); return true;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void payShortcut(PlayerCommandPreprocessEvent event) {
        // Essentials may own the bare command in Bukkit's map. Route only /pay itself;
        // other plugins' namespaces/arguments and earlier command cancellations are respected.
        if(!event.getMessage().strip().equalsIgnoreCase("/pay")) return;
        event.setCancelled(true);
        attempt(event.getPlayer(),()->menus.payPlayers(event.getPlayer(),0));
    }
    private void offline(String owner) {
        if(owner!=null && account(owner).isOnline()) throw new IllegalArgumentException("Affected players must be offline before reconciliation.");
    }
    private void auctionCommand(Player player,String[] args) throws SQLException {
        if(args.length==0) { menus.auctions(player,"all","",0,"recent"); return; }
        switch(args[0].toLowerCase(Locale.ROOT)) {
            case "sell" -> { if(args.length!=2) throw new IllegalArgumentException("Usage: /ah sell <price>"); menus.listing(player,Money.parse(args[1])); }
            case "claim" -> { items.claim(player); notice(player,"Claim checked. Full-inventory items remain stored."); }
            case "cancel" -> {
                if(args.length!=2) throw new IllegalArgumentException("Usage: /ah cancel <listing-id>");
                boolean cancelled=store.cancel(args[1],player.getUniqueId().toString());
                if(cancelled) menus.removeAuctionListing(args[1]);
                notice(player,cancelled?"Cancelled; use /ah claim.":"Listing was not available to cancel.");
            }
            case "mine" -> menus.yourItems(player);
            case "sort" -> {
                if(args.length!=2 || !Set.of("recent","highest","lowest").contains(args[1])) throw new IllegalArgumentException("Usage: /ah sort recent|highest|lowest");
                menus.auctions(player,"all","",0,args[1]);
            }
            case "page" -> { if(args.length!=2) throw new IllegalArgumentException("Usage: /ah page <number>"); menus.auctions(player,"all","",Integer.parseInt(args[1])-1,"recent"); }
            default -> throw new IllegalArgumentException("Usage: /ah [sell <price>|claim|mine|cancel <id>|sort <recent|highest|lowest>|page <number>]");
        }
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player player=event.getPlayer(); attempt(player,()-> {
            preferences.load(player.getUniqueId());
            refreshPlaceholderValues();
            if(nightVision!=null) nightVision.apply(player);
            items.claim(player);
            long remaining=store.items(player.getUniqueId().toString()).stream().filter(i->i.purpose().equals("CLAIM") && i.state().equals("HELD")).count();
            if(remaining>0) notice(player,"You have "+remaining+" stored item stack(s). Make space and use /lunar claim.");
        });
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        UUID id=event.getPlayer().getUniqueId();
        signs.cancel(event.getPlayer()); attempt(event.getPlayer(),()->items.returnSell(event.getPlayer()));
        if(tpa!=null) tpa.forget(id);
        if(notifications!=null) notifications.forget(id);
        if(priceDisplay instanceof PacketDisplays packetDisplays) packetDisplays.disconnect(id);
        else if(priceDisplay!=null) priceDisplay.forget(id);
        if(nightVision!=null) nightVision.release(event.getPlayer());
        preferences.unload(id);
        placeholderValues.forget(id);
    }
    @EventHandler public void worldChange(PlayerChangedWorldEvent event) { signs.cancel(event.getPlayer()); }
    @EventHandler(ignoreCancelled=true) public void teleport(PlayerTeleportEvent event) { signs.cancel(event.getPlayer()); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void chat(AsyncChatEvent event) {
        event.viewers().removeIf(audience->audience instanceof Player viewer && !preferences.enabled(viewer.getUniqueId(),"global_chat"));
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(org.bukkit.event.entity.PlayerDeathEvent event) {
        var message=event.deathMessage(); if(message==null || Boolean.FALSE.equals(event.getEntity().getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES))) return;
        event.deathMessage(null);
        getServer().getConsoleSender().sendMessage(message);
        for(Player viewer:getServer().getOnlinePlayers()) if(preferences.enabled(viewer.getUniqueId(),"death_messages")) viewer.sendMessage(message);
    }
    @Override public void onDisable() {
        if(ownTab!=null) ownTab.close();
        if(nightVision!=null) nightVision.close();
        if(priceDisplay!=null) {
            try { priceDisplay.close(); } catch(RuntimeException | LinkageError e) { getLogger().log(Level.WARNING,"Could not detach Item Price listener",e); }
        }
        if(notifications!=null) {
            try { notifications.close(); } catch(RuntimeException | LinkageError e) { getLogger().log(Level.WARNING,"Could not detach TPA packet listener",e); }
        }
        if(signs!=null) signs.close();
        if(store!=null) {
            for(Player player:getServer().getOnlinePlayers()) {
                if(player.getOpenInventory().getTopInventory().getHolder() instanceof Menus.Menu) player.closeInventory();
                if(items!=null) attempt(player,()->items.returnSell(player));
            }
            try { store.close(); } catch(SQLException e) { getLogger().log(Level.SEVERE,"Could not close journal",e); }
        }
        if(expansion!=null) {
            try { expansion.unregister(); } catch(RuntimeException | LinkageError e) { getLogger().log(Level.WARNING,"Could not unregister Lunar placeholders",e); }
        }
        if(formatter!=null) formatter.unregister();
        if(packetsLoaded) com.github.retrooper.packetevents.PacketEvents.getAPI().terminate();
        placeholderValues.clear();
    }
}
