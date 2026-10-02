package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

public enum PrefabStructureType {
   JIAN_LING_SHI("KOMUTECH_L_GL_预制鉴灵石"),
   ULTIMATE_SYNTH_CORE("KOMUTECH_L_GL_预制终极合成台核心"),
   MB_CRAFTSMAN("KOMUTECH_L_GL_预制工匠台"),
   MB_SPIRIT_MINE("KOMUTECH_L_GL_预制灵矿提取台"),
   MB_SPIRIT_INFUSE("KOMUTECH_L_GL_预制注灵台");

   private final String itemId;

   private PrefabStructureType(String nullxx) {
      this.itemId = nullxx;
   }

   public String itemId() {
      return this.itemId;
   }

   public static PrefabStructureType fromItem(SlimefunItem var0) {
      if (var0 == null) {
         return null;
      } else {
         for (PrefabStructureType var4 : values()) {
            if (var4.itemId.equals(var0.getId())) {
               return var4;
            }
         }

         return null;
      }
   }
}
