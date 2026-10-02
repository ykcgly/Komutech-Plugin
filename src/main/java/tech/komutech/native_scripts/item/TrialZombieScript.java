package tech.komutech.native_scripts.item;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.ChatInputService;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class TrialZombieScript implements NativeScript {
   private static final Pattern ARMOR = Pattern.compile("保护(\\d+)");
   private static final Pattern HEALTH = Pattern.compile("(\\d+)\\s*血");
   private static final Map<UUID, Integer> MODES = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      if (var2.isSneaking()) {
         int var5 = MODES.getOrDefault(var2.getUniqueId(), 0);
         ChatInputService.request(var2, var5 == 0 ? "§e请输入护甲等级(-1~255):" : "§e请输入血量:", var4x -> this.handleInput(var2, var3, var5, var4x));
         return null;
      } else {
         TrialZombieScript.Settings var4 = readSettings(var3.getItemMeta());
         spawnZombie(var2, var4);
         return null;
      }
   }

   private void handleInput(Player var1, ItemStack var2, int var3, String var4) {
      MODES.remove(var1.getUniqueId());

      try {
         if (var3 == 0) {
            int var5 = Integer.parseInt(var4.trim());
            updateLore(var2, "§b§l已设置护甲 :", formatArmor(var5));
            MODES.put(var1.getUniqueId(), 1);
            ChatInputService.request(var1, "§e请输入血量:", var3x -> this.handleInput(var1, var2, 1, var3x));
         } else {
            int var7 = Integer.parseInt(var4.trim());
            updateLore(var2, "§b§l已设置血量 :", "§a§l" + var7 + " 血");
            KomutechSupport.send(var1, "§a设置完成");
         }
      } catch (NumberFormatException var6) {
         KomutechSupport.send(var1, "§c无效数字");
      }
   }

   private static void spawnZombie(Player var0, TrialZombieScript.Settings var1) {
      Location var2 = var0.getLocation().add(var0.getLocation().getDirection().multiply(2)).add(0.0, 1.0, 0.0);
      Zombie var3 = (Zombie)var0.getWorld().spawnEntity(var2, EntityType.ZOMBIE);
      var3.setAdult();
      var3.setMaxHealth(var1.health);
      var3.setHealth(var1.health);
      var3.setAware(false);
      var3.setCanPickupItems(false);
      var3.customName(Component.text("§c测试"));
      var3.setCustomNameVisible(true);
      EntityEquipment var4 = var3.getEquipment();
      if (var4 != null) {
         var4.setHelmetDropChance(0.0F);
         var4.setChestplateDropChance(0.0F);
         var4.setLeggingsDropChance(0.0F);
         var4.setBootsDropChance(0.0F);
         if (var1.armorLevel >= 0) {
            Material[] var5 = new Material[]{Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
            ItemStack[] var6 = new ItemStack[var5.length];

            for (int var7 = 0; var7 < var5.length; var7++) {
               var6[var7] = new ItemStack(var5[var7]);
               if (var1.armorLevel > 0) {
                  ItemMeta var8 = var6[var7].getItemMeta();
                  if (var8 != null) {
                     var8.addEnchant(Enchantment.PROTECTION, Math.min(var1.armorLevel, 255), true);
                     var6[var7].setItemMeta(var8);
                  }
               }
            }

            var4.setHelmet(var6[0]);
            var4.setChestplate(var6[1]);
            var4.setLeggings(var6[2]);
            var4.setBoots(var6[3]);
         }
      }

      KomutechSupport.send(var0, "§a试炼僵尸已生成");
   }

   private static TrialZombieScript.Settings readSettings(ItemMeta var0) {
      int var1 = 0;
      double var2 = 1024.0;
      if (var0 != null && var0.hasLore()) {
         for (String var5 : var0.getLore()) {
            if (var5 != null) {
               if (var5.startsWith("§b§l已设置护甲 :")) {
                  if (var5.contains("无护甲")) {
                     var1 = -1;
                  } else if (var5.contains("合金甲无保护")) {
                     var1 = 0;
                  } else {
                     Matcher var6 = ARMOR.matcher(var5);
                     if (var6.find()) {
                        var1 = Integer.parseInt(var6.group(1));
                     }
                  }
               } else if (var5.startsWith("§b§l已设置血量 :")) {
                  Matcher var7 = HEALTH.matcher(var5);
                  if (var7.find()) {
                     var2 = Double.parseDouble(var7.group(1));
                  }
               }
            }
         }
      }

      return new TrialZombieScript.Settings(var1, var2);
   }

   private static void updateLore(ItemStack var0, String var1, String var2) {
      ItemMeta var3 = var0.getItemMeta();
      if (var3 != null) {
         ArrayList var4 = var3.hasLore() ? new ArrayList(var3.getLore()) : new ArrayList();
         String var5 = var1 + " " + var2;
         boolean var6 = false;

         for (int var7 = 0; var7 < var4.size(); var7++) {
            if (((String)var4.get(var7)).startsWith(var1)) {
               var4.set(var7, var5);
               var6 = true;
               break;
            }
         }

         if (!var6) {
            var4.add(var5);
         }

         var3.setLore(var4);
         var0.setItemMeta(var3);
      }
   }

   private static String formatArmor(int var0) {
      if (var0 < 0) {
         return "§a§l无护甲";
      } else {
         return var0 == 0 ? "§a§l合金甲无保护" : "§a§l保护" + var0 + "合金甲";
      }
   }

   private record Settings(int armorLevel, double health) {
   }
}
