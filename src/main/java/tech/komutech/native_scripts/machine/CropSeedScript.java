package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class CropSeedScript implements NativeScript {
   private static final Map<Location, CropSeedScript.PlantData> PLANT_DATA = new ConcurrentHashMap<>();
   private final CropSeedScript.Config config;
   private SlimefunItem productItem;

   private CropSeedScript(CropSeedScript.Config var1) {
      this.config = var1;
   }

   public static CropSeedScript cotton() {
      return new CropSeedScript(
         new CropSeedScript.Config(
            "KOMUTECH_L_LZZZ_MHZZ",
            List.of(
               new CropSeedScript.Stage(Material.SHORT_GRASS, 3000L),
               new CropSeedScript.Stage(Material.WHITE_TULIP, 5000L),
               new CropSeedScript.Stage(Material.LILY_OF_THE_VALLEY, 3000L)
            ),
            Material.FARMLAND,
            "KOMUTECH_L_LZ_MH",
            3,
            8
         )
      );
   }

   public static CropSeedScript hemp() {
      return new CropSeedScript(
         new CropSeedScript.Config(
            "KOMUTECH_L_LZZZ_MZZ",
            List.of(
               new CropSeedScript.Stage(Material.SHORT_GRASS, 3000L),
               new CropSeedScript.Stage(Material.TALL_GRASS, 5000L),
               new CropSeedScript.Stage(Material.LARGE_FERN, 3000L)
            ),
            Material.FARMLAND,
            "KOMUTECH_L_LZ_M",
            3,
            8
         )
      );
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("tick".equals(var1)) {
         MachineScriptHelper.MachineContext var4 = MachineScriptHelper.parse(var2[0]);
         if (var4 != null && this.config.machineId().equals(MachineScriptHelper.getMachineId(var4.machine()))) {
            this.tick(var4);
         }
      } else if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var3) {
         PLANT_DATA.remove(var3.getBlock().getLocation());
      } else if ("onBreak".equals(var1)) {
         this.handleBreak(var2);
      }

      return null;
   }

   private void tick(MachineScriptHelper.MachineContext var1) {
      Location var2 = var1.location();
      World var3 = var2.getWorld();
      if (var3 != null) {
         int var4 = var2.getBlockX();
         int var5 = var2.getBlockY();
         int var6 = var2.getBlockZ();
         long var7 = System.currentTimeMillis();
         CropSeedScript.PlantData var9 = PLANT_DATA.get(var2);
         if (var9 == null) {
            if (var3.getBlockAt(var4, var5 - 1, var6).getType() == this.config.requiredBlock()) {
               PLANT_DATA.put(var2, new CropSeedScript.PlantData(var7, -1, false, false));
            }
         } else if (!var9.mature) {
            long var10 = var7 - var9.startTime;
            if (!var9.checkedForMature && var10 >= this.config.totalMs() - 100L) {
               if (var3.getBlockAt(var4, var5 - 1, var6).getType() != this.config.requiredBlock()) {
                  PLANT_DATA.remove(var2);
                  return;
               }

               var9.checkedForMature = true;
            }

            if (var10 >= this.config.totalMs()) {
               var9.mature = true;
            }

            int var12 = -1;
            if (var10 >= this.config.totalMs()) {
               var12 = this.config.stages().size() - 1;
            } else {
               var12 = this.config.stageAt(var10);
            }

            if (var12 != var9.currentStage && var12 >= 0) {
               var1.block().setType(this.config.stages().get(var12).material());
               var9.currentStage = var12;
            }
         }
      }
   }

   private void handleBreak(Object[] var1) {
      try {
         Object var2 = var1[0];
         Location var3 = ((Block)var2.getClass().getMethod("getBlock").invoke(var2)).getLocation();
         CropSeedScript.PlantData var4 = PLANT_DATA.remove(var3);
         if (var4 == null || !var4.mature) {
            return;
         }

         if (this.productItem == null) {
            this.productItem = SlimefunItem.getById(this.config.productItemId());
         }

         if (!(this.productItem != null && var1.length >= 3 && var1[2] instanceof Collection var5)) {
            return;
         }

         int var7 = this.config.minDrop() + ThreadLocalRandom.current().nextInt(this.config.maxDrop() - this.config.minDrop() + 1);
         var5.add(this.productItem.getItem().clone().asQuantity(var7));
      } catch (ReflectiveOperationException var8) {
      }
   }

   private record Config(String machineId, List<CropSeedScript.Stage> stages, Material requiredBlock, String productItemId, int minDrop, int maxDrop) {
      long totalMs() {
         return this.stages.stream().mapToLong(CropSeedScript.Stage::durationMs).sum();
      }

      int stageAt(long var1) {
         long var3 = 0L;

         for (int var5 = 0; var5 < this.stages.size(); var5++) {
            var3 += this.stages.get(var5).durationMs();
            if (var1 < var3) {
               return var5;
            }
         }

         return this.stages.size() - 1;
      }
   }

   private static final class PlantData {
      final long startTime;
      int currentStage;
      boolean mature;
      boolean checkedForMature;

      PlantData(long var1, int var3, boolean var4, boolean var5) {
         this.startTime = var1;
         this.currentStage = var3;
         this.mature = var4;
         this.checkedForMature = var5;
      }
   }

   private record Stage(Material material, long durationMs) {
   }
}
