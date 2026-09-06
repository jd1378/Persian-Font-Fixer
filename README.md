# Persian Font Fixer

Shows Persian and Arabic chat correctly for Minecraft players whose client does not render
right-to-left text.

A Minecraft client with an English (or any left-to-right) language selected draws every
character as a separate glyph, left to right. Persian and Arabic then appear as disconnected
letters in reverse order. A client with Persian or Arabic selected shapes and reorders the text
itself, so any server-side "fix" would be applied twice and come out backwards.

This plugin therefore decides **per player**:

| viewer | receives |
| --- | --- |
| client language is Persian or Arabic | the message untouched |
| any other client language | the message shaped (joined letters) and reordered for a left-to-right renderer |
| console and log | the message untouched |

```
typed          سلام دنیا
English client without this plugin   ﺱ ﻝ ﺍ ﻡ  ﺩ ﻥ ﯼ ﺍ    disconnected, reversed
English client with this plugin      ﺎﯿﻧﺩ ﻡﻼﺳ            joined, right to left
Persian client, with or without      سلام دنیا           (its own renderer)
```

Mixed lines work too: `hello سلام دنیا` keeps the Latin part in place and reorders only the
Arabic-script runs. Digits keep their order, brackets are mirrored inside right-to-left runs,
and combining marks stay on their letter.

## Compatibility

One jar for every server version:

| server | mechanism | chat signatures (1.19.1+) |
| --- | --- | --- |
| Paper 1.16.5 and newer | `AsyncChatEvent` with a per-viewer `ChatRenderer` | kept; clients that need the fix see the message flagged as "Modified" |
| Spigot / Bukkit, or Paper without the Adventure chat API | `AsyncPlayerChatEvent`, recipients split by hand | kept for untouched viewers; fixed viewers get an unsigned system message |

Java 8 bytecode, `api-version: 1.13`, no dependencies (ProtocolLib is no longer needed).
Verified with bots on Paper 1.21.4 (Java 21), 1.16.5 and 1.12.2 (Java 8).

## Proxy jars (BungeeCord, Velocity)

`PersianFontFixer-bungee.jar` and `PersianFontFixer-velocity.jar` exist for setups that want
one install on the proxy instead of one per backend. They use the old design, because proxy
APIs offer nothing better:

- the sender's message is rewritten once, so **every** viewer gets the visual form, including
  clients with Persian or Arabic selected, who then see it reversed;
- 1.19.1+ clients are skipped entirely: BungeeCord ignores the change and forwards the original,
  Velocity disconnects the player for changing a signed message. Those players see raw text.

Prefer the backend jar. It works behind either proxy unchanged, per player, on every version.
**Never run a proxy jar and the backend jar together**: the already shaped text would be
shaped and reversed a second time.

Verified with bots: both proxies rewrite on 1.18.2 and leave 1.21.4 signed chat untouched.

## Configuration

`plugins/PersianFontFixer/config.yml`

```yaml
listener: auto        # auto | paper | bukkit
debug: false          # log every chat decision
rtl-locales: [fa, ar] # client languages whose renderer already handles right-to-left text
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

GPLv3.
