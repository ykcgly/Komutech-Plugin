package tech.komutech.native_scripts;

import org.bukkit.plugin.Plugin;

public interface NativeLifecycleScript extends NativeScript {
   void registerLifecycle(Plugin var1);

   @Override
   default void registerListeners(Plugin var1) {
      this.registerLifecycle(var1);
   }
}
