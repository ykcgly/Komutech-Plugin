package tech.komutech.native_scripts.support;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownMap {
   private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
   private final long cooldownMs;
   private final long ttlMs;

   public CooldownMap(long var1) {
      this(var1, 5000L);
   }

   public CooldownMap(long var1, long var3) {
      this.cooldownMs = var1;
      this.ttlMs = var3;
   }

   public boolean isOnCooldown(UUID var1) {
      Long var2 = this.lastUse.get(var1);
      return var2 != null && System.currentTimeMillis() - var2 < this.cooldownMs;
   }

   public void mark(UUID var1) {
      this.lastUse.put(var1, System.currentTimeMillis());
      this.maybeCleanup();
   }

   public boolean tryAcquire(UUID var1) {
      long var2 = System.currentTimeMillis();
      Long var4 = this.lastUse.get(var1);
      if (var4 != null && var2 - var4 < this.cooldownMs) {
         return false;
      } else {
         this.lastUse.put(var1, var2);
         this.maybeCleanup();
         return true;
      }
   }

   public long remainingSeconds(UUID var1) {
      Long var2 = this.lastUse.get(var1);
      if (var2 == null) {
         return 0L;
      } else {
         long var3 = this.cooldownMs - (System.currentTimeMillis() - var2);
         return var3 > 0L ? (var3 + 999L) / 1000L : 0L;
      }
   }

   private void maybeCleanup() {
      if (this.lastUse.size() >= 96) {
         long var1 = System.currentTimeMillis();
         Iterator var3 = this.lastUse.entrySet().iterator();

         while (var3.hasNext()) {
            Entry var4 = (Entry)var3.next();
            if (var1 - (Long)var4.getValue() > this.ttlMs) {
               var3.remove();
            }
         }
      }
   }
}
