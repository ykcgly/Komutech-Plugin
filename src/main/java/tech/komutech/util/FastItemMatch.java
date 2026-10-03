package tech.komutech.util;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 预计算型物品匹配器（语义对齐 {@code SlimefunUtils.isItemSimilar}，热路径专用）。
 *
 * <p><b>为什么不用 SlimefunUtils.isItemSimilar</b>：spark 火焰图显示模板机器每 tick
 * 的匹配链路 {@code tick → isItemSimilar → ItemStackService.resolveHandler →
 * SlimefunItem.getByItem} 占据了主线程耗时大头——{@code matches()} 会先对**双方**
 * 各做一次 {@code SlimefunItem.getByItem}（全注册表流式扫描，注册表越大越慢），
 * 只为解析 VirtualItemHandler；而本附属的物品全是静态堆，该步骤恒返回
 * NOT_HANDLED，之后才走经典的「材质 → 数量 → sf id → 显示名/lore」比较。
 * 等于每次比较白付两次注册表扫描。</p>
 *
 * <p>本类把「比较基准堆」的材质/数量/sf id/显示名/lore 在加载期解析成
 * {@link Spec}，运行期每次比较只需：1 次材质比较 + 1 次 meta 读取 + 1 次 PDC 读取
 * （+ 名/lore 比较），并完全保留原语义：</p>
 * <ol>
 *   <li>材质相等（硬条件）、槽位物品数量 &ge; 基准数量（checkAmount 恒开）；</li>
 *   <li>基准堆是 SlimefunItemStack：槽位物品带 id → id 相等即匹配（不再比名/lore），
 *       id 不同即不匹配；槽位物品无 id → 落到 meta 比较；</li>
 *   <li>基准堆是普通堆：双方都带 id → id 相等裁决；否则 meta 比较；</li>
 *   <li>meta 比较：显示名（含有无一致性）始终比对，lore 仅在 checkLore 时比对——
 *       与 {@code equalsItemMeta} 一致。</li>
 * </ol>
 *
 * <p>不支持的语义（对本附属均不适用，见上）：VirtualItemHandler、DistinctiveItem、
 * 虚拟物品。</p>
 */
public final class FastItemMatch {

   private FastItemMatch() {
   }

   /** 比较基准堆的预解析结果（加载期构建一次，运行期只读）。 */
   public record Spec(Material material, int amount, @Nullable String sfId,
                      @Nullable String displayName, @Nullable List<String> lore,
                      boolean sfStack) {

      @Nullable
      public static Spec of(@Nullable ItemStack stack) {
         if (stack == null || stack.getType().isAir()) {
            return null;
         }

         String id = null;
         String name = null;
         List<String> lore = null;
         if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
               id = Slimefun.getItemDataService().getItemData(meta).orElse(null);
               name = meta.hasDisplayName() ? meta.getDisplayName() : null;
               lore = meta.hasLore() ? meta.getLore() : null;
            }
         }

         return new Spec(stack.getType(), stack.getAmount(), id, name, lore,
            stack instanceof SlimefunItemStack);
      }
   }

   /**
    * 判断槽位物品是否匹配预解析的基准堆。
    *
    * @param item      槽位里的实际物品（可为 live 堆或 ItemStackWrapper）
    * @param spec      基准堆预解析结果
    * @param checkLore 是否比对 lore（两处调用点均为 true，与原调用一致）
    */
   public static boolean matches(@Nullable ItemStack item, Spec spec, boolean checkLore) {
      if (item == null || item.getType().isAir() || item.getType() != spec.material()) {
         return false;
      }

      if (item.getAmount() < spec.amount()) {
         return false;
      }

      if (!item.hasItemMeta()) {
         return false;
      }

      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return false;
      }

      String id = Slimefun.getItemDataService().getItemData(meta).orElse(null);
      if (spec.sfStack()) {
         if (id != null) {
            // id 相等即匹配（原语义：不再比名/lore）；id 不同即不匹配
            return id.equals(spec.sfId());
         }
      } else if (id != null && spec.sfId() != null) {
         // 双方都带 id：id 裁决
         return id.equals(spec.sfId());
      }

      // meta 比较（基准堆无 id 或槽位物品无 id）
      if (meta.hasDisplayName() != (spec.displayName() != null)) {
         return false;
      }

      if (spec.displayName() != null && !Objects.equals(spec.displayName(), meta.getDisplayName())) {
         return false;
      }

      if (checkLore) {
         if (meta.hasLore() != (spec.lore() != null)) {
            return false;
         }

         if (spec.lore() != null && !Objects.equals(spec.lore(), meta.getLore())) {
            return false;
         }
      }

      return true;
   }
}
