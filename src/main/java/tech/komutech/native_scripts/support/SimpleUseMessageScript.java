package tech.komutech.native_scripts.support;

import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class SimpleUseMessageScript implements NativeScript {
   private final String message;

   public SimpleUseMessageScript(String var1) {
      this.message = var1;
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(var1x -> {
         KomutechSupport.send(var1x.player(), this.message);
         return null;
      }).orElse(null);
   }
}
