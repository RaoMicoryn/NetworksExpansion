package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Data;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Options for matching items (MatchOption).
 * <p>
 * All {@code itemsMatch} overloads eventually call
 * {@link StackUtils#itemsMatch(ItemStackCache, ItemStack, MatchOption)}，
 * The "ignore" options described here decide which item properties are checked.
 * <p>
 * Each "ignore" option is stored as a bit in one {@code long}. To add a new option later, just add another bit.
 * <p>
 * The default is {@link #DEFAULT}: it keeps the existing behavior, ignoring amount and custom model data but checking other properties.
 *
 * @see StackUtils#itemsMatch(ItemStackCache, ItemStack, MatchOption)
 * @author balugaq
 */
@Data
public class MatchOption {
    // <editor-fold desc="Ignore flags; add new flags here">
    /** Ignore item damage. */
    public static final long IGNORE_DURABILITY = 1L;
    /** Ignore item enchantments. */
    public static final long IGNORE_ENCHANTMENT = 1L << 1;
    /** Ignore item attributes. */
    public static final long IGNORE_ATTRIBUTE = 1L << 2;
    /** Ignore item lore. */
    public static final long IGNORE_LORE = 1L << 3;
    /** Ignore custom model data. */
    public static final long IGNORE_CUSTOM_MODEL_DATA = 1L << 4;
    /** Ignore item amount. */
    @ApiStatus.Internal
    public static final long IGNORE_AMOUNT = 1L << 5; // Does NOT open to players
    // </editor-fold>

    private static final long IGNORE_MASK =
        IGNORE_DURABILITY | IGNORE_ENCHANTMENT | IGNORE_ATTRIBUTE | IGNORE_LORE | IGNORE_CUSTOM_MODEL_DATA | IGNORE_AMOUNT;

    /** Do not ignore anything. */
    public static final long NO_IGNORE = 0L;

    /** Default option: keeps the same behavior as {@code itemsMatch(cache, item)} before the changes. */
    public static final MatchOption DEFAULT = new MatchOption(IGNORE_AMOUNT | IGNORE_CUSTOM_MODEL_DATA);

    private final long ignoreFlags;

    private MatchOption(long ignoreFlags) {
        this.ignoreFlags = ignoreFlags & IGNORE_MASK;
    }

    public static @NotNull MatchOption of(long ignoreFlags) {
        return new MatchOption(ignoreFlags);
    }

    public @NotNull MatchOption withIgnore(long flag, boolean ignored) {
        final long flags = ignored ? (ignoreFlags | flag) : (ignoreFlags & ~flag);
        return new MatchOption(flags);
    }

    public boolean isIgnoreAmount() {
        return (ignoreFlags & IGNORE_AMOUNT) != 0;
    }

    public boolean isIgnoreDurability() {
        return (ignoreFlags & IGNORE_DURABILITY) != 0;
    }

    public boolean isIgnoreEnchantment() {
        return (ignoreFlags & IGNORE_ENCHANTMENT) != 0;
    }

    public boolean isIgnoreAttribute() {
        return (ignoreFlags & IGNORE_ATTRIBUTE) != 0;
    }

    public boolean isIgnoreLore() {
        return (ignoreFlags & IGNORE_LORE) != 0;
    }

    public boolean isIgnoreCustomModelData() {
        return (ignoreFlags & IGNORE_CUSTOM_MODEL_DATA) != 0;
    }

    /** No ignore options are enabled. */
    public boolean isEmpty() {
        return ignoreFlags == NO_IGNORE;
    }

    /** At least one ignore option is enabled. */
    public boolean isAnyIgnore() {
        return ignoreFlags != NO_IGNORE;
    }

    public static final Long2ObjectOpenHashMap<Set<DataComponentType>> cacheSets = new Long2ObjectOpenHashMap<>();
    static {
        cacheSets.defaultReturnValue(null);
    }

    public Set<DataComponentType> toDataComponentSet() {
        var set = cacheSets.get(ignoreFlags);
        if (set != null) return set;
        set = new HashSet<>();
        set.add(DataComponentTypes.LORE); // always ignore lore since it is served for StackUtils
        if (isIgnoreAttribute()) set.add(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (isIgnoreDurability()) set.add(DataComponentTypes.DAMAGE);
        if (isIgnoreEnchantment()) set.add(DataComponentTypes.ENCHANTMENTS);
        if (isIgnoreCustomModelData()) set.add(DataComponentTypes.CUSTOM_MODEL_DATA);
        cacheSets.put(ignoreFlags, set);
        return set;
    }
}
