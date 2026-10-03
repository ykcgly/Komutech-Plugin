package tech.komutech.objects.machine;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import tech.komutech.util.FastItemMatch;

/**
 * 模板机器的模板组：模板物品 + 该模板下的工作配方。
 *
 * <p>曾是 record，现改为 final class 以携带加载期预解析的
 * {@link FastItemMatch.Spec}——tick 里每个模板都要与槽位物品比对，
 * 走 {@link FastItemMatch} 省掉每次比较两次
 * {@code SlimefunItem.getByItem} 全注册表扫描（spark 火焰图主热点）。</p>
 */
public final class MachineTemplate {
   private final ItemStack template;
   private final List<CustomMachineRecipe> recipes;
   private final FastItemMatch.Spec spec;

   public MachineTemplate(ItemStack template, List<CustomMachineRecipe> recipes) {
      this.template = template;
      this.recipes = recipes;
      this.spec = FastItemMatch.Spec.of(template);
   }

   public ItemStack template() {
      return this.template;
   }

   /** 模板堆的预解析匹配规格（供材质索引等复用，可能为 null——模板堆为空时）。 */
   @Nullable
   public FastItemMatch.Spec spec() {
      return this.spec;
   }

   public List<CustomMachineRecipe> recipes() {
      return this.recipes;
   }

   /**
    * 槽位物品是否为该模板。
    *
    * <p>原实现 {@code SlimefunUtils.isItemSimilar(item, template, true, true, true)}
    * 展开为 checkLore=true / checkAmount=true / 快速 id 路径开，语义由
    * {@link FastItemMatch} 完整保留。</p>
    */
   public boolean isItemSimilar(@Nullable ItemStack item) {
      return this.spec != null && FastItemMatch.matches(item, this.spec, true);
   }
}
