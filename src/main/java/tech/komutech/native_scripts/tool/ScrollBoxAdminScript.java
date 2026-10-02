package tech.komutech.native_scripts.tool;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.combat.ScrollCombatEngine;
import tech.komutech.native_scripts.support.ChatInputService;
import tech.komutech.native_scripts.support.KomutechConfigMerge;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechPaths;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class ScrollBoxAdminScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String TITLE_LIST = "§c§l云篆匣管理";
   private static final String TITLE_DETAIL = "§d§l玩家卷轴详情 - ";
   private static final String SCROLL_PREFIX = "KOMUTECH_L_JZ_";
   private static final int SIZE = 54;
   private static final int[] BORDER_SLOTS = new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53};
   private static final int[] AVAILABLE_SLOTS = new int[]{
      10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
   };
   private final Map<UUID, ScrollBoxAdminScript.MenuState> openPlayers = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> awaitingInput = new ConcurrentHashMap<>();
   private Map<String, Object> scrollConfig = Map.of();
   private Plugin plugin;

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
      this.reloadConfig();
   }

   @Override
   public boolean handles(InventoryView var1) {
      String var2 = var1.getTitle();
      return "§c§l云篆匣管理".equals(var2) || var2.startsWith("§d§l玩家卷轴详情 - ");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return "onUse".equals(var1) ? UseEvents.parse(var2[0]).map(this::handleUse).orElse(null) : null;
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      if (!var2.isOp() && !var2.hasPermission("komutech.admin")) {
         KomutechSupport.send(var2, "§c你没有权限使用此道具");
         return null;
      } else {
         this.openList(var2, 1);
         return null;
      }
   }

   private void reloadConfig() {
      this.scrollConfig = KomutechConfigMerge.ensureTemplate(KomutechPaths.scrollConfig(), "卷轴属性.json");
   }

   private void openList(Player var1, int var2) {
      List var3 = this.listPlayerFiles();
      int var4 = Math.max(1, (int)Math.ceil((double)var3.size() / AVAILABLE_SLOTS.length));
      var2 = Math.max(1, Math.min(var2, var4));
      Inventory var5 = MenuGuiHelper.create(54, "§c§l云篆匣管理");
      MenuGuiHelper.applyBorder(var5, BORDER_SLOTS);
      var5.setItem(4, MenuGuiHelper.item(Material.PAINTING, "§e云篆匣管理", List.of("§7总玩家数: " + var3.size())));
      var5.setItem(49, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
      var5.setItem(48, var2 > 1 ? MenuGuiHelper.item(Material.ARROW, "§a上一页", List.of()) : MenuGuiHelper.border());
      var5.setItem(50, var2 < var4 ? MenuGuiHelper.item(Material.ARROW, "§a下一页", List.of()) : MenuGuiHelper.border());
      HashMap var6 = new HashMap();
      int var7 = (var2 - 1) * AVAILABLE_SLOTS.length;

      for (int var8 = 0; var8 < AVAILABLE_SLOTS.length && var7 + var8 < var3.size(); var8++) {
         String var9 = (String)var3.get(var7 + var8);
         int var10 = AVAILABLE_SLOTS[var8];
         var5.setItem(var10, MenuGuiHelper.item(Material.PLAYER_HEAD, "§e" + var9, List.of("§7左键查看卷轴")));
         var6.put(var10, var9);
      }

      var1.openInventory(var5);
      this.openPlayers.put(var1.getUniqueId(), ScrollBoxAdminScript.MenuState.list(var2, var4, var6));
   }

   private void openDetail(Player var1, String var2) {
      List var3 = this.getScrollList(var2);
      Inventory var4 = MenuGuiHelper.create(54, "§d§l玩家卷轴详情 - " + var2);
      MenuGuiHelper.applyBorder(var4, BORDER_SLOTS);
      var4.setItem(4, MenuGuiHelper.item(Material.PLAYER_HEAD, "§6" + var2, List.of()));
      var4.setItem(0, MenuGuiHelper.item(Material.ARROW, "§a返回", List.of("§7返回玩家列表")));
      var4.setItem(49, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
      var4.setItem(48, MenuGuiHelper.border());
      var4.setItem(50, MenuGuiHelper.border());
      HashMap var5 = new HashMap();
      if (var3.isEmpty()) {
         var4.setItem(22, MenuGuiHelper.item(Material.PAPER, "§c该玩家暂未存入卷轴", List.of()));
      } else {
         for (int var6 = 0; var6 < Math.min(var3.size(), AVAILABLE_SLOTS.length); var6++) {
            String var7 = (String)var3.get(var6);
            String var8 = skillName(var7);
            int var9 = this.getScrollProf(var2, var8);
            int var10 = AVAILABLE_SLOTS[var6];
            var4.setItem(var10, this.makeDisplayItem(var7, var9));
            var5.put(var10, var7);
         }
      }

      var1.openInventory(var4);
      this.openPlayers.put(var1.getUniqueId(), ScrollBoxAdminScript.MenuState.detail(var2, var5));
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         ScrollBoxAdminScript.MenuState var7 = this.openPlayers.get(var2.getUniqueId());
         if (var7 != null) {
            String var4 = var1.getView().getTitle();
            if ("§c§l云篆匣管理".equals(var4) || var4.startsWith("§d§l玩家卷轴详情 - ")) {
               var1.setCancelled(true);
               ItemStack var5 = var1.getCurrentItem();
               if (var5 != null && !var5.getType().isAir()) {
                  int var6 = var1.getSlot();
                  if ("§c§l云篆匣管理".equals(var4)) {
                     this.handleListClick(var2, var6, var7);
                  } else {
                     this.handleDetailClick(var2, var6, var7, var1.isRightClick());
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2 && this.openPlayers.containsKey(var2.getUniqueId())) {
         var1.setCancelled(true);
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         this.openPlayers.remove(var2.getUniqueId());
         this.awaitingInput.remove(var2.getUniqueId());
      }
   }

   private void handleListClick(Player var1, int var2, ScrollBoxAdminScript.MenuState var3) {
      if (var2 == 49) {
         var1.closeInventory();
      } else if (var2 == 48 && var3.page > 1) {
         this.openList(var1, var3.page - 1);
      } else if (var2 == 50 && var3.page < var3.totalPages) {
         this.openList(var1, var3.page + 1);
      } else if (var3.slotMap != null && var3.slotMap.containsKey(var2)) {
         this.openDetail(var1, var3.slotMap.get(var2));
      }
   }

   private void handleDetailClick(Player var1, int var2, ScrollBoxAdminScript.MenuState var3, boolean var4) {
      if (var2 == 0) {
         this.openList(var1, 1);
      } else if (var2 == 49) {
         var1.closeInventory();
      } else if (var4 && var3.scrollMap != null && var3.scrollMap.containsKey(var2)) {
         if (this.awaitingInput.containsKey(var1.getUniqueId())) {
            return;
         }

         String var5 = var3.scrollMap.get(var2);
         String var6 = skillName(var5);
         String var7 = var3.playerName;
         this.awaitingInput.put(var1.getUniqueId(), true);
         ChatInputService.request(var1, "§a请输入 §e" + var6 + " §a的新熟练度值（数字）:", var4x -> {
            this.awaitingInput.remove(var1.getUniqueId());
            if ("cancel".equalsIgnoreCase(var4x.trim())) {
               KomutechSupport.send(var1, "§c已取消");
            } else {
               try {
                  int var5x = Integer.parseInt(var4x.trim());
                  if (var5x < 0) {
                     KomutechSupport.send(var1, "§c请输入非负整数");
                     return;
                  }

                  int var6x = this.getMaxProf(var6);
                  int var7x = var6x > 0 ? Math.min(var5x, var6x) : var5x;
                  if (this.setScrollProf(var7, var6, var7x)) {
                     KomutechSupport.send(var1, "§a熟练度已更新为 " + var7x);
                     this.openDetail(var1, var7);
                  } else {
                     KomutechSupport.send(var1, "§c保存失败");
                  }
               } catch (NumberFormatException var8) {
                  KomutechSupport.send(var1, "§c请输入非负整数");
               }
            }
         });
      }
   }

   private List<String> listPlayerFiles() {
      try {
         Path var1 = KomutechPaths.yunZhuanXia();
         if (!Files.isDirectory(var1)) {
            return List.of();
         } else {
            ArrayList<String> var2 = new ArrayList<>();

            try (Stream<Path> var3 = Files.list(var1)) {
               var3.filter(var0 -> {
                  String var1x = var0.getFileName().toString();
                  return var1x.startsWith("[") && var1x.endsWith("]云篆匣.json");
               }).forEach(var1x -> {
                  String var2x = var1x.getFileName().toString();
                  var2.add(var2x.substring(1, var2x.length() - "]云篆匣.json".length()));
               });
            }

            var2.sort(String::compareTo);
            return var2;
         }
      } catch (Exception var8) {
         return List.of();
      }
   }

   private Map<String, Object> loadPlayerData(String var1) {
      try {
         Path var2 = KomutechPaths.playerFile(KomutechPaths.yunZhuanXia(), var1, "云篆匣");
         return !Files.exists(var2) ? null : KomutechJson.asMap(KomutechJson.parse(Files.readString(var2, StandardCharsets.UTF_8)));
      } catch (Exception var3) {
         return null;
      }
   }

   private boolean savePlayerData(String var1, Map<String, Object> var2) {
      try {
         Files.createDirectories(KomutechPaths.yunZhuanXia());
         Files.writeString(KomutechPaths.playerFile(KomutechPaths.yunZhuanXia(), var1, "云篆匣"), KomutechJson.stringify(var2), StandardCharsets.UTF_8);
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   private List<String> getScrollList(String var1) {
      Map var2 = this.loadPlayerData(var1);
      if (var2 == null) {
         return List.of();
      } else {
         List var3 = KomutechJson.asList(var2.get("卷轴数据"));
         ArrayList var4 = new ArrayList();

         for (Object var6 : var3) {
            if (var6 != null) {
               var4.add(String.valueOf(var6));
            }
         }

         return var4;
      }
   }

   private int getScrollProf(String var1, String var2) {
      Map var3 = this.loadPlayerData(var1);
      if (var3 == null) {
         return 0;
      } else {
         return KomutechJson.asMap(var3.get("熟练度记录")).get(var2) instanceof Number var5 ? var5.intValue() : 0;
      }
   }

   private boolean setScrollProf(String var1, String var2, int var3) {
      Map var4 = this.loadPlayerData(var1);
      if (var4 == null) {
         return false;
      } else {
         HashMap var5 = new HashMap<>(KomutechJson.asMap(var4.get("熟练度记录")));
         var5.put(var2, var3);
         var4.put("熟练度记录", var5);
         return this.savePlayerData(var1, var4);
      }
   }

   private int getMaxProf(String var1) {
      String var2 = ScrollCombatEngine.normalizeSkillId(var1);
      this.reloadConfig();
      if (this.scrollConfig.get(var2) instanceof Map var4) {
         return var4.get("熟练度上限") instanceof Number var6 ? var6.intValue() : 0;
      } else {
         return 0;
      }
   }

   private static String skillName(String var0) {
      return ScrollCombatEngine.normalizeSkillId(var0);
   }

   private ItemStack makeDisplayItem(String var1, int var2) {
      SlimefunItem var3 = SlimefunItem.getById(var1);
      if (var3 == null) {
         return MenuGuiHelper.item(Material.BARRIER, "§c未知卷轴", List.of(var1));
      } else {
         ItemStack var4 = var3.getItem().clone();
         ItemMeta var5 = var4.getItemMeta();
         if (var5 == null) {
            return var4;
         } else {
            ArrayList<String> var6 = var5.hasLore() ? new ArrayList<>(var5.getLore()) : new ArrayList<>();
            var6.removeIf(var0 -> var0.startsWith("§7熟练度："));
            String var7 = skillName(var1);
            int var8 = this.getMaxProf(var7);
            var6.add("§7熟练度：§f" + var2 + " §7/ §f" + var8);
            var5.setLore(var6);
            var4.setItemMeta(var5);
            return var4;
         }
      }
   }

   private record MenuState(int page, int totalPages, Map<Integer, String> slotMap, String playerName, Map<Integer, String> scrollMap) {
      static ScrollBoxAdminScript.MenuState list(int var0, int var1, Map<Integer, String> var2) {
         return new ScrollBoxAdminScript.MenuState(var0, var1, var2, null, null);
      }

      static ScrollBoxAdminScript.MenuState detail(String var0, Map<Integer, String> var1) {
         return new ScrollBoxAdminScript.MenuState(1, 1, null, var0, var1);
      }
   }
}
