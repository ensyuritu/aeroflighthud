package net.ensyuritu.aeroflighthud.mixin;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientSubLevel.class)
public interface ClientSubLevelAccessor {
    @Accessor("latestNetworkedVelocity")
    Vector3d aeroflighthud$getLatestNetworkedVelocity();
}
