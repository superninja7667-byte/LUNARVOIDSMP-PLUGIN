# Latest: 0.6.1-test — approved $30,000 per-item sell cap

The VAULT sell price is enabled at $30,000. Upgrades automatically add its missing price while preserving explicit custom prices. See docs/UPDATE_0.6.1.md (UPDATE_0.6.1.md from this folder).

# Current update: 0.6.0-test

Read [the current update and limitations](docs/UPDATE_0.6.0.md) first (from docs/, use UPDATE_0.6.0.md). It supersedes older Pay/listing/price and integration descriptions below. Owner update and reference files are preserved in this backup.

# LUNARVOIDSMP — unreleased source work

Custom Paper plugin built from the owner's specification and screenshots. **This is an incomplete development build for testing, not the finished server plugin.** EconomyShopGUI, zAuctionHouse and zMenu are not dependencies. No third-party plugin source was copied.

## Current delivery — owner requested a JAR now

The owner has now asked for a fresh full test run and a downloadable JAR immediately. This supersedes the earlier instruction to defer packaging until full readiness. **Version0.5.0-test is an incomplete test build**, not a claim that every requested feature is finished or live-tested.

Download `LunarVoidSMP-0.5.0-test.jar` and the source/recovery ZIP from this task's Outputs. The JAR includes SQLite and Lunar's internal PlaceholderAPI expansion; it does not require ProtocolLib. Keep Vault plus a working economy provider (usually EssentialsX), PlaceholderAPI, TAB and the other retained integrations. Use Java25 and Paper26.3. Stop a test server, back up worlds/inventories/economy/plugin data, replace older LunarVoidSMP JARs with this single JAR, preserve `plugins/LunarVoidSMP/`, then start normally without `/reload` or hot-loading. Run `lunar status` from the Falix console.

**Important limitations:** auction listing fee/limit and buying-confirmation layout remain unset, the approved `/sell` price table is empty, Pay confirmation layout is undecided, and inventory tooltips/TPA notification filtering still await replacement adapters. Several other settings remain unfinished. Their guards remain in place; packaging does not activate unapproved prices, rules or layouts. Night Vision and built-in PAPI values are implemented in source. See `docs/STATUS.md`, `docs/PLACEHOLDERS.md` and `docs/TESTING.md` for exact coverage and remaining work.

No live Paper/Geyser/Floodgate/Quest, provider or Falix RAM test has been run. The exact reported Falix build `26.3.2651` is unverified. Compilation targets official Paper API26.3.build.205-stable and Java25. Unit tests cannot establish full runtime compatibility.

## Build the JAR yourself

Install **JDK 25** and **Maven 3.9+**, extract the source ZIP, and run in the directory containing `pom.xml`:

```sh
mvn clean verify
```

When final packaging is appropriate, the result is `target/lunarvoidsmp-0.5.0-test.jar`. Use that shaded JAR, not `original-lunarvoidsmp-0.5.0-test.jar`. The shaded JAR includes SQLite; it does not bundle Paper, Vault, PlaceholderAPI, EssentialsX or TAB.

The optional manual GitHub Actions workflow can build a development JAR through GitHub's website, but has not been run as part of the current source-only work: publish these project files to your repository, open **Actions → Build plugin → Run workflow**, and download the `LunarVoidSMP` artifact after it succeeds. Repository publication is a separate user action; this task has not pushed or opened a PR.

## Implemented in this build

- `/shop` is the public player auction shop, using the supplied main/category layout. Players supply its listings by holding the stack in their main hand and running `/ah sell <price>`. The command lists immediately, with no listing confirmation screen, as subsequently requested. The entered price is the total for that whole stack. No item name or preset material price is required for an auction listing. Listings have no time limit. The listing limit and fee still need configuring in this development build.
- `Shop` main/category menu; category shortcuts filter auctions, not stock shops. Cobblestone replaces the disc; enchanted golden apple and enchanting table use the agreed icons.
- Stock buying menu titled `Shop`, selected item in slot 22, Emerald Block **Confirm** in slot 31 with total, Nether Star **Back** in slot 49. Quantity changes use the approved step configuration when supplied.
- Auction browsing, category filters, recent/highest/lowest sorting, partial and typo matching, persistent listings without expiration, seller cancellation and stored claims. Category classification is documented in `docs/STATUS.md` and needs owner review.
- `/ah sell <price>` immediately lists the entire main-hand stack. Empty hands and invalid amounts are rejected. The original metadata is preserved. Listing rule/provider failures keep or return the item, with durable claims if space is unavailable. Listing confirmation was explicitly removed; Sell, Pay and purchase confirmation are separate flows.
- Auction purchasing with buyer debit, seller payout, configured fee, unique listing reservation and direct delivery of the purchased stack. The buyer must have room for the whole stack before money moves. Completed listings are removed from other open auction menus, and competing purchase confirmations close. Sellers cannot buy their own items. Buyer and online seller receive chat messages after purchase. The buying confirmation layout is gated until approved.
- `/sell` titled `Sell`, 54 slots, payout button at bottom-right slot 53. Click/shift-click a player inventory stack to deposit it; click its displayed stack to take it back. Confirm pays the recalculated total for eligible items. Closing sells nothing and returns items or retains them as claims if there is insufficient space. Drag/hotbar swap/double-click operations are blocked to protect escrow.
- Online-player Pay selection, sign amount input, final confirmation, and Vault transfer/refund logic, Player selection opens from the barrel, `/pay` or `/lunar pay`; payment stays blocked until the confirmation layout is supplied/configured.
- Client-only sign input for Search and Pay. Nothing is placed in the world. Cancellation, timeout, logout and teleport cleanup are included. Actual Geyser/Quest behavior is untested.
- Persistent personal preferences and `Settings` menu with green **ON**, red **OFF**, and explicit **Unset** state. Rank Tag, Item Price and Night Vision default OFF; other defaults are not invented. Edit Shop and Instant RTL are absent.
- **Night Vision** setting uses a real Night Vision potion icon in Settings slot 42, OFF by default. ON supplies infinite Night Vision; OFF removes the effect managed by this setting. The choice persists across reconnects/restarts, and ON reapplies after respawn or milk removal.
- Global Chat preference filters recipients of Paper's normal chat event without muting the sender for other players. Separately injected Discord/plugin messages are not intercepted.
- Built-in PlaceholderAPI expansion for balance, player identity, ping, online/TPS, whole held-stack `/sell` value and all personal settings, plus selected scoreboard line wrappers preserving the supplied templates. Existing TAB files are never overwritten.
- Whole-stack fixed `/sell` valuation and display-copy projection code are retained. Actual inventory Item Price display awaits a replacement adapter; OFF by default, and separate from auction asking prices.
- TPA request message visibility, incoming Auto TPA, natural spawning controls for the current chunk and eight neighbors, and full TAB scoreboard visibility, as detailed below.
- SQLite transaction/item journal, cent-based amount validation, repeat-confirmation protection, durable claims, and audited operator reconciliation of interrupted effects.

## Decisions still required in config.yml

The empty values deliberately prevent unfinished flows from charging money. They are not final economic defaults.

| Configuration | Needed decision |
|---|---|
| `prices` | Final material buy/sell prices and sellable list. Amounts should be quoted decimal strings, e.g. an approved amount with at most two decimal places. Per-item sell prices are capped at $30,000; stack totals may exceed this. No live prices are supplied. See `docs/SELL_PRICE_PROPOSAL.md` for a draft scale awaiting review. |
| `auction.max-listings-per-player` | Positive whole listing limit |
| `auction.fee-percent` | Approved percentage from 0 inclusive to 100 exclusive; charged on successful sale and removed from the economy; fractional cents round down |
| `auction.buy-confirmation-layout` | Set to `purchase` only if the owner approves reusing the supplied buying layout for a **whole auction stack**. Otherwise auction purchases remain disabled. |
| `menus.navigation` | Confirmed: Hopper **Shop** 48, Sign **Search** 49, wooden block **Settings** 50, Barrel **Pay** 53 (zero-based). Missing/-1 values in older configs upgrade automatically; explicit custom positions are preserved. |
| `menus.quantity-steps` | Assign the six slots (19,20,21 red; 23,24,25 green) their approved -1/-5/-10 or +1/+5/+10 steps |
| `menus.auction-controls` | Exact Cauldron, Previous (slime ball), Next (magma cream) slots from a supplied auction screenshot |
| `menus.pay-confirmation` | Approve three distinct positions in the 27-slot Pay confirmation: cancel, item and confirm |
| `settings.defaults` | Remaining approved defaults and outstanding behavior decisions |
| `tab.lines` | Actual existing scoreboard lines; send TAB config and animations before making changes |

Stock buying/selling currently accepts **plain vanilla material stacks only**. Named, damaged, enchanted or otherwise modified stacks are not valued by the stock price table. They remain eligible for auctions, where the original serialized metadata is preserved. Unsellable deposits in Sell are returned rather than discarded. Technical amount input is 1–12 digits plus up to two decimal places; stock quantity is bounded to 36 maximum-size stacks. These are implementation bounds, not approved economic prices or policy limits.

## Permanent auction listings (0.4.2)

Items listed with `/ah sell <price>` have **no expiration time**. They stay in the public auction shop until purchased or explicitly withdrawn by their seller through the existing cancel command. There is no duration setting or scheduled auction expiration job. Old `auction.duration-seconds` values in an existing config are ignored.

A successful purchase removes the listing and immediately puts that specific stack into the buyer's inventory, preserving its metadata and quantity. The buyer receives an item/price confirmation in chat; an online seller receives the buyer/item/payout message. Offline sellers receive the Vault payout through the configured provider; an offline chat inbox has not been implemented. Your own listings cannot be bought, and two buyers cannot purchase the same listing.

The buyer must first have enough inventory space for the **whole stack**. A normal full-inventory attempt takes no money and leaves the item for sale. Space is checked after the confirmation inventory closes so returned cursor items are included. Delivery occurs before unrelated stored claims. If another plugin changes the inventory during a Vault operation, or the server interrupts delivery, the journal retains the item as a claim or REVIEW record rather than losing it; unit tests cannot establish crash-atomic money/inventory saves.

Existing **ACTIVE** listings from older LunarVoidSMP builds remain active regardless of their old deadline, including across restart. The legacy database `expires` column remains for file compatibility and is ignored. Records already expired or returned by an older build remain returned/claimable and are not relisted; sold/cancelled records are not revived. Preserve the database while replacing the old JAR.

## TAB / PlaceholderAPI / LuckPerms

Keep your existing scoreboard's order, text, colors, animations and expansions. Do not replace the whole scoreboard with a generated one.

After its configuration is reviewed, put each **original complete line** into the matching `tab.lines` key, and use one wrapper in TAB for that line:

| Key in `tab.lines` | Wrapper in TAB |
|---|---|
| `money` | `%lunar_line_money%` |
| `discord_link` | `%lunar_line_discord_link%` |
| `online` | `%lunar_line_online%` |
| `rank_tag` | `%lunar_line_rank_tag%` |
| `ping` | `%lunar_line_ping%` |
| `server_tps` | `%lunar_line_server_tps%` |

TAB hides a line that consists solely of a placeholder returning an empty value. Keep static text inside the configured template, not around the wrapper. Nested existing PlaceholderAPI expressions are evaluated; missing expansions still need installing. `%lunar_setting_<key>%` returns `true`, `false` or `unset`. The expansion persists through PAPI reloads.

Global Chat and the two spawning controls are actionable without extra integrations. Auto TPA needs compatible EssentialsX; TPA Request message visibility awaits its replacement message adapter; Scoreboard needs TAB with its scoreboard feature enabled and player data loaded. Money, Discord Link, Online, Ping and Server TPS become actionable when their line template and TAB/PAPI are present. Rank Tag remains OFF by default; its scoreboard wrapper honors that preference, but toggling it remains unavailable until viewer-specific rank/name display is implemented. Merely listing TAB/LuckPerms as soft dependencies does not implement viewer-specific tag changes.

## Clarified Settings behavior (0.4.2)

- **TPA Request** controls incoming Essentials `/tpa`, `/tpahere` and player-sent `/tpaall` request prompts. OFF hides the request and accept/deny/timeout instructions, while the request remains pending and can still be accepted manually. It does not hide acceptance results, errors, player chat or every teleport-related message. General Notifications is a separate, unfinished setting.
- **Auto TPA** ON accepts pending online incoming `/tpa` requests and future ones while enabled. It never accepts `/tpahere`, whose direction is reversed. Acceptance uses `essentials:tpaccept <player>` as the recipient, keeping Essentials permissions, costs, expiry, warmups and teleport events. New requests are checked next tick; an additional one-second scan handles pending requests and restored preferences. Failed attempts are not automatically repeated. OFF stops automatic acceptance; an already accepted teleport follows Essentials' own cancellation rules. Unset does not enable auto acceptance or change Essentials. An explicit Lunar Auto TPA choice disables Essentials' broader native auto mode to avoid accepting the opposite direction; use Lunar's setting rather than both auto controls.
- **Mob Spawning** OFF blocks new natural mobs, including friendly mobs and phantoms, in the player's current chunk plus its eight neighbors (a moving 3×3 chunk area). **Phantom Spawning** OFF blocks only natural phantoms in that area. The spawning position determines coverage, in the same world and at any height. Natural and initial chunk-population spawn reasons are covered; spawners, breeding, spawn eggs, commands and custom plugin spawns are unaffected. Existing mobs remain. Any online player's OFF preference blocks spawning in an overlapping protected area, including for other players. These are current-location chunks, not claimed land or every chunk the server has loaded.
- **Scoreboard** hides or shows that player's entire existing **TAB sidebar**, retaining its title, lines and animations. It does not replace the Bukkit scoreboard, remove nametags or alter other players' sidebars. Explicit choices are reapplied after reconnects/restarts and TAB reloads through its visibility API; Unset leaves TAB's choice alone. The five per-line settings still need original templates as described above.

### TPA message visibility replacement

ProtocolLib has been removed at the owner's request. Incoming TPA request notification filtering remains required, but its control is currently unavailable pending a replacement adapter. Incoming requests remain functional; hiding messages must never reject requests or auto-accept them. Existing exact/localized prompt matching and the adapter boundary remain available for the replacement implementation. PlaceholderAPI does not provide a general outgoing-message interception API.

Compilation uses EssentialsX **2.21.2** and TAB API **6.1.2** as provided APIs. Actual installed versions and runtime compatibility remain to be checked.

## Important unfinished features

- **Item Price inventory tooltips** await the replacement inventory display adapter. The valuation/projection code is retained; exact prices, special-item rules, inventory prediction and Geyser/Quest behavior still need completion or verification.
- **Money Tag and viewer-specific Rank Tag** are not implemented. Normal nametags are not hidden as a substitute for removing rank prefixes.
- **Death Messages**, general Notifications, and TNT/End Crystal particles remain unavailable pending implementation. They are preserved in Settings with an explanatory description.
- BetterRTP/Essentials retain their existing RTP/TPA commands, protections and cooldowns. Only the explicitly supported TPA request prompts are filtered when requested. Coordinate-free RTP success messaging still needs the actual BetterRTP language file and scope decision.
- Bedrock inventory heads showing the player's own skin, sign input, clicks and tooltip appearance have not been tested. Geyser may require custom-head registration/resource packs; no guarantee is made.
- No auction data migration, Bedrock resource pack, production installation, Falix RAM/load test or full GUI/client test has occurred.

See `docs/STATUS.md` and `docs/PROJECT_REQUIREMENTS.txt` for the preserved plan and remaining work. Unavailable settings are not functional ON/OFF controls.

## Commands and existing command conflicts

| Command | Purpose |
|---|---|
| `/shop` | Main Shop menu |
| `/sell` | Confirm-only Sell GUI |
| `/settings` | Settings GUI, accessible independently of Shop |
| `/pay` | Online player selection for gifting money from your balance |
| `/ah` | Auction listings titled Shop |
| `/ah sell <price>` | Immediately list the whole main-hand stack at the entered price |
| `/ah mine` | Active listing IDs |
| `/ah cancel <full-id>` | Cancel your active listing |
| `/ah claim` or `/lunar claim` | Claim stored items; full inventory retains them |
| `/ah sort recent\|highest\|lowest` | Sort listings while button placement is pending |
| `/ah page <number>` | Browse an auction page |
| `/lunar catalog [page]` | Configured server stock items |
| `/lunar search` | Same sign search as the main Shop sign |
| `/lunar pay [page]` | Online player selection; final transfer requires the configured confirmation layout |
| `/lunar status` | Admin integration/configuration status |
| `/lunar review` | Admin interrupted item/transaction IDs |

The main menu uses the confirmed screenshot positions: hopper **Shop** (48) opens all public auction listings; sign **Search** (49) searches listings; wooden block **Settings** (50) opens Settings; barrel **Pay** (53) opens online player selection. `/settings` and `/pay` work without opening Shop first.

Bare `/pay` (including case variations and trailing spaces) is routed to LunarVoidSMP even when Essentials owns that command in Bukkit. Earlier command-event cancellations are respected. Namespaced Essentials commands and parameterized Essentials payments are not intercepted; `/lunarvoidsmp:pay` explicitly opens the custom flow. The custom flow uses the selected sender's Vault balance and requires amount input and final confirmation. Confirmation layout is still unconfigured, so no transfer can run until it is supplied.

Namespaced commands such as `/lunarvoidsmp:sell` bypass another plugin's command ownership when diagnosing conflicts. If normal `/sell` still invokes Essentials, stop and resolve aliases/ownership deliberately; do not let both sell systems process an inventory.

Player commands do not introduce paid ranks or economic permissions. The technical administrator permission is `lunar.admin`, defaulting to server operators.

## Crash recovery and transaction limits

Vault and Minecraft inventory saves do not participate in a shared database transaction. A durable journal records an intended effect **before** the effect happens. If the server stops between an external money/item operation and its completion record, the plugin quarantines the record as **REVIEW**. It never automatically retries an ambiguous withdrawal, payout or item delivery. A false result returned normally by the provider is treated as a definite failure; provider exceptions are ambiguous.

Claims, listings, preference values, original serialized item bytes and reconciliation audit history are stored in `plugins/LunarVoidSMP/lunar.db`. Stop the server before copying it, or back up the WAL files with it. Never delete the database to clear an error. Restrict its file access like other player/economy data.

For an interrupted operation, an operator must examine the economy logs and affected player's saved inventory, with **all affected players offline**. Take a backup first. Then record the verified outcome:

```text
/lunar reconcile trade <transaction-id> complete <reason>
/lunar reconcile trade <transaction-id> failed <reason>
/lunar reconcile item <item-id> return <reason>
/lunar reconcile item <item-id> delivered <reason>
```

These commands require `lunar.admin`. Trade reconciliation does **not** move money: `complete` means staff verified the intended financial effects already happened; `failed` means staff verified no financial effect remains (including any required manual refund). Item `return` means staff verified the item is missing and should become a claim; `delivered` means staff verified it is already in the player's possession. Never choose an outcome by guessing. Related money records must be resolved before an item can be independently returned. Every decision stores operator, reason and timestamp; repeat reconciliation is rejected.

Database writes and item serialization currently run on the server thread to preserve ordering. This build has not been load-tested on 2.5 GB RAM; large auction inventories are scanned for matching. Do not infer production readiness from unit tests.

## Validation

Run `mvn clean verify` for the real SQLite transaction/restart tests, money parsing, search ranking, spawn-area boundaries, request acceptance policies, recipient-specific message filtering and Essentials message serialization. These tests use a fake economy provider and synthetic item bytes; they do not establish GUI behavior, actual Vault provider behavior or Bedrock compatibility. See the task's verification summary for the observed run results.

Use the separate `docs/TESTING.md` checklist on a test server before live use.

## Fixed /sell prices and inventory tooltip

`/sell` uses fixed unit amounts from `prices.<MATERIAL>.sell`, separate from public auction asking prices. Each unit price must be positive and at most **$30,000.00**. A whole stack pays unit price × quantity and may exceed $30,000. This has no effect on the existing auction asking-price input limits.

**Item Price defaults OFF**. The requested tooltip shows the whole stack's `/sell` payout with a green dollar sign and white amount. A display-copy projection and shared valuation function are retained and tested, but the actual outgoing-inventory adapter must be replaced following the owner's exclusion of ProtocolLib. The control is currently unavailable and does not falsely indicate that PlaceholderAPI supplies this behavior. Tooltip visibility must not control whether items can be sold. Real item lore, escrow and auction metadata must stay intact.

The owner requested a fixed price for every item type scaled with rarity, but exact amounts are not approved. `prices: {}` remains empty. `docs/SELL_PRICE_PROPOSAL.md` is an incomplete draft starting scale, not active pricing. Metadata-bearing item eligibility still needs the owner's choice. These requirements remain in scope.

## Confirmed navigation (0.4.2)

The owner confirmed the four bottom-row icon actions. The hopper label is exactly **Shop**, and opens auction listings. `/shop` remains the main category/navigation menu. `/settings` opens Settings anywhere commands are permitted. Barrel **Pay** and bare `/pay` open online player selection, followed by sign amount entry and final confirmation, gifting the sender's own money. The earlier need to confirm these four icon meanings is resolved. Exact final Pay confirmation positions are still undecided; the transfer guard is retained at confirmation rather than hiding player selection.

## Night Vision setting (0.4.2)

Night Vision is a new Settings control at zero-based slot **42**, beside Scoreboard. Its icon is a normal potion with base type **Night Vision**. The label uses the same green **ON** / red **OFF** status as the other settings. New players start OFF; an explicitly saved choice survives reconnects and restarts. No additional dependency or permission is required.

ON applies level-I Night Vision with the API's actual **infinite duration**, without potion particles. OFF removes the effect supplied by this setting. A once-per-second check and respawn/potion-event handling restore it after death, reconnect or milk while ON. When milk removes it, the adapter normally restores it next tick. Other plugins' cancelled effect changes remain respected.

The plugin records ownership on the player and removes its own effect at disconnect or plugin shutdown. A retained ownership marker allows cleanup after an interrupted session. Existing Night Vision from a potion or another plugin is left in place; when it expires, the ON setting supplies infinite Night Vision again. If a finite potion is applied under our infinite effect, the adapter yields to that potion so that turning this setting OFF need not discard it. An effect from another source is not claimed merely because it looks identical. Actual interactions with other plugins' potion-event listeners and Multiverse inventory/effect restoration still require live testing.

Managed-effect tests cover ON/OFF, ownership, existing/externally replaced effects, missing-effect reapplication, cancelled grants/removals and idempotent cleanup. SQLite tests cover OFF defaults and persisted ON/OFF preferences. These do not replace a Paper/Geyser/Quest test of the potion icon, actual infinite effect, milk, death and reconnect behavior.

## Built-in Lunar PlaceholderAPI expansion

The owner requested Lunar's own expansion inside the plugin, connected to the normal installed PlaceholderAPI. This is now implemented in source; it registers as `%lunar_...%` without a separate Lunar expansion JAR. See `docs/PLACEHOLDERS.md` for every name, examples, missing/offline-data behavior and the compatibility boundary. Compatible plugins can consume those values in PAPI-enabled fields. It does not make all plugins automatically compatible or implement missing packet/display features.

Snapshots update on the server thread about once per second, so raw placeholder reads do not call Vault, read live inventory or query SQLite from another plugin's callback thread. Existing scoreboard wrappers, per-line preferences and nested placeholders remain available without changing the scoreboard's design. Registration/cleanup and diagnostics are included; actual PAPI/TAB lifecycle and Quest rendering still need live verification. The owner subsequently requested packaging the current incomplete source as a test JAR.
