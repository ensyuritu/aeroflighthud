package net.ensyuritu.aerohud;


import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AeroFlightHud.MODID, value = Dist.CLIENT)
public class KeyMappingRegister {

    public static final KeyMapping CALIBRATION_KEY = new KeyMapping(
                "key.aeroflighthud.calibration",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.aeroflighthud.aeroflighthud"
    );

    public static final KeyMapping HUD_TOGGLE_KEY = new KeyMapping(
            "key.aeroflighthud.toggle_hud_visible",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.aeroflighthud.aeroflighthud"
    );

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event){
        event.register(CALIBRATION_KEY);
        event.register(HUD_TOGGLE_KEY);
    }
}
