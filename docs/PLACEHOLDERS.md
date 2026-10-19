# Built-in Lunar placeholders

LunarVoidSMP includes its own **internal PlaceholderAPI expansion**, using identifier `lunar`. Keep the normal PlaceholderAPI plugin installed. Lunar registers its expansion automatically; no Lunar expansion download, eCloud installation or separate expansion JAR is needed. This is an expansion connected to PlaceholderAPI, not a second copy of PlaceholderAPI.

## Compatibility

Use these values in TAB or other plugins' configuration fields that explicitly support PlaceholderAPI. Those plugins still control whether/how they parse text and what player they provide. The expansion cannot add PlaceholderAPI support to a plugin that does not implement it, and does not replace Vault, LuckPerms, TAB, Essentials or BetterRTP APIs. Existing third-party placeholders still need their own required expansions.

No ProtocolLib dependency is used. This does not implement inventory tooltip interception, TPA message filtering, per-viewer tags or other unfinished integration features. A held-item price placeholder reports the main-hand stack; PlaceholderAPI has no general knowledge of the inventory slot under a Bedrock crosshair.

## Values

All examples assume a connected player. Dollar formatting is plain text without color codes; the consuming plugin controls presentation.

| Placeholder | Output |
|---|---|
| `%lunar_name%` | Actual player name, including a Floodgate name prefix if present |
| `%lunar_uuid%` | Requested player's UUID; also available for offline player context |
| `%lunar_balance%` | Vault balance with two decimals, e.g. `1234.56` |
| `%lunar_balance_formatted%` | Balance with dollar sign/grouping, e.g. `$1,234.56` |
| `%lunar_balance_short%` | Compact balance without dollar sign, e.g. `1.2k` |
| `%lunar_ping%` | Ping in milliseconds, without unit suffix |
| `%lunar_online%` | Total online players; no player context needed |
| `%lunar_tps%` | Latest one-minute TPS with two decimals, capped at `20.00`; no player context needed |
| `%lunar_held_sell_value%` | Entire main-hand stack's fixed `/sell` payout, e.g. `640000.00` |
| `%lunar_held_sell_value_formatted%` | Same payout formatted as `$640,000.00` |
| `%lunar_held_sellable%` | `true` or `false` for that stack's eligibility in the current `/sell` table |

Balances and held-item values are read on the main server thread and cached about once per second (20 ticks). They are display snapshots, never the authority for purchases/payments. Actual transfers recheck the authoritative provider/items. A slow server may refresh less often. Missing provider balances, unpriced/ineligible items and unavailable player data return blank text, not an invented zero. A real zero balance returns `0.00`. Player-specific cache data is removed on disconnect, so offline balances/held items are not queried. Unknown Lunar parameter names return null to PlaceholderAPI, allowing it to retain unresolved placeholders normally.

The empty approved price table currently makes held stacks unsellable; suggested prices are not silently activated. The `$10,000` unit cap and quantity multiplication remain the same as `/sell`.

## Settings

`%lunar_setting_<key>%` returns `true`, `false` or `unset` for a connected player's cached preference. Examples:

- `%lunar_setting_night_vision%`
- `%lunar_setting_item_price%`
- `%lunar_setting_scoreboard%`

Supported keys:

`notifications`, `global_chat`, `death_messages`, `money_tag`, `money`, `discord_link`, `online`, `rank_tag`, `ping`, `tpa_request`, `tnt_particles`, `end_crystal_particles`, `auto_tpa`, `phantom_spawning`, `mob_spawning`, `server_tps`, `item_price`, `scoreboard`, `night_vision`.

A saved preference is not a feature-availability claim. Some settings remain unfinished and cannot be toggled yet. No player context or a disconnected player returns blank rather than guessing a persisted preference from default values.

## Existing TAB line wrappers

Retained wrappers: `%lunar_line_money%`, `%lunar_line_discord_link%`, `%lunar_line_online%`, `%lunar_line_rank_tag%`, `%lunar_line_ping%`, `%lunar_line_server_tps%`.

Copy the owner's original complete TAB line into the corresponding `tab.lines` key, including original colors, wording, animations and nested placeholders. Replace that TAB line with its single wrapper. OFF returns an empty whole line; ON/unset evaluates the original template through PlaceholderAPI. Nested Lunar values and other installed expansions can be used. Circular line wrappers terminate with empty text, and recursion state is cleaned up even if another expansion throws. Third-party nested expansions retain their own caller-thread requirements.

Do not redesign the scoreboard or rewrite TAB files automatically. The original TAB configuration/animations are still needed before final setup.

## Lifecycle and verification

The internal expansion persists through normal `/papi reload`, reports the Lunar plugin version, and advertises its placeholder names. Initial or later PlaceholderAPI enable attempts registration; name collisions log a warning rather than replacing someone else's `lunar` expansion. PAPI disable clears the cache/reference; Lunar shutdown unregisters its own expansion. Missing PAPI does not disable other Lunar features. Snapshot refresh failures clear stale display values and are isolated from the settings/transfer code.

`/lunar status` reports internal expansion registration. When a live test server is available, operator checks include:

```
/papi parse me %lunar_balance_formatted%
/papi parse me %lunar_setting_night_vision%
/papi parse me %lunar_held_sell_value_formatted%
```

Unit tests cover the real PAPI expansion boundary with a fake Bukkit player, exact cent formatting, unavailable values, settings states, preserved/delegated line templates and recursion recovery. They do not verify registry lifecycle against a running PAPI/TAB server or client display through Geyser/Quest. The complete plugin remains unfinished, and the owner has subsequently requested immediate delivery of an explicitly incomplete test JAR.
