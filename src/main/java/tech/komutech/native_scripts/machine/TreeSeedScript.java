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

public final class TreeSeedScript implements NativeScript {
   private static final List<TreeSeedScript.LeafLayer> LEAF_PATTERN = List.of(
      new TreeSeedScript.LeafLayer(1, 1),
      new TreeSeedScript.LeafLayer(0, 2),
      new TreeSeedScript.LeafLayer(-1, 1),
      new TreeSeedScript.LeafLayer(-2, 3),
      new TreeSeedScript.LeafLayer(-3, 2),
      new TreeSeedScript.LeafLayer(-4, 4)
   );
   private static final Map<Location, TreeSeedScript.TreeState> TREE_STATES = new ConcurrentHashMap<>();
   private final TreeSeedScript.TreeConfig config;
   private SlimefunItem dropItem;

   private TreeSeedScript(TreeSeedScript.TreeConfig var1) {
      this.config = var1;
   }

   public static TreeSeedScript pine() {
      return new TreeSeedScript(
         new TreeSeedScript.TreeConfig(
            "KOMUTECH_L_LZZZ_SSSM", 150000L, Material.GRASS_BLOCK, Material.SPRUCE_LOG, Material.SPRUCE_LEAVES, null, "KOMUTECH_L_LZ_SM", 7, 11, 12
         )
      );
   }

   public static TreeSeedScript peach() {
      return new TreeSeedScript(
         new TreeSeedScript.TreeConfig(
            "KOMUTECH_L_LZZZ_TSSM",
            150000L,
            Material.GRASS_BLOCK,
            Material.CHERRY_LOG,
            Material.CHERRY_LEAVES,
            List.of(Material.CHERRY_LEAVES, Material.FLOWERING_AZALEA_LEAVES),
            "KOMUTECH_L_LZ_TM",
            5,
            7,
            12
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
         TREE_STATES.remove(var3.getBlock().getLocation());
      } else if ("onBreak".equals(var1)) {
         this.handleBreak(var2);
      }

      return null;
   }

   private void tick(MachineScriptHelper.MachineContext var1) {
      Location var2 = var1.location();
      World var3 = var2.getWorld();
      if (var3 != null) {
         TreeSeedScript.TreeState var4 = TREE_STATES.get(var2);
         if (var4 == null || !var4.mature && !var4.failed) {
            int var5 = var2.getBlockX();
            int var6 = var2.getBlockY();
            int var7 = var2.getBlockZ();
            if (var4 == null) {
               if (var3.getBlockAt(var5, var6 - 1, var7).getType() != this.config.requiredBlock()) {
                  TREE_STATES.put(var2, TreeSeedScript.TreeState.failed());
               } else {
                  TREE_STATES.put(var2, TreeSeedScript.TreeState.started());
               }
            } else if (var1.block().getType().isAir()) {
               TREE_STATES.remove(var2);
            } else if (System.currentTimeMillis() - var4.startTime >= this.config.growTimeMs()) {
               if (var3.getBlockAt(var5, var6 - 1, var7).getType() != this.config.requiredBlock()) {
                  var4.failed = true;
               } else if (!this.canGenerateTree(var3, var5, var6, var7)) {
                  var4.failed = true;
               } else {
                  int var8 = this.generateTree(var3, var2);
                  if (var8 > 0) {
                     var4.mature = true;
                     var4.treeHeight = var8;
                  } else {
                     var4.failed = true;
                  }
               }
            }
         }
      }
   }

   private int generateTree(World var1, Location var2) {
      int var3 = var2.getBlockX();
      int var4 = var2.getBlockY();
      int var5 = var2.getBlockZ();
      int var6 = this.config.minHeight() + ThreadLocalRandom.current().nextInt(this.config.maxHeight() - this.config.minHeight() + 1);

      for (int var7 = 0; var7 < var6; var7++) {
         var1.getBlockAt(var3, var4 + var7, var5).setType(this.config.trunkMaterial());
      }

      Material var8 = this.config.leavesMaterial();
      if (this.config.leavesMaterials() != null && !this.config.leavesMaterials().isEmpty()) {
         var8 = this.config.leavesMaterials().get(ThreadLocalRandom.current().nextInt(this.config.leavesMaterials().size()));
      }

      generateLeaves(var1, var3, var4 + var6 - 1, var5, var8);
      return var6;
   }

   private static void generateLeaves(World var0, int var1, int var2, int var3, Material var4) {
      for (TreeSeedScript.LeafLayer var6 : LEAF_PATTERN) {
         int var7 = var2 + var6.yOffset();
         int var8 = var6.radius() * var6.radius();

         for (int var9 = -var6.radius(); var9 <= var6.radius(); var9++) {
            for (int var10 = -var6.radius(); var10 <= var6.radius(); var10++) {
               if (var9 * var9 + var10 * var10 <= var8) {
                  Block var11 = var0.getBlockAt(var1 + var9, var7, var3 + var10);
                  if (var11.getType().isAir()) {
                     var11.setType(var4);
                  }
               }
            }
         }
      }
   }

   private boolean canGenerateTree(World var1, int var2, int var3, int var4) {
      for (int var5 = 1; var5 <= this.config.spaceCheckHeight(); var5++) {
         Material var6 = var1.getBlockAt(var2, var3 + var5, var4).getType();
         if (!var6.isAir() && var6 != Material.SPRUCE_SAPLING && var6 != Material.CHERRY_SAPLING && var6 != Material.AZALEA) {
            return false;
         }
      }

      return true;
   }

   private void handleBreak(Object[] var1) {
      try {
         Object var2 = var1[0];
         Location var3 = ((Block)var2.getClass().getMethod("getBlock").invoke(var2)).getLocation();
         TreeSeedScript.TreeState var4 = TREE_STATES.remove(var3);
         if (var4 != null && var4.mature && var4.treeHeight > 0 && var1.length >= 3 && var1[2] instanceof Collection var5) {
            if ("KOMUTECH_L_LZZZ_TSSM".equals(this.config.machineId())) {
               SlimefunItem var7 = SlimefunItem.getById("KOMUTECH_L_LZZZ_TSSM");
               if (var7 != null) {
                  var5.add(var7.getItem().clone());
               }
            }

            if (this.dropItem == null) {
               this.dropItem = SlimefunItem.getById(this.config.dropItemId());
            }

            if (this.dropItem != null) {
               var5.add(this.dropItem.getItem().clone().asQuantity(var4.treeHeight));
            }

            World var13 = var3.getWorld();
            int var8 = var3.getBlockX();
            int var9 = var3.getBlockY();
            int var10 = var3.getBlockZ();

            for (int var11 = 1; var11 < var4.treeHeight; var11++) {
               var13.getBlockAt(var8, var9 + var11, var10).setType(Material.AIR);
            }
         }
      } catch (ReflectiveOperationException var12) {
      }
   }

   private record LeafLayer(int yOffset, int radius) {
   }

   private record TreeConfig(
      String machineId,
      long growTimeMs,
      Material requiredBlock,
      Material trunkMaterial,
      Material leavesMaterial,
      List<Material> leavesMaterials,
      String dropItemId,
      int minHeight,
      int maxHeight,
      int spaceCheckHeight
   ) {
   }

   private static final class TreeState {
      final long startTime;
      boolean mature;
      boolean failed;
      int treeHeight;

      private TreeState(long var1) {
         this.startTime = var1;
      }

      static TreeSeedScript.TreeState started() {
         return new TreeSeedScript.TreeState(System.currentTimeMillis());
      }

      static TreeSeedScript.TreeState failed() {
         TreeSeedScript.TreeState var0 = new TreeSeedScript.TreeState(0L);
         var0.failed = true;
         return var0;
      }
   }
}
