package com.github.breadmoirai.oneclickanvil.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class OneClickAnvilModMenu implements ModMenuApi {

   @Override
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return OneClickAnvilConfigScreen::new;
   }

}
