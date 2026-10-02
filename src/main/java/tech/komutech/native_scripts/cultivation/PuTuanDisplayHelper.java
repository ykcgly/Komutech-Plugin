package tech.komutech.native_scripts.cultivation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.support.DisplayReflectionHelper;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.MachineScriptHelper;

final class PuTuanDisplayHelper {
   private static final NamespacedKey MEDITATION_TAG = new NamespacedKey("KomutechNative".toLowerCase(), "pu_tuan_display");

   private PuTuanDisplayHelper() {
   }

   static NamespacedKey tagKey() {
      return MEDITATION_TAG;
   }

   static boolean isProtected(Entity var0) {
      return var0 instanceof Display var1 ? var1.getPersistentDataContainer().has(MEDITATION_TAG, PersistentDataType.STRING) : false;
   }

   static void tag(Display var0, Location var1) {
      var0.getPersistentDataContainer().set(MEDITATION_TAG, PersistentDataType.STRING, locationKey(var1));
   }

   static TextDisplay spawnStatusHologram(Location var0, String var1) {
      removeTaggedTextAt(var0, null);
      Location var2 = var0.clone().add(0.5, 1.55, 0.5);
      TextDisplay var3 = DisplayReflectionHelper.spawnTextDisplay(var2, var1, 0.7F);
      tag(var3, var0);
      return var3;
   }

   static void updateHologram(Location var0, String var1) {
      String var2 = MachineScriptHelper.getData(var0, "meditation_holo_uuid");
      if ((var2 == null ? null : findEntity(var0, var2)) instanceof TextDisplay var4 && !var4.isDead()) {
         var4.setText(var1);
         removeTaggedTextAt(var0, var4.getUniqueId());
      } else {
         TextDisplay var5 = spawnStatusHologram(var0, var1);
         MachineScriptHelper.setData(var0, "meditation_holo_uuid", var5.getUniqueId().toString());
      }
   }

   static void removeStoredOrNearby(Location var0, String var1) {
      String var2 = MachineScriptHelper.getData(var0, var1);
      Object var3 = null;
      if (var2 != null) {
         Entity var4 = findEntity(var0, var2);
         if (var4 != null && !var4.isDead()) {
            var4.remove();
         }
      }

      removeTaggedAt(var0, (UUID)var3);
   }

   static int removeTaggedAt(Location var0) {
      return removeTaggedAt(var0, null);
   }

   static int removeTaggedTextAt(Location var0, UUID var1) {
      return removeTaggedOfType(var0, var1, TextDisplay.class);
   }

   static int removeTaggedProjectionsAt(Location var0, UUID var1) {
      return removeTaggedOfType(var0, var1, BlockDisplay.class);
   }

   private static int removeTaggedOfType(Location var0, UUID var1, Class<? extends Display> var2) {
      if (var0 != null && var0.getWorld() != null) {
         String var3 = locationKey(var0);
         int var4 = 0;

         for (Entity var6 : EntityQueries.entities(var0.clone().add(0.5, 1.0, 0.5), 3.5)) {
            if (var2.isInstance(var6) && var6 instanceof Display var7 && !var7.isDead() && (var1 == null || !var1.equals(var7.getUniqueId()))) {
               String var8 = (String)var7.getPersistentDataContainer().get(MEDITATION_TAG, PersistentDataType.STRING);
               if (var3.equals(var8)) {
                  var7.remove();
                  var4++;
               }
            }
         }

         return var4;
      } else {
         return 0;
      }
   }

   static int removeTaggedAt(Location var0, UUID var1) {
      if (var0 != null && var0.getWorld() != null) {
         String var2 = locationKey(var0);
         int var3 = 0;

         for (Entity var5 : EntityQueries.entities(var0.clone().add(0.5, 1.0, 0.5), 3.5)) {
            if (var5 instanceof Display var6 && !var6.isDead() && (var1 == null || !var1.equals(var6.getUniqueId()))) {
               String var7 = (String)var6.getPersistentDataContainer().get(MEDITATION_TAG, PersistentDataType.STRING);
               if (var2.equals(var7)) {
                  var6.remove();
                  var3++;
               }
            }
         }

         return var3;
      } else {
         return 0;
      }
   }

   static int sweepChunk(Chunk var0, PuTuanDisplayHelper.OrphanDecision var1) {
      if (var0 != null && var0.isLoaded()) {
         int var2 = 0;

         for (Entity var6 : var0.getEntities()) {
            if (var6 instanceof Display var7 && !var7.isDead()) {
               String var8 = (String)var7.getPersistentDataContainer().get(MEDITATION_TAG, PersistentDataType.STRING);
               if (var8 != null && !var8.isEmpty() && var1.shouldRemove(var7, var8)) {
                  var7.remove();
                  var2++;
               }
            }
         }

         return var2;
      } else {
         return 0;
      }
   }

   static int sweepLoadedWorlds(PuTuanDisplayHelper.OrphanDecision var0) {
      int var1 = 0;

      for (World var3 : Bukkit.getWorlds()) {
         for (Chunk var7 : var3.getLoadedChunks()) {
            var1 += sweepChunk(var7, var0);
         }
      }

      return var1;
   }

   static Entity findEntity(Location var0, String var1) {
      if (var0 != null && var0.getWorld() != null && var1 != null) {
         try {
            return var0.getWorld().getEntity(UUID.fromString(var1));
         } catch (IllegalArgumentException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   static Location parseLocationKey(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String[] var1 = var0.split(",");
         if (var1.length != 4) {
            return null;
         } else {
            World var2 = Bukkit.getWorld(var1[0]);
            if (var2 == null) {
               return null;
            } else {
               try {
                  int var3 = Integer.parseInt(var1[1]);
                  int var4 = Integer.parseInt(var1[2]);
                  int var5 = Integer.parseInt(var1[3]);
                  return new Location(var2, var3, var4, var5);
               } catch (NumberFormatException var6) {
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   static String locationKey(Location var0) {
      return var0.getWorld().getName() + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   static List<Display> findTaggedNear(Location var0, double var1) {
      ArrayList var3 = new ArrayList();
      if (var0 != null && var0.getWorld() != null) {
         for (Entity var5 : EntityQueries.entities(var0, var1)) {
            if (var5 instanceof Display var6 && isProtected(var6) && !var6.isDead()) {
               var3.add(var6);
            }
         }

         return var3;
      } else {
         return var3;
      }
   }

   @FunctionalInterface
   interface OrphanDecision {
      boolean shouldRemove(Display var1, String var2);
   }
}
