package tech.komutech.native_scripts.support;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public final class EntityQueries {
   private EntityQueries() {
   }

   public static Collection<LivingEntity> living(Location var0, double var1) {
      if (var0 == null) {
         return Collections.emptyList();
      } else {
         World var3 = var0.getWorld();
         return (Collection<LivingEntity>)(var3 == null ? Collections.emptyList() : living(var3, var0, var1));
      }
   }

   public static Collection<LivingEntity> living(World var0, Location var1, double var2) {
      return var0.getNearbyLivingEntities(var1, var2, var2, var2);
   }

   public static Collection<LivingEntity> living(World var0, Location var1, double var2, double var4, double var6) {
      return var0.getNearbyLivingEntities(var1, var2, var4, var6);
   }

   public static Collection<Entity> entities(Location var0, double var1) {
      if (var0 == null) {
         return Collections.emptyList();
      } else {
         World var3 = var0.getWorld();
         return (Collection<Entity>)(var3 == null ? Collections.emptyList() : entities(var3, var0, var1));
      }
   }

   public static Collection<Entity> entities(World var0, Location var1, double var2) {
      return var0.getNearbyEntities(var1, var2, var2, var2);
   }

   public static Collection<Entity> entities(Entity var0, double var1) {
      return (Collection<Entity>)(var0 == null ? Collections.emptyList() : entities(var0.getLocation(), var1));
   }

   public static Collection<Entity> entities(World var0, Location var1, double var2, double var4, double var6) {
      return var0.getNearbyEntities(var1, var2, var4, var6);
   }
}
