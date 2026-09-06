package io.github.jd1378.persianfontfixer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
import net.kyori.adventure.text.TranslatableComponent;

/**
 * Paper renders chat once per viewer, so each player gets the form their client needs
 * while the signed message itself is never altered. The console and clients that draw
 * right-to-left text themselves see the original.
 *
 * The wrapped renderer is applied first and its whole output is reshaped afterwards.
 * Passing a reshaped message into it would not work: Paper's viewer-unaware renderers
 * (the default one included) cache their first result and return it for every viewer.
 */
final class PaperChatListener implements Listener {
    private final ViewerLocales locales;
    private final Logger debug;

    PaperChatListener(ViewerLocales locales, Logger debug) {
        this.locales = locales;
        this.debug = debug;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!hasArabic(event.message())) {
            return;
        }
        final ChatRenderer inner = event.renderer();
        event.renderer(new ChatRenderer() {
            @Override
            public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
                Component line = inner.render(source, sourceDisplayName, message, viewer);
                boolean fix = viewer instanceof Player && !locales.rendersRtl((Player) viewer);
                if (debug != null) {
                    debug.info("chat from " + source.getName() + " to " + viewer + ": " + (fix ? "visual form" : "untouched"));
                }
                return fix ? toVisual(line) : line;
            }
        });
    }

    static boolean hasArabic(Component component) {
        if (component instanceof TextComponent && RtlText.hasArabic(((TextComponent) component).content())) {
            return true;
        }
        if (component instanceof TranslatableComponent) {
            for (Component arg : ((TranslatableComponent) component).args()) {
                if (hasArabic(arg)) {
                    return true;
                }
            }
        }
        for (Component child : component.children()) {
            if (hasArabic(child)) {
                return true;
            }
        }
        return false;
    }

    /** Each text node is reordered on its own; style and translation-argument boundaries act as run boundaries. */
    static Component toVisual(Component component) {
        Component result = component;
        if (component instanceof TextComponent) {
            TextComponent text = (TextComponent) component;
            result = text.content(RtlText.toVisual(text.content()));
        } else if (component instanceof TranslatableComponent) {
            TranslatableComponent translatable = (TranslatableComponent) component;
            result = translatable.args(mapAll(translatable.args()));
        }
        List<Component> children = component.children();
        return children.isEmpty() ? result : result.children(mapAll(children));
    }

    private static List<Component> mapAll(List<Component> components) {
        List<Component> mapped = new ArrayList<Component>(components.size());
        for (Component component : components) {
            mapped.add(toVisual(component));
        }
        return mapped;
    }
}
