package tech.komutech.native_scripts.support;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

public final class KomutechAsyncScheduler {
   private static final long TARGET_PERIOD_MS = 50L;
   private static final long MIN_PERIOD_MS = 10L;
   private static final long MAX_PERIOD_MS = 500L;
   private static final long TASK_TIMEOUT_MS = 100L;
   private static KomutechAsyncScheduler instance;
   private final ConcurrentLinkedQueue<KomutechAsyncScheduler.QueuedTask> queue = new ConcurrentLinkedQueue<>();
   private ScheduledExecutorService executor;
   private Plugin plugin;
   private volatile boolean running;
   private volatile long currentPeriodMs = 50L;

   public static void start(Plugin var0) {
      if (instance == null) {
         instance = new KomutechAsyncScheduler();
      }

      instance.ensureRunning(var0);
   }

   public static void shutdown() {
      if (instance != null) {
         instance.running = false;
         if (instance.executor != null) {
            instance.executor.shutdownNow();
            instance.executor = null;
         }

         instance.queue.clear();
      }
   }

   public static void submit(Runnable var0, Consumer<Plugin> var1) {
      if (instance != null && var0 != null) {
         instance.queue.offer(new KomutechAsyncScheduler.QueuedTask(var0, var1 == null ? var0x -> {} : var1));
      }
   }

   private void ensureRunning(Plugin var1) {
      this.plugin = var1;
      if (!this.running) {
         this.running = true;
         this.executor = Executors.newSingleThreadScheduledExecutor(var0 -> {
            Thread var1x = new Thread(var0, "Komutech-AsyncScheduler");
            var1x.setDaemon(true);
            return var1x;
         });
         this.scheduleNext(0L);
      }
   }

   private void scheduleNext(long var1) {
      if (this.running && this.executor != null && !this.executor.isShutdown()) {
         this.executor.schedule(this::processBatch, var1, TimeUnit.MILLISECONDS);
      }
   }

   private void processBatch() {
      if (this.running) {
         long var1 = System.nanoTime();

         KomutechAsyncScheduler.QueuedTask var3;
         while ((var3 = this.queue.poll()) != null) {
            KomutechAsyncScheduler.QueuedTask var4 = var3;
            long var5 = System.nanoTime();

            try {
               var4.asyncWork().run();
               Plugin var7 = this.plugin;
               if (var7 != null && var7.isEnabled()) {
                  MainThread.run(var7, () -> var4.syncApply().accept(var7));
               }
            } catch (Exception var9) {
            }

            long var11 = (System.nanoTime() - var5) / 1000000L;
            if (var11 > 100L) {
               this.currentPeriodMs = Math.min(500L, Math.max(10L, var11 / 2L));
            }
         }

         long var10 = (System.nanoTime() - var1) / 1000000L;
         long var6;
         if (this.queue.isEmpty()) {
            var6 = Math.min(500L, this.currentPeriodMs * 2L);
         } else {
            var6 = Math.max(10L, 50L - var10);
            this.currentPeriodMs = 50L;
         }

         this.scheduleNext(var6);
      }
   }

   private record QueuedTask(Runnable asyncWork, Consumer<Plugin> syncApply) {
   }
}
