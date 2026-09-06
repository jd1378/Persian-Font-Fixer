# Persian Font Fixer

Shows Persian and Arabic chat correctly for Minecraft players whose client does not render
right-to-left text.

Minecraft clients **older than 1.16.2** draw every character as a separate glyph, left to
right, unless the selected language is itself right-to-left. Persian and Arabic then appear as
disconnected letters in reverse order. Since 1.16.2 (snapshot 20w30a, [MC-35765]) the client
shapes and reorders Arabic script in every language, so those clients must get the text
untouched; anything pre-shaped would be reordered a second time.

This plugin therefore decides **per player**:

| viewer | receives |
| --- | --- |
| client 1.16.2 or newer | the message untouched (the client renders it correctly itself) |
| older client, language Persian or Arabic | the message untouched (the client's own right-to-left mode) |
| older client, any other language | the message shaped (joined letters) and reordered for a left-to-right renderer |
| console and log | the message untouched |

```
typed                                   سلام دنیا
old English client without this plugin  ﺱ ﻝ ﺍ ﻡ  ﺩ ﻥ ﯼ ﺍ    disconnected, reversed
old English client with this plugin     ﺎﯿﻧﺩ ﻡﻼﺳ            joined, right to left
1.16.2+ or Persian client               سلام دنیا           (its own renderer)
```

Mixed lines work too: `hello سلام دنیا` keeps the Latin part in place and reorders only the
Arabic-script runs. Digits keep their order, brackets are mirrored inside right-to-left runs,
and combining marks stay on their letter.

The client version comes from ViaVersion when installed (exact per-player version on
multi-version servers), else from Paper's `Player#getProtocolVersion`, else clients are assumed
to match the server. `assume-clients` in the config overrides detection.

[MC-35765]: https://bugs.mojang.com/browse/MC-35765

## Compatibility

One jar for every server version:

| server | mechanism | chat signatures (1.19.1+) |
| --- | --- | --- |
| Paper 1.16.5 and newer | `AsyncChatEvent` with a per-viewer `ChatRenderer` | kept; clients that need the fix see the message flagged as "Modified" |
| Spigot / Bukkit, or Paper without the Adventure chat API | `AsyncPlayerChatEvent`, recipients split by hand | kept for untouched viewers; fixed viewers get an unsigned system message |

Java 8 bytecode, `api-version: 1.13`, no dependencies (ProtocolLib is no longer needed;
ViaVersion is used when present). Verified with bots on Paper 1.8.8, 1.12.2, 1.16.5 (Java 8),
1.21.4 (Java 21) including 1.12.2 clients joining 1.21.4 through ViaVersion, and loaded on 26.2.

## Proxy jars (BungeeCord, Velocity)

`PersianFontFixer-bungee.jar` and `PersianFontFixer-velocity.jar` exist for networks where
**every client is older than 1.16.2** and one install on the proxy is preferred. They use the
old design, because proxy APIs offer nothing better: the sender's message is rewritten once,
so every viewer gets the visual form, Persian-language clients included. Messages from 1.16.2+
senders are left alone. On a mixed-version network some viewers will always see the wrong
form, so use the backend jar there.

Prefer the backend jar. It works behind either proxy unchanged, per player, on every version.
**Never run a proxy jar and the backend jar together**: the already shaped text would be
shaped and reversed a second time.

Verified with bots: both proxies rewrite for 1.12.2 clients and leave 1.18.2 clients untouched.

## Configuration

`plugins/PersianFontFixer/config.yml`

```yaml
listener: auto        # auto | paper | bukkit
debug: false          # log every chat decision
assume-clients: auto  # auto | legacy | modern  (override client version detection)
rtl-locales: [fa, ar] # languages whose renderer handles right-to-left text on old clients
```

## Building and testing (Docker only, nothing installed on the host)

```sh
# compile, run unit tests, write out/PersianFontFixer.jar (+ -bungee, -velocity)
docker build --target artifact -o out .

# Paper server (offline mode) with the plugin baked in
docker compose up -d --build mc
docker compose logs -f mc

# two mineflayer bots, one English and one Persian, exchange Persian chat and assert what each receives
docker compose run --rm bot

# other server versions: MC_JAVA picks the itzg image (java21 / java17 / java16 / java8)
MC_VERSION=1.16.5 MC_JAVA=java8 docker compose up -d --build mc

# multi-version: ViaVersion on the server, bots joining as another client version
MC_PLUGINS=viaversion,viabackwards docker compose up -d --build mc
CLIENT_VERSION=1.12.2 docker compose run --rm -e EXPECT=backend bot
CLIENT_VERSION=1.21.4 docker compose run --rm -e EXPECT=untouched bot

docker compose down -v

# proxy jars: plain backend behind BungeeCord or Velocity, bots connect through the proxy
MC_VERSION=1.18.2 MC_JAVA=java17 docker compose --profile proxy up -d --build proxy-bungee
docker compose run --rm --no-deps -e MC_HOST=proxy-bungee -e MC_PORT=25577 -e EXPECT=legacy bot
```

Bind mounts are not used anywhere, so this works under Docker Desktop's file-sharing restrictions.

## Layout

```
core/      shaping + bidi reordering, plain Java, no Bukkit dependency, unit tests
plugin/    Bukkit bootstrap, Paper and Bukkit chat listeners, locale check
bungee/    BungeeCord plugin (legacy design)
velocity/  Velocity plugin (legacy design, Java 17)
docker/    mineflayer end-to-end test, proxy configs
```

The form table in `core` is derived from the JDK's Unicode data (presentation form names and
compatibility decompositions), and reordering uses `java.text.Bidi`, so there is no hand-written
letter table to keep in sync.

## Known limits

- A long right-to-left message that the client wraps onto two lines wraps at the wrong end,
  because the reordering cannot know the client's line width.
- Only chat is handled. Signs, item names, scoreboards and other text are untouched.

## License

GNU Affero General Public License v3.0 only (`AGPL-3.0-only`). See [LICENSE](LICENSE).
