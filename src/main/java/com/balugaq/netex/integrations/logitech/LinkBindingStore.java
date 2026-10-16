package com.balugaq.netex.integrations.logitech;

import com.balugaq.netex.utils.Debug;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.api.DriveType;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.unit.NetworksDrawer;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Quantum-link binding records: binding samples are saved in the target block's own block data (key {@link #KEY}),
 * target locations are also saved in a global index (linker-bindings.yml). On startup, {@link #replayAll} uses the index to
 * re-register all virtual caches. Entries whose Slimefun block data is not ready during startup are put aside and retried
 * (once every 100 ticks, up to {@link #MAX_REPLAY_ROUNDS} rounds). An entry is only removed as invalid when its data has loaded
 * and its type does not match.
 */
public final class LinkBindingStore {

    private static final String KEY = "LinkBindings";
    private static final int MAX_ENTRIES = 64;
    private static final String INDEX_FILE = "linker-bindings.yml";
    private static final String INDEX_KEY = "targets";

    private LinkBindingStore() {
    }

    static void record(@NotNull Location target, @NotNull ItemStack sample) {
        List<String> encoded = readEntries(target);
        String entry = Base64.getEncoder().encodeToString(sample.serializeAsBytes());
        if (encoded.contains(entry)) {
            indexAdd(target);
            return;
        }
        if (encoded.size() >= MAX_ENTRIES) {
            encoded.remove(0);
        }
        encoded.add(entry);
        StorageCacheUtils.setData(target, KEY, String.join(",", encoded));
        indexAdd(target);
    }

    static int replay(@NotNull Location target, @NotNull Consumer<ItemStack> injector) {
        int injected = 0;
        for (String entry : readEntries(target)) {
            try {
                ItemStack sample = ItemStack.deserializeBytes(Base64.getDecoder().decode(entry));
                injector.accept(sample);
                injected++;
            } catch (Throwable e) {
                Debug.debug("Broken quantum-link binding record, skipping: " + e.getMessage());
            }
        }
        return injected;
    }

    private static @NotNull List<String> readEntries(@NotNull Location target) {
        String data;
        try {
            data = StorageCacheUtils.getData(target, KEY);
        } catch (IllegalStateException e) {
            return new ArrayList<>();
        }
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> entries = new ArrayList<>(List.of(data.split(",", -1)));
        entries.removeIf(String::isEmpty);
        return entries;
    }

    public static void replayDrive(@NotNull Location driveLocation) {
        if (!LinkerGrid.initialized) {
            return;
        }
        if (replay(driveLocation, sample -> CellVirtualCache.inject(driveLocation, new ItemKey(sample))) > 0) {
            indexAdd(driveLocation);
        }
    }

    public static void replayDrawer(@NotNull Location drawerLocation) {
        if (!LinkerGrid.initialized) {
            return;
        }
        if (replay(drawerLocation, sample -> DrawerVirtualCache.inject(drawerLocation, sample)) > 0) {
            indexAdd(drawerLocation);
        }
    }

    public static void forget(@NotNull Location target) {
        List<String> entries = new ArrayList<>(readIndex());
        if (entries.remove(encode(target))) {
            writeIndex(entries);
        }
    }

    private static int replayRounds;
    private static final int MAX_REPLAY_ROUNDS = 600;

    public static void replayAll() {
        if (!LinkerGrid.initialized) {
            return;
        }
        List<String> entries = readIndex();
        if (entries.isEmpty()) {
            return;
        }
        List<String> alive = new ArrayList<>();
        List<String> deferred = new ArrayList<>();
        for (String entry : entries) {
            Location target = decode(entry);
            if (target == null) {
                deferred.add(entry);
                continue;
            }
            SlimefunItem item;
            try {
                item = StorageCacheUtils.getSfItem(target);
            } catch (IllegalStateException e) {
                deferred.add(entry);
                continue;
            }
            if (item == null) {
                deferred.add(entry);
                continue;
            }
            if (DriveType.of(item) != null) {
                replay(target, sample -> CellVirtualCache.inject(target, new ItemKey(sample)));
                alive.add(entry);
            } else if (item instanceof NetworksDrawer) {
                replay(target, sample -> DrawerVirtualCache.inject(target, sample));
                alive.add(entry);
            }
        }
        alive.addAll(deferred);
        if (alive.size() != entries.size()) {
            writeIndex(alive);
        }
        if (deferred.isEmpty() || ++replayRounds >= MAX_REPLAY_ROUNDS) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(Networks.getInstance(), LinkBindingStore::replayAll, 100L);
    }

    private static void indexAdd(@NotNull Location target) {
        List<String> entries = new ArrayList<>(readIndex());
        String entry = encode(target);
        if (entries.contains(entry)) {
            return;
        }
        entries.add(entry);
        writeIndex(entries);
    }

    private static @NotNull List<String> readIndex() {
        File file = indexFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(YamlConfiguration.loadConfiguration(file).getStringList(INDEX_KEY));
    }

    private static void writeIndex(@NotNull List<String> entries) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(INDEX_KEY, entries);
        try {
            yaml.save(indexFile());
        } catch (IOException e) {
            Networks.getInstance().getLogger().log(Level.WARNING, "Failed to save the quantum-link binding index", e);
        }
    }

    private static @NotNull File indexFile() {
        return new File(Networks.getInstance().getDataFolder(), INDEX_FILE);
    }

    private static @NotNull String encode(@NotNull Location location) {
        World world = location.getWorld();
        return world.getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    private static Location decode(@NotNull String entry) {
        String[] parts = entry.split(";");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
