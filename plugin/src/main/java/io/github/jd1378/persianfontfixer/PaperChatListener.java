package io.github.jd1378.persianfontfixer;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import io.github.jd1378.persianfontfixer.rtl.RtlText;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;

/**
 * Paper renders chat once per viewer, so each player gets the form their client needs
 * while the signed message itself is never altered. The console and clients that render
 * Arabic script themselves (1.16.2+, or a right-to-left language) see the original.
 *
 * The wrapped renderer is applied first and its whole output is reshaped afterwards.
 * Passing a reshaped message into it would not work: Paper's viewer-unaware renderers
 * (the default one included) cache their first result and return it for every viewer.
 */
final class PaperChatListener implements Listener {
    private final Viewers viewers;
    private final Logger debug;
    private final Logger logger;
    private volatile boolean reportedFailure;

    PaperChatListener(Viewers viewers, Logger logger, Logger debug) {
        this.viewers = viewers;
        this.logger = logger;
        this.debug = debug;
    }

    /** Whether this server's Adventure supports the transform; false on builds too old or too new for it. */
    static boolean selfTest() {
        try {
            return RtlText.hasArabic(((TextComponent) toVisual(Component.text("سلام"))).content());
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!hasArabic(event.message())) {
            return;
        }
        final ChatRenderer inner = event.renderer();
        event.renderer(new ChatRenderer() {
            // Viewer-unaware renderers return the same line object for every viewer, so the
            // visual form is built once per message instead of once per viewer. One immutable
            // pair in one field: a concurrent race can only cost a duplicate computation.
            private Component[] last;

            @Override
            public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
                Component line = inner.render(source, sourceDisplayName, message, viewer);
                boolean fix = viewer instanceof Player && viewers.needsVisualForm((Player) viewer);
                if (debug != null) {
                    debug.info("chat from " + source.getName() + " to " + viewer + ": " + (fix ? "visual form" : "untouched"));
                }
                return fix ? visualOf(line) : line;
            }

            private Component visualOf(Component line) {
                Component[] pair = last;
                if (pair == null || pair[0] != line) {
                    pair = new Component[]{line, visualOrOriginal(line)};
                    last = pair;
                }
                return pair[1];
            }
        });
    }

    /** A failed transform must never cost the viewer the message: fall back to the original line. */
    private Component visualOrOriginal(Component line) {
        try {
            return toVisual(line);
        } catch (LinkageError | RuntimeException e) {
            if (!reportedFailure) {
                reportedFailure = true;
                logger.warning("Could not reshape a chat line, sending it untouched. Set listener: bukkit in config.yml. Cause: " + e);
            }
            return line;
        }
    }

    static boolean hasArabic(Component component) {
        if (component instanceof TextComponent && RtlText.hasArabic(((TextComponent) component).content())) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasArabic(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reshapes every text node in the tree, translation arguments included. Each node is reordered
     * on its own, so style boundaries act as run boundaries. Goes through Adventure's own
     * replaceText walker (4.2+) rather than the translatable-argument accessors, which were
     * renamed between the oldest and newest Adventure versions this plugin runs on.
     */
    private static final TextReplacementConfig TO_VISUAL = TextReplacementConfig.builder()
            .match(Pattern.compile(".+", Pattern.DOTALL))
            .replacement((match, builder) -> builder.content(RtlText.toVisual(match.group())))
            .build();

    static Component toVisual(Component component) {
        return component.replaceText(TO_VISUAL);
    }
}
