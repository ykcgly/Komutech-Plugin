package tech.komutech.native_scripts.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.ChatInputService;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class SpiritItemAdjusterScript implements NativeScript {
   private static final String[] EDIT_MODES = new String[]{"功德值", "品阶", "灵力最大值", "熟练度"};
   private static final String[] RANK_KEYWORDS = new String[]{"黄阶", "玄阶", "地阶", "天阶", "仙阶"};
   private static final long DOUBLE_CLICK_THRESHOLD = 500L;
   private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();
   private static final Map<UUID, SpiritItemAdjusterScript.PlayerSession> SESSIONS = new ConcurrentHashMap<>();
   private static final Pattern MERIT = Pattern.compile("§[bc]功德?值：§6(\\d+)");
   private static final Pattern DEMERIT = Pattern.compile("§c缺德值：§6(\\d+)");
   private static final Pattern SPIRIT = Pattern.compile("§b灵力剩余：§6\\d+ §7/ §6(\\d+)");
   private static final Pattern PROF = Pattern.compile("§b熟练度：§6(\\d+) §7/ §6(\\d+)");
   private static final Map<String, SpiritItemAdjusterScript.RankConfig> RANKS = Map.of(
      "黄阶",
      new SpiritItemAdjusterScript.RankConfig("§e§l黄阶", 500),
      "玄阶",
      new SpiritItemAdjusterScript.RankConfig("§8§l玄阶", 1500),
      "地阶",
      new SpiritItemAdjusterScript.RankConfig("§d§l地阶", 3000),
      "天阶",
      new SpiritItemAdjusterScript.RankConfig("§4§l天阶", 10000),
      "仙阶",
      new SpiritItemAdjusterScript.RankConfig("§d§l仙阶", 20000)
   );

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         return UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
      } else {
         if ("onCommand".equals(var1) && var2.length >= 3) {
            Object var3 = var2[0];
            Object var4 = var2[1];
            if (var3 instanceof CommandSender var5 && var4 instanceof Command var6 && "itemeditor".equalsIgnoreCase(var6.getName())) {
               if (var5 instanceof Player var7) {
                  showHelp(var7);
               } else {
                  var5.sendMessage("§6===== 物品属性调整器 =====");
                  var5.sendMessage("§e请在游戏中使用此命令。");
               }

               return true;
            }
         }

         return null;
      }
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      UUID var3 = var2.getUniqueId();
      long var4 = System.currentTimeMillis();
      long var6 = LAST_CLICK.getOrDefault(var3, 0L);
      if (var4 - var6 <= 500L) {
         resetPlayerState(var2);
         LAST_CLICK.put(var3, var4);
         return null;
      } else {
         LAST_CLICK.put(var3, var4);
         SpiritItemAdjusterScript.PlayerSession var8 = SESSIONS.get(var3);
         if (var8 != null && var8.awaitingInput) {
            KomutechSupport.send(var2, "§c⚠️ 请先完成当前输入或输入 'cancel' 取消");
            return null;
         } else {
            if (var2.isSneaking()) {
               if (var8 == null || var8.editMode == null) {
                  this.handleFirstInput(var2);
               } else if (var8.targetValue == null) {
                  this.handleSecondInput(var2, var8.editMode);
               } else {
                  KomutechSupport.send(var2, "§a检测到已设置目标值，自动进入第一次选择");
                  SESSIONS.remove(var3);
                  this.handleFirstInput(var2);
               }
            } else {
               this.applyToOffhand(var2);
            }

            return null;
         }
      }
   }

   private void handleFirstInput(Player var1) {
      KomutechSupport.send(var1, "§6请选择要修改的属性:");
      KomutechSupport.send(var1, "§e功德值 §7| §e品阶 §7| §e灵力最大值 §7| §e熟练度");
      KomutechSupport.send(var1, "§a输入属性名称，或输入 'help' 查看帮助");
      setAwaiting(var1, true);
      ChatInputService.request(var1, "§e>", var1x -> {
         setAwaiting(var1, false);
         var1x = var1x.trim();
         if ("cancel".equalsIgnoreCase(var1x)) {
            KomutechSupport.send(var1, "§a✅ 已取消操作");
         } else if ("help".equalsIgnoreCase(var1x)) {
            showHelp(var1);
         } else {
            String var2 = null;

            for (String var6 : EDIT_MODES) {
               if (var1x.contains(var6)) {
                  var2 = var6;
                  break;
               }
            }

            if (var2 == null) {
               KomutechSupport.send(var1, "§c❌ 不支持的属性！请从以下选择:");
               KomutechSupport.send(var1, "§e功德值 §7| §e品阶 §7| §e灵力最大值 §7| §e熟练度");
            } else {
               SpiritItemAdjusterScript.PlayerSession var8 = new SpiritItemAdjusterScript.PlayerSession(var2, null, false);
               SESSIONS.put(var1.getUniqueId(), var8);
               KomutechSupport.send(var1, "§a✅ 已选择: §6" + var2);
               if ("品阶".equals(var2)) {
                  KomutechSupport.send(var1, "§a请再次蹲下右键，输入目标品阶 (黄阶/玄阶/地阶/天阶/仙阶)");
               } else {
                  KomutechSupport.send(var1, "§a请再次蹲下右键，输入目标" + var2 + "数值");
               }
            }
         }
      });
   }

   private void handleSecondInput(Player var1, String var2) {
      if ("品阶".equals(var2)) {
         KomutechSupport.send(var1, "§6请输入目标品阶: 黄阶/玄阶/地阶/天阶/仙阶");
      } else {
         KomutechSupport.send(var1, "§6请输入目标" + var2 + ":");
      }

      setAwaiting(var1, true);
      ChatInputService.request(var1, "§e>", var2x -> {
         setAwaiting(var1, false);
         var2x = var2x.trim();
         if ("cancel".equalsIgnoreCase(var2x)) {
            KomutechSupport.send(var1, "§a✅ 已取消操作");
            SESSIONS.remove(var1.getUniqueId());
         } else {
            SpiritItemAdjusterScript.PlayerSession var3 = SESSIONS.get(var1.getUniqueId());
            if (var3 != null) {
               Object var4;
               if ("品阶".equals(var2)) {
                  String var5 = null;

                  for (String var9 : RANK_KEYWORDS) {
                     if (var2x.contains(var9)) {
                        var5 = var9;
                        break;
                     }
                  }

                  if (var5 == null) {
                     KomutechSupport.send(var1, "§c❌ 无效的品阶！请输入: 黄阶/玄阶/地阶/天阶/仙阶");
                     return;
                  }

                  var4 = var5;
               } else {
                  try {
                     int var12 = Integer.parseInt(var2x);
                     if (!"功德值".equals(var2) && var12 < 0) {
                        KomutechSupport.send(var1, "§c❌ 请输入非负整数！");
                        return;
                     }

                     var4 = var12;
                  } catch (NumberFormatException var10) {
                     KomutechSupport.send(var1, "§c❌ 请输入有效的数字！");
                     return;
                  }
               }

               var3.targetValue = var4;
               KomutechSupport.send(var1, "§a✅ 已设置: §6" + var2 + " = " + var4);
               KomutechSupport.send(var1, "§a请使用普通右键应用到副手物品");
            }
         }
      });
   }

   private void applyToOffhand(Player var1) {
      ItemStack var2 = var1.getInventory().getItemInOffHand();
      if (var2 != null && !var2.getType().isAir()) {
         SpiritItemAdjusterScript.PlayerSession var3 = SESSIONS.get(var1.getUniqueId());
         if (var3 != null && var3.editMode != null && var3.targetValue != null) {
            String var4 = var3.editMode;
            switch (var4) {
               case "功德值":
                  int var11 = (Integer)var3.targetValue;
                  int var14 = parseMeritValue(var2);
                  if (setMeritValue(var2, var11)) {
                     KomutechSupport.send(var1, "§a✅ 功德值调整成功！\n§6原值: " + formatMerit(var14) + " §6→ §e" + formatMerit(var11));
                  } else {
                     KomutechSupport.send(var1, "§c❌ 功德值调整失败！物品数据异常");
                  }
                  break;
               case "品阶":
                  String var10 = (String)var3.targetValue;
                  String var13 = parseItemRank(var2);
                  if (var13 == null) {
                     cleanup(var1, "§c❌ 物品无品阶属性！");
                     return;
                  }

                  if (setItemRank(var2, var10)) {
                     KomutechSupport.send(var1, "§a✅ 品阶调整成功！\n§6原品阶: " + var13 + " §6→ §e" + var10);
                  } else {
                     KomutechSupport.send(var1, "§c❌ 品阶调整失败！物品数据异常");
                  }
                  break;
               case "灵力最大值":
                  int var9 = (Integer)var3.targetValue;
                  int var12 = parseSpiritMaxValue(var2);
                  if (var12 <= 0) {
                     cleanup(var1, "§c❌ 物品无灵力最大值属性！");
                     return;
                  }

                  if (setSpiritMaxValue(var2, var9)) {
                     KomutechSupport.send(var1, "§a✅ 灵力最大值调整成功！\n§6原值: " + var12 + " §6→ §e" + var9);
                  } else {
                     KomutechSupport.send(var1, "§c❌ 灵力最大值调整失败！物品数据异常");
                  }
                  break;
               case "熟练度":
                  int var7 = (Integer)var3.targetValue;
                  SpiritItemAdjusterScript.ProficiencyInfo var8 = parseProficiencyInfo(var2);
                  if (var8.max <= 0) {
                     cleanup(var1, "§c❌ 物品无熟练度属性！");
                     return;
                  }

                  if (var7 > var8.max) {
                     cleanup(var1, "§c❌ 熟练度(" + var7 + ")不能超过最大值(" + var8.max + ")！");
                     return;
                  }

                  if (setProficiency(var2, var7, var8.max)) {
                     KomutechSupport.send(var1, "§a✅ 熟练度调整成功！\n§6原值: " + var8.current + "/" + var8.max + " §6→ §e" + var7 + "/" + var8.max);
                  } else {
                     KomutechSupport.send(var1, "§c❌ 熟练度调整失败！物品数据异常");
                  }
            }

            cleanup(var1, null);
         } else {
            KomutechSupport.send(var1, "§c❌ 未设置修改内容！请先蹲下右键设置");
         }
      } else {
         KomutechSupport.send(var1, "§c❌ 副手未装备物品！请将目标物品放在副手");
      }
   }

   private static void resetPlayerState(Player var0) {
      SESSIONS.remove(var0.getUniqueId());
      KomutechSupport.send(var0, "§a已重置状态，可以重新选择属性");
   }

   private static void cleanup(Player var0, String var1) {
      SESSIONS.remove(var0.getUniqueId());
      if (var1 != null) {
         KomutechSupport.send(var0, var1);
      }
   }

   private static void setAwaiting(Player var0, boolean var1) {
      SESSIONS.compute(var0.getUniqueId(), (var1x, var2) -> {
         if (var2 == null) {
            return new SpiritItemAdjusterScript.PlayerSession(null, null, var1);
         } else {
            var2.awaitingInput = var1;
            return (SpiritItemAdjusterScript.PlayerSession)var2;
         }
      });
   }

   private static void showHelp(Player var0) {
      KomutechSupport.send(var0, "§6===== 物品属性调整器 =====");
      KomutechSupport.send(var0, "§e支持的功能:");
      KomutechSupport.send(var0, "§a功德值 §7- 调整功德/缺德值 (正数=功德，负数=缺德)");
      KomutechSupport.send(var0, "§a品阶 §7- 调整物品品阶 (黄阶/玄阶/地阶/天阶/仙阶)");
      KomutechSupport.send(var0, "§a灵力最大值 §7- 调整灵力最大值");
      KomutechSupport.send(var0, "§a熟练度 §7- 调整熟练度当前值");
      KomutechSupport.send(var0, "§6使用方法:");
      KomutechSupport.send(var0, "§e1. §7首次蹲下右键选择要修改的属性");
      KomutechSupport.send(var0, "§e2. §7再次蹲下右键输入数值/品阶");
      KomutechSupport.send(var0, "§e3. §7普通右键应用到副手物品");
      KomutechSupport.send(var0, "§c输入 'cancel' 可取消当前操作");
      KomutechSupport.send(var0, "§c快速双击右键可强制重置状态");
   }

   private static int parseMeritValue(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 != null && var1.hasLore()) {
         for (String var3 : var1.getLore()) {
            Matcher var4 = MERIT.matcher(var3);
            if (var4.find()) {
               return Integer.parseInt(var4.group(1));
            }

            Matcher var5 = DEMERIT.matcher(var3);
            if (var5.find()) {
               return -Integer.parseInt(var5.group(1));
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static boolean setMeritValue(ItemStack var0, int var1) {
      ItemMeta var2 = var0.getItemMeta();
      if (var2 == null) {
         return false;
      } else {
         ArrayList var3 = var2.hasLore() ? new ArrayList(var2.getLore()) : new ArrayList();
         String var4 = var1 >= 0 ? "§b功德值：§6" + Math.abs(var1) : "§c缺德值：§6" + Math.abs(var1);
         return replaceOrAdd(var3, var4, MERIT, DEMERIT) && save(var0, var2, var3);
      }
   }

   private static String parseItemRank(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 != null && var1.hasLore()) {
         for (String var3 : var1.getLore()) {
            for (String var7 : RANK_KEYWORDS) {
               if (var3.contains(var7)) {
                  return var7;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean setItemRank(ItemStack var0, String var1) {
      SpiritItemAdjusterScript.RankConfig var2 = RANKS.get(var1);
      if (var2 == null) {
         return false;
      } else {
         ItemMeta var3 = var0.getItemMeta();
         if (var3 != null && var3.hasLore()) {
            ArrayList var4 = new ArrayList(var3.getLore());
            boolean var5 = false;

            for (int var6 = 0; var6 < var4.size(); var6++) {
               String var7 = (String)var4.get(var6);
               String var8 = var7;
               if (!var5) {
                  for (SpiritItemAdjusterScript.RankConfig var10 : RANKS.values()) {
                     if (var7.contains(var10.display)) {
                        var8 = var7.replace(var10.display, var2.display);
                        var5 = true;
                        break;
                     }
                  }
               }

               if (var7.contains("§7/ §6")) {
                  var8 = var8.replaceAll("(§7/ §6)\\d+", "$1" + var2.profMax);
               }

               if (!var8.equals(var7)) {
                  var4.set(var6, var8);
               }
            }

            return var5 && save(var0, var3, var4);
         } else {
            return false;
         }
      }
   }

   private static int parseSpiritMaxValue(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 != null && var1.hasLore()) {
         for (String var3 : var1.getLore()) {
            Matcher var4 = SPIRIT.matcher(var3);
            if (var4.find()) {
               return Integer.parseInt(var4.group(1));
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static boolean setSpiritMaxValue(ItemStack var0, int var1) {
      ItemMeta var2 = var0.getItemMeta();
      if (var2 != null && var2.hasLore()) {
         ArrayList var3 = new ArrayList(var2.getLore());
         boolean var4 = false;

         for (int var5 = 0; var5 < var3.size(); var5++) {
            if (SPIRIT.matcher((CharSequence)var3.get(var5)).find()) {
               var3.set(var5, ((String)var3.get(var5)).replaceAll("(§7/ §6)\\d+", "$1" + var1));
               var4 = true;
            }
         }

         return var4 && save(var0, var2, var3);
      } else {
         return false;
      }
   }

   private static SpiritItemAdjusterScript.ProficiencyInfo parseProficiencyInfo(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 != null && var1.hasLore()) {
         for (String var3 : var1.getLore()) {
            Matcher var4 = PROF.matcher(var3);
            if (var4.find()) {
               return new SpiritItemAdjusterScript.ProficiencyInfo(Integer.parseInt(var4.group(1)), Integer.parseInt(var4.group(2)));
            }
         }

         return new SpiritItemAdjusterScript.ProficiencyInfo(0, 0);
      } else {
         return new SpiritItemAdjusterScript.ProficiencyInfo(0, 0);
      }
   }

   private static boolean setProficiency(ItemStack var0, int var1, int var2) {
      ItemMeta var3 = var0.getItemMeta();
      if (var3 != null && var3.hasLore()) {
         ArrayList var4 = new ArrayList(var3.getLore());

         for (int var5 = 0; var5 < var4.size(); var5++) {
            if (PROF.matcher((CharSequence)var4.get(var5)).find()) {
               var4.set(var5, "§b熟练度：§6" + var1 + " §7/ §6" + var2);
               return save(var0, var3, var4);
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean replaceOrAdd(List<String> var0, String var1, Pattern... var2) {
      for (int var3 = 0; var3 < var0.size(); var3++) {
         for (Pattern var7 : var2) {
            if (var7.matcher((CharSequence)var0.get(var3)).find()) {
               var0.set(var3, var1);
               return true;
            }
         }
      }

      var0.add(var1);
      return true;
   }

   private static boolean save(ItemStack var0, ItemMeta var1, List<String> var2) {
      var1.setLore(var2);
      var0.setItemMeta(var1);
      return true;
   }

   private static String formatMerit(int var0) {
      return var0 >= 0 ? String.valueOf(var0) : "缺德" + Math.abs(var0);
   }

   private static final class PlayerSession {
      String editMode;
      Object targetValue;
      boolean awaitingInput;

      PlayerSession(String var1, Object var2, boolean var3) {
         this.editMode = var1;
         this.targetValue = var2;
         this.awaitingInput = var3;
      }
   }

   private record ProficiencyInfo(int current, int max) {
   }

   private record RankConfig(String display, int profMax) {
   }
}
