package com.github.breadmoirai.oneclickanvil.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.text.Text;

public class OneClickAnvilModMenu implements ModMenuApi {

   @Override
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return parentScreen -> YetAnotherConfigLib.createBuilder()
         .title(Text.translatable("oneclickanvil.config.title"))
         .category(ConfigCategory.createBuilder()
            .name(Text.translatable("oneclickanvil.config.title"))
            .tooltip(Text.translatable("modmenu.summaryTranslation.one-click-anvil"))
            .group(OptionGroup.createBuilder()
               .name(Text.translatable("oneclickanvil.config.group.name"))
               .option(Option.<String>createBuilder()
                  .name(Text.translatable("oneclickanvil.config.item.name"))
                  .description(OptionDescription.of(Text.translatable("oneclickanvil.config.item.description")))
                  .binding("",
                     () -> OneClickAnvilConfig.getInstance().getItem(),
                     newVal -> OneClickAnvilConfig.getInstance().setItem(newVal))
                  .controller(StringControllerBuilder::create)
                  .build())
               .option(Option.<String>createBuilder().name(Text.translatable("oneclickanvil.config.rename.name"))
                  .description(OptionDescription.of(Text.translatable("oneclickanvil.config.rename.description")))
                  .binding("",
                     () -> OneClickAnvilConfig.getInstance().getRename(),
                     newVal -> OneClickAnvilConfig.getInstance().setRename(newVal))
                  .controller(StringControllerBuilder::create)
                  .build())
               .build())
            .build())
         .save(OneClickAnvilConfig::saveModConfig)
         .build()
         .generateScreen(parentScreen);
   }
}
