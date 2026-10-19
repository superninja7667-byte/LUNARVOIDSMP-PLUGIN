# Latest delivery update

The owner now explicitly requests testing and packaging immediately. The test JAR is authorized despite incomplete readiness; earlier packaging deferral text below is historical. All unfinished requirements and verification needs remain in scope, and no claim of a finished plugin is permitted.

# Current integration plan

## Owner constraints

- Paper26.3+, Falix2.5GB, Geyser/Floodgate and Meta Quest2.
- Keep PlaceholderAPI, TAB, LuckPerms, EssentialsX, Vault and BetterRTP integrations.
- Do not use ProtocolLib. Do not require another separately installed packet plugin as an unapproved replacement.
- Preserve GUI layouts, tooltip/notification requirements and all later owner decisions.
- Work in source until the plugin is fully ready; only then package/deliver the JAR.

## What PlaceholderAPI provides

The built-in Lunar expansion exposes cached balance, player/server values, fixed held-stack sell value, all personal settings, and wrappers around the owner's existing TAB line text. See `PLACEHOLDERS.md`; no separate Lunar expansion download is needed. PlaceholderAPI resolves text values; it does not supply arbitrary outgoing inventory/message interception. Replacing ProtocolLib with a PlaceholderAPI dependency alone would leave the tooltip and TPA notification requirements unmet.

## Replacement investigation

Official PacketEvents **2.14.0** release notes explicitly list Minecraft26.3 support:
https://github.com/retrooper/packetevents/releases/tag/v2.14.0

Assess whether a relocated, internally bundled integration is suitable without any additional server plugin. Verify distribution/bundling instructions, lifecycle, Java25/Paper26.3 compatibility, interaction with any existing packet libraries, and server/client memory overhead. Do not introduce it merely because the release supports the Minecraft protocol.

For inventory price display, preserve original lore/item bytes, server-side sale eligibility, arbitrary auction metadata, creative safety, slot boundaries, cursor changes and full-stack prices. Establish safe thread handling for Bukkit conversions and modern inventory component hashes/prediction; verify Geyser translation and Quest controls on actual clients.

For TPA notifications, filter only the intended recipient's localized request prompts. Retain pending requests and Essentials safety/permissions/costs/expiry; do not replace message visibility with request cancellation.

These adapters are currently unavailable. The code retains their narrow interfaces and tested supporting policies rather than presenting unfinished controls as working.

## Other readiness requirements still outstanding

- Complete approved every-item /sell price table and special-item valuation rules.
- Auction listing limit/fee, final buying/Pay confirmation layouts and remaining navigation-control positions.
- Remaining requested settings: viewer-specific Money/Rank tags, death-message/general-notification and particle handling.
- Actual TAB configuration/animations and BetterRTP messages to preserve the server's presentation.
- Live economy/provider, GUI/exploit/restart/reconciliation checks; Geyser/Floodgate/Quest tests; Falix memory/load tests.

Source compilation/unit tests are evidence for their exercised code only. They do not establish full readiness or justify a final JAR while any required work remains.
