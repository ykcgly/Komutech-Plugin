package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.libraries.dough.chat.ChatInput;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class KomutechChatInput {
   private KomutechChatInput() {
   }

   public static void waitFor(Player var0, Consumer<String> var1) {
      Plugin var2 = KomutechSupport.plugin();
      if (var2 != null) {
         ChatInput.waitForPlayer(var2, var0, var1::accept);
      }
   }
}
