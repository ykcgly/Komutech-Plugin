package tech.komutech.native_scripts.support;

import java.io.File;
import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class KomutechPaths {
   private KomutechPaths() {
   }

   public static File configRoot() {
      File var0 = new File("plugins/RykenSlimefunCustomizer/addon_configs/Komutech");
      if (var0.isDirectory()) {
         return var0;
      } else {
         Plugin var1 = Bukkit.getPluginManager().getPlugin("Komutech");
         if (var1 != null) {
            File var3 = new File(var1.getDataFolder(), "addon_configs/Komutech");
            if (!var3.exists()) {
               var3.mkdirs();
            }

            return var3;
         } else {
            File var2 = new File("plugins/Komutech/addon_configs/Komutech");
            if (!var2.exists()) {
               var2.mkdirs();
            }

            return var2;
         }
      }
   }

   public static Path playerAttributes() {
      return configRoot().toPath().resolve("玩家属性");
   }

   public static Path wanXiangGui() {
      return configRoot().toPath().resolve("萬象匱");
   }

   public static Path yunZhuanXia() {
      return configRoot().toPath().resolve("云篆匣");
   }

   public static Path naJie() {
      return configRoot().toPath().resolve("纳戒");
   }

   public static Path scrollConfig() {
      return configRoot().toPath().resolve("卷轴属性.json");
   }

   public static Path staffConfig() {
      return configRoot().toPath().resolve("灵杖属性.json");
   }

   public static Path puTuanConfig() {
      return configRoot().toPath().resolve("蒲团配置.json");
   }

   public static Path attributePointLimits() {
      return configRoot().toPath().resolve("属性加点限制.json");
   }
}
