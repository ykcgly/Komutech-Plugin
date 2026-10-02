package tech.komutech.native_scripts.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;

public final class AttributeResetMenuScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String TITLE_PLAYER = "§a§l属性查看 - 个人模式";
   private static final String TITLE_ADMIN_LIST = "§c§l属性管理 - 管理员模式";
   private static final String TITLE_ADMIN_VIEW = "§d§l属性详情 - ";
   private static final int[] BORDER = new int[]{0, 1, 2, 3, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52};
   private static final int[] ATTR_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20};
   private static final List<AttributeResetMenuScript.AttrDef> ATTRIBUTES = buildAttributes();
   private final Map<Player, AttributeResetMenuScript.AdminState> openPlayers = new HashMap<>();
   private Plugin plugin;

   private static List<AttributeResetMenuScript.AttrDef> buildAttributes() {
      ArrayList var0 = new ArrayList();
      addAttr(var0, "生命值", "GENERIC_MAX_HEALTH", Material.APPLE, "§c生命值");
      addAttr(var0, "攻击伤害", "GENERIC_ATTACK_DAMAGE", Material.IRON_SWORD, "§c攻击伤害");
      addAttr(var0, "移动速度", "GENERIC_MOVEMENT_SPEED", Material.FEATHER, "§f移动速度");
      addAttr(var0, "护甲值", "GENERIC_ARMOR", Material.IRON_CHESTPLATE, "§a护甲值");
      addAttr(var0, "盔甲韧性", "GENERIC_ARMOR_TOUGHNESS", Material.IRON_LEGGINGS, "§a盔甲韧性");
      addAttr(var0, "攻击速度", "GENERIC_ATTACK_SPEED", Material.WOODEN_SWORD, "§e攻击速度");
      addAttr(var0, "击退抗性", "GENERIC_KNOCKBACK_RESISTANCE", Material.SHIELD, "§7击退抗性");
      return var0;
   }

   private static void addAttr(List<AttributeResetMenuScript.AttrDef> var0, String var1, String var2, Material var3, String var4) {
      try {
         var0.add(new AttributeResetMenuScript.AttrDef(var1, Attribute.valueOf(var2), var3, var4));
      } catch (IllegalArgumentException var6) {
      }
   }

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      String var2 = var1.getTitle();
      return "§a§l属性查看 - 个人模式".equals(var2) || "§c§l属性管理 - 管理员模式".equals(var2) || var2.startsWith("§d§l属性详情 - ");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onButtonGroupClick".equals(var1) && var2[0] instanceof Player var3) {
         this.openPlayerMenu(var3);
         return true;
      } else {
         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         String var9 = var1.getView().getTitle();
         if ("§a§l属性查看 - 个人模式".equals(var9) || "§c§l属性管理 - 管理员模式".equals(var9) || var9.startsWith("§d§l属性详情 - ")) {
            var1.setCancelled(true);
            int var4 = var1.getSlot();
            if ("§a§l属性查看 - 个人模式".equals(var9)) {
               if (var4 == 53) {
                  int var11 = resetPlayerAttrs(var2);
                  KomutechSupport.send(var2, "§a已重置自身属性，移除了 " + var11 + " 个修饰器。");
                  var2.closeInventory();
               } else if (var4 == 49) {
                  var2.closeInventory();
               } else if (var4 == 8) {
                  if (!isAdmin(var2)) {
                     KomutechSupport.send(var2, "§c无管理员权限");
                     return;
                  }

                  this.openAdminList(var2, 1);
               }
            } else if ("§c§l属性管理 - 管理员模式".equals(var9)) {
               AttributeResetMenuScript.AdminState var10 = this.openPlayers.get(var2);
               int var12 = var10 == null ? 1 : var10.page();
               if (var4 == 53 && isAdmin(var2)) {
                  KomutechSupport.send(var2, "§c确定要重置所有在线玩家属性吗？输入 §6确认重置 §c以确认:");
                  KomutechChatInput.waitFor(var2, var2x -> {
                     if ("确认重置".equals(var2x)) {
                        int var3 = 0;

                        for (Player var5x : this.plugin.getServer().getOnlinePlayers()) {
                           var3 += resetPlayerAttrs(var5x);
                        }

                        KomutechSupport.send(var2, "§a已重置所有在线玩家属性，共移除 " + var3 + " 个修饰器。");
                        var2.closeInventory();
                     } else {
                        KomutechSupport.send(var2, "§c操作已取消");
                     }
                  });
               } else if (var4 == 49) {
                  var2.closeInventory();
               } else if (var4 == 8) {
                  this.openPlayerMenu(var2);
               } else if (var4 == 48 && var12 > 1) {
                  this.openAdminList(var2, var12 - 1);
               } else if (var4 == 50 && var10 != null && var12 < var10.total()) {
                  this.openAdminList(var2, var12 + 1);
               } else if (var10 != null && var10.slotMap().containsKey(var4)) {
                  String var7 = var10.slotMap().get(var4);
                  Player var8 = this.plugin.getServer().getPlayer(var7);
                  if (var8 == null) {
                     KomutechSupport.send(var2, "§c玩家离线");
                     return;
                  }

                  if (var1.isShiftClick() && var1.isLeftClick()) {
                     KomutechSupport.send(var2, "§c确定要重置玩家 §e" + var7 + " §c的属性吗？输入 §6确认重置 §c以确认:");
                     KomutechChatInput.waitFor(var2, var5x -> {
                        if ("确认重置".equals(var5x)) {
                           KomutechSupport.send(var2, "§a已重置玩家 " + var7 + " 的属性，移除了 " + resetPlayerAttrs(var8) + " 个修饰器。");
                           this.openAdminList(var2, var12);
                        } else {
                           KomutechSupport.send(var2, "§c操作已取消");
                        }
                     });
                  } else if (var1.isLeftClick()) {
                     this.openAdminView(var2, var8);
                  }
               }
            } else {
               if (var9.startsWith("§d§l属性详情 - ")) {
                  String var5 = var9.substring("§d§l属性详情 - ".length());
                  Player var6 = this.plugin.getServer().getPlayer(var5);
                  if (var4 == 49) {
                     this.openAdminList(var2, 1);
                  } else if (var4 == 53 && var6 != null) {
                     KomutechSupport.send(var2, "§a已重置玩家 " + var5 + " 的属性，移除了 " + resetPlayerAttrs(var6) + " 个修饰器。");
                     var2.closeInventory();
                  }
               }
            }
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         this.openPlayers.remove(var2);
      }
   }

   private void openPlayerMenu(Player var1) {
      Inventory var2 = MenuGuiHelper.create(54, "§a§l属性查看 - 个人模式");
      MenuGuiHelper.applyBorder(var2, BORDER);
      var2.setItem(4, MenuGuiHelper.item(Material.PLAYER_HEAD, "§6" + var1.getName(), List.of("§7你的属性面板")));
      var2.setItem(8, MenuGuiHelper.item(Material.COMPASS, "§6管理员模式", List.of("§7切换至管理员界面")));
      var2.setItem(49, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
      var2.setItem(53, MenuGuiHelper.item(Material.BARRIER, "§c§l重置所有属性", List.of("§7点击移除自身所有属性加成")));

      for (int var3 = 0; var3 < ATTRIBUTES.size() && var3 < ATTR_SLOTS.length; var3++) {
         AttributeResetMenuScript.AttrDef var4 = ATTRIBUTES.get(var3);
         var2.setItem(ATTR_SLOTS[var3], MenuGuiHelper.item(var4.mat(), var4.display(), getAttrLore(var1, var4)));
      }

      var1.openInventory(var2);
   }

   private void openAdminList(Player var1, int var2) {
      if (!isAdmin(var1)) {
         this.openPlayerMenu(var1);
      } else {
         List var3 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
         byte var4 = 28;
         int var5 = Math.max(1, (int)Math.ceil((double)var3.size() / var4));
         var2 = Math.max(1, Math.min(var2, var5));
         int var6 = (var2 - 1) * var4;
         Inventory var7 = MenuGuiHelper.create(54, "§c§l属性管理 - 管理员模式");
         MenuGuiHelper.applyBorder(var7, BORDER);
         var7.setItem(4, MenuGuiHelper.item(Material.NETHER_STAR, "§c玩家列表", List.of("§7在线: " + var3.size(), "§7第 " + var2 + "/" + var5 + " 页")));
         var7.setItem(8, MenuGuiHelper.item(Material.COMPASS, "§a个人模式", List.of("§7返回个人属性菜单")));
         var7.setItem(49, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
         var7.setItem(53, MenuGuiHelper.item(Material.TNT, "§c§l一键重置所有在线玩家", List.of("§7点击移除所有在线玩家属性加成")));
         HashMap var8 = new HashMap();
         int[] var9 = new int[]{10, 19, 28, 37};
         int var10 = 0;

         for (int var14 : var9) {
            for (int var15 = 0; var15 < 7 && var6 + var10 < var3.size(); var15++) {
               String var16 = (String)var3.get(var6 + var10);
               int var17 = var14 + var15;
               var7.setItem(var17, MenuGuiHelper.item(Material.PLAYER_HEAD, "§e" + var16, List.of("§7左键查看", "§7Shift+左键重置")));
               var8.put(var17, var16);
               var10++;
            }
         }

         if (var2 > 1) {
            var7.setItem(48, MenuGuiHelper.item(Material.ARROW, "§a上一页", List.of()));
         }

         if (var2 < var5) {
            var7.setItem(50, MenuGuiHelper.item(Material.ARROW, "§a下一页", List.of()));
         }

         var1.openInventory(var7);
         this.openPlayers.put(var1, new AttributeResetMenuScript.AdminState(var2, var5, var8));
      }
   }

   private void openAdminView(Player var1, Player var2) {
      Inventory var3 = MenuGuiHelper.create(54, "§d§l属性详情 - " + var2.getName());
      MenuGuiHelper.applyBorder(var3, BORDER);
      var3.setItem(4, MenuGuiHelper.item(Material.PLAYER_HEAD, "§6" + var2.getName(), List.of("§7属性详情")));
      var3.setItem(49, MenuGuiHelper.item(Material.ARROW, "§a返回列表", List.of()));
      var3.setItem(53, MenuGuiHelper.item(Material.BARRIER, "§c§l重置此玩家属性", List.of("§7点击移除该玩家所有加成")));

      for (int var4 = 0; var4 < ATTRIBUTES.size() && var4 < ATTR_SLOTS.length; var4++) {
         AttributeResetMenuScript.AttrDef var5 = ATTRIBUTES.get(var4);
         var3.setItem(ATTR_SLOTS[var4], MenuGuiHelper.item(var5.mat(), var5.display(), getAttrLore(var2, var5)));
      }

      var1.openInventory(var3);
   }

   private static List<String> getAttrLore(Player var0, AttributeResetMenuScript.AttrDef var1) {
      if (var1.attribute() == null) {
         return List.of("§c属性不可用");
      } else {
         AttributeInstance var2 = var0.getAttribute(var1.attribute());
         if (var2 == null) {
            return List.of("§c属性未找到");
         } else {
            ArrayList var3 = new ArrayList();
            var3.add("§7当前值: §f" + String.format("%.2f", var2.getValue()) + "§7（基础 " + String.format("%.2f", var2.getBaseValue()) + "）");
            if (var2.getModifiers().isEmpty()) {
               var3.add("§7无额外加成");
            } else {
               var3.add("§7修饰器:");

               for (AttributeModifier var5 : var2.getModifiers()) {
                  var3.add("  §8- §7" + var5.getName() + " §8| §a" + var5.getAmount());
               }
            }

            return var3;
         }
      }
   }

   private static int resetPlayerAttrs(Player var0) {
      int var1 = 0;

      for (AttributeResetMenuScript.AttrDef var3 : ATTRIBUTES) {
         if (var3.attribute() != null) {
            AttributeInstance var4 = var0.getAttribute(var3.attribute());
            if (var4 != null) {
               for (AttributeModifier var6 : new ArrayList<>(var4.getModifiers())) {
                  var4.removeModifier(var6);
                  var1++;
               }
            }
         }
      }

      return var1;
   }

   private static boolean isAdmin(Player var0) {
      return var0.isOp() || "Komu_A".equals(var0.getName());
   }

   private record AdminState(int page, int total, Map<Integer, String> slotMap) {
   }

   private record AttrDef(String id, Attribute attribute, Material mat, String display) {
   }
}
