package tech.komutech.native_scripts.item;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class FireworkLauncherScript implements NativeScript {
   private static final long COOLDOWN_MS = 3000L;
   private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
   private static final Type[] SHAPES = new Type[]{Type.BALL, Type.BALL_LARGE, Type.STAR, Type.BURST, Type.CREEPER};
   private static final Color[] COLORS = new Color[]{
      Color.RED,
      Color.BLUE,
      Color.GREEN,
      Color.YELLOW,
      Color.PURPLE,
      Color.ORANGE,
      Color.fromRGB(255, 192, 203),
      Color.AQUA,
      Color.LIME,
      Color.FUCHSIA,
      Color.WHITE
   };

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         return UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
      } else {
         if ("onDisable".equals(var1)) {
            COOLDOWNS.clear();
         }

         return null;
      }
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      String var4 = detectType(var3);
      if (var4 == null) {
         KomutechSupport.send(var2, "§c请使用正确的烟花发射器！");
         return null;
      } else {
         long var5 = System.currentTimeMillis();
         Long var7 = COOLDOWNS.get(var2.getUniqueId());
         if (var7 != null && var5 - var7 < 3000L) {
            double var11 = (3000L - (var5 - var7)) / 1000.0;
            KomutechSupport.send(var2, "§e请等待 " + String.format("%.1f", var11) + " 秒后再发射烟花！");
            return null;
         } else {
            COOLDOWNS.put(var2.getUniqueId(), var5);

            int var8 = switch (var4) {
               case "压缩节日烟花" -> ThreadLocalRandom.current().nextInt(24, 33);
               case "节日烟花" -> 1;
               default -> 1;
            };
            launch(var2, var8);
            if (!"烟花发射器".equals(var4)) {
               UseEvents.consumeOne(var1);
               var2.updateInventory();
            }

            KomutechSupport.send(var2, "\ud83c\udf86 §a烟花发射成功！");
            return null;
         }
      }
   }

   private static String detectType(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 != null && var1.hasLore()) {
         String var2 = String.join(" ", var1.getLore());
         if (var2.contains("压缩节日烟花")) {
            return "压缩节日烟花";
         } else if (var2.contains("烟花发射器")) {
            return "烟花发射器";
         } else {
            return var2.contains("节日烟花") ? "节日烟花" : null;
         }
      } else {
         return null;
      }
   }

   private static void launch(Player var0, int var1) {
      Location var2 = var0.getLocation().clone().add(0.0, 1.6, 0.0);
      ThreadLocalRandom var3 = ThreadLocalRandom.current();

      for (int var4 = 0; var4 < var1; var4++) {
         double var5 = var3.nextDouble(9.6, 32.0);
         double var7 = var3.nextDouble(Math.PI * 2);
         Location var9 = var2.clone().add(Math.cos(var7) * var5, 5.0 + var3.nextDouble(1.6, 1.8), Math.sin(var7) * var5);
         Firework var10 = (Firework)var0.getWorld().spawn(var9, Firework.class);
         FireworkMeta var11 = var10.getFireworkMeta();
         ArrayList var12 = new ArrayList();
         int var13 = var3.nextInt(1, 6);

         for (int var14 = 0; var14 < var13; var14++) {
            var12.add(COLORS[var3.nextInt(COLORS.length)]);
         }

         var11.addEffect(
            FireworkEffect.builder().with(SHAPES[var3.nextInt(SHAPES.length)]).withColor(var12).flicker(var3.nextBoolean()).trail(var3.nextBoolean()).build()
         );
         var11.setPower(1);
         var10.setFireworkMeta(var11);
         var10.setVelocity(new Vector(0.0, var3.nextDouble(0.2, 0.22), 0.0));
      }
   }
}
