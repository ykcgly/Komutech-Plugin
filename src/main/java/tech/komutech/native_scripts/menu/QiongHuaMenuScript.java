package tech.komutech.native_scripts.menu;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
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
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;

public final class QiongHuaMenuScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String MAIN_TITLE = "§6§l✨ 琼华阁 ✨";
   private static final String TITLE_PREFIX = "§6§l✨ ";
   private static final String TITLE_SUFFIX = " ✨";
   private static final int[] BORDER = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
   private static final Map<Integer, String> MAIN_SLOTS = Map.of(10, "jichucailiao", 11, "lingzhizhongzi", 12, "leisi");
   private static final Map<String, String> CATEGORY_NAMES = Map.of("jichucailiao", "§6✦ 基础材料", "lingzhizhongzi", "§2✦ 灵植种子", "leisi", "§8✦ 儡肆");
   private static final Map<String, List<QiongHuaMenuScript.ShopEntry>> SHOPS = Map.of(
      "jichucailiao",
      List.of(
         entry("KOMUTECH_L_JCWP_琼华阁通行柬1", "KOMUTECH_L_DJ_ZPLS", 64, "KOMUTECH_L_DJ_功德券", 4),
         entry("KOMUTECH_L_JCWP_SC", "KOMUTECH_L_DJ_XPLS", 16),
         entry("KOMUTECH_L_JCWP_SYM", "KOMUTECH_L_DJ_XPLS", 4),
         entry("KOMUTECH_L_GJ_KCB", "KOMUTECH_L_DJ_XPLS", 8)
      ),
      "lingzhizhongzi",
      List.of(
         entry("KOMUTECH_L_LZZZ_TSSM", "KOMUTECH_L_DJ_XPLS", 16),
         entry("KOMUTECH_L_LZZZ_SSSM", "KOMUTECH_L_DJ_XPLS", 16),
         entry("KOMUTECH_L_LZZZ_YGZZ", "KOMUTECH_L_DJ_XPLS", 16),
         entry("KOMUTECH_L_LZZZ_MHZZ", "KOMUTECH_L_DJ_XPLS", 16),
         entry("KOMUTECH_L_LZZZ_MZZ", "KOMUTECH_L_DJ_XPLS", 16)
      ),
      "leisi",
      List.of(
         entry("KOMUTECH_L_JCWP_儡肆通行柬", "KOMUTECH_L_DJ_SPLS", 64, "KOMUTECH_L_DJ_功德券", 16),
         entry("KOMUTECH_L_SW_KLN", "KOMUTECH_L_DJ_SPLS", 64, "KOMUTECH_L_KW_TXY", 4)
      )
   );
   private final Map<Player, String> openPlayers = new ConcurrentHashMap<>();
   private final Map<Player, Map<Integer, QiongHuaMenuScript.ShopEntry>> playerSlotMaps = new ConcurrentHashMap<>();
   private Plugin plugin;

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      if (var1 == null) {
         return false;
      } else {
         return KomutechMenuRouter.isHandlerFor(var1.getTopInventory(), this)
            ? true
            : isQiongHuaTitle(MenuGuiHelper.legacyTitle(var1)) || isQiongHuaTitle(var1.getTitle());
      }
   }

   private static boolean isQiongHuaTitle(String var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if ("§6§l✨ 琼华阁 ✨".equals(var0)) {
         return true;
      } else {
         String var1 = ChatColor.stripColor(var0);
         if (var1.contains("琼华阁")) {
            return true;
         } else {
            boolean var2 = var0.startsWith("§6§l✨ ") || var1.startsWith("✨ ");
            if (!var2) {
               return SHOPS.containsKey(var1.trim()) || var1.contains("基础材料") || var1.contains("灵植种子") || var1.contains("儡肆");
            } else {
               return SHOPS.keySet().stream().anyMatch(var2x -> var0.contains(var2x) || var1.contains(var2x))
                  ? true
                  : var1.contains("基础材料") || var1.contains("灵植种子") || var1.contains("儡肆");
            }
         }
      }
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onButtonGroupClick".equals(var1) && var2[0] instanceof Player var6) {
         this.openMain(var6);
         return true;
      } else {
         if ("onUse".equals(var1) && var2.length > 0) {
            try {
               Player var3 = (Player)var2[0].getClass().getMethod("getPlayer").invoke(var2[0]);
               this.openMain(var3);
            } catch (ReflectiveOperationException var5) {
            }
         }

         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (this.handles(var1.getView())) {
            var1.setCancelled(true);
            if (var1.getClickedInventory() == null || !var1.getClickedInventory().equals(var1.getView().getBottomInventory())) {
               int var7 = var1.getRawSlot();
               if (var7 >= 0 && var7 < var1.getView().getTopInventory().getSize()) {
                  String var4 = this.openPlayers.getOrDefault(var2, this.resolvePageFromTitle(var1.getView()));
                  if ("main".equals(var4) || isMainTitle(var1.getView())) {
                     String var8 = MAIN_SLOTS.get(var7);
                     if (var8 != null) {
                        this.openCategory(var2, var8);
                     } else if (var7 == 49) {
                        var2.closeInventory();
                     }
                  } else if (var7 == 49) {
                     this.openMain(var2);
                  } else {
                     Map var5 = this.playerSlotMaps.get(var2);
                     QiongHuaMenuScript.ShopEntry var6 = var5 == null ? null : (QiongHuaMenuScript.ShopEntry)var5.get(var7);
                     if (var6 != null) {
                        this.purchase(var2, var6, var1.isShiftClick() ? 64 : 1);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (this.handles(var1.getView())) {
         var1.setCancelled(true);
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (this.plugin == null) {
            this.clearPlayer(var2);
         } else {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
               if (!var2.isOnline()) {
                  this.clearPlayer(var2);
               } else {
                  InventoryView var2x = var2.getOpenInventory();
                  if (var2x == null || !this.isOpenQiongHuaView(var2x)) {
                     this.clearPlayer(var2);
                  }
               }
            });
         }
      }
   }

   private boolean isOpenQiongHuaView(InventoryView var1) {
      return KomutechMenuRouter.isHandlerFor(var1.getTopInventory(), this)
         ? true
         : isQiongHuaTitle(MenuGuiHelper.legacyTitle(var1)) || isQiongHuaTitle(var1.getTitle());
   }

   private void clearPlayer(Player var1) {
      this.openPlayers.remove(var1);
      this.playerSlotMaps.remove(var1);
   }

   private void openMain(Player var1) {
      Inventory var2 = MenuGuiHelper.create(54, "§6§l✨ 琼华阁 ✨");
      MenuGuiHelper.applyBorder(var2, BORDER);
      var2.setItem(4, MenuGuiHelper.item("PAINTING", "§6§l琼华阁", List.of("§f点击上方分类查看可兑换物品", "§f每个物品标有兑换所需灵石")));
      var2.setItem(10, MenuGuiHelper.item("EMERALD", "§6✦ 基础材料", List.of("§7基础材料", "", "§a点击查看")));
      var2.setItem(11, MenuGuiHelper.item("WHEAT_SEEDS", "§2✦ 灵植种子", List.of("§7各类灵植种子", "", "§a点击查看")));
      var2.setItem(12, MenuGuiHelper.item("SKULL_BANNER_PATTERN", "§8✦ 儡肆", List.of("§7傀儡交易", "", "§a点击查看")));
      var2.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of("§7关闭菜单")));
      var1.openInventory(var2);
      this.openPlayers.put(var1, "main");
      this.playerSlotMaps.put(var1, Map.of());
      KomutechMenuRouter.bindInventory(var2, this);
   }

   private void openCategory(Player var1, String var2) {
      List<QiongHuaMenuScript.ShopEntry> var3 = SHOPS.get(var2);
      if (var3 != null) {
         String var4 = CATEGORY_NAMES.getOrDefault(var2, var2);
         String var5 = "§6§l✨ " + var4 + " ✨";
         Inventory var6 = MenuGuiHelper.create(54, var5);
         MenuGuiHelper.applyBorder(var6, BORDER);
         HashMap var7 = new HashMap();
         int var8 = 10;

         for (QiongHuaMenuScript.ShopEntry var10 : var3) {
            if (var8 > 43) {
               break;
            }

            SlimefunItem var11 = SlimefunItem.getById(var10.productId());
            if (var11 != null) {
               ItemStack var12 = var11.getItem().clone();
               ItemMeta var13 = var12.getItemMeta();
               ArrayList var14 = var13.getLore() == null ? new ArrayList() : new ArrayList(var13.getLore());
               var14.add("§f---§a§l点击购买§f---");
               var14.add("§a•需：" + var10.priceText());
               var13.setLore(var14);
               var12.setItemMeta(var13);
               var6.setItem(var8, var12);
               var7.put(var8, var10);
            }

            if ((++var8 - 9) % 9 == 0) {
               var8 += 2;
            }
         }

         var6.setItem(49, MenuGuiHelper.item("ARROW", "§a返回", List.of("§7返回主菜单")));
         var1.openInventory(var6);
         this.openPlayers.put(var1, var2);
         this.playerSlotMaps.put(var1, var7);
         KomutechMenuRouter.bindInventory(var6, this);
      }
   }

   private void purchase(Player var1, QiongHuaMenuScript.ShopEntry var2, int var3) {
      if (!this.hasEnough(var1, var2, var3)) {
         KomutechSupport.send(var1, "§c材料不足！");
      } else {
         SlimefunItem var4 = SlimefunItem.getById(var2.productId());
         if (var4 != null) {
            this.removeItems(var1, var2, var3);
            ItemStack var5 = var4.getItem().clone();
            var5.setAmount(var3);
            KomutechSupport.giveOrDrop(var1, var5);
            KomutechSupport.send(var1, "§a购买成功！");
         }
      }
   }

   private boolean hasEnough(Player var1, QiongHuaMenuScript.ShopEntry var2, int var3) {
      for (QiongHuaMenuScript.PricePart var5 : var2.prices()) {
         int var6 = var5.amount() * var3;
         int var7 = 0;

         for (ItemStack var11 : var1.getInventory().getContents()) {
            if (var11 != null && var11.getType() != Material.AIR) {
               SlimefunItem var12 = SlimefunItem.getByItem(var11);
               if (var12 != null && var5.id().equals(var12.getId())) {
                  var7 += var11.getAmount();
               }
            }
         }

         if (var7 < var6) {
            return false;
         }
      }

      return true;
   }

   private void removeItems(Player var1, QiongHuaMenuScript.ShopEntry var2, int var3) {
      for (QiongHuaMenuScript.PricePart var5 : var2.prices()) {
         int var6 = var5.amount() * var3;
         ItemStack[] var7 = var1.getInventory().getContents();

         for (int var8 = 0; var8 < var7.length && var6 > 0; var8++) {
            ItemStack var9 = var7[var8];
            if (var9 != null && var9.getType() != Material.AIR) {
               SlimefunItem var10 = SlimefunItem.getByItem(var9);
               if (var10 != null && var5.id().equals(var10.getId())) {
                  int var11 = Math.min(var9.getAmount(), var6);
                  var9.setAmount(var9.getAmount() - var11);
                  if (var9.getAmount() <= 0) {
                     var1.getInventory().setItem(var8, null);
                  }

                  var6 -= var11;
               }
            }
         }
      }
   }

   private static boolean isMainTitle(InventoryView var0) {
      String var1 = MenuGuiHelper.legacyTitle(var0);
      String var2 = ChatColor.stripColor(var1);
      return "§6§l✨ 琼华阁 ✨".equals(var1) || var2.contains("琼华阁");
   }

   private String resolvePageFromTitle(InventoryView var1) {
      if (isMainTitle(var1)) {
         return "main";
      } else {
         String var2 = MenuGuiHelper.legacyTitle(var1);
         String var3 = ChatColor.stripColor(var2);

         for (Entry var5 : CATEGORY_NAMES.entrySet()) {
            String var6 = ChatColor.stripColor((String)var5.getValue());
            if (var3.contains(var6) || var2.contains((CharSequence)var5.getKey()) || var3.contains((CharSequence)var5.getKey())) {
               return (String)var5.getKey();
            }
         }

         return "main";
      }
   }

   private static QiongHuaMenuScript.ShopEntry entry(String var0, String var1, int var2, String var3, int var4) {
      return new QiongHuaMenuScript.ShopEntry(var0, List.of(new QiongHuaMenuScript.PricePart(var1, var2), new QiongHuaMenuScript.PricePart(var3, var4)));
   }

   private static QiongHuaMenuScript.ShopEntry entry(String var0, String var1, int var2) {
      return new QiongHuaMenuScript.ShopEntry(var0, List.of(new QiongHuaMenuScript.PricePart(var1, var2)));
   }

   private static String resolveItemName(String var0) {
      SlimefunItem var1 = SlimefunItem.getById(var0);
      if (var1 == null) {
         return var0;
      } else {
         ItemStack var2 = var1.getItem();
         return var2.hasItemMeta() && var2.getItemMeta().hasDisplayName() ? var2.getItemMeta().getDisplayName() : var0;
      }
   }

   private record PricePart(String id, int amount) {
   }

   private record ShopEntry(String productId, List<QiongHuaMenuScript.PricePart> prices) {
      String priceText() {
         StringBuilder var1 = new StringBuilder();

         for (int var2 = 0; var2 < this.prices.size(); var2++) {
            if (var2 > 0) {
               var1.append(" + ");
            }

            QiongHuaMenuScript.PricePart var3 = this.prices.get(var2);
            var1.append(var3.amount()).append(" ").append(QiongHuaMenuScript.resolveItemName(var3.id())).append(" §7(").append(var3.id()).append(")");
         }

         return var1.toString();
      }
   }
}
