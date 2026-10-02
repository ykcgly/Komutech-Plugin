package tech.komutech.native_scripts.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.storage.WanXiangGuiStorage;
import tech.komutech.native_scripts.support.KomutechAsyncScheduler;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;

public final class WanXiangGuiConfigMenuScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String PLAYER_TITLE = "§a§l我的存储";
   private static final String ADMIN_TITLE = "§c§l管理员模式";
   private static final String STORAGE_LIST_PREFIX = "§e§l存储列表 - ";
   private static final String STORAGE_VIEW_PREFIX = "§d§l存储内容 - ";
   private static final int MODE_SLOT = 4;
   private static final int INFO_SLOT = 8;
   private static final int BACK_SLOT = 49;
   private static final int SORT_SLOT = 47;
   private static final int SEARCH_SLOT = 51;
   private static final int BACK_BUTTON_SLOT = 45;
   private static final int[] BORDER_SLOTS = new int[]{0, 1, 2, 3, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
   private final Map<Player, WanXiangGuiConfigMenuScript.MenuState> openPlayers = new HashMap<>();
   private final Map<Player, Boolean> turning = new HashMap<>();
   private final Map<Player, Boolean> awaitingSearch = new HashMap<>();

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      String var2 = MenuGuiHelper.legacyTitle(var1);
      return "§a§l我的存储".equals(var2) || "§c§l管理员模式".equals(var2) || var2.startsWith("§e§l存储列表 - ") || var2.startsWith("§d§l存储内容 - ");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onButtonGroupClick".equals(var1) && var2[0] instanceof Player var6) {
         this.openPlayerMenu(var6);
         return true;
      } else {
         if ("onUse".equals(var1)) {
            try {
               Player var3 = (Player)var2[0].getClass().getMethod("getPlayer").invoke(var2[0]);
               this.openPlayerMenu(var3);
            } catch (ReflectiveOperationException var5) {
            }
         }

         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         String var7 = MenuGuiHelper.legacyTitle(var1.getView());
         if (this.handles(var1.getView())) {
            var1.setCancelled(true);
            WanXiangGuiConfigMenuScript.MenuState var4 = this.openPlayers.get(var2);
            if (var4 != null) {
               ItemStack var5 = var1.getCurrentItem();
               if (var5 != null && !var5.getType().isAir()) {
                  int var6 = var1.getSlot();
                  if ("§a§l我的存储".equals(var7)) {
                     this.handlePlayerMenuClick(var2, var4, var6);
                  } else if ("§c§l管理员模式".equals(var7)) {
                     this.handleAdminMenuClick(var2, var4, var6);
                  } else if (var7.startsWith("§e§l存储列表 - ")) {
                     this.handleStorageListClick(var2, var4, var6);
                  } else if (var7.startsWith("§d§l存储内容 - ")) {
                     this.handleStorageViewClick(var2, var4, var6, var1);
                  }
               }
            }
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (!Boolean.TRUE.equals(this.turning.remove(var2))) {
            if (Boolean.TRUE.equals(this.awaitingSearch.remove(var2))) {
               WanXiangGuiConfigMenuScript.MenuState var5 = this.openPlayers.get(var2);
               if (var5 instanceof WanXiangGuiConfigMenuScript.ViewState var4) {
                  KomutechSupport.send(var2, "§a请在聊天栏输入搜索关键词，输入 cancel 取消:");
                  KomutechChatInput.waitFor(var2, var3 -> {
                     if ("cancel".equalsIgnoreCase(var3.trim())) {
                        KomutechSupport.send(var2, "§c已取消搜索");
                     } else {
                        String var4x = var3.trim();
                        if (var4x.isEmpty()) {
                           KomutechSupport.send(var2, "§c关键词不能为空");
                        } else {
                           Map var5x = WanXiangGuiStorage.read(var4.owner(), var4.storage());
                           Map var6 = WanXiangGuiStorage.search(var5x, var4x);
                           List var7 = KomutechJson.asList(KomutechJson.asMap(var6.get("pages")).get("1"));
                           if (var7 != null && !var7.isEmpty() && var7.get(0) != null) {
                              WanXiangGuiStorage.setMeta(var5x, "1", "search", var4x);
                              WanXiangGuiStorage.write(var4.owner(), var4.storage(), var5x);
                              this.turning.put(var2, true);
                              this.openStorageView(var2, var4.owner(), var4.storage(), "1", "search", var6, var4x);
                              KomutechSupport.send(var2, "§a找到 " + WanXiangGuiStorage.countStoredItems(var6) + " 个物品");
                           } else {
                              KomutechSupport.send(var2, "§c没有找到匹配的物品");
                           }
                        }
                     }
                  });
               }
            } else {
               this.openPlayers.remove(var2);
               KomutechMenuRouter.unbindInventory(var1.getView().getTopInventory());
            }
         }
      }
   }

   private void handlePlayerMenuClick(Player var1, WanXiangGuiConfigMenuScript.MenuState var2, int var3) {
      if (var2 instanceof WanXiangGuiConfigMenuScript.PlayerState var4) {
         if (var3 == 4 && isAdmin(var1)) {
            this.turning.put(var1, true);
            this.openAdminMenu(var1, 1);
         } else if (var3 == 49) {
            var1.closeInventory();
         } else {
            int var5 = var3 / 9;
            int var6 = var3 % 9;
            if (var6 >= 1 && var6 <= 7) {
               int var7 = var6 - 1;
               if (var5 != 1 || var7 >= var4.storages().size()) {
                  if (var5 == 2 && var7 < var4.storages().size()) {
                     String var9 = var4.storages().get(var7);
                     confirmDelete(var1, var9, () -> {
                        WanXiangGuiStorage.deleteStorage(var1.getName(), var9);
                        KomutechSupport.send(var1, "§a已删除存储 " + var9);
                        this.openPlayerMenu(var1);
                     });
                  } else {
                     if (var5 == 3 && var7 < var4.storages().size()) {
                        String var8 = var4.storages().get(var7);
                        this.promptRename(var1, var8);
                     }
                  }
               }
            }
         }
      }
   }

   private void handleAdminMenuClick(Player var1, WanXiangGuiConfigMenuScript.MenuState var2, int var3) {
      if (var2 instanceof WanXiangGuiConfigMenuScript.AdminState var4) {
         if (!isAdmin(var1)) {
            KomutechSupport.send(var1, "§c你没有权限使用管理员模式");
            this.openPlayerMenu(var1);
         } else if (var3 == 4) {
            this.openPlayerMenu(var1);
         } else if (var3 == 49) {
            var1.closeInventory();
         } else if (var3 == 53) {
            KomutechSupport.send(var1, "§c确定要清空所有存储数据吗？此操作不可逆。请输入密码以确认:");
            KomutechChatInput.waitFor(var1, var2x -> {
               if (WanXiangGuiStorage.clearPassword().equals(var2x)) {
                  KomutechAsyncScheduler.submit(WanXiangGuiStorage::deleteAllStorages, var2xx -> {
                     KomutechSupport.send(var1, "§a已清空所有存储数据");
                     this.openAdminMenu(var1, 1);
                  });
               } else {
                  KomutechSupport.send(var1, "§c密码错误，操作已取消");
               }
            });
         } else if (var3 == 48 && var4.page() > 1) {
            this.openAdminMenu(var1, var4.page() - 1);
         } else if (var3 == 50 && var4.page() < var4.total()) {
            this.openAdminMenu(var1, var4.page() + 1);
         } else {
            String var5 = var4.slotMap().get(var3);
            if (var5 != null) {
               this.openStorageList(var1, var5);
            }
         }
      }
   }

   private void handleStorageListClick(Player var1, WanXiangGuiConfigMenuScript.MenuState var2, int var3) {
      if (var2 instanceof WanXiangGuiConfigMenuScript.ListState var4) {
         if (var3 == 4) {
            this.openAdminMenu(var1, 1);
         } else if (var3 == 49) {
            var1.closeInventory();
         } else if (var3 == 53) {
            confirmDelete(
               var1,
               var4.playerName() + " 的所有存储",
               () -> KomutechAsyncScheduler.submit(() -> WanXiangGuiStorage.deletePlayerStorages(var4.playerName()), var3x -> {
                  KomutechSupport.send(var1, "§a已删除玩家 " + var4.playerName() + " 的所有存储");
                  this.openAdminMenu(var1, 1);
               })
            );
         } else {
            int var5 = var3 / 9;
            int var6 = var3 % 9;
            if (var6 >= 1 && var6 <= 7) {
               int var7 = var6 - 1;
               if (var5 == 1 && var7 < var4.storages().size()) {
                  this.openStorageView(var1, var4.playerName(), var4.storages().get(var7), "1", "normal", null, "");
               } else {
                  if (var5 == 2 && var7 < var4.storages().size()) {
                     String var8 = var4.storages().get(var7);
                     confirmDelete(var1, var8, () -> {
                        WanXiangGuiStorage.deleteStorage(var4.playerName(), var8);
                        KomutechSupport.send(var1, "§a已删除存储 " + var8);
                        this.openStorageList(var1, var4.playerName());
                     });
                  }
               }
            }
         }
      }
   }

   private void handleStorageViewClick(Player var1, WanXiangGuiConfigMenuScript.MenuState var2, int var3, InventoryClickEvent var4) {
      if (var2 instanceof WanXiangGuiConfigMenuScript.ViewState var5) {
         Map var6 = WanXiangGuiStorage.read(var5.owner(), var5.storage());
         if (var3 == 45 && "search".equals(var5.mode())) {
            WanXiangGuiStorage.setMeta(var6, "1", "normal", "");
            WanXiangGuiStorage.write(var5.owner(), var5.storage(), var6);
            this.turning.put(var1, true);
            this.openStorageView(var1, var5.owner(), var5.storage(), "1", "normal", null, "");
         } else if (var3 == 48 && var5.pageInt() > 1) {
            String var16 = String.valueOf(var5.pageInt() - 1);
            WanXiangGuiStorage.setMeta(var6, var16, var5.mode(), var5.keyword());
            WanXiangGuiStorage.write(var5.owner(), var5.storage(), var6);
            this.turning.put(var1, true);
            this.openStorageView(var1, var5.owner(), var5.storage(), var16, var5.mode(), var5.searchData(), var5.keyword());
         } else if (var3 == 49) {
            this.turning.put(var1, true);
            this.openStorageList(var1, var5.owner());
         } else if (var3 == 50) {
            Map var15 = displayData(var5);
            if (KomutechJson.asMap(var15.get("pages")).containsKey(String.valueOf(var5.pageInt() + 1))) {
               String var18 = String.valueOf(var5.pageInt() + 1);
               WanXiangGuiStorage.setMeta(var6, var18, var5.mode(), var5.keyword());
               WanXiangGuiStorage.write(var5.owner(), var5.storage(), var6);
               this.turning.put(var1, true);
               this.openStorageView(var1, var5.owner(), var5.storage(), var18, var5.mode(), var5.searchData(), var5.keyword());
            }
         } else if (var3 == 47 && "normal".equals(var5.mode())) {
            Map var14 = WanXiangGuiStorage.sortAll(var6);
            WanXiangGuiStorage.write(var5.owner(), var5.storage(), var14);
            this.turning.put(var1, true);
            this.openStorageView(var1, var5.owner(), var5.storage(), "1", "normal", null, "");
            KomutechSupport.send(var1, "§a整理完成！");
         } else if (var3 == 51) {
            this.awaitingSearch.put(var1, true);
            var1.closeInventory();
         } else {
            Inventory var7 = var4.getView().getTopInventory();
            if (var3 >= 0 && var3 < 45 && var4.getClickedInventory() == var7) {
               ItemStack var17 = var4.getCurrentItem();
               if (var17 != null && !var17.getType().isAir()) {
                  if (var1.getInventory().firstEmpty() == -1) {
                     KomutechSupport.send(var1, "§c背包已满");
                  } else {
                     Map var19 = displayData(var5);
                     List var10 = KomutechJson.asList(KomutechJson.asMap(var19.get("pages")).get(var5.page()));
                     Object var11 = var10.get(var3);
                     if ("search".equals(var5.mode())) {
                        WanXiangGuiStorage.removeSerialized(var6, var11);
                        var10.set(var3, null);
                     } else if (var6 != null) {
                        Map var12 = KomutechJson.asMap(var6.get("pages"));
                        List var13 = KomutechJson.asList(var12.get(var5.page()));
                        var13.set(var3, null);
                        var12.put(var5.page(), var13);
                        var6.put("pages", var12);
                     }

                     WanXiangGuiStorage.write(var5.owner(), var5.storage(), var6);
                     KomutechSupport.giveOrDrop(var1, var17.clone());
                     KomutechSupport.send(var1, "§a已取出物品");
                     this.turning.put(var1, true);
                     if ("search".equals(var5.mode())) {
                        Map var20 = WanXiangGuiStorage.search(var6, var5.keyword());
                        this.openStorageView(var1, var5.owner(), var5.storage(), var5.page(), var5.mode(), var20, var5.keyword());
                     } else {
                        this.openStorageView(var1, var5.owner(), var5.storage(), var5.page(), var5.mode(), null, "");
                     }
                  }
               }
            } else {
               if (var3 >= 0 && var3 < 45 && var4.getClickedInventory() != var7 && "normal".equals(var5.mode())) {
                  ItemStack var8 = var4.getCurrentItem();
                  if (var8 == null || var8.getType().isAir()) {
                     return;
                  }

                  WanXiangGuiStorage.StoreResult var9 = WanXiangGuiStorage.store(var6, var8);
                  if (!var9.success()) {
                     KomutechSupport.send(var1, "§c存入失败: " + var9.reason());
                     return;
                  }

                  WanXiangGuiStorage.write(var5.owner(), var5.storage(), var6);
                  if (var8.getAmount() > 1) {
                     var8.setAmount(var8.getAmount() - 1);
                  } else {
                     var4.getClickedInventory().setItem(var4.getSlot(), null);
                  }

                  KomutechSupport.send(var1, "§a已存入物品");
                  this.turning.put(var1, true);
                  this.openStorageView(var1, var5.owner(), var5.storage(), var9.page(), var5.mode(), null, "");
               }
            }
         }
      }
   }

   private static Map<String, Object> displayData(WanXiangGuiConfigMenuScript.ViewState var0) {
      return "search".equals(var0.mode()) && var0.searchData() != null ? var0.searchData() : WanXiangGuiStorage.read(var0.owner(), var0.storage());
   }

   private void openPlayerMenu(Player var1) {
      List var2 = WanXiangGuiStorage.listStorages(var1.getName());
      Inventory var3 = MenuGuiHelper.create(54, "§a§l我的存储");
      applyBorder(var3);
      var3.setItem(4, MenuGuiHelper.item("COMPASS", "§a切换模式", List.of("§7点击切换到管理员模式")));
      var3.setItem(8, MenuGuiHelper.item("PAPER", "§a你的存储", List.of("§7共 " + var2.size() + " 个")));
      int var4 = 10;

      for (int var5 = 0; var5 < var2.size() && var4 <= 16; var5++) {
         String var6 = (String)var2.get(var5);
         var3.setItem(var4, MenuGuiHelper.item("PAPER", "§f存储名: §e" + var6, null));
         var3.setItem(var4 + 9, MenuGuiHelper.item("REDSTONE_BLOCK", "§c删除", List.of("§7删除存储 " + var6)));
         var3.setItem(var4 + 18, MenuGuiHelper.item("NAME_TAG", "§a重命名", List.of("§7重命名存储 " + var6)));
         if ((++var4 - 9) % 9 == 0) {
            var4 += 2;
         }
      }

      var3.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of("§7关闭菜单")));
      this.bindAndOpen(var1, var3, new WanXiangGuiConfigMenuScript.PlayerState(var2));
   }

   private void openAdminMenu(Player var1, int var2) {
      if (!isAdmin(var1)) {
         KomutechSupport.send(var1, "§c你没有权限使用管理员模式");
         this.openPlayerMenu(var1);
      } else {
         List var3 = WanXiangGuiStorage.listAllPlayers();
         int var4 = Math.max(1, (int)Math.ceil(var3.size() / 28.0));
         int var5 = Math.min(Math.max(var2, 1), var4);
         int var6 = (var5 - 1) * 28;
         int var7 = Math.min(var6 + 28, var3.size());
         Inventory var8 = MenuGuiHelper.create(54, "§c§l管理员模式");
         applyBorder(var8);
         var8.setItem(4, MenuGuiHelper.item("COMPASS", "§a切换模式", List.of("§7点击切换到玩家模式")));
         var8.setItem(8, MenuGuiHelper.item("PAPER", "§6管理员面板", List.of("§7总玩家数: " + var3.size(), "§7第 " + var5 + "/" + var4 + " 页")));
         var8.setItem(53, MenuGuiHelper.item("TNT", "§c清空所有数据", List.of("§7删除全部存储文件", "§c§l不可逆！")));
         int[] var9 = new int[]{10, 19, 28, 37};
         HashMap var10 = new HashMap();
         int var11 = var6;

         for (int var15 : var9) {
            for (int var16 = 0; var16 < 7 && var11 < var7; var16++) {
               String var17 = (String)var3.get(var11);
               int var18 = var15 + var16;
               var8.setItem(var18, MenuGuiHelper.item("PLAYER_HEAD", "§e" + var17, List.of("§7点击查看存储列表")));
               var10.put(var18, var17);
               var11++;
            }
         }

         if (var5 > 1) {
            var8.setItem(48, MenuGuiHelper.item("ARROW", "§a上一页", List.of("§7第 " + (var5 - 1) + " 页")));
         }

         if (var5 < var4) {
            var8.setItem(50, MenuGuiHelper.item("ARROW", "§a下一页", List.of("§7第 " + (var5 + 1) + " 页")));
         }

         var8.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of("§7关闭菜单")));
         this.bindAndOpen(var1, var8, new WanXiangGuiConfigMenuScript.AdminState(var5, var4, var10));
      }
   }

   private void openStorageList(Player var1, String var2) {
      List var3 = WanXiangGuiStorage.listStorages(var2);
      Inventory var4 = MenuGuiHelper.create(54, "§e§l存储列表 - " + var2);
      applyBorder(var4);
      var4.setItem(4, MenuGuiHelper.item("ARROW", "§a返回", List.of("§7返回玩家列表")));
      var4.setItem(8, MenuGuiHelper.item("PAPER", "§6玩家: " + var2, List.of("§7共 " + var3.size() + " 个存储")));
      var4.setItem(53, MenuGuiHelper.item("TNT", "§c删除该玩家所有存储", List.of("§7删除玩家 " + var2 + " 的所有存储", "§c§l不可逆！")));
      int var5 = 10;

      for (int var6 = 0; var6 < var3.size() && var5 <= 16; var6++) {
         String var7 = (String)var3.get(var6);
         var4.setItem(var5, MenuGuiHelper.item("BOOK", "§f存储名: §e" + var7, List.of("§7点击查看内容")));
         var4.setItem(var5 + 9, MenuGuiHelper.item("REDSTONE_BLOCK", "§c删除", List.of("§7删除此存储")));
         if ((++var5 - 9) % 9 == 0) {
            var5 += 2;
         }
      }

      var4.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of("§7关闭菜单")));
      this.bindAndOpen(var1, var4, new WanXiangGuiConfigMenuScript.ListState(var2, var3));
   }

   private void openStorageView(Player var1, String var2, String var3, String var4, String var5, Map<String, Object> var6, String var7) {
      Map var8 = WanXiangGuiStorage.read(var2, var3);
      Map var9 = "search".equals(var5) && var6 != null ? var6 : var8;
      Inventory var10 = MenuGuiHelper.create(54, "§d§l存储内容 - " + var3);
      ItemStack var11 = MenuGuiHelper.item("BLACK_STAINED_GLASS_PANE", "§8", null);

      for (int var12 = 45; var12 < 54; var12++) {
         var10.setItem(var12, var11);
      }

      List var16 = KomutechJson.asList(KomutechJson.asMap(var9.get("pages")).get(var4));

      for (int var13 = 0; var13 < 45; var13++) {
         Object var14 = var16 != null && var13 < var16.size() ? var16.get(var13) : null;
         ItemStack var15 = WanXiangGuiStorage.deserialize(var14);
         if (var15 != null) {
            var10.setItem(var13, var15);
         }
      }

      var10.setItem(48, MenuGuiHelper.item("ARROW", "§a上一页", List.of("§7上一页")));
      var10.setItem(49, MenuGuiHelper.item("ARROW", "§a返回", List.of("§7返回存储列表")));
      var10.setItem(50, MenuGuiHelper.item("ARROW", "§a下一页", List.of("§7下一页")));
      var10.setItem(47, MenuGuiHelper.item("HOPPER", "§a按拼音整理", List.of("§7点击整理物品")));
      var10.setItem(51, MenuGuiHelper.item("NAME_TAG", "§a搜索", List.of("§7点击输入关键词搜索")));
      if ("search".equals(var5)) {
         var10.setItem(45, MenuGuiHelper.item("ARROW", "§a返回", List.of("§7返回正常浏览")));
      }

      this.bindAndOpen(var1, var10, new WanXiangGuiConfigMenuScript.ViewState(var2, var3, var4, var5, var6, var7));
   }

   private void bindAndOpen(Player var1, Inventory var2, WanXiangGuiConfigMenuScript.MenuState var3) {
      this.turning.put(var1, true);
      KomutechMenuRouter.bindInventory(var2, this);
      this.openPlayers.put(var1, var3);
      var1.openInventory(var2);
   }

   private static void applyBorder(Inventory var0) {
      ItemStack var1 = MenuGuiHelper.item("BLACK_STAINED_GLASS_PANE", " ", null);

      for (int var5 : BORDER_SLOTS) {
         var0.setItem(var5, var1);
      }
   }

   private static void confirmDelete(Player var0, String var1, Runnable var2) {
      KomutechSupport.send(var0, "§c确定要删除 §e" + var1 + "§c 吗？请输入 §6确认删除 §c以确认:");
      KomutechChatInput.waitFor(var0, var2x -> {
         if ("确认删除".equals(var2x)) {
            var2.run();
         } else {
            KomutechSupport.send(var0, "§c删除已取消");
         }
      });
   }

   private void promptRename(Player var1, String var2) {
      KomutechSupport.send(var1, "§a请输入新的存储名（当前: " + var2 + "），输入 cancel 取消:");
      KomutechChatInput.waitFor(var1, var3 -> {
         if ("cancel".equalsIgnoreCase(var3.trim())) {
            KomutechSupport.send(var1, "§c已取消重命名");
         } else {
            String var4 = var3.trim();
            if (!WanXiangGuiStorage.isValidStorageName(var4)) {
               KomutechSupport.send(var1, "§c名称不能为空，且不能包含非法字符");
            } else if (WanXiangGuiStorage.storageFileExists(var1.getName(), var4)) {
               KomutechSupport.send(var1, "§c已存在同名存储");
            } else {
               if (WanXiangGuiStorage.renameStorage(var1.getName(), var2, var4)) {
                  KomutechSupport.send(var1, "§a已重命名为 " + var4);
                  this.openPlayerMenu(var1);
               } else {
                  KomutechSupport.send(var1, "§c重命名失败");
               }
            }
         }
      });
   }

   private static boolean isAdmin(Player var0) {
      return var0.isOp() || "Komu_A".equals(var0.getName());
   }

   private record AdminState(int page, int total, Map<Integer, String> slotMap) implements WanXiangGuiConfigMenuScript.MenuState {
   }

   private record ListState(String playerName, List<String> storages) implements WanXiangGuiConfigMenuScript.MenuState {
   }

   private sealed interface MenuState
      permits WanXiangGuiConfigMenuScript.PlayerState,
      WanXiangGuiConfigMenuScript.AdminState,
      WanXiangGuiConfigMenuScript.ListState,
      WanXiangGuiConfigMenuScript.ViewState {
   }

   private record PlayerState(List<String> storages) implements WanXiangGuiConfigMenuScript.MenuState {
   }

   private record ViewState(String owner, String storage, String page, String mode, Map<String, Object> searchData, String keyword)
      implements WanXiangGuiConfigMenuScript.MenuState {
      int pageInt() {
         try {
            return Integer.parseInt(this.page);
         } catch (NumberFormatException var2) {
            return 1;
         }
      }
   }
}
