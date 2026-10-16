package com.balugaq.netex.api.groups;

import com.balugaq.netex.utils.GuideUtil;
import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.utils.Keys;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Final_ROOT
 * @since 2.0
 */
public class MainItemGroup extends FlexItemGroup {
    private static final int BACK_SLOT = 1;
    private static final int PREVIOUS_SLOT = 3;
    private static final int NEXT_SLOT = 5;
    private static final int ICON_SLOT = 7;
    // Only the header row has a border; the content area is left empty on purpose.
    private static final int[] BORDER = new int[]{0, 2, 4, 6, 8};
    // Layout: every category and its sub groups are laid out as one continuous flow,
    // left to right, wrapping to the next row when full (no gaps, no empty rows).
    //   e.g. [Category A][a1][a2][a3][a4][Category B][b1][b2][b3]
    private static final int COLUMNS = 9;
    private static final int FIRST_ROW = 1;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private final @Nullable ItemStack item;
    private final int page;
    private List<ItemGroup> fatherItemGroupList = new ArrayList<>();
    private List<List<ItemGroup>> sonItemGroupList = new ArrayList<>();
    private Map<Integer, MainItemGroup> pageMap = new LinkedHashMap<>();

    public MainItemGroup(@NotNull NamespacedKey key, @Nullable ItemStack item, int tier) {
        super(key, ItemStackUtil.getCleanItem(item), tier);
        this.page = 1;
        this.item = item;
        this.pageMap.put(1, this);
    }

    private MainItemGroup(@NotNull NamespacedKey key, @Nullable ItemStack item, int tier, int page) {
        super(key, ItemStackUtil.getCleanItem(item), tier);
        this.page = page;
        this.item = item;
    }

    @Override
    public boolean isVisible(@NotNull Player p, @NotNull PlayerProfile profile, @NotNull SlimefunGuideMode layout) {
        return layout.equals(SlimefunGuideMode.SURVIVAL_MODE) && this.page == 1;
    }

    @Override
    public void open(
        @NotNull Player player,
        @NotNull PlayerProfile playerProfile,
        @NotNull SlimefunGuideMode slimefunGuideMode) {
        playerProfile.getGuideHistory().add(this, this.page);
        this.generateMenu(player, playerProfile, slimefunGuideMode).open(player);
    }

    @Override
    public void register(@NotNull SlimefunAddon addon) {
        super.register(addon);
        for (int i = 0; i < this.fatherItemGroupList.size(); i++) {
            this.fatherItemGroupList.get(i).register(addon);
            for (ItemGroup itemGroup : this.sonItemGroupList.get(i)) {
                itemGroup.register(addon);
            }
        }
    }

    public void addTo(@NotNull ItemGroup fatherItemGroup, @NotNull ItemGroup @NotNull ... itemGroups) {
        if (this.fatherItemGroupList.contains(fatherItemGroup)
            && this.sonItemGroupList.size() > this.fatherItemGroupList.indexOf(fatherItemGroup)) {
            this.sonItemGroupList
                .get(this.fatherItemGroupList.indexOf(fatherItemGroup))
                .addAll(Arrays.stream(itemGroups).toList());
        } else if (!this.fatherItemGroupList.contains(fatherItemGroup)) {
            this.fatherItemGroupList.add(fatherItemGroup);
            this.sonItemGroupList.add(new ArrayList<>(Arrays.stream(itemGroups).toList()));
        }
    }

    @SuppressWarnings("deprecation")
    @NotNull
    private ChestMenu generateMenu(
        @NotNull Player player,
        @NotNull PlayerProfile playerProfile,
        @NotNull SlimefunGuideMode slimefunGuideMode) {
        ChestMenu chestMenu = new ChestMenu(Lang.getString("groups.expansion.slimefun.main"));

        chestMenu.setEmptySlotsClickable(false);
        chestMenu.addMenuOpeningHandler(pl -> pl.playSound(pl.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1));

        chestMenu.addItem(BACK_SLOT, ItemStackUtil.getCleanItem(ChestMenuUtils.getBackButton(player)));
        chestMenu.addMenuClickHandler(BACK_SLOT, (pl, s, is, action) -> {
            GuideHistory guideHistory = playerProfile.getGuideHistory();
            if (action.isShiftClicked()) {
                SlimefunGuide.openMainMenu(playerProfile, slimefunGuideMode, guideHistory.getMainMenuPage());
            } else {
                guideHistory.goBack(Slimefun.getRegistry().getSlimefunGuide(slimefunGuideMode));
            }
            return false;
        });

        if (this.totalPages() > 1) {
            chestMenu.addItem(
                PREVIOUS_SLOT,
                ItemStackUtil.getCleanItem(ChestMenuUtils.getPreviousButton(
                    player, this.page, this.totalPages())));
            chestMenu.addMenuClickHandler(PREVIOUS_SLOT, (p, slot, item, action) -> {
                GuideUtil.removeLastEntry(playerProfile.getGuideHistory());
                MainItemGroup mainItemGroup = this.getByPage(Math.max(this.page - 1, 1));
                mainItemGroup.open(player, playerProfile, slimefunGuideMode);
                return false;
            });

            chestMenu.addItem(
                NEXT_SLOT,
                ItemStackUtil.getCleanItem(ChestMenuUtils.getNextButton(
                    player, this.page, this.totalPages())));
            chestMenu.addMenuClickHandler(NEXT_SLOT, (p, slot, item, action) -> {
                GuideUtil.removeLastEntry(playerProfile.getGuideHistory());
                MainItemGroup mainItemGroup = this.getByPage(
                    Math.min(this.page + 1, this.totalPages()));
                mainItemGroup.open(player, playerProfile, slimefunGuideMode);
                return false;
            });
        } else {
            // Only one page: arrows are useless, keep the header bar clean.
            for (int arrowSlot : new int[]{PREVIOUS_SLOT, NEXT_SLOT}) {
                chestMenu.addItem(arrowSlot, ItemStackUtil.getCleanItem(ChestMenuUtils.getBackground()));
                chestMenu.addMenuClickHandler(arrowSlot, ChestMenuUtils.getEmptyClickHandler());
            }
        }

        chestMenu.addItem(ICON_SLOT, ItemStackUtil.getCleanItem(super.item));
        chestMenu.addMenuClickHandler(ICON_SLOT, ChestMenuUtils.getEmptyClickHandler());

        for (int slot : BORDER) {
            chestMenu.addItem(slot, ItemStackUtil.getCleanItem(ChestMenuUtils.getBackground()));
            chestMenu.addMenuClickHandler(slot, ChestMenuUtils.getEmptyClickHandler());
        }

        List<ItemGroup> entries = this.flatEntries();
        int firstIndex = (this.page - 1) * PAGE_SIZE;
        for (int k = 0; k < PAGE_SIZE; k++) {
            int i = firstIndex + k;
            if (i >= entries.size()) {
                break;
            }

            ItemGroup group = entries.get(i);
            int target = FIRST_ROW * COLUMNS + k;
            chestMenu.addItem(target, ItemStackUtil.getCleanItem(group.getItem(player)));
            chestMenu.addMenuClickHandler(target, (p, s, is, action) -> {
                if (group instanceof FlexItemGroup flexItemGroup) {
                    flexItemGroup.open(player, playerProfile, slimefunGuideMode);
                }
                return false;
            });
        }

        return chestMenu;
    }

    /**
     * Flattens the groups into one list: category, its sub groups, next category, its sub groups, ...
     */
    private List<ItemGroup> flatEntries() {
        List<ItemGroup> entries = new ArrayList<>();
        for (int i = 0; i < this.fatherItemGroupList.size(); i++) {
            entries.add(this.fatherItemGroupList.get(i));
            if (i < this.sonItemGroupList.size()) {
                entries.addAll(this.sonItemGroupList.get(i));
            }
        }
        return entries;
    }

    private int totalPages() {
        return (this.flatEntries().size() - 1) / PAGE_SIZE + 1;
    }

    @NotNull
    public MainItemGroup getByPage(int page) {
        if (pageMap.containsKey(page)) {
            return pageMap.get(page);
        } else {
            synchronized (this.pageMap.get(1)) {
                if (pageMap.containsKey(page)) {
                    return pageMap.get(page);
                }
                MainItemGroup mainItemGroup = pageMap.get(1);
                mainItemGroup = new MainItemGroup(
                    Keys.newKey(this.getKey().getKey() + "_" + page),
                    mainItemGroup.item,
                    mainItemGroup.getTier(),
                    page);
                mainItemGroup.fatherItemGroupList = this.fatherItemGroupList;
                mainItemGroup.sonItemGroupList = this.sonItemGroupList;
                mainItemGroup.pageMap = this.pageMap;
                pageMap.put(page, mainItemGroup);
                return mainItemGroup;
            }
        }
    }
}
