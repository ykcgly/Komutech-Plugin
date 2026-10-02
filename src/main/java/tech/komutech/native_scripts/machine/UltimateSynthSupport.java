package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.storage.WanXiangGuiStorage;
import tech.komutech.native_scripts.support.CultivationMath;
import tech.komutech.native_scripts.support.KomutechMenuHelper;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.support.SlimefunBlockPlacer;
import tech.komutech.native_scripts.support.StructureMissingHint;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class UltimateSynthSupport {
   public static final String CORE_ID = "KOMUTECH_L_ZJ_終極合成台核心";
   public static final String PROCESSOR_ID = "KOMUTECH_L_ZJ_終極合成台";
   public static final String DATA_STATE = "KOMUTECH_L_ZJ_ZJHC_zt";
   public static final String DATA_DIR = "KOMUTECH_L_ZJ_ZJHC_fx";
   public static final String DATA_PROJECTION = "KOMUTECH_L_ZJ_ZJHC_ty";
   public static final String GUI_TITLE = "§6太极合成核心";
   public static final int[] INPUT_SLOTS = new int[]{0, 1, 2, 9, 10, 11, 18, 19, 20};
   private static final int[] DECORATION_SLOTS = new int[]{3, 4, 5, 12, 14, 21, 22, 23};
   public static final int[] OUTPUT_SLOTS = new int[]{6, 7, 8, 15, 16, 17, 24, 25, 26};
   public static final int WORK_SLOT = 13;
   public static final String ITEM_WUXING = "KOMUTECH_L_FZ_五行祖炁";
   public static final String ITEM_XXZZ = "Komutech_L_XX_ZZ";
   public static final String ITEM_KONGPU = "KOMUTECH_L_X_空蒲";
   public static final String ITEM_TIANXIANG = "KOMUTECH_L_FZ_天象祖炁";
   public static final String ITEM_ZAOHUA = "KOMUTECH_L_FZ_造化祖炁";
   public static final String ITEM_WU = "KOMUTECH_L_ZJ_無";
   public static final String ITEM_WANXIANG = "KOMUTECH_L_ZJ_萬象匱";
   public static final String ITEM_WANYANYI = "KOMUTECH_L_ZJ_萬衍儀";
   public static final Material ENDER_CHEST;
   public static final String LING_ID = "KOMUTECH_L_DJ_JPLJ";
   public static final long SPIRIT_REQ_WU = 100000L;
   public static final long SPIRIT_REQ_ADVANCED = 100000L;
   private static final StructureBlock[] CRAFT_STRUCTURE;
   private static final StructureBlock[] CORE_STRUCTURE;
   private static final Color[][] TRAIL_COLORS;

   private UltimateSynthSupport() {
   }

   public static String locationKey(Location var0) {
      String var10000 = var0.getWorld().getName();
      return var10000 + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   public static boolean isProcessorInputSlot(int var0) {
      return containsSlot(INPUT_SLOTS, var0);
   }

   public static boolean isProcessorOutputSlot(int var0) {
      return containsSlot(OUTPUT_SLOTS, var0);
   }

   public static boolean isProcessorMaterialSlot(int var0) {
      return isProcessorInputSlot(var0) || isProcessorOutputSlot(var0);
   }

   /** @deprecated */
   public static boolean isProcessorInteractiveSlot(int var0) {
      return isProcessorMaterialSlot(var0);
   }

   public static boolean isProcessorDecorationSlot(int var0) {
      return containsSlot(DECORATION_SLOTS, var0);
   }

   private static boolean containsSlot(int[] var0, int var1) {
      for(int var5 : var0) {
         if (var5 == var1) {
            return true;
         }
      }

      return false;
   }

   public static Rotated rot(int var0, int var1, int var2) {
      Rotated var10000;
      switch (var2) {
         case 1 -> var10000 = new Rotated(-var1, var0);
         case 2 -> var10000 = new Rotated(-var0, -var1);
         case 3 -> var10000 = new Rotated(var1, -var0);
         default -> var10000 = new Rotated(var0, var1);
      }

      return var10000;
   }

   public static StructureCheck checkStructure(Location var0, StructureBlock[] var1) {
      World var2 = var0.getWorld();
      if (var2 == null) {
         return new StructureCheck(false, List.of("世界无效"));
      } else {
         int var3 = parseInt(MachineScriptHelper.getData(var0, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
         ArrayList var4 = new ArrayList();

         for(StructureBlock var8 : var1) {
            Rotated var9 = rot(var8.x(), var8.z(), var3);
            int var10 = var0.getBlockX() + var9.x();
            int var11 = var0.getBlockY() + var8.y();
            int var12 = var0.getBlockZ() + var9.z();
            Block var13 = var2.getBlockAt(var10, var11, var12);
            if (!var13.getType().name().equalsIgnoreCase(var8.material())) {
               int var10001 = var10 - var0.getBlockX();
               var4.add("位置 (" + var10001 + "," + (var11 - var0.getBlockY()) + "," + (var12 - var0.getBlockZ()) + ") 需要 " + var8.material());
            } else if (var8.sfId() != null) {
               SlimefunItem var14 = MachineScriptHelper.getSfItem(var13.getLocation());
               String var15 = var14 == null ? null : var14.getId();
               if (var15 == null || !idsEqual(var8.sfId(), var15)) {
                  int var16 = var10 - var0.getBlockX();
                  var4.add("位置 (" + var16 + "," + (var11 - var0.getBlockY()) + "," + (var12 - var0.getBlockZ()) + ") 需要粘液物品 " + var8.sfId() + (var15 == null ? "" : "，找到 " + var15));
               }
            }
         }

         return new StructureCheck(var4.isEmpty(), var4);
      }
   }

   /**
    * 收集全部缺失 / 放错的方块（世界绝对坐标），供 StructureMissingHint 生成红色提示实体。
    * checkStructure 只产出文本错误，这里产出可直接定位的坐标与材质。
    */
   public static List<StructureMissingHint.MissingBlock> collectMissing(Location var0, StructureBlock[] var1) {
      World var2 = var0.getWorld();
      ArrayList var3 = new ArrayList();
      if (var2 == null) {
         return var3;
      }

      int var4 = parseInt(MachineScriptHelper.getData(var0, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);

      for (StructureBlock var8 : var1) {
         Rotated var9 = rot(var8.x(), var8.z(), var4);
         int var10 = var0.getBlockX() + var9.x();
         int var11 = var0.getBlockY() + var8.y();
         int var12 = var0.getBlockZ() + var9.z();
         Location var13 = new Location(var2, (double)var10, (double)var11, (double)var12);
         Material var14 = Material.matchMaterial(var8.material());
         if (var14 != null) {
            Block var15 = var2.getBlockAt(var10, var11, var12);
            if (var15.getType() != var14) {
               var3.add(new StructureMissingHint.MissingBlock(var13, var14, (String)null));
            } else if (var8.sfId() != null) {
               SlimefunItem var16 = MachineScriptHelper.getSfItem(var15.getLocation());
               String var17 = var16 == null ? null : var16.getId();
               if (var17 == null || !idsEqual(var8.sfId(), var17)) {
                  var3.add(new StructureMissingHint.MissingBlock(var13, var14, var8.sfId()));
               }
            }
         }
      }

      return var3;
   }

   /** 核心结构（含方向旋转）的缺失清单。 */
   public static List<StructureMissingHint.MissingBlock> collectCoreMissing(Location var0) {
      return collectMissing(var0, CORE_STRUCTURE);
   }

   private static boolean idsEqual(String var0, String var1) {
      return var0 != null && var1 != null && var0.equalsIgnoreCase(var1);
   }

   public static StructureCheck checkCraftStructure(Location var0) {
      return checkStructure(var0, CRAFT_STRUCTURE);
   }

   public static StructureCheck checkCoreStructure(Location var0) {
      return checkStructure(var0, CORE_STRUCTURE);
   }

   public static StructureBlock[] coreStructure() {
      return CORE_STRUCTURE;
   }

   public static StructureBlock[] craftStructure() {
      return CRAFT_STRUCTURE;
   }

   public static Location getTargetProcessor(Player var0) {
      Block var1 = var0.getTargetBlockExact(5);
      if (var1 == null) {
         return null;
      } else {
         SlimefunItem var2 = MachineScriptHelper.getSfItem(var1.getLocation());
         return var2 != null && "KOMUTECH_L_ZJ_終極合成台".equals(var2.getId()) ? var1.getLocation() : null;
      }
   }

   public static boolean preCheck(Player var0, Location var1, Object var2) {
      Location var3 = var1.clone().add((double)0.0F, (double)-1.0F, (double)0.0F);
      SlimefunItem var4 = MachineScriptHelper.getSfItem(var3);
      if (var4 != null && "KOMUTECH_L_ZJ_終極合成台核心".equals(var4.getId())) {
         if (parseInt(MachineScriptHelper.getData(var3, "KOMUTECH_L_ZJ_ZJHC_zt"), 0) != 1) {
            KomutechSupport.send(var0, "§c核心未激活！");
            return false;
         } else {
            int[] var5 = OUTPUT_SLOTS;
            int var6 = var5.length;
            byte var7 = 0;
            if (var7 < var6) {
               int var8 = var5[var7];
               if (MachineScriptHelper.getItemInSlot(var2, var8) != null) {
                  KomutechSupport.send(var0, "§c输出槽已满！");
                  return false;
               }
            }

            MatchedRecipe var9 = matchRecipe(var2);
            if (var9 == null) {
               KomutechSupport.send(var0, "§c没有匹配的配方！");
               return false;
            } else {
               return var9.spiritReq() > 0L ? checkCultivation(var0, var9.spiritReq()) : true;
            }
         }
      } else {
         KomutechSupport.send(var0, "§c核心机器无效！");
         return false;
      }
   }

   public static boolean checkCultivation(Player var0, long var1) {
      Map var3 = PlayerAttributeStore.load(var0.getName());
      if (var3 == null) {
         KomutechSupport.send(var0, "§c无法读取修仙属性，无法合成");
         return false;
      } else {
         long var4 = CultivationMath.spiritValue(var3);
         if (var4 < var1) {
            CultivationMath.CultivationInfo var6 = CultivationMath.getCultivationInfo(var1);
            KomutechSupport.send(var0, "§c修为不足！至少需要 §6" + var6.stage() + " §c方可进行此合成");
            return false;
         } else {
            return true;
         }
      }
   }

   public static void finalCraft(Player var0, Location var1, Object var2) {
      Location var3 = var1.clone().add((double)0.0F, (double)-1.0F, (double)0.0F);
      StructureCheck var4 = checkCraftStructure(var3);
      if (!var4.valid()) {
         KomutechSupport.send(var0, "§c合成失败：合成期间结构被破坏！");
      } else {
         SlimefunItem var5 = MachineScriptHelper.getSfItem(var3);
         if (var5 != null && "KOMUTECH_L_ZJ_終極合成台核心".equals(var5.getId())) {
            if (parseInt(MachineScriptHelper.getData(var3, "KOMUTECH_L_ZJ_ZJHC_zt"), 0) != 1) {
               KomutechSupport.send(var0, "§c核心已关闭，合成失败！");
            } else {
               MatchedRecipe var6 = matchRecipe(var2);
               if (var6 == null) {
                  KomutechSupport.send(var0, "§c合成失败：配方不匹配！");
               } else if (var6.spiritReq() <= 0L || checkCultivation(var0, var6.spiritReq())) {
                  ItemStack var7 = getItemStack(var6.outputId());
                  if (var7 == null) {
                     KomutechSupport.send(var0, "§c合成失败：输出物品无效！");
                  } else {
                     consumeRecipe(var2, var6);
                     prepareGraduationOutput(var0, var6.outputId(), var7);
                     deliverCraftOutput(var0, var2, var7);
                     int var8 = consumeRandomLing(var0, var3);
                     MachineScriptHelper.setData(var3, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
                     if (var8 > 0) {
                        KomutechSupport.send(var0, "§a合成成功！消耗了 " + var8 + " 个灵晶，核心已关闭。");
                     } else {
                        KomutechSupport.send(var0, "§a合成成功！核心已关闭。");
                     }

                  }
               }
            }
         } else {
            KomutechSupport.send(var0, "§c核心机器已变更，合成失败！");
         }
      }
   }

   private static void prepareGraduationOutput(Player var0, String var1, ItemStack var2) {
      if (var2 != null && var0 != null && "KOMUTECH_L_ZJ_萬象匱".equalsIgnoreCase(var1)) {
         String var3 = "毕业-" + var0.getName();
         String var4 = var3;

         for(int var5 = 1; WanXiangGuiStorage.storageFileExists(var0.getName(), var4); var4 = var3 + "-" + var5++) {
         }

         WanXiangGuiStorage.setStorageName(var2, var4);
         if (!WanXiangGuiStorage.storageFileExists(var0.getName(), var4)) {
            WanXiangGuiStorage.write(var0.getName(), var4, WanXiangGuiStorage.emptyData());
         }

      }
   }

   private static void deliverCraftOutput(Player var0, Object var1, ItemStack var2) {
      if (var2 != null) {
         if (var0 != null && var0.isOnline()) {
            KomutechSupport.giveOrDrop(var0, var2.clone());
         } else {
            for(int var6 : OUTPUT_SLOTS) {
               if (MachineScriptHelper.getItemInSlot(var1, var6) == null) {
                  KomutechMenuHelper.replaceExistingItem(var1, var6, var2.clone());
                  break;
               }
            }

         }
      }
   }

   private static int consumeRandomLing(Player var0, Location var1) {
      World var2 = var1.getWorld();
      if (var2 == null) {
         return 0;
      } else {
         int var3 = parseInt(MachineScriptHelper.getData(var1, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
         ArrayList var4 = new ArrayList();

         for(StructureBlock var8 : CRAFT_STRUCTURE) {
            if ("KOMUTECH_L_DJ_JPLJ".equals(var8.sfId())) {
               Rotated var9 = rot(var8.x(), var8.z(), var3);
               var4.add(var1.clone().add((double)var9.x(), (double)var8.y(), (double)var9.z()));
            }
         }

         if (var4.isEmpty()) {
            return 0;
         } else {
            int var13 = Math.min(ThreadLocalRandom.current().nextInt(8, 25), var4.size());
            HashSet<Integer> var14 = new HashSet<>();

            while(var14.size() < var13) {
               var14.add(ThreadLocalRandom.current().nextInt(var4.size()));
            }

            int var15 = 0;

            for(int var17 : var14) {
               Location var10 = (Location)var4.get(var17);
               Block var11 = var2.getBlockAt(var10);
               SlimefunItem var12 = MachineScriptHelper.getSfItem(var10);
               if (var11.getType() == Material.AMETHYST_CLUSTER && var12 != null && "KOMUTECH_L_DJ_JPLJ".equalsIgnoreCase(var12.getId())) {
                  SlimefunBlockPlacer.remove(var10);
                  ++var15;
               }
            }

            return var15;
         }
      }
   }

   public static List<Location> lingLocations(Location var0) {
      int var1 = parseInt(MachineScriptHelper.getData(var0, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
      ArrayList var2 = new ArrayList();

      for(StructureBlock var6 : CRAFT_STRUCTURE) {
         if ("KOMUTECH_L_DJ_JPLJ".equals(var6.sfId())) {
            Rotated var7 = rot(var6.x(), var6.z(), var1);
            var2.add(var0.clone().add((double)var7.x(), (double)var6.y(), (double)var7.z()));
         }
      }

      return var2;
   }

   public static List<Location> beaconLocations(Location var0) {
      int var1 = parseInt(MachineScriptHelper.getData(var0, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
      ArrayList var2 = new ArrayList();

      for(StructureBlock var6 : CRAFT_STRUCTURE) {
         if ("beacon".equals(var6.material())) {
            Rotated var7 = rot(var6.x(), var6.z(), var1);
            var2.add(var0.clone().add((double)var7.x(), (double)var6.y(), (double)var7.z()));
         }
      }

      return var2;
   }

   public static Color trailColor(int var0, int var1) {
      Color[] var2 = TRAIL_COLORS[Math.floorMod(var0, TRAIL_COLORS.length)];
      return var2[Math.floorMod(var1, var2.length)];
   }

   public static Location trailPosition(Location var0, int var1, double var2) {
      double var4 = var2 * (double)0.5F + (double)var1 * (double)2.0F;
      double var6 = (double)6.0F + Math.sin(var2 * 0.3 + (double)var1);
      double var8 = Math.sin(var2 * 0.8 + (double)var1) * (double)2.0F;
      return new Location(var0.getWorld(), var0.getX() + var6 * Math.cos(var4), var0.getY() + (double)2.0F + var8, var0.getZ() + var6 * Math.sin(var4));
   }

   private static MatchedRecipe matchRecipe(Object var0) {
      int[] var1 = new int[]{0, 2, 9, 11, 19};
      int[] var2 = new int[]{0, 1, 2, 9, 11, 18, 19, 20};
      if (slotsEmpty(var0, var1) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(1, "KOMUTECH_L_FZ_五行祖炁", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(10, "Komutech_L_XX_ZZ", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(18, "KOMUTECH_L_FZ_天象祖炁", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(20, "KOMUTECH_L_FZ_造化祖炁", 1))) {
         return new MatchedRecipe("KOMUTECH_L_ZJ_無", 100000L, new SlotRequirement[]{UltimateSynthSupport.SlotRequirement.sf(1, "KOMUTECH_L_FZ_五行祖炁", 1), UltimateSynthSupport.SlotRequirement.sf(10, "Komutech_L_XX_ZZ", 1), UltimateSynthSupport.SlotRequirement.sf(18, "KOMUTECH_L_FZ_天象祖炁", 1), UltimateSynthSupport.SlotRequirement.sf(20, "KOMUTECH_L_FZ_造化祖炁", 1)});
      } else if (slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(10, "KOMUTECH_L_ZJ_無", 1)) && slotsMatchMaterial(var0, var2, ENDER_CHEST)) {
         SlotRequirement[] var5 = buildSurroundConsumes(var2, ENDER_CHEST);
         SlotRequirement[] var6 = mergeRequirements(new SlotRequirement[]{UltimateSynthSupport.SlotRequirement.sf(10, "KOMUTECH_L_ZJ_無", 1)}, var5);
         return new MatchedRecipe("KOMUTECH_L_ZJ_萬象匱", 100000L, var6);
      } else if (slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(10, "KOMUTECH_L_ZJ_無", 1)) && slotsMatchSlimefun(var0, var2, "Komutech_L_XX_ZZ")) {
         SlotRequirement[] var3 = buildSurroundConsumesSf(var2, "Komutech_L_XX_ZZ");
         SlotRequirement[] var4 = mergeRequirements(new SlotRequirement[]{UltimateSynthSupport.SlotRequirement.sf(10, "KOMUTECH_L_ZJ_無", 1)}, var3);
         return new MatchedRecipe("KOMUTECH_L_ZJ_萬衍儀", 100000L, var4);
      } else {
         return matchesKongPu(var0) ? new MatchedRecipe("KOMUTECH_L_X_空蒲", 0L, new SlotRequirement[]{UltimateSynthSupport.SlotRequirement.sf(0, "KOMUTECH_L_FZ_五行祖炁", 1), UltimateSynthSupport.SlotRequirement.sf(1, "KOMUTECH_L_FZ_天象祖炁", 1), UltimateSynthSupport.SlotRequirement.sf(2, "KOMUTECH_L_FZ_造化祖炁", 1), UltimateSynthSupport.SlotRequirement.sf(9, "KOMUTECH_L_X_莲台", 1), UltimateSynthSupport.SlotRequirement.sf(10, "KOMUTECH_L_ZJ_無", 64), UltimateSynthSupport.SlotRequirement.sf(11, "KOMUTECH_L_X_莲台", 1), UltimateSynthSupport.SlotRequirement.sf(18, "Komutech_L_KW_TXY", 1), UltimateSynthSupport.SlotRequirement.sf(19, "KOMUTECH_L_X_云榻", 1), UltimateSynthSupport.SlotRequirement.sf(20, "Komutech_L_KW_TXY", 1)}) : null;
      }
   }

   private static boolean matchesKongPu(Object var0) {
      return slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(0, "KOMUTECH_L_FZ_五行祖炁", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(1, "KOMUTECH_L_FZ_天象祖炁", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(2, "KOMUTECH_L_FZ_造化祖炁", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(9, "KOMUTECH_L_X_莲台", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(10, "KOMUTECH_L_ZJ_無", 64)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(11, "KOMUTECH_L_X_莲台", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(18, "Komutech_L_KW_TXY", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(19, "KOMUTECH_L_X_云榻", 1)) && slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(20, "Komutech_L_KW_TXY", 1));
   }

   private static SlotRequirement[] buildSurroundConsumes(int[] var0, Material var1) {
      SlotRequirement[] var2 = new SlotRequirement[var0.length];

      for(int var3 = 0; var3 < var0.length; ++var3) {
         var2[var3] = UltimateSynthSupport.SlotRequirement.mat(var0[var3], var1, 1);
      }

      return var2;
   }

   private static SlotRequirement[] buildSurroundConsumesSf(int[] var0, String var1) {
      SlotRequirement[] var2 = new SlotRequirement[var0.length];

      for(int var3 = 0; var3 < var0.length; ++var3) {
         var2[var3] = UltimateSynthSupport.SlotRequirement.sf(var0[var3], var1, 1);
      }

      return var2;
   }

   private static SlotRequirement[] mergeRequirements(SlotRequirement[] var0, SlotRequirement[] var1) {
      SlotRequirement[] var2 = new SlotRequirement[var0.length + var1.length];
      System.arraycopy(var0, 0, var2, 0, var0.length);
      System.arraycopy(var1, 0, var2, var0.length, var1.length);
      return var2;
   }

   private static void consumeRecipe(Object var0, MatchedRecipe var1) {
      for(SlotRequirement var5 : var1.consumes()) {
         consumeSlot(var0, var5);
      }

   }

   private static void consumeSlot(Object var0, SlotRequirement var1) {
      MachineScriptHelper.ItemStackInMenu var2 = MachineScriptHelper.getItemInSlot(var0, var1.slot());
      if (var2 != null) {
         ItemStack var3 = var2.stack();
         if (var3.getAmount() > var1.amount()) {
            var3.setAmount(var3.getAmount() - var1.amount());
            KomutechMenuHelper.replaceExistingItem(var0, var1.slot(), var3);
         } else {
            KomutechMenuHelper.replaceExistingItem(var0, var1.slot(), (ItemStack)null);
         }

      }
   }

   private static boolean slotMatches(Object var0, SlotRequirement var1) {
      MachineScriptHelper.ItemStackInMenu var2 = MachineScriptHelper.getItemInSlot(var0, var1.slot());
      if (var2 == null) {
         return false;
      } else {
         ItemStack var3 = var2.stack();
         if (var3.getAmount() < var1.amount()) {
            return false;
         } else if (var1.material() != null) {
            return var3.getType() == var1.material();
         } else {
            SlimefunItem var4 = SlimefunItem.getByItem(var3);
            return var4 != null && idsEqual(var4.getId(), var1.slimefunId());
         }
      }
   }

   private static boolean slotsEmpty(Object var0, int[] var1) {
      for(int var5 : var1) {
         if (MachineScriptHelper.getItemInSlot(var0, var5) != null) {
            return false;
         }
      }

      return true;
   }

   private static boolean slotsMatchSlimefun(Object var0, int[] var1, String var2) {
      for(int var6 : var1) {
         if (!slotMatches(var0, UltimateSynthSupport.SlotRequirement.sf(var6, var2, 1))) {
            return false;
         }
      }

      return true;
   }

   private static boolean slotsMatchMaterial(Object var0, int[] var1, Material var2) {
      for(int var6 : var1) {
         MachineScriptHelper.ItemStackInMenu var7 = MachineScriptHelper.getItemInSlot(var0, var6);
         if (var7 == null || var7.stack().getType() != var2) {
            return false;
         }
      }

      return true;
   }

   private static ItemStack getItemStack(String var0) {
      SlimefunItem var1 = SlimefunItem.getById(var0);
      return var1 == null ? null : var1.getItem().clone();
   }

   public static int parseInt(String var0, int var1) {
      if (var0 == null) {
         return var1;
      } else {
         try {
            return Integer.parseInt(var0);
         } catch (NumberFormatException var3) {
            return var1;
         }
      }
   }

   private static StructureBlock[] buildCraftStructure() {
      String[] var0 = new String[]{"-5,0,-1,black_concrete", "-5,0,-2,black_concrete", "-4,0,-2,black_concrete", "-4,0,-3,black_concrete", "-4,0,-4,black_concrete", "-3,0,-3,black_concrete", "-3,0,-4,black_concrete", "-2,0,-3,black_concrete", "-2,0,-4,black_concrete", "-2,0,-5,black_concrete", "-1,0,-2,black_concrete", "-1,0,-3,black_concrete", "-1,0,-4,black_concrete", "-1,0,-5,black_concrete", "0,0,-1,black_concrete", "0,0,-2,black_concrete", "0,0,-3,black_concrete", "0,0,-4,black_concrete", "0,0,-5,black_concrete", "1,0,1,black_concrete", "1,0,0,black_concrete", "1,0,-1,black_concrete", "1,0,-2,black_concrete", "1,0,-3,black_concrete", "1,0,-4,black_concrete", "1,0,-5,black_concrete", "2,0,2,black_concrete", "2,0,1,black_concrete", "2,0,0,black_concrete", "2,0,-1,black_concrete", "2,0,-3,black_concrete", "2,0,-4,black_concrete", "2,0,-5,black_concrete", "3,0,2,black_concrete", "3,0,1,black_concrete", "3,0,0,black_concrete", "3,0,-1,black_concrete", "3,0,-2,black_concrete", "3,0,-3,black_concrete", "3,0,-4,black_concrete", "4,0,1,black_concrete", "4,0,0,black_concrete", "4,0,-1,black_concrete", "4,0,-2,black_concrete", "4,0,-3,black_concrete", "4,0,-4,black_concrete", "5,0,0,black_concrete", "5,0,-1,black_concrete", "5,0,-2,black_concrete", "-5,0,0,white_concrete", "-5,0,1,white_concrete", "-5,0,2,white_concrete", "-4,0,-1,white_concrete", "-4,0,0,white_concrete", "-4,0,1,white_concrete", "-4,0,2,white_concrete", "-4,0,3,white_concrete", "-4,0,4,white_concrete", "-3,0,-2,white_concrete", "-3,0,-1,white_concrete", "-3,0,0,white_concrete", "-3,0,1,white_concrete", "-3,0,2,white_concrete", "-3,0,3,white_concrete", "-3,0,4,white_concrete", "-2,0,-2,white_concrete", "-2,0,-1,white_concrete", "-2,0,0,white_concrete", "-2,0,1,white_concrete", "-2,0,3,white_concrete", "-2,0,4,white_concrete", "-2,0,5,white_concrete", "-1,0,-1,white_concrete", "-1,0,0,white_concrete", "-1,0,1,white_concrete", "-1,0,2,white_concrete", "-1,0,3,white_concrete", "-1,0,4,white_concrete", "-1,0,5,white_concrete", "0,0,1,white_concrete", "0,0,2,white_concrete", "0,0,3,white_concrete", "0,0,4,white_concrete", "0,0,5,white_concrete", "1,0,2,white_concrete", "1,0,3,white_concrete", "1,0,4,white_concrete", "1,0,5,white_concrete", "2,0,3,white_concrete", "2,0,4,white_concrete", "2,0,5,white_concrete", "3,0,3,white_concrete", "3,0,4,white_concrete", "4,0,2,white_concrete", "4,0,3,white_concrete", "4,0,4,white_concrete", "5,0,1,white_concrete", "5,0,2,white_concrete", "-2,0,2,black_concrete", "2,0,-2,white_concrete", "-5,5,-5,beacon", "5,5,-5,beacon", "-5,5,5,beacon", "5,5,5,beacon", "-5,5,-6,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,5,-4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-6,5,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-4,5,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,6,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,5,-6,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,5,-4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "6,5,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "4,5,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,6,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,5,6,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,5,4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-6,5,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-4,5,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,6,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,5,6,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,5,4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "6,5,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "4,5,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,6,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,1,-2,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-4,1,-4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-2,1,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,1,-2,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "4,1,-4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "2,1,-5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-5,1,2,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-4,1,4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "-2,1,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "5,1,2,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "4,1,4,amethyst_cluster,KOMUTECH_L_DJ_JPLJ", "2,1,5,amethyst_cluster,KOMUTECH_L_DJ_JPLJ"};
      return parseRows(var0);
   }

   private static StructureBlock[] buildCoreStructure() {
      ArrayList var0 = new ArrayList(Arrays.asList(CRAFT_STRUCTURE));
      String[] var1 = new String[]{"0,0,0,shroomlight,KOMUTECH_L_ZJ_終極合成台核心", "0,1,0,player_head,KOMUTECH_L_ZJ_終極合成台", "-6,0,2,crying_obsidian", "-6,0,1,crying_obsidian", "-6,0,0,crying_obsidian", "-6,0,-1,crying_obsidian", "-6,0,-2,crying_obsidian", "-5,0,4,crying_obsidian", "-5,0,3,crying_obsidian", "-5,0,-4,crying_obsidian", "-5,0,-3,crying_obsidian", "-4,0,5,crying_obsidian", "-4,0,-5,crying_obsidian", "-3,0,5,crying_obsidian", "-3,0,-5,crying_obsidian", "-2,0,6,crying_obsidian", "-2,0,-6,crying_obsidian", "-1,0,6,crying_obsidian", "-1,0,-6,crying_obsidian", "0,0,6,crying_obsidian", "0,0,-6,crying_obsidian", "1,0,6,crying_obsidian", "1,0,-6,crying_obsidian", "2,0,6,crying_obsidian", "2,0,-6,crying_obsidian", "3,0,5,crying_obsidian", "3,0,-5,crying_obsidian", "4,0,5,crying_obsidian", "4,0,-5,crying_obsidian", "5,0,4,crying_obsidian", "5,0,3,crying_obsidian", "5,0,-4,crying_obsidian", "5,0,-3,crying_obsidian", "6,0,2,crying_obsidian", "6,0,1,crying_obsidian", "6,0,0,crying_obsidian", "6,0,-1,crying_obsidian", "6,0,-2,crying_obsidian", "-5,3,-5,end_rod", "-5,4,-5,lantern", "5,3,-5,end_rod", "5,4,-5,lantern", "-5,3,5,end_rod", "-5,4,5,lantern", "5,3,5,end_rod", "5,4,5,lantern"};
      var0.addAll(Arrays.asList(parseRows(var1)));
      return (StructureBlock[])var0.toArray((var0x) -> new StructureBlock[var0x]);
   }

   private static StructureBlock[] parseRows(String[] var0) {
      StructureBlock[] var1 = new StructureBlock[var0.length];

      for(int var2 = 0; var2 < var0.length; ++var2) {
         String[] var3 = var0[var2].split(",");
         var1[var2] = new StructureBlock(Integer.parseInt(var3[0]), Integer.parseInt(var3[1]), Integer.parseInt(var3[2]), var3[3].toLowerCase(Locale.ROOT), var3.length > 4 ? var3[4] : null);
      }

      return var1;
   }

   static {
      ENDER_CHEST = Material.ENDER_CHEST;
      CRAFT_STRUCTURE = buildCraftStructure();
      CORE_STRUCTURE = buildCoreStructure();
      TRAIL_COLORS = new Color[][]{{Color.fromRGB(252, 96, 118), Color.fromRGB(253, 122, 95), Color.fromRGB(255, 154, 68), Color.fromRGB(253, 122, 95), Color.fromRGB(252, 96, 118)}, {Color.fromRGB(251, 194, 235), Color.fromRGB(209, 194, 237), Color.fromRGB(166, 193, 238), Color.fromRGB(209, 194, 237), Color.fromRGB(251, 194, 235)}, {Color.fromRGB(67, 233, 123), Color.fromRGB(62, 241, 169), Color.fromRGB(56, 249, 215), Color.fromRGB(62, 241, 169), Color.fromRGB(67, 233, 123)}};
   }

   public static record StructureBlock(int x, int y, int z, String material, String sfId) {
   }

   public static record StructureCheck(boolean valid, List<String> errors) {
   }

   public static record Rotated(int x, int z) {
   }

   private static record SlotRequirement(int slot, String slimefunId, Material material, int amount) {
      static SlotRequirement sf(int var0, String var1, int var2) {
         return new SlotRequirement(var0, var1, (Material)null, var2);
      }

      static SlotRequirement mat(int var0, Material var1, int var2) {
         return new SlotRequirement(var0, (String)null, var1, var2);
      }
   }

   private static record MatchedRecipe(String outputId, long spiritReq, SlotRequirement[] consumes) {
   }
}
