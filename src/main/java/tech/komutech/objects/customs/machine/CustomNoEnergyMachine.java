package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.parent.AbstractEmptyMachine;
import tech.komutech.objects.machine.ScriptedEvalBreakHandler;
import tech.komutech.objects.machine.SmallerMachineInfo;
import tech.komutech.script.ScriptEval;
import tech.komutech.objects.script.lambda.RSCClickHandler;

public class CustomNoEnergyMachine extends AbstractEmptyMachine<MachineOperation> {
   private final List<Integer> input;
   private final List<Integer> output;
   @Nullable
   private final ScriptEval eval;
   private final MachineProcessor<MachineOperation> processor;
   private final CustomMenu menu;

   public CustomNoEnergyMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      CustomMenu var5,
      List<Integer> var6,
      List<Integer> var7,
      @Nullable ScriptEval var8,
      int var9
   ) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, Collections.singletonList(var9));
   }

   public CustomNoEnergyMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      CustomMenu var5,
      List<Integer> var6,
      List<Integer> var7,
      @Nullable final ScriptEval var8,
      List<Integer> var9
   ) {
      super(var1, var2, var3, var4);
      this.input = var6;
      this.output = var7;
      this.eval = var8;
      this.processor = new MachineProcessor(this);
      this.menu = var5;
      if (var8 != null) {
         var8.addThing("setWorking", (Consumer<Object>)var1x -> var8.addThing("working", var1x));
         var8.addThing("working", false);
         var8.doInit();
         this.addItemHandler(new ItemHandler[]{new BlockPlaceHandler(false) {
            public void onPlayerPlace(@NotNull BlockPlaceEvent var1) {
               CustomNoEnergyMachine.this.eval.evalFunction("onPlace", var1);
            }
         }, (BlockUseHandler)var1x -> this.eval.evalFunction("onUse", var1x), new BlockBreakHandler(false, false) {
            public void onPlayerBreak(@NotNull BlockBreakEvent var1, @NotNull ItemStack var2x, @NotNull List<ItemStack> var3x) {
               MachineOperation var4x = CustomNoEnergyMachine.this.getMachineProcessor().getOperation(var1.getBlock());
               if (var4x != null) {
                  CustomNoEnergyMachine.this.getMachineProcessor().endOperation(var1.getBlock());
               }

               CustomNoEnergyMachine.this.eval.evalFunction("onBreak", var1, var2x, var3x);
            }
         }});
      }

      this.addItemHandler(new ItemHandler[]{new ScriptedEvalBreakHandler(this, var8)});
      this.addItemHandler(new ItemHandler[]{this.getBlockTicker()});
      if (this.menu != null) {
         for (int var11 : var9) {
            if (var11 > -1 && var11 < 54) {
               final MenuClickHandler var12 = this.menu.getMenuClickHandler(var11);
               this.menu.addMenuClickHandler(var11, new RSCClickHandler() {
                  @Override
                  public void mainFunction(Player var1, int var2x, ItemStack var3x, ClickAction var4x) {
                     if (var8 != null) {
                        var8.addThing("working", true);
                     }
                  }

                  @Override
                  public void andThen(Player var1, int var2x, ItemStack var3x, ClickAction var4x) {
                     if (var12 != null) {
                        var12.onClick(var1, var2x, var3x, var4x);
                     }
                  }
               });
            }

            this.processor.setProgressBar(var5.getProgressBarItem());
         }

         this.createPreset(this, var1x -> {
            this.menu.registerInteractableSlots(this.input);
            this.menu.registerInteractableSlots(this.output);
            this.menu.apply(var1x);
         });
      }
   }

   public void preRegister() {
      super.preRegister();
      this.addItemHandler(new ItemHandler[]{this.getBlockTicker()});
   }

   protected void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
      if (this.eval != null) {
         SmallerMachineInfo var4 = new SmallerMachineInfo(var3.getBlockMenu(), var3, this, var2, var1, this.processor);
         this.eval.evalFunction("tick", var4);
      }
   }

   public int[] getInputSlots() {
      return this.input.stream().mapToInt(var0 -> var0).toArray();
   }

   public int[] getOutputSlots() {
      return this.output.stream().mapToInt(var0 -> var0).toArray();
   }

   @Override
   public BlockTicker getBlockTicker() {
      return new BlockTicker() {
         public boolean isSynchronized() {
            return true;
         }

         public void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
            CustomNoEnergyMachine.this.tick(var1, var2, var3);
         }
      };
   }

   @NotNull
   public MachineProcessor<MachineOperation> getMachineProcessor() {
      return this.processor;
   }

   public CustomMenu getMenu() {
      return this.menu;
   }
}
