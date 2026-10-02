package tech.komutech.util;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import java.lang.reflect.Field;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import tech.komutech.KT;
import tech.komutech.util.colors.CMIChatColor;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.global.RecipeTypeMap;

public class ExceptionHandler {
   @NotNull
   private static final ConsoleCommandSender console = Bukkit.getConsoleSender();

   public static ExceptionHandler.HandleResult handleIdConflict(String var0) {
      SlimefunItem var1 = SlimefunItem.getById(var0);
      if (var1 != null) {
         console.sendMessage(CMIChatColor.translate("&4ERROR | ID冲突：" + var0 + "与" + var1.getAddon().getName() + "中的物品发生了ID冲突"));
         return ExceptionHandler.HandleResult.FAILED;
      } else {
         return ExceptionHandler.HandleResult.SUCCESS;
      }
   }

   public static ExceptionHandler.HandleResult handleMenuConflict(String var0) {
      CustomMenu var2 = CommonUtils.getIf(KT.menus.values(), var1x -> var1x.getID().equalsIgnoreCase(var0));
      if (var2 != null) {
         console.sendMessage(CMIChatColor.translate("&4ERROR | ID冲突：已存在菜单ID为" + var0 + "的菜单"));
         return ExceptionHandler.HandleResult.FAILED;
      } else {
         return ExceptionHandler.HandleResult.SUCCESS;
      }
   }

   public static ExceptionHandler.HandleResult handleGroupIdConflict(String var0) {
      ItemGroup var1 = CommonUtils.getIf(Slimefun.getRegistry().getAllItemGroups(), var1x -> var1x.getKey().getKey().equalsIgnoreCase(var0));
      if (var1 != null) {
         String var2 = "&4ERROR | ID冲突：" + var0 + "与物品组 " + var1.getKey().getKey() + "发生ID冲突";
         if (var1.getAddon() != null) {
            if (var1.getAddon().getClass() == tech.komutech.KomutechPlugin.class) {
               console.sendMessage(CMIChatColor.translate(var2));
               return ExceptionHandler.HandleResult.FAILED;
            } else {
               return ExceptionHandler.HandleResult.SUCCESS;
            }
         } else {
            console.sendMessage(CMIChatColor.translate(var2));
            return ExceptionHandler.HandleResult.FAILED;
         }
      } else {
         return ExceptionHandler.HandleResult.SUCCESS;
      }
   }

   public static void handleWarning(String var0) {
      if (var0 != null && !var0.isBlank()) {
         console.sendMessage(CMIChatColor.translate("&eWARNING | " + var0));
      }
   }

   public static void handleError(String var0) {
      if (var0 != null && !var0.isBlank()) {
         console.sendMessage(CMIChatColor.translate("&4ERROR | " + var0));
      }
   }

   public static void handleError(String var0, Throwable var1) {
      if (var0 != null && !var0.isBlank()) {
         if (var1 != null) {
            console.sendMessage(CMIChatColor.translate("&4ERROR | " + var0));
            KT.plugin().getLogger().log(Level.SEVERE, var0, var1);
         } else {
            handleError(var0);
         }
      }
   }

   public static void debugLog(String var0) {
      if (KT.plugin().getConfig().getBoolean("debug")) {
         if (var0 == null || var0.isBlank()) {
            return;
         }

         console.sendMessage(CMIChatColor.translate("&6DEBUG | " + var0));
      }
   }

   public static void handleDanger(String var0) {
      if (var0 != null && !var0.isBlank()) {
         console.sendMessage(CMIChatColor.translate("&c&u&l&bD&4&lA&c&lN&b&lG&4&lE&c&lR | " + var0));
      }
   }

   public static void info(String var0) {
      if (var0 != null && !var0.isBlank()) {
         console.sendMessage(CMIChatColor.translate("&aINFO | " + var0));
      }
   }

   public static <T extends Enum<T>> Pair<ExceptionHandler.HandleResult, T> handleEnumValueOf(String var0, Class<T> var1, String var2) {
      try {
         if (!var2.contains("|")) {
            return new Pair(ExceptionHandler.HandleResult.SUCCESS, Enum.valueOf(var1, var2.toUpperCase()));
         }

         for (String var7 : var2.split("\\|")) {
            try {
               return new Pair(ExceptionHandler.HandleResult.SUCCESS, Enum.valueOf(var1, var7.trim().toUpperCase()));
            } catch (NullPointerException | IllegalArgumentException var9) {
            }
         }

         handleError(var0);
      } catch (NullPointerException | IllegalArgumentException var10) {
         handleError(var0);
      }

      return new Pair(ExceptionHandler.HandleResult.FAILED, null);
   }

   public static Pair<ExceptionHandler.HandleResult, ItemGroup> handleItemGroupGet(String var1) {
      ItemGroup var2 = CommonUtils.getIf(KT.groups.values(), var1x -> var1x.getKey().getKey().equalsIgnoreCase(var1));
      if (var2 == null) {
         ItemGroup var3 = CommonUtils.getIf(Slimefun.getRegistry().getAllItemGroups(), var1x -> var1x.getKey().toString().equalsIgnoreCase(var1));
         if (var3 == null) {
            handleError("无法在 Komutech 中找到该物品组 " + var1);
            return new Pair(ExceptionHandler.HandleResult.FAILED, null);
         } else {
            return new Pair(ExceptionHandler.HandleResult.SUCCESS, var3);
         }
      } else {
         return new Pair(ExceptionHandler.HandleResult.SUCCESS, var2);
      }
   }

   public static Pair<ExceptionHandler.HandleResult, RecipeType> getRecipeType(String var0, String var1) {
      try {
         Field var2 = RecipeType.class.getDeclaredField(var1);
         return new Pair(ExceptionHandler.HandleResult.SUCCESS, (RecipeType)var2.get(null));
      } catch (NoSuchFieldException var4) {
         RecipeType var3 = RecipeTypeMap.getRecipeType(var1);
         if (var3 == null) {
            handleError(var0);
            return new Pair(ExceptionHandler.HandleResult.FAILED, null);
         } else {
            return new Pair(ExceptionHandler.HandleResult.SUCCESS, var3);
         }
      } catch (IllegalAccessException var5) {
         return new Pair(ExceptionHandler.HandleResult.FAILED, null);
      }
   }

   public static enum HandleResult {
      SUCCESS,
      FAILED;
   }
}
