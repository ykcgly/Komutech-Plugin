package tech.komutech.native_scripts.support;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PlayerEffects {
   private PlayerEffects() {
   }

   public static PotionEffectType potionType(String var0) {
      PotionEffectType var1 = PotionEffectType.getByName(var0);
      if (var1 == null) {
         var1 = switch (var0) {
            case "FAST_DIGGING" -> PotionEffectType.HASTE;
            case "JUMP" -> PotionEffectType.JUMP_BOOST;
            case "INCREASE_DAMAGE" -> PotionEffectType.STRENGTH;
            case "DAMAGE_RESISTANCE" -> PotionEffectType.RESISTANCE;
            case "SLOW" -> PotionEffectType.SLOWNESS;
            default -> null;
         };
      }

      return var1;
   }

   public static void addEffect(Player var0, String var1, int var2, int var3) {
      PotionEffectType var4 = potionType(var1);
      if (var4 != null) {
         addEffect(var0, var4, var2, var3);
      }
   }

   public static void addEffect(Player var0, PotionEffectType var1, int var2, int var3) {
      if (var1 != null) {
         var0.addPotionEffect(new PotionEffect(var1, var2, var3, true, true, true));
      }
   }

   public static void clearAllEffects(Player var0) {
      for (PotionEffect var2 : var0.getActivePotionEffects()) {
         var0.removePotionEffect(var2.getType());
      }
   }

   public static void feed(Player var0, int var1) {
      var0.setFoodLevel(Math.min(20, var0.getFoodLevel() + var1));
      var0.setSaturation(var0.getSaturation() + var1);
      var0.getWorld().playSound(var0.getLocation(), "entity.generic.eat", 1.0F, 1.0F);
      var0.getWorld().playSound(var0.getLocation(), "entity.player.burp", 1.0F, 1.0F);
   }

   public static void heal(Player var0, double var1) {
      if (!(var1 <= 0.0)) {
         double var3 = var0.getMaxHealth();
         var0.setHealth(Math.min(var3, var0.getHealth() + var1));
      }
   }
}
