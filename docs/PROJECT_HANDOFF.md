# Latest: 0.6.1-test — approved $30,000 per-item sell cap

The VAULT sell price is enabled at $30,000. Upgrades automatically add its missing price while preserving explicit custom prices. See docs/UPDATE_0.6.1.md (UPDATE_0.6.1.md from this folder).

# Current update: 0.6.0-test

Read [the current update and limitations](docs/UPDATE_0.6.0.md) first (from docs/, use UPDATE_0.6.0.md). It supersedes older Pay/listing/price and integration descriptions below. Owner update and reference files are preserved in this backup.

# LUNARVOIDSMP recovery handoff — 0.5.0-test

## Latest user instruction

Run all available tests and provide a JAR immediately. This supersedes the earlier instruction to wait until the entire project is finished. The delivered JAR is an incomplete test build. Do not claim full readiness or that live Minecraft scenarios were tested.

## Start here after restoring

This archive contains the complete source project, documentation, approved specification files and supplied screenshots. The original repository HEAD was b7328e2329aaf60746dc4f136194fe1065cda690. Source changes were not committed or pushed. Use the included source as the working state, not the original README-only checkout. Source root contains pom.xml; JDK25 and Maven3.9+ are required. Run mvn clean verify to compile, test and build. Use target/lunarvoidsmp-0.5.0-test.jar, not original-lunarvoidsmp-0.5.0-test.jar.

## Requirements to preserve

- Paper26.3+ goal, Falix2.5GB, Java25, Bedrock through Geyser/Floodgate on Meta Quest2. Exact reported Falix build2651 is unverified. Official compile API26.3.build.205-stable.
- No ProtocolLib. Keep normal PlaceholderAPI; Lunar has its own built-in internal expansion registered as lunar, with no separate expansion download. Do not pretend this implements missing packet interception.
- Retain EssentialsX/Vault economy, TAB/PAPI, LuckPerms and BetterRTP integration plans. The old shop/menu plugins are to be replaced, not required.
- /shop main category/navigation menu. Hopper named Shop at48 opens public auction listings; sign Search49; wooden Settings50; barrel Pay53. /settings and bare /pay open their menus directly. Preserve the rest of the owner's screenshots/layouts.
- /ah sell <price> immediately lists the ENTIRE main-hand stack at that total price, retaining metadata. No listing confirmation and NO expiration. Seller cannot buy own item. Buyer must have space before charging; successful purchase delivers stack directly and notifies buyer/online seller.
- /sell is separate fixed-price SERVER selling, paid only on confirmation. Closing sells nothing and returns escrow or retains durable claims. Every item needs its own rarity-based price; max UNIT sell price$30,000. Whole-stack payout multiplies quantity and may exceed$30,000. Exact prices remain unapproved/empty. Existing metadata restrictions remain pending owner decision.
- Item Price OFF by default; requested inventory tooltip shows WHOLE-stack /sell value, green dollar sign and white amount. Current placeholder values report main-hand stack; they do not implement hovering/crosshair inventory tooltips.
- Night Vision added at Settings42, actual Night Vision potion icon, defaultOFF; ON infinite level-I effect with ownership cleanup/reapply and persisted choice. Rank Tag also defaultsOFF. Do not redesign TAB or overwrite its files.
- Settings greenON/redOFF, no Toggle suffix. Edit Shop and Instant RTL removed;39 remains empty. TPA Request controls notifications, Auto TPA accepts online incoming /tpa requests to recipient, not /tpahere. Mob/Phantom OFF block relevant natural spawns in current chunk and eight neighbors; Scoreboard OFF hides entire TAB sidebar.

## Implemented/tested

Current source includes SQLite money/item journals, durable claims/recovery/reconciliation, auction/sell/pay transfer policies, menus/sign input, settings/preferences, existing TPA/spawn/TAB controls, Night Vision, and internal PAPI expansion. See STATUS.md and README for exact caveats. Full mvn clean verify in the delivery turn passed66 automated tests with no failures/errors/skips. Tests cover exercised logic and fake Bukkit/API boundaries, not a running server/client.

## Still unfinished — do not silently discard or activate guesses

- Replacement inventory-tooltip and incoming-TPA-message adapters, without ProtocolLib. Official PacketEvents2.14.0 lists MC26.3 support; internally bundling a suitable implementation is a candidate, not implemented/verified.
- Exact complete sell price table/special-item rules; auction listing limit/fee and final purchase layout; final Pay confirmation positions; auction sort/page-control positions and quantity pane mapping. Their configuration guards keep these flows blocked until chosen.
- Per-viewer Money/Rank tags, death/general notification/particle settings and other retained requirements listed in STATUS.md.
- Actual TAB config/animations and BetterRTP language configuration; Bedrock own-skin icon behavior and full provider integration.
- Live Paper/Essentials/Vault/PAPI/TAB/Geyser/Floodgate/Quest testing, GUI exploit/restart/load tests. No production deployment/migration or Falix memory test occurred.

Read PROJECT_REQUIREMENTS.txt for the master layout, STATUS.md for later decisions, PLACEHOLDERS.md for the36 PAPI placeholders, TESTING.md for remaining live scenarios, and INTEGRATION_PLAN.md for replacement work. In attached reference files, the later owner decisions above override earlier wording. Superseded wrong first specification is intentionally excluded from this backup.
