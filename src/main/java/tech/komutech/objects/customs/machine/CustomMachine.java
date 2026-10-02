package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import java.util.List;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.parent.AbstractEmptyMachine;
import tech.komutech.objects.machine.MachineInfo;
import tech.komutech.objects.machine.MachineRecord;
import tech.komutech.objects.machine.ScriptedEvalBreakHandler;
import tech.komutech.script.ScriptEval;

public class CustomMachine extends AbstractEmptyMachine<MachineOperation> implements EnergyNetComponent {
   private final MachineRecord theRecord;
   private final List<Integer> input;
   private final List<Integer> output;
   private final EnergyNetComponentType type;
   private final MachineProcessor<MachineOperation> processor;
   @Nullable
   private final ScriptEval eval;
   private final CustomMenu menu;

   public CustomMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      @Nullable CustomMenu var5,
      List<Integer> var6,
      List<Integer> var7,
      MachineRecord var8,
      EnergyNetComponentType var9,
      @Nullable ScriptEval var10
   ) {
      super(var1, var2, var3, var4);
      this.input = var6;
      this.output = var7;
      this.theRecord = var8;
      this.menu = var5;
      this.type = var9;
      this.eval = var10;
      this.processor = new MachineProcessor(this);
      if (var10 != null) {
         var10.doInit();
         this.addItemHandler(new ItemHandler[]{new BlockPlaceHandler(false) {
            public void onPlayerPlace(@NotNull BlockPlaceEvent var1) {
               CustomMachine.this.eval.evalFunction("onPlace", var1);
            }
         }, (BlockUseHandler)var1x -> this.eval.evalFunction("onUse", var1x), new BlockBreakHandler(false, false) {
            public void onPlayerBreak(@NotNull BlockBreakEvent var1, @NotNull ItemStack var2x, @NotNull List<ItemStack> var3x) {
               MachineOperation var4x = CustomMachine.this.getMachineProcessor().getOperation(var1.getBlock());
               if (var4x != null) {
                  CustomMachine.this.getMachineProcessor().endOperation(var1.getBlock());
               }

               CustomMachine.this.eval.evalFunction("onBreak", var1, var2x, var3x);
            }
         }});
      }

      this.addItemHandler(new ItemHandler[]{new ScriptedEvalBreakHandler(this, var10)});
      if (var5 != null) {
         this.processor.setProgressBar(var5.getProgressBarItem());
         this.createPreset(this, var2x -> {
            var5.registerInteractableSlots(this.input);
            var5.registerInteractableSlots(this.output);
            var5.apply(var2x);
         });
      }
   }

   public void preRegister() {
      super.preRegister();
      this.addItemHandler(new ItemHandler[]{this.getBlockTicker()});
   }

   protected void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
      if (this.eval != null) {
         BlockMenu var4 = StorageCacheUtils.getMenu(var1.getLocation());
         MachineInfo var5 = new MachineInfo(var4, var3, var2, var1, this.processor, null, this);
         this.eval.evalFunction("tick", var5);
      }
   }

   @Override
   public BlockTicker getBlockTicker() {
      return new BlockTicker() {
         public boolean isSynchronized() {
            return true;
         }

         public void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
            CustomMachine.this.tick(var1, var2, var3);
         }
      };
   }

   public int[] getInputSlots() {
      return this.input.stream().mapToInt(var0 -> var0).toArray();
   }

   public int[] getOutputSlots() {
      return this.output.stream().mapToInt(var0 -> var0).toArray();
   }

   @NotNull
   public EnergyNetComponentType getEnergyComponentType() {
      return this.type;
   }

   public int getCapacity() {
      return this.theRecord.capacity();
   }

   @NotNull
   public MachineProcessor<MachineOperation> getMachineProcessor() {
      return this.processor;
   }

   public CustomMenu getMenu() {
      return this.menu;
   }
}
