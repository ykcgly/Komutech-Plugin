package tech.komutech.native_scripts.storage;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.support.KomutechAddonConfig;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechPaths;

public final class WanXiangGuiStorage {
   public static final String STORAGE_ID = "KOMUTECH_L_ZJ_萬象匱";
   public static final int SLOTS_PER_PAGE = 45;
   public static final int INVENTORY_SIZE = 54;
   private static final NamespacedKey STORAGE_NAME_KEY = new NamespacedKey("komutechnative", "wx_storage_name");
   private static final Collator COLLATOR;
   private static final String STORAGE_LORE_PREFIX = "§7┃ 存储标识: §f";
   private static final String[] STORAGE_LORE_PREFIXES;
   private static final String[] STORAGE_MARKERS;

   public static Path dataDir() {
      return KomutechPaths.wanXiangGui();
   }

   public static Path filePath(String var0, String var1) {
      return dataDir().resolve(fileName(var0, var1));
   }

   private WanXiangGuiStorage() {
   }

   public static String getStorageName(ItemStack var0) {
      if (var0 != null && var0.hasItemMeta()) {
         ItemMeta var1 = var0.getItemMeta();
         String var2 = (String)var1.getPersistentDataContainer().get(STORAGE_NAME_KEY, PersistentDataType.STRING);
         if (isValidStorageName(var2)) {
            return var2;
         } else {
            List<String> var3 = var1.getLore();
            if (var3 == null) {
               return null;
            } else {
               for(String var5 : var3) {
                  for(String var9 : STORAGE_LORE_PREFIXES) {
                     if (var5.startsWith(var9)) {
                        String var10 = var5.substring(var9.length()).trim();
                        if (isValidStorageName(var10)) {
                           return var10;
                        }
                     }
                  }

                  String var11 = extractStorageName(ChatColor.stripColor(var5));
                  if (var11 != null) {
                     return var11;
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static void setStorageName(ItemStack var0, String var1) {
      ItemMeta var2 = var0.getItemMeta();
      ArrayList<String> var3 = var2.getLore() == null ? new ArrayList<>() : new ArrayList<>(var2.getLore());
      var3.removeIf((var0x) -> {
         if (var0x.startsWith("§7┃ 存储标识: §f")) {
            return true;
         } else {
            return extractStorageName(ChatColor.stripColor(var0x)) != null;
         }
      });
      var3.add("§7┃ 存储标识: §f" + var1);
      var2.setLore(var3);
      var2.getPersistentDataContainer().set(STORAGE_NAME_KEY, PersistentDataType.STRING, var1);
      var0.setItemMeta(var2);
   }

   private static String extractStorageName(String var0) {
      if (var0 == null) {
         return null;
      } else {
         var0 = var0.trim().replaceFirst("^[|┃│\\s]+", "");

         for(String var4 : STORAGE_MARKERS) {
            int var5 = var0.indexOf(var4);
            if (var5 >= 0) {
               String var6 = var0.substring(var5 + var4.length()).trim();
               if (isValidStorageName(var6)) {
                  return var6;
               }
            }
         }

         return null;
      }
   }

   public static String fileName(String var0, String var1) {
      return var1 == null ? null : "[" + var0 + "]" + var1.replaceAll("[\\\\/:*?\"<>|]", "_") + ".json";
   }

   public static String resolveStorageName(String var0, String var1) {
      if (var1 == null) {
         return null;
      } else if (storageFileExists(var0, var1)) {
         return var1;
      } else {
         for(String var3 : listStorages(var0)) {
            if (storageNameEquals(var1, var3)) {
               return var3;
            }
         }

         return var1;
      }
   }

   public static Map<String, Object> read(String var0, String var1) {
      var1 = resolveStorageName(var0, var1);
      Path var2 = filePath(var0, var1);
      if (!Files.exists(var2, new LinkOption[0])) {
         return emptyData();
      } else {
         try {
            Map var3 = KomutechJson.asMap(KomutechJson.parse(Files.readString(var2, StandardCharsets.UTF_8)));
            if (!var3.containsKey("meta")) {
               var3.put("meta", Map.of("page", "1", "mode", "normal", "kw", ""));
            }

            if (!var3.containsKey("pages")) {
               var3.put("pages", Map.of("1", emptyPage()));
            }

            normalizePages(var3);
            return var3;
         } catch (RuntimeException | IOException var4) {
            return emptyData();
         }
      }
   }

   public static void write(String var0, String var1, Map<String, Object> var2) {
      try {
         var1 = resolveStorageName(var0, var1);
         Files.createDirectories(dataDir());
         Files.writeString(filePath(var0, var1), KomutechJson.stringify(var2), StandardCharsets.UTF_8);
      } catch (IOException var4) {
      }

   }

   private static boolean storageNameEquals(String var0, String var1) {
      if (var0 != null && var1 != null) {
         return var0.equals(var1) || normalizeStorageKey(var0).equals(normalizeStorageKey(var1));
      } else {
         return false;
      }
   }

   private static String normalizeStorageKey(String var0) {
      StringBuilder var1 = new StringBuilder(var0.trim().length());

      for(char var5 : var0.trim().toCharArray()) {
         char var10001;
         switch (var5) {
            case '双':
            case '雙':
               var10001 = 21452;
               break;
            case '无':
            case '無':
               var10001 = 26080;
               break;
            default:
               var10001 = var5;
         }

         var1.append(var10001);
      }

      return var1.toString();
   }

   public static Map<String, Object> emptyData() {
      LinkedHashMap var0 = new LinkedHashMap();
      var0.put("pages", Map.of("1", emptyPage()));
      var0.put("meta", Map.of("page", "1", "mode", "normal", "kw", ""));
      return var0;
   }

   private static void normalizePages(Map<String, Object> var0) {
      Map<String, Object> var1 = KomutechJson.asMap(var0.get("pages"));

      for(String var3 : new ArrayList<>(var1.keySet())) {
         Object var4 = KomutechJson.asList(var1.get(var3));
         if (((List)var4).size() > 45) {
            var4 = new ArrayList(((List)var4).subList(0, 45));
         }

         while(((List)var4).size() < 45) {
            ((List)var4).add((Object)null);
         }

         var1.put(var3, var4);
      }

      var0.put("pages", var1);
   }

   public static String normalizedEntry(Object var0) {
      if (var0 == null) {
         return null;
      } else if (var0 instanceof String) {
         String var1 = (String)var0;
         return var1;
      } else {
         return KomutechJson.stringify(var0);
      }
   }

   public static List<Object> emptyPage() {
      ArrayList var0 = new ArrayList();

      for(int var1 = 0; var1 < 45; ++var1) {
         var0.add((Object)null);
      }

      return var0;
   }

   private static void setPageEntry(Map<String, Object> var0, String var1, int var2, Object var3) {
      Map var4 = KomutechJson.asMap(var0.get("pages"));
      List var5 = KomutechJson.asList(var4.get(var1));
      if (!var4.containsKey(var1)) {
         var5 = emptyPage();
      }

      while(var5.size() <= var2) {
         var5.add((Object)null);
      }

      var5.set(var2, var3);
      var4.put(var1, var5);
      var0.put("pages", var4);
   }

   public static String serialize(ItemStack var0) {
      if (var0 != null && var0.getType() != Material.AIR) {
         SlimefunItem var1 = SlimefunItem.getByItem(var0);
         return var1 != null ? KomutechJson.stringify(Map.of("type", "slimefun", "id", var1.getId())) : KomutechJson.stringify(Map.of("type", "vanilla", "material", var0.getType().name()));
      } else {
         return null;
      }
   }

   public static ItemStack deserialize(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         Object var10000;
         if (var0 instanceof String) {
            String var2 = (String)var0;
            var10000 = KomutechJson.parse(var2);
         } else {
            var10000 = var0;
         }

         Map var1 = KomutechJson.asMap(var10000);
         String var4 = KomutechJson.getString(var1, "type", "");
         if ("slimefun".equals(var4)) {
            SlimefunItem var5 = SlimefunItem.getById(KomutechJson.getString(var1, "id", ""));
            return var5 == null ? null : var5.getItem().clone();
         } else {
            Material var3 = Material.matchMaterial(KomutechJson.getString(var1, "material", "AIR"));
            return var3 != null && !var3.isAir() ? new ItemStack(var3, 1) : null;
         }
      }
   }

   public static ReadResult readForItem(ItemStack var0, String var1) {
      String var2 = getStorageName(var0);
      if (var2 == null) {
         Map var3 = emptyData();
         return new ReadResult(var3, true);
      } else {
         return new ReadResult(read(var1, var2), false);
      }
   }

   public static void writeForItem(ItemStack var0, String var1, Map<String, Object> var2) {
      String var3 = getStorageName(var0);
      if (var3 != null) {
         write(var1, var3, var2);
      }

   }

   public static boolean isStorageItem(ItemStack var0) {
      if (var0 != null && !var0.getType().isAir()) {
         SlimefunItem var1 = SlimefunItem.getByItem(var0);
         if (var1 != null && "KOMUTECH_L_ZJ_萬象匱".equals(var1.getId())) {
            return true;
         } else if (var0.hasItemMeta() && var0.getItemMeta().hasDisplayName()) {
            String var2 = ChatColor.stripColor(var0.getItemMeta().getDisplayName());
            return var2.contains("萬象匱") || var2.contains("萬象匣") || var2.contains("万象匣") || var2.contains("万象匱");
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static Map<String, Object> search(Map<String, Object> var0, String var1) {
      String var2 = var1 == null ? "" : var1.toLowerCase(Locale.ROOT);
      Map var3 = KomutechJson.asMap(var0.get("pages"));
      record Entry(String serialized, String name) {
      }

      ArrayList<Entry> var4 = new ArrayList<>();

      for(Object var6 : var3.values()) {
         for(Object var8 : KomutechJson.asList(var6)) {
            if (var8 != null) {
               ItemStack var9 = deserialize(var8);
               if (var9 != null && searchText(var9).contains(var2)) {
                  String var10 = var9.hasItemMeta() && var9.getItemMeta().hasDisplayName() ? var9.getItemMeta().getDisplayName().replaceAll("§.", "") : var9.getType().name();
                  var4.add(new Entry(String.valueOf(var8), var10));
               }
            }
         }
      }

      var4.sort(Comparator.comparing(Entry::name, COLLATOR));
      LinkedHashMap var12 = new LinkedHashMap();
      LinkedHashMap var13 = new LinkedHashMap();
      int var14 = 0;

      for(Entry var16 : var4) {
         int var17 = var14 / 45 + 1;
         List var11 = (List)var13.computeIfAbsent(String.valueOf(var17), (var0x) -> emptyPage());
         var11.set(var14 % 45, var16.serialized());
         ++var14;
      }

      var12.put("pages", var13.isEmpty() ? Map.of("1", emptyPage()) : var13);
      return var12;
   }

   private static String searchText(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      var1.add(var0.getType().name().toLowerCase(Locale.ROOT));
      if (var0.hasItemMeta() && var0.getItemMeta().hasDisplayName()) {
         var1.add(var0.getItemMeta().getDisplayName().replaceAll("§.", "").toLowerCase(Locale.ROOT));
      }

      SlimefunItem var2 = SlimefunItem.getByItem(var0);
      if (var2 != null) {
         var1.add(var2.getId().toLowerCase(Locale.ROOT));
      }

      return String.join(" ", var1);
   }

   public static Map<String, Object> sortAll(Map<String, Object> var0) {
      Map var1 = KomutechJson.asMap(var0.get("pages"));
      record Entry(String serialized, String name) {
      }

      ArrayList<Entry> var2 = new ArrayList<>();

      for(Object var4 : var1.values()) {
         for(Object var6 : KomutechJson.asList(var4)) {
            if (var6 != null) {
               ItemStack var7 = deserialize(var6);
               if (var7 != null) {
                  String var8 = var7.hasItemMeta() && var7.getItemMeta().hasDisplayName() ? var7.getItemMeta().getDisplayName().replaceAll("§.", "") : var7.getType().name();
                  var2.add(new Entry(String.valueOf(var6), var8));
               }
            }
         }
      }

      var2.sort(Comparator.comparing(Entry::name, COLLATOR));
      LinkedHashMap var9 = new LinkedHashMap();
      int var10 = 0;

      for(Entry var12 : var2) {
         int var13 = var10 / 45 + 1;
         List var14 = (List)var9.computeIfAbsent(String.valueOf(var13), (var0x) -> emptyPage());
         var14.set(var10 % 45, var12.serialized());
         ++var10;
      }

      if (var9.isEmpty()) {
         var9.put("1", emptyPage());
      }

      var0.put("pages", var9);
      var0.put("meta", Map.of("page", "1", "mode", "normal", "kw", ""));
      return var0;
   }

   public static List<String> defaultBlacklist() {
      return List.of("KOMUTECH_L_ZJ_萬象匱", "KOMUTECH_L_ZJ_萬衍儀", "KOMUTECH_L_ZJ_無");
   }

   public static List<String> blacklist() {
      List var0 = KomutechAddonConfig.getStringList("L_ZJ_WXG_BAN");
      return var0.isEmpty() ? defaultBlacklist() : var0;
   }

   public static int storageLimit() {
      return KomutechAddonConfig.getInt("L_ZJ_WXG_CCmax", 5);
   }

   public static String clearPassword() {
      return KomutechAddonConfig.getString("L_ZJ_WXG_MM", "0108");
   }

   public static boolean isValidStorageName(String var0) {
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.trim();
         if (!var1.isEmpty() && !var1.matches(".*[\\\\/:*?\"<>|].*")) {
            return !"无效".equals(var1) && !"未命名".equals(var1) && !"无".equals(var1);
         } else {
            return false;
         }
      }
   }

   public static int countPlayerStorages(String var0) {
      return listStorages(var0).size();
   }

   public static boolean storageFileExists(String var0, String var1) {
      return Files.exists(filePath(var0, var1), new LinkOption[0]);
   }

   public static List<String> listStorages(String var0) {
      ArrayList var1 = new ArrayList();

      try {
         Path var2 = dataDir();
         if (!Files.isDirectory(var2, new LinkOption[0])) {
            return var1;
         }

         String var3 = "[" + var0 + "]";
         Stream<Path> var4 = Files.list(var2);

         try {
            Stream var10000 = var4.filter((var0x) -> Files.isRegularFile(var0x, new LinkOption[0])).map((var0x) -> var0x.getFileName().toString()).filter((var1x) -> var1x.startsWith(var3) && var1x.endsWith(".json")).map((var1x) -> var1x.substring(var3.length(), var1x.length() - 5));
            Objects.requireNonNull(var1);
            var10000.forEach(var1::add);
         } catch (Throwable var8) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (IOException var9) {
      }

      return var1;
   }

   public static List<String> listAllPlayers() {
      TreeSet var0 = new TreeSet();

      try {
         Path var1 = dataDir();
         if (!Files.isDirectory(var1, new LinkOption[0])) {
            return List.of();
         }

         Stream<Path> var2 = Files.list(var1);

         try {
            Stream var10000 = var2.filter((var0x) -> Files.isRegularFile(var0x, new LinkOption[0])).map((var0x) -> var0x.getFileName().toString()).filter((var0x) -> var0x.startsWith("[") && var0x.contains("]") && var0x.endsWith(".json")).map((var0x) -> var0x.substring(1, var0x.indexOf(93)));
            Objects.requireNonNull(var0);
            var10000.forEach(var0::add);
         } catch (Throwable var6) {
            if (var2 != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (var2 != null) {
            var2.close();
         }
      } catch (IOException var7) {
      }

      return new ArrayList(var0);
   }

   public static boolean renameStorage(String var0, String var1, String var2) {
      if (isValidStorageName(var2) && !storageFileExists(var0, var2)) {
         Path var3 = filePath(var0, var1);
         Path var4 = filePath(var0, var2);

         try {
            Files.move(var3, var4);
            return true;
         } catch (IOException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void deleteStorage(String var0, String var1) {
      try {
         Files.deleteIfExists(filePath(var0, var1));
      } catch (IOException var3) {
      }

   }

   public static void deletePlayerStorages(String var0) {
      for(String var2 : listStorages(var0)) {
         deleteStorage(var0, var2);
      }

   }

   public static void deleteAllStorages() {
      try {
         Path var0 = dataDir();
         if (!Files.isDirectory(var0, new LinkOption[0])) {
            return;
         }

         Stream<Path> var1 = Files.list(var0);

         try {
            var1.filter((var0x) -> Files.isRegularFile(var0x, new LinkOption[0])).filter((var0x) -> var0x.getFileName().toString().endsWith(".json")).forEach((var0x) -> {
               try {
                  Files.deleteIfExists(var0x);
               } catch (IOException var2) {
               }

            });
         } catch (Throwable var5) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (var1 != null) {
            var1.close();
         }
      } catch (IOException var6) {
      }

   }

   public static boolean isBlacklisted(ItemStack var0) {
      if (var0 != null && !var0.getType().isAir()) {
         SlimefunItem var1 = SlimefunItem.getByItem(var0);
         return var1 == null ? false : blacklist().contains(var1.getId());
      } else {
         return false;
      }
   }

   public static boolean exists(Map<String, Object> var0, ItemStack var1) {
      String var2 = serialize(var1);
      if (var2 == null) {
         return false;
      } else {
         for(Object var4 : KomutechJson.asMap(var0.get("pages")).values()) {
            for(Object var6 : KomutechJson.asList(var4)) {
               if (var6 != null && var2.equals(normalizedEntry(var6))) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static SlotRef findEmptySlot(Map<String, Object> var0) {
      Map<String, Object> var1 = KomutechJson.asMap(var0.get("pages"));
      ArrayList<Integer> var2 = new ArrayList<>();

      for(String var4 : var1.keySet()) {
         try {
            var2.add(Integer.parseInt(var4));
         } catch (NumberFormatException var7) {
         }
      }

      var2.sort(Integer::compareTo);

      for(int var10 : var2) {
         List var5 = KomutechJson.asList(var1.get(String.valueOf(var10)));

         for(int var6 = 0; var6 < Math.min(45, var5.size()); ++var6) {
            if (var5.get(var6) == null) {
               return new SlotRef(String.valueOf(var10), var6);
            }
         }
      }

      String var9 = String.valueOf(var2.isEmpty() ? 1 : (Integer)var2.get(var2.size() - 1) + 1);
      var1.put(var9, emptyPage());
      var0.put("pages", var1);
      return new SlotRef(var9, 0);
   }

   public static StoreResult store(Map<String, Object> var0, ItemStack var1) {
      if (isBlacklisted(var1)) {
         return WanXiangGuiStorage.StoreResult.fail("禁止存入");
      } else if (exists(var0, var1)) {
         return WanXiangGuiStorage.StoreResult.fail("物品已存在");
      } else {
         String var2 = serialize(var1);
         if (var2 == null) {
            return WanXiangGuiStorage.StoreResult.fail("无法序列化");
         } else {
            SlotRef var3 = findEmptySlot(var0);
            setPageEntry(var0, var3.page(), var3.slot(), var2);
            return WanXiangGuiStorage.StoreResult.ok(var3.page(), var3.slot());
         }
      }
   }

   public static void removeSerialized(Map<String, Object> var0, Object var1) {
      if (var1 != null) {
         String var2 = normalizedEntry(var1);
         Map<String, Object> var3 = KomutechJson.asMap(var0.get("pages"));

         for(String var5 : new ArrayList<>(var3.keySet())) {
            List var6 = KomutechJson.asList(var3.get(var5));

            for(int var7 = 0; var7 < var6.size(); ++var7) {
               Object var8 = var6.get(var7);
               if (var8 != null && var2.equals(normalizedEntry(var8))) {
                  var6.set(var7, (Object)null);
                  var3.put(var5, var6);
                  var0.put("pages", var3);
                  return;
               }
            }
         }

      }
   }

   public static String metaPage(Map<String, Object> var0) {
      return KomutechJson.getString(KomutechJson.asMap(var0.get("meta")), "page", "1");
   }

   public static String metaMode(Map<String, Object> var0) {
      return KomutechJson.getString(KomutechJson.asMap(var0.get("meta")), "mode", "normal");
   }

   public static String metaKeyword(Map<String, Object> var0) {
      return KomutechJson.getString(KomutechJson.asMap(var0.get("meta")), "kw", "");
   }

   public static void setMeta(Map<String, Object> var0, String var1, String var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("page", var1 == null ? "1" : var1);
      var4.put("mode", var2 == null ? "normal" : var2);
      var4.put("kw", var3 == null ? "" : var3);
      var0.put("meta", var4);
   }

   public static int countStoredItems(Map<String, Object> var0) {
      int var1 = 0;

      for(Object var3 : KomutechJson.asMap(var0.get("pages")).values()) {
         for(Object var5 : KomutechJson.asList(var3)) {
            if (var5 != null) {
               ++var1;
            }
         }
      }

      return var1;
   }

   public static LoadedView loadView(String var0, String var1) {
      Map var2 = read(var0, var1);
      String var3 = metaPage(var2);
      String var4 = metaMode(var2);
      String var5 = metaKeyword(var2);
      Map var6 = null;
      if ("search".equals(var4) && var5 != null && !var5.isEmpty()) {
         var6 = search(var2, var5);
         List var7 = KomutechJson.asList(KomutechJson.asMap(var6.get("pages")).get("1"));
         if (var7 == null || var7.isEmpty() || var7.get(0) == null) {
            var4 = "normal";
            var5 = "";
            var6 = null;
            var3 = "1";
            setMeta(var2, var3, var4, var5);
            write(var0, var1, var2);
         }
      }

      return new LoadedView(var2, var3, var4, var6, var5);
   }

   static {
      COLLATOR = Collator.getInstance(Locale.CHINA);
      STORAGE_LORE_PREFIXES = new String[]{"§7┃ 存储标识: §f", "§7┃ 存儲標識: §f", "§7| 存储标识: §f", "§7| 存儲標識: §f"};
      STORAGE_MARKERS = new String[]{"存储标识:", "存儲標識:", "存储標識:", "存儲标识:"};
   }

   public static record ReadResult(Map<String, Object> data, boolean unnamed) {
   }

   public static record SlotRef(String page, int slot) {
   }

   public static record StoreResult(boolean success, String reason, String page, int slot) {
      public static StoreResult fail(String var0) {
         return new StoreResult(false, var0, (String)null, -1);
      }

      public static StoreResult ok(String var0, int var1) {
         return new StoreResult(true, (String)null, var0, var1);
      }
   }

   public static record LoadedView(Map<String, Object> data, String page, String mode, Map<String, Object> searchData, String keyword) {
   }
}
