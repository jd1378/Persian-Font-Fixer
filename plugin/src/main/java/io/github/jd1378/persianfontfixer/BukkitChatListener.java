package io.github.jd1378.persianfontfixer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import io.github.jd1378.persianfontfixer.rtl.RtlText;

/**
 * Fallback for servers without Paper's per-viewer renderer.
 *
 * The event carries one message for every recipient, so the split is done by hand: the
 * message stays untouched for the console and for clients that render Arabic script
 * themselves (1.16.2+, or a right-to-left language), and the remaining players are removed
 * from the recipients and sent the visual form directly. On 1.19.1+ the untouched message
 * keeps its chat signature.
 */
final class BukkitChatListener implements Listener {
    private final Viewers viewers;
    private final Logger debug;

    BukkitChatListener(Viewers viewers, Logger debug) {
        this.viewers = viewers;
        this.debug = debug;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String raw = event.getMessage();
        if (!RtlText.hasArabic(raw)) {
            return;
        }
        Set<Player> recipients = event.getRecipients();
        List<Player> needsFix = new ArrayList<Player>();
        for (Player viewer : recipients) {
            if (viewers.needsVisualForm(viewer)) {
                needsFix.add(viewer);
            }
        }
        if (debug != null) {
            debug.info("chat from " + event.getPlayer().getName() + ": " + needsFix.size() + " of "
                    + recipients.size() + " recipients need the visual form");
        }
        if (needsFix.isEmpty()) {
            return;
        }

        String visual = RtlText.toVisual(raw);
        try {
            recipients.removeAll(needsFix);
        } catch (UnsupportedOperationException immutableRecipients) {
            event.setMessage(visual); // another plugin froze the recipients; everyone gets the visual form
            return;
        }
        String line = String.format(event.getFormat(), event.getPlayer().getDisplayName(), visual);
        for (Player viewer : needsFix) {
            viewer.sendMessage(line);
        }
    }
}
