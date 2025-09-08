package com.github.breadmoirai.oneclickanvil.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class OneClickAnvilConfig {
   private static final Path CONFIG_PATH;
   private static final OneClickAnvilConfig INSTANCE;
   private static final Gson GSON;

   static {
      CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("oneclickanvil.json");
      INSTANCE = new OneClickAnvilConfig();
      GSON = new GsonBuilder()
         .setPrettyPrinting()
         .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
         .create();
   }

   public static OneClickAnvilConfig getInstance() {
      return INSTANCE;
   }

   public static void loadModConfig() {
      if (Files.exists(CONFIG_PATH)) {
         try {
            String s = Files.readString(CONFIG_PATH);
            OneClickAnvilConfig config = GSON.fromJson(s, OneClickAnvilConfig.class);
            OneClickAnvilConfig instance = getInstance();
            instance.entries.clear();
            instance.entries.addAll(config.entries);
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         saveModConfig();
      }
   }

   public static void saveModConfig() {
      System.out.println("Saving OneClickAnvil Mod Config to " + CONFIG_PATH);
      try {
         String s = GSON.toJson(getInstance());
         Files.writeString(CONFIG_PATH, s);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static class Entry {
      private String item = "";
      private String rename = "";
      private boolean enabled = true;

      public String getItem() {
         return item;
      }

      public void setItem(String item) {
         this.item = item;
      }

      public String getRename() {
         return rename;
      }

      public void setRename(String rename) {
         this.rename = rename;
      }

      public boolean isEnabled() {
         return enabled;
      }

      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
      }
   }

   private final List<Entry> entries = new ArrayList<>();

   public List<Entry> getEntries() {
      return List.copyOf(entries);
   }

   public void addEntry() {
      Entry e = new Entry();
      entries.add(e);
   }

   public void removeEntry(Entry e) {
      entries.remove(e);
   }
}
