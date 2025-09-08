package com.github.breadmoirai.oneclickanvil.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.utils.OptionUtils;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;

public class OneClickAnvilConfigScreen extends YACLScreen {
   public Screen parent;

   public OneClickAnvilConfigScreen(Screen parent) {
      super(createConfig(), parent);
      this.parent = parent;
   }

   private static YetAnotherConfigLib createConfig() {
      return YetAnotherConfigLib.createBuilder()
         .title(Text.translatable("oneclickanvil.config.title"))
         .category(categoryForConfig(OneClickAnvilConfig.getInstance()))
         .save(OneClickAnvilConfig::saveModConfig)
         .build();
   }

   private static ConfigCategory categoryForConfig(OneClickAnvilConfig config) {
      ConfigCategory.Builder builder = ConfigCategory.createBuilder()
         .name(Text.translatable("oneclickanvil.config.title"))
         .tooltip(Text.translatable("modmenu.summaryTranslation.one-click-anvil"));
      List<OneClickAnvilConfig.Entry> entries = config.getEntries();
      for (int i = 0; i < entries.size(); i++) {
         builder.group(optionsForEntry(i + 1, entries.get(i)));
      }
      builder.group(OptionGroup.createBuilder()
         .option(
            ButtonOption.createBuilder().name(Text.translatable("oneclickanvil.config.entry.add"))
               .action((yaclScreen, buttonOption) -> {
                     Runnable action = config::addEntry;
                     reloadConfigScreen((OneClickAnvilConfigScreen) yaclScreen, action);
                  }
               ).build()
         ).option(
            ButtonOption.createBuilder().name(Text.translatable("oneclickanvil.config.entry.remove"))
               .action((yaclScreen, buttonOption) -> {
                     Runnable action = () -> config.removeEntry(config.getEntries().getLast());
                     reloadConfigScreen((OneClickAnvilConfigScreen) yaclScreen, action);
                  }
               ).build()
         )
         .build());
      return builder.build();
   }

   private static void reloadConfigScreen(OneClickAnvilConfigScreen configScreen, Runnable action) {
      HashMap<String, String> changes = new HashMap<>();
      OptionUtils.forEachOptions(configScreen.config, option -> {
         if (option.changed()) {
            changes.put(option.description().text().getString(), (String) option.pendingValue());
         }
      });
      action.run();
      OneClickAnvilConfigScreen newScreen = new OneClickAnvilConfigScreen(configScreen.parent);
      OptionUtils.forEachOptions(newScreen.config, option -> {
         String desc = option.description().text().getString();
         if (changes.containsKey(desc)) {
            //noinspection unchecked
            ((Option<String>) option).requestSet(changes.get(desc));
         }
         if (option.changed()) {
            changes.put(desc, (String) option.pendingValue());
         }
      });
      MinecraftClient.getInstance().setScreen(newScreen);
   }

   private static OptionGroup optionsForEntry(int index, OneClickAnvilConfig.Entry entry) {
      return OptionGroup.createBuilder()
         .name(Text.translatable("oneclickanvil.config.entry.name").append(" #" + index))
         .option(Option.<String>createBuilder()
            .name(Text.translatable("oneclickanvil.config.item.name"))
            .description(OptionDescription.of(
               Text.translatable("oneclickanvil.config.entry.name").append(" #" + index + " - ")
                  .append(Text.translatable("oneclickanvil.config.item.description"))))
            .binding("",
               entry::getItem,
               entry::setItem)
            .controller(StringControllerBuilder::create)
            .build()
         ).option(Option.<String>createBuilder().name(Text.translatable("oneclickanvil.config.rename.name"))
            .description(OptionDescription.of(
               Text.translatable("oneclickanvil.config.entry.name").append(" #" + index + " - ")
                  .append(Text.translatable("oneclickanvil.config.rename.description"))))
            .binding("",
               entry::getRename,
               entry::setRename)
            .controller(StringControllerBuilder::create)
            .build()
         ).option(
            Option.<Boolean>createBuilder().name(Text.translatable("oneclickanvil.config.enabled.name"))
               .description(OptionDescription.of(
                  Text.translatable("oneclickanvil.config.entry.name").append(" #" + index + " - ")
                     .append(Text.translatable("oneclickanvil.config.enabled.description"))))
               .binding(true,
                  entry::isEnabled,
                  entry::setEnabled)
               .controller(BooleanControllerBuilder::create)
               .build()
         ).build();
   }
}
