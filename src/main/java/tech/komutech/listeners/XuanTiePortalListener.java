package tech.komutech.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import tech.komutech.KT;

/**
 * 玄铁镐挖掘下界传送门。
 *
 * <p>下界传送门硬度为 -1，生存模式下原版根本不会发起破坏（BlockDamageEvent / BlockBreakEvent
 * 都不触发）。这里在命中后改用 {@link Player#breakBlock(Block)} 以玩家身份走完整破坏流程 ——
 * 该方法内部会 fire BlockBreakEvent，且移除方块时不做硬度校验，因此玄铁镐已配置的
 * {@code drop_from: nether_portal} 掉落链路可以直接复用，无需另行实现。
 *
 * <p>移植自原工程 {@code tech.origintech.listeners.XuanTiePortalListener}，
 * 该文件在RSC → Java 重构时整体丢失，导致玄铁镐无法清理传送门框。
 */
public final class XuanTiePortalListener implements Listener {
   /** 物品注册时 ID 被统一大写（{@code KT.upper}），故常量直接写成大写。 */
   private static final String PICK_ID_UPPER = "KOMUTECH_L_GJ_玄铁镐";
   private static final long COOLDOWN_MS = 250L;
   private final Map<UUID, Long> lastBreak = new ConcurrentHashMap<>();

   public XuanTiePortalListener() {
      Bukkit.getPluginManager().registerEvents(this, KT.plugin());
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent var1) {
      if (var1.getAction() == Action.LEFT_CLICK_BLOCK) {
         this.tryBreak(var1.getPlayer(), var1.getClickedBlock(), var1);
      }
   }

   @EventHandler
   public void onBlockDamage(BlockDamageEvent var1) {
      this.tryBreak(var1.getPlayer(), var1.getBlock(), var1);
   }

   /** 挥手兜底：左键点击事件在某些客户端/ 连点场景下可能丢失，用挥手动画 + 射线再补一条入口。 */
   @EventHandler
   public void onSwing(PlayerAnimationEvent var1) {
      if (var1.getAnimationType() == PlayerAnimationType.ARM_SWING) {
         Player var2 = var1.getPlayer();
         Block var3 = this.rayTracePortal(var2);
         if (var3 != null) {
            this.tryBreak(var2, var3, var1);
         }
      }
   }

   /** 注意 {@code ignorePassableBlocks} 必须为 false：NETHER_PORTAL 无碰撞箱，传 true 会让射线直接穿过去。 */
   private Block rayTracePortal(Player var1) {
      RayTraceResult var2 = var1.getWorld()
         .rayTraceBlocks(var1.getEyeLocation(), var1.getEyeLocation().getDirection(), 5.0, FluidCollisionMode.NEVER, false);
      if (var2 != null) {
         Block var3 = var2.getHitBlock();
         if (var3 != null && var3.getType() == Material.NETHER_PORTAL) {
            return var3;
         }
      }

      return null;
   }

   private void tryBreak(Player var1, Block var2, Cancellable var3) {
      if (var2 != null && var2.getType() == Material.NETHER_PORTAL && var1.getGameMode() == GameMode.SURVIVAL) {
         ItemStack var4 = var1.getInventory().getItemInMainHand();
         if (isXuanTiePick(var4)) {
            long var5 = System.currentTimeMillis();
            Long var6 = this.lastBreak.get(var1.getUniqueId());
            if (var6 != null && var5 - var6 < COOLDOWN_MS) {
               return;
            }

            this.lastBreak.put(var1.getUniqueId(), var5);
            if (var1.breakBlock(var2)) {
               var3.setCancelled(true);
               var2.getWorld().spawnParticle(Particle.PORTAL, var2.getLocation().add(0.5, 0.5, 0.5), 30, 0.4, 0.4, 0.4, 0.1);
               var1.playSound(var2.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F);
            }
         }
      }
   }

   private static boolean isXuanTiePick(ItemStack var0) {
      if (var0 == null || var0.getType().isAir()) {
         return false;
      }

      SlimefunItem var1 = SlimefunItem.getByItem(var0);
      if (var1 != null && var1.getId() != null) {
         // 物品 ID 在注册时被统一转成大写，比对前必须先 toUpperCase
         String var2 = var1.getId().toUpperCase(Locale.ROOT);
         if (var2.equals(PICK_ID_UPPER) || var2.endsWith("_GJ_玄铁镐")) {
            return true;
         }
      }

      // 兜底：玄铁镐是 saveditem（原版下界合金镐套壳），按模板物品比对
      SlimefunItem var3 = SlimefunItem.getById(PICK_ID_UPPER);
      if (var3 != null) {
         ItemStack var4 = var3.getItem();
         return var4 != null && var4.isSimilar(var0);
      }

      return false;
   }
}