package tech.komutech.objects.customs.generations;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import java.util.List;
import javax.annotation.Nonnull;

public class GenerationInfo {
   private final SlimefunItemStack slimefunItemStack;
   private final List<GenerationArea> areas;

   public GenerationInfo(@Nonnull SlimefunItemStack var1, @Nonnull List<GenerationArea> var2) {
      this.slimefunItemStack = var1;
      this.areas = var2;
   }

   public SlimefunItemStack getSlimefunItemStack() {
      return this.slimefunItemStack;
   }

   public List<GenerationArea> getAreas() {
      return this.areas;
   }
}
