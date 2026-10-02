package tech.komutech.objects.slimefun;

import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.LoopIterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;

public class AsyncChanceRecipeTask implements Runnable {
   private static final int UPDATE_INTERVAL = 15;
   private final Map<Integer, LoopIterator<ItemStack>> iterators = new HashMap<>();
   private final ReadWriteLock lock = new ReentrantReadWriteLock();
   private Inventory inventory;
   private int id;

   public void start(@Nonnull Inventory var1) {
      Validate.notNull(var1, "Inventory must not be null");
      this.inventory = var1;
      this.id = Bukkit.getScheduler().runTaskTimerAsynchronously(KT.plugin(), this, 0L, 14L).getTaskId();
   }

   public void add(int var1, @Nonnull List<ItemStack> var2) {
      Validate.notNull(var2, "Cannot add a null list of ItemStacks");
      this.lock.writeLock().lock();

      try {
         this.iterators.put(var1, new LoopIterator(var2));
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public boolean isEmpty() {
      this.lock.readLock().lock();

      boolean var1;
      try {
         var1 = this.iterators.isEmpty();
      } finally {
         this.lock.readLock().unlock();
      }

      return var1;
   }

   public void clear() {
      this.lock.writeLock().lock();

      try {
         this.iterators.clear();
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   @Override
   public void run() {
      if (this.inventory.getViewers().isEmpty()) {
         Bukkit.getScheduler().cancelTask(this.id);
      } else {
         this.lock.readLock().lock();

         try {
            for (Entry var2 : this.iterators.entrySet()) {
               this.inventory.setItem((Integer)var2.getKey(), (ItemStack)((LoopIterator)var2.getValue()).next());
            }
         } finally {
            this.lock.readLock().unlock();
         }
      }
   }
}
