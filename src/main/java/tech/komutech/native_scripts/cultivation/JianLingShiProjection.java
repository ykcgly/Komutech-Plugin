package tech.komutech.native_scripts.cultivation;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.StructureProjectionHelper;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class JianLingShiProjection {
   private static final NamespacedKey PROJECTION_KEY = new NamespacedKey("KomutechNative".toLowerCase(), "jls_projection");
   private static final String PROJECTION_STATE_KEY = "KOMUTECH_L_X_JLS_ty";
   private static final Map<String, List<Display>> PROJECTIONS = new ConcurrentHashMap<>();

   private JianLingShiProjection() {
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
      String var3 = JianLingShiSupport.locationKey(var0);

      for (JianLingShiSupport.StructureBlock var7 : JianLingShiSupport.structure()) {
         JianLingShiSupport.Rotated var8 = JianLingShiSupport.rot(var7.x(), var7.z(), var1);
         Location var9 = var0.clone().add(var8.x(), var7.y(), var8.z()).add(0.5, 0.5, 0.5);
         ItemStack var10 = null;
         if (var7.dynamicCluster()) {
            Block var11 = var0.getWorld().getBlockAt(var0.getBlockX() + var8.x(), var0.getBlockY() + var7.y(), var0.getBlockZ() + var8.z());
            SlimefunItem var12 = MachineScriptHelper.getSfItem(var11.getLocation());
            if (var12 != null) {
               var10 = var12.getItem().clone();
            } else {
               SlimefunItem var13 = SlimefunItem.getById("KOMUTECH_L_DJ_JPLJ");
               var10 = var13 != null ? var13.getItem().clone() : new ItemStack(Material.AMETHYST_CLUSTER);
            }
         }

         StructureProjectionHelper.SpawnedProjection var14 = StructureProjectionHelper.spawn(var9, var7.material(), var7.sfId(), var10);
         if (var14 != null) {
            tagDisplay(var14.body(), var3);
            var2.add(var14.body());
            if (var14.label() != null) {
               tagDisplay(var14.label(), var3);
               var2.add(var14.label());
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
         String var1 = JianLingShiSupport.locationKey(var0);
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
      String var1 = JianLingShiSupport.locationKey(var0);
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
      String var1 = JianLingShiSupport.locationKey(var0);

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
      return "1".equals(MachineScriptHelper.getData(var0, "KOMUTECH_L_X_JLS_ty"));
   }

   private static void removeTaggedEntities(Location var0) {
      String var1 = JianLingShiSupport.locationKey(var0);

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
      MachineScriptHelper.setData(var0, "KOMUTECH_L_X_JLS_ty", var1 ? "1" : "0");
   }
}
