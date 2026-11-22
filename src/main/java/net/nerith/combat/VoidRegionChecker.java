package net.nerith.combat;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

public class VoidRegionChecker {

    public static boolean isInVoidRegion(Location loc) {
        try {
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(loc));
            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase("void")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
