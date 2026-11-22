package net.nerith.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class CombatListener implements Listener {

    private final NerithMcCombat plugin;
    private final CombatManager combat;

    public CombatListener(NerithMcCombat plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null || attacker.equals(victim)) return;

        combat.tag(attacker, victim);
        combat.tag(victim, attacker);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.cancelTeleport(p.getUniqueId(), false);

        boolean inCombat = combat.isInCombat(p.getUniqueId());
        boolean inVoid = Bukkit.getPluginManager().isPluginEnabled("WorldGuard")
                && VoidRegionChecker.isInVoidRegion(p.getLocation());

        if (inCombat) {
            combat.remove(p.getUniqueId());
            combat.markLoggedOut(p.getUniqueId());
        }
        if ((inCombat || inVoid) && !p.isDead()) {
            p.setHealth(0.0);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (combat.consumeLoggedOut(p.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    p.sendMessage(NerithMcCombat.color("&cYou logged out in combat and die")), 5L);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        combat.remove(e.getEntity().getUniqueId());
        plugin.cancelTeleport(e.getEntity().getUniqueId(), false);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Location spawn = plugin.getSpawn();
        if (spawn != null) {
            e.setRespawnLocation(spawn);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!plugin.isTeleporting(p.getUniqueId())) return;

        Location start = plugin.getTeleportStart(p.getUniqueId());
        Location to = e.getTo();
        if (start == null || to == null) return;

        if (start.getBlockX() != to.getBlockX()
                || start.getBlockY() != to.getBlockY()
                || start.getBlockZ() != to.getBlockZ()) {
            plugin.cancelTeleport(p.getUniqueId(), true);
        }
    }
}
