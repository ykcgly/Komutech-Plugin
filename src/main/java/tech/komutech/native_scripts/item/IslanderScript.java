package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class IslanderScript implements NativeScript {
   private static final Map<String, ItemStack> ITEM_CACHE = new HashMap<>();
   private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
   private static final String[] CACHE_IDS = new String[]{
      "KOMUTECH_L_SW_XDRHH", "KOMUTECH_L_DJ_功德券", "KOMUTECH_L_J_布衣", "KOMUTECH_L_J_布裤", "KOMUTECH_L_J_布鞋", "KOMUTECH_L_W_玄铁剑", "KOMUTECH_L_W_玄铁弓"
   };

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(Boolean.FALSE);
   }

   private Boolean handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      long var3 = System.currentTimeMillis();
      Long var5 = COOLDOWNS.get(var2.getUniqueId());
      if (var5 != null && var3 - var5 < 1000L) {
         KomutechSupport.actionBar(var2, "§c请不要连续召唤");
         return Boolean.FALSE;
      } else {
         COOLDOWNS.put(var2.getUniqueId(), var3);
         Location var6 = var2.getEyeLocation().add(var2.getEyeLocation().getDirection().multiply(3));
         Material var7 = var6.getBlock().getType();
         if (!var7.isSolid() && var7 != Material.WATER && var7 != Material.LAVA) {
            boolean var8 = Math.random() < 0.5;
            LivingEntity var9 = var8
               ? (LivingEntity)var2.getWorld().spawnEntity(var6, EntityType.SKELETON)
               : (LivingEntity)var2.getWorld().spawnEntity(var6, EntityType.ZOMBIE);
            var9.customName(Component.text("§c§l小岛人"));
            var9.setCustomNameVisible(true);
            if (var9.getAttribute(Attribute.MAX_HEALTH) != null) {
               var9.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100.0);
            }

            var9.setHealth(100.0);
            var9.setRemoveWhenFarAway(false);
            var9.setCanPickupItems(false);
            equip(var9, var8);
            applyEffects(var9);
            UseEvents.consumeOne(var1);
            var2.updateInventory();
            KomutechSupport.send(var2, "§a小岛人（" + (var8 ? "骷髅" : "僵尸") + "）生成完成");
            var2.playSound(var2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            return Boolean.TRUE;
         } else {
            KomutechSupport.send(var2, "§c目标位置无法生成生物！");
            return Boolean.FALSE;
         }
      }
   }

   private static void equip(LivingEntity var0, boolean var1) {
      ensureItemsCached();
      EntityEquipment var2 = var0.getEquipment();
      if (var2 != null) {
         setPiece(var2, EquipmentSlot.HEAD, "KOMUTECH_L_SW_XDRHH", 0.95F);
         setPiece(var2, EquipmentSlot.CHEST, "KOMUTECH_L_J_布衣", 0.0F);
         setPiece(var2, EquipmentSlot.LEGS, "KOMUTECH_L_J_布裤", 0.0F);
         setPiece(var2, EquipmentSlot.FEET, "KOMUTECH_L_J_布鞋", 0.0F);
         setPiece(var2, EquipmentSlot.OFF_HAND, "KOMUTECH_L_DJ_功德券", 0.75F);
         String var3 = var1 ? "KOMUTECH_L_W_玄铁弓" : "KOMUTECH_L_W_玄铁剑";
         ItemStack var4 = ITEM_CACHE.get(var3);
         if (var4 != null) {
            var2.setItemInMainHand(var4.clone());
            var2.setItemInMainHandDropChance(0.0F);
         }

         if (var0 instanceof Skeleton var5) {
            var5.setCanPickupItems(false);
         } else if (var0 instanceof Zombie var6) {
            var6.setCanPickupItems(false);
         }
      }
   }

   private static void setPiece(EntityEquipment var0, EquipmentSlot var1, String var2, float var3) {
      ItemStack var4 = ITEM_CACHE.get(var2);
      if (var4 != null) {
         switch (var1) {
            case HEAD:
               var0.setHelmet(var4.clone());
               var0.setHelmetDropChance(var3);
               break;
            case CHEST:
               var0.setChestplate(var4.clone());
               var0.setChestplateDropChance(var3);
               break;
            case LEGS:
               var0.setLeggings(var4.clone());
               var0.setLeggingsDropChance(var3);
               break;
            case FEET:
               var0.setBoots(var4.clone());
               var0.setBootsDropChance(var3);
               break;
            case OFF_HAND:
               var0.setItemInOffHand(var4.clone());
               var0.setItemInOffHandDropChance(var3);
         }
      }
   }

   private static void applyEffects(LivingEntity var0) {
      PotionEffectType var1 = PotionEffectType.getByName("RESISTANCE");
      if (var1 == null) {
         var1 = PotionEffectType.getByName("DAMAGE_RESISTANCE");
      }

      PotionEffectType var2 = PotionEffectType.getByName("SPEED");
      int var3 = 19999980;
      if (var1 != null) {
         var0.addPotionEffect(new PotionEffect(var1, var3, 1, true, true));
      }

      if (var2 != null) {
         var0.addPotionEffect(new PotionEffect(var2, var3, 3, true, true));
      }
   }

   private static void ensureItemsCached() {
      for (String var3 : CACHE_IDS) {
         cache(var3);
      }
   }

   private static void cache(String var0) {
      if (!ITEM_CACHE.containsKey(var0)) {
         SlimefunItem var1 = SlimefunItem.getById(var0);
         if (var1 != null) {
            ITEM_CACHE.put(var0, var1.getItem());
         }
      }
   }
}
