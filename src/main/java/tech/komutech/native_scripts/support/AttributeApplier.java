package tech.komutech.native_scripts.support;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class AttributeApplier {
   public static final double MAX_REACH_BONUS = 5.0;
   private static final String CRANE_ATTRIBUTE_PLUGIN = "CraneAttribute";
   private static volatile Boolean craneAttributePresent;
   private static final Map<String, AttributeApplier.AttrMeta> META = Map.of(
      "血量",
      new AttributeApplier.AttrMeta("10110305-1003-1204-0823-141408280000", List.of("GENERIC_MAX_HEALTH", "MAX_HEALTH"), false),
      "攻击力",
      new AttributeApplier.AttrMeta("10110305-1003-1204-0823-092412040000", List.of("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE"), false),
      "防御力",
      new AttributeApplier.AttrMeta("10110305-1003-1204-0823-042100060000", List.of("GENERIC_ARMOR", "ARMOR"), false),
      "速度",
      new AttributeApplier.AttrMeta("10110305-1003-1204-0823-210505050000", List.of("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED"), true),
      "灵识",
      new AttributeApplier.AttrMeta(
         null,
         List.of("BLOCK_INTERACTION_RANGE", "ENTITY_INTERACTION_RANGE"),
         false,
         List.of("10110305-1003-1204-0823-082317040001", "10110305-1003-1204-0823-082317040002")
      )
   );

   private AttributeApplier() {
   }

   public static void registerCompatListeners(final Plugin var0) {
      Bukkit.getPluginManager().registerEvents(new Listener() {
         @EventHandler
         public void onJoin(PlayerJoinEvent var1) {
            Bukkit.getScheduler().runTaskLater(var0, () -> AttributeApplier.applyStoredAttributes(var1.getPlayer()), 1L);
         }
      }, var0);
   }

   public static void applyStoredAttributes(Player var0) {
      if (var0 != null && var0.isOnline()) {
         Map var1 = PlayerAttributeStore.load(var0.getName());
         if (var1 != null) {
            applyPlayerAttributes(var0, var1);
            PlayerAttributeStore.save(var0.getName(), var1);
         }
      }
   }

   public static void reapplyAllOnline() {
      for (Player var1 : Bukkit.getOnlinePlayers()) {
         applyStoredAttributes(var1);
      }
   }

   public static boolean isCraneAttributePresent() {
      if (craneAttributePresent == null) {
         Plugin var0 = Bukkit.getPluginManager().getPlugin("CraneAttribute");
         craneAttributePresent = var0 != null && var0.isEnabled();
      }

      return craneAttributePresent;
   }

   private static boolean shouldApplyBukkitModifiers() {
      return true;
   }

   public static void clearSwitchable(Player var0) {
      for (String var2 : CultivationMath.SWITCHABLE_ATTRS) {
         removeModifier(var0, var2);
      }
   }

   public static void applyPlayerAttributes(Player var0, Map<String, Object> var1) {
      applyPlayerAttributes(var0, var1, true);
   }

   public static void applyPlayerAttributes(Player var0, Map<String, Object> var1, boolean var2) {
      List var3 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var1, "灵根", ""));
      double var4 = PlayerAttributeStore.getDouble(var1, "总品质", 0.01);
      double var6 = CultivationMath.getRealmCoefficient(var1);
      double var8 = PlayerAttributeStore.getDouble(var1, "根骨", 1.0);

      for (String var11 : CultivationMath.SWITCHABLE_ATTRS) {
         double var12 = CultivationMath.getLinggenFinalCoefficient(var3, var4, var11);
         double var14 = PlayerAttributeStore.getDouble(var1, var11, 0.0);
         double var16 = var14 == 0.0
            ? 0.0
            : CultivationMath.computeActualValue(var14, CultivationMath.baseGrowth.getOrDefault(var11, 0.0), var12, var6, var4, var8);
         var16 = AttributePointLimits.clampForApply(var1, var11, var16, var2);
         var1.put(var11 + "_实际", var16);
         applyAttribute(var0, var11, var16, PlayerAttributeStore.getBoolean(var1, var11 + "_启用", true));
      }

      int var18 = PlayerAttributeStore.getInt(var1, "灵气获取", 0);
      var1.put("灵气获取_实际", AttributePointLimits.spiritGainPercent(var1, var18));
   }

   public static void applyAttribute(Player var0, String var1, double var2, boolean var4) {
      if (!shouldApplyBukkitModifiers()) {
         removeModifier(var0, var1);
      } else if (!var4) {
         removeModifier(var0, var1);
      } else {
         AttributeApplier.AttrMeta var5 = META.get(var1);
         if (var5 != null) {
            if ("灵识".equals(var1)) {
               double var11 = Math.min(var2, 5.0);

               for (int var12 = 0; var12 < var5.names.size(); var12++) {
                  AttributeInstance var9 = resolveAttribute(var0, var5.names.get(var12));
                  if (var9 != null && var5.uuids != null) {
                     UUID var13 = UUID.fromString(var5.uuids.get(var12));
                     removeByUuid(var9, var13);
                     if (var11 > 0.0) {
                        var9.addModifier(new AttributeModifier(var13, var5.uuids.get(var12), var11, Operation.ADD_NUMBER));
                     }
                  }
               }
            } else {
               AttributeInstance var6 = resolveAttribute(var0, var5.names);
               if (var6 != null && var5.uuid != null) {
                  UUID var7 = UUID.fromString(var5.uuid);
                  removeByUuid(var6, var7);
                  double var8 = var2;
                  Operation var10 = Operation.ADD_NUMBER;
                  if (var5.multiply) {
                     var10 = Operation.MULTIPLY_SCALAR_1;
                     var8 = Math.min(var2, 200.0) / 100.0;
                  }

                  if (var8 != 0.0) {
                     var6.addModifier(new AttributeModifier(var7, var5.uuid, var8, var10));
                  }
               }
            }
         }
      }
   }

   public static void removeModifier(Player var0, String var1) {
      AttributeApplier.AttrMeta var2 = META.get(var1);
      if (var2 != null) {
         if ("灵识".equals(var1) && var2.uuids != null) {
            for (int var5 = 0; var5 < var2.names.size(); var5++) {
               AttributeInstance var4 = resolveAttribute(var0, var2.names.get(var5));
               if (var4 != null) {
                  removeByUuid(var4, UUID.fromString(var2.uuids.get(var5)));
               }
            }
         } else {
            AttributeInstance var3 = resolveAttribute(var0, var2.names);
            if (var3 != null && var2.uuid != null) {
               removeByUuid(var3, UUID.fromString(var2.uuid));
            }
         }
      }
   }

   private static void removeByUuid(AttributeInstance var0, UUID var1) {
      AttributeModifier var2 = var0.getModifier(var1);
      if (var2 != null) {
         var0.removeModifier(var2);
      }
   }

   private static AttributeInstance resolveAttribute(Player var0, List<String> var1) {
      for (String var3 : var1) {
         AttributeInstance var4 = resolveAttribute(var0, var3);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   private static AttributeInstance resolveAttribute(Player var0, String var1) {
      try {
         Attribute var2 = Attribute.valueOf(var1);
         return var0.getAttribute(var2);
      } catch (IllegalArgumentException var3) {
         return null;
      }
   }

   private record AttrMeta(String uuid, List<String> names, boolean multiply, List<String> uuids) {
      AttrMeta(String var1, List<String> var2, boolean var3) {
         this(var1, var2, var3, null);
      }
   }
}
