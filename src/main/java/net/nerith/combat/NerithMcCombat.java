package net.nerith.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NerithMcCombat extends JavaPlugin {

    private CombatManager combatManager;
    private Location spawn;

    // /spawn teleports in progress
    private final Map<UUID, BukkitTask> teleporting = new HashMap<>();
    private final Map<UUID, Location> teleportStart = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSpawn();

        combatManager = new CombatManager(this);
        combatManager.start();

        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager), this);
        getLogger().info("NerithMcCombat enabled.");
    }

    @Override
    public void onDisable() {
        teleporting.values().forEach(BukkitTask::cancel);
        teleporting.clear();
    }

    public static Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    // ---------------- Spawn ----------------

    private void loadSpawn() {
        if (!getConfig().contains("spawn.world")) return;
        World w = Bukkit.getWorld(getConfig().getString("spawn.world", ""));
        if (w == null) return;
        spawn = new Location(
                w,
                getConfig().getDouble("spawn.x"),
                getConfig().getDouble("spawn.y"),
                getConfig().getDouble("spawn.z"),
                (float) getConfig().getDouble("spawn.yaw"),
                (float) getConfig().getDouble("spawn.pitch"));
    }

    public Location getSpawn() {
        return spawn == null ? null : spawn.clone();
    }

    private void saveSpawn(Location l) {
        spawn = l.clone();
        getConfig().set("spawn.world", l.getWorld().getName());
        getConfig().set("spawn.x", l.getX());
        getConfig().set("spawn.y", l.getY());
        getConfig().set("spawn.z", l.getZ());
        getConfig().set("spawn.yaw", l.getYaw());
        getConfig().set("spawn.pitch", l.getPitch());
        saveConfig();
    }

    // ---------------- Commands ----------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("nerithmccombat.setspawn")) {
                player.sendMessage(color("&cNo permission."));
                return true;
            }
            saveSpawn(player.getLocation());
            player.sendMessage(color("&aSpawn set."));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("spawn")) {
            startSpawnTeleport(player);
            return true;
        }
        return false;
    }

    private void startSpawnTeleport(Player player) {
        if (spawn == null) {
            player.sendMessage(color("&cSpawn is not set."));
            return;
        }
        UUID id = player.getUniqueId();
        cancelTeleport(id, false);

        teleportStart.put(id, player.getLocation().clone());

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            int seconds = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelTeleport(id, false);
                    return;
                }
                player.sendActionBar(color("&aTeleport in &f" + seconds + " &7don't move"));
                if (seconds <= 0) {
                    cancelTeleport(id, false);
                    player.teleport(getSpawn());
                    return;
                }
                seconds--;
            }
        }.runTaskTimer(this, 0L, 20L);

        teleporting.put(id, task);
    }

    public boolean isTeleporting(UUID id) {
        return teleporting.containsKey(id);
    }

    public Location getTeleportStart(UUID id) {
        return teleportStart.get(id);
    }

    public void cancelTeleport(UUID id, boolean clearBar) {
        BukkitTask t = teleporting.remove(id);
        if (t != null) t.cancel();
        teleportStart.remove(id);
        if (clearBar) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendActionBar(Component.empty());
        }
    }
}
