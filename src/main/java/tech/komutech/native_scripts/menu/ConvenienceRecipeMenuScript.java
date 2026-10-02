package tech.komutech.native_scripts.menu;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;

public final class ConvenienceRecipeMenuScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String MAIN_TITLE = "§6口木科技便捷配方";
   private static final String SUB_PREFIX = "§6便捷配方 - ";
   private static final int ITEMS_PER_PAGE = 28;
   private static final int[] BORDER = new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52};
   private static final int[] OUT_SLOTS = new int[]{
      10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
   };
   private static final NamespacedKey MACHINE_KEY = new NamespacedKey("komutech_l_bxpf", "machine");
   private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fff\\u3400-\\u4dbf]+");
   private static final List<ConvenienceRecipeMenuScript.Category> CATEGORIES = List.of(
      new ConvenienceRecipeMenuScript.Category(
         "jcwp",
         "§a基础物品",
         Material.CRAFTING_TABLE,
         List.of("KOMUTECH_L_JQ_下品一芥乾坤"),
         List.of(
            new ConvenienceRecipeMenuScript.ManualEntry("KOMUTECH_L_LZZZ_YGZZ", "KOMUTECH_L_JQ_下品一芥乾坤"),
            new ConvenienceRecipeMenuScript.ManualEntry("KOMUTECH_L_DJ_XPLJ", "KOMUTECH_L_JQ_下品一芥乾坤"),
            new ConvenienceRecipeMenuScript.ManualEntry("KOMUTECH_L_DJ_ZPLJ", "KOMUTECH_L_JQ_中品一芥乾坤")
         )
      ),
      new ConvenienceRecipeMenuScript.Category("fz", "§b法则", Material.ENCHANTED_BOOK, List.of("KOMUTECH_L_PF_法则合成演示"), List.of()),
      new ConvenienceRecipeMenuScript.Category("lz", "§d灵杖", Material.BLAZE_ROD, List.of("KOMUTECH_L_JQ_LNLQF"), List.of()),
      new ConvenienceRecipeMenuScript.Category("jz", "§5卷轴", Material.BOOK, List.of("KOMUTECH_L_JQ_JZZXT"), List.of()),
      new ConvenienceRecipeMenuScript.Category("fl", "§e符箓", Material.PAPER, List.of("KOMUTECH_L_JQ_FLHZT"), List.of()),
      new ConvenienceRecipeMenuScript.Category("zj", "§5終極", Material.NETHER_STAR, List.of("KOMUTECH_L_PF_终极合成演示"), List.of())
   );
   private final Map<String, List<ConvenienceRecipeMenuScript.RecipeEntry>> cache = new HashMap<>();
   private final Map<Player, ConvenienceRecipeMenuScript.MenuState> openPlayers = new HashMap<>();
   private boolean cacheReady;
   private Plugin plugin;

   private void ensureCache() {
      if (!this.cacheReady) {
         for (ConvenienceRecipeMenuScript.Category var2 : CATEGORIES) {
            ArrayList var3 = new ArrayList();
            if (var2.machines() != null) {
               for (String var5 : var2.machines()) {
                  for (String var7 : parseOutputs(var5)) {
                     var3.add(new ConvenienceRecipeMenuScript.RecipeEntry(var7, var5));
                  }
               }
            }

            if (var2.manual() != null) {
               for (ConvenienceRecipeMenuScript.ManualEntry var9 : var2.manual()) {
                  var3.add(new ConvenienceRecipeMenuScript.RecipeEntry(var9.id(), var9.machine()));
               }
            }

            this.cache.put(var2.id(), var3);
         }

         this.cacheReady = true;
      }
   }

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      String var2 = var1.getTitle();
      return "§6口木科技便捷配方".equals(var2) || var2.startsWith("§6便捷配方 - ");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onButtonGroupClick".equals(var1) && var2[0] instanceof Player var6) {
         this.openMain(var6);
         return true;
      } else {
         if ("onUse".equals(var1)) {
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
         String var8 = var1.getView().getTitle();
         if ("§6口木科技便捷配方".equals(var8) || var8.startsWith("§6便捷配方 - ")) {
            var1.setCancelled(true);
            ItemStack var4 = var1.getCurrentItem();
            if (var4 != null && !var4.getType().isAir()) {
               int var5 = var1.getSlot();
               if ("§6口木科技便捷配方".equals(var8)) {
                  if (var5 == 53) {
                     this.search(var2);
                  } else if (var5 == 49) {
                     var2.closeInventory();
                  } else {
                     for (int var9 = 0; var9 < CATEGORIES.size(); var9++) {
                        if (var5 == 10 + var9) {
                           this.openCategory(var2, CATEGORIES.get(var9).id(), 1);
                           return;
                        }
                     }
                  }
               } else {
                  ConvenienceRecipeMenuScript.MenuState var6 = this.openPlayers.get(var2);
                  if (var5 == 53) {
                     this.search(var2);
                  } else if (var5 == 49) {
                     this.openMain(var2);
                  } else if (var5 == 48 && var6 != null && var6.page() > 1) {
                     this.openCategory(var2, var6.catId(), var6.page() - 1);
                  } else if (var5 == 50 && var6 != null) {
                     int var10 = this.totalPages(var6.catId());
                     if (var6.page() < var10) {
                        this.openCategory(var2, var6.catId(), var6.page() + 1);
                     }
                  } else if (var4.hasItemMeta()) {
                     String var7 = (String)var4.getItemMeta().getPersistentDataContainer().get(MACHINE_KEY, PersistentDataType.STRING);
                     if (var7 != null) {
                        this.openGuide(var2, var7);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         this.openPlayers.remove(var2);
      }
   }

   private void openMain(Player var1) {
      this.ensureCache();
      Inventory var2 = MenuGuiHelper.create(54, "§6口木科技便捷配方");
      MenuGuiHelper.applyBorder(var2, BORDER);
      var2.setItem(4, MenuGuiHelper.item(Material.PAINTING, "§6口木科技便捷配方", List.of("§7点击分类查看产物")));

      for (int var3 = 0; var3 < CATEGORIES.size(); var3++) {
         ConvenienceRecipeMenuScript.Category var4 = CATEGORIES.get(var3);
         var2.setItem(10 + var3, MenuGuiHelper.item(var4.icon(), var4.name(), List.of("§7点击查看产物")));
      }

      var2.setItem(53, MenuGuiHelper.item(Material.COMPASS, "§e\ud83d\udd0d 搜索", List.of("§7点击后在聊天栏输入物品名")));
      var2.setItem(49, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
      var1.openInventory(var2);
      this.openPlayers.put(var1, new ConvenienceRecipeMenuScript.MenuState(null, 0));
   }

   private void openCategory(Player var1, String var2, int var3) {
      this.ensureCache();
      List var4 = this.cache.getOrDefault(var2, List.of());
      if (!var4.isEmpty()) {
         ConvenienceRecipeMenuScript.Category var5 = CATEGORIES.stream().filter(var1x -> var1x.id().equals(var2)).findFirst().orElse(null);
         if (var5 != null) {
            int var6 = Math.max(1, (int)Math.ceil(var4.size() / 28.0));
            var3 = Math.max(1, Math.min(var3, var6));
            int var7 = (var3 - 1) * 28;
            Inventory var8 = MenuGuiHelper.create(54, "§6便捷配方 - " + var5.name());
            MenuGuiHelper.applyBorder(var8, BORDER);
            var8.setItem(4, MenuGuiHelper.item(Material.PAPER, "§6" + var5.name() + " - 产物列表", List.of("§7第 " + var3 + "/" + var6 + " 页")));

            for (int var9 = 0; var9 < 28 && var7 + var9 < var4.size(); var9++) {
               ConvenienceRecipeMenuScript.RecipeEntry var10 = (ConvenienceRecipeMenuScript.RecipeEntry)var4.get(var7 + var9);
               SlimefunItem var11 = SlimefunItem.getById(var10.out());
               if (var11 != null) {
                  ItemStack var12 = var11.getItem().clone();
                  ItemMeta var13 = var12.getItemMeta();
                  ArrayList var14 = var13.hasLore() ? new ArrayList(var13.getLore()) : new ArrayList();
                  SlimefunItem var15 = SlimefunItem.getById(var10.machine());
                  String var16 = var15 != null && var15.getItem().getItemMeta().hasDisplayName()
                     ? var15.getItem().getItemMeta().getDisplayName()
                     : var10.machine();
                  var14.add("§7产出机器：" + var16);
                  var13.setLore(var14);
                  var13.getPersistentDataContainer().set(MACHINE_KEY, PersistentDataType.STRING, var10.machine());
                  var12.setItemMeta(var13);
                  var8.setItem(OUT_SLOTS[var9], var12);
               }
            }

            if (var3 > 1) {
               var8.setItem(48, MenuGuiHelper.item(Material.ARROW, "§a上一页", List.of()));
            }

            var8.setItem(49, MenuGuiHelper.item(Material.ARROW, "§a返回", List.of("§7返回主菜单")));
            if (var3 < var6) {
               var8.setItem(50, MenuGuiHelper.item(Material.ARROW, "§a下一页", List.of()));
            }

            var8.setItem(53, MenuGuiHelper.item(Material.COMPASS, "§e\ud83d\udd0d 搜索", List.of()));
            var1.openInventory(var8);
            this.openPlayers.put(var1, new ConvenienceRecipeMenuScript.MenuState(var2, var3));
         }
      }
   }

   private void search(Player var1) {
      this.ensureCache();
      KomutechSupport.send(var1, "§e请在聊天栏输入要搜索的物品名称（输入 cancel 取消）:");
      KomutechChatInput.waitFor(var1, var2 -> {
         if (var2 != null && !"cancel".equalsIgnoreCase(var2)) {
            String var3 = extractChinese(var2);
            if (var3.isBlank()) {
               KomutechSupport.send(var1, "§c请输入有效的中文关键词。");
            } else {
               for (ConvenienceRecipeMenuScript.Category var5 : CATEGORIES) {
                  for (ConvenienceRecipeMenuScript.RecipeEntry var7 : this.cache.getOrDefault(var5.id(), List.of())) {
                     SlimefunItem var8 = SlimefunItem.getById(var7.out());
                     if (var8 != null) {
                        String var9 = var8.getItem().getItemMeta().hasDisplayName() ? var8.getItem().getItemMeta().getDisplayName() : var7.out();
                        if (extractChinese(var9).contains(var3) || var7.out().toLowerCase().contains(var3.toLowerCase())) {
                           KomutechSupport.send(var1, "§a找到匹配项：" + var9 + "，位于分类：" + var5.name());
                           this.openCategory(var1, var5.id(), 1);
                           return;
                        }
                     }
                  }
               }

               KomutechSupport.send(var1, "§c未找到包含 \"" + var3 + "\" 的物品。");
            }
         } else {
            KomutechSupport.send(var1, "§c已取消搜索。");
         }
      });
   }

   private void openGuide(Player var1, String var2) {
      SlimefunItem var3 = SlimefunItem.getById(var2);
      if (var3 != null) {
         PlayerProfile.find(var1).ifPresent(var1x -> SlimefunGuide.displayItem(var1x, var3, true));
      }
   }

   private int totalPages(String var1) {
      int var2 = this.cache.getOrDefault(var1, List.of()).size();
      return Math.max(1, (int)Math.ceil(var2 / 28.0));
   }

   private static Set<String> parseOutputs(String var0) {
      HashSet var1 = new HashSet();
      SlimefunItem var2 = SlimefunItem.getById(var0);
      if (var2 == null) {
         return var1;
      } else {
         try {
            if (var2.getClass().getMethod("getMachineRecipes").invoke(var2) instanceof Iterable var4) {
               for (Object var6 : var4) {
                  if (var6.getClass().getMethod("getOutput").invoke(var6) instanceof ItemStack[] var8) {
                     for (ItemStack var12 : var8) {
                        SlimefunItem var13 = SlimefunItem.getByItem(var12);
                        if (var13 != null) {
                           var1.add(var13.getId());
                        }
                     }
                  }
               }
            }
         } catch (ReflectiveOperationException var14) {
         }

         return var1;
      }
   }

   private static String extractChinese(String var0) {
      if (var0 == null) {
         return "";
      } else {
         Matcher var1 = CHINESE.matcher(var0);
         StringBuilder var2 = new StringBuilder();

         while (var1.find()) {
            var2.append(var1.group());
         }

         return var2.toString();
      }
   }

   private record Category(String id, String name, Material icon, List<String> machines, List<ConvenienceRecipeMenuScript.ManualEntry> manual) {
   }

   private record ManualEntry(String id, String machine) {
   }

   private record MenuState(String catId, int page) {
   }

   private record RecipeEntry(String out, String machine) {
   }
}
