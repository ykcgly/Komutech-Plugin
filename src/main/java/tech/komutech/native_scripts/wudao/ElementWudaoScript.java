package tech.komutech.native_scripts.wudao;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechMenuHelper;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class ElementWudaoScript implements NativeScript {
   private static final long GENERATION_TIME = 15000L;
   private static final long DETECTION_INTERVAL = 3000L;
   private static final long FINAL_COUNTDOWN = 3000L;
   private static final long SUCCESS_COOLDOWN = 5000L;
   private static final int MIN_ITEMS = 1;
   private static final int BASE_MAX_ITEMS = 8;
   private static final int MAX_ITEMS_LIMIT = 32;
   private static final int MAX_EMPTY_CHECKS = 5;
   private static final String TARGET_MACHINE_ID = "KOMUTECH_L_JQ_FZCZQ";
   private static final String TLY_ITEM_ID = "KOMUTECH_L_KW_TLY";
   private static final Set<Material> DIRT_TYPES = EnumSet.of(
      Material.DIRT,
      Material.GRASS_BLOCK,
      Material.COARSE_DIRT,
      Material.DIRT_PATH,
      Material.PODZOL,
      Material.MYCELIUM,
      Material.ROOTED_DIRT,
      Material.MUD,
      Material.FARMLAND
   );
   private static final Set<Material> ICE_TYPES = EnumSet.of(Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE);
   private final String machineId;
   private final String targetItemId;
   private final ElementWudaoScript.SenseKind kind;
   private final Map<String, ElementWudaoScript.WudaoState> states = new HashMap<>();

   public ElementWudaoScript(String var1, String var2, ElementWudaoScript.SenseKind var3) {
      this.machineId = var1;
      this.targetItemId = var2;
      this.kind = var3;
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("tick".equals(var1)) {
         MachineScriptHelper.MachineContext var6 = MachineScriptHelper.parse(var2[0]);
         if (var6 != null && this.machineId.equals(MachineScriptHelper.getMachineId(var6.machine()))) {
            this.handleTick(var6.block().getLocation());
         }

         return null;
      } else if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var5) {
         this.states.put(blockKey(var5.getBlock().getLocation()), new ElementWudaoScript.WudaoState());
         var5.getPlayer().sendMessage(ChatColor.GREEN + "✓ 已放置悟道石");
         return null;
      } else if ("onBreak".equals(var1) && var2[0] instanceof BlockBreakEvent var3) {
         this.states.remove(blockKey(var3.getBlock().getLocation()));
         return null;
      } else {
         return null;
      }
   }

   private void handleTick(Location var1) {
      String var2 = blockKey(var1);
      ElementWudaoScript.WudaoState var3 = this.states.computeIfAbsent(var2, var0 -> new ElementWudaoScript.WudaoState());
      long var4 = System.currentTimeMillis();
      if (var4 - var3.lastCheck >= 3000L) {
         var3.lastCheck = var4;
         Player var6 = this.findPlayerOnStone(var1);
         if (var6 != null) {
            var3.emptyChecks = 0;
            this.processPlayer(var1, var6, var4, var3);
         } else {
            this.handleNoPlayer(var3);
         }
      }
   }

   private void handleNoPlayer(ElementWudaoScript.WudaoState var1) {
      var1.emptyChecks++;
      if (var1.emptyChecks >= 5) {
         var1.timer = null;
         var1.emptyChecks = 0;
         var1.cooldownUntil = 0L;
      }
   }

   private void processPlayer(Location var1, Player var2, long var3, ElementWudaoScript.WudaoState var5) {
      if (var3 >= var5.cooldownUntil) {
         ElementWudaoScript.WudaoTimer var6 = var5.timer;
         if (var6 == null) {
            ElementWudaoScript.SenseResult var10 = this.senseAndConsume(var1);
            if (var10.count() > 0) {
               int var8 = Math.min(8 + var10.count() * this.kind.bonus, 32);
               var5.timer = new ElementWudaoScript.WudaoTimer(var2.getUniqueId(), var3, var10, var8);
               var2.sendMessage(ChatColor.GREEN + "[悟道石] 感悟" + var10.count() + this.kind.unitLabel + "，开始悟道！");
            }
         } else if (!var6.playerId.equals(var2.getUniqueId())) {
            var2.sendMessage(ChatColor.RED + "[悟道石] 玩家已更换，悟道中断！");
            var5.timer = null;
         } else {
            long var7 = var3 - var6.startTime;
            if (this.kind == ElementWudaoScript.SenseKind.GOLD && !var6.hasConverted && this.checkIronDestroyed(var6.sense.furnaces())) {
               var2.sendMessage(ChatColor.RED + "[悟道石] 粗铁块被破坏，悟道中断！");
               var5.timer = null;
            } else {
               if (this.kind == ElementWudaoScript.SenseKind.GOLD && !var6.hasConverted && var7 >= 7500L) {
                  this.convertIronBlocks(var6.sense.furnaces());
                  var6.hasConverted = true;
                  var2.sendMessage(ChatColor.YELLOW + "[悟道石] 唔~唔~唔~");
               } else if (this.kind != ElementWudaoScript.SenseKind.GOLD && !var6.notifiedHalf && var7 >= 7500L) {
                  var2.sendMessage(ChatColor.YELLOW + "[悟道石] 唔~唔~唔~");
                  var6.notifiedHalf = true;
               }

               if (!var6.notifiedFinal && var7 >= 12000L) {
                  var2.sendMessage(ChatColor.YELLOW + "[悟道石] 来啦！");
                  var6.notifiedFinal = true;
               }

               if (!var6.generated && var7 >= 15000L) {
                  boolean var9 = this.generateItems(var1, var2, var6.maxItems, var6.sense.count());
                  var6.generated = true;
                  var5.timer = null;
                  if (var9) {
                     var5.cooldownUntil = var3 + 5000L;
                  }
               }
            }
         }
      }
   }

   private ElementWudaoScript.SenseResult senseAndConsume(Location var1) {
      return switch (this.kind) {
         case GOLD -> this.senseGold(var1);
         case WOOD -> ElementWudaoScript.SenseResult.of(this.clearSaplings(var1));
         case WATER -> ElementWudaoScript.SenseResult.of(this.replaceFlowingWater(var1));
         case FIRE -> ElementWudaoScript.SenseResult.of(this.clearBlocks(var1, var0 -> var0 == Material.FIRE || var0 == Material.SOUL_FIRE));
         case EARTH -> ElementWudaoScript.SenseResult.of(this.clearDistinctBlocks(var1, DIRT_TYPES));
         case ICE -> ElementWudaoScript.SenseResult.of(this.clearDistinctBlocks(var1, ICE_TYPES));
         case WIND -> ElementWudaoScript.SenseResult.of(this.clearEntities(var1, EntityType.BREEZE));
         case LIGHTNING -> ElementWudaoScript.SenseResult.of(this.clearBlocks(var1, var0 -> var0 == Material.LIGHTNING_ROD));
         case MECHANISM -> ElementWudaoScript.SenseResult.of(this.clearEntities(var1, EntityType.IRON_GOLEM));
         case ALCHEMY -> ElementWudaoScript.SenseResult.of(this.clearEntities(var1, EntityType.WITCH));
         case SOUND -> ElementWudaoScript.SenseResult.of(this.clearEntities(var1, EntityType.WARDEN));
         case SPACE -> ElementWudaoScript.SenseResult.of(this.clearBlocks(var1, var0 -> var0 == Material.NETHER_PORTAL));
         case KILL -> ElementWudaoScript.SenseResult.of(this.clearEntities(var1, EntityType.ZOMBIE, EntityType.SKELETON));
      };
   }

   private ElementWudaoScript.SenseResult senseGold(Location var1) {
      World var2 = var1.getWorld();
      if (var2 == null) {
         return ElementWudaoScript.SenseResult.of(0);
      } else {
         ArrayList var3 = new ArrayList();
         int var4 = var1.getBlockX();
         int var5 = var1.getBlockY();
         int var6 = var1.getBlockZ();

         for (int var7 = -2; var7 <= 2; var7++) {
            for (int var8 = -1; var8 <= 2; var8++) {
               for (int var9 = -2; var9 <= 2; var9++) {
                  if (var2.getBlockAt(var4 + var7, var5 + var8, var6 + var9).getType() == Material.BLAST_FURNACE
                     && var2.getBlockAt(var4 + var7, var5 + var8 + 1, var6 + var9).getType() == Material.RAW_IRON_BLOCK) {
                     var3.add(new ElementWudaoScript.FurnaceSpot(var2, var4 + var7, var5 + var8 + 1, var6 + var9));
                  }
               }
            }
         }

         return ElementWudaoScript.SenseResult.furnaces(var3);
      }
   }

   private int clearSaplings(Location var1) {
      World var2 = var1.getWorld();
      if (var2 == null) {
         return 0;
      } else {
         HashSet var3 = new HashSet();
         this.forEachInArea(var1, var1x -> {
            String var2x = var1x.getType().name();
            if (var2x.contains("SAPLING") && !"DEAD_BUSH".equals(var2x)) {
               var3.add(var2x);
               var1x.setType(Material.AIR);
            }
         });
         return var3.size();
      }
   }

   private int replaceFlowingWater(Location var1) {
      int[] var2 = new int[]{0};
      this.forEachInArea(var1, var1x -> {
         if (var1x.getType() == Material.WATER) {
            if (var1x.getBlockData() instanceof Levelled var3 && var3.getLevel() > 0) {
               var3.setLevel(0);
               var1x.setBlockData(var3);
               var2[0]++;
            }
         }
      });
      return var2[0];
   }

   private int clearBlocks(Location var1, Predicate<Material> var2) {
      int[] var3 = new int[]{0};
      this.forEachInArea(var1, var2x -> {
         if (var2.test(var2x.getType())) {
            var3[0]++;
            var2x.setType(Material.AIR);
         }
      });
      return var3[0];
   }

   private int clearDistinctBlocks(Location var1, Set<Material> var2) {
      EnumSet var3 = EnumSet.noneOf(Material.class);
      this.forEachInArea(var1, var2x -> {
         Material var3x = var2x.getType();
         if (var2.contains(var3x)) {
            var3.add(var3x);
            var2x.setType(Material.AIR);
         }
      });
      return var3.size();
   }

   private int clearEntities(Location var1, EntityType... var2) {
      World var3 = var1.getWorld();
      if (var3 == null) {
         return 0;
      } else {
         EnumSet var4 = EnumSet.noneOf(EntityType.class);

         for (EntityType var8 : var2) {
            var4.add(var8);
         }

         Location var9 = var1.clone().add(0.5, 1.0, 0.5);
         int var10 = 0;

         for (Entity var12 : EntityQueries.entities(var3, var9, 2.5, 1.5, 2.5)) {
            if (var4.contains(var12.getType()) && var12.isValid() && !var12.isDead()) {
               var12.remove();
               var10++;
            }
         }

         return var10;
      }
   }

   private void forEachInArea(Location var1, Consumer<Block> var2) {
      World var3 = var1.getWorld();
      if (var3 != null) {
         int var4 = var1.getBlockX();
         int var5 = var1.getBlockY();
         int var6 = var1.getBlockZ();

         for (int var7 = var4 - 2; var7 <= var4 + 2; var7++) {
            for (int var8 = var5 - 1; var8 <= var5 + 1; var8++) {
               for (int var9 = var6 - 2; var9 <= var6 + 2; var9++) {
                  var2.accept(var3.getBlockAt(var7, var8, var9));
               }
            }
         }
      }
   }

   private Player findPlayerOnStone(Location var1) {
      World var2 = var1.getWorld();
      if (var2 == null) {
         return null;
      } else {
         Location var3 = var1.clone().add(0.5, 1.0, 0.5);

         for (Entity var5 : EntityQueries.entities(var2, var3, 1.0, 2.0, 1.0)) {
            if (var5 instanceof Player var6 && var6.isValid() && !var6.isDead()) {
               return var6;
            }
         }

         return null;
      }
   }

   private boolean checkIronDestroyed(List<ElementWudaoScript.FurnaceSpot> var1) {
      for (ElementWudaoScript.FurnaceSpot var3 : var1) {
         Material var4 = var3.converted ? Material.IRON_BLOCK : Material.RAW_IRON_BLOCK;
         if (var3.world.getBlockAt(var3.x, var3.y, var3.z).getType() != var4) {
            return true;
         }
      }

      return false;
   }

   private void convertIronBlocks(List<ElementWudaoScript.FurnaceSpot> var1) {
      for (ElementWudaoScript.FurnaceSpot var3 : var1) {
         Block var4 = var3.world.getBlockAt(var3.x, var3.y, var3.z);
         if (var4.getType() == Material.RAW_IRON_BLOCK) {
            var4.setType(Material.IRON_BLOCK);
            var3.converted = true;
         }
      }
   }

   private boolean generateItems(Location var1, Player var2, int var3, int var4) {
      Location var5 = var1.clone().add(0.0, 0.0, 1.0);
      SlimefunItem var6 = MachineScriptHelper.getSfItem(var5);
      if (var6 != null && "KOMUTECH_L_JQ_FZCZQ".equals(var6.getId())) {
         Object var7 = MachineScriptHelper.getMenu(var5);
         if (var7 == null) {
            var2.sendMessage(ChatColor.RED + "[悟道石] 无法打开法则承载器菜单，请重放机器后再试");
            return false;
         } else if (this.kind == ElementWudaoScript.SenseKind.LIGHTNING && !this.consumeTly(var7)) {
            var2.sendMessage(ChatColor.RED + "[悟道石] 目标机器中没有霆雷玉！");
            return false;
         } else {
            SlimefunItem var8 = SlimefunItem.getById(this.targetItemId);
            if (var8 == null) {
               var2.sendMessage(ChatColor.RED + "[悟道石] 无法找到物品: " + this.targetItemId);
               return false;
            } else {
               ArrayList var9 = new ArrayList();

               for (int var10 = 9; var10 <= 44; var10++) {
                  MachineScriptHelper.ItemStackInMenu var11 = MachineScriptHelper.getItemInSlot(var7, var10);
                  if (var11 == null || var11.stack() == null || var11.stack().getType().isAir()) {
                     var9.add(var10);
                  }
               }

               if (var9.isEmpty()) {
                  var2.sendMessage(ChatColor.RED + "[悟道石] 法则承载器已满，请先取出物品后再悟道");
                  return false;
               } else {
                  int var13 = Math.min(1 + ThreadLocalRandom.current().nextInt(var3 - 1 + 1), var9.size());
                  ItemStack var14 = var8.getItem().clone();
                  var14.setAmount(1);

                  for (int var12 = 0; var12 < var13; var12++) {
                     KomutechMenuHelper.replaceExistingItem(var7, (Integer)var9.get(var12), var14.clone());
                  }

                  var2.sendMessage(ChatColor.GOLD + "[悟道石] 我悟了！！！");
                  var2.sendMessage(ChatColor.GREEN + "[悟道石] 感悟" + var4 + this.kind.unitLabel + "，生成 " + var13 + " 个物品（请从南侧法则承载器取出）");
                  return true;
               }
            }
         }
      } else {
         var2.sendMessage(ChatColor.RED + "[悟道石] 目标机器不存在！请在悟道石 +Z（南）侧放置法则承载器");
         return false;
      }
   }

   private boolean consumeTly(Object var1) {
      ItemStack[] var2 = MachineScriptHelper.getMenuContents(var1);

      for (int var3 = 0; var3 < var2.length; var3++) {
         ItemStack var4 = var2[var3];
         if (var4 != null && !var4.getType().isAir()) {
            SlimefunItem var5 = SlimefunItem.getByItem(var4);
            if (var5 != null && "KOMUTECH_L_KW_TLY".equals(var5.getId())) {
               if (var4.getAmount() > 1) {
                  ItemStack var6 = var4.clone();
                  var6.setAmount(var4.getAmount() - 1);
                  KomutechMenuHelper.replaceExistingItem(var1, var3, var6);
               } else {
                  KomutechMenuHelper.replaceExistingItem(var1, var3, null);
               }

               return true;
            }
         }
      }

      return false;
   }

   private static String blockKey(Location var0) {
      World var1 = var0.getWorld();
      String var2 = var1 == null ? "null" : var1.getUID().toString();
      return var2 + ":" + var0.getBlockX() + ":" + var0.getBlockY() + ":" + var0.getBlockZ();
   }

   private static final class FurnaceSpot {
      final World world;
      final int x;
      final int y;
      final int z;
      boolean converted;

      FurnaceSpot(World var1, int var2, int var3, int var4) {
         this.world = var1;
         this.x = var2;
         this.y = var3;
         this.z = var4;
      }
   }

   public static enum SenseKind {
      GOLD(4, "个高炉"),
      WOOD(4, "种树苗"),
      WATER(4, "处流水"),
      FIRE(4, "团火焰"),
      EARTH(4, "种泥土"),
      ICE(8, "种寒冰"),
      WIND(4, "个旋风人"),
      LIGHTNING(8, "个避雷针"),
      MECHANISM(4, "个铁傀儡"),
      ALCHEMY(4, "个女巫"),
      SOUND(4, "个监守者"),
      SPACE(8, "块传送门"),
      KILL(4, "个小岛人");

      final int bonus;
      final String unitLabel;

      private SenseKind(int nullxx, String nullxxx) {
         this.bonus = nullxx;
         this.unitLabel = nullxxx;
      }
   }

   private record SenseResult(int count, List<ElementWudaoScript.FurnaceSpot> furnaces) {
      static ElementWudaoScript.SenseResult of(int var0) {
         return new ElementWudaoScript.SenseResult(var0, List.of());
      }

      static ElementWudaoScript.SenseResult furnaces(List<ElementWudaoScript.FurnaceSpot> var0) {
         return new ElementWudaoScript.SenseResult(var0.size(), var0);
      }
   }

   private static final class WudaoState {
      long lastCheck;
      int emptyChecks;
      long cooldownUntil;
      ElementWudaoScript.WudaoTimer timer;
   }

   private static final class WudaoTimer {
      final UUID playerId;
      final long startTime;
      final ElementWudaoScript.SenseResult sense;
      final int maxItems;
      boolean hasConverted;
      boolean notifiedHalf;
      boolean notifiedFinal;
      boolean generated;

      WudaoTimer(UUID var1, long var2, ElementWudaoScript.SenseResult var4, int var5) {
         this.playerId = var1;
         this.startTime = var2;
         this.sense = var4;
         this.maxItems = var5;
      }
   }
}
