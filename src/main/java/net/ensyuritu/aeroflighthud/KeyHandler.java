package net.ensyuritu.aeroflighthud;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = AeroFlightHud.MODID, value = Dist.CLIENT)
public class KeyHandler {

    //private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        while(KeyMappingRegister.CALIBRATION_KEY.consumeClick()){
            //LOGGER.debug("calibration key pressed");
            HudRenderer.CalibrationMode changedModeTo = HudRenderer.changeCalibrationMode();
            Minecraft mc = Minecraft.getInstance();
            switch(changedModeTo){
                case HudRenderer.CalibrationMode.NegativeX:
                    mc.gui.setOverlayMessage(Component.translatable("text.aeroflighthud.calibrate_negative_x"), false);
                    break;
                case HudRenderer.CalibrationMode.NegativeZ:
                    mc.gui.setOverlayMessage(Component.translatable("text.aeroflighthud.calibrate_negative_z"), false);
                    break;
                case HudRenderer.CalibrationMode.PositiveX:
                    mc.gui.setOverlayMessage(Component.translatable("text.aeroflighthud.calibrate_positive_x"), false);
                    break;
                case HudRenderer.CalibrationMode.PositiveZ:
                    mc.gui.setOverlayMessage(Component.translatable("text.aeroflighthud.calibrate_positive_z"), false);
                    break;
            }
        }

        while(KeyMappingRegister.HUD_TOGGLE_KEY.consumeClick()){
            boolean toggledToTrue = HudRenderer.ToggleHudVisible();
            Minecraft mc = Minecraft.getInstance();

            if(toggledToTrue){
                mc.gui.setOverlayMessage(Component.translatable("text.aeroflighthud.hud_visible"), false);
            }else{
                mc.gui.setOverlayMessage(Component.translatable("text.aeroflighthud.hud_hidden"), false);
            }
        }
    }
}
