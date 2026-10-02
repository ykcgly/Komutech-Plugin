package tech.komutech.native_scripts.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.StructureProjectionHelper;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class UltimateSynthProjection {
   private static final NamespacedKey PROJECTION_KEY = new NamespacedKey("KomutechNative".toLowerCase(), "zjs_projection");
   private static final String PROJECTION_STATE_KEY = "KOMUTECH_L_ZJ_ZJHC_ty";
   private static final Map<String, List<Display>> PROJECTIONS = new ConcurrentHashMap<>();

   private UltimateSynthProjection() {
   }

   public static boolean hasProjection(Location var0) {
      if (isProjectionEnabled(var0) && !hasLiveDisplays(var0)) {
         setProjectionState(var0, false);
         return false;
      } else {
         return hasLiveDisplays(var0);
      }
   }

   public static void toggleProjection(Location var0, int var1) {
      if (hasProjection(var0)) {
         removeProjection(var0);
      } else {
         spawnProjection(var0, var1);
      }
   }

   public static void spawnProjection(Location var0, int var1) {
      removeProjection(var0);
      ArrayList var2 = new ArrayList();
      String var3 = UltimateSynthSupport.locationKey(var0);

      for (UltimateSynthSupport.StructureBlock var7 : UltimateSynthSupport.coreStructure()) {
         UltimateSynthSupport.Rotated var8 = UltimateSynthSupport.rot(var7.x(), var7.z(), var1);
         Location var9 = var0.clone().add(var8.x(), var7.y(), var8.z()).add(0.5, 0.5, 0.5);
         StructureProjectionHelper.SpawnedProjection var10 = StructureProjectionHelper.spawn(var9, var7.material(), var7.sfId(), null);
         if (var10 != null) {
            tagDisplay(var10.body(), var3);
            var2.add(var10.body());
            if (var10.label() != null) {
               tagDisplay(var10.label(), var3);
               var2.add(var10.label());
            }
         }
      }

      if (!var2.isEmpty()) {
         PROJECTIONS.put(var3, var2);
         setProjectionState(var0, true);
      }
   }

   public static void removeProjection(Location var0) {
      if (var0 != null) {
         String var1 = UltimateSynthSupport.locationKey(var0);
         List<Display> var2 = PROJECTIONS.remove(var1);
         if (var2 != null) {
            for (Display var4 : var2) {
               if (!var4.isDead()) {
                  var4.remove();
               }
            }
         }

         removeTaggedEntities(var0);
         setProjectionState(var0, false);
      }
   }

   public static void clearForCoreBreak(Location var0) {
      removeProjection(var0);
   }

   private static boolean hasLiveDisplays(Location var0) {
      String var1 = UltimateSynthSupport.locationKey(var0);
      List<Display> var2 = PROJECTIONS.get(var1);
      if (var2 != null && !var2.isEmpty()) {
         var2.removeIf(Entity::isDead);
         if (var2.isEmpty()) {
            PROJECTIONS.remove(var1);
            return scanTaggedEntities(var0);
         } else {
            return true;
         }
      } else {
         return scanTaggedEntities(var0);
      }
   }

   private static boolean scanTaggedEntities(Location var0) {
      String var1 = UltimateSynthSupport.locationKey(var0);

      for (Entity var3 : EntityQueries.entities(var0.getWorld(), var0, 16.0)) {
         if (var3 instanceof Display && !var3.isDead()) {
            String var4 = (String)var3.getPersistentDataContainer().get(PROJECTION_KEY, PersistentDataType.STRING);
            if (var1.equals(var4)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isProjectionEnabled(Location var0) {
      return "1".equals(MachineScriptHelper.getData(var0, "KOMUTECH_L_ZJ_ZJHC_ty"));
   }

   private static void removeTaggedEntities(Location var0) {
      String var1 = UltimateSynthSupport.locationKey(var0);

      for (Entity var3 : EntityQueries.entities(var0.getWorld(), var0, 16.0)) {
         if (var3 instanceof Display) {
            String var4 = (String)var3.getPersistentDataContainer().get(PROJECTION_KEY, PersistentDataType.STRING);
            if (var1.equals(var4) && !var3.isDead()) {
               var3.remove();
            }
         }
      }
   }

   private static void tagDisplay(Display var0, String var1) {
      var0.getPersistentDataContainer().set(PROJECTION_KEY, PersistentDataType.STRING, var1);
   }

   private static void setProjectionState(Location var0, boolean var1) {
      MachineScriptHelper.setData(var0, "KOMUTECH_L_ZJ_ZJHC_ty", var1 ? "1" : "0");
   }
}
