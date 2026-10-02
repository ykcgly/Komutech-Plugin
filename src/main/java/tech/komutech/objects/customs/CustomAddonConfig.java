package tech.komutech.objects.customs;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import tech.komutech.script.ScriptEval;

public record CustomAddonConfig(File configFile, YamlConfiguration config, ScriptEval onReloadHandler) {
   public void tryReload() {
      try {
         this.config.load(this.configFile);
         if (this.onReloadHandler != null) {
            this.onReloadHandler.evalFunction("onConfigReload", this.config);
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }
}
