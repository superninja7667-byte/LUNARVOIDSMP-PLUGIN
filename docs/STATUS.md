# Confirmed scope and remaining decisions

Coding was explicitly approved by the owner after the handoff documents. EconomyShopGUI, zAuctionHouse and zMenu are removed from the dependency plan; all shop/auction/menu logic is original custom code. The historical requirements are preserved in PROJECT_REQUIREMENTS.txt, with later owner messages taking precedence.

## Confirmed corrections since that document

- `/shop` is the public player auction shop. A seller holds the item stack and uses `/ah sell <price>`; the price is supplied by that seller for the whole stack, with immediate listing and no confirmation screen, per the owner's subsequent answer. Preset material prices are not needed for public auction listings. The supplied main/category layout remains the entry to this auction shop.
- No Instant RTL. Instant RTP remains unrequested/undefined and is not included.
- Emerald Block purchase button reads **Confirm** and shows the selected total.
- Settings displayed names are Notifications, Global Chat, Death Messages, Money Tag, Money, Discord Link, Online, Rank Tag, Ping, TPA Request, TNT Particles, End Crystal Particles, Auto TPA, Phantom Spawning, Mob Spawning, Server TPS, Item Price, Scoreboard, Night Vision. No displayed name includes Toggle. Rank Tag, Item Price and Night Vision start OFF. Other defaults are unset.
- Edit Shop is removed.
- Main and auction inventory titles are **Shop**. Listing confirmation has been removed by explicit owner choice; selling is **Sell**; settings is **Settings**.
- Cauldron sorting options: **Most Recent**, **Highest Price**, **Lowest Price**.
- Previous page uses Slime Ball; Next page uses Magma Cream (owner called it Magma ball).
- Server reported by owner: Falix Paper 26.3.2651 (MC 26.3), 2.5 GB RAM. Exact installed build/plugin versions and Java runtime remain unverified. Maven targets official Paper API 26.3 build 205; future versions untested.
- Planned retained integrations: Vault economy (normally Essentials), PlaceholderAPI/TAB, LuckPerms, Essentials, BetterRTP, Geyser/Floodgate. Not every integration is implemented; see README.

## Screenshot-derived slots (zero based, nine columns)

Rows count only the top menu, not the player inventory.

| Menu | Positions |
|---|---|
| Main Shop, 54 slots | Diamond 13; Sword 21; replacement Cobblestone 22; Enchanted Golden Apple 23; Enchanting Table 31; Player Head 45. Confirmed bottom items: Hopper **Shop** 48 (all auction listings); Sign **Search** 49; wooden block **Settings** 50; Barrel **Pay** 53. |
| Purchase, 54 slots | Red panes 19/20/21; selected item 22; green panes 23/24/25; Emerald Block 31; Nether Star 49. Exact step-to-pane mapping awaits confirmation. |
| Settings, 54 slots | First group 11–15; second 20–24; third 29–33; Server TPS 38; Item Price 40; Scoreboard 41. Slot 39 stays empty because its historical Instant RTL interpretation was explicitly rejected. Edit Shop/Anvil is removed; the later requested Night Vision potion now uses free slot 42. |
| Historical Confirm Listing screenshot | Superseded: owner chose immediate `/ah sell <price>` listing; this menu is no longer opened. |
| Sell, 54 slots | Item input/display 0–52; green confirmation 53. |

Settings screenshots depict Notifications as a book while the handoff describes a purple enchanted-looking icon. This build uses Enchanted Book at the observed position as a provisional interpretation; exact material still needs confirmation. Earlier text says Light Block for Auto TPA; that is preserved despite the screenshot resembling a sea lantern. Navigation icon materials and these discrepancies must be approved before calling visuals final.

## Proposed category classification, pending review

The current implementation uses material/block/food properties, common equipment material names, and actual enchantment metadata. Blocks may overlap Food or Enchanting, e.g. an edible placeable item or enchanting table. Enchanting includes enchanted stacks/books and vanilla enchanting supplies. Materials covers non-block, non-food, non-equipment, non-enchanted items. This classifier is implemented for testing; it is not a finalized catalogue or owner-approved taxonomy.

## Unfinished work retained in scope

Replacement implementation and live validation of the inventory sell-price adapter, per-viewer Money/Rank tags, death-message filtering, general notification scope, particle controls, real-provider validation of spawning/TPA/scoreboard controls, RTP coordinate-message scope/configuration, own-skin inventory heads on Bedrock, final auction-control/Pay-confirmation layouts, complete pricing (including metadata rules), auction fees/limits, no-match/back/cancellation behavior and exact defaults.

Do not silently enable a placeholder setting, invent final prices, modify an existing scoreboard layout, or claim all requirements are complete. The unavailable controls identify pending work rather than discarding it.

## Owner decisions implemented in 0.2.0

- "We don't need listing" was clarified through the question card as **remove listing confirmation**, not remove public listings or their limit. `/ah sell <price>` now immediately lists the entire held stack. `/sell` and buying/Pay confirmations retain their own behavior. The subsequent no-expiration decision supersedes listing duration; fee and listing limit remain undecided.
- TPA Request means **incoming request notifications**, not permission to send or receive requests. OFF suppresses supported Essentials request prompts using optional compatible ProtocolLib, without rejecting pending requests.
- Auto TPA ON accepts **existing and new** incoming `/tpa` requests from online players, through Essentials' acceptance command. Opposite-direction `/tpahere` requests are excluded. No automatic teleport is enabled by an unset preference.
- Spawning OFF protects **the player's current chunk and eight neighboring chunks**, moving with the player, for **natural spawns only**. Initial natural chunk population is included. No existing mobs are removed and no claimed-land system is inferred. Mob Spawning includes friendly mobs and phantoms; Phantom Spawning only affects phantoms. Overlapping OFF preferences win.
- Scoreboard controls the **entire existing TAB sidebar** for that player using TAB's API, preserving its content and other scoreboard features.
- No new default ON/OFF choices have been invented; previously unset preferences remain unset until configured or changed.

## Owner decisions implemented in 0.3.0

- Auction listings have **no time limit** and remain in the shop until purchased or explicitly withdrawn. Duration config, expiry checks and expiration jobs are removed. Existing ACTIVE records ignore legacy deadlines, including after restart. Already expired/returned items are not resurrected.
- Successful purchases remove the listing from public browsing and put the purchased whole stack directly in the buyer's inventory. Existing open listing views lose the sold icon and competing buying confirmations close.
- Buyer receives a chat purchase confirmation; an online seller receives a chat sale notification. Core and database guards plus the GUI reject seller self-purchases and duplicate sales.
- For full inventories, the owner chose **require space first**: no money is taken and the listing remains active. The inventory check includes returned cursor contents and the purchased item is delivered before older claims. Exceptional external inventory changes or interrupted delivery retain the journal safety fallback; real server verification is still required.
- Auction listing limit, fee and buying-layout decisions remain outstanding. No replacement duration or timeout has been invented.

## Owner clarification — fixed /sell payouts and Item Price (0.4.0)

- `/sell` is the separate server payout, unrelated to public `/shop` auction asking prices. Each item type needs a fixed price, with rarer items worth more. Values are not yet approved; `SELL_PRICE_PROPOSAL.md` is a sample scale for owner review, not active or complete pricing.
- Maximum **unit** payout $10,000; multiply by quantity for stacks. A stack can pay more than $10,000. No new auction asking-price cap was introduced.
- Item Price shows the **whole stack** payout when selecting/hovering inventory items. Default OFF, independent from sell availability. Saved explicit choices remain persistent.
- `/sell` still pays only after its bottom-right confirmation button is clicked; closing it sells nothing and returns deposits. That behavior was already implemented.
- Added optional ProtocolLib display-copy adapter with source-lore preservation, player-slot filtering, cursor updates, refresh on toggle/shutdown, creative exclusion and failure disable/refresh. Java/Geyser inventory-prediction behavior and Quest presentation remain untested.
- Special-item eligibility question remains pending. Existing metadata restrictions stay in effect until the owner chooses whether contents/enchantments/damage/names affect eligibility or value. Unpriced/ineligible items explicitly show `Not sellable in /sell` and remain returnable.

## Owner navigation clarification implemented in 0.4.1

- Main menu hopper is named **Shop**, opens public auction listings; sign **Search** searches those listings; wooden block **Settings** opens Settings; barrel **Pay** opens online-player gifting. Screenshot slots: 48, 49, 50, 53 respectively.
- `/settings` already works independently; added `/pay` for direct player selection without first opening Shop. Bare `/pay` routes to LunarVoidSMP even if Essentials owns that command, respecting earlier cancellations. Namespaced or parameterized Essentials commands remain separate. `/lunarvoidsmp:pay` is an explicit custom entry point.
- Gifting spends the sender's Vault account, using the existing sign amount and final confirmation flow. No new amount rules or payment confirmation design was invented. The selection menu now opens before that layout is configured; the final confirmation still blocks transfers until configured.
- Existing configs with absent/-1 navigation positions are upgraded to the confirmed values; explicit custom positions are preserved. Remaining fee/limit/price/confirmation decisions stay gated.

## Owner addition implemented in 0.4.2 — Night Vision

- New Settings entry **Night Vision**, using `PotionType.NIGHT_VISION` on a potion icon. Placed in free slot 42 beside Scoreboard; slot39 remains empty and no other control moves.
- Default **OFF** for new/unset players. ON supplies infinite level-I Night Vision; OFF removes this setting's effect. Persistent preference key `night_vision` is appended without changing existing keys or menu mappings.
- Reapply after join/respawn/milk while ON, plus the existing once-per-second settings scan. Effects use persistent ownership marking; cleanup on logout/shutdown, recovery from a saved ownership marker and respect for existing/external Night Vision and cancelled effect operations.
- No added dependency. Live Paper/Geyser/Quest icon rendering, effect events and cross-plugin interactions remain unverified.

## Latest owner constraint — no ProtocolLib; final JAR only when ready

This supersedes the earlier ProtocolLib adapter notes above. ProtocolLib is excluded from the server setup and has been removed from the current source dependencies/plugin metadata. Its two adapters were removed; interfaces and tested valuation/prompt matching remain to support their replacements. Item Price and TPA Request message visibility are currently unavailable pending replacement, and neither requirement has been dropped. PlaceholderAPI remains integrated for supported text/scoreboard values; it cannot itself implement packet interception.

No new JAR is to be packaged or delivered until the complete plugin is ready. Prior versioned Outputs remain historical incomplete development builds. Current source version is `0.5.0-SNAPSHOT`. Use compilation/tests without the Maven package phase during this source work. Readiness also requires the undecided GUI/economy rules, unfinished settings and integrations, actual provider configuration and live Paper/Geyser/Floodgate/Quest validation.

Candidate research: official PacketEvents v2.14.0 release explicitly lists Minecraft26.3 support. Bundling a suitable packet library inside the plugin may avoid an additional server plugin. No candidate has been added or claimed compatible; its lifecycle, conversion safety, inventory prediction and Geyser behavior must be checked before implementing the replacement.

## Built-in custom PlaceholderAPI expansion requested and implemented in source

- Internal identifier `lunar`; connected to installed PlaceholderAPI, no separate Lunar expansion download or second PlaceholderAPI implementation.
- Added cached name/UUID, Vault balance (numeric/formatted/short), ping, online count, TPS and whole main-hand stack `/sell` value/eligibility. All19 settings and six original TAB line wrappers remain exposed. Full list: `PLACEHOLDERS.md`.
- Values are sampled synchronously on the server thread about once per second and consumed as immutable snapshots; provider failures remain blank rather than false zero balances. Offline balances/inventories are not queried. Amounts shown by placeholders never authorize transfers.
- Registration success/collision, optional absence, PAPI enable/disable, normal PAPI reload persistence, shutdown cleanup and status diagnostics are handled. No ProtocolLib introduced.
- Compatible PAPI-consuming fields can use the placeholders; universal plugin compatibility, packet interception, viewer-specific tags and live provider/client validation are not implied. Final JAR remains deferred until full readiness.

## Latest delivery instruction — fresh full checks and JAR immediately

The owner explicitly requested running all available tests and supplying a JAR now, citing a19-minute chat limit. This supersedes the earlier no-JAR-until-ready instruction for the current delivery. Package current source as **0.5.0-test**, preserving all existing requirement/configuration guards and documenting unfinished features. Do not claim the plugin is complete or runtime-tested. Preserve source/specification/screenshots in a recovery archive for continuity.

## Latest — 0.6.0-test

The owner's UPDATE_SPEC_2026_10_18.txt supersedes older conflicting decisions. See UPDATE_0.6.0.md for implemented changes, table compatibility fixes, open decisions and remaining integrations. Pay uses sign submission immediately. Listing and purchase now use three-row confirmations. Own TPA/RTP and optional own TAB are compiled implementations. Tooltip/skins/timers and live compatibility remain incomplete.

## Latest — 0.6.1-test

Owner explicitly approved a $30,000 per-item sell cap. VAULT at $30,000 is now enabled, including migration of its missing price from the populated 0.6.0 configuration. Explicit custom prices are preserved. Stack totals can exceed the unit cap. Other open decisions and unfinished features remain as documented in UPDATE_0.6.0.md; no auction limit or fee was supplied.
