package tech.komutech.native_scripts.tool;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
import tech.komutech.native_scripts.support.KomutechAsyncScheduler;
import tech.komutech.native_scripts.support.KomutechConfigMerge;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechPaths;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MainThread;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class YunZhuanXiaScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String TITLE = "§b云篆匣";
   private static final int SIZE = 9;
   private static final String SCROLL_PREFIX = "KOMUTECH_L_JZ_";
   private static final long COOLDOWN_MS = 1000L;
   private final Map<UUID, Long> cooldowns = new HashMap<>();
   private final Map<Player, YunZhuanXiaScript.OpenState> openPlayers = new HashMap<>();
   private Plugin plugin;
   private Map<String, Object> scrollConfig;
   private long scrollConfigMtime = Long.MIN_VALUE;

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MenuGuiHelper.titleStartsWith(var1, "§b云篆匣");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      long var3 = System.currentTimeMillis();
      long var5 = this.cooldowns.getOrDefault(var2.getUniqueId(), 0L);
      if (var3 - var5 < 1000L) {
         double var9 = Math.ceil((1000L - (var3 - var5)) / 100.0) / 10.0;
         KomutechSupport.actionBar(var2, "§c云篆匣冷却中，请等待 §6" + var9 + " §c秒");
         return null;
      } else if (MenuGuiHelper.titleStartsWith(var2.getOpenInventory(), "§b云篆匣")) {
         KomutechSupport.send(var2, "§c云篆匣已打开，请先关闭当前界面再试！");
         return null;
      } else {
         this.cooldowns.put(var2.getUniqueId(), var3);
         String var7 = var2.getName();
         Map[] var8 = new Map[1];
         KomutechAsyncScheduler.submit(() -> var8[0] = this.load(var7), var3x -> MainThread.run(this.plugin, () -> this.openInventory(var2, var8[0])));
         return null;
      }
   }

   private void openInventory(Player var1, Map<String, Object> var2) {
      int var3 = this.unlockedSlots(var1);
      Inventory var4 = MenuGuiHelper.create(9, "§b云篆匣");
      List var5 = ensureScrollList(var2);
      Map var6 = KomutechJson.asMap(var2.get("熟练度记录"));

      for (int var7 = 0; var7 < 9; var7++) {
         if (var7 < var3) {
            Object var8 = var7 < var5.size() ? var5.get(var7) : null;
            if (var8 != null) {
               String var9 = String.valueOf(var8);
               int var10 = KomutechJson.getInt(var6, skillName(var9), 0);
               ItemStack var11 = this.makeDisplayItem(var9, var10);
               if (var11 != null) {
                  var4.setItem(var7, var11);
               }
            }
         } else {
            var4.setItem(var7, MenuGuiHelper.item(Material.BARRIER, "§c未解锁", List.of("§7需要更高的悟性")));
         }
      }

      this.openPlayers.put(var1, new YunZhuanXiaScript.OpenState(var2, var3));
      var1.openInventory(var4);
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (MenuGuiHelper.titleStartsWith(var1.getView(), "§b云篆匣")) {
            var1.setCancelled(true);
            YunZhuanXiaScript.OpenState var20 = this.openPlayers.get(var2);
            if (var20 != null) {
               InventoryView var4 = var1.getView();
               Inventory var5 = var4.getTopInventory();
               Inventory var6 = var1.getClickedInventory();
               if (var6 != null) {
                  int var7 = var1.getRawSlot();
                  Map var8 = var20.data();
                  int var9 = var20.wuxing();
                  List var10 = ensureScrollList(var8);
                  String var11 = var2.getName();
                  if (var6.equals(var5) && var7 >= 0 && var7 < var9) {
                     String var21 = var7 < var10.size() && var10.get(var7) != null ? String.valueOf(var10.get(var7)) : null;
                     if (var21 != null) {
                        SlimefunItem var22 = SlimefunItem.getById(var21);
                        if (var22 != null) {
                           HashMap var23 = var2.getInventory().addItem(new ItemStack[]{var22.getItem().clone()});
                           if (!var23.isEmpty()) {
                              KomutechSupport.send(var2, "§c背包已满，无法取出");
                           } else {
                              var10.set(var7, null);
                              var8.put("卷轴数据", var10);
                              this.saveAsync(var11, var8);
                              var5.setItem(var7, null);
                           }
                        }
                     }
                  } else {
                     if (var6.equals(var2.getInventory())) {
                        ItemStack var12 = var1.getCurrentItem();
                        if (var12 == null || var12.getType().isAir()) {
                           return;
                        }

                        SlimefunItem var13 = SlimefunItem.getByItem(var12);
                        if (var13 == null || !var13.getId().toUpperCase(Locale.ROOT).startsWith("KOMUTECH_L_JZ_")) {
                           KomutechSupport.send(var2, "§c只能存入卷轴物品");
                           return;
                        }

                        String var14 = var13.getId();

                        for (int var15 = 0; var15 < var9; var15++) {
                           if (var14.equals(String.valueOf(var10.get(var15)))) {
                              KomutechSupport.send(var2, "§c此卷轴已经存入了");
                              return;
                           }
                        }

                        int var24 = -1;

                        for (int var16 = 0; var16 < var9; var16++) {
                           if (var10.get(var16) == null) {
                              var24 = var16;
                              break;
                           }
                        }

                        if (var24 == -1) {
                           KomutechSupport.send(var2, "§c可用云篆匣已满（当前悟性：" + var9 + "）");
                           return;
                        }

                        if (var12.getAmount() > 1) {
                           var12.setAmount(var12.getAmount() - 1);
                        } else {
                           var2.getInventory().setItem(var1.getSlot(), null);
                        }

                        var10.set(var24, var14);
                        HashMap var25 = new HashMap<>(KomutechJson.asMap(var8.get("熟练度记录")));
                        String var17 = skillName(var14);
                        var25.putIfAbsent(var17, 0);
                        int var18 = KomutechJson.getInt(var25, var17, 0);
                        var8.put("卷轴数据", var10);
                        var8.put("熟练度记录", var25);
                        this.saveAsync(var11, var8);
                        ItemStack var19 = this.makeDisplayItem(var14, var18);
                        if (var19 != null) {
                           var5.setItem(var24, var19);
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
      if (var1.getWhoClicked() instanceof Player var2 && this.openPlayers.containsKey(var2) && MenuGuiHelper.titleStartsWith(var1.getView(), "§b云篆匣")) {
         var1.setCancelled(true);
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (MenuGuiHelper.titleStartsWith(var1.getView(), "§b云篆匣")) {
            if (var2.getOpenInventory().getTopInventory() != var1.getInventory()) {
               this.openPlayers.remove(var2);
            }
         }
      }
   }

   private int unlockedSlots(Player var1) {
      Map var2 = PlayerAttributeStore.load(var1.getName());
      double var3 = var2 == null ? 1.0 : PlayerAttributeStore.getDouble(var2, "悟性", 1.0);
      return Math.max(1, Math.min(9, (int)Math.floor(var3)));
   }

   private static List<Object> ensureScrollList(Map<String, Object> var0) {
      ArrayList var1 = new ArrayList<>(KomutechJson.asList(var0.computeIfAbsent("卷轴数据", var0x -> emptyScrolls())));

      while (var1.size() < 9) {
         var1.add(null);
      }

      return var1;
   }

   private static List<Object> emptyScrolls() {
      ArrayList var0 = new ArrayList();

      for (int var1 = 0; var1 < 9; var1++) {
         var0.add(null);
      }

      return var0;
   }

   private Map<String, Object> load(String var1) {
      try {
         Path var2 = KomutechPaths.playerFile(KomutechPaths.yunZhuanXia(), var1, "云篆匣");
         if (!Files.exists(var2)) {
            HashMap var6 = new HashMap();
            var6.put("卷轴数据", emptyScrolls());
            var6.put("熟练度记录", new HashMap());
            return var6;
         } else {
            Map var5 = KomutechJson.asMap(KomutechJson.parse(Files.readString(var2, StandardCharsets.UTF_8)));
            ensureScrollList(var5);
            return var5;
         }
      } catch (Exception var4) {
         HashMap var3 = new HashMap();
         var3.put("卷轴数据", emptyScrolls());
         var3.put("熟练度记录", new HashMap());
         return var3;
      }
   }

   private void saveAsync(String var1, Map<String, Object> var2) {
      KomutechAsyncScheduler.submit(() -> this.save(var1, var2), var0 -> {});
   }

   private void save(String var1, Map<String, Object> var2) {
      try {
         Files.createDirectories(KomutechPaths.yunZhuanXia());
         Files.writeString(KomutechPaths.playerFile(KomutechPaths.yunZhuanXia(), var1, "云篆匣"), KomutechJson.stringify(var2), StandardCharsets.UTF_8);
      } catch (Exception var4) {
      }
   }

   private ItemStack makeDisplayItem(String var1, int var2) {
      SlimefunItem var3 = SlimefunItem.getById(var1);
      if (var3 == null) {
         return null;
      } else {
         ItemStack var4 = var3.getItem().clone();
         ItemMeta var5 = var4.getItemMeta();
         ArrayList<String> var6 = var5.getLore() == null ? new ArrayList<>() : new ArrayList<>(var5.getLore());
         var6.removeIf(var0 -> var0.startsWith("§7熟练度："));
         int var7 = this.getMaxProficiency(skillName(var1));
         var6.add("§7熟练度：§f" + var2 + " §7/ §f" + var7);
         var5.setLore(var6);
         var4.setItemMeta(var5);
         return var4;
      }
   }

   private int getMaxProficiency(String var1) {
      this.ensureScrollConfig();
      if (this.scrollConfig != null && var1 != null && !var1.isBlank()) {
         String var2 = ScrollCombatEngine.normalizeSkillId(var1);
         Map var3 = KomutechJson.asMap(this.scrollConfig.get(var2));
         if (var3.isEmpty()) {
            this.scrollConfig = null;
            this.scrollConfigMtime = Long.MIN_VALUE;
            this.ensureScrollConfig();
            var3 = KomutechJson.asMap(this.scrollConfig.get(var2));
         }

         return KomutechJson.getInt(var3, "熟练度上限", 0);
      } else {
         return 0;
      }
   }

   private void ensureScrollConfig() {
      Path var1 = KomutechPaths.scrollConfig();
      long var2 = -1L;

      try {
         if (Files.exists(var1)) {
            var2 = Files.getLastModifiedTime(var1).toMillis();
         }
      } catch (Exception var6) {
      }

      if (this.scrollConfig == null || var2 != this.scrollConfigMtime) {
         this.scrollConfig = KomutechConfigMerge.ensureTemplate(var1, "卷轴属性.json");

         try {
            if (Files.exists(var1)) {
               var2 = Files.getLastModifiedTime(var1).toMillis();
            }
         } catch (Exception var5) {
         }

         this.scrollConfigMtime = var2;
      }
   }

   private static String skillName(String var0) {
      return ScrollCombatEngine.normalizeSkillId(var0);
   }

   private record OpenState(Map<String, Object> data, int wuxing) {
   }
}
