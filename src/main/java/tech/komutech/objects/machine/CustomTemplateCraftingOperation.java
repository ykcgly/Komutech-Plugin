package tech.komutech.objects.machine;

import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;

public class CustomTemplateCraftingOperation implements MachineOperation {
   private final CustomMachineRecipe recipe;
   private final MachineTemplate template;
   private final int ticks;
   private int currentTicks = 0;

   public CustomTemplateCraftingOperation(MachineTemplate var1, CustomMachineRecipe var2, int var3) {
      Validate.isTrue(var2.getOutput().length != 0, "The recipe must have at least one output.");
      Validate.isTrue(var3 >= 0, "The amount of total ticks must be a positive integer or zero, received: " + var3);
      this.recipe = var2;
      this.ticks = var3;
      this.template = var1;
   }

   public void addProgress(int var1) {
      Validate.isTrue(var1 > 0, "Progress must be positive.");
      this.currentTicks += var1;
   }

   public int getProgress() {
      return this.currentTicks;
   }

   public int getTotalTicks() {
      return this.ticks;
   }

   public CustomMachineRecipe getRecipe() {
      return this.recipe;
   }

   public MachineTemplate getTemplate() {
      return this.template;
   }
}
