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
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.jeg.JegHook;
import tech.komutech.native_scripts.support.JegBridge;
import tech.komutech.objects.customs.machine.CustomLinkedRecipeMachine;
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
 * <p><b>两个入口</b>：
 * <ul>
 *   <li><b>机器级</b>（{@link #openMachine}，JEG 指南点击大型配方机器时进入，
 *       对齐尘世百味的三段结构）：
 *       <ol>
 *         <li>合成配方页：机器自身的 3x3 合成配方，右下角「配方展示」进产物列表；</li>
 *         <li>产物列表页：该机器全部工作配方按主产物去重，点击进配方页；</li>
 *         <li>配方页：单配方材料分页，右下角在「该产物的配方 × 材料页」内连续翻页。</li>
 *       </ol></li>
 *   <li><b>配方级</b>（{@link #open}，机器 GUI 的「多物品输入」→ 单配方页改道时进入）：
 *       直接展示该配方的材料分页，行为与改动前一致。</li>
 * </ul>
 *
 * <p>所有粘液材料点击走 {@link JegBridge} 跳对应物品的 JEG/原生指南页。</p>
 */
public final class BigRecipeMenu {

   /** 固定槽位（与尘百 BigRecipeMenu 对齐）。 */
   private static final int SLOT_ICON = 8;      // 机器图标
   private static final int SLOT_OUTPUT = 24;   // 主产物
   private static final int SLOT_BACK = 35;     // 返回
   private static final int SLOT_PAGE = 53;     // 翻页 / 产物列表

   /** 合成配方页的 3x3 槽位（对齐尘百）。 */
   private static final int[] CRAFT_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};

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

   /**
    * 该机器是否需要机器级完整视图：任一工作配方的绑定槽超过 9。
    * 满足时 JEG 指南点击该机器改开 {@link #openMachine}；普通机器保留 JEG 默认展示。
    */
   public static boolean isLargeRecipeMachine(CustomLinkedRecipeMachine machine) {
      for (MachineRecipe r : machine.getMachineRecipes()) {
         if (r instanceof CustomLinkedMachineRecipe linked && countLarge(linked) > 9) {
            return true;
         }
      }

      return false;
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

   // ------------------------------------------------------------------
   // 机器级入口（JEG 指南路径）
   // ------------------------------------------------------------------

   /**
    * 打开机器的完整配方视图。
    *
    * @param index      0 = 合成配方页；i = 按全局顺序的第 i 个工作配方页（不经产物列表）
    * @param backOpener 「返回」动作；null = 回 JEG/原生指南
    */
   public static void openMachine(Player p, CustomLinkedRecipeMachine machine, int index, Runnable backOpener) {
      List<CustomLinkedMachineRecipe> work = linkedRecipes(machine);
      int total = work.size() + 1; // 0 = 合成页
      int idx = Math.floorMod(index, total);

      if (idx == 0) {
         openCraftPage(p, machine, backOpener);
      } else {
         openRecipePage(p, machine, work, idx - 1, 0, false, backOpener);
      }
   }

   /** 合成配方页：机器自身的 3x3 合成配方，53 = 配方展示（产物列表）。 */
   private static void openCraftPage(Player p, CustomLinkedRecipeMachine machine, Runnable backOpener) {
      ChestMenu menu = new ChestMenu(CMIChatColor.stripColor(machine.getItemName()) + " &8· &e合成配方");
      menu.setEmptySlotsClickable(false);
      menu.addMenuOpeningHandler(pl -> SoundEffect.GUIDE_BUTTON_CLICK_SOUND.playFor(pl));
      for (int i = 0; i < 54; i++) {
         menu.addItem(i, ChestMenuUtils.getBackground(), (pl, s, it, a) -> false);
      }

      // 3x3 合成材料（可点击跳对应指南页）
      ItemStack[] craft = machine.getRecipe();
      for (int i = 0; i < 9 && i < craft.length; i++) {
         if (craft[i] == null || craft[i].getType().isAir()) {
            continue;
         }

         ItemStack display = describeCraftInput(craft[i], i);
         menu.addItem(CRAFT_SLOTS[i], display, (pl, s, it, a) -> {
            navigateIngredient(pl, display);
            return false;
         });
      }

      // 配方类型图标 + 机器本体
      ItemStack rtIcon = machine.getRecipeType().getItem(p);
      menu.addItem(10, rtIcon != null ? rtIcon : ChestMenuUtils.getBackground(), (pl, s, it, a) -> false);
      menu.addItem(16, describeMachine(machine, null), (pl, s, it, a) -> false);
      menu.addItem(SLOT_ICON, infoItem("合成配方", "使用 " + recipeTypeName(p, machine) + " 合成该机器"),
         (pl, s, it, a) -> false);

      // 返回（35）
      menu.addItem(SLOT_BACK, backItem(backOpener == null), (pl, s, it, a) -> {
         runBack(pl, backOpener);
         return false;
      });

      // 右下角：配方展示（产物列表）
      List<CustomLinkedMachineRecipe> work = linkedRecipes(machine);
      if (!work.isEmpty()) {
         menu.addItem(SLOT_PAGE, productsButton(work.size()), (pl, s, it, a) -> {
            openProductList(pl, machine, 0, backOpener);
            return false;
         });
      }

      menu.open(p);
   }

   /** 某配方所需的材料页数。 */
   private static int matPagesOf(CustomLinkedMachineRecipe recipe) {
      int n = 0;
      for (ItemStack v : recipe.getLinkedInput().values()) {
         if (v != null && !v.getType().isAir()) {
            n++;
         }
      }

      return Math.max(1, (n + PER_PAGE - 1) / PER_PAGE);
   }

   /** 产物列表页：主产物去重后逐格展示，点击进入该产物的配方页。 */
   private static void openProductList(Player p, CustomLinkedRecipeMachine machine, int page, Runnable backOpener) {
      List<CustomLinkedMachineRecipe> work = linkedRecipes(machine);
      // 按主产物身份去重（SF id 优先，否则材质+显示名），记录每产物命中的配方下标
      Map<String, ItemStack> products = new LinkedHashMap<>();
      Map<String, List<Integer>> indices = new LinkedHashMap<>();
      for (int i = 0; i < work.size(); i++) {
         ItemStack main = mainOutput(work.get(i));
         if (main.getType().isAir() || main.getType() == Material.BARRIER) {
            continue;
         }

         String key = productKey(main);
         products.putIfAbsent(key, main);
         indices.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
      }

      List<String> keys = new ArrayList<>(products.keySet());
      if (keys.isEmpty()) {
         openCraftPage(p, machine, backOpener);
         return;
      }

      int perPage = 45; // 0..44 放产物，45 返回，53 翻页
      int pages = Math.max(1, (keys.size() + perPage - 1) / perPage);
      int pg = Math.floorMod(page, pages);

      ChestMenu menu = new ChestMenu(CMIChatColor.stripColor(machine.getItemName())
         + " &8· &e产物配方 &7[" + (pg + 1) + "/" + pages + "]");
      menu.setEmptySlotsClickable(false);
      menu.addMenuOpeningHandler(pl -> SoundEffect.GUIDE_BUTTON_CLICK_SOUND.playFor(pl));
      for (int i = 0; i < 54; i++) {
         menu.addItem(i, ChestMenuUtils.getBackground(), (pl, s, it, a) -> false);
      }

      for (int i = 0; i < perPage; i++) {
         int k = pg * perPage + i;
         if (k >= keys.size()) {
            break;
         }

         String key = keys.get(k);
         List<Integer> idxs = indices.get(key);
         menu.addItem(i, productEntry(products.get(key), idxs.size()), (pl, s, it, a) -> {
            // cycle 限定为该产物的配方集合：翻页只在同一产物的多个配方间循环
            List<CustomLinkedMachineRecipe> cycle = new ArrayList<>(idxs.size());
            for (int idx : idxs) {
               cycle.add(work.get(idx));
            }

            openRecipePage(pl, machine, cycle, 0, 0, true, backOpener);
            return false;
         });
      }

      // 返回合成配方页（45）
      menu.addItem(45, backItem(false), (pl, s, it, a) -> {
         openCraftPage(pl, machine, backOpener);
         return false;
      });

      // 翻页（53）
      if (pages > 1) {
         menu.addItem(SLOT_PAGE, pageButton("产物 " + (pg + 1) + "/" + pages), (pl, s, it, a) -> {
            openProductList(pl, machine, a.isRightClicked() ? pg - 1 : pg + 1, backOpener);
            return false;
         });
      }

      menu.open(p);
   }

   // ------------------------------------------------------------------
   // 配方级入口（机器 GUI「多物品输入」路径 + 机器级配方页共用渲染）
   // ------------------------------------------------------------------

   /**
    * 打开单个大型配方的材料分页视图（机器 GUI 路径，行为与改动前一致）。
    */
   public static void open(Player player, CustomLinkedMachineRecipe recipe) {
      openRecipePage(player, null, List.of(recipe), 0, 0, false, null);
   }

   /**
    * 配方页共用渲染：材料区绑定槽直映 + 超出分页；右下角在
    * 「cycle 内的配方 × 每配方材料页」铺平后的序列里循环翻页。
    *
    * @param machine    机器上下文；null = 纯配方视图（标题/图标/返回退化为旧行为）
    * @param cycle      可循环展示的配方集合
    * @param pos        当前配方在 cycle 中的下标
    * @param matPage    当前配方的材料页码
    * @param fromList   是否从产物列表进入（返回键回产物列表）
    * @param backOpener 「返回」动作；null = 回指南
    */
   private static void openRecipePage(Player p, CustomLinkedRecipeMachine machine,
                                      List<CustomLinkedMachineRecipe> cycle, int pos, int matPage,
                                      boolean fromList, Runnable backOpener) {
      CustomLinkedMachineRecipe recipe = cycle.get(pos);
      // 按槽号排序：与机器 GUI 中的摆放顺序一致，玩家能对上位置
      List<Map.Entry<Integer, ItemStack>> entries = new ArrayList<>();
      for (Map.Entry<Integer, ItemStack> e : new TreeMap<>(recipe.getLinkedInput()).entrySet()) {
         if (e.getValue() != null && !e.getValue().getType().isAir()) {
            entries.add(e);
         }
      }

      int matPages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
      matPage = Math.floorMod(matPage, matPages);
      final int curMatPage = matPage;

      String title = machine != null
         ? CMIChatColor.stripColor(machine.getItemName())
            + " &8· &e配方 " + (pos + 1) + "/" + cycle.size()
            + (matPages > 1 ? " &8· &7材料 " + (matPage + 1) + "/" + matPages : "")
         : "&6大型配方 &7[" + (matPage + 1) + "/" + matPages + "]";

      ChestMenu menu = new ChestMenu(title);
      menu.setEmptySlotsClickable(false);
      menu.addMenuOpeningHandler(pl -> SoundEffect.GUIDE_BUTTON_CLICK_SOUND.playFor(pl));

      // 背景
      for (int i = 0; i < 54; i++) {
         menu.addItem(i, ChestMenuUtils.getBackground(), (pl, s, it, a) -> false);
      }

      // 固定槽预留
      boolean[] reserved = new boolean[54];
      reserved[SLOT_ICON] = true;
      reserved[SLOT_OUTPUT] = true;
      reserved[SLOT_BACK] = true;
      reserved[SLOT_PAGE] = true;

      // 材料：绑定槽直映（槽号在 0..53 且未被占用），越界或冲突则顺序补位
      boolean[] used = new boolean[54];
      int start = matPage * PER_PAGE;
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
         menu.addItem(guiSlot, display, (pl, s, it, a) -> {
            navigateIngredient(pl, display);
            return false;
         });
      }

      // 产物：主产物放 24
      ItemStack main = mainOutput(recipe);
      menu.addItem(SLOT_OUTPUT, describeOutput(main, recipe), (pl, s, it, a) -> false);

      // 机器图标（右上角）：机器上下文用机器本体，纯配方视图沿用产物图标
      menu.addItem(SLOT_ICON, machine != null ? describeMachine(machine, recipe) : machineIcon(recipe),
         (pl, s, it, a) -> false);

      // 返回（35）：从产物列表进入 → 回产物列表；否则回指定位置/指南
      menu.addItem(SLOT_BACK, backItem(machine == null && backOpener == null && !fromList), (pl, s, it, a) -> {
         if (machine != null && fromList) {
            openProductList(pl, machine, 0, backOpener);
         } else {
            runBack(pl, backOpener);
         }
         return false;
      });

      // 翻页（53）：在「cycle × 各自材料页」的序列里循环；单页单配方时为返回键。
      // 各配方的材料页数不同（2 材料与 242 材料的配方共存），不能共用一个页数做除法，
      // 直接在 (pos, matPage) 二维坐标上走下一步：先走完当前配方的材料页，再换下一配方。
      if (cycle.size() > 1 || matPages > 1) {
         boolean multiRecipe = cycle.size() > 1;
         String label = multiRecipe
            ? "配方 " + (pos + 1) + "/" + cycle.size() + (matPages > 1 ? " · 材料 " + (matPage + 1) + "/" + matPages : "")
            : "材料 " + (matPage + 1) + "/" + matPages;
         menu.addItem(SLOT_PAGE, pageButton(label), (pl, s, it, a) -> {
            int np = pos;
            int nm = curMatPage;
            if (a.isRightClicked()) {
               // 上一页：当前配方首页则跳到上一配方的最后一页
               if (nm > 0) {
                  nm--;
               } else {
                  np = Math.floorMod(np - 1, cycle.size());
                  nm = matPagesOf(cycle.get(np)) - 1;
               }
            } else {
               // 下一页：走完当前配方的材料页再换下一配方
               if (nm + 1 < matPagesOf(cycle.get(np))) {
                  nm++;
               } else {
                  np = Math.floorMod(np + 1, cycle.size());
                  nm = 0;
               }
            }

            openRecipePage(pl, machine, cycle, np, nm, fromList, backOpener);
            return false;
         });
      } else {
         menu.addItem(SLOT_PAGE, backItem(machine == null && backOpener == null && !fromList), (pl, s, it, a) -> {
            if (machine != null && fromList) {
               openProductList(pl, machine, 0, backOpener);
            } else {
               runBack(pl, backOpener);
            }
            return false;
         });
      }

      menu.open(p);
   }

   // ------------------------------------------------------------------
   // 导航与工具
   // ------------------------------------------------------------------

   /** 返回动作：有指定 opener 用之，否则回 JEG/原生指南（JEG 未接管时走 Slimefun 静态入口）。 */
   private static void runBack(Player p, Runnable backOpener) {
      if (backOpener != null) {
         backOpener.run();
         return;
      }

      if (JegHook.available()) {
         JegHook.openGuide(p);
         return;
      }

      PlayerProfile.find(p).ifPresent(profile ->
         SlimefunGuide.openMainMenu(profile, SlimefunGuideMode.SURVIVAL_MODE, 1));
   }

   /**
    * 材料点击导航。
    *
    * <p>与终焉厨锅一致：附属材料跳 JEG/原生指南的该物品页；
    * 原版材料经 JegBridge 的 ItemStack 路由进 JEG 的原版配方页
    * （无原版配方的物品 JEG 静默，菜单保持不动）。
    */
   private static void navigateIngredient(Player player, ItemStack display) {
      // JegBridge 内部走 SlimefunGuide.displayItem 静态入口，装了 JEG 自动进 JEG；
      // 原版材料也会路由到 JEG 的原版配方页
      if (!JegBridge.displayItem(player, display)) {
         SlimefunItem sf = SlimefunItem.getByItem(display);
         if (sf != null) {
            PlayerProfile.find(player).ifPresent(profile ->
               SlimefunGuide.displayItem(profile, sf, true));
         }
      }
   }

   /** 机器注册的全部工作配方（联动槽形态）。 */
   private static List<CustomLinkedMachineRecipe> linkedRecipes(CustomLinkedRecipeMachine machine) {
      List<CustomLinkedMachineRecipe> out = new ArrayList<>();
      for (MachineRecipe r : machine.getMachineRecipes()) {
         if (r instanceof CustomLinkedMachineRecipe linked) {
            out.add(linked);
         }
      }

      return out;
   }

   /** 产物身份键：SF id 优先，否则材质+显示名。 */
   private static String productKey(ItemStack out) {
      SlimefunItem sf = SlimefunItem.getByItem(out);
      if (sf != null) {
         return "sf:" + sf.getId();
      }

      String name = out.hasItemMeta() && out.getItemMeta().hasDisplayName()
         ? out.getItemMeta().getDisplayName() : "";
      return "mc:" + out.getType() + ":" + name;
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

      tech.komutech.objects.customs.LinkedOutput linked = recipe.getLinkedOutput();
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

   private static String recipeTypeName(Player p, CustomLinkedRecipeMachine machine) {
      try {
         return CMIChatColor.stripColor(machine.getRecipeType().getItem(p).getItemMeta().getDisplayName());
      } catch (Throwable t) {
         return machine.getRecipeType().getKey().getKey();
      }
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

   private static ItemStack describeCraftInput(ItemStack in, int index) {
      ItemStack clone = in.clone();
      ItemMeta meta = clone.getItemMeta();
      if (meta != null) {
         List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
         lore.add("");
         lore.add(CMIChatColor.translate("&a合成材料 " + (index + 1)));
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

   /** 产物列表条目：数量 lore + 配方数提示。 */
   private static ItemStack productEntry(ItemStack out, int recipeCount) {
      ItemStack clone = out.clone();
      ItemMeta meta = clone.getItemMeta();
      if (meta != null) {
         List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
         lore.add("");
         lore.add(CMIChatColor.translate("&a产物"));
         lore.add(CMIChatColor.translate("&7共有 &f" + recipeCount + " &7个配方"));
         lore.add(CMIChatColor.translate("&e点击查看配方"));
         meta.setLore(lore);
         clone.setItemMeta(meta);
      }

      return clone;
   }

   /** 机器图标（右上角）：带耗时/身份 lore。 */
   private static ItemStack describeMachine(CustomLinkedRecipeMachine machine, CustomLinkedMachineRecipe r) {
      ItemStack icon = machine.getItem().clone();
      ItemMeta meta = icon.getItemMeta();
      if (meta != null) {
         List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
         lore.add("");
         if (r != null) {
            lore.add(CMIChatColor.translate("&7耗时: &f" + (r.getTicks() / 2) + "s"));
            lore.add(CMIChatColor.translate("&8在该机器中制作"));
         } else {
            lore.add(CMIChatColor.translate("&8机器本体（合成产物）"));
         }

         meta.setLore(lore);
         icon.setItemMeta(meta);
      }

      return icon;
   }

   private static ItemStack infoItem(String name, String desc) {
      ItemStack it = new ItemStack(Material.BOOK);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CMIChatColor.translate("&e" + name));
         meta.setLore(List.of(CMIChatColor.translate("&7" + desc)));
         it.setItemMeta(meta);
      }

      return it;
   }

   /** 合成配方页右下角按钮：进入产物列表。 */
   private static ItemStack productsButton(int workCount) {
      ItemStack it = new ItemStack(Material.BOOK);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CMIChatColor.translate("&e配方展示"));
         meta.setLore(List.of(
            CMIChatColor.translate("&7按产物浏览全部工作配方"),
            CMIChatColor.translate("&7当前共 &f" + workCount + " &7个配方"),
            "",
            CMIChatColor.translate("&e点击打开产物列表")));
         it.setItemMeta(meta);
      }

      return it;
   }

   private static ItemStack pageButton(String name) {
      ItemStack it = new ItemStack(Material.ARROW);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CMIChatColor.translate("&e" + name));
         meta.setLore(List.of(
            CMIChatColor.translate("&7左键：下一页"),
            CMIChatColor.translate("&7右键：上一页")));
         it.setItemMeta(meta);
      }

      return it;
   }

   private static ItemStack backItem(boolean toGuide) {
      ItemStack it = new ItemStack(Material.OAK_DOOR);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CMIChatColor.translate(toGuide ? "&e返回指南" : "&e返回"));
         if (toGuide) {
            meta.setLore(List.of(CMIChatColor.translate("&7返回 Slimefun / JEG 指南")));
         }

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

         products.putIfAbsent(productKey(main), main);
      }

      return products;
   }
}
