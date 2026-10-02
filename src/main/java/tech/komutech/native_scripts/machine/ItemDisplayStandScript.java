package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class ItemDisplayStandScript implements NativeScript {
   private static final String MACHINE_ID = "KOMUTECH_JZ_JCJQ_物品展台";
   private static final NamespacedKey MACHINE_KEY = new NamespacedKey("komutech", "wpzt");
   private static final int ITEM_SLOT = 13;
   private static final int STATUS_SLOT = 4;
   private static final int MANUAL_SLOT = 8;
   private static final int OPEN_SLOT = 22;
   private static final int CLOSE_SLOT = 26;
   private static final int MOVE_UP_SLOT = 0;
   private static final int MOVE_DOWN_SLOT = 18;
   private static final int YAW_LEFT_SLOT = 1;
   private static final int YAW_RIGHT_SLOT = 19;
   private static final int PITCH_DOWN_SLOT = 2;
   private static final int PITCH_UP_SLOT = 20;
   private static final int ROLL_LEFT_SLOT = 3;
   private static final int ROLL_RIGHT_SLOT = 21;
   private static final int RESET_POS_SLOT = 9;
   private static final int RESET_YAW_SLOT = 10;
   private static final int RESET_PITCH_SLOT = 11;
   private static final int RESET_ROLL_SLOT = 12;
   private static final int GLOW_SLOT = 6;
   private static final int NAME_SLOT = 24;
   private static final double MOVE_STEP = 0.1;
   private static final float ANGLE_STEP = 5.0F;
   private static final Map<String, ItemDisplay> PROJECTIONS = new HashMap<>();
   private static final Map<String, Float> PROJ_YAW = new HashMap<>();
   private static final Map<String, Float> PROJ_PITCH = new HashMap<>();
   private static final Map<String, Float> PROJ_ROLL = new HashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onOpen".equals(var1) && var2[0] instanceof Player var3) {
         this.onOpen(var3);
      } else if ("onClose".equals(var1) && var2[0] instanceof Player var4) {
         this.onClose(var4);
      } else if ("onClick".equals(var1) && var2.length >= 2 && var2[0] instanceof Player var5) {
         int var7 = ((Integer)var2[1]).intValue();
         this.onClick(var5, var7);
         // 13 号槽为展示物品槽，必须保持可交互；其余槽位都是功能按钮，
         // 返回 false 取消点击事件，避免按钮被玩家直接拿走。
         return var7 == 13 ? null : Boolean.FALSE;
      } else if ("onBreak".equals(var1) && var2[0] instanceof BlockBreakEvent var6) {
         removeProjection(var6.getBlock());
      }

      return null;
   }

   private void onOpen(Player var1) {
      Inventory var2 = var1.getOpenInventory().getTopInventory();
      Block var3 = getMachineFromStatus(var2);
      if (var3 == null || !isValidMachine(var3)) {
         var3 = getTargetBlock(var1);
         if (var3 == null) {
            KomutechSupport.send(var1, "§c错误：请瞄准要操作的机器！");
            return;
         }

         if (!isValidMachine(var3)) {
            KomutechSupport.send(var1, "§c错误：瞄准的方块不是可操作的机器！");
            return;
         }
      } else if (!restoreFromStatus(var2, var3)) {
         ItemDisplay var4 = findProjectionByMachine(var3);
         if (var4 != null) {
            String var5 = locationKey(var3);
            PROJECTIONS.put(var5, var4);
            PROJ_YAW.put(var5, 0.0F);
            PROJ_PITCH.put(var5, 0.0F);
            PROJ_ROLL.put(var5, 0.0F);
         }
      }

      refreshButtons(var2, var3);
   }

   private void onClose(Player var1) {
      Inventory var2 = var1.getOpenInventory().getTopInventory();
      Block var3 = getMachineFromStatus(var2);
      if (var3 != null) {
         String var4 = locationKey(var3);
         ItemDisplay var5 = PROJECTIONS.get(var4);
         if (var5 != null && !var5.isDead()) {
            ItemStack var6 = var2.getItem(13);
            ItemStack var7 = var5.getItemStack();
            if (var6 == null || var6.getType().isAir() || !var6.isSimilar(var7)) {
               var5.remove();
               PROJECTIONS.remove(var4);
               PROJ_YAW.remove(var4);
               PROJ_PITCH.remove(var4);
               PROJ_ROLL.remove(var4);
               var2.setItem(4, null);
            }
         }
      }
   }

   private void onClick(Player var1, int var2) {
      Inventory var3 = var1.getOpenInventory().getTopInventory();
      if (var2 == 4) {
         KomutechSupport.send(var1, "§c不能操作状态槽");
      } else {
         Block var4 = getMachineFromStatus(var3);
         if (var4 == null) {
            KomutechSupport.send(var1, "§c错误：未检测到机器，请瞄准后重新打开菜单");
         } else if (!isValidMachine(var4)) {
            KomutechSupport.send(var1, "§c错误：机器失效，请重新打开菜单");
         } else {
            String var5 = locationKey(var4);
            ItemDisplay var6 = PROJECTIONS.get(var5);
            if (var2 == 8) {
               Block var16 = getTargetBlock(var1);
               if (var16 == null) {
                  KomutechSupport.send(var1, "§c请先瞄准机器");
               } else if (!isValidMachine(var16)) {
                  KomutechSupport.send(var1, "§c瞄准的方块不是机器");
               } else {
                  String var19 = locationKey(var16);
                  if (!var19.equals(var5)) {
                     var4 = var16;
                     var5 = var19;
                     var6 = PROJECTIONS.get(var19);
                     KomutechSupport.send(var1, "§a已切换到新机器");
                  }

                  ItemDisplay var22 = findProjectionByMachine(var4);
                  if (var22 != null) {
                     PROJECTIONS.put(var5, var22);
                     PROJ_YAW.put(var5, 0.0F);
                     PROJ_PITCH.put(var5, 0.0F);
                     PROJ_ROLL.put(var5, 0.0F);
                     KomutechSupport.send(var1, "§a找到并绑定投影");
                  } else {
                     KomutechSupport.send(var1, "§e未找到投影，可点击开启");
                  }

                  saveStatusToSlot(var3, var4);
                  refreshButtons(var3, var4);
               }
            } else if (var2 == 22) {
               ItemStack var15 = var3.getItem(13);
               if (var15 != null && !var15.getType().isAir()) {
                  if (var6 != null && !var6.isDead()) {
                     var6.remove();
                  }

                  try {
                     var6 = createProjection(var4, var15);
                     PROJECTIONS.put(var5, var6);
                     PROJ_YAW.put(var5, 0.0F);
                     PROJ_PITCH.put(var5, 0.0F);
                     PROJ_ROLL.put(var5, 0.0F);
                     KomutechSupport.send(var1, "§a投影已开启");
                  } catch (RuntimeException var10) {
                     KomutechSupport.send(var1, "§c开启失败");
                  }

                  saveStatusToSlot(var3, var4);
                  refreshButtons(var3, var4);
               } else {
                  KomutechSupport.send(var1, "§c请放入物品");
               }
            } else if (var2 == 26) {
               if (var6 != null && !var6.isDead()) {
                  var6.remove();
               }

               PROJECTIONS.remove(var5);
               PROJ_YAW.remove(var5);
               PROJ_PITCH.remove(var5);
               PROJ_ROLL.remove(var5);
               var3.setItem(4, null);
               KomutechSupport.send(var1, "§a投影已关闭");
               refreshButtons(var3, var4);
            } else if (var6 != null && !var6.isDead()) {
               float var7 = PROJ_YAW.getOrDefault(var5, 0.0F);
               float var8 = PROJ_PITCH.getOrDefault(var5, 0.0F);
               float var9 = PROJ_ROLL.getOrDefault(var5, 0.0F);
               if (var2 == 0) {
                  var6.teleport(var6.getLocation().add(0.0, 0.1, 0.0));
                  KomutechSupport.send(var1, "§a向上移动 0.1 格");
               } else if (var2 == 18) {
                  var6.teleport(var6.getLocation().add(0.0, -0.1, 0.0));
                  KomutechSupport.send(var1, "§a向下移动 0.1 格");
               } else if (var2 == 1) {
                  var7 = updateAngle(var7, -5.0F);
                  PROJ_YAW.put(var5, var7);
                  applyRotation(var6, var7, var8, var9);
                  KomutechSupport.send(var1, "§a向左旋转 5°，当前偏航 " + (int)var7);
               } else if (var2 == 19) {
                  var7 = updateAngle(var7, 5.0F);
                  PROJ_YAW.put(var5, var7);
                  applyRotation(var6, var7, var8, var9);
                  KomutechSupport.send(var1, "§a向右旋转 5°，当前偏航 " + (int)var7);
               } else if (var2 == 2) {
                  var8 = updateAngle(var8, -5.0F);
                  PROJ_PITCH.put(var5, var8);
                  applyRotation(var6, var7, var8, var9);
                  KomutechSupport.send(var1, "§a向下俯仰 5°，当前俯仰 " + (int)var8);
               } else if (var2 == 20) {
                  var8 = updateAngle(var8, 5.0F);
                  PROJ_PITCH.put(var5, var8);
                  applyRotation(var6, var7, var8, var9);
                  KomutechSupport.send(var1, "§a向上俯仰 5°，当前俯仰 " + (int)var8);
               } else if (var2 == 3) {
                  var9 = updateAngle(var9, -5.0F);
                  PROJ_ROLL.put(var5, var9);
                  applyRotation(var6, var7, var8, var9);
                  KomutechSupport.send(var1, "§a向左翻滚 5°，当前翻滚 " + (int)var9);
               } else if (var2 == 21) {
                  var9 = updateAngle(var9, 5.0F);
                  PROJ_ROLL.put(var5, var9);
                  applyRotation(var6, var7, var8, var9);
                  KomutechSupport.send(var1, "§a向右翻滚 5°，当前翻滚 " + (int)var9);
               } else if (var2 == 9) {
                  var6.teleport(var4.getLocation().clone().add(0.5, 2.0, 0.5));
                  KomutechSupport.send(var1, "§a位置已重置");
               } else if (var2 == 10) {
                  PROJ_YAW.put(var5, 0.0F);
                  applyRotation(var6, 0.0F, var8, var9);
                  KomutechSupport.send(var1, "§a偏航已重置");
               } else if (var2 == 11) {
                  PROJ_PITCH.put(var5, 0.0F);
                  applyRotation(var6, var7, 0.0F, var9);
                  KomutechSupport.send(var1, "§a俯仰已重置");
               } else if (var2 == 12) {
                  PROJ_ROLL.put(var5, 0.0F);
                  applyRotation(var6, var7, var8, 0.0F);
                  KomutechSupport.send(var1, "§a翻滚已重置");
               } else if (var2 == 6) {
                  var6.setGlowing(!var6.isGlowing());
                  KomutechSupport.send(var1, var6.isGlowing() ? "§a发光开启" : "§7发光关闭");
               } else if (var2 == 24) {
                  var6.setCustomNameVisible(!var6.isCustomNameVisible());
                  KomutechSupport.send(var1, var6.isCustomNameVisible() ? "§a名称显示" : "§7名称隐藏");
               }

               saveStatusToSlot(var3, var4);
               refreshButtons(var3, var4);
            } else {
               KomutechSupport.send(var1, "§c请先开启投影");
            }
         }
      }
   }

   private static ItemDisplay createProjection(Block var0, ItemStack var1) {
      Location var2 = var0.getLocation().clone().add(0.5, 2.0, 0.5);
      ItemDisplay var3 = (ItemDisplay)var0.getWorld().spawn(var2, ItemDisplay.class, var2x -> {
         var2x.setItemStack(var1.clone());
         var2x.setInvulnerable(true);
         var2x.setGravity(false);
         var2x.setBrightness(new Brightness(15, 15));
         ItemMeta var3x = var1.getItemMeta();
         var2x.setCustomName(var3x != null && var3x.hasDisplayName() ? var3x.getDisplayName() : var1.getType().name().toLowerCase().replace('_', ' '));
         var2x.setCustomNameVisible(false);
         var2x.getPersistentDataContainer().set(MACHINE_KEY, PersistentDataType.STRING, locationKey(var0));
      });
      applyRotation(var3, 0.0F, 0.0F, 0.0F);
      return var3;
   }

   private static void applyRotation(ItemDisplay var0, float var1, float var2, float var3) {
      try {
         Class var4 = Class.forName("org.joml.Vector3f");
         Class var5 = Class.forName("org.joml.Quaternionf");
         Class var6 = Class.forName("org.joml.AxisAngle4f");
         Object var7 = var5.getConstructor().newInstance();
         var5.getMethod("rotateY", float.class).invoke(var7, (float)Math.toRadians(var1));
         var5.getMethod("rotateX", float.class).invoke(var7, (float)Math.toRadians(var2));
         var5.getMethod("rotateZ", float.class).invoke(var7, (float)Math.toRadians(var3));
         Object var8 = var4.getConstructor(float.class, float.class, float.class).newInstance(0.0F, 0.0F, 0.0F);
         Object var9 = var4.getConstructor(float.class, float.class, float.class).newInstance(1.0F, 1.0F, 1.0F);
         Object var10 = var6.getConstructor(float.class, float.class, float.class, float.class).newInstance(0.0F, 0.0F, 1.0F, 0.0F);
         Transformation var11 = Transformation.class.getConstructor(var4, var5, var4, var6).newInstance(var8, var7, var9, var10);
         var0.setTransformation(var11);
      } catch (ReflectiveOperationException var12) {
         var0.setRotation(var1, var2);
      }
   }

   private static float updateAngle(float var0, float var1) {
      return (var0 + var1 + 360.0F) % 360.0F;
   }

   private static void removeProjection(Block var0) {
      String var1 = locationKey(var0);
      ItemDisplay var2 = PROJECTIONS.get(var1);
      if (var2 != null && !var2.isDead()) {
         var2.remove();
      }

      PROJECTIONS.remove(var1);
      PROJ_YAW.remove(var1);
      PROJ_PITCH.remove(var1);
      PROJ_ROLL.remove(var1);
   }

   private static boolean hasProjection(Block var0) {
      String var1 = locationKey(var0);
      ItemDisplay var2 = PROJECTIONS.get(var1);
      return var2 != null && !var2.isDead();
   }

   private static ItemDisplay findProjectionByMachine(Block var0) {
      String var1 = locationKey(var0);

      for (Entity var3 : var0.getWorld().getEntities()) {
         if (var3 instanceof ItemDisplay var4 && !var4.isDead()) {
            String var5 = (String)var4.getPersistentDataContainer().get(MACHINE_KEY, PersistentDataType.STRING);
            if (var1.equals(var5)) {
               return var4;
            }
         }
      }

      return null;
   }

   private static Block getTargetBlock(Player var0) {
      RayTraceResult var1 = var0.getWorld().rayTraceBlocks(var0.getEyeLocation(), var0.getEyeLocation().getDirection(), 5.0, FluidCollisionMode.NEVER, true);
      return var1 == null ? null : var1.getHitBlock();
   }

   private static boolean isValidMachine(Block var0) {
      if (var0 == null) {
         return false;
      } else {
         SlimefunItem var1 = MachineScriptHelper.getSfItem(var0.getLocation());
         return var1 != null && "KOMUTECH_JZ_JCJQ_物品展台".equals(var1.getId());
      }
   }

   private static String locationKey(Block var0) {
      Location var1 = var0.getLocation();
      return var1.getWorld().getName() + "," + var1.getBlockX() + "," + var1.getBlockY() + "," + var1.getBlockZ();
   }

   private static String getMachineKeyFromStatus(Inventory var0) {
      ItemStack var1 = var0.getItem(4);
      if (var1 != null && var1.getType() == Material.PAPER && var1.hasItemMeta()) {
         List var2 = var1.getItemMeta().getLore();
         if (var2 != null && !var2.isEmpty()) {
            try {
               return ((String)var2.get(0)).replaceAll("§[0-9a-fklmnor]", "").substring(5).trim();
            } catch (RuntimeException var4) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Block getMachineFromStatus(Inventory var0) {
      String var1 = getMachineKeyFromStatus(var0);
      if (var1 == null) {
         return null;
      } else {
         String[] var2 = var1.split(",");
         if (var2.length != 4) {
            return null;
         } else {
            World var3 = Bukkit.getWorld(var2[0]);
            return var3 == null ? null : var3.getBlockAt(Integer.parseInt(var2[1]), Integer.parseInt(var2[2]), Integer.parseInt(var2[3]));
         }
      }
   }

   private static void saveStatusToSlot(Inventory var0, Block var1) {
      String var2 = locationKey(var1);
      boolean var3 = hasProjection(var1);
      ItemStack var4 = new ItemStack(Material.PAPER);
      ItemMeta var5 = var4.getItemMeta();
      var5.setDisplayName("§7机器状态");
      ArrayList var6 = new ArrayList();
      var6.add("§b机器位置: " + var2);
      if (var3) {
         ItemDisplay var7 = PROJECTIONS.get(var2);
         var6.add("§bUUID: " + var7.getUniqueId());
         var6.add("§b偏航: " + PROJ_YAW.getOrDefault(var2, 0.0F));
         var6.add("§b俯仰: " + PROJ_PITCH.getOrDefault(var2, 0.0F));
         var6.add("§b翻滚: " + PROJ_ROLL.getOrDefault(var2, 0.0F));
         var6.add("§b发光: " + var7.isGlowing());
         var6.add("§b名称显示: " + var7.isCustomNameVisible());
      } else {
         var6.add("§7无投影");
      }

      var5.setLore(var6);
      var4.setItemMeta(var5);
      var0.setItem(4, var4);
   }

   private static boolean restoreFromStatus(Inventory var0, Block var1) {
      ItemStack var2 = var0.getItem(4);
      if (var2 != null && var2.getType() == Material.PAPER && var2.hasItemMeta()) {
         List<String> var3 = var2.getItemMeta().getLore();
         if (var3 != null && var3.size() >= 2 && !"无投影".equals(((String)var3.get(1)).replaceAll("§[0-9a-fklmnor]", ""))) {
            try {
               String var4 = ((String)var3.get(0)).replaceAll("§[0-9a-fklmnor]", "").substring(5).trim();
               if (!locationKey(var1).equals(var4)) {
                  return false;
               } else {
                  String var5 = null;
                  float var6 = 0.0F;
                  float var7 = 0.0F;
                  float var8 = 0.0F;
                  boolean var9 = false;
                  boolean var10 = false;

                  for (String var12 : var3) {
                     String var13 = var12.replaceAll("§[0-9a-fklmnor]", "");
                     if (var13.startsWith("UUID:")) {
                        var5 = var13.substring(5).trim();
                     } else if (var13.startsWith("偏航:")) {
                        var6 = Float.parseFloat(var13.substring(3).trim());
                     } else if (var13.startsWith("俯仰:")) {
                        var7 = Float.parseFloat(var13.substring(3).trim());
                     } else if (var13.startsWith("翻滚:")) {
                        var8 = Float.parseFloat(var13.substring(3).trim());
                     } else if (var13.startsWith("发光:")) {
                        var9 = "true".equals(var13.substring(3).trim());
                     } else if (var13.startsWith("名称显示:")) {
                        var10 = "true".equals(var13.substring(5).trim());
                     }
                  }

                  if (var5 == null) {
                     return false;
                  } else if (Bukkit.getEntity(UUID.fromString(var5)) instanceof ItemDisplay var16 && !var16.isDead()) {
                     PROJECTIONS.put(var4, var16);
                     PROJ_YAW.put(var4, var6);
                     PROJ_PITCH.put(var4, var7);
                     PROJ_ROLL.put(var4, var8);
                     if (var9) {
                        var16.setGlowing(true);
                     }

                     var16.setCustomNameVisible(var10);
                     return true;
                  } else {
                     return false;
                  }
               }
            } catch (RuntimeException var14) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void refreshButtons(Inventory var0, Block var1) {
      ItemStack var2 = var0.getItem(13);
      boolean var3 = hasProjection(var1);
      String var4 = var3 ? locationKey(var1) : null;
      updateLore(var0, 22, List.of("§7点击将13号槽物品投影", "§7当前物品: " + getItemDisplayName(var2), var3 ? "§a投影已开启" : "§7投影未开启"));
      updateLore(var0, 26, List.of("§7点击关闭投影", var3 ? "§a点击关闭" : "§7无投影"));
      updateLore(var0, 0, List.of("§7向上移动" + (var3 ? "" : " §c(需先开启)")));
      updateLore(var0, 18, List.of("§7向下移动" + (var3 ? "" : " §c(需先开启)")));
      updateLore(var0, 1, List.of("§7向左旋转" + (var3 ? " §a当前:" + PROJ_YAW.getOrDefault(var4, 0.0F) : " §c(需先开启)")));
      updateLore(var0, 19, List.of("§7向右旋转" + (var3 ? " §a当前:" + PROJ_YAW.getOrDefault(var4, 0.0F) : " §c(需先开启)")));
      updateLore(var0, 2, List.of("§7向下俯仰" + (var3 ? " §a当前:" + PROJ_PITCH.getOrDefault(var4, 0.0F) : " §c(需先开启)")));
      updateLore(var0, 20, List.of("§7向上俯仰" + (var3 ? " §a当前:" + PROJ_PITCH.getOrDefault(var4, 0.0F) : " §c(需先开启)")));
      updateLore(var0, 3, List.of("§7向左翻滚" + (var3 ? " §a当前:" + PROJ_ROLL.getOrDefault(var4, 0.0F) : " §c(需先开启)")));
      updateLore(var0, 21, List.of("§7向右翻滚" + (var3 ? " §a当前:" + PROJ_ROLL.getOrDefault(var4, 0.0F) : " §c(需先开启)")));
      updateLore(var0, 9, List.of("§7重置位置" + (var3 ? "" : " §c(需先开启)")));
      updateLore(var0, 10, List.of("§7重置偏航" + (var3 ? "" : " §c(需先开启)")));
      updateLore(var0, 11, List.of("§7重置俯仰" + (var3 ? "" : " §c(需先开启)")));
      updateLore(var0, 12, List.of("§7重置翻滚" + (var3 ? "" : " §c(需先开启)")));
      String var5 = var3 ? (PROJECTIONS.get(var4).isGlowing() ? "§a开启" : "§7关闭") : "§7关闭";
      updateLore(var0, 6, List.of("§7切换发光", "§7当前: " + var5 + (var3 ? "" : " §c(需先开启)")));
      String var6 = var3 ? (PROJECTIONS.get(var4).isCustomNameVisible() ? "§a显示" : "§7隐藏") : "§7隐藏";
      updateLore(var0, 24, List.of("§7切换名称", "§7当前: " + var6 + (var3 ? "" : " §c(需先开启)")));
      updateLore(var0, 8, List.of("§7手动检测并绑定当前瞄准的机器"));
      if (var1 != null) {
         saveStatusToSlot(var0, var1);
      }
   }

   private static void updateLore(Inventory var0, int var1, List<String> var2) {
      ItemStack var3 = var0.getItem(var1);
      if (var3 != null) {
         ItemMeta var4 = var3.getItemMeta();
         var4.setLore(var2);
         var3.setItemMeta(var4);
      }
   }

   private static String getItemDisplayName(ItemStack var0) {
      if (var0 != null && !var0.getType().isAir()) {
         ItemMeta var1 = var0.getItemMeta();
         return var1 != null && var1.hasDisplayName()
            ? var1.getDisplayName() + " §7(" + var0.getType().name() + ")"
            : "§e" + var0.getType().name().toLowerCase().replace('_', ' ');
      } else {
         return "§c空";
      }
   }
}
