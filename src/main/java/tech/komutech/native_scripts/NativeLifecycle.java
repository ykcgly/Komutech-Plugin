package tech.komutech.native_scripts;

import java.util.Collection;
import java.util.Map;
import org.bukkit.plugin.Plugin;

public final class NativeLifecycle {
   private NativeLifecycle() {
   }

   public static void registerAll(Map<String, NativeScript> var0, Plugin var1) {
      for (NativeScript var3 : var0.values()) {
         if (var3 instanceof NativeLifecycleScript var4) {
            var4.registerLifecycle(var1);
         }
      }
   }

   public static void registerAll(Collection<NativeScript> var0, Plugin var1) {
      for (NativeScript var3 : var0) {
         if (var3 instanceof NativeLifecycleScript var4) {
            var4.registerLifecycle(var1);
         }
      }
   }
}
