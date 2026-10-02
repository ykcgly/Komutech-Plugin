package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.machine.MachineRecord;
import tech.komutech.script.ScriptEval;
import tech.komutech.util.ExceptionHandler;

public class CustomEnergyGenerator extends CustomMachine implements EnergyNetProvider {
   private final ScriptEval eval;
   private final int defaultOutput;

   public CustomEnergyGenerator(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      @Nullable CustomMenu var5,
      List<Integer> var6,
      List<Integer> var7,
      MachineRecord var8,
      EnergyNetComponentType var9,
      @Nullable ScriptEval var10,
      int var11
   ) {
      super(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
      this.eval = var10;
      this.defaultOutput = var11;
   }

   public int getGeneratedOutput(@NotNull Location var1, @NotNull SlimefunBlockData var2) {
      if (this.eval == null) {
         return this.defaultOutput;
      } else {
         try {
            Object var3 = this.eval.evalFunction("getGeneratedOutput", var1, var2);
            if (var3 instanceof Integer var4) {
               return var4;
            } else {
               ExceptionHandler.handleWarning("getGeneratedOutput() 返回了一个非整数值: " + var3 + " 导致自定义发电机的默认输出值将被使用， 请找附属对应作者修复此问题！");
               return this.defaultOutput;
            }
         } catch (Exception var5) {
            return this.defaultOutput;
         }
      }
   }
}
