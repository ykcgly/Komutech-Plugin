package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import tech.komutech.native_scripts.cultivation.JianLingShiSupport;
import tech.komutech.native_scripts.machine.UltimateSynthSupport;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class PrefabStructurePlacer {
   private static final String DEFAULT_CLUSTER = "KOMUTECH_L_DJ_JPLJ";
   private static final int[] MULTIBLOCK_SLOT_X = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
   private static final int[] MULTIBLOCK_SLOT_Z = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};

   private PrefabStructurePlacer() {
   }

   public static PrefabStructurePlacer.PlacementSpec spec(PrefabStructureType var0) {
      return switch (var0) {
         case JIAN_LING_SHI -> jianLingShiSpec();
         case ULTIMATE_SYNTH_CORE -> ultimateSynthCoreSpec();
         case MB_CRAFTSMAN -> multiblockSpec(
            "工匠台", Map.of(3, "AMETHYST_CLUSTER", 4, "CRAFTING_TABLE", 5, "AMETHYST_CLUSTER", 6, "SMITHING_TABLE", 7, "DISPENSER", 8, "SMITHING_TABLE")
         );
         case MB_SPIRIT_MINE -> multiblockSpec(
            "灵矿提取台",
            Map.of(1, "AMETHYST_CLUSTER", 3, "GRINDSTONE", 4, "SMITHING_TABLE", 5, "GRINDSTONE", 6, "BLAST_FURNACE", 7, "DISPENSER", 8, "BLAST_FURNACE")
         );
         case MB_SPIRIT_INFUSE -> multiblockSpec(
            "注灵台", Map.of(3, "AMETHYST_CLUSTER", 4, "ENCHANTING_TABLE", 5, "AMETHYST_CLUSTER", 6, "BUDDING_AMETHYST", 7, "DISPENSER", 8, "BUDDING_AMETHYST")
         );
      };
   }

   public static PrefabStructurePlacer.Dimensions dimensions(PrefabStructureType var0) {
      PrefabStructurePlacer.PlacementSpec var1 = spec(var0);
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      boolean var8 = true;

      for (PrefabStructurePlacer.PlacementBlock var10 : var1.blocks()) {
         if (var8) {
            var2 = var3 = var10.x();
            var4 = var5 = var10.y();
            var6 = var7 = var10.z();
            var8 = false;
         } else {
            var2 = Math.min(var2, var10.x());
            var3 = Math.max(var3, var10.x());
            var4 = Math.min(var4, var10.y());
            var5 = Math.max(var5, var10.y());
            var6 = Math.min(var6, var10.z());
            var7 = Math.max(var7, var10.z());
         }
      }

      return new PrefabStructurePlacer.Dimensions(var3 - var2 + 1, var5 - var4 + 1, var7 - var6 + 1);
   }

   public static PrefabStructurePlacer.PlacementResult paste(Player var0, PrefabStructureType var1, int var2) {
      Block var3 = getTargetBlock(var0);
      if (var3 == null) {
         return new PrefabStructurePlacer.PlacementResult(false, "§c请看向一个方块！");
      } else {
         Location var4 = var3.getLocation().add(0.0, 1.0, 0.0);
         PrefabStructurePlacer.PlacementSpec var5 = spec(var1);
         if (!hasSpace(var0.getWorld(), var4, var5, var2)) {
            return new PrefabStructurePlacer.PlacementResult(false, "§c目标区域空间不足或已有方块，请换位置放置。");
         } else {
            for (PrefabStructurePlacer.PlacementBlock var7 : var5.blocks()) {
               int[] var8 = rotateXZ(var7.x(), var7.z(), var2);
               Location var9 = var4.clone().add(var8[0], var7.y(), var8[1]);
               if (!placeBlock(var9, var7)) {
                  return new PrefabStructurePlacer.PlacementResult(false, "§c搭建失败：无法放置方块 (" + var7.x() + "," + var7.y() + "," + var7.z() + ")");
               }
            }

            initializeState(var4, var1, var2);
            return new PrefabStructurePlacer.PlacementResult(true, "§a预制建筑「" + var5.displayName() + "」搭建完成！");
         }
      }
   }

   private static PrefabStructurePlacer.PlacementSpec jianLingShiSpec() {
      ArrayList var0 = new ArrayList();

      for (JianLingShiSupport.StructureBlock var4 : JianLingShiSupport.structure()) {
         var0.add(new PrefabStructurePlacer.PlacementBlock(var4.x(), var4.y(), var4.z(), var4.material(), var4.sfId(), var4.dynamicCluster()));
      }

      return new PrefabStructurePlacer.PlacementSpec("鉴灵石", var0, true);
   }

   private static PrefabStructurePlacer.PlacementSpec ultimateSynthCoreSpec() {
      ArrayList var0 = new ArrayList();

      for (UltimateSynthSupport.StructureBlock var4 : UltimateSynthSupport.coreStructure()) {
         var0.add(new PrefabStructurePlacer.PlacementBlock(var4.x(), var4.y(), var4.z(), var4.material(), var4.sfId(), false));
      }

      return new PrefabStructurePlacer.PlacementSpec("终极合成台核心", var0, false);
   }

   private static PrefabStructurePlacer.PlacementSpec multiblockSpec(String var0, Map<Integer, String> var1) {
      ArrayList var2 = new ArrayList();

      for (Entry var4 : var1.entrySet()) {
         int var5 = (Integer)var4.getKey();
         var2.add(new PrefabStructurePlacer.PlacementBlock(MULTIBLOCK_SLOT_X[var5], 0, MULTIBLOCK_SLOT_Z[var5], (String)var4.getValue(), null, false));
      }

      return new PrefabStructurePlacer.PlacementSpec(var0, var2, false);
   }

   private static boolean hasSpace(World var0, Location var1, PrefabStructurePlacer.PlacementSpec var2, int var3) {
      for (PrefabStructurePlacer.PlacementBlock var5 : var2.blocks()) {
         int[] var6 = rotateXZ(var5.x(), var5.z(), var3);
         Block var7 = var0.getBlockAt(var1.getBlockX() + var6[0], var1.getBlockY() + var5.y(), var1.getBlockZ() + var6[1]);
         if (!var7.getType().isAir()) {
            return false;
         }
      }

      return true;
   }

   private static boolean placeBlock(Location var0, PrefabStructurePlacer.PlacementBlock var1) {
      if (var1.dynamicCluster()) {
         return SlimefunBlockPlacer.place(var0, "KOMUTECH_L_DJ_JPLJ");
      } else {
         return var1.sfId() != null && !var1.sfId().isEmpty()
            ? SlimefunBlockPlacer.place(var0, var1.sfId())
            : SlimefunBlockPlacer.placeVanilla(var0, var1.material());
      }
   }

   private static void initializeState(Location var0, PrefabStructureType var1, int var2) {
      switch (var1) {
         case JIAN_LING_SHI:
            MachineScriptHelper.setData(var0, "KOMUTECH_L_X_JLS_fx", String.valueOf(var2));
            MachineScriptHelper.setData(var0, "KOMUTECH_L_X_JLS_zt", "0");
            MachineScriptHelper.setData(var0, "KOMUTECH_L_X_JLS_ty", "0");
            break;
         case ULTIMATE_SYNTH_CORE:
            MachineScriptHelper.setData(var0, "KOMUTECH_L_ZJ_ZJHC_fx", "0");
            MachineScriptHelper.setData(var0, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
            MachineScriptHelper.setData(var0, "KOMUTECH_L_ZJ_ZJHC_ty", "0");
      }
   }

   private static int[] rotateXZ(int var0, int var1, int var2) {
      return switch (var2 & 3) {
         case 1 -> new int[]{-var1, var0};
         case 2 -> new int[]{-var0, -var1};
         case 3 -> new int[]{var1, -var0};
         default -> new int[]{var0, var1};
      };
   }

   private static Block getTargetBlock(Player var0) {
      BlockIterator var1 = new BlockIterator(var0, 8);

      while (var1.hasNext()) {
         Block var2 = var1.next();
         if (!var2.getType().isAir()) {
            return var2;
         }
      }

      return null;
   }

   public static String directionLabel(int var0) {
      return switch (var0 & 3) {
         case 1 -> "§f东";
         case 2 -> "§f南";
         case 3 -> "§f西";
         default -> "§f北";
      };
   }

   public static SlimefunItem resolveItem(ItemStack var0) {
      return var0 == null ? null : SlimefunItem.getByItem(var0);
   }

   public record Dimensions(int width, int height, int depth) {
   }

   public record PlacementBlock(int x, int y, int z, String material, String sfId, boolean dynamicCluster) {
   }

   public record PlacementResult(boolean success, String message) {
   }

   public record PlacementSpec(String displayName, List<PrefabStructurePlacer.PlacementBlock> blocks, boolean rotatable) {
   }
}
