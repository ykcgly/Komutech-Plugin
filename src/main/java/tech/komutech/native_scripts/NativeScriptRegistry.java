package tech.komutech.native_scripts;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.cultivation.JianLingShiScript;
import tech.komutech.native_scripts.cultivation.PuTuanScript;
import tech.komutech.native_scripts.item.AttributePillScript;
import tech.komutech.native_scripts.item.FireworkLauncherScript;
import tech.komutech.native_scripts.item.FuLingFuScript;
import tech.komutech.native_scripts.item.GuiYuanZhuScript;
import tech.komutech.native_scripts.item.HuazangStaffScript;
import tech.komutech.native_scripts.item.IslanderScript;
import tech.komutech.native_scripts.item.LingStoneScript;
import tech.komutech.native_scripts.item.MeritTicketScript;
import tech.komutech.native_scripts.item.ScrollScript;
import tech.komutech.native_scripts.item.SpiritItemAdjusterScript;
import tech.komutech.native_scripts.item.StaffScript;
import tech.komutech.native_scripts.item.TrialZombieScript;
import tech.komutech.native_scripts.item.WanXiangGuiScript;
import tech.komutech.native_scripts.item.WuGouPiScript;
import tech.komutech.native_scripts.item.XiDianZhuScript;
import tech.komutech.native_scripts.item.ZeLingZhuScript;
import tech.komutech.native_scripts.machine.BreakNoDropScript;
import tech.komutech.native_scripts.machine.CropSeedScript;
import tech.komutech.native_scripts.machine.ItemDisplayStandScript;
import tech.komutech.native_scripts.machine.LingMaiMineScript;
import tech.komutech.native_scripts.machine.MaterialNoDropScript;
import tech.komutech.native_scripts.machine.PendingAddonScript;
import tech.komutech.native_scripts.machine.SongMuScript;
import tech.komutech.native_scripts.machine.SuPeiTaiScript;
import tech.komutech.native_scripts.machine.TreeSeedScript;
import tech.komutech.native_scripts.machine.UltimateSynthCoreScript;
import tech.komutech.native_scripts.machine.UltimateSynthProcessorScript;
import tech.komutech.native_scripts.machine.WanYanYiScript;
import tech.komutech.native_scripts.machine.YuGanSeedScript;
import tech.komutech.native_scripts.menu.AttributeResetMenuScript;
import tech.komutech.native_scripts.menu.ConvenienceRecipeMenuScript;
import tech.komutech.native_scripts.menu.GuideMenuScript;
import tech.komutech.native_scripts.menu.PlayerAttributeMenuScript;
import tech.komutech.native_scripts.menu.QiongHuaMenuScript;
import tech.komutech.native_scripts.menu.WanXiangGuiConfigMenuScript;
import tech.komutech.native_scripts.support.ChatInputService;
import tech.komutech.native_scripts.support.KomutechAdminCommands;
import tech.komutech.native_scripts.support.KomutechAsyncScheduler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.SimpleUseMessageScript;
import tech.komutech.native_scripts.tool.CopyCardStubScript;
import tech.komutech.native_scripts.tool.DisplayCleanerScript;
import tech.komutech.native_scripts.tool.HologramCleanerScript;
import tech.komutech.native_scripts.tool.ItemFrameModifierScript;
import tech.komutech.native_scripts.tool.MaterialReplacerScript;
import tech.komutech.native_scripts.tool.NaJieScript;
import tech.komutech.native_scripts.tool.PdcInspectorScript;
import tech.komutech.native_scripts.tool.PositionRecorderScript;
import tech.komutech.native_scripts.tool.PrefabStructureScript;
import tech.komutech.native_scripts.tool.RegionItemFrameScript;
import tech.komutech.native_scripts.tool.ScrollBoxAdminScript;
import tech.komutech.native_scripts.tool.StorageClipboardScript;
import tech.komutech.native_scripts.tool.StorageDetectorScript;
import tech.komutech.native_scripts.tool.YunZhuanXiaScript;
import tech.komutech.native_scripts.wudao.ElementWudaoScript;
import tech.komutech.native_scripts.wudao.RegexTransformWudaoScript;
import tech.komutech.native_scripts.wudao.SlotRuleWudaoScript;




public final class NativeScriptRegistry {
   private static final Map<String, NativeScript> SCRIPTS = new HashMap<>();
   private static final SimpleUseMessageScript NET_STORAGE_MSG = new SimpleUseMessageScript("手中的道具非你能探索的哟~");
   private static final NoOpNativeScript NO_OP = new NoOpNativeScript();
   private static PlayerAttributeMenuScript PLAYER_ATTR_MENU;
   private static WanXiangGuiScript WANXIANG_GUI;
   private static GuideMenuScript GUIDE_MENU;
   private static QiongHuaMenuScript QIONGHUA_MENU;
   private static ConvenienceRecipeMenuScript RECIPE_MENU;
   private static AttributeResetMenuScript ATTR_RESET_MENU;
   private static WanXiangGuiConfigMenuScript WX_CONFIG_MENU;
   private static PuTuanScript PU_TUAN;
   private static JianLingShiScript JIAN_LING_SHI;
   private static NaJieScript NA_JIE;
   private static YunZhuanXiaScript YUN_ZHUAN_XIA;
   private static FuLingFuScript FU_LING_FU;
   private static IslanderScript ISLANDER;
   private static TrialZombieScript TRIAL_ZOMBIE;
   private static FireworkLauncherScript FIREWORK_LAUNCHER;
   private static SpiritItemAdjusterScript SPIRIT_ADJUSTER;
   private static ScrollBoxAdminScript SCROLL_BOX_ADMIN;
   private static PrefabStructureScript PREFAB_STRUCTURE;
   private static StorageDetectorScript STORAGE_DETECTOR;
   private static PositionRecorderScript POSITION_RECORDER;
   private static MaterialReplacerScript MATERIAL_REPLACER;
   private static CopyCardStubScript COPY_CARD;
   private static PdcInspectorScript PDC_INSPECTOR;
   private static HologramCleanerScript HOLOGRAM_CLEANER;
   private static DisplayCleanerScript DISPLAY_CLEANER;
   private static ItemFrameModifierScript ITEM_FRAME_MODIFIER;
   private static RegionItemFrameScript REGION_ITEM_FRAME;
   private static StorageClipboardScript STORAGE_CLIPBOARD;
   private static LingMaiMineScript LINGMAI_MINE;
   private static LingMaiMineScript LINGMAI_CRYSTAL_MINE;
   private static WanYanYiScript WAN_YAN_YI;
   private static SuPeiTaiScript SU_PEI_TAI;
   private static SongMuScript SONG_MU;
   private static CropSeedScript COTTON_SEED;
   private static CropSeedScript HEMP_SEED;
   private static TreeSeedScript PINE_SEED;
   private static TreeSeedScript PEACH_SEED;
   private static UltimateSynthCoreScript ULTIMATE_SYNTH_CORE;
   private static UltimateSynthProcessorScript ULTIMATE_SYNTH_PROCESSOR;
   private static YuGanSeedScript YU_GAN_SEED;
   private static ItemDisplayStandScript ITEM_DISPLAY_STAND;
   private static PendingAddonScript PENDING_PLACE;
   private static PendingAddonScript PENDING_OPEN;
   private static boolean singletonsReady;

   private NativeScriptRegistry() {
   }

   private static void ensureSingletons() {
      if (!singletonsReady) {
         PLAYER_ATTR_MENU = new PlayerAttributeMenuScript();
         WANXIANG_GUI = new WanXiangGuiScript();
         GUIDE_MENU = new GuideMenuScript();
         QIONGHUA_MENU = new QiongHuaMenuScript();
         RECIPE_MENU = new ConvenienceRecipeMenuScript();
         ATTR_RESET_MENU = new AttributeResetMenuScript();
         WX_CONFIG_MENU = new WanXiangGuiConfigMenuScript();
         PU_TUAN = new PuTuanScript();
         JIAN_LING_SHI = new JianLingShiScript();
         NA_JIE = new NaJieScript();
         YUN_ZHUAN_XIA = new YunZhuanXiaScript();
         FU_LING_FU = new FuLingFuScript();
         ISLANDER = new IslanderScript();
         TRIAL_ZOMBIE = new TrialZombieScript();
         FIREWORK_LAUNCHER = new FireworkLauncherScript();
         SPIRIT_ADJUSTER = new SpiritItemAdjusterScript();
         SCROLL_BOX_ADMIN = new ScrollBoxAdminScript();
         PREFAB_STRUCTURE = new PrefabStructureScript();
         STORAGE_DETECTOR = new StorageDetectorScript();
         POSITION_RECORDER = new PositionRecorderScript();
         MATERIAL_REPLACER = new MaterialReplacerScript();
         COPY_CARD = new CopyCardStubScript();
         PDC_INSPECTOR = new PdcInspectorScript();
         HOLOGRAM_CLEANER = new HologramCleanerScript();
         DISPLAY_CLEANER = new DisplayCleanerScript();
         ITEM_FRAME_MODIFIER = new ItemFrameModifierScript();
         REGION_ITEM_FRAME = new RegionItemFrameScript();
         STORAGE_CLIPBOARD = new StorageClipboardScript();
         LINGMAI_MINE = new LingMaiMineScript(
            10,
            "KOMUTECH_L_KW_LSYK",
            "KOMUTECH_L_DJ_XPLS",
            "KOMUTECH_L_DJ_XPLS",
            "KOMUTECH_L_KW_KSZK",
            "KOMUTECH_L_KW_KSZK",
            "KOMUTECH_L_KW_KSZK",
            "KOMUTECH_L_KW_KSZK",
            "KOMUTECH_L_KW_KSZK",
            "KOMUTECH_L_KW_KSZK",
            "KOMUTECH_L_KW_KSZK"
         );
         LINGMAI_CRYSTAL_MINE = new LingMaiMineScript(
            5, "KOMUTECH_L_KW_PP", "KOMUTECH_L_KW_JYY", "KOMUTECH_L_KW_ZMY", "KOMUTECH_L_KW_LSY", "KOMUTECH_L_KW_YHY", "KOMUTECH_L_KW_YTY"
         );
         WAN_YAN_YI = new WanYanYiScript();
         SU_PEI_TAI = new SuPeiTaiScript();
         SONG_MU = new SongMuScript();
         COTTON_SEED = CropSeedScript.cotton();
         HEMP_SEED = CropSeedScript.hemp();
         PINE_SEED = TreeSeedScript.pine();
         PEACH_SEED = TreeSeedScript.peach();
         ULTIMATE_SYNTH_CORE = new UltimateSynthCoreScript();
         ULTIMATE_SYNTH_PROCESSOR = new UltimateSynthProcessorScript();
         YU_GAN_SEED = new YuGanSeedScript();
         ITEM_DISPLAY_STAND = new ItemDisplayStandScript();
         PENDING_PLACE = PendingAddonScript.onPlace();
         PENDING_OPEN = PendingAddonScript.onOpen();
         singletonsReady = true;
      }
   }

   public static void bootstrap() {
      ensureSingletons();
      SCRIPTS.clear();
      register("L_道具/功德券", new MeritTicketScript());
      register("L_道具/灵石", new LingStoneScript());
      register("L_道具/归元珠", new GuiYuanZhuScript());
      register("L_道具/洗点珠", new XiDianZhuScript());
      register("L_道具/擇靈珠", new ZeLingZhuScript());
      register("L_道具/无垢坯", new WuGouPiScript());
      register("L_道具/萬象匱", WANXIANG_GUI);
      register("L_道具/小岛人", ISLANDER);
      register("L_道具/试炼僵尸", TRIAL_ZOMBIE);
      register("L_道具/烟花发射器", FIREWORK_LAUNCHER);
      register("L_道具/灵-物品属性调整器", SPIRIT_ADJUSTER);
      register("L_丹药/属性丹", new AttributePillScript());
      register("L_符/缚灵符", FU_LING_FU);
      register("L_符/魂缚符", FU_LING_FU);
      register("L_工具/纳戒", NA_JIE);
      register("L_工具/云篆匣", YUN_ZHUAN_XIA);
      register("L_工具/云篆匣管理器", SCROLL_BOX_ADMIN);
      register("L_工具/预制建筑", PREFAB_STRUCTURE);
      register("L_卷轴/勾豆灰", new ScrollScript("勾豆灰"));
      register("L_卷轴/碎玉闪", new ScrollScript("碎玉闪"));
      register("L_卷轴/九霄环佩鸣", new ScrollScript("九霄环佩鸣"));
      register("L_卷轴/寒霜锁", new ScrollScript("寒霜锁"));
      register("L_卷轴/冰火两重天", new ScrollScript("冰火两重天"));
      register("L_卷轴/御龙护身决", new ScrollScript("御龙护身决"));
      register("L_卷轴/游龙惊鸿诀", new ScrollScript("游龙惊鸿诀"));
      register("L_卷轴/星陨劫", new ScrollScript("星陨劫"));
      register("L_卷轴/五行必杀", new ScrollScript("五行必杀"));
      register("L_灵杖/烧火棍", new StaffScript("烧火棍"));
      register("L_灵杖/灵杖", new StaffScript("灵杖"));
      register("L_灵杖/蜉蝣梦", new StaffScript("蜉蝣梦"));
      register("L_灵杖/空引津", new StaffScript("空引津"));
      register("L_灵杖/花葬", new HuazangStaffScript());
      register("L_灵杖/归墟", new StaffScript("归墟"));
      register("L_修炼/蒲团", PU_TUAN);
      register("L_修炼/鉴灵石", JIAN_LING_SHI);
      register("L_机器/灵脉宝窟", LINGMAI_MINE);
      register("L_机器/灵脉晶辉宝窟", LINGMAI_CRYSTAL_MINE);
      register("L_机器/塑坯台", SU_PEI_TAI);
      register("L_机器/萬衍儀", WAN_YAN_YI);
      register("L_机器/終極合成台", ULTIMATE_SYNTH_PROCESSOR);
      register("L_机器/終極合成台核心", ULTIMATE_SYNTH_CORE);
      register("L_材料/松木", SONG_MU);
      register("L_灵植种子/桃树树苗", PEACH_SEED);
      register("L_灵植种子/松树树苗", PINE_SEED);
      register("L_灵植种子/玉干种子", YU_GAN_SEED);
      register("L_灵植种子/棉花种子", COTTON_SEED);
      register("L_灵植种子/麻种子", HEMP_SEED);
      register("L_悟道/金元素悟道石", wudao("KOMUTECH_L_WD_JYSWDS", "KOMUTECH_L_FZ_YSJYSFZ", ElementWudaoScript.SenseKind.GOLD));
      register("L_悟道/木元素悟道石", wudao("KOMUTECH_L_WD_MYSWDS", "KOMUTECH_L_FZ_YSMYSFZ", ElementWudaoScript.SenseKind.WOOD));
      register("L_悟道/水元素悟道石", wudao("KOMUTECH_L_WD_SYSWDS", "KOMUTECH_L_FZ_YSSYSFZ", ElementWudaoScript.SenseKind.WATER));
      register("L_悟道/火元素悟道石", wudao("KOMUTECH_L_WD_HYSWDS", "KOMUTECH_L_FZ_YSHYSFZ", ElementWudaoScript.SenseKind.FIRE));
      register("L_悟道/土元素悟道石", wudao("KOMUTECH_L_WD_TYSWDS", "KOMUTECH_L_FZ_YSTYSFZ", ElementWudaoScript.SenseKind.EARTH));
      register("L_悟道/冰元素悟道石", wudao("KOMUTECH_L_WD_BYSWDS", "KOMUTECH_L_FZ_YSBYSFZ", ElementWudaoScript.SenseKind.ICE));
      register("L_悟道/风元素悟道石", wudao("KOMUTECH_L_WD_FYSWDS", "KOMUTECH_L_FZ_YSFYSFZ", ElementWudaoScript.SenseKind.WIND));
      register("L_悟道/雷元素悟道石", wudao("KOMUTECH_L_WD_LYSWDS", "KOMUTECH_L_FZ_YSLYSFZ", ElementWudaoScript.SenseKind.LIGHTNING));
      register("L_悟道/机关悟道石", wudao("KOMUTECH_L_WD_JGWDS", "KOMUTECH_L_FZ_YSJGFZ", ElementWudaoScript.SenseKind.MECHANISM));
      register("L_悟道/炼金悟道石", wudao("KOMUTECH_L_WD_LJWDS", "KOMUTECH_L_FZ_YSLJFZ", ElementWudaoScript.SenseKind.ALCHEMY));
      register("L_悟道/声音悟道石", wudao("KOMUTECH_L_WD_SYWDS", "KOMUTECH_L_FZ_YSSYFZ", ElementWudaoScript.SenseKind.SOUND));
      register("L_悟道/空间悟道石", wudao("KOMUTECH_L_WD_KJWDS", "KOMUTECH_L_FZ_YSKJFZ", ElementWudaoScript.SenseKind.SPACE));
      register("L_悟道/杀戮悟道石", wudao("KOMUTECH_L_WD_SLWDS", "KOMUTECH_L_FZ_YSSLFZ", ElementWudaoScript.SenseKind.KILL));
      register("L_悟道/明悟石", RegexTransformWudaoScript.mingWu());
      register("L_悟道/破妄石", RegexTransformWudaoScript.poWang());
      register("L_悟道/元炁石", RegexTransformWudaoScript.yuanQi());
      register("L_悟道/五行通明石", SlotRuleWudaoScript.wuXing());
      register("L_悟道/天象通明石", SlotRuleWudaoScript.tianXiang());
      register("L_悟道/造化通明石", SlotRuleWudaoScript.zaoHua());
      register("L-菜单/口木科技指南", GUIDE_MENU);
      register("L-菜单/琼华阁", QIONGHUA_MENU);
      register("L-菜单/口木科技便捷配方", RECIPE_MENU);
      register("L-菜单/玩家属性", PLAYER_ATTR_MENU);
      register("L-菜单/属性重置菜单", ATTR_RESET_MENU);
      register("L-菜单/萬象匱配置", WX_CONFIG_MENU);
      register("工具/印物笺", STORAGE_CLIPBOARD);
      register("工具/全息文字清除器", HOLOGRAM_CLEANER);
      register("工具/展示物品清除器", DISPLAY_CLEANER);
      register("工具/展示框修改器", ITEM_FRAME_MODIFIER);
      register("工具/范围展示框修改器", REGION_ITEM_FRAME);
      register("工具/方位记录器", POSITION_RECORDER);
      register("工具/材质替换器", MATERIAL_REPLACER);
      register("工具/网络存储查询器", NET_STORAGE_MSG);
      register("工具/手持网络存储查询器", NET_STORAGE_MSG);
      register("工具/网络存储修改器", NET_STORAGE_MSG);
      register("工具/便携复制卡获取器", COPY_CARD);
      register("工具/头颅ID提取器", PDC_INSPECTOR);
      register("工具/存储检测器", STORAGE_DETECTOR);
      register("机器/物品展台", ITEM_DISPLAY_STAND);
      register("待加入/堆叠光暗核心1", PENDING_PLACE);
      register("待加入/堆叠光暗核心2", PENDING_PLACE);
      register("待加入/口木版堆叠光暗开采器", PENDING_OPEN);
      register("待加入/快捷序列化构造机", PENDING_PLACE);
      register("换材质不掉落", new MaterialNoDropScript());
      register("破坏不会掉落", new BreakNoDropScript());
      register("configHandler", NO_OP);
   }

   private static ElementWudaoScript wudao(String var0, String var1, ElementWudaoScript.SenseKind var2) {
      return new ElementWudaoScript(var0, var1, var2);
   }

   public static void register(String var0, NativeScript var1) {
      SCRIPTS.put(var0, var1);
   }

   public static void registerLifecycleListeners(Plugin var0) {
      KomutechAdminCommands.register();
      ChatInputService.register(var0);
      KomutechMenuRouter.register(var0);
      KomutechAsyncScheduler.start(var0);
      NativeLifecycle.registerAll(SCRIPTS, var0);
   }

   public static NativeScript get(String var0) {
      return SCRIPTS.get(var0);
   }

   public static PuTuanScript puTuan() {
      return PU_TUAN;
   }

   public static void shutdown() {
      KomutechAsyncScheduler.shutdown();
   }
}
