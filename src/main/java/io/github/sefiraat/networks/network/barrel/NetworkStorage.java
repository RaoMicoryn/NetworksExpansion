package io.github.sefiraat.networks.network.barrel;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.StackUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkStorage extends BarrelIdentity {
    public NetworkStorage(@NotNull Location location, ItemStack itemStack, long amount) {
        super(location, itemStack, amount, amount, BarrelType.NETWORKS);
    }

    public NetworkStorage(@NotNull Location location, ItemStack itemStack, long amount, long limit) {
        super(location, itemStack, amount, limit, BarrelType.NETWORKS);
    }

    @Override
    @Nullable
    public ItemStack requestItem(@NotNull ItemRequest itemRequest) {
        final BlockMenu blockMenu = StorageCacheUtils.getMenu(this.getLocation());

        if (blockMenu == null) {
            return null;
        }

        final QuantumCache cache = NetworkQuantumStorage.getCaches().get(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        // Check that the snapshot still matches: getItemStack0/getItemStacksBatch0 match requests using the item snapshot from the barrel scan,
        // but this method reads the live cache. If the storage is emptied and then filled with a different item between the two accesses
        // (the root only lives for one tick, so this can happen within the same tick), taking items right away could give the wrong item.
        // If the check fails, skip this request (nothing has been taken yet, so nothing is lost) and let the outer code try another barrel.
        final ItemStack cacheItem = cache.getItemStack();
        if (cacheItem != null && !StackUtils.itemsMatch(itemRequest.getItemStack(), cacheItem)) {
            return null;
        }

        return NetworkQuantumStorage.getItemStack(cache, blockMenu, itemRequest.getAmount());
    }

    @Override
    public void depositItemStack(ItemStack @NotNull [] itemsToDeposit) {
        if (StorageCacheUtils.getSfItem(this.getLocation()) instanceof NetworkQuantumStorage) {
            final BlockMenu blockMenu = StorageCacheUtils.getMenu(this.getLocation());
            if (blockMenu == null) {
                return;
            }
            final QuantumCache cache = NetworkQuantumStorage.getCaches().get(this.getLocation());
            if (cache != null) {
                NetworkQuantumStorage.tryInputItem(blockMenu.getLocation(), itemsToDeposit, cache);
            }
        }
    }

    @Override
    public int[] getInputSlot() {
        return new int[]{NetworkQuantumStorage.INPUT_SLOT};
    }

    @Override
    public int[] getOutputSlot() {
        return new int[]{NetworkQuantumStorage.OUTPUT_SLOT};
    }
}
