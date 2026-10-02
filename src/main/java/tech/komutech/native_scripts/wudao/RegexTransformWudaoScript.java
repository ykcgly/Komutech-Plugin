package tech.komutech.native_scripts.wudao;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechMenuHelper;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class RegexTransformWudaoScript implements NativeScript {
   private static final long TIME = 15000L;
   private static final long INTERVAL = 3000L;
   private static final int MAX_EMPTY = 5;
   private final String machineId;
   private final String southMachineId;
   private final int targetSlot;
   private final int startSlot;
   private final int endSlot;
   private final int baseAmount;
   private final Pattern pattern;
   private final String replacement;
   private final String startMsg;
   private final String halfMsg;
   private final String finalMsg;
   private final String successMsg;
   private final String placeMsg;
   private final Map<String, RegexTransformWudaoScript.State> states = new HashMap<>();

   public RegexTransformWudaoScript(String var1, String var2, Pattern var3, String var4, String var5, String var6, String var7, String var8, String var9) {
      this(var1, var2, 49, 9, 44, 1, var3, var4, var5, var6, var7, var8, var9);
   }

   public RegexTransformWudaoScript(
      String var1,
      String var2,
      int var3,
      int var4,
      int var5,
      int var6,
      Pattern var7,
      String var8,
      String var9,
      String var10,
      String var11,
      String var12,
      String var13
   ) {
      this.machineId = var1;
      this.southMachineId = var2;
      this.targetSlot = var3;
      this.startSlot = var4;
      this.endSlot = var5;
      this.baseAmount = var6;
      this.pattern = var7;
      this.replacement = var8;
      this.startMsg = var9;
      this.halfMsg = var10;
      this.finalMsg = var11;
      this.successMsg = var12;
      this.placeMsg = var13;
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
         this.states.put(blockKey(var5.getBlock().getLocation()), new RegexTransformWudaoScript.State());
         var5.getPlayer().sendMessage(ChatColor.GOLD + this.placeMsg);
         return null;
      } else if ("onBreak".equals(var1) && var2[0] instanceof BlockBreakEvent var3) {
         this.states.remove(blockKey(var3.getBlock().getLocation()));
         return null;
      } else {
         return null;
      }
   }

   private void handleTick(Location var1) {
      RegexTransformWudaoScript.State var2 = this.states.computeIfAbsent(blockKey(var1), var0 -> new RegexTransformWudaoScript.State());
      long var3 = System.currentTimeMillis();
      if (var3 - var2.last >= 3000L) {
         var2.last = var3;
         Player var5 = this.findPlayer(var1);
         if (var5 != null) {
            var2.empty = 0;
            this.process(var1, var5, var3, var2);
         } else if (++var2.empty >= 5) {
            var2.timer = null;
            var2.empty = 0;
         }
      }
   }

   private void process(Location var1, Player var2, long var3, RegexTransformWudaoScript.State var5) {
      Location var6 = var1.clone().add(0.0, 0.0, 1.0);
      Object var7 = MachineScriptHelper.getMenu(var6);
      if (var7 != null) {
         if (var5.timer == null) {
            SlimefunItem var10 = MachineScriptHelper.getSfItem(var6);
            if (var10 != null && this.southMachineId.equals(var10.getId())) {
               RegexTransformWudaoScript.CheckResult var9 = this.checkCondition(var7);
               if (!var9.valid) {
                  var2.sendMessage(ChatColor.RED + var9.message);
               } else if (!this.canPlace(var7, var9.targetId)) {
                  var2.sendMessage(ChatColor.RED + "目标槽位不可用");
               } else {
                  var5.timer = new RegexTransformWudaoScript.Timer(var2.getUniqueId(), var3, var9.sourceId, var9.targetId);
                  var2.sendMessage(ChatColor.AQUA + this.startMsg);
               }
            } else {
               var2.sendMessage(ChatColor.RED + "南侧机器出错");
            }
         } else if (!var5.timer.playerId.equals(var2.getUniqueId())) {
            var2.sendMessage(ChatColor.RED + "玩家已更换，中断！");
            var5.timer = null;
         } else {
            long var8 = var3 - var5.timer.start;
            if (!var5.timer.half && var8 >= 7500L) {
               var2.sendMessage(ChatColor.YELLOW + this.halfMsg);
               var5.timer.half = true;
            } else if (!var5.timer.finalMsg && var8 >= 12000L) {
               var2.sendMessage(ChatColor.YELLOW + this.finalMsg);
               var5.timer.finalMsg = true;
            } else if (var8 >= 15000L) {
               this.convert(var6, var2, var7, var5.timer);
               var5.timer = null;
            }
         }
      }
   }

   private void convert(Location var1, Player var2, Object var3, RegexTransformWudaoScript.Timer var4) {
      RegexTransformWudaoScript.CheckResult var5 = this.checkCondition(var3);
      if (var5.valid && var5.sourceId.equals(var4.from)) {
         SlimefunItem var6 = SlimefunItem.getById(var4.to);
         if (var6 != null) {
            ItemStack[] var7 = MachineScriptHelper.getMenuContents(var3);
            ItemStack var8 = this.targetSlot < var7.length ? var7[this.targetSlot] : null;
            if (var8 != null && !var8.getType().isAir()) {
               SlimefunItem var9 = SlimefunItem.getByItem(var8);
               if (var9 == null || !var4.to.equals(var9.getId()) || var8.getAmount() >= var8.getMaxStackSize()) {
                  var2.sendMessage(ChatColor.RED + "转换条件不满足");
                  return;
               }
            }

            for (int var10 = this.startSlot; var10 <= this.endSlot; var10++) {
               KomutechMenuHelper.replaceExistingItem(var3, var10, null);
            }

            ItemStack var11 = var6.getItem().clone();
            var11.setAmount(1);
            if (var8 != null && !var8.getType().isAir()) {
               var8.setAmount(var8.getAmount() + 1);
               KomutechMenuHelper.replaceExistingItem(var3, this.targetSlot, var8);
            } else {
               KomutechMenuHelper.replaceExistingItem(var3, this.targetSlot, var11);
            }

            var2.sendMessage(ChatColor.GOLD + this.successMsg);
         }
      } else {
         var2.sendMessage(ChatColor.RED + "转换条件不满足");
      }
   }

   private RegexTransformWudaoScript.CheckResult checkCondition(Object var1) {
      ItemStack[] var2 = MachineScriptHelper.getMenuContents(var1);
      if (this.startSlot >= var2.length) {
         return RegexTransformWudaoScript.CheckResult.fail("物品放置错误");
      } else {
         ItemStack var3 = var2[this.startSlot];
         if (var3 != null && !var3.getType().isAir()) {
            SlimefunItem var4 = SlimefunItem.getByItem(var3);
            if (var4 == null) {
               return RegexTransformWudaoScript.CheckResult.fail("物品放置错误: 物品类型错误");
            } else {
               String var5 = var4.getId();
               Matcher var6 = this.pattern.matcher(var5);
               if (!var6.matches()) {
                  return RegexTransformWudaoScript.CheckResult.fail("物品放置错误: 物品不符合规则");
               } else {
                  String var7 = var6.replaceAll(this.replacement);
                  if (var3.getAmount() != this.baseAmount) {
                     return RegexTransformWudaoScript.CheckResult.fail("物品放置错误: 物品数量错误");
                  } else {
                     int var8 = this.startSlot + 1;

                     while (var8 <= this.endSlot) {
                        int var9 = var8 - this.startSlot + this.baseAmount;
                        ItemStack var10 = var8 < var2.length ? var2[var8] : null;
                        if (var10 != null && !var10.getType().isAir()) {
                           SlimefunItem var11 = SlimefunItem.getByItem(var10);
                           if (var11 != null && var5.equals(var11.getId()) && var10.getAmount() == var9) {
                              var8++;
                              continue;
                           }

                           return RegexTransformWudaoScript.CheckResult.fail("物品放置错误: 物品类型或数量不一致");
                        }

                        return RegexTransformWudaoScript.CheckResult.fail("物品放置错误: 有空槽位");
                     }

                     return RegexTransformWudaoScript.CheckResult.ok(var5, var7);
                  }
               }
            }
         } else {
            return RegexTransformWudaoScript.CheckResult.fail("物品放置错误: 有空槽位");
         }
      }
   }

   private boolean canPlace(Object var1, String var2) {
      ItemStack[] var3 = MachineScriptHelper.getMenuContents(var1);
      if (this.targetSlot >= var3.length) {
         return false;
      } else {
         ItemStack var4 = var3[this.targetSlot];
         if (var4 != null && !var4.getType().isAir()) {
            SlimefunItem var5 = SlimefunItem.getByItem(var4);
            return var5 != null && var2.equals(var5.getId()) && var4.getAmount() < var4.getMaxStackSize();
         } else {
            return true;
         }
      }
   }

   private Player findPlayer(Location var1) {
      if (var1.getWorld() == null) {
         return null;
      } else {
         Location var2 = var1.clone().add(0.5, 1.0, 0.5);

         for (Entity var4 : EntityQueries.entities(var1.getWorld(), var2, 1.0, 2.0, 1.0)) {
            if (var4 instanceof Player var5 && var5.isValid() && !var5.isDead()) {
               return var5;
            }
         }

         return null;
      }
   }

   private static String blockKey(Location var0) {
      World var1 = var0.getWorld();
      String var2 = var1 == null ? "null" : var1.getUID().toString();
      return var2 + ":" + var0.getBlockX() + ":" + var0.getBlockY() + ":" + var0.getBlockZ();
   }

   public static RegexTransformWudaoScript mingWu() {
      return new RegexTransformWudaoScript(
         "KOMUTECH_L_WD_MWS", "KOMUTECH_L_JQ_FZCZQ", Pattern.compile("^(KOMUTECH_L_FZ_)(YS)(.*)$"), "$1XX$3", "渐渐明悟", "心有所感", "渐入佳境", "明悟通达", "✓ 已放置明悟石"
      );
   }

   public static RegexTransformWudaoScript poWang() {
      return new RegexTransformWudaoScript(
         "KOMUTECH_L_WD_PWS", "KOMUTECH_L_JQ_FZCZQ", Pattern.compile("^(KOMUTECH_L_FZ_)(XX)(.*)$"), "$1WZ$3", "破妄开始", "渐识真我", "即将破妄", "破妄成功", "✓ 已放置破妄石"
      );
   }

   public static RegexTransformWudaoScript yuanQi() {
      return new RegexTransformWudaoScript(
         "KOMUTECH_L_WD_YQS", "KOMUTECH_L_JQ_ZQFZCZQ", Pattern.compile("^(KOMUTECH_L_FZ_)(五行|天象|造化)灵曦$"), "$1$2祖炁", "元炁汇聚", "炁流涌动", "即将成炁", "祖炁已成", "✓ 已放置元炁石"
      );
   }

   private record CheckResult(boolean valid, String message, String sourceId, String targetId) {
      static RegexTransformWudaoScript.CheckResult fail(String var0) {
         return new RegexTransformWudaoScript.CheckResult(false, var0, null, null);
      }

      static RegexTransformWudaoScript.CheckResult ok(String var0, String var1) {
         return new RegexTransformWudaoScript.CheckResult(true, null, var0, var1);
      }
   }

   private static final class State {
      long last;
      int empty;
      RegexTransformWudaoScript.Timer timer;
   }

   private static final class Timer {
      final UUID playerId;
      final long start;
      final String from;
      final String to;
      boolean half;
      boolean finalMsg;

      Timer(UUID var1, long var2, String var4, String var5) {
         this.playerId = var1;
         this.start = var2;
         this.from = var4;
         this.to = var5;
      }
   }
}
