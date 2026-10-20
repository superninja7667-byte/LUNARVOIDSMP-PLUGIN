package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;

final class Menus implements Listener {
    @FunctionalInterface interface Action { void run() throws Exception; }
    static final class Menu implements InventoryHolder {
        final UUID owner;
        final String kind;
        final Inventory inventory;
        final Map<Integer,Action> actions=new HashMap<>();
        final Map<String,Integer> listings=new HashMap<>();
        final String transaction=UUID.randomUUID().toString();
        String auctionListing;
        boolean busy;
        Menu(Player player,String kind,int size,String title) {
            owner=player.getUniqueId(); this.kind=kind; inventory=Bukkit.createInventory(this,size,Component.text(title));
        }
        @Override public Inventory getInventory() { return inventory; }
    }
    private final LunarPlugin p;
    Menus(LunarPlugin plugin) { p=plugin; }
    static Component text(String value) { return Component.text(value,NamedTextColor.WHITE).decoration(TextDecoration.ITALIC,false); }
    static ItemStack icon(Material material,String name,String... lore) {
        ItemStack item=new ItemStack(material); var meta=item.getItemMeta(); meta.displayName(text(name));
        meta.lore(Arrays.stream(lore).map(Menus::text).toList()); item.setItemMeta(meta); return item;
    }
    private void button(Menu menu,int slot,ItemStack item,Action action) {
        if(slot<0) return;
        if(slot>=menu.inventory.getSize() || menu.actions.containsKey(slot) || menu.inventory.getItem(slot)!=null)
            throw new IllegalArgumentException("Menu slot "+slot+" is out of range or overlaps another button.");
        menu.inventory.setItem(slot,item); if(action!=null) menu.actions.put(slot,action);
    }
    void main(Player player) {
        Menu menu=new Menu(player,"MAIN",54,"Shop");
        ItemStack head=icon(Material.PLAYER_HEAD,"Player", "Balance: "+p.balance(player));
        SkullMeta skull=(SkullMeta)head.getItemMeta(); skull.setOwningPlayer(player); head.setItemMeta(skull);
        button(menu,45,head,null);
        button(menu,13,icon(Material.DIAMOND,"Materials"),()->auctions(player,"materials","",0,"recent"));
        button(menu,21,icon(Material.DIAMOND_SWORD,"Armor & Tools"),()->auctions(player,"equipment","",0,"recent"));
        button(menu,22,icon(Material.END_CRYSTAL,"PVP Items"),()->auctions(player,"pvp","",0,"recent"));
        button(menu,23,icon(Material.ENCHANTED_GOLDEN_APPLE,"Food"),()->auctions(player,"food","",0,"recent"));
        button(menu,31,icon(Material.ENCHANTING_TABLE,"Enchanting"),()->auctions(player,"enchanting","",0,"recent"));
        navigation(menu,player,false);
        player.openInventory(menu.inventory);
    }
    private void navigation(Menu menu,Player player,boolean pay) {
        button(menu,48,icon(Material.HOPPER,"Shop"),()->auctions(player,"all","",0,"recent"));
        button(menu,49,icon(Material.OAK_SIGN,"Search"),()-> { if(pay) paySearch(player); else search(player); });
        button(menu,50,icon(Material.OAK_PLANKS,"Settings"),()->settings(player));
        button(menu,51,icon(Material.CHEST,"Your Items","Click to view"),()->yourItems(player));
        button(menu,52,icon(Material.ENDER_CHEST,"Quick Search","Click to search"),()-> { if(pay) paySearch(player); else search(player); });
        button(menu,53,icon(Material.BARREL,"Pay"),()->payPlayers(player,0));
    }
    void yourItems(Player player) throws SQLException {
        Menu menu=new Menu(player,"YOUR_ITEMS",54,"Auction -> Your Items");
        int slot=0;
        for(Store.Listing listing:p.store.listings()) {
            if(!listing.owner().equals(player.getUniqueId().toString()) || slot>=54) continue;
            ItemStack item=ItemStack.deserializeBytes(p.store.item(listing.item()).payload());
            append(item,"Price: "+Money.full(listing.price()));
            button(menu,slot,item,null); menu.listings.put(listing.id(),slot++);
        }
        while(slot<54) button(menu,slot++,icon(Material.RED_STAINED_GLASS_PANE," "),null);
        player.openInventory(menu.inventory);
    }
    private void paySearch(Player player) {
        p.signs.open(player,"Search",value->payPlayers(player,0,value));
    }
    void search(Player player) { p.signs.open(player,"Search",value->p.attempt(player,()->auctions(player,"all",value,0,"recent"))); }
    void auctions(Player player,String category,String query,int page,String sort) throws SQLException {
        List<Store.Listing> listings=new ArrayList<>(); Map<String,ItemStack> stacks=new HashMap<>();
        for(Store.Listing listing:p.store.listings()) {
            Store.Item held=p.store.item(listing.item()); ItemStack item=ItemStack.deserializeBytes(held.payload());
            if(matches(item,category) && Search.rank(query,name(item))>=0) { listings.add(listing); stacks.put(listing.id(),item); }
        }
        Comparator<Store.Listing> comparator=switch(sort) {
            case "highest" -> Comparator.comparingLong(Store.Listing::price).reversed();
            case "lowest" -> Comparator.comparingLong(Store.Listing::price);
            default -> Comparator.comparingLong(Store.Listing::created).reversed();
        };
        if(!query.isBlank()) comparator=Comparator.<Store.Listing>comparingInt(l->Search.rank(query,name(stacks.get(l.id())))).thenComparing(comparator);
        listings.sort(comparator.thenComparing(Store.Listing::id));
        int last=Math.max(0,(listings.size()-1)/45), current=Math.max(0,Math.min(page,last));
        Menu menu=new Menu(player,"AUCTIONS",54,"Auction (Page "+(current+1)+")");
        for(int i=current*45;i<Math.min(listings.size(),(current+1)*45);i++) {
            Store.Listing listing=listings.get(i); ItemStack item=stacks.get(listing.id()).clone();
            append(item,"Price: "+Money.full(listing.price()),"Seller: "+p.playerName(listing.owner()));
            button(menu,i-current*45,item,()->auctionBuy(player,listing));
            menu.listings.put(listing.id(),i-current*45);
        }
        String nextSort=switch(sort) { case "default"->"lowest"; case "lowest"->"highest"; case "highest"->"recent"; default->"default"; };
        button(menu,47,icon(Material.CAULDRON,"Filter","Click to change",(sort.equals("default")?"> ":"")+"Default",(sort.equals("lowest")?"> ":"")+"Lowest Price",(sort.equals("highest")?"> ":"")+"Highest Price",(sort.equals("recent")?"> ":"")+"Recently Listed"),()->auctions(player,category,query,0,nextSort));
        if(current>0) button(menu,45,icon(Material.SLIME_BALL,"Previous page","Click to view previous page"),()->auctions(player,category,query,current-1,sort));
        button(menu,51,icon(Material.CHEST,"Your Items","Click to view"),()->yourItems(player));
        button(menu,53,icon(Material.MAGMA_CREAM,"Next page","Click to view next page"),()->auctions(player,category,query,current+1,sort));
        player.openInventory(menu.inventory);
    }
    private boolean matches(ItemStack item,String category) {
        Material m=item.getType(); String n=m.name();
        boolean equipment=n.matches(".*_(SWORD|PICKAXE|AXE|SHOVEL|HOE|HELMET|CHESTPLATE|LEGGINGS|BOOTS)") || Set.of("BOW","CROSSBOW","TRIDENT","MACE","SHIELD","ELYTRA","SHEARS","FISHING_ROD").contains(n);
        boolean enchanted=m==Material.ENCHANTED_BOOK || !item.getEnchantments().isEmpty();
        return switch(category) {
            case "blocks" -> m.isBlock(); case "pvp" -> Set.of("END_CRYSTAL","TNT","TNT_MINECART","RESPAWN_ANCHOR","GLOWSTONE","OBSIDIAN","ENDER_PEARL","FIRE_CHARGE","FLINT_AND_STEEL","TOTEM_OF_UNDYING","SHIELD","TRIDENT","BOW","CROSSBOW").contains(n); case "food" -> m.isEdible(); case "equipment" -> equipment;
            case "enchanting" -> enchanted || Set.of(Material.ENCHANTING_TABLE,Material.ANVIL,Material.LAPIS_LAZULI,Material.EXPERIENCE_BOTTLE,Material.BOOK,Material.BOOKSHELF).contains(m);
            case "materials" -> !m.isBlock() && !m.isEdible() && !equipment && !enchanted;
            default -> true;
        };
    }
    static String name(ItemStack item) {
        String material=item.getType().name().replace('_',' ');
        var meta=item.getItemMeta(); return meta!=null && meta.hasDisplayName()?material+" "+PlainTextComponentSerializer.plainText().serialize(meta.displayName()):material;
    }
    private static void append(ItemStack item,String... lines) {
        var meta=item.getItemMeta(); List<Component> lore=new ArrayList<>(meta.hasLore()?meta.lore():List.of());
        for(String line:lines) lore.add(text(line)); meta.lore(lore); item.setItemMeta(meta);
    }
    void listing(Player player,long price) throws SQLException {
        if(price<=0) throw new IllegalArgumentException("Price must be greater than $0.");
        ItemStack snapshot=player.getInventory().getItemInMainHand().clone();
        if(snapshot.getType().isAir()) throw new IllegalArgumentException("Hold the stack you want to list.");
        p.requireEconomy();
        int slot=player.getInventory().getHeldItemSlot();
        String item=p.items.take(player,slot,snapshot,"CLAIM");
        try {
            p.store.list(item,player.getUniqueId().toString(),price,p.getConfig().getInt("auction.max-listings-per-player",0));
        } catch(SQLException | RuntimeException e) {
            try { p.items.claim(player); } catch(SQLException recovery) { e.addSuppressed(recovery); }
            throw e;
        }
        p.notice(player,"Listed "+snapshot.getAmount()+"x "+name(snapshot)+" for "+Money.full(price)+".");
    }
    private void auctionBuy(Player player,Store.Listing listing) throws SQLException {
        if(listing.owner().equals(player.getUniqueId().toString())) throw new IllegalArgumentException("You cannot buy your own listing.");
        Store.Item held=p.store.item(listing.item());
        if(held==null || !held.owner().equals(listing.owner()) || !held.state().equals("HELD") || !held.purpose().equals("AUCTION"))
            throw new IllegalArgumentException("This listing is no longer available.");
        ItemStack original=ItemStack.deserializeBytes(held.payload());
        String soldItem=original.getAmount()+"x "+name(original);
        ItemStack preview=ItemStack.deserializeBytes(held.payload()); append(preview,"Price: "+Money.full(listing.price()),"Seller: "+p.playerName(listing.owner()));
        var pm=preview.getItemMeta(); var pl=new ArrayList<Component>(pm.lore()); pl.set(pl.size()-2,Component.text("Price: ",NamedTextColor.GREEN).append(text(Money.full(listing.price())))); pl.set(pl.size()-1,Component.text("Seller: ",NamedTextColor.AQUA).append(text(p.playerName(listing.owner())))); pm.lore(pl); preview.setItemMeta(pm);
        Menu menu=new Menu(player,"AUCTION_BUY",27,"Confirm Purchase"); menu.auctionListing=listing.id(); button(menu,13,preview,null);
        button(menu,11,icon(Material.RED_STAINED_GLASS_PANE,"Cancel","Click to cancel"),player::closeInventory);
        button(menu,15,icon(Material.LIME_STAINED_GLASS_PANE,"Confirm","Click to buy"),()-> {
            if(menu.busy) return; menu.busy=true;
            // Closing first returns a held cursor stack before checking the buyer's actual space.
            player.closeInventory(); p.requireEconomy();
            
            long fee=BigDecimal.valueOf(listing.price()).multiply(new BigDecimal(p.getConfig().getString("auction.fee-percent"))).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN).longValueExact();
            boolean ok=p.trades.buyAuction(new Store.Trade(menu.transaction,"BUY_AUCTION",player.getUniqueId().toString(),listing.owner(),listing.price(),fee,"PREPARED",listing.id()),listing.item(),()->p.items.fits(player,original));
            if(!ok) { p.notice(player,"Purchase did not complete. Contact staff if marked for review."); return; }
            removeAuctionListing(listing.id());
            Player seller=Bukkit.getPlayer(UUID.fromString(listing.owner()));
            if(seller!=null) p.notice(seller,player.getName()+" bought your "+soldItem+". Received "+Money.full(listing.price()-fee));
            // Deliver this purchase before unrelated older claims can occupy the checked space.
            if(p.items.give(player,p.store.item(listing.item())))
                p.notice(player,"Purchased "+soldItem+" for "+Money.full(listing.price())+". Added to your inventory.");
            else p.notice(player,"Purchase completed, but your inventory changed during payment. Your item is safely stored; make space and use /ah claim.");
        }); player.openInventory(menu.inventory);
    }
    void removeAuctionListing(String id) {
        for(Player viewer:Bukkit.getOnlinePlayers()) {
            if(!(viewer.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu)) continue;
            Integer slot=menu.listings.remove(id);
            if(slot!=null) { menu.inventory.clear(slot); menu.actions.remove(slot); }
            if(id.equals(menu.auctionListing)) {
                viewer.closeInventory(); p.notice(viewer,"This listing is no longer available.");
            }
        }
    }
    void catalog(Player player,int page) {
        Menu menu=new Menu(player,"CATALOG",54,"Shop");
        List<Material> materials=p.prices.materials().stream().filter(m->p.prices.buy(m)!=null).sorted().toList();
        int current=Math.max(0,Math.min(page,Math.max(0,(materials.size()-1)/45)));
        for(int i=current*45;i<Math.min(materials.size(),(current+1)*45);i++) {
            Material material=materials.get(i); button(menu,i-current*45,icon(material,name(new ItemStack(material)),Money.full(p.prices.buy(material))),()->purchase(player,material,1));
        }
        player.openInventory(menu.inventory);
        if(materials.isEmpty()) p.notice(player,"No buy prices are configured yet.");
    }
    void purchase(Player player,Material material,int quantity) {
        int count=Math.max(1,Math.min(quantity,36*material.getMaxStackSize()));
        Long unit=p.prices.buy(material); if(unit==null) throw new IllegalArgumentException("This item has no buy price.");
        long total=Math.multiplyExact(unit,count); Menu menu=new Menu(player,"PURCHASE",54,"Shop");
        ItemStack item=icon(material,name(new ItemStack(material)),"Quantity: "+count); item.setAmount(Math.min(count,material.getMaxStackSize())); button(menu,22,item,null);
        for(int slot:new int[]{19,20,21,23,24,25}) {
            Object value=p.getConfig().get("menus.quantity-steps."+slot);
            if(!(value instanceof Number number) || !Set.of(-10,-5,-1,1,5,10).contains(number.intValue()) || (slot<22)!=(number.intValue()<0)) {
                button(menu,slot,icon(slot<22?Material.RED_STAINED_GLASS_PANE:Material.LIME_STAINED_GLASS_PANE,"Quantity","Step placement awaiting confirmation"),null); continue;
            }
            int delta=number.intValue(); button(menu,slot,icon(delta<0?Material.RED_STAINED_GLASS_PANE:Material.LIME_STAINED_GLASS_PANE,(delta>0?"+":"")+delta),()->purchase(player,material,count+delta));
        }
        button(menu,49,icon(Material.NETHER_STAR,"Back"),()->main(player));
        button(menu,31,icon(Material.EMERALD_BLOCK,"Confirm",Money.full(total),"Quantity: "+count),()-> {
            if(menu.busy) return; menu.busy=true; p.requireEconomy();
            List<String> ids=new ArrayList<>();
            for(int remaining=count;remaining>0;) {
                int part=Math.min(remaining,material.getMaxStackSize());
                ids.add(p.store.addItem(player.getUniqueId().toString(),new ItemStack(material,part).serializeAsBytes(),"HELD","STOCK")); remaining-=part;
            }
            Store.Trade trade=new Store.Trade(menu.transaction,"BUY_STOCK",player.getUniqueId().toString(),null,total,0,"PREPARED",null);
            boolean ok=p.trades.execute(trade,ids); player.closeInventory();
            p.notice(player,ok?"Purchased for "+Money.full(total)+". Items that don't fit stay in /lunar claim.":"Purchase did not complete. No automatic retry; contact staff if marked for review.");
            if(ok) p.items.claim(player);
        }); player.openInventory(menu.inventory);
    }
    void sell(Player player) throws SQLException {
        player.closeInventory();
        Menu menu=new Menu(player,"SELL",54,"Sell"); fillSell(player,menu); player.openInventory(menu.inventory);
    }
    private List<Store.Item> sellItems(Player player) throws SQLException { return p.store.items(player.getUniqueId().toString()).stream().filter(i->i.state().equals("HELD") && i.purpose().equals("SELL")).toList(); }
    private void fillSell(Player player,Menu menu) throws SQLException {
        menu.inventory.clear(); menu.actions.clear(); long total=0; List<String> eligible=new ArrayList<>(); int slot=0;
        for(Store.Item held:sellItems(player)) {
            ItemStack item=ItemStack.deserializeBytes(held.payload()); Long price=p.prices.sell(item);
            if(price!=null) { total=Math.addExact(total,price); eligible.add(held.id()); }
            int cell=slot++; if(cell>=53) throw new IllegalStateException("Too many sell entries.");
            ItemStack preview=item.clone(); if(price==null) append(preview,"Not sellable; returned on close");
            button(menu,cell,preview,()-> {
                if(menu.busy) return;
                p.store.update("UPDATE items SET purpose='CLAIM' WHERE id=? AND owner=? AND state='HELD' AND purpose='SELL'",held.id(),player.getUniqueId().toString());
                p.items.claim(player); fillSell(player,menu);
            });
        }
        long payout=total;
        ItemStack confirm=icon(Material.LIME_STAINED_GLASS_PANE,"$ "+Money.shortAmount(total),"Click to sell items");
        var meta=confirm.getItemMeta(); meta.displayName(Component.text("$ ",NamedTextColor.GREEN).append(text(Money.shortAmount(total)))); confirm.setItemMeta(meta);
        button(menu,53,confirm,()-> {
            if(menu.busy || payout<=0) return; menu.busy=true; p.requireEconomy();
            // Recalculate from authoritative escrow and prices at confirmation, not from client lore.
            long fresh=0; List<String> freshIds=new ArrayList<>();
            for(Store.Item held:sellItems(player)) { Long price=p.prices.sell(ItemStack.deserializeBytes(held.payload())); if(price!=null) { fresh=Math.addExact(fresh,price); freshIds.add(held.id()); } }
            if(fresh!=payout || !freshIds.equals(eligible)) throw new IllegalArgumentException("Items changed. Reopen Sell to check the total.");
            boolean ok=p.trades.execute(new Store.Trade(menu.transaction,"SELL",null,player.getUniqueId().toString(),fresh,0,"PREPARED",null),freshIds);
            player.closeInventory(); p.notice(player,ok?"Sold for "+Money.full(fresh):"Sale did not complete. Contact staff if marked for review.");
        });
    }
    void payPlayers(Player player,int page) { payPlayers(player,page,""); }
    void payPlayers(Player player,int page,String query) {
        Menu menu=new Menu(player,"PAY_PLAYERS",54,"Pay");
        List<Player> targets=Bukkit.getOnlinePlayers().stream().filter(t->!t.equals(player) && player.canSee(t) && Search.rank(query,t.getName())>=0).sorted(Comparator.comparing(Player::getName)).map(t->(Player)t).toList();
        int current=Math.max(0,Math.min(page,Math.max(0,(targets.size()-1)/45)));
        for(int i=current*45;i<Math.min(targets.size(),(current+1)*45);i++) {
            Player target=targets.get(i); ItemStack head=icon(Material.PLAYER_HEAD,target.getName()); SkullMeta meta=(SkullMeta)head.getItemMeta(); meta.setOwningPlayer(target); head.setItemMeta(meta);
            button(menu,i-current*45,head,()->p.signs.open(player,"Amount",value->p.attempt(player,()->payNow(player,target.getUniqueId(),Money.parse(value)))));
        }
        button(menu,45,icon(Material.SLIME_BALL,"Previous page"),()->payPlayers(player,current-1,query));
        button(menu,46,icon(Material.MAGMA_CREAM,"Next page"),()->payPlayers(player,current+1,query));
        ItemStack money=icon(Material.EMERALD,"Money"); var mm=money.getItemMeta(); mm.lore(List.of(Component.text("$",NamedTextColor.GREEN).append(text(p.balance(player).replace("$",""))))); money.setItemMeta(mm);
        button(menu,47,money,null); navigation(menu,player,true);
        player.openInventory(menu.inventory);
    }
    private void payNow(Player player,UUID target,long amount) throws SQLException {
        p.requireEconomy();
        Player online=Bukkit.getPlayer(target);
        if(online==null || !online.isOnline() || !player.canSee(online) || online.equals(player)) throw new IllegalArgumentException("That player is no longer available.");
        boolean ok=p.trades.execute(new Store.Trade(UUID.randomUUID().toString(),"PAY",player.getUniqueId().toString(),target.toString(),amount,0,"PREPARED",null),List.of());
        p.notice(player,ok?"Sent "+Money.full(amount)+" to "+online.getName():"Payment did not complete. Contact staff if marked for review.");
        if(ok) p.notice(online,"Received "+Money.full(amount)+" from "+player.getName());
    }
    void settings(Player player) {
        Menu menu=new Menu(player,"SETTINGS",54,"Settings");
        String[] names={"Notifications","Global Chat","Death Messages","Money Tag","Money","Discord Link","Online","Rank Tag","Ping","TPA Request","TNT Particles","End Crystal Particles","Auto TPA","Phantom Spawning","Mob Spawning","Server TPS","Item Price","Scoreboard","Night Vision"};
        Material[] icons={Material.ENCHANTED_BOOK,Material.WRITABLE_BOOK,Material.SKELETON_SKULL,Material.LIME_DYE,Material.ENDER_EYE,Material.NETHER_BRICK,Material.SLIME_BALL,Material.ARMOR_STAND,Material.ECHO_SHARD,Material.PAPER,Material.TNT,Material.END_CRYSTAL,Material.LIGHT,Material.PHANTOM_MEMBRANE,Material.ZOMBIE_HEAD,Material.REDSTONE_BLOCK,Material.EMERALD,Material.OAK_HANGING_SIGN,Material.POTION};
        int[] slots={11,12,13,14,15,20,21,22,23,24,29,30,31,32,33,38,40,41,42};
        for(int i=0;i<names.length;i++) {
            String key=Preferences.KEYS.get(i); Boolean value=p.preferences.enabled(player.getUniqueId(),key);
            boolean supported=Set.of("notifications","death_messages","global_chat","mob_spawning","phantom_spawning","night_vision").contains(key)
                || (key.equals("auto_tpa") && p.tpaAvailable()) || (key.equals("tpa_request") && p.notificationsAvailable())
                || (key.equals("scoreboard") && p.scoreboardAvailable(player))
                || (key.equals("item_price") && p.itemPricesAvailable())
                || (Set.of("tnt_particles","end_crystal_particles").contains(key) && p.packetDisplaysAvailable())
                || (Set.of("money","discord_link","online","ping","server_tps","rank_tag","money_tag").contains(key) && p.lineConfigured(key));
            String detail=switch(key) {
                case "tpa_request" -> supported?"Show or hide incoming TPA request messages":"Message integration pending";
                case "auto_tpa" -> supported?"Accept pending and new online /tpa requests to you":"Own incoming teleport requests";
                case "mob_spawning","phantom_spawning" -> "Natural spawns in your chunk and its eight neighbors";
                case "night_vision" -> "Infinite Night Vision while enabled";
                case "item_price" -> supported?"Show the whole stack value in /sell":"Inventory display integration pending";
                case "scoreboard" -> supported?"Show or hide your entire TAB scoreboard":"Requires TAB scoreboard; player data must be loaded";
                default -> supported?"Click to change your preference":"Awaiting agreed behavior or integration";
            };
            ItemStack item=icon(icons[i],names[i],detail); var meta=item.getItemMeta();
            if(key.equals("night_vision")) ((PotionMeta)meta).setBasePotionType(PotionType.NIGHT_VISION);
            Component status=value==null?Component.text("Unset",NamedTextColor.GRAY):Component.text(value?"ON":"OFF",value?NamedTextColor.GREEN:NamedTextColor.RED);
            meta.displayName(text(names[i]+" ").append(status)); item.setItemMeta(meta);
            button(menu,slots[i],item,supported?()-> { p.changePreference(player,key); settings(player); }:null);
        }
        boolean instant=p.preferences.enabled(player.getUniqueId(),"instant_rtp");
        ItemStack instantIcon=icon(Material.ENDER_PEARL,"Instant RTP"); var im=instantIcon.getItemMeta(); im.displayName(text("Instant RTP ").append(Component.text(instant?"ON":"OFF",instant?NamedTextColor.GREEN:NamedTextColor.RED))); instantIcon.setItemMeta(im);
        button(menu,39,instantIcon,()-> { p.changePreference(player,"instant_rtp"); settings(player); });
        player.openInventory(menu.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player) || !menu.owner.equals(player.getUniqueId()) || menu.busy) return;
        if(!Set.of(ClickType.LEFT,ClickType.RIGHT,ClickType.SHIFT_LEFT,ClickType.SHIFT_RIGHT).contains(event.getClick())) return;
        int rawSlot=event.getRawSlot(),playerSlot=event.getSlot();
        Action action=menu.actions.get(rawSlot);
        boolean deposit=menu.kind.equals("SELL") && event.getClickedInventory()==player.getInventory();
        ItemStack clicked=event.getCurrentItem();
        ItemStack snapshot=clicked==null?null:clicked.clone();
        // Paper requires opening/closing inventories outside the click event's transaction.
        p.getServer().getScheduler().runTask(p,()-> {
            if(!player.isOnline() || player.getOpenInventory().getTopInventory()!=menu.inventory || menu.busy) return;
            p.attempt(player,()-> {
                if(rawSlot>=0 && rawSlot<menu.inventory.getSize()) {
                    if(action!=null) action.run();
                } else if(deposit && snapshot!=null && !snapshot.getType().isAir()) {
                    if(sellItems(player).size()>=53) throw new IllegalArgumentException("Sell is full.");
                    p.items.take(player,playerSlot,snapshot,"SELL"); fillSell(player,menu);
                }
            });
        });
    }
    @EventHandler public void drag(InventoryDragEvent event) { if(event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true); }
    @EventHandler public void close(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof Menu menu && menu.kind.equals("SELL") && event.getPlayer() instanceof Player player)
            p.attempt(player,()->p.items.returnSell(player));
    }
}
