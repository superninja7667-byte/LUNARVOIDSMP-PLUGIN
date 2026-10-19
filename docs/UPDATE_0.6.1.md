# 0.6.1-test — approved $30,000 sell cap

- `/sell` accepts unit prices up to **$30,000.00**. A stack pays that price multiplied by its quantity; the cap applies per item.
- The supplied **VAULT price of $30,000** is now enabled. All 1,238 modern table entries fit the cap.
- On upgrade from 0.6.0-test, a missing Vault price is added automatically even if the other prices were already imported. Explicitly configured or disabled Vault prices are preserved.
- Auction asking prices keep their separate existing limits.

## Installation

Stop the test server, back up the LunarVoidSMP data folder, replace the older Lunar JAR with `LunarVoidSMP-0.6.1-test.jar`, and restart normally. Keep only one Lunar JAR and preserve `lunar.db` and configuration. No configuration deletion is needed.

The remaining 0.6.0 functionality and limitations are documented in UPDATE_0.6.0.md. Inventory tooltip, skins, maintenance timers and exact TAB parity remain unfinished. Auction listing limit and fee still need actual values. Live Falix/Paper/provider/Geyser/Quest and load verification has not run.
