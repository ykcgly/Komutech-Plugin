package tech.komutech.objects.machine;

import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import javax.annotation.Nonnull;

public class CustomCraftingOperation extends CraftingOperation {
   private final CustomMachineRecipe recipe;

   public CustomCraftingOperation(@Nonnull CustomMachineRecipe var1) {
      super(var1.getInput(), var1.getOutput(), var1.getTicks());
      Validate.isTrue(var1.getTicks() >= 0, "The amount of total ticks must be a positive integer or zero, received: " + var1.getTicks());
      this.recipe = var1;
   }

   public int getTotalTicks() {
      return this.recipe.getTicks();
   }

   public CustomMachineRecipe getRecipe() {
      return this.recipe;
   }
}
