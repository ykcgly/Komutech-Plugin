package tech.komutech.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.JegBridge;
import tech.komutech.objects.customs.LinkedOutput;
import tech.komutech.objects.machine.CustomLinkedMachineRecipe;
import tech.komutech.util.colors.CMIChatColor;

/**
 * 大型配方展示菜单（参照终焉厨锅 / 尘世百味 {@code BigRecipeMenu} 的三段结构）。
 *
 * <p><b>为什么需要它</b>：本附属的联动槽配方大量超出 54 格界面 ——
 * 实测 275 个配方绑定槽数 &gt; 9，最多的 {@code Komutech_L_DFKJQ_ZLT} 有 242 槽。
 * Slimefun/JEG 原生的配方页只放得下 9 个输入格，多余材料直接不显示，
 * 玩家看到的配方是残缺的。本类按「材料分页」补齐这一段。
 *
 * <p><b>三段结构</b>：
 * <ol>
 *   <li><b>材料页</b>：绑定槽直映（key 即机器 GUI 槽号），超出 54 的按槽号顺序分页，
 *       53 为翻页；材料点击走 {@link JegBridge} 跳对应物品的指南页；</li>
 *   <li>产物固定在 24，机器图标在 8，返回在 35；</li>
 *   <li>若该配方有多个产物，右下角提供产物列表入口（与终焉厨锅同款：配方补全式浏览）。</li>
 * </ol>
 *
 * <p><b>与原生配方页的关系</b>：本菜单<b>只处理超出 9 槽的联动配方</b>。
 * 常规配方（≤9 槽）继续走 Slimefun/JEG 原生页面 —— 那边有搜索、收藏、
 * 配方树等完整功能，自绘一套反而更差。
 */
public final class BigRecipeMenu {

   /** 固定槽位（与尘百 BigRecipeMenu 对齐）。 */
   private static final int SLOT_ICON = 8;      // 机器图标
   private static final int SLOT_OUTPUT = 24;   // 主产物
   private static final int SLOT_BACK = 35;     // 返回
   private static final int SLOT_PAGE = 53;     // 翻页 / 产物列表

   /** 每页可用槽数：0..53 扣除 4 个固定槽。 */
   private static final int PER_PAGE = 54 - 4;

   private BigRecipeMenu() {
   }

   /**
    * 该配方是否为「大型配方」（绑定槽超过 9，即原生配方页放不下）。
    *
    * <p>判定用绑定槽数而不是输入数组长度：{@code CustomLinkedMachineRecipe} 的
    * {@code getInput()} 是 {@code linkedInput.values()}，长度可能远大于 9，
    * 但真正决定展示效果的是槽位号是否超出 54。
    */
   public static boolean isLargeRecipe(CustomLinkedMachineRecipe recipe) {
      return countLarge(recipe) > 9;
   }

   /** 统计该配方中槽位号超出 9 的材料个数（槽号 &ge; 9 一律放不进原生 3x3）。 */
   private static int countLarge(CustomLinkedMachineRecipe recipe) {
      int n = 0;
      for (Map.Entry<Integer, ItemStack> e : recipe.getLinkedInput().entrySet()) {
         if (e.getValue() != null && !e.getValue().getType().isAir() && e.getKey() != null && e.getKey() >= 9) {
            n++;
         }
      }

      return n;
   }

   /**
    * 打开大型配方的材料分页视图。
    *
    * @param recipe 目标联动配方
    * @param player 玩家
    */
   public static void open(Player player, CustomLinkedMachineRecipe recipe) {
      // 按槽号排序：与机器 GUI 中的摆放顺序一致，玩家能对上位置
      Map<Integer, ItemStack> sorted = new TreeMap<>(recipe.getLinkedInput());
      List<Map.Entry<Integer, ItemStack>> entries = new ArrayList<>();
      for (Map.Entry<Integer, ItemStack> e : sorted.entrySet()) {
         if (e.getValue() != null && !e.getValue().getType().isAir()) {
            entries.add(e);
         }
      }

      int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
      openPage(player, recipe, entries, 0, pages);
   }

   private static void openPage(Player player, CustomLinkedMachineRecipe recipe,
                                List<Map.Entry<Integer, ItemStack>> entries, int page, int pages) {
      ChestMenu menu = new ChestMenu(title(recipe, page + 1, pages));
      menu.setEmptySlotsClickable(false);
      menu.addMenuOpeningHandler(p -> SoundEffect.GUIDE_BUTTON_CLICK_SOUND.playFor(p));

      // 背景
      for (int i = 0; i < 54; i++) {
         menu.addItem(i, ChestMenuUtils.getBackground(), (p, s, it, a) -> false);
      }

      // 固定槽预留
      boolean[] reserved = new boolean[54];
      reserved[SLOT_ICON] = true;
      reserved[SLOT_OUTPUT] = true;
      reserved[SLOT_BACK] = true;
      reserved[SLOT_PAGE] = true;

      // 材料：绑定槽直映（槽号在 0..53 且未被占用），越界或冲突则顺序补位
      boolean[] used = new boolean[54];
      int start = page * PER_PAGE;
      int end = Math.min(entries.size(), start + PER_PAGE);
      for (int i = start; i < end; i++) {
         Map.Entry<Integer, ItemStack> entry = entries.get(i);
         int guiSlot = entry.getKey();
         if (guiSlot < 0 || guiSlot >= 54 || reserved[guiSlot] || used[guiSlot]) {
            guiSlot = -1;
            for (int s = 52; s >= 0; s--) {
               if (!reserved[s] && !used[s]) {
                  guiSlot = s;
                  break;
               }
            }
         }

         if (guiSlot < 0) {
            continue;
         }

         used[guiSlot] = true;
         ItemStack display = describeInput(entry.getValue(), entry.getKey());
         menu.addItem(guiSlot, display, (p, s, it, a) -> {
            navigateIngredient(p, display, () -> openPage(p, recipe, entries, page, pages));
            return false;
         });
      }

      // 产物：主产物放 24
      ItemStack main = mainOutput(recipe);
      menu.addItem(SLOT_OUTPUT, describeOutput(main, recipe), (p, s, it, a) -> false);

      // 机器图标
      menu.addItem(SLOT_ICON, machineIcon(recipe), (p, s, it, a) -> false);

      // 返回：左键回 JEG/原生指南，Shift 无差别（统一回指南）
      menu.addItem(SLOT_BACK, backButton(), (p, s, it, a) -> {
         PlayerProfile.find(p).ifPresent(profile -> {
            // 回退路径：JEG 未接管时直接走 Slimefun 静态入口（它内部查注册表）
            if (!JegBridge.back(profile, p, true)) {
               SlimefunGuide.openMainMenu(profile, SlimefunGuideMode.SURVIVAL_MODE, 1);
            }
         });
         return false;
      });

      // 翻页
      if (pages > 1) {
         menu.addItem(SLOT_PAGE, pageButton(page, pages), (p, s, it, a) -> {
            int next = a.isRightClicked() ? page - 1 : page + 1;
            next = Math.floorMod(next, pages);
            openPage(p, recipe, entries, next, pages);
            return false;
         });
      } else {
         // 单页时右下角放返回（与终焉厨锅一致：多配方才有产物列表可浏览）
         menu.addItem(SLOT_PAGE, backButton(), (p, s, it, a) -> {
            PlayerProfile.find(p).ifPresent(profile ->
               SlimefunGuide.openMainMenu(profile, SlimefunGuideMode.SURVIVAL_MODE, 1));
            return false;
         });
      }

      menu.open(player);
   }

   /**
    * 材料点击导航。
    *
    * <p>与终焉厨锅一致：附属材料跳 JEG/原生指南的该物品页；
    * 原版材料（无 Slimefun 归属）无处可跳，菜单保持不动。
    */
   private static void navigateIngredient(Player player, ItemStack display, Runnable reopenSelf) {
      SlimefunItem sf = SlimefunItem.getByItem(display);
      if (sf == null) {
         return;
      }

      // JegBridge 内部走 SlimefunGuide.displayItem 静态入口，装了 JEG 自动进 JEG
      if (!JegBridge.displayItem(player, display)) {
         PlayerProfile.find(player).ifPresent(profile ->
            SlimefunGuide.displayItem(profile, sf, true));
      }
   }

   /** 主产物：取第一个非空气产物。 */
   private static ItemStack mainOutput(CustomLinkedMachineRecipe recipe) {
      ItemStack[] out = recipe.getOutput();
      if (out != null) {
         for (ItemStack o : out) {
            if (o != null && !o.getType().isAir()) {
               return o;
            }
         }
      }

      LinkedOutput linked = recipe.getLinkedOutput();
      if (linked != null && linked.freeOutput() != null) {
         for (ItemStack o : linked.freeOutput()) {
            if (o != null && !o.getType().isAir()) {
               return o;
            }
         }
      }

      return new ItemStack(Material.BARRIER);
   }

   /** 该配方的主产物对应的 Slimefun 物品（用于取图标）。 */
   private static ItemStack machineIcon(CustomLinkedMachineRecipe recipe) {
      ItemStack main = mainOutput(recipe);
      SlimefunItem sf = SlimefunItem.getByItem(main);
      return sf != null ? sf.getItem() : new ItemStack(Material.CRAFTING_TABLE);
   }

   private static String title(CustomLinkedMachineRecipe recipe, int page, int pages) {
      return "&6大型配方 &7[" + page + "/" + pages + "]";
   }

   private static ItemStack describeInput(ItemStack in, int slot) {
      ItemStack clone = in.clone();
      ItemMeta meta = clone.getItemMeta();
      if (meta != null) {
         List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
         lore.add("");
         lore.add(CMIChatColor.translate("&a槽位 &f" + slot));
         if (clone.getAmount() > 1) {
            lore.add(CMIChatColor.translate("&c数量: &f" + clone.getAmount()));
         }

         if (SlimefunItem.getByItem(clone) != null) {
            lore.add(CMIChatColor.translate("&8点击查看获取方式"));
         }

         meta.setLore(lore);
         clone.setItemMeta(meta);
      }

      return clone;
   }

   private static ItemStack describeOutput(ItemStack out, CustomLinkedMachineRecipe recipe) {
      ItemStack clone = out.clone();
      ItemMeta meta = clone.getItemMeta();
      if (meta != null) {
         List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
         lore.add("");
         lore.add(CMIChatColor.translate("&a产物"));
         if (clone.getAmount() > 1) {
            lore.add(CMIChatColor.translate("&c数量: &f" + clone.getAmount()));
         }

         meta.setLore(lore);
         clone.setItemMeta(meta);
      }

      return clone;
   }

   private static ItemStack pageButton(int page, int pages) {
      ItemStack it = new ItemStack(Material.ARROW);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CMIChatColor.translate("&e材料 " + (page + 1) + "/" + pages));
         List<String> lore = new ArrayList<>();
         lore.add(CMIChatColor.translate("&7左键：下一页"));
         lore.add(CMIChatColor.translate("&7右键：上一页"));
         meta.setLore(lore);
         it.setItemMeta(meta);
      }

      return it;
   }

   private static ItemStack backButton() {
      ItemStack it = new ItemStack(Material.OAK_DOOR);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CMIChatColor.translate("&e返回指南"));
         List<String> lore = new ArrayList<>();
         lore.add(CMIChatColor.translate("&7返回 Slimefun / JEG 指南"));
         meta.setLore(lore);
         it.setItemMeta(meta);
      }

      return it;
   }

   /** 供「按产物浏览」用：取该机器所有产物（去重）。 */
   public static Map<String, ItemStack> productsOf(List<CustomLinkedMachineRecipe> recipes) {
      Map<String, ItemStack> products = new LinkedHashMap<>();
      for (CustomLinkedMachineRecipe r : recipes) {
         ItemStack main = mainOutput(r);
         if (main.getType().isAir() || main.getType() == Material.BARRIER) {
            continue;
         }

         SlimefunItem sf = SlimefunItem.getByItem(main);
         String key = sf != null ? "sf:" + sf.getId()
            : "mc:" + main.getType() + ":" + (main.hasItemMeta() && main.getItemMeta().hasDisplayName()
               ? main.getItemMeta().getDisplayName() : "");
         products.putIfAbsent(key, main);
      }

      return products;
   }
}
