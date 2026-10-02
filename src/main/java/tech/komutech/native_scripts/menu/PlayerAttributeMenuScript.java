package tech.komutech.native_scripts.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.AttributePointLimits;
import tech.komutech.native_scripts.support.CultivationMath;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeLifecycleScript;

public final class PlayerAttributeMenuScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String TITLE_PLAYER = "§a§l玩家属性 - 个人模式";
   private static final String TITLE_ADMIN_LIST = "§c§l玩家属性 - 管理员模式";
   private static final String TITLE_ADMIN_VIEW = "§d§l玩家属性详情 - ";
   private static final String ADMIN_PASSWORD = "0108";
   private static final int[] BORDER = new int[]{0, 1, 2, 3, 5, 7, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 52};
   private static final int[] ADMIN_SLOTS = new int[]{
      10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
   };
   private static final Map<Integer, String> ATTR_SLOTS = Map.ofEntries(
      Map.entry(10, "功德"),
      Map.entry(12, "煞气"),
      Map.entry(14, "根基"),
      Map.entry(15, "悟性"),
      Map.entry(16, "根骨"),
      Map.entry(31, "灵力"),
      Map.entry(23, "血量"),
      Map.entry(24, "攻击力"),
      Map.entry(25, "防御力"),
      Map.entry(32, "速度"),
      Map.entry(33, "灵识"),
      Map.entry(34, "灵气获取"),
      Map.entry(6, "属性点"),
      Map.entry(22, "灵气"),
      Map.entry(13, "修为"),
      Map.entry(40, "灵根属性")
   );
   private static final Map<Integer, String> UPGRADE_ATTRS = Map.of(23, "血量", 24, "攻击力", 25, "防御力", 32, "速度", 34, "灵气获取");
   private static final Set<String> POINT_ATTRS = Set.of("血量", "攻击力", "防御力", "速度", "灵识", "灵气获取");
   private static final long TOGGLE_DEBOUNCE_MS = 350L;
   private static final Map<Integer, String> QUALITY_SLOTS = Map.of(20, "金", 28, "木", 30, "水", 37, "火", 39, "土", 29, "雷", 19, "风", 21, "冰", 11, "光", 38, "暗");
   private final Map<Player, PlayerAttributeMenuScript.Session> openPlayers = new HashMap<>();
   private final Set<Player> awaiting = new HashSet<>();
   private final Map<UUID, Long> toggleReadyAt = new ConcurrentHashMap<>();
   private Plugin plugin;

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      AttributeApplier.registerCompatListeners(var1);
      KomutechMenuRouter.registerHandler(this);
      Bukkit.getScheduler().runTaskLater(var1, AttributeApplier::reapplyAllOnline, 40L);
   }

   @Override
   public boolean handles(InventoryView var1) {
      String var2 = MenuGuiHelper.legacyTitle(var1);
      return "§a§l玩家属性 - 个人模式".equals(var2) || "§c§l玩家属性 - 管理员模式".equals(var2) || var2.startsWith("§d§l玩家属性详情 - ");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onButtonGroupClick".equals(var1) && var2.length > 0 && var2[0] instanceof Player var3) {
         this.openPlayer(var3);
         return true;
      } else {
         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         InventoryView var8 = var1.getView();
         String var4 = MenuGuiHelper.legacyTitle(var8);
         if ("§a§l玩家属性 - 个人模式".equals(var4) || "§c§l玩家属性 - 管理员模式".equals(var4) || var4.startsWith("§d§l玩家属性详情 - ")) {
            var1.setCancelled(true);
            PlayerAttributeMenuScript.Session var5 = this.openPlayers.get(var2);
            if (var5 != null) {
               ItemStack var6 = var1.getCurrentItem();
               if (var6 != null && var6.getType() != Material.AIR) {
                  int var7 = var1.getSlot();
                  if ("§a§l玩家属性 - 个人模式".equals(var4)) {
                     this.handlePlayerClick(var2, var5, var7, var1);
                  } else if ("§c§l玩家属性 - 管理员模式".equals(var4)) {
                     this.handleAdminListClick(var2, var5, var7, var1);
                  } else {
                     this.handleAdminViewClick(var2, var5, var7, var1);
                  }
               }
            }
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         String var4 = MenuGuiHelper.legacyTitle(var1.getView());
         if ("§a§l玩家属性 - 个人模式".equals(var4) || "§c§l玩家属性 - 管理员模式".equals(var4) || var4.startsWith("§d§l玩家属性详情 - ")) {
            if (var2.getOpenInventory().getTopInventory() != var1.getInventory()) {
               this.openPlayers.remove(var2);
               this.toggleReadyAt.remove(var2.getUniqueId());
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         String var4 = MenuGuiHelper.legacyTitle(var1.getView());
         if ("§a§l玩家属性 - 个人模式".equals(var4) || "§c§l玩家属性 - 管理员模式".equals(var4) || var4.startsWith("§d§l玩家属性详情 - ")) {
            var1.setCancelled(true);
         }
      }
   }

   private void handlePlayerClick(Player var1, PlayerAttributeMenuScript.Session var2, int var3, InventoryClickEvent var4) {
      if (var3 == 51 && var4.isLeftClick() && !var4.isShiftClick() && var2.tempData != null) {
         if (this.allowToggle(var1)) {
            this.toggleAllAttributes(var1, var2.tempData);
            this.openPlayerWithTemp(var1, var2.tempData);
         }
      } else if (var4.isShiftClick() && var4.isLeftClick()) {
         String var5 = ATTR_SLOTS.get(var3);
         if (var5 != null && CultivationMath.SWITCHABLE_ATTRS.contains(var5) && var2.tempData != null) {
            if (!this.allowToggle(var1)) {
               return;
            }

            String var6 = var5 + "_启用";
            boolean var7 = PlayerAttributeStore.getBoolean(var2.tempData, var6, true);
            var2.tempData.put(var6, !var7);
            KomutechSupport.send(var1, "§a" + var5 + " 已" + (!var7 ? "启用" : "禁用"));
            this.openPlayerWithTemp(var1, var2.tempData);
         }
      } else if (var3 == 8) {
         this.openAdminList(var1, 1);
      } else if (var3 == 49) {
         var1.closeInventory();
      } else if (var3 == 53 && var2.originalData != null && var2.tempData != null) {
         AttributePointLimits.clearExempt(var2.tempData);
         AttributeApplier.applyPlayerAttributes(var1, var2.tempData, false);
         PlayerAttributeStore.save(var1.getName(), var2.tempData);
         KomutechSupport.send(var1, "§a加点已保存并应用！");
         this.openPlayer(var1);
      } else if (UPGRADE_ATTRS.containsKey(var3) && var2.originalData != null && var2.tempData != null) {
         this.adjustUpgradeAttr(var1, var2, UPGRADE_ATTRS.get(var3), var4);
      } else {
         if (var3 == 33 && var2.originalData != null && var2.tempData != null) {
            if (!CultivationMath.isGoldenCoreRealm(var2.tempData)) {
               KomutechSupport.send(var1, "§c修为未达金丹期，无法分配灵识");
               return;
            }

            this.adjustUpgradeAttr(var1, var2, "灵识", var4);
         }
      }
   }

   private void adjustUpgradeAttr(Player var1, PlayerAttributeMenuScript.Session var2, String var3, InventoryClickEvent var4) {
      int var5 = PlayerAttributeStore.getInt(var2.originalData, var3, 0);
      int var6 = PlayerAttributeStore.getInt(var2.tempData, var3, 0);
      int var7 = PlayerAttributeStore.getInt(var2.tempData, "属性点", 0);
      if (var4.isShiftClick() && var4.isRightClick()) {
         int var8 = Math.min(10, var7);
         if (var8 <= 0) {
            KomutechSupport.send(var1, "§c属性点不足");
         } else {
            int var9 = 0;

            while (var9 < var8 && AttributePointLimits.canAllocatePoint(var2.tempData, var3, var6 + var9)) {
               var9++;
            }

            if (var9 <= 0) {
               KomutechSupport.send(var1, "§c" + var3 + "已达上限（" + AttributePointLimits.limitLabel(var2.tempData, var3) + "）");
            } else {
               var2.tempData.put(var3, var6 + var9);
               var2.tempData.put("属性点", var7 - var9);
               if (var9 < 10) {
                  KomutechSupport.send(var1, "§e已加点 §a+" + var9 + " §7（点数或上限不足10）");
               }

               this.openPlayerWithTemp(var1, var2.tempData);
            }
         }
      } else if (var4.isLeftClick() && !var4.isShiftClick()) {
         if (var7 <= 0) {
            KomutechSupport.send(var1, "§c属性点不足");
         } else if (!AttributePointLimits.canAllocatePoint(var2.tempData, var3, var6)) {
            KomutechSupport.send(var1, "§c" + var3 + "已达上限（" + AttributePointLimits.limitLabel(var2.tempData, var3) + "）");
         } else {
            var2.tempData.put(var3, var6 + 1);
            var2.tempData.put("属性点", var7 - 1);
         }

         this.openPlayerWithTemp(var1, var2.tempData);
      } else {
         if (var4.isRightClick() && !var4.isShiftClick()) {
            if (var6 > var5) {
               var2.tempData.put(var3, var6 - 1);
               var2.tempData.put("属性点", var7 + 1);
            } else {
               KomutechSupport.send(var1, "§c不能低于原始值");
            }

            this.openPlayerWithTemp(var1, var2.tempData);
         }
      }
   }

   private boolean allowToggle(Player var1) {
      long var2 = System.currentTimeMillis();
      Long var4 = this.toggleReadyAt.get(var1.getUniqueId());
      if (var4 != null && var2 < var4) {
         return false;
      } else {
         this.toggleReadyAt.put(var1.getUniqueId(), var2 + 350L);
         return true;
      }
   }

   private void handleAdminListClick(Player var1, PlayerAttributeMenuScript.Session var2, int var3, InventoryClickEvent var4) {
      if (var3 == 8) {
         this.openPlayer(var1);
      } else if (var3 == 49) {
         var1.closeInventory();
      } else if (var3 == 48 && var2.page > 1) {
         this.openAdminList(var1, var2.page - 1);
      } else if (var3 == 50 && var2.page < var2.totalPages) {
         this.openAdminList(var1, var2.page + 1);
      } else if (var3 != 53 || !var4.isLeftClick()) {
         String var5 = var2.slotMap.get(var3);
         if (var5 != null) {
            if (var4.isShiftClick() && var4.isRightClick()) {
               if (!this.awaiting.contains(var1)) {
                  this.awaiting.add(var1);
                  KomutechSupport.send(var1, "§c输入 \"确认删除\" 以删除 " + var5 + ":");
                  KomutechChatInput.waitFor(var1, var4x -> {
                     this.awaiting.remove(var1);
                     if (!"确认删除".equals(stripColor(var4x).trim())) {
                        KomutechSupport.send(var1, "§c操作取消");
                     } else {
                        Player var5x = Bukkit.getPlayer(var5);
                        if (var5x != null && var5x.isOnline()) {
                           AttributeApplier.clearSwitchable(var5x);
                           KomutechSupport.send(var5x, "§c你的玩家属性数据已被管理员移除。");
                        }

                        PlayerAttributeStore.delete(var5);
                        KomutechSupport.send(var1, "§a已删除玩家 " + var5 + " 的属性数据。");
                        this.openAdminList(var1, var2.page);
                     }
                  });
               }
            } else {
               if (var4.isLeftClick() && !var4.isShiftClick()) {
                  this.openAdminView(var1, var5);
               }
            }
         }
      } else if (!this.awaiting.contains(var1)) {
         this.awaiting.add(var1);
         KomutechSupport.send(var1, "§c请输入管理员密码:");
         KomutechChatInput.waitFor(var1, var3x -> {
            this.awaiting.remove(var1);
            if (!"0108".equals(stripColor(var3x).trim())) {
               KomutechSupport.send(var1, "§c密码错误");
            } else {
               int var4x = 0;

               try {
                  Path var5x = PlayerAttributeStore.fileFor("x").getParent();
                  if (Files.exists(var5x)) {
                     try (Stream<Path> var6 = Files.list(var5x)) {
                        for (Path var8 : var6.toList()) {
                           String var9 = var8.getFileName().toString();
                           if (var9.startsWith("[") && var9.endsWith("].json")) {
                              String var10 = var9.substring(1, var9.length() - 6);
                              Player var11 = Bukkit.getPlayer(var10);
                              if (var11 != null && var11.isOnline()) {
                                 AttributeApplier.clearSwitchable(var11);
                                 KomutechSupport.send(var11, "§c你的玩家属性数据已被管理员重置。");
                              }

                              Files.deleteIfExists(var8);
                              var4x++;
                           }
                        }
                     }
                  }
               } catch (IOException var14) {
               }

               KomutechSupport.send(var1, "§a已删除 " + var4x + " 个玩家数据。");
               this.openAdminList(var1, var2.page);
            }
         });
      }
   }

   private void handleAdminViewClick(Player var1, PlayerAttributeMenuScript.Session var2, int var3, InventoryClickEvent var4) {
      if (var2.viewData != null && var2.viewPlayerName != null) {
         if (var3 == 8) {
            this.openPlayer(var1);
         } else if (var3 == 48) {
            this.openAdminList(var1, 1);
         } else if (var3 == 49) {
            var1.closeInventory();
         } else if (var3 != 53) {
            if (var4.isRightClick()) {
               if (var3 == 7 && var2.viewData.containsKey("突破失败")) {
                  var2.viewData.put("突破失败", !PlayerAttributeStore.getBoolean(var2.viewData, "突破失败", false));
                  this.refreshAdminView(var1, var2);
                  return;
               }

               this.handleAdminRightClick(var1, var2, var3);
            }
         } else {
            PlayerAttributeStore.save(var2.viewPlayerName, var2.viewData);
            Player var5 = Bukkit.getPlayer(var2.viewPlayerName);
            if (var5 != null && var5.isOnline()) {
               AttributeApplier.applyPlayerAttributes(var5, var2.viewData);
               KomutechSupport.send(var1, "§a修改已保存并应用。");
            } else {
               KomutechSupport.send(var1, "§a修改已保存，玩家上线后将自动生效。");
            }

            this.openAdminView(var1, var2.viewPlayerName);
         }
      }
   }

   private void handleAdminRightClick(Player var1, PlayerAttributeMenuScript.Session var2, int var3) {
      if (!this.awaiting.contains(var1)) {
         Map var4 = var2.viewData;
         String var5 = var2.viewPlayerName;
         if (QUALITY_SLOTS.containsKey(var3)) {
            String var8 = QUALITY_SLOTS.get(var3);
            Map var7 = PlayerAttributeStore.getMap(var4, "灵根品质");
            if (var7.containsKey(var8)) {
               this.handleQualityModify(var1, var5, var4, var8, var7);
            }
         } else if (var3 == 40 && PlayerAttributeStore.getString(var4, "灵根", null) != null) {
            this.handleLinggenAttrModify(var1, var5, var4);
         } else {
            String var6 = ATTR_SLOTS.get(var3);
            if (var6 != null && ("根基".equals(var6) || var4.containsKey(var6))) {
               if ("修为".equals(var6)) {
                  KomutechSupport.send(var1, "§c修为不可直接修改，请通过修改灵气值来调整修为。");
               } else if ("根基".equals(var6)) {
                  this.handleFoundationModify(var1, var5, var4);
               } else if (var3 == 15 || var3 == 16) {
                  this.handleWuxingGenguModify(var1, var5, var4, var6);
               } else if (var3 == 32) {
                  this.handleSpeedModify(var1, var5, var4);
               } else if (var3 == 22 && var4.containsKey("灵气")) {
                  this.handleSpiritModify(var1, var5, var4);
               } else if (var3 == 31 && var4.containsKey("灵力")) {
                  this.handleLingliBonusModify(var1, var5, var4);
               } else {
                  this.handleCommonAttrModify(var1, var5, var4, var6);
               }
            }
         }
      }
   }

   private void handleQualityModify(Player var1, String var2, Map<String, Object> var3, String var4, Map<String, Object> var5) {
      this.awaiting.add(var1);
      KomutechSupport.send(var1, "§a输入" + var4 + "品质（0~1）:");
      KomutechChatInput.waitFor(var1, var6 -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var6).trim())) {
            double var7;
            try {
               var7 = Double.parseDouble(stripColor(var6).trim());
            } catch (NumberFormatException var13) {
               KomutechSupport.send(var1, "§c无效数值");
               return;
            }

            if (!(var7 < 0.0) && !(var7 > 1.0)) {
               var5.put(var4, var7);
               var3.put("灵根品质", var5);
               double var9 = 0.0;

               for (String var12 : QUALITY_SLOTS.values()) {
                  var9 += PlayerAttributeStore.getDouble(var5, var12, 0.0);
               }

               var3.put("总品质", Math.round(var9 * 100.0) / 100.0);
               List var14 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var3, "灵根", ""));
               var3.put("灵根属性", CultivationMath.computeLinggenAttr(var14, var9));
               syncLingliBase(var3);
               this.refreshAdminView(var1, this.openSession(var1, var2, var3));
            } else {
               KomutechSupport.send(var1, "§c无效数值");
            }
         }
      });
   }

   private void handleSpeedModify(Player var1, String var2, Map<String, Object> var3) {
      this.awaiting.add(var1);
      int var4 = PlayerAttributeStore.getInt(var3, "速度", 0);
      List var5 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var3, "灵根", ""));
      double var6 = PlayerAttributeStore.getDouble(var3, "总品质", 0.01);
      double var8 = CultivationMath.computeActualValue(
         var4,
         CultivationMath.baseGrowth.getOrDefault("速度", 0.1),
         CultivationMath.getLinggenFinalCoefficient(var5, var6, "速度"),
         CultivationMath.getRealmCoefficient(var3),
         var6,
         PlayerAttributeStore.getDouble(var3, "根骨", 1.0)
      );
      KomutechSupport.send(var1, "§a当前速度点数: §e" + var4 + " §7(速度加成: §e+" + String.format("%.2f", var8) + "%§7)");
      KomutechSupport.send(var1, "§a请输入要设置的属性点数量（整数）:");
      KomutechChatInput.waitFor(var1, var4x -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var4x).trim())) {
            int var5x;
            try {
               var5x = Integer.parseInt(stripColor(var4x).trim());
            } catch (NumberFormatException var7) {
               KomutechSupport.send(var1, "§c点数必须为非负整数");
               return;
            }

            if (var5x < 0) {
               KomutechSupport.send(var1, "§c点数必须为非负整数");
            } else {
               var3.put("速度", var5x);
               AttributePointLimits.markAdminExempt(var3);
               this.refreshAdminView(var1, this.openSession(var1, var2, var3));
            }
         }
      });
   }

   private void handleSpiritModify(Player var1, String var2, Map<String, Object> var3) {
      this.awaiting.add(var1);
      KomutechSupport.send(var1, "§a请输入新的灵气值（直接输入数字如 100，或完整格式如 100/1000）:");
      KomutechChatInput.waitFor(var1, var4 -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var4).trim())) {
            String var5 = stripColor(var4).trim();
            String var6;
            if (var5.matches("\\d+")) {
               long var7 = Long.parseLong(var5);
               CultivationMath.CultivationInfo var9 = CultivationMath.getCultivationInfo(var7);
               var6 = var7 + "/" + var9.maxStr();
               var3.put("修为", var9.stage());
            } else {
               if (!var5.matches("\\d+/\\d+") && !"無".equals(var5)) {
                  KomutechSupport.send(var1, "§c格式错误");
                  return;
               }

               var6 = var5;
               Matcher var10 = Pattern.compile("^(\\d+)/").matcher(var5);
               if (var10.find()) {
                  var3.put("修为", CultivationMath.getCultivationInfo(Long.parseLong(var10.group(1))).stage());
               }
            }

            var3.put("灵气", var6);
            syncLingliBase(var3);
            this.refreshAdminView(var1, this.openSession(var1, var2, var3));
         }
      });
   }

   private void handleLingliBonusModify(Player var1, String var2, Map<String, Object> var3) {
      PlayerAttributeStore.Lingli var4 = PlayerAttributeStore.parseLingli(var3.get("灵力"));
      this.awaiting.add(var1);
      KomutechSupport.send(var1, "§a当前灵力: §e" + (int)var4.current() + "/" + (int)var4.totalMax() + " §7(额外: " + var4.bonus() + ")");
      KomutechSupport.send(var1, "§a请输入新的额外加成值（整数，可正可负）:");
      KomutechChatInput.waitFor(var1, var4x -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var4x).trim())) {
            int var5;
            try {
               var5 = Integer.parseInt(stripColor(var4x).trim());
            } catch (NumberFormatException var7) {
               KomutechSupport.send(var1, "§c无效数字");
               return;
            }

            PlayerAttributeStore.Lingli var6 = PlayerAttributeStore.parseLingli(var3.get("灵力"));
            var3.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var6.current(), var6.max(), var5)));
            this.refreshAdminView(var1, this.openSession(var1, var2, var3));
         }
      });
   }

   private void handleLinggenAttrModify(Player var1, String var2, Map<String, Object> var3) {
      this.awaiting.add(var1);
      KomutechSupport.send(var1, "§a输入新灵根（用顿号分隔，或输入“混沌”）:");
      KomutechChatInput.waitFor(var1, var4 -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var4).trim())) {
            String var5 = stripColor(var4).trim();
            if (!"混沌".equals(var5) && !"混沌灵根".equals(var5)) {
               if (!var5.matches(".*[、，\\s].*")) {
                  var5 = String.join("、", var5.split(""));
               } else {
                  var5 = String.join("、", CultivationMath.splitLinggen(var5.replace('，', '、')));
               }

               List<String> var13 = CultivationMath.splitLinggen(var5);
               List var14 = var13.stream().filter(var0 -> !CultivationMath.VALID_LINGGEN.contains(var0)).toList();
               if (!var14.isEmpty()) {
                  KomutechSupport.send(var1, "§c无效的灵根: " + String.join("、", var14) + "，请重新输入");
               } else {
                  List var15 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var3, "灵根", ""));
                  var3.put("灵根", var5);
                  Map<String, Object> var9 = PlayerAttributeStore.getMap(var3, "灵根品质");
                  var15.stream().filter(var1xx -> !var13.contains(var1xx)).forEach(var9::remove);

                  for (String var11 : var13) {
                     if (!var15.contains(var11)) {
                        var9.put(var11, isMutatedLinggen(var11) ? 1.0 : 0.01);
                     }
                  }

                  var3.put("灵根品质", var9);
                  double var16 = var13.stream().mapToDouble(var1xx -> PlayerAttributeStore.getDouble(var9, var1xx, 0.0)).sum();
                  var3.put("总品质", Math.round(var16 * 100.0) / 100.0);
                  var3.put("灵根属性", CultivationMath.computeLinggenAttr(var13, var16));
                  syncLingliBase(var3);
                  this.refreshAdminView(var1, this.openSession(var1, var2, var3));
               }
            } else {
               var3.put("灵根", String.join("、", CultivationMath.VALID_LINGGEN));
               HashMap var6 = new HashMap();

               for (String var8 : CultivationMath.VALID_LINGGEN) {
                  var6.put(var8, 0.1);
               }

               var3.put("灵根品质", var6);
               var3.put("总品质", 1.0);
               var3.put("灵根属性", "混沌灵根");
               syncLingliBase(var3);
               this.refreshAdminView(var1, this.openSession(var1, var2, var3));
            }
         }
      });
   }

   private void handleCommonAttrModify(Player var1, String var2, Map<String, Object> var3, String var4) {
      this.awaiting.add(var1);
      KomutechSupport.send(var1, "§a请输入新的" + var4 + "值:");
      KomutechChatInput.waitFor(var1, var5 -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var5).trim())) {
            int var6;
            try {
               var6 = Integer.parseInt(stripColor(var5).trim());
            } catch (NumberFormatException var8) {
               KomutechSupport.send(var1, "§c无效数字");
               return;
            }

            var3.put(var4, var6);
            if (AttributePointLimits.isLimited(var4)) {
               AttributePointLimits.markAdminExempt(var3);
            }

            this.refreshAdminView(var1, this.openSession(var1, var2, var3));
         }
      });
   }

   private void handleFoundationModify(Player var1, String var2, Map<String, Object> var3) {
      this.awaiting.add(var1);
      Map var4 = PlayerAttributeStore.getMap(var3, "根基");
      KomutechSupport.send(var1, "§a当前溢出的灵气: §e" + var4.getOrDefault("溢出的灵气", "0/0"));
      KomutechSupport.send(var1, "§a请输入新的溢出灵气值（格式如 12.5/50）:");
      KomutechChatInput.waitFor(var1, var4x -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var4x).trim())) {
            String var5 = stripColor(var4x).trim();
            if (!var5.matches("\\d+\\.?\\d*/\\d+\\.?\\d*")) {
               KomutechSupport.send(var1, "§c格式错误，正确格式如 12.5/50");
            } else {
               Map var6 = PlayerAttributeStore.getMap(var3, "根基");
               var6.put("溢出的灵气", var5);
               String[] var7 = var5.split("/");
               double var8 = Double.parseDouble(var7[0]);
               double var10 = Double.parseDouble(var7[1]);
               var6.put("稳固值", Math.min(25, (int)Math.floor(var8 / (var10 * 0.01))));
               var3.put("根基", var6);
               this.refreshAdminView(var1, this.openSession(var1, var2, var3));
            }
         }
      });
   }

   private void handleWuxingGenguModify(Player var1, String var2, Map<String, Object> var3, String var4) {
      this.awaiting.add(var1);
      KomutechSupport.send(var1, "§a请输入新的" + var4 + "值（当前: " + var3.get(var4) + "，需≤10）:");
      KomutechChatInput.waitFor(var1, var5 -> {
         this.awaiting.remove(var1);
         if (!"cancel".equalsIgnoreCase(stripColor(var5).trim())) {
            double var6;
            try {
               var6 = Double.parseDouble(stripColor(var5).trim());
            } catch (NumberFormatException var9) {
               KomutechSupport.send(var1, "§c无效数值（必须≤10）");
               return;
            }

            if (var6 > 10.0) {
               KomutechSupport.send(var1, "§c无效数值（必须≤10）");
            } else {
               var3.put(var4, Math.round(var6 * 100.0) / 100.0);
               this.refreshAdminView(var1, this.openSession(var1, var2, var3));
            }
         }
      });
   }

   private static void syncLingliBase(Map<String, Object> var0) {
      PlayerAttributeStore.Lingli var1 = PlayerAttributeStore.parseLingli(var0.get("灵力"));
      int var2 = CultivationMath.computeBaseMaxLingli(var0);
      var0.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var1.current(), var2, var1.bonus())));
   }

   private static boolean isMutatedLinggen(String var0) {
      return List.of("雷", "风", "冰", "光", "暗").contains(var0);
   }

   private PlayerAttributeMenuScript.Session openSession(Player var1, String var2, Map<String, Object> var3) {
      PlayerAttributeMenuScript.Session var4 = new PlayerAttributeMenuScript.Session();
      var4.viewPlayerName = var2;
      var4.viewData = var3;
      this.openPlayers.put(var1, var4);
      return var4;
   }

   private void openPlayer(Player var1) {
      Map var2 = PlayerAttributeStore.load(var1.getName());
      if (var2 == null) {
         this.openMenu(var1, this.buildPlayerMenu(var1, null), new PlayerAttributeMenuScript.Session());
      } else {
         CultivationMath.ensureDataComplete(var1.getName(), var2);
         AttributeApplier.applyPlayerAttributes(var1, var2, false);
         PlayerAttributeStore.save(var1.getName(), var2);
         Map var3 = PlayerAttributeStore.deepCopy(var2);
         Map var4 = PlayerAttributeStore.deepCopy(var2);
         PlayerAttributeMenuScript.Session var5 = new PlayerAttributeMenuScript.Session();
         var5.originalData = var4;
         var5.tempData = var3;
         this.openMenu(var1, this.buildPlayerMenu(var1, var3), var5);
      }
   }

   private void openPlayerWithTemp(Player var1, Map<String, Object> var2) {
      PlayerAttributeMenuScript.Session var3 = this.openPlayers.get(var1);
      PlayerAttributeMenuScript.Session var4 = new PlayerAttributeMenuScript.Session();
      var4.originalData = var3 == null ? null : var3.originalData;
      var4.tempData = var2;
      this.openMenu(var1, this.buildPlayerMenu(var1, var2), var4);
   }

   private void openAdminList(Player var1, int var2) {
      if (!isAdmin(var1)) {
         KomutechSupport.send(var1, "§c无权限");
         this.openPlayer(var1);
      } else {
         ArrayList<String> var3 = new ArrayList<>();

         try {
            Path var4 = PlayerAttributeStore.fileFor("x").getParent();
            if (Files.exists(var4)) {
               try (Stream<Path> var5 = Files.list(var4)) {
                  var5.filter(var0 -> {
                     String var1x = var0.getFileName().toString();
                     return var1x.startsWith("[") && var1x.endsWith("].json");
                  }).sorted().forEach(var1x -> var3.add(var1x.getFileName().toString().replaceAll("^\\[|\\]\\.json$", "")));
               }
            }
         } catch (IOException var15) {
         }

         int var16 = ADMIN_SLOTS.length;
         int var17 = Math.max(1, (int)Math.ceil((double)var3.size() / var16));
         int var6 = Math.min(Math.max(var2, 1), var17);
         Inventory var7 = MenuGuiHelper.create(54, "§c§l玩家属性 - 管理员模式");
         MenuGuiHelper.applyBorder(var7, BORDER);
         var7.setItem(4, MenuGuiHelper.item("COMPASS", "§6管理员模式", List.of("§7当前为管理员模式")));
         var7.setItem(6, MenuGuiHelper.item("PAPER", "§e总玩家数: " + var3.size(), List.of()));
         var7.setItem(8, MenuGuiHelper.item("COMPASS", "§a个人模式", List.of("§7点击切换")));
         var7.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of()));
         var7.setItem(53, MenuGuiHelper.item("TNT", "§c§l清除所有玩家数据", List.of("§7点击后需输入密码")));
         var7.setItem(48, var6 > 1 ? MenuGuiHelper.item("ARROW", "§a上一页", List.of()) : MenuGuiHelper.border());
         var7.setItem(50, var6 < var17 ? MenuGuiHelper.item("ARROW", "§a下一页", List.of()) : MenuGuiHelper.border());
         PlayerAttributeMenuScript.Session var8 = new PlayerAttributeMenuScript.Session();
         var8.page = var6;
         var8.totalPages = var17;
         int var9 = (var6 - 1) * var16;

         for (int var10 = 0; var10 < var16 && var9 + var10 < var3.size(); var10++) {
            String var11 = (String)var3.get(var9 + var10);
            int var12 = ADMIN_SLOTS[var10];
            var7.setItem(var12, MenuGuiHelper.item("PLAYER_HEAD", "§e" + var11, List.of("§7左键查看", "§7Shift+右键删除")));
            var8.slotMap.put(var12, var11);
         }

         this.openMenu(var1, var7, var8);
      }
   }

   private void openAdminView(Player var1, String var2) {
      if (!isAdmin(var1)) {
         KomutechSupport.send(var1, "§c无权限");
         this.openPlayer(var1);
      } else {
         Map var3 = PlayerAttributeStore.load(var2);
         if (var3 == null) {
            KomutechSupport.send(var1, "§c无数据");
         } else {
            Map var4 = PlayerAttributeStore.deepCopy(var3);
            PlayerAttributeMenuScript.Session var5 = new PlayerAttributeMenuScript.Session();
            var5.viewPlayerName = var2;
            var5.viewData = var4;
            this.openMenu(var1, this.buildAdminView(var2, var4), var5);
         }
      }
   }

   private void refreshAdminView(Player var1, PlayerAttributeMenuScript.Session var2) {
      this.openMenu(var1, this.buildAdminView(var2.viewPlayerName, var2.viewData), var2);
   }

   private Inventory buildPlayerMenu(Player var1, Map<String, Object> var2) {
      Inventory var3 = MenuGuiHelper.create(54, "§a§l玩家属性 - 个人模式");
      MenuGuiHelper.applyBorder(var3, BORDER);
      var3.setItem(4, MenuGuiHelper.item("PLAYER_HEAD", "§6" + var1.getName(), List.of("§7当前玩家")));
      var3.setItem(8, MenuGuiHelper.item("COMPASS", "§a管理员模式", List.of("§7点击切换")));
      var3.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of()));
      var3.setItem(53, MenuGuiHelper.item("LIME_STAINED_GLASS_PANE", "§a确认加点", List.of("§7点击保存并应用属性")));
      var3.setItem(48, MenuGuiHelper.border());
      var3.setItem(50, MenuGuiHelper.border());
      if (var2 == null) {
         var3.setItem(22, MenuGuiHelper.item("PAPER", "§c无玩家属性数据", List.of("§7你还没有进行过灵根鉴定")));
         return var3;
      } else {
         this.renderCommon(var3, var2, false);
         boolean var4 = CultivationMath.SWITCHABLE_ATTRS.stream().allMatch(var1x -> PlayerAttributeStore.getBoolean(var2, var1x + "_启用", true));
         var3.setItem(51, MenuGuiHelper.item("LEVER", var4 ? "§a全局启用中" : "§c全局禁用中", List.of("§7点击一键切换所有属性启用/禁用")));
         return var3;
      }
   }

   private Inventory buildAdminView(String var1, Map<String, Object> var2) {
      Inventory var3 = MenuGuiHelper.create(54, "§d§l玩家属性详情 - " + var1);
      MenuGuiHelper.applyBorder(var3, BORDER);
      var3.setItem(4, MenuGuiHelper.item("PLAYER_HEAD", "§6" + var1, List.of()));
      var3.setItem(8, MenuGuiHelper.item("COMPASS", "§a个人模式", List.of("§7返回个人属性")));
      var3.setItem(48, MenuGuiHelper.item("ARROW", "§a返回", List.of("§7返回玩家列表")));
      var3.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of()));
      var3.setItem(53, MenuGuiHelper.item("LIME_STAINED_GLASS_PANE", "§a确认修改", List.of("§7点击保存修改")));
      var3.setItem(50, MenuGuiHelper.border());
      this.renderCommon(var3, var2, true);
      if (var2.containsKey("突破失败")) {
         boolean var4 = PlayerAttributeStore.getBoolean(var2, "突破失败", false);
         var3.setItem(7, MenuGuiHelper.item(var4 ? "REDSTONE" : "EMERALD", "§e突破状态", List.of("§7突破失败: " + (var4 ? "§c是" : "§a否"), "§a右键切换状态")));
      }

      return var3;
   }

   private void renderCommon(Inventory var1, Map<String, Object> var2, boolean var3) {
      if (PlayerAttributeStore.getString(var2, "修为", null) != null) {
         var1.setItem(13, MenuGuiHelper.item("PAINTING", "§6修为", List.of("§7" + var2.get("修为"))));
      }

      if (PlayerAttributeStore.getString(var2, "灵气", null) != null) {
         ArrayList var4 = new ArrayList<>(List.of("§7" + var2.get("灵气")));
         if (var3) {
            var4.add("§a右键修改");
         }

         var1.setItem(22, MenuGuiHelper.item("EXPERIENCE_BOTTLE", "§6灵气", var4));
      }

      for (Entry var5 : ATTR_SLOTS.entrySet()) {
         int var6 = (Integer)var5.getKey();
         String var7 = (String)var5.getValue();
         if ((!"根基".equals(var7) || var2.containsKey("根基"))
            && (var2.containsKey(var7) || "根基".equals(var7))
            && (!"灵识".equals(var7) || CultivationMath.isGoldenCoreRealm(var2))) {
            List var8 = this.buildAttrLore(var2, var7, var3);

            String var9 = switch (var7) {
               case "功德", "煞气", "灵力" -> "GHAST_TEAR";
               case "根基" -> "BEDROCK";
               case "悟性", "根骨" -> "ENCHANTED_BOOK";
               case "血量" -> "APPLE";
               case "攻击力" -> "IRON_SWORD";
               case "防御力" -> "IRON_CHESTPLATE";
               case "速度" -> "FEATHER";
               case "灵气获取" -> "GLOWSTONE_DUST";
               case "灵识" -> "ENDER_EYE";
               case "属性点", "灵根属性" -> "NETHER_STAR";
               case "灵气" -> "EXPERIENCE_BOTTLE";
               case "修为" -> "PAINTING";
               default -> "PAPER";
            };
            var1.setItem(var6, MenuGuiHelper.item(var9, "§6" + var7, var8));
         }
      }

      String var13 = PlayerAttributeStore.getString(var2, "灵根", null);
      if (var13 != null) {
         ArrayList var14 = new ArrayList<>(
            List.of(
               "§7灵根: §e" + var13,
               "§7属性: §e" + PlayerAttributeStore.getString(var2, "灵根属性", "未知"),
               "§7总品质: §e" + PlayerAttributeStore.getString(var2, "总品质", "0.00")
            )
         );
         if (var3) {
            var14.add("§a右键修改灵根");
         }

         var1.setItem(40, MenuGuiHelper.item("NETHER_STAR", "§6灵根属性", var14));
      }

      Map<String, Object> var15 = PlayerAttributeStore.getMap(var2, "灵根品质");
      Map<String, String> var16 = Map.of(
         "金",
         "YELLOW_DYE",
         "木",
         "GREEN_DYE",
         "水",
         "BLUE_DYE",
         "火",
         "BLAZE_POWDER",
         "土",
         "BROWN_DYE",
         "雷",
         "PURPLE_DYE",
         "风",
         "WIND_CHARGE",
         "冰",
         "LIGHT_BLUE_DYE",
         "光",
         "WHITE_DYE",
         "暗",
         "BLACK_DYE"
      );

      for (Entry<Integer, String> var18 : QUALITY_SLOTS.entrySet()) {
         String var19 = (String)var18.getValue();
         if (var15.containsKey(var19)) {
            ArrayList<String> var10 = new ArrayList<>(List.of("§7品质: §f" + formatQuality(var15.get(var19))));
            if (var3) {
               var10.add("§a右键修改");
            }

            var1.setItem((Integer)var18.getKey(), MenuGuiHelper.item(var16.getOrDefault(var19, "PAPER"), "§e" + var19, var10));
         }
      }
   }

   private List<String> buildAttrLore(Map<String, Object> var1, String var2, boolean var3) {
      ArrayList var4 = new ArrayList();
      if ("灵力".equals(var2)) {
         PlayerAttributeStore.Lingli var7 = PlayerAttributeStore.parseLingli(var1.get("灵力"));
         var4.add("§7当前/最大: §f" + (int)var7.current() + "/" + (int)var7.totalMax());
         var4.add("§7额外加成: §a" + (var7.bonus() >= 0.0 ? "+" + var7.bonus() : var7.bonus()));
         if (var3) {
            var4.add("§a右键修改额外加成");
         }

         return var4;
      } else if ("根基".equals(var2)) {
         Map var6 = PlayerAttributeStore.getMap(var1, "根基");
         var4.add("§7稳固值: §f" + var6.getOrDefault("稳固值", 0));
         var4.add("§7溢出的灵气: §f" + var6.getOrDefault("溢出的灵气", "0/0"));
         if (var3) {
            var4.add("§a右键修改溢出灵气");
         }

         return var4;
      } else {
         var4.add("§7" + var1.get(var2));
         if (CultivationMath.SWITCHABLE_ATTRS.contains(var2)) {
            var4.add(this.getActualBonusDisplay(var1, var2, var3));
            var4.add(PlayerAttributeStore.getBoolean(var1, var2 + "_启用", true) ? "§a● 已启用" : "§c○ 已禁用");
            if (!var3) {
               var4.add("§7§oShift+左键切换状态");
            }
         }

         if ("灵气获取".equals(var2)) {
            var4.add(this.getActualBonusDisplay(var1, var2, var3));
         }

         if (!var3 && POINT_ATTRS.contains(var2)) {
            var4.add("§a左键 +1  §eShift+右键 +10  §c右键 -1");
         }

         if ("属性点".equals(var2) && !var3) {
            var4.add("§7在左侧属性图标上分配点数");
         }

         if (AttributePointLimits.isLimited(var2) && !var3) {
            int var5 = PlayerAttributeStore.getInt(var1, var2, 0);
            var4.add("§7加点上限: §e" + AttributePointLimits.limitLabel(var1, var2));
            if (AttributePointLimits.isAtLimit(var1, var2, var5)) {
               var4.add("§c已达上限");
            }
         }

         if (var3 && !"修为".equals(var2)) {
            var4.add("§a右键修改");
         }

         return var4;
      }
   }

   private String getActualBonusDisplay(Map<String, Object> var1, String var2, boolean var3) {
      if (var1.containsKey("灵根") && var1.containsKey("总品质")) {
         List var4 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var1, "灵根", ""));
         double var5 = PlayerAttributeStore.getDouble(var1, "总品质", 0.01);
         double var7 = CultivationMath.getRealmCoefficient(var1);
         double var9 = CultivationMath.getLinggenFinalCoefficient(var4, var5, var2);
         double var11 = PlayerAttributeStore.getDouble(var1, var2, 0.0);
         double var13 = PlayerAttributeStore.getDouble(var1, "根骨", 1.0);
         if (var11 == 0.0) {
            if ("速度".equals(var2)) {
               return "§7实际增加: §a+0.00%";
            } else if ("灵气获取".equals(var2)) {
               return "§7修炼效率: §a100%";
            } else {
               return "灵识".equals(var2) ? "§7实际增加: §a+0.00 格" : "§7实际增加: §a0.00 " + var2;
            }
         } else if ("灵气获取".equals(var2)) {
            int var23 = (int)var11;
            double var16 = AttributePointLimits.spiritGainPercent(var1, var23);
            double var18 = AttributePointLimits.limit(var1, var2);
            return var16 >= var18 - 1.0E-6 ? String.format("§7修炼效率: §a%.0f%% §7(上限 %.0f%%)", var16, var18) : String.format("§7修炼效率: §a%.0f%%", var16);
         } else {
            double var15 = CultivationMath.computeActualValue(var11, CultivationMath.baseGrowth.getOrDefault(var2, 0.0), var9, var7, var5, var13);
            double var17 = AttributePointLimits.clampForApply(var1, var2, var15, var3);
            double var19 = AttributePointLimits.isLimited(var2) ? AttributePointLimits.limit(var1, var2) : Double.MAX_VALUE;
            if ("速度".equals(var2)) {
               return var15 >= var19 - 1.0E-6
                  ? String.format("§7实际增加: §a+%.2f%% §7(上限 %.0f%%)", Math.min(var17, var19), var19)
                  : String.format("§7实际增加: §a+%.2f%%", var17);
            } else if ("灵识".equals(var2)) {
               double var21 = Math.min(var17, 5.0);
               return var15 >= 5.0 ? String.format("§7实际增加: §a+%.2f 格 §7(已达上限)", var21) : String.format("§7实际增加: §a+%.2f 格", var17);
            } else {
               return var15 >= var19 - 1.0E-6
                  ? String.format("§7实际增加: §a+%.2f %s §7(上限 %.0f)", Math.min(var17, var19), var2, var19)
                  : String.format("§7实际增加: §a+%.2f %s", var17, var2);
            }
         }
      } else {
         return "§7实际增加: §c未知";
      }
   }

   private void toggleAllAttributes(Player var1, Map<String, Object> var2) {
      boolean var3 = !CultivationMath.SWITCHABLE_ATTRS.stream().allMatch(var1x -> PlayerAttributeStore.getBoolean(var2, var1x + "_启用", true));

      for (String var5 : CultivationMath.SWITCHABLE_ATTRS) {
         var2.put(var5 + "_启用", var3);
      }

      KomutechSupport.send(var1, "§a所有战斗属性已" + (var3 ? "启用" : "禁用"));
   }

   private void openMenu(Player var1, Inventory var2, PlayerAttributeMenuScript.Session var3) {
      var1.openInventory(var2);
      this.openPlayers.put(var1, var3);
   }

   private static boolean isAdmin(Player var0) {
      return var0.isOp() || "Komu_A".equals(var0.getName());
   }

   private static String formatQuality(Object var0) {
      return var0 instanceof Number var1 ? String.format("%.2f", var1.doubleValue()) : String.valueOf(var0);
   }

   private static String stripColor(String var0) {
      return var0 == null ? "" : var0.replaceAll("§.", "");
   }

   private static final class Session {
      Map<String, Object> originalData;
      Map<String, Object> tempData;
      Map<String, Object> viewData;
      String viewPlayerName;
      int page = 1;
      int totalPages = 1;
      Map<Integer, String> slotMap = new HashMap<>();
   }
}
