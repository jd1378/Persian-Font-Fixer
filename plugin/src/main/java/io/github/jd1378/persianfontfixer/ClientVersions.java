package io.github.jd1378.persianfontfixer;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Which players run a client that shapes and reorders Arabic script on its own.
 *
 * Since 1.16.2 (snapshot 20w30a, MC-35765) the client does both in every language, so it must
 * receive the original text; the visual form would be reordered a second time. Older clients
 * draw code points as-is and need the visual form unless their language is right-to-left.
 */
final class ClientVersions {
    /** Protocol number of Minecraft 1.16.2. */
    static final int SELF_RENDERING_PROTOCOL = 751;

    interface Source {
        int protocolOf(Player player);
    }

    private final Source source;
    private final Boolean forced;

    /**
     * @param assume "auto" to detect per player, "legacy" or "modern" to override detection
     */
    ClientVersions(String assume, Logger logger) {
        String mode = assume.toLowerCase(Locale.ROOT);
        forced = mode.equals("legacy") ? Boolean.FALSE : mode.equals("modern") ? Boolean.TRUE : null;
        source = forced != null ? null : resolveSource(logger);
    }

    boolean rendersArabicItself(Player player) {
        if (forced != null) {
            return forced;
        }
        return source.protocolOf(player) >= SELF_RENDERING_PROTOCOL;
    }

    /** ViaVersion knows the real client version on multi-version servers; Paper reports it natively; otherwise clients match the server. */
    private static Source resolveSource(Logger logger) {
        Source via = viaVersion();
        if (via != null) {
            logger.info("Client versions from ViaVersion");
            return via;
        }
        Source paper = paperProtocolVersion();
        if (paper != null) {
            logger.info("Client versions from Paper");
            return paper;
        }
        final int server = serverProtocolClass();
        logger.info("Client versions assumed equal to the server's (" + (server >= SELF_RENDERING_PROTOCOL ? "1.16.2 or newer" : "before 1.16.2") + ")");
        return new Source() {
            @Override
            public int protocolOf(Player player) {
                return server;
            }
        };
    }

    private static Source viaVersion() {
        try {
            Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
            final Object api = via.getMethod("getAPI").invoke(null);
            final Method getPlayerVersion = api.getClass().getMethod("getPlayerVersion", java.util.UUID.class);
            getPlayerVersion.setAccessible(true);
            return new Source() {
                @Override
                public int protocolOf(Player player) {
                    try {
                        return (Integer) getPlayerVersion.invoke(api, player.getUniqueId());
                    } catch (ReflectiveOperationException e) {
                        return serverProtocolClass();
                    }
                }
            };
        } catch (ReflectiveOperationException | RuntimeException absent) {
            return null;
        }
    }

    private static Source paperProtocolVersion() {
        try {
            final Method getProtocolVersion = Player.class.getMethod("getProtocolVersion");
            return new Source() {
                @Override
                public int protocolOf(Player player) {
                    try {
                        return (Integer) getProtocolVersion.invoke(player);
                    } catch (ReflectiveOperationException e) {
                        return serverProtocolClass();
                    }
                }
            };
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    /** Not the exact protocol number, just which side of 1.16.2 the server is on. */
    static int serverProtocolClass() {
        return isAtLeast1_16_2(Bukkit.getBukkitVersion()) ? SELF_RENDERING_PROTOCOL : 0;
    }

    /** Accepts "1.16.5-R0.1-SNAPSHOT" and the year-based "26.2-R0.1-SNAPSHOT". */
    static boolean isAtLeast1_16_2(String bukkitVersion) {
        String[] parts = bukkitVersion.split("-", 2)[0].split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            if (major != 1) {
                return major > 1;
            }
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return minor > 16 || (minor == 16 && patch >= 2);
        } catch (NumberFormatException unknown) {
            return true; // unknown format means something newer than anything this was written for
        }
    }
}
