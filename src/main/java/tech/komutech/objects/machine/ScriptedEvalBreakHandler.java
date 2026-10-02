package tech.komutech.objects.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import java.util.List;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tech.komutech.script.ScriptEval;

public class ScriptedEvalBreakHandler extends BlockBreakHandler {
   private final ScriptEval eval;
   private final InventoryBlock machine;

   public ScriptedEvalBreakHandler(InventoryBlock var1, ScriptEval var2) {
      super(false, false);
      this.eval = var2;
      this.machine = var1;
   }

   public void onPlayerBreak(BlockBreakEvent var1, @NotNull ItemStack var2, @NotNull List<ItemStack> var3) {
      Block var4 = var1.getBlock();
      Location var5 = var4.getLocation();
      BlockMenu var6 = StorageCacheUtils.getMenu(var5);
      if (var6 != null) {
         if (this.machine.getInputSlots().length > 0) {
            var6.dropItems(var5, this.machine.getInputSlots());
         }

         if (this.machine.getOutputSlots().length > 0) {
            var6.dropItems(var5, this.machine.getOutputSlots());
         }
      }

      if (this.eval != null) {
         this.eval.evalFunction("onBreak", var1, var2, var3);
      }
   }
}
