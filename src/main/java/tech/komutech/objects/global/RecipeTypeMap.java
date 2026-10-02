package tech.komutech.objects.global;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;

public class RecipeTypeMap {
   private static final Map<String, RecipeType> recipeTypes = new HashMap<>();
   private static volatile boolean externalRecipeTypesRegistered;

   public static void removeRecipeTypes(String... var0) {
      for (String var4 : var0) {
         recipeTypes.remove(var4);
      }
   }

   public static void pushRecipeType(RecipeType var0) {
      recipeTypes.put(var0.getKey().getKey().toUpperCase(), var0);
   }

   public static void pushRecipeType(List<RecipeType> var0) {
      var0.forEach(RecipeTypeMap::pushRecipeType);
   }

   public static void clearRecipeTypes() {
      recipeTypes.clear();
      externalRecipeTypesRegistered = false;
   }

   @Nullable
   public static RecipeType getRecipeType(String var0) {
      ensureExternalRecipeTypes();
      return recipeTypes.get(var0);
   }

   public static void ensureExternalRecipeTypes() {
      if (!externalRecipeTypesRegistered) {
         if (Bukkit.getPluginManager().isPluginEnabled("Slimefun")) {
            externalRecipeTypesRegistered = true;
            RecipeTypeMap.RecipeTypeExpandIntegration.registerRecipeTypes();
         }
      }
   }

   public static enum RecipeTypeExpandIntegration {
      INFINITY_EXPANSION("io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench", "TYPE", true),
      SLIME_TINKER("io.github.sefiraat.slimetinker.items.workstations.workbench.Workbench", "TYPE", true);

      private final String clazz;
      private final String fieldName;
      private final boolean isStatic;

      private RecipeTypeExpandIntegration(String nullxx, String nullxxx, boolean nullxxxx) {
         this.clazz = nullxx;
         this.fieldName = nullxxx;
         this.isStatic = nullxxxx;
      }

      public RecipeType get() {
         try {
            Class var1 = Class.forName(this.clazz);
            Field var2 = var1.getDeclaredField(this.fieldName);
            return (RecipeType)var2.get(null);
         } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException var3) {
            return null;
         }
      }

      static void registerRecipeTypes() {
         for (RecipeTypeMap.RecipeTypeExpandIntegration var3 : values()) {
            String var4 = var3.clazz;
            String var5 = var3.fieldName;
            if (var3.isIntegrationReady()) {
               try {
                  Class var6 = Class.forName(var4);
                  Object var7 = var3.isStatic ? var6.getField(var5).get(null) : var6.getDeclaredConstructor().newInstance();
                  if (var7 instanceof RecipeType) {
                     RecipeTypeMap.pushRecipeType((RecipeType)var7);
                  }
               } catch (Exception var8) {
                  if (KT.plugin() != null) {
                     KT.plugin().getLogger().warning("Failed to get external recipe type from " + var4 + "#" + var5 + ": " + var8.getMessage());
                  }
               }
            }
         }
      }

      private boolean isIntegrationReady() {
         if ("io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench".equals(this.clazz)) {
            return Bukkit.getPluginManager().isPluginEnabled("InfinityExpansion");
         } else {
            return "io.github.sefiraat.slimetinker.items.workstations.workbench.Workbench".equals(this.clazz)
               ? Bukkit.getPluginManager().isPluginEnabled("SlimeTinker")
               : true;
         }
      }
   }
}
