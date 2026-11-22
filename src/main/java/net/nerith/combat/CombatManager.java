package net.nerith.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CombatManager {

    private static final int COMBAT_TIME = 10;

    private final NerithMcCombat plugin;
    private final Map<UUID, Integer> time = new HashMap<>();
    private final Map<UUID, String> opponent = new HashMap<>();
    private final Set<UUID> loggedOutInCombat = new HashSet<>();

    public CombatManager(NerithMcCombat plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : new ArrayList<>(time.keySet())) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline()) {
                    remove(id);
                    continue;
                }
                int t = time.get(id) - 1;
                if (t <= 0) {
                    time.put(id, 0);
                    show(p);
                    remove(id);
                    p.sendMessage(NerithMcCombat.color("&aYou are no longer in combat"));
                } else {
                    time.put(id, t);
                    show(p);
                }
            }
        }, 20L, 20L);
    }

    public void tag(Player player, Player other) {
        UUID id = player.getUniqueId();
        boolean already = time.containsKey(id);
        time.put(id, COMBAT_TIME);
        opponent.put(id, other.getName());
        if (!already) {
            player.sendMessage(NerithMcCombat.color("&cYou are in combat don't log out"));
        }
        show(player);
    }

    private void show(Player p) {
        UUID id = p.getUniqueId();
        int t = time.getOrDefault(id, 0);
        String name = opponent.getOrDefault(id, "");
        p.sendActionBar(NerithMcCombat.color("&cCombat : &f" + t + " &7(" + name + ")"));
    }

    public boolean isInCombat(UUID id) {
        return time.containsKey(id);
    }

    public void remove(UUID id) {
        time.remove(id);
        opponent.remove(id);
    }

    public void markLoggedOut(UUID id) {
        loggedOutInCombat.add(id);
    }

    public boolean consumeLoggedOut(UUID id) {
        return loggedOutInCombat.remove(id);
    }
}
