# 0.6.0-test update — 18 October 2026

This update follows UPDATE_SPEC_2026_10_18.txt. It is a test build, not a claim of full production or Bedrock verification.

## Implemented

- Empty `/sell` pricing automatically imports the provided EconomyShopGUI table; existing nonempty pricing is preserved. GUI totals use those prices. Stack quantities multiply the unit price. Metadata protection remains in place.
- Auction browse has five listing rows and the requested filter/previous/Your Items/next controls; title shows its actual clamped page. Empty listing cells stay empty. Sort cycles Default, Lowest, Highest, Recently Listed.
- `/ah sell <price>` opens a three-row Confirm Listing (Cancel11, stack13, Confirm15); whole main-hand stack is only removed on confirmation, checked against the original snapshot. Closing changes nothing. Confirm Purchase uses the specified three-row positions. No expiry or Time Left.
- Your Items displays up to54 active listings with red empty panes; sold listings disappear. Click withdrawal stays undecided; existing `/ah cancel <id>` works.
- Main navigation adds Your Items51 and Quick Search52; Pay has the same main navigation. Search in Pay filters online visible players. Payment sign says Amount; submission executes once, through the existing transaction journal, without a confirmation menu. Invalid/blank input sends nothing.
- Notifications hides plugin notices, including trade/pay/TPA feedback; Global Chat and Death Messages filter recipients independently. Admin console diagnostics remain visible. Natural mob/phantom controls retain the current-chunk-and-eight-neighbors policy.
- Own `/tpa`, `/tpahere`, `/tpaccept [player]`, `/tpdeny [player]`, `/tpacancel`, `/tpatoggle`:30-second sender cooldown,60-second timeout, clickable acceptance/denial. Request notifications are separate from request permission. Auto TPA accepts pending/future online incoming `/tpa`, never `/tpahere`.
- `/rtp`: bounded async chunk search within20,000 blocks of world spawn and inside the border; requires dry empty feet/head spaces and a nonhazardous solid floor. Instant RTP setting at39 skips the5-second stand-still delay. New players and deaths without a valid bed/anchor receive immediate random searches; later joins retain position. Cancelled searches do not force fallback teleports.
- Internal number formatter supports K/M/B/T/Q/P/E/Z/Y, one decimal for scaled values. Built-in `%formatter_number_shorten_...%` expansion uses PlaceholderAPI if installed; an existing formatter registration is preserved.
- Built-in maintenance toggle/reason/UUID whitelist, online kick and login blocking, server-list MOTD/count/hover. The provided maintenance config is included, malformed gradient corrected. Timers are not implemented because they remain an open decision.
- Bundled, relocated PacketEvents2.14.0 filters TNT/crystal explosion animation per viewer while retaining sound/knockback; no ProtocolLib or separate PacketEvents JAR required. Live verification is still needed.
- Optional own TAB implementation, `own-tab.enabled: false` by default: supplied animation frames, per-line sidebar settings, per-viewer rank/money name tags, Vault-backed LuckPerms prefix/suffix/group reads, ping objective and group/name sorting. Refuses to render while TAB is enabled. Missing original lunarTitle/discordSweep animations remain a parity question; this prototype uses supplied lunarServer/lunarDiscord frames. No world setting or scoreboard world line is added.
- Existing TAB users can apply docs/TAB_CONFIG_WITH_LINE_SETTINGS.yml; original line templates are imported into Lunar config. Keep PAPI and the original TAB until tested. No TAB configuration is changed automatically outside this plugin.

## Price table compatibility findings

The original table is preserved unedited in docs/SELL_PRICES_FROM_ECONOMYSHOPGUI.yml. Runtime resource maps legacy names CHAIN->IRON_CHAIN, DISC_FRAGMENT->DISC_FRAGMENT_5, SCUTE->TURTLE_SCUTE. Duplicate legacy FIREWORK($75) is omitted; explicit FIREWORK_ROCKET($1.37) is preserved. Thus1238 unique modern entries are shipped, with1237 activated under the prior cap. VAULT($30,000) is disabled pending the owner's decision about the earlier$10,000 unit cap. Variant-priced enchanted books/potions/tipped arrows remain excluded. Named/enchanted/damaged/filled metadata stacks retain their existing protection; this update does not choose the unresolved special-item payout policy.

## Required decisions and remaining work

- Auction listing limit and fee remain null. The confirmation GUI opens, but creation/purchase stays blocked until the owner sets `auction.max-listings-per-player` and `auction.fee-percent`. No fee/limit is silently chosen.
- Inventory hover price tooltip remains unavailable. PacketEvents' HashedStack conversion uses placeholder zero hashes; a verified26.3 prediction-hash adapter is required. No persistent item lore is written and no unverified reconciliation is active.
- Skin replacement is unimplemented pending online/offline and Floodgate identity/skin details. Keep SkinsRestorer.
- Maintenance timers, full legacy TAB command compatibility, exact missing header/footer animations and Bedrock head fallback are still open. Keep PAPI; no general PAPI replacement was approved.
- Your Items pagination beyond54 needs a layout decision.
- Real Paper/provider/PAPI/TAB/Geyser/Floodgate/Meta Quest tests and a2.5GB load test have not run. Packet filtering, random spawning and optional TAB rendering are compiled implementations, not live-verified behavior.

## Installation

Stop a test Falix server; back up worlds, economy and plugins/LunarVoidSMP including lunar.db. Use Java25 and Paper26.3. Replace only the old LunarVoidSMP JAR, preserving its data folder. Keep Vault and Essentials economy, PAPI, LuckPerms, TAB (own TAB off), Geyser/Floodgate and SkinsRestorer. Remove the obsolete shop plugins as previously planned. Disable/remove SimpleTPA/BetterRTP before testing the new equivalents. Do not remove TAB or SkinsRestorer on the assumption parity has been proven. Use a normal restart, never /reload. Run `lunar status` in console and inspect startup errors; supply logs if it fails.

Owner/manager permissions: grant `lunar.admin` to smp_owner and smp_manager using LuckPerms. Default is op. No other group is granted that node by this plugin.
