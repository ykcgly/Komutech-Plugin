package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.combat.ScrollCombatEngine;
import tech.komutech.native_scripts.storage.WanXiangGuiStorage;
import tech.komutech.native_scripts.storage.WanYanYiMachineStorage;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MachineMenuGuard;
import tech.komutech.native_scripts.support.MainThread;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.NativeScript;

public final class WanYanYiScript implements NativeScript, NativeLifecycleScript, KomutechMenuHandler {
   private static final String SEARCH_ITEM_ID = "MAGIC_EXPANSION_ITEM_NAME_TAG";
   private static final int INPUT_SLOT = 49;
   private static final int LOAD_BUTTON = 53;
   private static final int PREV_SLOT = 48;
   private static final int NEXT_SLOT = 50;
   private static final int SORT_SLOT = 47;
   private static final int SEARCH_SLOT = 51;
   private static final double WITHDRAW_BASE_PER_ITEM = 0.35;
   private static final double WITHDRAW_GENGU_SCALE = 0.25;
   private static final Boolean BLOCK_CLICK = Boolean.FALSE;
   private final Map<Player, WanYanYiScript.Session> openPlayers = new HashMap<>();
   private static final Map<UUID, WanYanYiScript.SavedState> savedStates = new ConcurrentHashMap<>();
   private final Map<UUID, Long> handledClickTicks = new ConcurrentHashMap<>();

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
      var1.getServer().getPluginManager().registerEvents(new Listener() {
         @EventHandler
         public void onInventoryOpen(InventoryOpenEvent var1) {
            if (var1.getPlayer() instanceof Player var2) {
               if (WanYanYiScript.this.handles(var1.getView())) {
                  KomutechMenuRouter.bindInventory(var1.getView().getTopInventory(), WanYanYiScript.this);
                  WanYanYiScript.this.restoreBoundStorageItem(var2);
               }
            }
         }
      }, var1);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MachineMenuGuard.isOwnMachine(var1, "KOMUTECH_L_ZJ_萬衍儀") ? true : isWanYanYiTitle(MenuGuiHelper.legacyTitle(var1));
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onOpen".equals(var1) && var2[0] instanceof Player var8) {
         this.onOpen(var8);
         return null;
      } else if ("onClose".equals(var1) && var2[0] instanceof Player var7) {
         this.onClose(var7);
         return null;
      } else if ("onClick".equals(var1) && var2.length >= 4 && var2[0] instanceof Player var3) {
         int var11 = var2[1] instanceof Number var5 ? var5.intValue() : -1;
         if (!this.handles(var3.getOpenInventory())) {
            return null;
         } else if (var11 == 49) {
            this.scheduleInputSlotSync(var3);
            return null;
         } else {
            this.handleScriptClick(var3, var11, var2[3]);
            return BLOCK_CLICK;
         }
      } else {
         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         InventoryView var6 = var1.getView();
         int var4 = var6.getTopInventory().getSize();
         int var5 = var1.getRawSlot();
         if (var1.getClickedInventory() == var6.getBottomInventory() && var1.isShiftClick()) {
            var1.setCancelled(true);
         } else if (var5 < 0 || var5 >= var4) {
            if (var1.getAction() == InventoryAction.COLLECT_TO_CURSOR
               || var1.getAction() == InventoryAction.UNKNOWN && var1.getClick() == ClickType.DOUBLE_CLICK) {
               var1.setCancelled(true);
            }
         } else if (allowInputSlotInteraction(var1)) {
            var1.setCancelled(false);
            this.scheduleInputSlotSync(var2);
         } else {
            var1.setCancelled(true);
            if (var5 >= 0 && var5 < var4) {
               this.handleScriptClick(var2, var5, var1);
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      MachineMenuGuard.protectTopInventoryDrag(var1, var0 -> var0 == 49);
      if (!var1.isCancelled()) {
         if (var1.getRawSlots().contains(49) && var1.getWhoClicked() instanceof Player var2) {
            this.scheduleInputSlotSync(var2);
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         this.handledClickTicks.remove(var2.getUniqueId());
         WanYanYiScript.Session var4 = this.openPlayers.remove(var2);
         if (var4 != null) {
            persistSession(var2, var4, var1.getView().getTopInventory());
         }

         KomutechMenuRouter.unbindInventory(var1.getView().getTopInventory());
      }
   }

   private void onClose(Player var1) {
      this.handledClickTicks.remove(var1.getUniqueId());
      WanYanYiScript.Session var2 = this.openPlayers.remove(var1);
      if (var2 != null) {
         Inventory var3 = var1.getOpenInventory().getTopInventory();
         if (this.handles(var1.getOpenInventory())) {
            persistSession(var1, var2, var3);
         } else {
            saveState(var1, var2);
         }
      }
   }

   private void onOpen(Player var1) {
      Inventory var2 = var1.getOpenInventory().getTopInventory();
      KomutechMenuRouter.bindInventory(var2, this);
      this.restoreBoundStorageItem(var1);
      WanXiangGuiStorage.ReadResult var3 = getStorageData(var2, var1.getName());
      if (var3 != null && !var3.unnamed()) {
         WanYanYiScript.LoadedState var4 = loadState(var1, var3);
         WanYanYiScript.Session var5 = new WanYanYiScript.Session();
         var5.page = var4.page;
         var5.mode = var4.mode;
         var5.searchData = var4.searchData;
         var5.keyword = var4.keyword;
         var5.loaded = var3.data();
         var5.storageKey = WanXiangGuiStorage.getStorageName(var2.getItem(49));
         this.openPlayers.put(var1, var5);
         fillGui(var2, var3.data(), var4.page, var4.mode, var4.searchData, var4.keyword);
      } else {
         fillEmpty(var2);
         this.openPlayers.put(var1, new WanYanYiScript.Session());
         if (var3 != null && var3.unnamed()) {
            KomutechSupport.send(var1, "§c萬象匱未命名，无法加载数据。");
         }
      }
   }

   private void restoreBoundStorageItem(Player var1) {
      Location var2 = resolveMachineLocation(var1);
      if (var2 != null) {
         Inventory var3 = var1.getOpenInventory().getTopInventory();
         ItemStack var4 = var3.getItem(49);
         if (var4 != null && !var4.getType().isAir()) {
            WanYanYiMachineStorage.save(var2, var4);
         } else {
            ItemStack var5 = WanYanYiMachineStorage.load(var2);
            if (var5 != null) {
               var3.setItem(49, var5);
            }
         }
      }
   }

   private boolean handleScriptClick(Player var1, int var2, Object var3) {
      long var4 = Bukkit.getCurrentTick();
      Long var6 = this.handledClickTicks.put(var1.getUniqueId(), var4);
      if (var6 != null && var6 == var4) {
         return true;
      } else {
         Inventory var7 = var1.getOpenInventory().getTopInventory();
         WanYanYiScript.Session var8 = this.ensureSession(var1, var7);
         if (var8 == null) {
            return true;
         } else if (var2 == 49) {
            return false;
         } else if (var2 >= 45 && var2 < 54) {
            this.handleControl(var1, var7, var8, var2);
            return true;
         } else if (var2 >= 0 && var2 < 45) {
            this.handleWithdraw(var1, var8, var2, var3);
            return true;
         } else {
            return true;
         }
      }
   }

   private WanYanYiScript.Session ensureSession(Player var1, Inventory var2) {
      WanYanYiScript.Session var3 = this.openPlayers.get(var1);
      if (var3 != null) {
         return var3;
      } else {
         WanXiangGuiStorage.ReadResult var4 = getStorageData(var2, var1.getName());
         if (var4 != null && !var4.unnamed()) {
            WanYanYiScript.LoadedState var5 = loadState(var1, var4);
            var3 = new WanYanYiScript.Session();
            var3.page = var5.page;
            var3.mode = var5.mode;
            var3.searchData = var5.searchData;
            var3.keyword = var5.keyword;
            var3.loaded = var4.data();
            var3.storageKey = WanXiangGuiStorage.getStorageName(var2.getItem(49));
            this.openPlayers.put(var1, var3);
            return var3;
         } else {
            KomutechSupport.send(var1, "§c请先在49号槽放入已命名的萬象匱");
            return null;
         }
      }
   }

   private void handleControl(Player var1, Inventory var2, WanYanYiScript.Session var3, int var4) {
      if (var4 == 53) {
         WanXiangGuiStorage.ReadResult var11 = getStorageData(var2, var1.getName());
         if (var11 != null && !var11.unnamed()) {
            var3.loaded = var11.data();
            var3.storageKey = WanXiangGuiStorage.getStorageName(var2.getItem(49));
            var3.mode = "normal";
            var3.searchData = null;
            var3.keyword = "";
            var3.page = "1";
            fillGui(var2, var11.data(), "1", "normal", null, "");
            var1.playSound(var1.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
            var1.spawnParticle(Particle.HAPPY_VILLAGER, var1.getLocation().add(0.0, 1.0, 0.0), 8, 0.4, 0.3, 0.4, 0.0);
            int var12 = countPages(var11.data());
            int var13 = WanXiangGuiStorage.countStoredItems(var11.data());
            KomutechSupport.send(var1, "§a已加载萬象匱内容 §7(共 " + var12 + " 页 / " + var13 + " 种物品)");
         } else {
            KomutechSupport.send(var1, "§c请在49号槽放入已命名的萬象匱");
         }
      } else if (var3.loaded == null) {
         KomutechSupport.send(var1, "§c请先点击加载按钮");
      } else {
         Map var5 = "search".equals(var3.mode) ? var3.searchData : var3.loaded;
         int var6 = parsePage(var3.page);
         if (var4 == 48 && hasPage(var5, String.valueOf(var6 - 1))) {
            var3.page = String.valueOf(var6 - 1);
            fillGui(var2, var3.loaded, var3.page, var3.mode, var3.searchData, var3.keyword);
         } else if (var4 == 50 && hasPage(var5, String.valueOf(var6 + 1))) {
            var3.page = String.valueOf(var6 + 1);
            fillGui(var2, var3.loaded, var3.page, var3.mode, var3.searchData, var3.keyword);
         } else if (var4 == 47 && "normal".equals(var3.mode)) {
            Map var7 = WanXiangGuiStorage.sortAll(var3.loaded);
            var3.loaded = var7;
            ItemStack var8 = var2.getItem(49);
            WanXiangGuiStorage.writeForItem(var8, var1.getName(), var7);
            String var9 = String.valueOf(var6);
            List var10 = KomutechJson.asList(KomutechJson.asMap(var7.get("pages")).get(var9));
            var3.page = var10 != null && var10.subList(0, Math.min(45, var10.size())).stream().anyMatch(var0 -> var0 != null) ? var9 : "1";
            fillGui(var2, var7, var3.page, "normal", null, "");
            var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.5F);
            KomutechSupport.send(var1, "§a整理完成！");
         } else if (var4 == 51) {
            this.handleSearch(var1, var2, var3);
         }
      }
   }

   private void handleSearch(Player var1, Inventory var2, WanYanYiScript.Session var3) {
      ItemStack var4 = findSearchItem(var1);
      if (var4 == null) {
         KomutechSupport.send(var1, "§c你没有搜索道具，请手持§a魔法2的便携式命名牌 §c或已§a命名的命名牌后点击搜索");
      } else {
         ItemMeta var5 = var4.getItemMeta();
         if (var5 != null && var5.hasDisplayName()) {
            String var6 = ChatColor.stripColor(var5.getDisplayName()).trim();
            if (var6.isEmpty()) {
               KomutechSupport.send(var1, "§c搜索道具名称为空");
            } else {
               Map var7 = WanXiangGuiStorage.search(var3.loaded, var6);
               List var8 = KomutechJson.asList(KomutechJson.asMap(var7.get("pages")).get("1"));
               if (var8 != null && !var8.isEmpty() && var8.get(0) != null) {
                  var3.mode = "search";
                  var3.searchData = var7;
                  var3.keyword = var6;
                  var3.page = "1";
                  fillGui(var2, var3.loaded, "1", "search", var7, var6);
                  int var9 = 0;

                  for (Object var11 : KomutechJson.asMap(var7.get("pages")).values()) {
                     for (Object var13 : KomutechJson.asList(var11)) {
                        if (var13 != null) {
                           var9++;
                        }
                     }
                  }

                  KomutechSupport.send(var1, "§a找到 " + var9 + " 个物品");
               } else {
                  KomutechSupport.send(var1, "§c没有找到匹配的物品");
               }
            }
         } else {
            KomutechSupport.send(var1, "§c搜索道具没有名称");
         }
      }
   }

   private void handleWithdraw(Player var1, WanYanYiScript.Session var2, int var3, Object var4) {
      if (var2.loaded == null) {
         KomutechSupport.send(var1, "§c请先点击加载按钮");
      } else {
         Map var5 = "search".equals(var2.mode) && var2.searchData != null ? var2.searchData : var2.loaded;
         Map var6 = KomutechJson.asMap(var5.get("pages"));
         List var7 = KomutechJson.asList(var6.get(var2.page));
         if (var7 != null && var3 < var7.size() && var7.get(var3) != null) {
            ItemStack var8 = WanXiangGuiStorage.deserialize(var7.get(var3));
            if (var8 != null) {
               Map var9 = PlayerAttributeStore.load(var1.getName());
               if (var9 == null) {
                  KomutechSupport.send(var1, "§c无法读取修仙属性");
               } else {
                  boolean var10 = isShiftClick(var4);
                  boolean var11 = isRightClick(var4);
                  int var12 = resolveWithdrawAmount(var1, var8, var10, var11);
                  if (var12 <= 0) {
                     KomutechSupport.send(var1, "§c背包已满");
                  } else {
                     int var13 = ScrollCombatEngine.calcGenericSpiritCost(var9, 0.35, var12, 0.25);
                     if (ScrollCombatEngine.tryConsumeSpiritPower(var1, var9, var13)) {
                        if (!deliverWithdraw(var1, var8, var12, var10 && var11)) {
                           KomutechSupport.send(var1, "§c背包已满");
                        } else {
                           PlayerAttributeStore.save(var1.getName(), var9);
                           var1.playSound(var1.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.1F);
                           var1.spawnParticle(Particle.ITEM, var1.getLocation().add(0.0, 1.0, 0.0), 6, 0.2, 0.2, 0.2, 0.05, var8);
                           PlayerAttributeStore.Lingli var14 = PlayerAttributeStore.parseLingli(var9.get("灵力"));
                           KomutechSupport.actionBar(
                              var1, "§a取出 " + var12 + " 个 §7| §b消耗灵力 §6" + var13 + " §7| §f剩余 §6" + String.format("%.0f", var14.current())
                           );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static int resolveWithdrawAmount(Player var0, ItemStack var1, boolean var2, boolean var3) {
      int var4 = var1.getMaxStackSize();
      if (var2 && var3) {
         return computeSimilarCapacity(var0, var1, var4);
      } else if (var2) {
         return var4;
      } else {
         return var3 ? Math.min(8, var4) : 1;
      }
   }

   private static boolean deliverWithdraw(Player var0, ItemStack var1, int var2, boolean var3) {
      if (var3) {
         int var4 = var2;

         while (var4 > 0) {
            int var5 = Math.min(var4, var1.getMaxStackSize());
            HashMap var6 = var0.getInventory().addItem(new ItemStack[]{cloneAmount(var1, var5)});
            if (!var6.isEmpty()) {
               return false;
            }

            var4 -= var5;
         }

         return true;
      } else {
         KomutechSupport.giveOrDrop(var0, cloneAmount(var1, var2));
         return true;
      }
   }

   private static int computeSimilarCapacity(Player var0, ItemStack var1, int var2) {
      PlayerInventory var3 = var0.getInventory();
      int var4 = 0;

      for (int var5 = 0; var5 < 36; var5++) {
         ItemStack var6 = var3.getItem(var5);
         if (var6 == null || var6.getType().isAir()) {
            var4 += var2;
         } else if (var6.isSimilar(var1)) {
            var4 += var2 - var6.getAmount();
         }
      }

      return var4;
   }

   private void scheduleInputSlotSync(Player var1) {
      MainThread.run(KomutechSupport.plugin(), () -> Bukkit.getScheduler().runTaskLater(KomutechSupport.plugin(), () -> this.syncInputSlotState(var1), 1L));
   }

   private void syncInputSlotState(Player var1) {
      if (var1.isOnline()) {
         InventoryView var2 = var1.getOpenInventory();
         if (this.handles(var2)) {
            Inventory var3 = var2.getTopInventory();
            WanYanYiScript.Session var4 = this.openPlayers.computeIfAbsent(var1, var0 -> new WanYanYiScript.Session());
            ItemStack var5 = var3.getItem(49);
            String var6 = null;
            if (WanXiangGuiStorage.isStorageItem(var5)) {
               var6 = WanXiangGuiStorage.getStorageName(var5);
               if (var6 != null && !WanXiangGuiStorage.isValidStorageName(var6)) {
                  var6 = null;
               }
            }

            if (var6 == null) {
               Location var8 = resolveMachineLocation(var1);
               if (var8 != null) {
                  WanYanYiMachineStorage.clear(var8);
               }

               if (var4.loaded != null || var4.storageKey != null) {
                  var4.loaded = null;
                  var4.storageKey = null;
                  var4.mode = "normal";
                  var4.searchData = null;
                  var4.keyword = "";
                  var4.page = "1";
                  fillEmpty(var3);
                  var1.updateInventory();
               } else if (var5 != null && WanXiangGuiStorage.isStorageItem(var5)) {
                  KomutechSupport.send(var1, "§c萬象匱未命名，无法加载数据。");
               }
            } else {
               if (!var6.equals(var4.storageKey)) {
                  var4.storageKey = var6;
                  var4.loaded = null;
                  var4.mode = "normal";
                  var4.searchData = null;
                  var4.keyword = "";
                  var4.page = "1";
                  fillEmpty(var3);
                  Location var7 = resolveMachineLocation(var1);
                  if (var7 != null) {
                     WanYanYiMachineStorage.save(var7, var5);
                  }

                  var1.updateInventory();
               }
            }
         }
      }
   }

   private static ItemStack cloneAmount(ItemStack var0, int var1) {
      ItemStack var2 = var0.clone();
      var2.setAmount(Math.min(var1, var0.getMaxStackSize()));
      return var2;
   }

   private static boolean allowInputSlotInteraction(InventoryClickEvent var0) {
      if (var0.getRawSlot() != 49) {
         return false;
      } else if (var0.getClickedInventory() == var0.getView().getBottomInventory() && var0.isShiftClick()) {
         return false;
      } else {
         ClickType var1 = var0.getClick();
         if (var1 != ClickType.NUMBER_KEY && var1 != ClickType.SWAP_OFFHAND) {
            InventoryAction var2 = var0.getAction();
            if (var2 != InventoryAction.PLACE_ALL
               && var2 != InventoryAction.PLACE_ONE
               && var2 != InventoryAction.PLACE_SOME
               && var2 != InventoryAction.SWAP_WITH_CURSOR) {
               ItemStack var3 = var0.getCursor();
               if (var3 != null && !var3.getType().isAir()) {
                  return true;
               } else {
                  ItemStack var4 = var0.getCurrentItem();
                  return var4 != null && !var4.getType().isAir()
                     ? var2 == InventoryAction.PICKUP_ALL
                        || var2 == InventoryAction.PICKUP_HALF
                        || var2 == InventoryAction.PICKUP_ONE
                        || var2 == InventoryAction.PICKUP_SOME
                        || var2 == InventoryAction.DROP_ALL_SLOT
                        || var2 == InventoryAction.DROP_ONE_SLOT
                     : false;
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      }
   }

   private static boolean isShiftClick(Object var0) {
      return var0 instanceof InventoryClickEvent var1 ? var1.isShiftClick() : KomutechSupport.isShiftClick(var0);
   }

   private static boolean isRightClick(Object var0) {
      if (!(var0 instanceof InventoryClickEvent var1)) {
         return KomutechSupport.isRightClick(var0);
      } else {
         ClickType var2 = var1.getClick();
         return var2 == ClickType.RIGHT || var2 == ClickType.SHIFT_RIGHT;
      }
   }

   private static boolean isWanYanYiTitle(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = ChatColor.stripColor(var0);
         if (!var1.contains("萬衍儀") && !var1.contains("万衍仪") && !var1.contains("万衍仪典")) {
            boolean var2 = var1.contains("萬") || var1.contains("万");
            boolean var3 = var1.contains("衍");
            boolean var4 = var1.contains("儀") || var1.contains("仪") || var1.contains("典");
            return var2 && var3 && var4;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static WanXiangGuiStorage.ReadResult getStorageData(Inventory var0, String var1) {
      ItemStack var2 = var0.getItem(49);
      return WanXiangGuiStorage.isStorageItem(var2) ? WanXiangGuiStorage.readForItem(var2, var1) : null;
   }

   private static ItemStack findSearchItem(Player var0) {
      for (ItemStack var4 : var0.getInventory().getContents()) {
         if (var4 != null && !var4.getType().isAir()) {
            SlimefunItem var5 = SlimefunItem.getByItem(var4);
            if (var5 != null && "MAGIC_EXPANSION_ITEM_NAME_TAG".equals(var5.getId())) {
               return var4;
            }
         }
      }

      for (ItemStack var9 : var0.getInventory().getContents()) {
         if (var9 != null && var9.getType() == Material.NAME_TAG && var9.hasItemMeta() && var9.getItemMeta().hasDisplayName()) {
            return var9;
         }
      }

      return null;
   }

   private static WanYanYiScript.LoadedState loadState(Player var0, WanXiangGuiStorage.ReadResult var1) {
      String var2 = WanXiangGuiStorage.getStorageName(var0.getOpenInventory().getTopInventory().getItem(49));
      if (var2 != null) {
         WanXiangGuiStorage.LoadedView var9 = WanXiangGuiStorage.loadView(var0.getName(), var2);
         return new WanYanYiScript.LoadedState(var9.page(), var9.mode(), var9.searchData(), var9.keyword());
      } else {
         WanYanYiScript.SavedState var3 = savedStates.remove(var0.getUniqueId());
         String var4 = var3 == null ? "normal" : var3.mode;
         String var5 = var3 == null ? "" : var3.keyword;
         String var6 = "1";
         if ("normal".equals(var4) && var3 != null && var3.page != null) {
            var6 = var3.page;
         }

         Map var7 = null;
         if ("search".equals(var4) && var5 != null && !var5.isEmpty() && var1 != null && !var1.unnamed()) {
            var7 = WanXiangGuiStorage.search(var1.data(), var5);
            List var8 = KomutechJson.asList(KomutechJson.asMap(var7.get("pages")).get("1"));
            if (var8 == null || var8.isEmpty() || var8.get(0) == null) {
               var4 = "normal";
               var5 = "";
               var7 = null;
            }

            var6 = "1";
         } else if ("search".equals(var4)) {
            var4 = "normal";
            var5 = "";
         }

         return new WanYanYiScript.LoadedState(var6, var4, var7, var5);
      }
   }

   private static void saveState(Player var0, WanYanYiScript.Session var1) {
      WanYanYiScript.SavedState var2 = new WanYanYiScript.SavedState();
      var2.mode = var1.mode == null ? "normal" : var1.mode;
      var2.keyword = var1.keyword == null ? "" : var1.keyword;
      var2.page = var1.page == null ? "1" : var1.page;
      savedStates.put(var0.getUniqueId(), var2);
   }

   private static void persistSession(Player var0, WanYanYiScript.Session var1, Inventory var2) {
      saveState(var0, var1);
      Location var3 = resolveMachineLocation(var0);
      ItemStack var4 = var2.getItem(49);
      if (var3 != null) {
         WanYanYiMachineStorage.save(var3, var4);
      }

      if (var4 != null && WanXiangGuiStorage.isStorageItem(var4)) {
         String var5 = WanXiangGuiStorage.getStorageName(var4);
         if (var5 != null) {
            Map var6 = var1.loaded;
            if (var6 == null) {
               var6 = WanXiangGuiStorage.read(var0.getName(), var5);
            }

            if (var6 != null) {
               WanXiangGuiStorage.setMeta(var6, var1.page, var1.mode, var1.keyword);
               WanXiangGuiStorage.writeForItem(var4, var0.getName(), var6);
            }
         }
      }
   }

   private static Location resolveMachineLocation(Player var0) {
      try {
         InventoryHolder var1 = var0.getOpenInventory().getTopInventory().getHolder(false);
         if (var1 == null) {
            return null;
         } else {
            return var1.getClass().getMethod("getLocation").invoke(var1) instanceof Location var3 ? var3 : null;
         }
      } catch (ReflectiveOperationException var4) {
         return null;
      }
   }

   private static void fillEmpty(Inventory var0) {
      ItemStack var1 = MenuGuiHelper.item("BLACK_STAINED_GLASS_PANE", " ", null);

      for (int var2 = 0; var2 < 45; var2++) {
         var0.setItem(var2, var1);
      }

      var0.setItem(48, MenuGuiHelper.item("GRAY_STAINED_GLASS_PANE", "§8", null));
      var0.setItem(50, MenuGuiHelper.item("GRAY_STAINED_GLASS_PANE", "§8", null));
      var0.setItem(51, MenuGuiHelper.item("NAME_TAG", "§a搜索", List.of("§7点击使用搜索道具")));
   }

   private static void fillGui(Inventory var0, Map<String, Object> var1, String var2, String var3, Map<String, Object> var4, String var5) {
      if (var1 == null || !var1.containsKey("pages")) {
         var1 = WanXiangGuiStorage.emptyData();
      }

      Map var6 = "search".equals(var3) && var4 != null ? var4 : var1;
      List var7 = KomutechJson.asList(KomutechJson.asMap(var6.get("pages")).get(var2));
      ItemStack var8 = MenuGuiHelper.item("BLACK_STAINED_GLASS_PANE", " ", null);

      for (int var9 = 0; var9 < 45; var9++) {
         Object var10 = var7 != null && var9 < var7.size() ? var7.get(var9) : null;
         ItemStack var11 = WanXiangGuiStorage.deserialize(var10);
         var0.setItem(var9, var11 != null ? var11 : var8);
      }

      int var13 = parsePage(var2);
      int var14 = countPages(var6);
      String var15 = String.valueOf(var13 - 1);
      if (var13 > 1 && hasPage(var6, var15)) {
         var0.setItem(48, MenuGuiHelper.item("ARROW", "§a上一页 §7(" + var13 + "/" + var14 + ")", List.of("§7点击切换到第 " + var15 + " 页")));
      } else {
         var0.setItem(48, MenuGuiHelper.item("GRAY_STAINED_GLASS_PANE", "§8", null));
      }

      String var12 = String.valueOf(var13 + 1);
      if (hasPage(var6, var12)) {
         var0.setItem(50, MenuGuiHelper.item("ARROW", "§a下一页 §7(" + var13 + "/" + var14 + ")", List.of("§7点击切换到第 " + var12 + " 页")));
      } else {
         var0.setItem(50, MenuGuiHelper.item("GRAY_STAINED_GLASS_PANE", "§8第 " + var13 + "/" + var14 + " 页", null));
      }

      if ("normal".equals(var3)) {
         var0.setItem(51, MenuGuiHelper.item("NAME_TAG", "§a搜索", List.of("§7点击使用搜索道具")));
      } else {
         var0.setItem(51, MenuGuiHelper.item("NAME_TAG", "§a搜索 (关键词: " + var5 + ")", List.of("§7点击使用搜索道具")));
      }
   }

   private static boolean hasPage(Map<String, Object> var0, String var1) {
      return KomutechJson.asMap(var0.get("pages")).containsKey(var1);
   }

   private static int parsePage(String var0) {
      try {
         return Integer.parseInt(var0);
      } catch (NumberFormatException var2) {
         return 1;
      }
   }

   private static int countPages(Map<String, Object> var0) {
      Map<String, Object> var1 = KomutechJson.asMap(var0.get("pages"));
      int var2 = 0;

      for (String var4 : var1.keySet()) {
         try {
            var2 = Math.max(var2, Integer.parseInt(var4));
         } catch (NumberFormatException var6) {
         }
      }

      return Math.max(1, var2);
   }

   private record LoadedState(String page, String mode, Map<String, Object> searchData, String keyword) {
   }

   private static final class SavedState {
      String page = "1";
      String mode = "normal";
      String keyword = "";
   }

   private static final class Session {
      String page = "1";
      String mode = "normal";
      Map<String, Object> searchData;
      String keyword = "";
      String storageKey;
      Map<String, Object> loaded;
   }
}
