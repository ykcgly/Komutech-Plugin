package tech.komutech.native_scripts.tool;

import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class CopyCardStubScript implements NativeScript {
   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(var0 -> {
         KomutechSupport.send(var0.player(), "大胆竟敢想作弊");
         return null;
      }).orElse(null);
   }
}
