package tech.komutech.native_scripts.menu;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;

public final class GuideMenuScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String MAIN_TITLE = "§b§l✨ 口木科技教程 ✨";
   private static final String TITLE_PREFIX = "§b§l✨ ";
   private static final int[] BORDER = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
   private static final int[] REWARD_SLOTS = new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
   private static final String[][] CATEGORIES = new String[][]{
      {"rumen", "§a✦ 入门篇", "AMETHYST_CLUSTER", "10"},
      {"shenwu", "§b✦ 深悟篇", "ENDER_PEARL", "11"},
      {"dacheng", "§6✦ 大成篇", "ENDER_EYE", "12"},
      {"lingzhi", "§2✦ 灵植篇", "BAMBOO", "13"},
      {"lingzhang", "§d✦ 灵杖篇", "BLAZE_ROD", "14"},
      {"juanzhou", "§5✦ 卷轴篇", "BOOK", "15"}
   };
   private static final Object[][] REWARDS = new Object[][]{
      {"KOMUTECH_L_LZ_YG", 40}, {"KOMUTECH_L_DJ_XPLS", 40}, {"KOMUTECH_L_DJ_擇靈珠", 1}, {"KOMUTECH_L_GJ_印物笺", 5}
   };
   private static final String MYSTERY_NAME = "§e✨ 神秘奖励 ✨";
   private static final double MYSTERY_SPAWN_CHANCE = 50.0;
   private static final Map<String, List<GuideMenuScript.CategoryTip>> CATEGORY_TIPS = buildCategoryTips();
   private final Map<Player, String> openPlayers = new HashMap<>();
   private Plugin plugin;

   private static Map<String, List<GuideMenuScript.CategoryTip>> buildCategoryTips() {
      HashMap var0 = new HashMap();
      var0.put(
         "rumen",
         List.of(
            new GuideMenuScript.CategoryTip("§6灵石获取", "AMETHYST_SHARD", List.of("§7• 灵脉宝窟/矿井开采灵石原矿获得下品灵石", "§7• 灵矿提取台处理原矿，灵能提炼器转换", "§7• 上品/极品需功德券辅助合成")),
            new GuideMenuScript.CategoryTip("§6矿物获取", "IRON_INGOT", List.of("§7• 灵石原矿开采", "§7• 矿石原胚/杂矿概率获得", "§7• 玄铁、寒铁等由原胚在灵能提取器随机产出")),
            new GuideMenuScript.CategoryTip("§6灵脉宝窟", "FURNACE", List.of("§7• 右键打开，点击石头开采", "§7• 下界合金镐可一键挖掘", "§7• 产出：灵石原矿、下品灵石、杂矿")),
            new GuideMenuScript.CategoryTip("§6灵脉晶辉宝窟", "DIAMOND", List.of("§7• 同上，产出各类宝石璞胚"))
         )
      );
      var0.put(
         "shenwu",
         List.of(
            new GuideMenuScript.CategoryTip("§6法则概述", "ENCHANTED_BOOK", List.of("§7• 一丝、些许、完整三个品级", "§7• 涵盖金木水火土冰风雷机关炼金声音空间杀戮")),
            new GuideMenuScript.CategoryTip("§6一丝法则", "GOLD_NUGGET", List.of("§7• 悟道石+材料+南方法则承载器", "§7• 悟道成功在承载器生成")),
            new GuideMenuScript.CategoryTip("§6些许法则", "GOLD_INGOT", List.of("§7• 明悟石按顺序放入1-36个一丝法则")),
            new GuideMenuScript.CategoryTip("§6完整法则", "GOLD_BLOCK", List.of("§7• 破妄石按顺序放入1-36个些许法则")),
            new GuideMenuScript.CategoryTip("§6高级法则", "NETHER_STAR", List.of("§7• 五行灵曦：15个各完整五行法则合成", "§7• 五行祖炁：1-36个灵曦合成"))
         )
      );
      var0.put(
         "dacheng",
         List.of(
            new GuideMenuScript.CategoryTip("§6身外身简介", "PLAYER_HEAD", List.of("§7可绑定分身，自动化关键")),
            new GuideMenuScript.CategoryTip("§6无垢坯", "CLAY_BALL", List.of("§7• 上品一芥乾坤合成清灵壤+元灵髓", "§7• 炼枢造生仪合成无垢坯")),
            new GuideMenuScript.CategoryTip("§6蕴灵身", "BLAZE_POWDER", List.of("§7• 手持无垢坯右键使用", "§7• 消耗99%生命绑定自身")),
            new GuideMenuScript.CategoryTip("§6进阶", "BEACON", List.of("§7• 蕴灵身可合成循工偶、百巧工等"))
         )
      );
      var0.put(
         "lingzhi",
         List.of(
            new GuideMenuScript.CategoryTip("§6种子获取", "WHEAT_SEEDS", List.of("§7• 桃、松、玉干、棉花、麻种子在下品一芥乾坤合成")),
            new GuideMenuScript.CategoryTip("§6种植环境", "GRASS_BLOCK", List.of("§7• 普通作物需耕地", "§7• 树木需草方块，周围留空", "§7• 玉干草方块即可")),
            new GuideMenuScript.CategoryTip("§6生长收获", "BAMBOO", List.of("§7• 成熟后破坏种子格自动掉落", "§7• 稀释灵液可增加玉干产量")),
            new GuideMenuScript.CategoryTip("§6产物用途", "OAK_LOG", List.of("§7• 玉干→褚纸、玄铁工具", "§7• 桃/松木→木材", "§7• 棉花→一袋棉花→布", "§7• 麻→布"))
         )
      );
      var0.put(
         "lingzhang",
         List.of(
            new GuideMenuScript.CategoryTip("§6灵杖简介", "BLAZE_ROD", List.of("§7• 黄阶1、玄阶1.05、地阶1.10、天阶1.15、仙阶1.20倍率", "§7• 品阶差距已大幅压缩，主要靠基础属性拉开")),
            new GuideMenuScript.CategoryTip("§6云篆匣配合", "CHEST", List.of("§7• 卷轴需先放入工具「云篆匣」收纳", "§7• 主手持灵杖，§e双击右键§7切换云篆匣中的卷轴", "§7• 灵杖 Lore 会显示当前绑定卷轴")),
            new GuideMenuScript.CategoryTip("§6攻击与释放", "BLAZE_POWDER", List.of("§7• §e单击右键§7：灵杖普通攻击", "§7• §eShift+右键§7：释放当前绑定的卷轴技能", "§7• 无需副手再拿卷轴")),
            new GuideMenuScript.CategoryTip("§6灵力补充", "EMERALD", List.of("§7• 手持灵石右键补充", "§7• Shift+右键自动补满")),
            new GuideMenuScript.CategoryTip("§6功德值", "EXPERIENCE_BOTTLE", List.of("§7• 伤敌增功德，伤友增缺德", "§7• 不再增幅灵杖/卷轴伤害", "§7• 仅可减免突破时雷劫伤害，并会被消耗")),
            new GuideMenuScript.CategoryTip("§6灵杖获取", "CRAFTING_TABLE", List.of("§7• 烧火棍：工匠台", "§7• 黄/玄/地/天阶：灵能炼器坊", "§7• 仙阶：归墟（暂无合成配方）"))
         )
      );
      var0.put(
         "juanzhou",
         List.of(
            new GuideMenuScript.CategoryTip("§6卷轴简介", "BOOK", List.of("§7• 分黄玄地天仙五阶", "§7• 倍率 1 / 1.05 / 1.10 / 1.15 / 1.20", "§7• 各阶卷轴拥有独立技能与熟练度")),
            new GuideMenuScript.CategoryTip("§6存入云篆匣", "CHEST", List.of("§7• 卷轴需放入工具「云篆匣」中收纳", "§7• 云篆匣可存放你已获得的全部卷轴", "§7• 未收纳的卷轴无法被灵杖切换")),
            new GuideMenuScript.CategoryTip("§6切换与释放", "BLAZE_ROD", List.of("§7• 主手持灵杖，§e双击右键§7切换云篆匣中的卷轴", "§7• §eShift+右键§7释放当前绑定卷轴", "§7• 释放消耗灵力并进入冷却")),
            new GuideMenuScript.CategoryTip("§6熟练度", "EXPERIENCE_BOTTLE", List.of("§7• 每100点降低1.5%冷却，上限40%", "§7• 使用卷轴技能可提升熟练度")),
            new GuideMenuScript.CategoryTip("§6功德渡劫", "GOLD_NUGGET", List.of("§7• 功德不再增幅卷轴伤害", "§7• 仅可减免突破时雷劫伤害", "§7• 减免时会消耗对应功德")),
            new GuideMenuScript.CategoryTip(
               "§6卷轴获取",
               "WRITABLE_BOOK",
               List.of("§7• 黄阶：勾豆灰 / 碎玉闪", "§7• 玄阶：九霄环佩鸣 / 寒霜锁", "§7• 地阶：冰火两重天 / 御龙护身决", "§7• 天阶：游龙惊鸿诀 / 星陨劫", "§7• 仙阶：五行必杀（均卷轴撰写台合成）")
            )
         )
      );
      return var0;
   }

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
            : isGuideTitle(MenuGuiHelper.legacyTitle(var1)) || isGuideTitle(var1.getTitle());
      }
   }

   private static boolean isGuideTitle(String var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else if ("§b§l✨ 口木科技教程 ✨".equals(var0)) {
         return true;
      } else {
         String var1 = ChatColor.stripColor(var0);
         if (var1.contains("口木科技教程")) {
            return true;
         } else {
            return !var0.startsWith("§b§l✨ ") && !var1.startsWith("✨ ")
               ? false
               : var1.contains("入门篇") || var1.contains("深悟篇") || var1.contains("大成篇") || var1.contains("灵植篇") || var1.contains("灵杖篇") || var1.contains("卷轴篇");
         }
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2 && this.handles(var1.getView())) {
         var1.setCancelled(true);
         var1.setResult(Result.DENY);
         int var9 = var1.getRawSlot();
         if (var9 >= 0 && var9 < var1.getView().getTopInventory().getSize()) {
            String var4 = this.openPlayers.getOrDefault(var2, resolvePage(var1.getView()));
            if (!"main".equals(var4) && !isMainTitle(var1.getView())) {
               if (var9 == 49) {
                  this.openMain(var2);
               }
            } else {
               for (String[] var8 : CATEGORIES) {
                  if (String.valueOf(var9).equals(var8[3])) {
                     this.openCategory(var2, var8[0], var8[1]);
                     return;
                  }
               }

               ItemStack var10 = var1.getCurrentItem();
               if (var10 != null && var10.hasItemMeta() && var10.getItemMeta().hasDisplayName() && "§e✨ 神秘奖励 ✨".equals(var10.getItemMeta().getDisplayName())) {
                  this.giveRandomReward(var2);
                  var1.getView().getTopInventory().setItem(var9, null);
               } else {
                  if (var9 == 49) {
                     var2.closeInventory();
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
         var1.setResult(Result.DENY);
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
               Object var3 = var2[0];
               Player var7 = (Player)var3.getClass().getMethod("getPlayer").invoke(var3);
               this.openMain(var7);
            } catch (ReflectiveOperationException var5) {
            }
         }

         return null;
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (this.plugin == null) {
            this.openPlayers.remove(var2);
         } else {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
               if (!var2.isOnline()) {
                  this.openPlayers.remove(var2);
               } else {
                  InventoryView var2x = var2.getOpenInventory();
                  if (var2x == null || !this.handles(var2x)) {
                     this.openPlayers.remove(var2);
                  }
               }
            });
         }
      }
   }

   private void openMain(Player var1) {
      Inventory var2 = MenuGuiHelper.create(54, "§b§l✨ 口木科技教程 ✨");
      MenuGuiHelper.applyBorder(var2, BORDER);
      var2.setItem(4, MenuGuiHelper.item("PAINTING", "§a§l口木科技教程说明", List.of("§f点击上方分类查看教程", "§f点击下方随机奖励物品", "§f即可获得惊喜！")));

      for (String[] var6 : CATEGORIES) {
         var2.setItem(Integer.parseInt(var6[3]), MenuGuiHelper.item(var6[2], var6[1], List.of("§7点击查看")));
      }

      if (ThreadLocalRandom.current().nextDouble() * 100.0 < 50.0) {
         int var7 = REWARD_SLOTS[ThreadLocalRandom.current().nextInt(REWARD_SLOTS.length)];
         var2.setItem(var7, MenuGuiHelper.item("ENDER_CHEST", "§e✨ 神秘奖励 ✨", List.of("§7点击随机获得奖励", "§8展示物品不可取出")));
      }

      var2.setItem(49, MenuGuiHelper.item("BARRIER", "§c关闭", List.of("§7关闭菜单")));
      this.openPlayers.put(var1, "main");
      KomutechMenuRouter.bindInventory(var2, this);
      var1.openInventory(var2);
   }

   private void openCategory(Player var1, String var2, String var3) {
      Inventory var4 = MenuGuiHelper.create(54, "§b§l✨ " + var3.replaceAll("§.", "") + " ✨");
      MenuGuiHelper.applyBorder(var4, BORDER);
      var4.setItem(49, MenuGuiHelper.item("ARROW", "§a返回主菜单", List.of()));
      List<GuideMenuScript.CategoryTip> var5 = CATEGORY_TIPS.getOrDefault(var2, List.of());
      if (var5.isEmpty()) {
         var4.setItem(13, MenuGuiHelper.item("BOOK", var3, List.of("§7教程内容请参考游戏内说明")));
      } else {
         var4.setItem(4, MenuGuiHelper.item("PAPER", "§6" + var3 + "说明", List.of("§7点击条目查看详情", "§e共" + var5.size() + "个知识点")));
         int var6 = 10;

         for (GuideMenuScript.CategoryTip var8 : var5) {
            if (var6 > 43) {
               break;
            }

            var4.setItem(var6, MenuGuiHelper.item(var8.icon(), var8.name(), var8.lore()));
            if ((++var6 - 9) % 9 == 0) {
               var6 += 2;
            }
         }
      }

      this.openPlayers.put(var1, var2);
      KomutechMenuRouter.bindInventory(var4, this);
      var1.openInventory(var4);
   }

   private void giveRandomReward(Player var1) {
      double var2 = ThreadLocalRandom.current().nextDouble() * 100.0;
      double var4 = 0.0;

      for (Object[] var9 : REWARDS) {
         var4 += ((Number)var9[1]).doubleValue();
         if (!(var2 >= var4)) {
            String var10 = String.valueOf(var9[0]);
            SlimefunItem var11 = SlimefunItem.getById(var10);
            if (var11 != null) {
               ItemStack var12 = var11.getItem().clone();
               KomutechSupport.giveOrDrop(var1, var12);
               String var13 = var12.hasItemMeta() && var12.getItemMeta().hasDisplayName() ? var12.getItemMeta().getDisplayName() : var10;
               KomutechSupport.send(var1, "§a获得奖励: " + var13);
            } else {
               KomutechSupport.send(var1, "§c奖励物品无效，请联系管理");
            }

            return;
         }
      }

      KomutechSupport.send(var1, "§f\ud83c\udf89倒霉\ud83e\udd5a，你成功避开了奖励。");
   }

   private static boolean isMainTitle(InventoryView var0) {
      return "§b§l✨ 口木科技教程 ✨".equals(MenuGuiHelper.legacyTitle(var0))
         || "§b§l✨ 口木科技教程 ✨".equals(var0.getTitle())
         || ChatColor.stripColor(var0.getTitle()).contains("口木科技教程");
   }

   private static String resolvePage(InventoryView var0) {
      if (isMainTitle(var0)) {
         return "main";
      } else {
         String var1 = ChatColor.stripColor(MenuGuiHelper.legacyTitle(var0));
         if (var1.contains("入门")) {
            return "rumen";
         } else if (var1.contains("深悟")) {
            return "shenwu";
         } else if (var1.contains("大成")) {
            return "dacheng";
         } else if (var1.contains("灵植")) {
            return "lingzhi";
         } else if (var1.contains("灵杖")) {
            return "lingzhang";
         } else {
            return var1.contains("卷轴") ? "juanzhou" : "main";
         }
      }
   }

   private record CategoryTip(String name, String icon, List<String> lore) {
   }
}
