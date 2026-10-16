package io.github.sefiraat.networks.network.barrel;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.ReflectionUtil;
import io.gitee.crankygoudan.tsingshantechnology.items.chucun.StorageCache;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Network barrel adapter for the TsingshanTechnology (Qingshan Technology) storage unit.
 * Its storage layout is almost the same as the InfinityExpansion StorageUnit:
 * input slot 10, display slot 13, output slot 16; the amount is saved under the "sstored" key,
 * and the displayed item has the tsingshantechnology-fixed:display PDC tag.
 */
public class TsingshanBarrel extends BarrelIdentity {

    @NotNull
    private final StorageCache cache;

    @ParametersAreNonnullByDefault
    public TsingshanBarrel(Location location, @Nullable ItemStack itemStack, long amount, StorageCache cache) {
        super(location, itemStack, amount, TsingshanBarrel.getLimit(cache), BarrelType.TSINGSHAN_TECHNOLOGY);
        this.cache = cache;
    }

    private static long getLimit(StorageCache cache) {
        try {
            return ReflectionUtil.getValue(ReflectionUtil.getValue(cache, "storageUnit"), "max", int.class);
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Nullable
    @Override
    public ItemStack requestItem(@NotNull ItemRequest itemRequest) {
        BlockMenu blockMenu = StorageCacheUtils.getMenu(this.getLocation());
        return blockMenu == null ? null : blockMenu.getItemInSlot(this.getOutputSlot()[0]);
    }

    public static ItemStack getActualItemStack(BlockMenu menu) {
        final ItemStack rawDisplayItem = menu.getItemInSlot(13);
        if (rawDisplayItem == null || rawDisplayItem.getType() == Material.AIR) {
            return null;
        }

        final ItemStack displayItem = rawDisplayItem.clone();
        if (!displayItem.hasItemMeta()) {
            return null;
        }
        final ItemMeta displayItemMeta = displayItem.getItemMeta();
        if (displayItemMeta == null) {
            return null;
        }

        Byte correct = DataTypeMethods.getCustom(displayItemMeta, Keys.TSINGSHAN_DISPLAY, PersistentDataType.BYTE);
        if (correct == null || correct != 1) {
            return null;
        }

        displayItemMeta.getPersistentDataContainer().remove(Keys.TSINGSHAN_DISPLAY);
        displayItem.setItemMeta(displayItemMeta);

        return displayItem;
    }

    @Override
    public void depositItemStack(ItemStack[] itemsToDeposit) {
        cache.depositAll(itemsToDeposit, true);
    }

    @Override
    public int[] getInputSlot() {
        return new int[]{10};
    }

    @Override
    public int[] getOutputSlot() {
        return new int[]{16};
    }
}
