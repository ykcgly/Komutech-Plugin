package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MachineMenuGuard;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.NativeScript;

public final class LingMaiMineScript implements NativeScript, NativeLifecycleScript, KomutechMenuHandler {
   private static final int WORK_SLOT = 40;
   private static final int PICKAXE_SLOT = 53;
   private static final int[] OUTPUT_SLOTS = new int[]{46, 47, 48, 49, 50, 51, 52};
   private static final int COOLDOWN_MS = 500;
   private static final int DURABILITY_PER = 1;
   private static final int PICKAXE_DURABILITY = 2040;
   private static final int STACK_LIMIT = 64;
   private final int chance;
   private final String[] outputIds;
   private final Map<String, SlimefunItem> rewardCache = new HashMap<>();
   private final Map<UUID, Long> cooldowns = new HashMap<>();

   public LingMaiMineScript(int var1, String... var2) {
      this.chance = var1;
      this.outputIds = var2;
   }

   private SlimefunItem rewardItem(String var1) {
      return this.rewardCache.computeIfAbsent(var1, SlimefunItem::getById);
   }

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return !MachineMenuGuard.isOwnMachine(var1, "KOMUTECH_L_JQ_灵脉宝窟") && !MachineMenuGuard.isOwnMachine(var1, "KOMUTECH_L_JQ_灵脉晶辉宝窟")
         ? MachineMenuGuard.titleContains(var1, "灵脉宝窟", "灵脉晶辉宝窟", "靈脈寶窟", "靈脈晶輝寶窟")
         : true;
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onClose".equals(var1) && var2[0] instanceof Player var6) {
         this.cooldowns.remove(var6.getUniqueId());
         return null;
      } else if ("onOpen".equals(var1) && var2[0] instanceof Player var5) {
         Inventory var10 = var5.getOpenInventory().getTopInventory();
         if (this.handles(var5.getOpenInventory())) {
            KomutechMenuRouter.bindInventory(var10, this);
         }

         return null;
      } else if ("onClick".equals(var1) && var2.length >= 4 && var2[0] instanceof Player var3) {
         int var9 = (Integer)var2[1];
         if (this.isAllowedSlot(var9)) {
            return null;
         } else {
            this.handleClick(var3, var9, var2[3]);
            return Boolean.FALSE;
         }
      } else {
         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (this.handles(var1.getView())) {
            int var4 = var1.getRawSlot();
            MachineMenuGuard.protectTopInventory(var1, this::isAllowedSlot);
            if (var1.isCancelled() && !this.isAllowedSlot(var4)) {
               this.handleClick(var2, var4, var1);
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (this.handles(var1.getView())) {
         MachineMenuGuard.protectTopInventoryDrag(var1, this::isAllowedSlot);
      }
   }

   private boolean isAllowedSlot(int var1) {
      return var1 == 53 ? true : MachineMenuGuard.isSlotIn(var1, OUTPUT_SLOTS);
   }

   private void handleClick(Player var1, int var2, Object var3) {
      if (this.checkCooldown(var1)) {
         if (var2 == 40) {
            // 右键 = 一键挖掘（消耗下界合金镐耐久）
            if (KomutechSupport.isRightClick(var3) && !KomutechSupport.isShiftClick(var3)) {
               this.autoMine(var1);
               return;
            }

            // 左键 = 重置石头（原为 shift+左键，现按反馈改为普通左键）
            if (!KomutechSupport.isRightClick(var3) && !KomutechSupport.isShiftClick(var3)) {
               this.refill(var1);
               return;
            }
         }

         if (var2 >= 0 && var2 <= 35) {
            this.mine(var1, var2);
         }
      }
   }

   /**
    * 防连点冷却。
    *
    * <p>原实现命中时向玩家发「操作太频繁，请稍后再试」，实测把正常操作也一并拦掉了
    * （石头是一格格点的，连续开采必然触发），体验很差。现改为<b>静默丢弃</b>：
    * 冷却窗口内直接忽略该次点击，不给任何提示，也不影响后续操作。
    */
   private boolean checkCooldown(Player var1) {
      long var2 = System.currentTimeMillis();
      Long var4 = this.cooldowns.get(var1.getUniqueId());
      if (var4 != null && var2 - var4 < COOLDOWN_MS) {
         return false;
      } else {
         this.cooldowns.put(var1.getUniqueId(), var2);
         return true;
      }
   }

   private void refill(Player var1) {
      Inventory var2 = var1.getOpenInventory().getTopInventory();

      for (int var3 = 0; var3 <= 35; var3++) {
         ItemStack var4 = var2.getItem(var3);
         if (var4 == null || isGlass(var4)) {
            var2.setItem(var3, stoneItem());
         }
      }
   }

   private void mine(Player var1, int var2) {
      Inventory var3 = var1.getOpenInventory().getTopInventory();
      ItemStack var4 = var3.getItem(var2);
      if (var4 != null && var4.getType() == Material.STONE) {
         if (var4.getAmount() > 1) {
            var4.setAmount(var4.getAmount() - 1);
         } else {
            var3.setItem(var2, glassItem());
         }

         this.tryReward(var1, var3);
      }
   }

   private void autoMine(Player var1) {
      try {
         Inventory var2 = var1.getOpenInventory().getTopInventory();
         ItemStack var3 = var2.getItem(53);
         if (var3 == null || var3.getType() != Material.NETHERITE_PICKAXE) {
            KomutechSupport.send(var1, "§c需要下界合金镐！");
            return;
         }

         ItemMeta var4 = var3.getItemMeta();
         if (var4 == null) {
            return;
         }

         int var5 = var4 instanceof Damageable var6 ? var6.getDamage() : 0;
         int var15 = 2040 - var5;
         if (var15 < 1) {
            var2.setItem(53, null);
            KomutechSupport.send(var1, "§c工具耐久已耗尽！");
            return;
         }

         int var7 = 0;
         boolean var8 = false;

         for (int var9 = 0; var9 <= 35 && !var8; var9++) {
            ItemStack var10 = var2.getItem(var9);
            if (var10 != null && var10.getType() == Material.STONE) {
               int var11 = var10.getAmount();
               int var12 = 0;

               while (var11 > 0 && (var7 + var12) * 1 < var15) {
                  var12++;
                  var11--;
                  if (ThreadLocalRandom.current().nextInt(100) < this.chance) {
                     ItemStack var13 = this.randomReward();
                     if (var13 != null && !this.placeReward(var2, var13)) {
                        var8 = true;
                        var12--;
                        var11++;
                        break;
                     }
                  }

                  if (var8) {
                     break;
                  }
               }

               if (var11 > 0) {
                  var10.setAmount(var11);
               } else {
                  var2.setItem(var9, glassItem());
               }

               var7 += var12;
            }
         }

         if (var7 > 0 && var4 instanceof Damageable var16) {
            int var17 = var5 + var7 * 1;
            if (var17 >= 2040) {
               var2.setItem(53, null);
            } else {
               var16.setDamage(var17);
               var3.setItemMeta(var4);
            }
         }

         if (var8) {
            KomutechSupport.send(var1, "§c输出槽位已满");
         }
      } catch (RuntimeException var14) {
         KomutechSupport.send(var1, "§c自动挖掘出错！");
      }
   }

   private void tryReward(Player var1, Inventory var2) {
      if (ThreadLocalRandom.current().nextInt(100) < this.chance) {
         ItemStack var3 = this.randomReward();
         if (var3 != null && !this.placeReward(var2, var3)) {
            KomutechSupport.send(var1, "&c输出槽位已满");
         }
      }
   }

   private ItemStack randomReward() {
      if (this.outputIds.length == 0) {
         return null;
      } else {
         String var1 = this.outputIds[ThreadLocalRandom.current().nextInt(this.outputIds.length)];
         SlimefunItem var2 = this.rewardItem(var1);
         return var2 == null ? null : var2.getItem().clone();
      }
   }

   private boolean placeReward(Inventory var1, ItemStack var2) {
      String var3 = getSfId(var2);

      for (int var7 : OUTPUT_SLOTS) {
         ItemStack var8 = var1.getItem(var7);
         if (var8 == null || var8.getType().isAir()) {
            var1.setItem(var7, var2);
            return true;
         }

         if (var8.getType() == var2.getType()) {
            String var9 = getSfId(var8);
            if ((var9 != null && var9.equals(var3) || var9 == null && var3 == null) && var8.getAmount() < 64) {
               var8.setAmount(var8.getAmount() + 1);
               return true;
            }
         }
      }

      return false;
   }

   private static String getSfId(ItemStack var0) {
      SlimefunItem var1 = SlimefunItem.getByItem(var0);
      return var1 == null ? null : var1.getId();
   }

   private static ItemStack stoneItem() {
      ItemStack var0 = new ItemStack(Material.STONE, 1);
      ItemMeta var1 = var0.getItemMeta();
      var1.setDisplayName("§f点击挖掘");
      var0.setItemMeta(var1);
      return var0;
   }

   private static ItemStack glassItem() {
      ItemStack var0 = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1);
      ItemMeta var1 = var0.getItemMeta();
      var1.setDisplayName(" ");
      var0.setItemMeta(var1);
      return var0;
   }

   private static boolean isGlass(ItemStack var0) {
      return var0 != null && var0.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE && var0.hasItemMeta() && " ".equals(var0.getItemMeta().getDisplayName());
   }
}
