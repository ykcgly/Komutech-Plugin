package tech.komutech.native_scripts.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class KomutechJson {
   private KomutechJson() {
   }

   public static Object parse(String var0) {
      return var0 != null && !var0.isBlank() ? new KomutechJson.Parser(var0.trim()).parseValue() : null;
   }

   public static String stringify(Object var0) {
      StringBuilder var1 = new StringBuilder();
      writeValue(var1, var0);
      return var1.toString();
   }

   public static Map<String, Object> asMap(Object var0) {
      if (!(var0 instanceof Map<?, ?> var1)) {
         return new LinkedHashMap<>();
      } else {
         LinkedHashMap var2 = new LinkedHashMap();

         for (Map.Entry<?, ?> var4 : var1.entrySet()) {
            var2.put(String.valueOf(var4.getKey()), var4.getValue());
         }

         return var2;
      }
   }

   public static List<Object> asList(Object var0) {
      return var0 instanceof List var1 ? new ArrayList<>(var1) : new ArrayList<>();
   }

   public static String getString(Map<String, Object> var0, String var1, String var2) {
      return var0 != null && var0.containsKey(var1) && var0.get(var1) != null ? String.valueOf(var0.get(var1)) : var2;
   }

   public static double getDouble(Map<String, Object> var0, String var1, double var2) {
      if (var0 != null && var0.containsKey(var1)) {
         Object var4 = var0.get(var1);
         if (var4 instanceof Number var5) {
            return var5.doubleValue();
         } else {
            try {
               return Double.parseDouble(String.valueOf(var4));
            } catch (NumberFormatException var6) {
               return var2;
            }
         }
      } else {
         return var2;
      }
   }

   public static int getInt(Map<String, Object> var0, String var1, int var2) {
      return (int)Math.round(getDouble(var0, var1, var2));
   }

   public static boolean getBoolean(Map<String, Object> var0, String var1, boolean var2) {
      if (var0 != null && var0.containsKey(var1)) {
         Object var3 = var0.get(var1);
         return var3 instanceof Boolean var4 ? var4 : Boolean.parseBoolean(String.valueOf(var3));
      } else {
         return var2;
      }
   }

   private static void writeValue(StringBuilder var0, Object var1) {
      if (var1 == null) {
         var0.append("null");
      } else if (var1 instanceof String var2) {
         var0.append('"').append(escape(var2)).append('"');
      } else if (var1 instanceof Boolean || var1 instanceof Number) {
         var0.append(var1);
      } else if (var1 instanceof Map<?, ?> var3) {
         var0.append('{');
         boolean var5 = true;

         for (Map.Entry<?, ?> var7 : var3.entrySet()) {
            if (!var5) {
               var0.append(',');
            }

            var5 = false;
            var0.append('"').append(escape(String.valueOf(var7.getKey()))).append("\":");
            writeValue(var0, var7.getValue());
         }

         var0.append('}');
      } else if (var1 instanceof List var4) {
         var0.append('[');

         for (int var8 = 0; var8 < var4.size(); var8++) {
            if (var8 > 0) {
               var0.append(',');
            }

            writeValue(var0, var4.get(var8));
         }

         var0.append(']');
      } else {
         var0.append('"').append(escape(String.valueOf(var1))).append('"');
      }
   }

   private static String escape(String var0) {
      return var0.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }

   private static final class Parser {
      private final String text;
      private int index;

      private Parser(String var1) {
         this.text = var1;
      }

      private Object parseValue() {
         this.skipWhitespace();
         if (this.index >= this.text.length()) {
            return null;
         } else {
            char var1 = this.text.charAt(this.index);
            if (var1 == '{') {
               return this.parseObject();
            } else if (var1 == '[') {
               return this.parseArray();
            } else if (var1 == '"') {
               return this.parseString();
            } else if (var1 == 't' && this.text.startsWith("true", this.index)) {
               this.index += 4;
               return true;
            } else if (var1 == 'f' && this.text.startsWith("false", this.index)) {
               this.index += 5;
               return false;
            } else if (var1 == 'n' && this.text.startsWith("null", this.index)) {
               this.index += 4;
               return null;
            } else {
               return this.parseNumber();
            }
         }
      }

      private Map<String, Object> parseObject() {
         LinkedHashMap var1 = new LinkedHashMap();
         this.index++;
         this.skipWhitespace();
         if (this.peek('}')) {
            this.index++;
            return var1;
         } else {
            while (this.index < this.text.length()) {
               this.skipWhitespace();
               String var2 = this.parseString();
               this.skipWhitespace();
               this.expect(':');
               Object var3 = this.parseValue();
               var1.put(var2, var3);
               this.skipWhitespace();
               if (this.peek('}')) {
                  this.index++;
                  return var1;
               }

               this.expect(',');
            }

            return var1;
         }
      }

      private List<Object> parseArray() {
         ArrayList var1 = new ArrayList();
         this.index++;
         this.skipWhitespace();
         if (this.peek(']')) {
            this.index++;
            return var1;
         } else {
            while (this.index < this.text.length()) {
               var1.add(this.parseValue());
               this.skipWhitespace();
               if (this.peek(']')) {
                  this.index++;
                  return var1;
               }

               this.expect(',');
            }

            return var1;
         }
      }

      private String parseString() {
         this.index++;
         StringBuilder var1 = new StringBuilder();

         while (this.index < this.text.length()) {
            char var2 = this.text.charAt(this.index++);
            if (var2 == '"') {
               return var1.toString();
            }

            if (var2 == '\\' && this.index < this.text.length()) {
               char var3 = this.text.charAt(this.index++);
               switch (var3) {
                  case '"':
                  case '/':
                  case '\\':
                     var1.append(var3);
                     break;
                  case 'n':
                     var1.append('\n');
                     break;
                  case 'r':
                     var1.append('\r');
                     break;
                  case 't':
                     var1.append('\t');
                     break;
                  case 'u':
                     if (this.index + 4 <= this.text.length()) {
                        String var4 = this.text.substring(this.index, this.index + 4);
                        var1.append((char)Integer.parseInt(var4, 16));
                        this.index += 4;
                     }
                     break;
                  default:
                     var1.append(var3);
               }
            } else {
               var1.append(var2);
            }
         }

         return var1.toString();
      }

      private Number parseNumber() {
         int var1 = this.index;
         if (this.peek('-')) {
            this.index++;
         }

         while (this.index < this.text.length() && Character.isDigit(this.text.charAt(this.index))) {
            this.index++;
         }

         if (this.index < this.text.length() && this.text.charAt(this.index) == '.') {
            this.index++;

            while (this.index < this.text.length() && Character.isDigit(this.text.charAt(this.index))) {
               this.index++;
            }

            return Double.parseDouble(this.text.substring(var1, this.index));
         } else {
            return Long.parseLong(this.text.substring(var1, this.index));
         }
      }

      private void skipWhitespace() {
         while (this.index < this.text.length() && Character.isWhitespace(this.text.charAt(this.index))) {
            this.index++;
         }
      }

      private boolean peek(char var1) {
         return this.index < this.text.length() && this.text.charAt(this.index) == var1;
      }

      private void expect(char var1) {
         this.skipWhitespace();
         if (this.index < this.text.length() && this.text.charAt(this.index) == var1) {
            this.index++;
         } else {
            throw new IllegalArgumentException("Expected '" + var1 + "' at " + this.index);
         }
      }
   }
}
