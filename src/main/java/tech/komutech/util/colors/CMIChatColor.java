package tech.komutech.util.colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Color;

public class CMIChatColor {
   private static final Map<Character, CMIChatColor> BY_CHAR = new HashMap<>();
   private static final Map<String, CMIChatColor> BY_NAME = new HashMap<>();
   private static final LinkedHashMap<String, CMIChatColor> CUSTOM_BY_NAME = new LinkedHashMap<>();
   private static final Map<String, CMIChatColor> CUSTOM_BY_HEX = new HashMap<>();
   private static final TreeMap<String, CMIChatColor> CUSTOM_BY_RGB = new TreeMap<>();
   public static final String colorReplacerPlaceholder = "＆";
   public static final String hexSymbol = "#";
   public static final String colorHexReplacerPlaceholder = "{＆#";
   public static final String colorFontPrefix = "{@";
   public static final String colorCodePrefix = "{#";
   public static final String colorCodeSuffix = "}";
   public static final String hexColorRegex = "(\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})";
   public static final Pattern cleanOfficialColorRegexPattern;
   public static final Pattern cleanQuirkyHexColorRegexPattern;
   public static final Pattern hexColorRegexPattern;
   public static final Pattern hexColorRegexPatternLast;
   public static final Pattern hexDeColorNamePattern;
   public static final String ColorNameRegex = "(\\{#)([a-zA-Z_]{3,})(\\})";
   public static final Pattern hexColorNamePattern;
   public static final Pattern hexColorNamePatternLast;
   public static final String ColorFontRegex = "(\\{@)([a-zA-Z_]{3,})(\\})";
   public static final Pattern gradientPattern;
   public static final String hexColorDecolRegex = "(&x)(&[0-9A-Fa-f]){6}";
   public static final Pattern postGradientPattern;
   public static final Pattern post2GradientPattern;
   public static final Pattern fullPattern;
   public static final Pattern formatPattern;
   public static final CMIChatColor BLACK;
   public static final CMIChatColor DARK_BLUE;
   public static final CMIChatColor DARK_GREEN;
   public static final CMIChatColor DARK_AQUA;
   public static final CMIChatColor DARK_RED;
   public static final CMIChatColor DARK_PURPLE;
   public static final CMIChatColor GOLD;
   public static final CMIChatColor GRAY;
   public static final CMIChatColor DARK_GRAY;
   public static final CMIChatColor BLUE;
   public static final CMIChatColor GREEN;
   public static final CMIChatColor AQUA;
   public static final CMIChatColor RED;
   public static final CMIChatColor LIGHT_PURPLE;
   public static final CMIChatColor YELLOW;
   public static final CMIChatColor WHITE;
   public static final CMIChatColor OBFUSCATED;
   public static final CMIChatColor BOLD;
   public static final CMIChatColor STRIKETHROUGH;
   public static final CMIChatColor UNDERLINE;
   public static final CMIChatColor ITALIC;
   public static final CMIChatColor RESET;
   public static final CMIChatColor HEX;
   private char c = '\n';
   private boolean colorable = true;
   private boolean isReset = false;
   private Pattern pattern = null;
   private int redChannel = -1;
   private int greenChannel = -1;
   private int blueChannel = -1;
   private String hexCode = null;
   private String name;

   private static String charEscape(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         switch (var3) {
            case '\b':
               var1.append("\\b");
               break;
            case '\t':
               var1.append("\\t");
               break;
            case '\n':
               var1.append("\\n");
               break;
            case '\f':
               var1.append("\\f");
               break;
            case '\r':
               var1.append("\\r");
               break;
            case '"':
               var1.append("\\\"");
               break;
            case '/':
               var1.append("/");
               break;
            case '\\':
               var1.append("\\\\");
               break;
            default:
               if ((var3 < 0 || var3 > 31) && (var3 < 127 || var3 > 159) && (var3 < 8192 || var3 > 8447)) {
                  var1.append(var3);
               } else {
                  String var4 = Integer.toHexString(var3);
                  var1.append("\\u");
                  var1.append("0".repeat(4 - var4.length()));
                  var1.append(var4.toUpperCase());
               }
         }
      }

      return var1.toString();
   }

   private static String escape(String var0) {
      return var0.replace("#", "\\#").replace("{", "\\{").replace("}", "\\}");
   }

   public CMIChatColor(String var1, char var2, int var3, int var4, int var5) {
      this(var1, var2, true, false, var3, var4, var5);
   }

   public CMIChatColor(String var1) {
      this(null, var1);
   }

   public CMIChatColor(String var1, String var2) {
      if (var2 != null) {
         if (var2.startsWith("{#")) {
            var2 = var2.substring("{#".length());
         }

         if (var2.endsWith("}")) {
            var2 = var2.substring(0, var2.length() - "}".length());
         }

         if (var2.startsWith("#")) {
            var2 = var2.substring(1);
         }

         if (var2.length() == 3 || var2.length() == 6) {
            this.hexCode = var2;
         }

         this.name = var1;

         try {
            if (this.hexCode != null) {
               this.redChannel = Integer.valueOf(this.hexCode.substring(0, 2), 16);
               this.greenChannel = Integer.valueOf(this.hexCode.substring(2, 4), 16);
               this.blueChannel = Integer.parseInt(this.hexCode.substring(4, 6), 16);
            }
         } catch (Throwable var4) {
            this.redChannel = -1;
            this.greenChannel = -1;
            this.blueChannel = -1;
            this.hexCode = null;
         }
      }
   }

   public CMIChatColor(String var1, char var2, Boolean var3) {
      this(var1, var2, var3, false);
   }

   public CMIChatColor(String var1, char var2, Boolean var3, Boolean var4) {
      this(var1, var2, var3, var4, -1, -1, -1);
   }

   public CMIChatColor(String var1, char var2, Boolean var3, Boolean var4, int var5, int var6, int var7) {
      this.name = var1;
      this.c = var2;
      this.colorable = var3;
      this.isReset = var4;
      this.pattern = Pattern.compile("(?i)(&[" + var2 + "])");
      this.redChannel = var5;
      this.greenChannel = var6;
      this.blueChannel = var7;
      BY_CHAR.put(var2, this);
      BY_NAME.put(this.getName().toLowerCase().replace("_", ""), this);
   }

   public boolean isValid() {
      return this.c != '\n' || this.getHex() != null || this.name != null || this.blueChannel > -1 && this.greenChannel > -1 && this.redChannel > -1;
   }

   public static String processGradient(String var0) {
      Matcher var1 = gradientPattern.matcher(var0);

      while (var1.find()) {
         String var2 = var1.group();
         CMIChatColor var3 = getColor("{#" + var1.group(2).replace("#", "") + "}");
         CMIChatColor var4 = getColor("{#" + var1.group(5).replace("#", "") + "}");
         if (var3 != null && var4 != null) {
            String var5 = var1.group(3);
            boolean var6 = !var1.group(6).isEmpty();
            StringBuilder var7 = new StringBuilder();
            Set<CMIChatColor> var8 = getFormats(var5);
            var5 = stripColor(var5);

            for (int var9 = 0; var9 < var5.length(); var9++) {
               char var11 = var5.charAt(var9);
               int var12 = var5.length();
               var12 = Math.max(var12, 2);
               double var13 = var9 * 100.0 / (var12 - 1);
               CMIChatColor var15 = mixColors(var3, var4, var13);
               var7.append("{#").append(var15.getHex()).append("}");
               if (!var8.isEmpty()) {
                  for (CMIChatColor var17 : var8) {
                     var7.append("&").append(var17.getChar());
                  }
               }

               if (Character.codePointAt(var5, var9) > 127) {
                  if (Character.isSurrogate(var11)) {
                     var7.append(var11).append(var5.charAt(var9 + 1));
                     var9++;
                  } else {
                     var7.append(var11);
                  }
               } else {
                  var7.append(var11);
               }
            }

            if (var6) {
               var7.append("{#").append(var1.group(5).replace("#", "")).append(">").append("}");
            }

            var0 = var0.replace(var2, var7.toString());
            if (var6) {
               var0 = processGradient(var0);
            }
         }
      }

      return var0;
   }

   public static List<String> translate(List<String> var0) {
      var0.replaceAll(CMIChatColor::translate);
      return var0;
   }

   public static String translate(String var0) {
      if (var0 == null) {
         return null;
      } else {
         if ((var0 = processGradient(var0)).contains("{#")) {
            Matcher var4 = hexColorRegexPattern.matcher(var0);

            while (var4.find()) {
               String var3 = var4.group();
               StringBuilder var2 = new StringBuilder("§x");

               for (char var8 : var3.substring(2, var3.length() - 1).toCharArray()) {
                  var2.append('§').append(var8);
                  if (var3.substring(2, var3.length() - 1).length() == 3) {
                     var2.append('§').append(var8);
                  }
               }

               var0 = var0.replace(var3, var2.toString());
            }

            Matcher var22 = hexColorNamePattern.matcher(var0);

            while (var22.find()) {
               String var25 = var22.group(2);
               CMIChatColor var28 = getByCustomName(var25.toLowerCase().replace("_", ""));
               if (var28 != null) {
                  String var31 = var28.getHex();
                  StringBuilder var9 = new StringBuilder("§x");

                  for (char var13 : var31.toCharArray()) {
                     var9.append('§').append(var13);
                  }

                  var0 = var0.replace(var22.group(), var9.toString());
               }
            }
         }

         if (var0.contains("&#")) {
            Matcher var20 = cleanQuirkyHexColorRegexPattern.matcher(var0);

            while (var20.find()) {
               String var18 = var20.group();
               StringBuilder var16 = new StringBuilder("§x");
               String var1 = var18.substring(2);

               for (char var32 : var1.toCharArray()) {
                  var16.append('§').append(var32);
                  if (var1.length() == 3) {
                     var16.append('§').append(var32);
                  }
               }

               var0 = var0.replace(var18, var16.toString());
            }
         }

         if (var0.contains("#")) {
            Matcher var21 = cleanOfficialColorRegexPattern.matcher(var0);

            while (var21.find()) {
               String var19 = var21.group();
               StringBuilder var17 = new StringBuilder("§x");
               String var15 = var19.substring(1);

               for (char var33 : var15.toCharArray()) {
                  var17.append('§').append(var33);
                  if (var15.length() == 3) {
                     var17.append('§').append(var33);
                  }
               }

               var0 = var0.replace(var19, var17.toString());
            }
         }

         return ChatColor.translateAlternateColorCodes('&', var0);
      }
   }

   public static String convertNamedHex(String var0) {
      if (var0 == null) {
         return null;
      } else {
         if ((var0 = processGradient(var0)).contains("{#")) {
            Matcher var1 = hexColorNamePattern.matcher(var0);

            while (var1.find()) {
               String var2 = var1.group(2);
               CMIChatColor var3 = getByCustomName(var2.toLowerCase().replace("_", ""));
               if (var3 != null) {
                  String var4 = var3.getHex();
                  StringBuilder var5 = new StringBuilder("{#");

                  for (char var9 : var4.toCharArray()) {
                     var5.append('&').append(var9);
                  }

                  var5.append("}");
                  var0 = var0.replace(var1.group(), var5.toString());
               }
            }
         }

         return var0;
      }
   }

   public static String applyEqualGradient(String var0, List<CMIChatColor> var1) {
      if (var1 != null && !var1.isEmpty()) {
         int var2 = var0.length() / var1.size();
         StringBuilder var3 = new StringBuilder();
         var3.append(((CMIChatColor)var1.get(0)).getFormatedHex(">"));

         for (int var4 = 0; var4 <= var1.size() - 1; var4++) {
            if (var4 > 0 && var2 > 0) {
               var3.append(((CMIChatColor)var1.get(var4)).getFormatedHex("<>"));
            }

            for (int var5 = 0; var5 < var2; var5++) {
               var3.append(var0.charAt(0));
               var0 = var0.substring(1);
            }
         }

         var3.append(var0).append(((CMIChatColor)var1.get(var1.size() - 1)).getFormatedHex("<"));
         return var3.toString();
      } else {
         return var0;
      }
   }

   @Deprecated
   public static String translateAlternateColorCodes(String var0) {
      return translate(var0);
   }

   public static String colorize(String var0) {
      return var0 == null ? null : translate(var0);
   }

   public static String simpleFlaten(String var0) {
      return var0.replace("&", "＆").replace("{#", "{＆#");
   }

   public static String flaten(String var0) {
      return flaten(var0, true);
   }

   public static String flaten(String var0, boolean var1) {
      return deColorize(var0, var1).replace("&", "＆").replace("{#", "{＆#");
   }

   public static String deColorize(String var0) {
      return deColorize(var0, true);
   }

   public static String deColorize(String var0, boolean var1) {
      if (var0 == null) {
         return null;
      } else {
         if (var1) {
            var0 = translate(var0);
         }

         if ((var0 = var0.replace("§", "&")).contains("&x")) {
            Matcher var2 = hexDeColorNamePattern.matcher(var0);

            while (var2.find()) {
               String var3 = var2.group(3).replace("&", "");
               CMIChatColor var4 = CUSTOM_BY_HEX.get(var3.toLowerCase());
               if (var4 != null) {
                  var0 = var0.replace(var2.group(), "{#" + var4.getName().toLowerCase().replace("_", "") + "}");
               } else {
                  var0 = var0.replace(var2.group(), "{#" + var3 + "}");
               }
            }
         }

         return var0;
      }
   }

   public static List<String> deColorize(List<String> var0) {
      var0.replaceAll(CMIChatColor::deColorize);
      return var0;
   }

   public static String stripColor(String var0) {
      if (var0 == null) {
         return null;
      } else {
         var0 = translate(var0);
         return ChatColor.stripColor(var0);
      }
   }

   public static String stripHexColor(String var0) {
      var0 = translate(var0);
      Matcher var2 = hexColorRegexPattern.matcher(var0);

      while (var2.find()) {
         String var1 = var2.group();
         var0 = var0.replace(var1, "");
      }

      if (var0.contains("&x") || var0.contains("§x")) {
         var2 = hexDeColorNamePattern.matcher(var0);

         while (var2.find()) {
            String var4 = var2.group();
            var0 = var0.replace(var4, "");
         }
      }

      return var0;
   }

   public static String getLastColors(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var5;
         Matcher var1 = hexColorRegexPatternLast.matcher(var5 = deColorize(var0));
         if (var1.find()) {
            String var7 = var1.group(0);
            if (var5.endsWith(var7)) {
               return var7;
            } else {
               String[] var8 = var5.split(escape(var7), 2);
               if (var8.length < 2) {
                  return var7;
               } else {
                  String var9 = getLastColors(var8[1]);
                  return var9 != null && !var9.isEmpty() ? var9 : var7;
               }
            }
         } else {
            var1 = hexColorNamePatternLast.matcher(var5);
            if (!var1.find()) {
               return ChatColor.getLastColors(translate(var5));
            } else {
               String var2 = var1.group();
               if (var5.endsWith(var2)) {
                  return var2;
               } else {
                  String[] var3 = var5.split(escape(var2), 2);
                  if (var3.length < 2) {
                     return var2;
                  } else {
                     String var4 = getLastColors(var3[1]);
                     return var4 != null && !var4.isEmpty() ? var4 : var2;
                  }
               }
            }
         }
      }
   }

   public String getColorCode() {
      return this.hexCode != null ? "{#" + this.hexCode + "}" : "&" + this.c;
   }

   public String getBukkitColorCode() {
      return this.hexCode != null ? translate("{#" + this.hexCode + "}") : "§" + this.c;
   }

   @Override
   public String toString() {
      return this.getBukkitColorCode();
   }

   public char getChar() {
      return this.c;
   }

   public void setChar(char var1) {
      this.c = var1;
   }

   public boolean isFormat() {
      return !this.colorable && !this.isReset;
   }

   public boolean isReset() {
      return this.isReset;
   }

   public ChatColor getColor() {
      return ChatColor.getByChar(this.getChar());
   }

   public static Set<CMIChatColor> getFormats(String var0) {
      var0 = var0.replace("§", "&");
      HashSet var1 = new HashSet();
      Matcher var2 = formatPattern.matcher(var0);

      while (var2.find()) {
         String var3 = var2.group();
         CMIChatColor var4 = getFormat(var3);
         if (var4 != null && var4.isFormat()) {
            var1.add(var4);
         }
      }

      return var1;
   }

   public static CMIChatColor getFormat(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = deColorize(var0);
         if ((var0 = var0.replace("§", "&")).length() > 1) {
            String var2 = var0.toLowerCase().replace("_", "");
            CMIChatColor var3 = BY_NAME.get(var2);
            if (var3 != null) {
               return var3;
            }

            var3 = CUSTOM_BY_NAME.get(var2);
            if (var3 != null) {
               return var3;
            }
         }

         if (var1.length() > 1 && String.valueOf(var1.charAt(var1.length() - 2)).equalsIgnoreCase("&")) {
            var0 = var0.substring(var0.length() - 1);

            for (Entry var8 : BY_CHAR.entrySet()) {
               if (String.valueOf(var8.getKey()).equalsIgnoreCase(var0)) {
                  return ((CMIChatColor)var8.getValue()).isFormat() ? (CMIChatColor)var8.getValue() : null;
               }
            }
         }

         return null;
      }
   }

   public static CMIChatColor getColor(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var2 = deColorize(var0);
         if (var2.contains("{#")) {
            Matcher var3 = hexColorRegexPatternLast.matcher(var2);
            if (var3.find()) {
               return new CMIChatColor(var3.group(2));
            }

            var3 = hexColorNamePatternLast.matcher(var2);
            if (var3.find()) {
               return getByCustomName(var3.group(2));
            }
         }

         if ((var0 = deColorize(var0).replace("&", "")).length() > 1) {
            String var8 = var0.toLowerCase().replace("_", "");
            CMIChatColor var4 = BY_NAME.get(var8);
            if (var4 != null) {
               return var4;
            }

            CMIChatColor var5 = CUSTOM_BY_NAME.get(var8);
            if (var5 != null) {
               return var5;
            }
         }

         if (var2.length() > 1 && String.valueOf(var2.charAt(var2.length() - 2)).equalsIgnoreCase("&")) {
            var0 = var0.substring(var0.length() - 1);

            for (Entry var10 : BY_CHAR.entrySet()) {
               if (String.valueOf(var10.getKey()).equalsIgnoreCase(var0)) {
                  return (CMIChatColor)var10.getValue();
               }
            }
         }

         CMIChatColor var1;
         return (var1 = new CMIChatColor(var0)).getHex() != null ? var1 : null;
      }
   }

   public static CMIChatColor getRandomColor() {
      ArrayList var0 = new ArrayList();

      for (Entry var2 : BY_NAME.entrySet()) {
         if (((CMIChatColor)var2.getValue()).isColorable()) {
            var0.add((CMIChatColor)var2.getValue());
         }
      }

      Collections.shuffle(var0);
      return (CMIChatColor)var0.get(0);
   }

   public Color getRGBColor() {
      return this.blueChannel < 0 ? null : Color.fromRGB(this.redChannel, this.greenChannel, this.blueChannel);
   }

   public java.awt.Color getJavaColor() {
      return this.blueChannel < 0 ? null : new java.awt.Color(this.redChannel, this.greenChannel, this.blueChannel);
   }

   public String getHex() {
      return this.hexCode;
   }

   public String getFormatedHex() {
      return this.getFormatedHex(null);
   }

   public String getFormatedHex(String var1) {
      return "{#" + this.hexCode + (var1 == null ? "" : var1) + "}";
   }

   public String getCleanName() {
      return this.name == null ? this.getHex() : this.name.replace("_", "");
   }

   public static CMIChatColor getByCustomName(String var0) {
      if (var0.startsWith("{#")) {
         var0 = var0.substring("{#".length());
      }

      if (var0.endsWith("}")) {
         var0 = var0.substring(0, var0.length() - "}".length());
      }

      if (var0.equalsIgnoreCase("random")) {
         ArrayList var1 = new ArrayList<>(CUSTOM_BY_NAME.values());
         int var2 = new Random().nextInt(var1.size());
         return (CMIChatColor)var1.get(var2);
      } else {
         return CUSTOM_BY_NAME.get(var0.toLowerCase().replace("_", ""));
      }
   }

   public static CMIChatColor getByHex(String var0) {
      if (var0.startsWith("{#")) {
         var0 = var0.substring("{#".length());
      }

      if (var0.endsWith("}")) {
         var0 = var0.substring(0, var0.length() - "}".length());
      }

      return CUSTOM_BY_HEX.get(var0.toLowerCase().replace("_", ""));
   }

   public static Map<String, CMIChatColor> getByName() {
      return BY_NAME;
   }

   public static Map<String, CMIChatColor> getByCustomName() {
      return CUSTOM_BY_NAME;
   }

   public static String getHexFromCoord(int var0, int var1) {
      var0 = var0 < 0 ? 0 : Math.min(var0, 255);
      var1 = var1 < 0 ? 0 : Math.min(var1, 255);
      int var2 = (int)(255.0 - var1 * 255 * (1.0 + Math.sin(6.3 * var0)) / 2.0);
      int var3 = (int)(255.0 - var1 * 255 * (1.0 + Math.cos(6.3 * var0)) / 2.0);
      int var4 = (int)(255.0 - var1 * 255 * (1.0 - Math.sin(6.3 * var0)) / 2.0);
      StringBuilder var5 = new StringBuilder().append(Integer.toHexString((var4 << 16) + (var3 << 8) + var2 & 16777215));

      while (var5.length() < 6) {
         var5.append("0").append((CharSequence)var5);
      }

      return "#" + var5;
   }

   public static String getHexRedGreenByPercent(int var0, int var1) {
      float var2 = var0 * 33.0F / 100.0F / 100.0F;
      java.awt.Color var3 = java.awt.Color.getHSBColor(var2, 1.0F, 1.0F);
      StringBuilder var4 = new StringBuilder().append(Integer.toHexString((var3.getRed() << 16) + (var3.getGreen() << 8) + var3.getBlue() & 16777215));

      while (var4.length() < 6) {
         var4.append("0").append((CharSequence)var4);
      }

      return "#" + var4;
   }

   public int getRed() {
      return this.redChannel;
   }

   public int getGreen() {
      return this.greenChannel;
   }

   public int getBlue() {
      return this.blueChannel;
   }

   public static CMIChatColor getClosest(String var0) {
      if (var0.startsWith("#")) {
         var0 = var0.substring(1);
      }

      CMIChatColor var2;
      if ((var2 = CUSTOM_BY_RGB.get(var0)) != null) {
         return var2;
      } else {
         java.awt.Color var1;
         try {
            var1 = new java.awt.Color(
               Integer.valueOf(var0.substring(0, 2), 16), Integer.valueOf(var0.substring(2, 4), 16), Integer.valueOf(var0.substring(4, 6), 16)
            );
         } catch (Throwable var16) {
            return null;
         }

         double var3 = Double.MAX_VALUE;

         for (Entry var6 : CUSTOM_BY_HEX.entrySet()) {
            java.awt.Color var11 = new java.awt.Color(
               Integer.valueOf(((CMIChatColor)var6.getValue()).hexCode.substring(0, 2), 16),
               Integer.valueOf(((CMIChatColor)var6.getValue()).hexCode.substring(2, 4), 16),
               Integer.valueOf(((CMIChatColor)var6.getValue()).hexCode.substring(4, 6), 16)
            );
            int var12 = var11.getRed();
            int var10;
            int var13 = var12 + (var10 = var1.getRed()) >> 1;
            int var7;
            int var8;
            int var9;
            double var14 = Math.sqrt(
               ((512 + var13) * (var9 = var12 - var10) * var9 >> 8)
                  + 4 * (var8 = var11.getGreen() - var1.getGreen()) * var8
                  + ((767 - var13) * (var7 = var11.getBlue() - var1.getBlue()) * var7 >> 8)
            );
            if (var14 < var3) {
               var2 = (CMIChatColor)var6.getValue();
               var3 = var14;
            }
         }

         if (var2 != null) {
            CUSTOM_BY_RGB.put(var0, var2);
            return var2;
         } else {
            CUSTOM_BY_RGB.put(var0, null);
            return null;
         }
      }
   }

   public static String getClosestVanilla(String var0) {
      try {
         if (var0.startsWith("#")) {
            var0 = var0.substring(1);
         }

         CMIChatColor var2;
         if ((var2 = CUSTOM_BY_HEX.get(var0)) != null && var2.getChar() != '\n') {
            return "&" + var2.getChar();
         }

         java.awt.Color var1;
         try {
            var1 = new java.awt.Color(
               Integer.valueOf(var0.substring(0, 2), 16), Integer.valueOf(var0.substring(2, 4), 16), Integer.valueOf(var0.substring(4, 6), 16)
            );
         } catch (Throwable var17) {
            return null;
         }

         double var3 = Double.MAX_VALUE;
         CMIChatColor var5 = null;

         for (Entry var7 : BY_CHAR.entrySet()) {
            int var8;
            int var9;
            int var10;
            int var11;
            java.awt.Color var12;
            int var13;
            int var14;
            double var15;
            if (((CMIChatColor)var7.getValue()).isColorable()
               && (
                     var15 = Math.sqrt(
                        (
                              (
                                       512
                                          + (
                                             var14 = (
                                                      var13 = (var12 = new java.awt.Color(
                                                            ((CMIChatColor)var7.getValue()).getRed(),
                                                            ((CMIChatColor)var7.getValue()).getGreen(),
                                                            ((CMIChatColor)var7.getValue()).getBlue()
                                                         ))
                                                         .getRed()
                                                   )
                                                   + (var11 = var1.getRed())
                                                >> 1
                                          )
                                    )
                                    * (var10 = var13 - var11)
                                    * var10
                                 >> 8
                           )
                           + 4 * (var9 = var12.getGreen() - var1.getGreen()) * var9
                           + ((767 - var14) * (var8 = var12.getBlue() - var1.getBlue()) * var8 >> 8)
                     )
                  )
                  < var3) {
               var5 = (CMIChatColor)var7.getValue();
               var3 = var15;
            }
         }

         if (var5 != null) {
            if (var2 != null) {
               var2.setChar(var5.getChar());
            } else {
               CUSTOM_BY_HEX.put(var0, var5);
            }

            return "&" + var5.getChar();
         }
      } catch (Throwable var18) {
         var18.printStackTrace();
      }

      return null;
   }

   public CMIChatColor mixColors(CMIChatColor var1, double var2) {
      return mixColors(this, var1, var2);
   }

   public static CMIChatColor mixColors(CMIChatColor var0, CMIChatColor var1, double var2) {
      double var10;
      double var4 = 1.0 - (var10 = var2 / 100.0);
      int var6 = (int)(var1.getRed() * var10 + var0.getRed() * var4);
      int var7 = (int)(var1.getGreen() * var10 + var0.getGreen() * var4);
      int var8 = (int)(var1.getBlue() * var10 + var0.getBlue() * var4);
      String var9 = String.format("#%02x%02x%02x", var6, var7, var8);
      return new CMIChatColor(var9);
   }

   public boolean isColorable() {
      return this.colorable;
   }

   public Pattern getPattern() {
      return this.pattern;
   }

   public String getName() {
      return this.name;
   }

   static {
      for (CMICustomColors var3 : CMICustomColors.values()) {
         CUSTOM_BY_NAME.put(var3.name().toLowerCase().replace("_", ""), new CMIChatColor(var3.toString(), var3.getHex()));
         CUSTOM_BY_HEX.put(var3.getHex().toLowerCase(), new CMIChatColor(var3.toString(), var3.getHex()));
      }

      for (float var5 = 0.0F; var5 <= 1.0F; var5 += 0.1F) {
         for (float var6 = 0.1F; var6 <= 1.0F; var6 += 0.1F) {
            for (float var7 = 0.0F; var7 <= 1.0F; var7 += 0.03F) {
               java.awt.Color var8 = java.awt.Color.getHSBColor(var7, var5, var6);
               StringBuilder var4 = new StringBuilder().append(Integer.toHexString((var8.getRed() << 16) + (var8.getGreen() << 8) + var8.getBlue() & 16777215));

               while (var4.length() < 6) {
                  var4.append("0").append((CharSequence)var4);
               }

               getClosest(var4.toString());
            }
         }
      }

      cleanOfficialColorRegexPattern = Pattern.compile("(?<!\\{|:\"|＆)#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})");
      cleanQuirkyHexColorRegexPattern = Pattern.compile("&#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})");
      hexColorRegexPattern = Pattern.compile("(\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})");
      hexColorRegexPatternLast = Pattern.compile("(\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})(?!.*\\{#)");
      hexDeColorNamePattern = Pattern.compile("((&|§)x)(((&|§)[0-9A-Fa-f]){6})");
      hexColorNamePattern = Pattern.compile("(\\{#)([a-zA-Z_]{3,})(\\})");
      hexColorNamePatternLast = Pattern.compile("(\\{#)([a-zA-Z_]{3,})(\\})(?!.*\\{#)");
      gradientPattern = Pattern.compile("(\\{(#[^\\{]*?)>\\})(.*?)(\\{(#.*?)<(>?)\\})");
      postGradientPattern = Pattern.compile(
         "((\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})|(\\{#)([a-zA-Z_]{3,})(\\}))(.)((\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})|(\\{#)([a-zA-Z_]{3,})(\\}))"
      );
      post2GradientPattern = Pattern.compile(
         "((\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})|(\\{#)([a-zA-Z_]{3,})(\\}))(.)(((\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})|(\\{#)([a-zA-Z_]{3,})(\\}))(.))+"
      );
      fullPattern = Pattern.compile(
         "(&[0123456789abcdefklmnorABCDEFKLMNOR])|(\\{#)([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(\\})|(\\{#)([a-zA-Z_]{3,})(\\})|(\\{@)([a-zA-Z_]{3,})(\\})"
      );
      formatPattern = Pattern.compile("(&[klmnorKLMNOR])");
      BLACK = new CMIChatColor("Black", '0', 0, 0, 0);
      DARK_BLUE = new CMIChatColor("Dark_Blue", '1', 0, 0, 170);
      DARK_GREEN = new CMIChatColor("Dark_Green", '2', 0, 170, 0);
      DARK_AQUA = new CMIChatColor("Dark_Aqua", '3', 0, 170, 170);
      DARK_RED = new CMIChatColor("Dark_Red", '4', 170, 0, 0);
      DARK_PURPLE = new CMIChatColor("Dark_Purple", '5', 170, 0, 170);
      GOLD = new CMIChatColor("Gold", '6', 255, 170, 0);
      GRAY = new CMIChatColor("Gray", '7', 170, 170, 170);
      DARK_GRAY = new CMIChatColor("Dark_Gray", '8', 85, 85, 85);
      BLUE = new CMIChatColor("Blue", '9', 85, 85, 255);
      GREEN = new CMIChatColor("Green", 'a', 85, 255, 85);
      AQUA = new CMIChatColor("Aqua", 'b', 85, 255, 255);
      RED = new CMIChatColor("Red", 'c', 255, 85, 85);
      LIGHT_PURPLE = new CMIChatColor("Light_Purple", 'd', 255, 85, 255);
      YELLOW = new CMIChatColor("Yellow", 'e', 255, 255, 85);
      WHITE = new CMIChatColor("White", 'f', 255, 255, 255);
      OBFUSCATED = new CMIChatColor("Obfuscated", 'k', false);
      BOLD = new CMIChatColor("Bold", 'l', false);
      STRIKETHROUGH = new CMIChatColor("Strikethrough", 'm', false);
      UNDERLINE = new CMIChatColor("Underline", 'n', false);
      ITALIC = new CMIChatColor("Italic", 'o', false);
      RESET = new CMIChatColor("Reset", 'r', false, true);
      HEX = new CMIChatColor("Hex", 'x', false, false);
   }
}
