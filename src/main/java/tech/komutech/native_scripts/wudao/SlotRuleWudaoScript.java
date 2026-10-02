package tech.komutech.native_scripts.wudao;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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

public final class SlotRuleWudaoScript implements NativeScript {
   private static final long TIME = 15000L;
   private static final long INTERVAL = 3000L;
   private static final int MAX_EMPTY = 5;
   private final String machineId;
   private final String southMachineId;
   private final String outputId;
   private final int[] outputSlots;
   private final List<SlotRuleWudaoScript.SlotRule> rules;
   private final String startMsg;
   private final String halfMsg;
   private final String finalMsg;
   private final String successMsg;
   private final String placeMsg;
   private final Map<String, SlotRuleWudaoScript.State> states = new HashMap<>();

   public SlotRuleWudaoScript(
      String var1,
      String var2,
      String var3,
      int[] var4,
      List<SlotRuleWudaoScript.SlotRule> var5,
      String var6,
      String var7,
      String var8,
      String var9,
      String var10
   ) {
      this.machineId = var1;
      this.southMachineId = var2;
      this.outputId = var3;
      this.outputSlots = var4;
      this.rules = var5;
      this.startMsg = var6;
      this.halfMsg = var7;
      this.finalMsg = var8;
      this.successMsg = var9;
      this.placeMsg = var10;
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
         this.states.put(blockKey(var5.getBlock().getLocation()), new SlotRuleWudaoScript.State());
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
      SlotRuleWudaoScript.State var2 = this.states.computeIfAbsent(blockKey(var1), var0 -> new SlotRuleWudaoScript.State());
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

   private void process(Location var1, Player var2, long var3, SlotRuleWudaoScript.State var5) {
      Location var6 = var1.clone().add(0.0, 0.0, 1.0);
      Object var7 = MachineScriptHelper.getMenu(var6);
      if (var7 != null) {
         if (var5.timer == null) {
            SlimefunItem var10 = MachineScriptHelper.getSfItem(var6);
            if (var10 == null || !this.southMachineId.equals(var10.getId())) {
               var2.sendMessage(ChatColor.RED + "南侧机器出错");
            } else if (this.checkRules(var7) && this.canPlaceOutput(var7)) {
               var5.timer = new SlotRuleWudaoScript.Timer(var2.getUniqueId(), var3);
               var2.sendMessage(ChatColor.AQUA + this.startMsg);
            } else {
               var2.sendMessage(ChatColor.RED + "物品放置错误或输出槽位不足");
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
               this.convert(var7, var2);
               var5.timer = null;
            }
         }
      }
   }

   private void convert(Object var1, Player var2) {
      if (!this.checkRules(var1)) {
         var2.sendMessage(ChatColor.RED + "物品已变更，无法完成");
      } else {
         if (this.convertItems(var1)) {
            var2.sendMessage(ChatColor.GOLD + this.successMsg);
         } else {
            var2.sendMessage(ChatColor.RED + "转换失败");
         }
      }
   }

   private boolean convertItems(Object var1) {
      SlimefunItem var2 = SlimefunItem.getById(this.outputId);
      if (var2 == null) {
         return false;
      } else {
         ItemStack[] var3 = MachineScriptHelper.getMenuContents(var1);
         int var4 = var2.getItem().getMaxStackSize();
         int var5 = 1 + ThreadLocalRandom.current().nextInt(6);
         ArrayList<SlotRuleWudaoScript.OutputSlot> var6 = new ArrayList<>();

         for (int var10 : this.outputSlots) {
            ItemStack var11 = var10 < var3.length ? var3[var10] : null;
            if (var11 != null && !var11.getType().isAir()) {
               SlimefunItem var12 = SlimefunItem.getByItem(var11);
               if (var12 != null && this.outputId.equals(var12.getId()) && var11.getAmount() < var4) {
                  var6.add(new SlotRuleWudaoScript.OutputSlot(var10, var11, var4 - var11.getAmount()));
               }
            } else {
               var6.add(new SlotRuleWudaoScript.OutputSlot(var10, null, var4));
            }
         }

         int var13 = var6.stream().mapToInt(var0 -> var0.available).sum();
         if (var13 < var5) {
            return false;
         } else {
            for (SlotRuleWudaoScript.SlotRule var16 : this.rules) {
               for (int var18 = var16.start; var18 <= var16.end; var18++) {
                  KomutechMenuHelper.replaceExistingItem(var1, var18, null);
               }
            }

            Collections.shuffle(var6);
            int var15 = var5;

            for (SlotRuleWudaoScript.OutputSlot var19 : var6) {
               if (var15 <= 0) {
                  break;
               }

               int var20 = Math.min(var15, var19.available);
               if (var19.item != null) {
                  var19.item.setAmount(var19.item.getAmount() + var20);
                  KomutechMenuHelper.replaceExistingItem(var1, var19.slot, var19.item);
               } else {
                  ItemStack var21 = var2.getItem().clone();
                  var21.setAmount(var20);
                  KomutechMenuHelper.replaceExistingItem(var1, var19.slot, var21);
               }

               var15 -= var20;
            }

            return true;
         }
      }
   }

   private boolean checkRules(Object var1) {
      ItemStack[] var2 = MachineScriptHelper.getMenuContents(var1);

      for (SlotRuleWudaoScript.SlotRule var4 : this.rules) {
         int var5 = var4.start;

         while (var5 <= var4.end) {
            int var6 = var5 - var4.start + 1;
            ItemStack var7 = var5 < var2.length ? var2[var5] : null;
            if (var7 != null && !var7.getType().isAir()) {
               SlimefunItem var8 = SlimefunItem.getByItem(var7);
               if (var8 != null && var4.itemId.equals(var8.getId()) && var7.getAmount() == var6) {
                  var5++;
                  continue;
               }

               return false;
            }

            return false;
         }
      }

      return true;
   }

   private boolean canPlaceOutput(Object var1) {
      ItemStack[] var2 = MachineScriptHelper.getMenuContents(var1);

      for (int var6 : this.outputSlots) {
         ItemStack var7 = var6 < var2.length ? var2[var6] : null;
         if (var7 == null || var7.getType().isAir()) {
            return true;
         }

         SlimefunItem var8 = SlimefunItem.getByItem(var7);
         if (var8 != null && this.outputId.equals(var8.getId()) && var7.getAmount() < var7.getMaxStackSize()) {
            return true;
         }
      }

      return false;
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

   public static SlotRuleWudaoScript wuXing() {
      return new SlotRuleWudaoScript(
         "KOMUTECH_L_WD_WXTMS",
         "KOMUTECH_L_JQ_LXFZCZQ",
         "KOMUTECH_L_FZ_五行灵曦",
         new int[]{15, 16, 24, 25, 33, 34},
         List.of(
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZJYSFZ", 0, 4),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZMYSFZ", 9, 13),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZSYSFZ", 18, 22),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZHYSFZ", 27, 31),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZTYSFZ", 36, 40)
         ),
         "开始领悟",
         "五行汇聚",
         "迷惘渐散",
         "大道自成",
         "✓ 已放置五行通明石"
      );
   }

   public static SlotRuleWudaoScript tianXiang() {
      return new SlotRuleWudaoScript(
         "KOMUTECH_L_WD_TXTMS",
         "KOMUTECH_L_JQ_LXFZCZQ",
         "KOMUTECH_L_FZ_天象灵曦",
         new int[]{15, 16, 24, 25, 33, 34},
         List.of(
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZBYSFZ", 0, 4),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZFYSFZ", 9, 13),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZLYSFZ", 18, 22)
         ),
         "天象初现",
         "星象汇聚",
         "天机将显",
         "天象自成",
         "✓ 已放置天象通明石"
      );
   }

   public static SlotRuleWudaoScript zaoHua() {
      return new SlotRuleWudaoScript(
         "KOMUTECH_L_WD_ZHTMS",
         "KOMUTECH_L_JQ_LXFZCZQ",
         "KOMUTECH_L_FZ_造化灵曦",
         new int[]{15, 16, 24, 25, 33, 34},
         List.of(
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZJGFZ", 0, 4),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZLJFZ", 9, 13),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZSYFZ", 18, 22),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZKJFZ", 27, 31),
            new SlotRuleWudaoScript.SlotRule("KOMUTECH_L_FZ_WZSLFZ", 36, 40)
         ),
         "造化初启",
         "万物生发",
         "造化将成",
         "造化自成",
         "✓ 已放置造化通明石"
      );
   }

   private static final class OutputSlot {
      final int slot;
      final ItemStack item;
      final int available;

      OutputSlot(int var1, ItemStack var2, int var3) {
         this.slot = var1;
         this.item = var2;
         this.available = var3;
      }
   }

   public record SlotRule(String itemId, int start, int end) {
   }

   private static final class State {
      long last;
      int empty;
      SlotRuleWudaoScript.Timer timer;
   }

   private static final class Timer {
      final UUID playerId;
      final long start;
      boolean half;
      boolean finalMsg;

      Timer(UUID var1, long var2) {
         this.playerId = var1;
         this.start = var2;
      }
   }
}
