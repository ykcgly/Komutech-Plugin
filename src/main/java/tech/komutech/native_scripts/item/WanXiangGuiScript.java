package tech.komutech.native_scripts.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.storage.WanXiangGuiHolder;
import tech.komutech.native_scripts.storage.WanXiangGuiStorage;
import tech.komutech.native_scripts.support.KomutechAsyncScheduler;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class WanXiangGuiScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String TITLE_PREFIX = "§f§l萬象匣";
   private static final int SLOT_PREV = 48;
   private static final int SLOT_CLOSE = 49;
   private static final int SLOT_NEXT = 50;
   private static final int SLOT_SORT = 47;
   private static final int SLOT_BACK = 45;
   private static final int SLOT_SEARCH = 51;
   private final Map<UUID, WanXiangGuiScript.Session> openPlayers = new HashMap<>();
   private final Set<UUID> naming = new HashSet<>();
   private final Set<UUID> turning = new HashSet<>();
   private final Set<UUID> awaitingSearch = new HashSet<>();

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MenuGuiHelper.isWanXiangGuiTitle(var1);
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return "onUse".equals(var1) ? UseEvents.parse(var2[0]).map(this::handleUse).orElse(null) : null;
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      if (var2.isSneaking()) {
         UUID var6 = var2.getUniqueId();
         if (this.naming.remove(var6)) {
            KomutechSupport.send(var2, "§c已取消命名输入，可重新开始命名。");
            return null;
         } else {
            this.naming.add(var6);
            KomutechSupport.send(var2, "§a请在聊天栏输入此物品的存储名称（支持中文、字母、数字），输入 cancel 取消:");
            KomutechChatInput.waitFor(var2, var3x -> {
               this.naming.remove(var6);
               if ("cancel".equalsIgnoreCase(var3x.trim())) {
                  KomutechSupport.send(var2, "§c已取消命名。");
               } else {
                  String var4x = var3x.trim();
                  if (!WanXiangGuiStorage.isValidStorageName(var4x)) {
                     KomutechSupport.send(var2, "§c名称不能为空，且不能包含 \\ / : * ? \" < > | 等字符");
                  } else {
                     ItemStack var5x = findStorage(var2);
                     if (var5x == null) {
                        KomutechSupport.send(var2, "§c存储道具已丢失");
                     } else {
                        String var6x = WanXiangGuiStorage.resolveStorageName(var2.getName(), var4x);
                        WanXiangGuiStorage.setStorageName(var5x, var6x);
                        Map var7 = WanXiangGuiStorage.read(var2.getName(), var6x);
                        boolean var8 = WanXiangGuiStorage.storageFileExists(var2.getName(), var6x);
                        if (!var8) {
                           WanXiangGuiStorage.write(var2.getName(), var6x, var7);
                        }

                        if (var8 && WanXiangGuiStorage.countStoredItems(var7) > 0) {
                           KomutechSupport.actionBar(var2, "§a已绑定已有存储: §f" + var6x);
                        } else {
                           KomutechSupport.actionBar(var2, "§a已设置存储名称: §f" + var6x);
                        }

                        this.openGui(var2, var5x, var6x, var7, "1", "normal", null, "");
                     }
                  }
               }
            });
            return null;
         }
      } else {
         String var4 = WanXiangGuiStorage.resolveStorageName(var2.getName(), WanXiangGuiStorage.getStorageName(var3));
         if (var4 == null) {
            KomutechSupport.send(var2, "§c此物品尚未命名，请蹲下右键为它命名。");
            return null;
         } else {
            WanXiangGuiStorage.LoadedView var5 = WanXiangGuiStorage.loadView(var2.getName(), var4);
            this.openGui(var2, var3, var4, var5.data(), var5.page(), var5.mode(), var5.searchData(), var5.keyword());
            return null;
         }
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (MenuGuiHelper.isWanXiangGuiTitle(var1.getView())) {
            var1.setCancelled(true);
            UUID var11 = var2.getUniqueId();
            WanXiangGuiScript.Session var4 = this.openPlayers.get(var11);
            if (var4 == null) {
               var4 = this.recoverSession(var2);
               if (var4 == null) {
                  return;
               }

               this.openPlayers.put(var11, var4);
            }

            ItemStack var5 = var4.item;
            if (var5 == null || var5.getType().isAir()) {
               var5 = findStorage(var2);
               var4.item = var5;
            }

            if (var5 == null) {
               KomutechSupport.send(var2, "§c存储道具已丢失，界面关闭。");
               var2.closeInventory();
            } else {
               String var6 = storageName(var2, var5);
               if (var6 == null) {
                  var2.closeInventory();
                  KomutechSupport.send(var2, "§c物品未命名，请先蹲下右键命名。");
               } else {
                  Inventory var7 = var1.getView().getTopInventory();
                  Inventory var8 = var1.getView().getBottomInventory();
                  Inventory var9 = var1.getClickedInventory();
                  if ("normal".equals(var4.mode) && var9 != null && var9 != var7) {
                     ItemStack var13 = var1.getCurrentItem();
                     if (var13 != null && !var13.getType().isAir()) {
                        this.depositItem(var2, var4, var6, var13, var1.getSlot(), var9);
                     }
                  } else if ("normal".equals(var4.mode) && var1.getRawSlot() >= var7.getSize()) {
                     ItemStack var12 = var1.getCurrentItem();
                     if (var12 != null && !var12.getType().isAir()) {
                        this.depositItem(var2, var4, var6, var12, var1.getSlot(), var9);
                     }
                  } else {
                     int var10 = var1.getSlot();
                     if (var1.getClickedInventory() == var7 && var10 >= 45 && var10 < 54) {
                        this.handleControlClick(var2, var4, var6, var10);
                     } else {
                        if (var1.getClickedInventory() == var7 && var10 >= 0 && var10 < 45) {
                           this.handleContentClick(var2, var4, var6, var10, var7);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getWhoClicked() instanceof Player) {
         if (MenuGuiHelper.isWanXiangGuiTitle(var1.getView())) {
            for (int var3 : var1.getRawSlots()) {
               if (var3 < 54) {
                  var1.setCancelled(true);
                  return;
               }
            }
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         UUID var10 = var2.getUniqueId();
         if (!this.turning.remove(var10)) {
            if (this.awaitingSearch.remove(var10)) {
               WanXiangGuiScript.Session var11 = this.openPlayers.get(var10);
               if (var11 != null) {
                  KomutechSupport.send(var2, "§a请在聊天栏输入关键词（输入 cancel 取消）:");
                  KomutechChatInput.waitFor(var2, var4x -> {
                     if ("cancel".equalsIgnoreCase(var4x.trim())) {
                        KomutechSupport.send(var2, "§c已取消搜索");
                        this.reopenAfterSearchCancel(var2, var11);
                     } else {
                        String var5x = var4x.trim();
                        if (var5x.isEmpty()) {
                           KomutechSupport.send(var2, "§c关键词不能为空");
                           this.reopenAfterSearchCancel(var2, var11);
                        } else {
                           ItemStack var6x = var11.item;
                           if (var6x == null) {
                              var6x = findStorage(var2);
                           }

                           String var7x = var6x == null ? null : storageName(var2, var6x);
                           if (var7x == null) {
                              this.openPlayers.remove(var10);
                           } else {
                              Map var8x = WanXiangGuiStorage.read(var2.getName(), var7x);
                              Map var9x = WanXiangGuiStorage.search(var8x, var5x);
                              List var10x = KomutechJson.asList(KomutechJson.asMap(var9x.get("pages")).get("1"));
                              if (var10x != null && !var10x.isEmpty() && var10x.get(0) != null) {
                                 var11.mode = "search";
                                 var11.searchData = var9x;
                                 var11.keyword = var5x;
                                 var11.page = "1";
                                 WanXiangGuiStorage.setMeta(var8x, "1", "search", var5x);
                                 WanXiangGuiStorage.write(var2.getName(), var7x, var8x);
                                 this.turning.add(var10);
                                 this.openGui(var2, var6x, var7x, var8x, "1", "search", var9x, var5x);
                                 KomutechSupport.send(var2, "§a找到 " + WanXiangGuiStorage.countStoredItems(var9x) + " 个物品。");
                              } else {
                                 KomutechSupport.send(var2, "§c没有找到匹配的物品。");
                                 WanXiangGuiStorage.setMeta(var8x, "1", "normal", "");
                                 WanXiangGuiStorage.write(var2.getName(), var7x, var8x);
                                 var11.mode = "normal";
                                 var11.searchData = null;
                                 var11.keyword = "";
                                 var11.page = "1";
                                 this.turning.add(var10);
                                 this.openGui(var2, var6x, var7x, var8x, "1", "normal", null, "");
                              }
                           }
                        }
                     }
                  });
               }
            } else {
               WanXiangGuiScript.Session var4 = this.openPlayers.remove(var10);
               if (var4 != null) {
                  KomutechMenuRouter.unbindInventory(var1.getView().getTopInventory());
                  if (var4.item != null) {
                     String var5 = storageName(var2, var4.item);
                     if (var5 != null) {
                        String var6 = var2.getName();
                        String var7 = var4.page;
                        String var8 = var4.mode;
                        String var9 = var4.keyword == null ? "" : var4.keyword;
                        KomutechAsyncScheduler.submit(() -> {
                           Map var5x = WanXiangGuiStorage.read(var6, var5);
                           WanXiangGuiStorage.setMeta(var5x, var7, var8, var9);
                           WanXiangGuiStorage.write(var6, var5, var5x);
                        }, var0 -> {});
                     }
                  }
               }
            }
         }
      }
   }

   private void reopenAfterSearchCancel(Player var1, WanXiangGuiScript.Session var2) {
      ItemStack var3 = var2.item;
      if (var3 == null) {
         var3 = findStorage(var1);
      }

      String var4 = var3 == null ? null : storageName(var1, var3);
      if (var4 == null) {
         this.openPlayers.remove(var1.getUniqueId());
      } else {
         Map var5 = WanXiangGuiStorage.read(var1.getName(), var4);
         var2.mode = "normal";
         var2.searchData = null;
         var2.keyword = "";
         var2.page = WanXiangGuiStorage.metaPage(var5);
         this.turning.add(var1.getUniqueId());
         this.openGui(var1, var3, var4, var5, var2.page, "normal", null, "");
      }
   }

   private void handleControlClick(Player var1, WanXiangGuiScript.Session var2, String var3, int var4) {
      Map var5 = WanXiangGuiStorage.read(var1.getName(), var3);
      Map var6 = "search".equals(var2.mode) && var2.searchData != null ? var2.searchData : var5;
      UUID var7 = var1.getUniqueId();
      if (var4 == 48 && hasPage(var6, String.valueOf(Integer.parseInt(var2.page) - 1))) {
         var2.page = String.valueOf(Integer.parseInt(var2.page) - 1);
         this.turning.add(var7);
         this.refreshGui(var1, var2, var3, var5);
      } else if (var4 == 50 && hasPage(var6, String.valueOf(Integer.parseInt(var2.page) + 1))) {
         var2.page = String.valueOf(Integer.parseInt(var2.page) + 1);
         this.turning.add(var7);
         this.refreshGui(var1, var2, var3, var5);
      } else if (var4 == 49) {
         var1.closeInventory();
      } else if (var4 == 45 && "search".equals(var2.mode)) {
         var2.mode = "normal";
         var2.searchData = null;
         var2.keyword = "";
         var2.page = "1";
         WanXiangGuiStorage.setMeta(var5, "1", "normal", "");
         WanXiangGuiStorage.write(var1.getName(), var3, var5);
         this.turning.add(var7);
         this.refreshGui(var1, var2, var3, var5);
      } else if (var4 == 47 && "normal".equals(var2.mode)) {
         KomutechSupport.send(var1, "§7整理中...");
         String var8 = var1.getName();
         Map[] var9 = new Map[1];
         KomutechAsyncScheduler.submit(() -> {
            Map var3x = WanXiangGuiStorage.sortAll(WanXiangGuiStorage.read(var8, var3));
            WanXiangGuiStorage.write(var8, var3, var3x);
            var9[0] = var3x;
         }, var6x -> {
            Map var7x = var9[0];
            if (var7x != null) {
               var2.page = "1";
               var2.mode = "normal";
               var2.searchData = null;
               var2.keyword = "";
               this.turning.add(var7);
               this.refreshGui(var1, var2, var3, var7x);
               KomutechSupport.send(var1, "§a整理完成！");
            }
         });
      } else if (var4 == 51 && "normal".equals(var2.mode)) {
         KomutechSupport.send(var1, "§a请先手动关闭当前界面，然后在聊天栏输入关键词（输入 cancel 取消）:");
         this.awaitingSearch.add(var7);
         var1.closeInventory();
      }
   }

   private void refreshGui(Player var1, WanXiangGuiScript.Session var2, String var3, Map<String, Object> var4) {
      this.openGui(var1, var2.item, var3, var4, var2.page, var2.mode, var2.searchData, var2.keyword);
   }

   private void handleContentClick(Player var1, WanXiangGuiScript.Session var2, String var3, int var4, Inventory var5) {
      Map var6 = "search".equals(var2.mode) && var2.searchData != null ? var2.searchData : WanXiangGuiStorage.read(var1.getName(), var3);
      Map var7 = KomutechJson.asMap(var6.get("pages"));
      List var8 = KomutechJson.asList(var7.get(var2.page));
      if (var8 != null && var4 < var8.size() && var8.get(var4) != null) {
         Object var9 = var8.get(var4);
         ItemStack var10 = WanXiangGuiStorage.deserialize(var9);
         if (var10 != null) {
            if (var1.getInventory().firstEmpty() == -1) {
               KomutechSupport.send(var1, "§c背包已满");
               return;
            }

            KomutechSupport.giveOrDrop(var1, var10);
         }

         var8.set(var4, null);
         var7.put(var2.page, var8);
         var6.put("pages", var7);
         if ("search".equals(var2.mode)) {
            Map var11 = WanXiangGuiStorage.read(var1.getName(), var3);
            WanXiangGuiStorage.removeSerialized(var11, var9);
            WanXiangGuiStorage.write(var1.getName(), var3, var11);
            var2.searchData = var6;
            var5.setItem(var4, null);
         } else {
            WanXiangGuiStorage.write(var1.getName(), var3, var6);
            var5.setItem(var4, null);
         }
      }
   }

   private void depositItem(Player var1, WanXiangGuiScript.Session var2, String var3, ItemStack var4, int var5, Inventory var6) {
      if (!WanXiangGuiStorage.storageFileExists(var1.getName(), var3)
         && WanXiangGuiStorage.countPlayerStorages(var1.getName()) >= WanXiangGuiStorage.storageLimit()) {
         KomutechSupport.actionBar(var1, "§c你已达到最大存储数量（" + WanXiangGuiStorage.storageLimit() + "个），无法创建新的存储。");
      } else {
         Map var7 = WanXiangGuiStorage.read(var1.getName(), var3);
         WanXiangGuiStorage.StoreResult var8 = WanXiangGuiStorage.store(var7, var4);
         if (!var8.success()) {
            if ("禁止存入".equals(var8.reason())) {
               KomutechSupport.actionBar(var1, "§c不能存入此物品");
            } else if ("物品已存在".equals(var8.reason())) {
               KomutechSupport.actionBar(var1, "§c该物品已存在");
            } else {
               KomutechSupport.actionBar(var1, "§c无法存储");
            }
         } else {
            WanXiangGuiStorage.write(var1.getName(), var3, var7);
            if (var4.getAmount() > 1) {
               var4.setAmount(var4.getAmount() - 1);
            } else if (var6 != null) {
               var6.setItem(var5, null);
            } else {
               var1.getInventory().setItem(var5, null);
            }

            var1.updateInventory();
            KomutechSupport.actionBar(var1, "§a已存入物品");
            if (var8.page().equals(var2.page)) {
               ItemStack var9 = WanXiangGuiStorage.deserialize(KomutechJson.asList(KomutechJson.asMap(var7.get("pages")).get(var2.page)).get(var8.slot()));
               if (var9 != null) {
                  var1.getOpenInventory().getTopInventory().setItem(var8.slot(), var9);
               }
            } else {
               var2.page = var8.page();
               this.turning.add(var1.getUniqueId());
               this.refreshGui(var1, var2, var3, var7);
            }
         }
      }
   }

   private void openGui(Player var1, ItemStack var2, String var3, Map<String, Object> var4, String var5, String var6, Map<String, Object> var7, String var8) {
      Map var9 = "search".equals(var6) && var7 != null ? var7 : var4;
      String var10 = "§f§l萬象匣" + ("search".equals(var6) ? " §7- 搜索 \"" + var8 + "\" 第 " + var5 + " 页" : " §7- 第 " + var5 + " 页");
      Inventory var11 = Bukkit.createInventory(new WanXiangGuiHolder(var3, var5, var6), 54, ChatColor.translateAlternateColorCodes('&', var10));
      Map var12 = KomutechJson.asMap(var9.get("pages"));
      List var13 = KomutechJson.asList(var12.get(var5));
      ItemStack var14 = MenuGuiHelper.item("BLACK_STAINED_GLASS_PANE", " ", null);

      for (int var15 = 0; var15 < 45; var15++) {
         Object var16 = var13 != null && var15 < var13.size() ? var13.get(var15) : null;
         ItemStack var17 = WanXiangGuiStorage.deserialize(var16);
         var11.setItem(var15, var17 != null ? var17 : null);
      }

      String var18 = String.valueOf(Integer.parseInt(var5) - 1);
      var11.setItem(
         48,
         hasPage(var9, var18)
            ? MenuGuiHelper.item("ARROW", "§a上一页", List.of("§7点击切换到第 " + var18 + " 页"))
            : MenuGuiHelper.item("GRAY_STAINED_GLASS_PANE", "§8无上一页", null)
      );
      String var19 = String.valueOf(Integer.parseInt(var5) + 1);
      var11.setItem(
         50,
         hasPage(var9, var19)
            ? MenuGuiHelper.item("ARROW", "§a下一页", List.of("§7点击切换到第 " + var19 + " 页"))
            : MenuGuiHelper.item("GRAY_STAINED_GLASS_PANE", "§8无下一页", null)
      );
      var11.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of("§7关闭存储界面")));
      if ("normal".equals(var6)) {
         var11.setItem(47, MenuGuiHelper.item("HOPPER", "§a按拼音整理", List.of("§7点击将所有页物品按拼音排序")));
         var11.setItem(51, MenuGuiHelper.item("NAME_TAG", "§a搜索", List.of("§7点击输入关键词搜索物品")));
      } else {
         var11.setItem(45, MenuGuiHelper.item("ARROW", "§a返回", List.of("§7返回正常浏览")));
      }

      for (int var20 = 45; var20 < 54; var20++) {
         if (var11.getItem(var20) == null) {
            var11.setItem(var20, var14);
         }
      }

      WanXiangGuiScript.Session var21 = this.openPlayers.get(var1.getUniqueId());
      if (var21 == null) {
         var21 = new WanXiangGuiScript.Session();
      }

      var21.item = var2;
      var21.page = var5;
      var21.mode = var6;
      var21.searchData = var7;
      var21.keyword = var8 == null ? "" : var8;
      this.openPlayers.put(var1.getUniqueId(), var21);
      this.turning.add(var1.getUniqueId());
      KomutechMenuRouter.bindInventory(var11, this);
      var1.openInventory(var11);
   }

   private WanXiangGuiScript.Session recoverSession(Player var1) {
      InventoryView var2 = var1.getOpenInventory();
      if (var2.getTopInventory().getHolder() instanceof WanXiangGuiHolder var8) {
         ItemStack var10 = findStorage(var1);
         if (var10 == null) {
            return null;
         } else {
            WanXiangGuiScript.Session var11 = new WanXiangGuiScript.Session();
            var11.item = var10;
            var11.page = var8.page;
            var11.mode = var8.mode;
            if ("search".equals(var8.mode)) {
               String var12 = storageName(var1, var10);
               if (var12 != null) {
                  Map var7 = WanXiangGuiStorage.read(var1.getName(), var12);
                  var11.searchData = WanXiangGuiStorage.search(var7, WanXiangGuiStorage.metaKeyword(var7));
                  var11.keyword = WanXiangGuiStorage.metaKeyword(var7);
               }
            }

            return var11;
         }
      } else {
         ItemStack var3 = findStorage(var1);
         if (var3 == null) {
            return null;
         } else {
            String var9 = storageName(var1, var3);
            if (var9 == null) {
               return null;
            } else {
               WanXiangGuiStorage.LoadedView var5 = WanXiangGuiStorage.loadView(var1.getName(), var9);
               WanXiangGuiScript.Session var6 = new WanXiangGuiScript.Session();
               var6.item = var3;
               var6.page = var5.page();
               var6.mode = var5.mode();
               var6.searchData = "search".equals(var5.mode()) ? var5.searchData() : null;
               var6.keyword = var5.keyword() == null ? "" : var5.keyword();
               return var6;
            }
         }
      }
   }

   private static String storageName(Player var0, ItemStack var1) {
      return var1 == null ? null : WanXiangGuiStorage.resolveStorageName(var0.getName(), WanXiangGuiStorage.getStorageName(var1));
   }

   private static boolean hasPage(Map<String, Object> var0, String var1) {
      return KomutechJson.asMap(var0.get("pages")).containsKey(var1);
   }

   private static ItemStack findStorage(Player var0) {
      ItemStack var1 = var0.getInventory().getItemInMainHand();
      if (WanXiangGuiStorage.isStorageItem(var1)) {
         return var1;
      } else {
         for (ItemStack var5 : var0.getInventory().getContents()) {
            if (WanXiangGuiStorage.isStorageItem(var5)) {
               return var5;
            }
         }

         return null;
      }
   }

   private static final class Session {
      ItemStack item;
      String page = "1";
      String mode = "normal";
      Map<String, Object> searchData;
      String keyword = "";
   }
}
