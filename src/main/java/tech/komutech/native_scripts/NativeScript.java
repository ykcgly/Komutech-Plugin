package tech.komutech.native_scripts;

import org.bukkit.plugin.Plugin;

public interface NativeScript {
   default void registerListeners(Plugin var1) {
   }

   Object invoke(String var1, Object... var2);
}
