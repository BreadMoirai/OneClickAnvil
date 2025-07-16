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

@Environment(EnvType.CLIENT)
public class OneClickAnvilConfig {
   private static final Path CONFIG_PATH;
   private static final OneClickAnvilConfig INSTANCE;
   private static final transient Gson GSON;

   static {
      CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("oneclickanvil.json");
      INSTANCE = new OneClickAnvilConfig();
      GSON = new GsonBuilder()
              .setPrettyPrinting()
              .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
              .create();
   }


   private String item = "";
   private String rename = "";

   public static OneClickAnvilConfig getInstance() {
      return INSTANCE;
   }

   public static void loadModConfig() {
      if (Files.exists(CONFIG_PATH)) {
         try {
            String s = Files.readString(CONFIG_PATH);
            OneClickAnvilConfig config = GSON.fromJson(s, OneClickAnvilConfig.class);
            OneClickAnvilConfig instance = getInstance();
            instance.item = config.item;
            instance.rename = config.rename;
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


   public String getItem() {
      return item;
   }

   public void setItem(String item) {
      this.item = item;
   }

   public String getRename() {
      return rename;
   }

   public void setRename(String name) {
      this.rename = name;
   }
}
