package net.ensyuritu.aeroflighthud;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.*;

import java.lang.Math;

@EventBusSubscriber (modid = AeroFlightHud.MODID, value = Dist.CLIENT)
public class HudRenderer {


    public enum CalibrationMode {
        NegativeZ,
        PositiveX,
        PositiveZ,
        NegativeX;

        CalibrationMode nextMode(){
            int nextIndex = (this.ordinal() + 1) % values().length;
            return values()[nextIndex];
        }
    }

    protected static CalibrationMode calibrationMode = CalibrationMode.NegativeZ;

    protected static boolean hudVisible = true;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event){
        if(!hudVisible) return;
        Minecraft mc = Minecraft.getInstance();
        final float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);

        Player player = mc.player;
        if(player == null) return;
        if(player.getVehicle() == null) return;

        SubLevel currentSubLevel;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;



        if (player instanceof EntityMovementExtension extension) {
            currentSubLevel = extension.sable$getTrackingSubLevel();

            if(currentSubLevel instanceof ClientSubLevel clientSubLevel){
                Pose3dc currentClientPose = clientSubLevel.renderPose(partialTick);
                Pose3d currentPose = clientSubLevel.logicalPose();
                var lastPose = currentSubLevel.lastPose();

                if (currentPose != null && lastPose != null) {
                    var clientPos = currentClientPose.position();
                    var pos = currentPose.position();
                    var lastPos = lastPose.position();

                    double velX = (pos.x() - lastPos.x()) * 20.0;
                    double velY = (pos.y() - lastPos.y()) * 20.0;
                    double velZ = (pos.z() - lastPos.z()) * 20.0;

                    Quaterniond quaternion = new Quaterniond(currentClientPose.orientation());

                    var nonCalibratedEulerAngles = new org.joml.Vector3d();
                    quaternion.getEulerAnglesYXZ(nonCalibratedEulerAngles);

//                    double vehiclePitch = Math.toDegrees(nonCalibratedEulerAngles.x);
//                    double vehicleYaw = Math.toDegrees(nonCalibratedEulerAngles.y);
//                    double vehicleRoll = Math.toDegrees(nonCalibratedEulerAngles.z);

                    switch(calibrationMode){
                        case NegativeZ:
                            break;
                        case PositiveX:
                            quaternion.rotateY(Math.toRadians(-90.0));
                            break;
                        case PositiveZ:
                            quaternion.rotateY(Math.toRadians(180.0));
                            break;
                        case NegativeX:
                            quaternion.rotateY(Math.toRadians(90.0));
                            break;
                    }
                    //prepare variables
                    var calibratedEulerAngles = new org.joml.Vector3d();
                    quaternion.getEulerAnglesYXZ(calibratedEulerAngles);

                    int guiWidth = graphics.guiWidth();
                    int guiHeight = graphics.guiHeight();
                    Vector2i guiCenter = new Vector2i(guiWidth / 2, guiHeight / 2);
                    double guiAspect = (double)guiWidth / guiHeight;
                    //int windowWidth = mc.getWindow().getWidth();
                    //int windowHeight = mc.getWindow().getHeight();
                    //Vector2i windowCenter = new Vector2i(windowWidth / 2, windowHeight / 2);
                    //double windowAspect = (double)windowWidth / windowHeight;
                    double vFov = mc.options.fov().get() * mc.player.getFieldOfViewModifier();
                    double hFovGui = Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(vFov) / 2) * guiAspect));
                    float rawPlayerYaw = player.getYRot();
                    float playerLocalYaw = rawPlayerYaw % 360;
                    if(playerLocalYaw <= 0) playerLocalYaw += 360;
                    if(playerLocalYaw > 180) playerLocalYaw -= 360;
                    float playerLocalPitch = -player.getXRot();


                    //Draw Flight HUD
                    int hudColor = 0xFF00FF00;

                    final Quaterniond vehicleRotation = new Quaterniond(currentClientPose.orientation());
                    final Quaterniond invertedVehicleRotation = new Quaterniond(vehicleRotation);
                    invertedVehicleRotation.invert();



                    //boresight
                    Vector3d vehicleHeadingVector = new Vector3d(0, 0, 0);
                    switch (calibrationMode) {
                        case NegativeZ -> vehicleHeadingVector.z = -1.0;
                        case PositiveX -> vehicleHeadingVector.x = 1.0;
                        case PositiveZ -> vehicleHeadingVector.z = 1.0;
                        case NegativeX -> vehicleHeadingVector.x = -1.0;
                    }
                    vehicleHeadingVector.rotateY(Math.toRadians(playerLocalYaw));
                    vehicleHeadingVector.rotateX(Math.toRadians(playerLocalPitch));
                    double rawHudBoresightPosX = vehicleHeadingVector.x / (vehicleHeadingVector.z * Math.tan(Math.toRadians(hFovGui) / 2));
                    double rawHudBoresightPosY = vehicleHeadingVector.y / (vehicleHeadingVector.z * Math.tan(Math.toRadians(vFov) / 2));

                    if((Math.abs(rawHudBoresightPosX) < 1.0) && (Math.abs(rawHudBoresightPosY) < 1.0) && vehicleHeadingVector.z > 0.01){
                        float hudBoresightScreenPosX = (float)(guiCenter.x - rawHudBoresightPosX * guiCenter.x);
                        float hudBoresightScreenPosY = (float)(guiCenter.y - rawHudBoresightPosY * guiCenter.y);

                        //draw
//                        graphics.hLine(hudBoresightScreenPosX -10, hudBoresightScreenPosX -5, hudBoresightScreenPosY, hudColor);
//                        graphics.hLine(hudBoresightScreenPosX +5, hudBoresightScreenPosX +10, hudBoresightScreenPosY, hudColor);
                        drawScalableLine(graphics,
                                hudBoresightScreenPosX - 5.0f, hudBoresightScreenPosY,
                                hudBoresightScreenPosX - 10.0f, hudBoresightScreenPosY,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudBoresightScreenPosX + 5.0f, hudBoresightScreenPosY,
                                hudBoresightScreenPosX + 10.0f, hudBoresightScreenPosY,
                                1.0f, hudColor
                        );

                        drawScalableLine(graphics,
                                hudBoresightScreenPosX + 5.0f, hudBoresightScreenPosY,
                                hudBoresightScreenPosX + 2.5f, hudBoresightScreenPosY + 5.0f,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudBoresightScreenPosX + 2.5f, hudBoresightScreenPosY + 5.0f,
                                hudBoresightScreenPosX, hudBoresightScreenPosY,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudBoresightScreenPosX - 5.0f, hudBoresightScreenPosY,
                                hudBoresightScreenPosX - 2.5f, hudBoresightScreenPosY + 5.0f,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudBoresightScreenPosX - 2.5f, hudBoresightScreenPosY + 5.0f,
                                hudBoresightScreenPosX, hudBoresightScreenPosY,
                                1.0f, hudColor
                        );
                    }

                    //flightpathmarker
                    Vector3d localVelocity = new Vector3d(velX, velY, velZ).rotate(invertedVehicleRotation);

                    Vector3d playerEyeVelocity = new Vector3d(localVelocity);
                    playerEyeVelocity.rotateY(Math.toRadians(playerLocalYaw));
                    playerEyeVelocity.rotateX(Math.toRadians(playerLocalPitch));

                    if(playerEyeVelocity.length() * 20 >= 0.1 && playerEyeVelocity.z > 0.01){
                        //calculate drawpoint
                        double rawHudVelocityVectorPosX = playerEyeVelocity.x / (playerEyeVelocity.z * Math.tan(Math.toRadians(hFovGui) / 2));
                        double rawHudVelocityVectorPosY = playerEyeVelocity.y / (playerEyeVelocity.z * Math.tan(Math.toRadians(vFov) / 2));
                        float hudVelocityVectorPosX = (float)(guiCenter.x - rawHudVelocityVectorPosX * guiCenter.x);
                        float hudVelocityVectorPosY = (float)(guiCenter.y - rawHudVelocityVectorPosY * guiCenter.y);

                        //draw
                        //square
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX -2.0f, hudVelocityVectorPosY + 2.0f,
                                hudVelocityVectorPosX +2.0f, hudVelocityVectorPosY + 2.0f,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX -2.0f, hudVelocityVectorPosY - 2.0f,
                                hudVelocityVectorPosX +2.0f, hudVelocityVectorPosY - 2.0f,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX +2.0f, hudVelocityVectorPosY - 2.0f,
                                hudVelocityVectorPosX +2.0f, hudVelocityVectorPosY + 2.0f,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX -2.0f, hudVelocityVectorPosY - 2.0f,
                                hudVelocityVectorPosX -2.0f, hudVelocityVectorPosY + 2.0f,
                                1.0f, hudColor

                        );
                        //three things
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX , hudVelocityVectorPosY - 2.0f,
                                hudVelocityVectorPosX , hudVelocityVectorPosY - 5.0f,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX -2.0f, hudVelocityVectorPosY,
                                hudVelocityVectorPosX -5.0f, hudVelocityVectorPosY,
                                1.0f, hudColor
                        );
                        drawScalableLine(graphics,
                                hudVelocityVectorPosX +2.0f, hudVelocityVectorPosY,
                                hudVelocityVectorPosX +5.0f, hudVelocityVectorPosY,
                                1.0f, hudColor
                        );

                    }

                    //AltMeter
                    int altMeterDrawPosX = guiCenter.x + 150;
                    int altMeterDrawHeight = 200;
                    int altMeterMarkGap = 10;
                    int altDiffPerMark = 4;
                    int altDisplayPerMarks = 5;
                    double altDiffPerPixel = (double)altMeterMarkGap / (double)altDiffPerMark;
                    int altDisplayDiff = altDiffPerMark * altDisplayPerMarks;

                    //AltMeterDraw

                    graphics.vLine(altMeterDrawPosX, guiCenter.y - (altMeterDrawHeight / 2), guiCenter.y + (altMeterDrawHeight / 2), hudColor);
                    graphics.drawString(font, String.format("%.0f", clientPos.y()), altMeterDrawPosX + 8, guiCenter.y - font.lineHeight / 2, hudColor, false);

                    double bottomAltValue = clientPos.y() - (((double)altMeterDrawHeight / 2) / altDiffPerPixel);
                    int drawAltMarkPosY = guiCenter.y + (altMeterDrawHeight / 2) + (int)Math.floor(mathMod(bottomAltValue * altDiffPerPixel, altMeterMarkGap));
                    int drawAltValue = (int)Math.floor(Math.floor(bottomAltValue) / altDiffPerMark ) * altDiffPerMark;

                    while(true){
                        drawAltValue += altDiffPerMark;
                        drawAltMarkPosY -= altMeterMarkGap;

                        if(!(drawAltMarkPosY > guiCenter.y - (altMeterDrawHeight / 2))){
                            break;
                        }


                        graphics.hLine(altMeterDrawPosX, altMeterDrawPosX + 5, drawAltMarkPosY, hudColor);
                        if(guiCenter.y + 8 > drawAltMarkPosY && drawAltMarkPosY > guiCenter.y - 8){
                            continue;
                        }
                        if(drawAltValue % altDisplayDiff == 0){
                            graphics.drawString(font, String.format("%d", drawAltValue), altMeterDrawPosX + 8, drawAltMarkPosY - font.lineHeight / 2, hudColor, false);
                        }
                    }

                    //AirSpeedMeter
                    int speedMeterDrawPosX = guiCenter.x - 150;
                    int speedMeterDrawHeight = 200;
                    int speedMeterMarkGap = 10;
                    int speedDiffPerMark = 2;
                    int speedDisplayPerMarks = 5;
                    double speedDiffPerPixel = (double)speedMeterMarkGap / (double)speedDiffPerMark;
                    int speedDisplayDiff = speedDiffPerMark * speedDisplayPerMarks;

                    double speedMeterValue = switch (calibrationMode) {
                        case NegativeZ -> (-localVelocity.z());
                        case PositiveX -> (localVelocity.x());
                        case PositiveZ -> (localVelocity.z());
                        case NegativeX -> (-localVelocity.x());
                    };

                    String speedMeterValueString = String.format("%.1f", speedMeterValue);

                    //AirSpeedMeterDraw
                    graphics.vLine(speedMeterDrawPosX, guiCenter.y - (speedMeterDrawHeight / 2), guiCenter.y + (speedMeterDrawHeight / 2), hudColor);
                    graphics.drawString(font, speedMeterValueString, speedMeterDrawPosX - 7 - font.width(speedMeterValueString), guiCenter.y - font.lineHeight / 2, hudColor, false);

                    double bottomSpeedValue = speedMeterValue - (((double)speedMeterDrawHeight / 2) / speedDiffPerPixel);
                    int drawSpeedMarkPosY = guiCenter.y + (speedMeterDrawHeight / 2) + (int)Math.floor(mathMod(bottomSpeedValue * speedDiffPerPixel, speedMeterMarkGap));
                    int drawSpeedValue = (int)Math.floor(Math.floor(bottomSpeedValue) / speedDiffPerMark ) * speedDiffPerMark;

                    while(true){
                        drawSpeedValue += speedDiffPerMark;
                        drawSpeedMarkPosY -= speedMeterMarkGap;

                        if(!(drawSpeedMarkPosY > guiCenter.y - (speedMeterDrawHeight / 2))){
                            break;
                        }

                        if(drawSpeedValue < 0) continue;


                        graphics.hLine(speedMeterDrawPosX, speedMeterDrawPosX - 5, drawSpeedMarkPosY, hudColor);
                        if(guiCenter.y + 8 > drawSpeedMarkPosY && drawSpeedMarkPosY > guiCenter.y - 8){
                            continue;
                        }

                        if(drawSpeedValue % speedDisplayDiff == 0){
                            String speedText = String.format("%.1f", (double)drawSpeedValue);
                            graphics.drawString(font, speedText, speedMeterDrawPosX - 7 - font.width(speedText), drawSpeedMarkPosY - font.lineHeight / 2, hudColor, false);
                        }
                    }

                    //PitchLadder
                    final int ladderStepDegrees = 15; //Must choose from one of the divisors of 90
                    final int ladderSteps = 90 / ladderStepDegrees;


                    Quaterniond playerLocalRotation = new Quaterniond()
                            .rotationY(Math.toRadians(-playerLocalYaw))
                            .rotateX(Math.toRadians(-playerLocalPitch));
                    Quaterniond playerGlobalRotation = new Quaterniond(vehicleRotation);
                    playerGlobalRotation.mul(playerLocalRotation);
                    Vector3d playerGlobalHeading = new Vector3d(0, 0, 1);
                    playerGlobalRotation.transform(playerGlobalHeading);


                    final double playerGlobalHeadingYawRad = Math.atan2(playerGlobalHeading.x, playerGlobalHeading.z);


                    Vector3d headingPitchZero = new Vector3d(0, 0, 1);
                    headingPitchZero.rotateY(playerGlobalHeadingYawRad);
                    headingPitchZero.rotate(invertedVehicleRotation);
                    headingPitchZero.rotateY(Math.toRadians(playerLocalYaw));
                    headingPitchZero.rotateX(Math.toRadians(playerLocalPitch));



                    Vector3d[] headingPitchUp = new Vector3d[ladderSteps];
                    Vector3d[] headingPitchDown = new Vector3d[ladderSteps];
                    for(int i=0; i<ladderSteps; i++){
                        headingPitchUp[i] = new Vector3d(0, 0, 1);
                        headingPitchUp[i].rotateX(-Math.toRadians((i + 1) * ladderStepDegrees));
                        headingPitchUp[i].rotateY(playerGlobalHeadingYawRad);
                        headingPitchUp[i].rotate(invertedVehicleRotation);
                        headingPitchUp[i].rotateY(Math.toRadians(playerLocalYaw));
                        headingPitchUp[i].rotateX(Math.toRadians(playerLocalPitch));

                        headingPitchDown[i] = new Vector3d(0, 0, 1);
                        headingPitchDown[i].rotateX(Math.toRadians((i + 1) * ladderStepDegrees));
                        headingPitchDown[i].rotateY(playerGlobalHeadingYawRad);
                        headingPitchDown[i].rotate(invertedVehicleRotation);
                        headingPitchDown[i].rotateY(Math.toRadians(playerLocalYaw));
                        headingPitchDown[i].rotateX(Math.toRadians(playerLocalPitch));


                    }

                    //PitchLadder Draw

                    Vector3d playerGlobalRotationEulerAngles = new Vector3d();
                    playerGlobalRotation.getEulerAnglesYXZ(playerGlobalRotationEulerAngles);
                    float playerEyeRollRad = (float) -playerGlobalRotationEulerAngles.z;

                    //PitchZero
                    double rawHudHeadingPitchZeroPosX = headingPitchZero.x / (headingPitchZero.z * Math.tan(Math.toRadians(hFovGui) / 2));
                    double rawHudHeadingPitchZeroPosY = headingPitchZero.y / (headingPitchZero.z * Math.tan(Math.toRadians(vFov) / 2));

                    if(Math.abs(rawHudHeadingPitchZeroPosX) <= 1.5 && Math.abs(rawHudHeadingPitchZeroPosY) <= 1.5 && headingPitchZero.z > 0.01){
                        float hudHeadingPitchZeroPosX = (float)(guiCenter.x - rawHudHeadingPitchZeroPosX * guiCenter.x);
                        float hudHeadingPitchZeroPosY = (float)(guiCenter.y - rawHudHeadingPitchZeroPosY * guiCenter.y);

                        //graphics.fill(hudHeadingPitchZeroPosX -1, hudHeadingPitchZeroPosY -1, hudHeadingPitchZeroPosX +1, hudHeadingPitchZeroPosY +1, hudColorPitchZero);
                        drawScalableLine(graphics,
                                hudHeadingPitchZeroPosX - (float) Math.cos(playerEyeRollRad) * 100.0f,
                                hudHeadingPitchZeroPosY - (float) Math.sin(playerEyeRollRad) * 100.0f,
                                hudHeadingPitchZeroPosX - (float) Math.cos(playerEyeRollRad) * 15.0f,
                                hudHeadingPitchZeroPosY - (float) Math.sin(playerEyeRollRad) * 15.0f,
                                1.0f, hudColor);
                        drawScalableLine(graphics,
                                hudHeadingPitchZeroPosX + (float) Math.cos(playerEyeRollRad) * 100.0f,
                                hudHeadingPitchZeroPosY + (float) Math.sin(playerEyeRollRad) * 100.0f,
                                hudHeadingPitchZeroPosX + (float) Math.cos(playerEyeRollRad) * 15.0f,
                                hudHeadingPitchZeroPosY + (float) Math.sin(playerEyeRollRad) * 15.0f,
                                1.0f, hudColor);
                        drawFont(graphics,
                                "0",
                                hudHeadingPitchZeroPosX - (float) Math.cos(playerEyeRollRad) * 120.0f,
                                hudHeadingPitchZeroPosY - (float) Math.sin(playerEyeRollRad) * 120.0f,
                                (float) Math.toDegrees(playerEyeRollRad),
                                hudColor,
                                false);
                        drawFont(graphics,
                                "0",
                                hudHeadingPitchZeroPosX + (float) Math.cos(playerEyeRollRad) * 120.0f,
                                hudHeadingPitchZeroPosY + (float) Math.sin(playerEyeRollRad) * 120.0f,
                                (float) Math.toDegrees(playerEyeRollRad),
                                hudColor,
                                false);
                    }



                    //PitchUp
                    int displayDeg = 0;
                    for(Vector3d heading : headingPitchUp){
                        double rawHudHeadingPosX = heading.x / (heading.z * Math.tan(Math.toRadians(hFovGui) / 2));
                        double rawHudHeadingPosY = heading.y / (heading.z * Math.tan(Math.toRadians(vFov) / 2));

                        displayDeg += ladderStepDegrees;
                        if(Math.abs(rawHudHeadingPosX) <= 1.5 && Math.abs(rawHudHeadingPosY) <= 1.5 && heading.z > 0.01){
                            float hudHeadingX = (float)(guiCenter.x - rawHudHeadingPosX * guiCenter.x);
                            float hudHeadingY = (float)(guiCenter.y - rawHudHeadingPosY * guiCenter.y);

                            //horizonLine
                            drawScalableLine(graphics,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 15.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 15.0f,
                                    1.0f, hudColor);
                            drawScalableLine(graphics,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 15.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 15.0f,
                                    1.0f, hudColor);
                            //vertialLine
                            drawScalableLine(graphics,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 60.0f + (float) Math.sin(-playerEyeRollRad) * 5.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 60.0f + (float) Math.cos(-playerEyeRollRad) * 5.0f,
                                    1.0f, hudColor);
                            drawScalableLine(graphics,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 60.0f + (float) Math.sin(-playerEyeRollRad) * 5.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 60.0f + (float) Math.cos(-playerEyeRollRad) * 5.0f,
                                    1.0f, hudColor);
                            drawFont(graphics,
                                    String.valueOf(displayDeg),
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 80.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 80.0f,
                                    (float) Math.toDegrees(playerEyeRollRad),
                                    hudColor,
                                    false);
                            drawFont(graphics,
                                    String.valueOf(displayDeg),
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 80.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 80.0f,
                                    (float) Math.toDegrees(playerEyeRollRad),
                                    hudColor,
                                    false);
                        }
                    }

                    //PitchDown
                    displayDeg = 0;
                    for(Vector3d heading : headingPitchDown){
                        double rawHudHeadingPosX = heading.x / (heading.z * Math.tan(Math.toRadians(hFovGui) / 2));
                        double rawHudHeadingPosY = heading.y / (heading.z * Math.tan(Math.toRadians(vFov) / 2));

                        displayDeg -= ladderStepDegrees;
                        if(Math.abs(rawHudHeadingPosX) <= 1.5 && Math.abs(rawHudHeadingPosY) <= 1.5 && heading.z > 0.01){
                            float hudHeadingX = (float)(guiCenter.x - rawHudHeadingPosX * guiCenter.x);
                            float hudHeadingY = (float)(guiCenter.y - rawHudHeadingPosY * guiCenter.y);

                            //hirozonLine
                            drawScalableLine(graphics,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 25.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 25.0f,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 15.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 15.0f,
                                    1.0f, hudColor);
                            drawScalableLine(graphics,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 25.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 25.0f,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 15.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 15.0f,
                                    1.0f, hudColor);

                            drawScalableLine(graphics,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 45.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 45.0f,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 35.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 35.0f,
                                    1.0f, hudColor);
                            drawScalableLine(graphics,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 45.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 45.0f,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 35.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 35.0f,
                                    1.0f, hudColor);

                            drawScalableLine(graphics,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 55.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 55.0f,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    1.0f, hudColor);
                            drawScalableLine(graphics,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 55.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 55.0f,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    1.0f, hudColor);
                            //verticalLine
                            drawScalableLine(graphics,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 60.0f - (float) Math.sin(-playerEyeRollRad) * 5.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 60.0f - (float) Math.cos(-playerEyeRollRad) * 5.0f,
                                    1.0f, hudColor);
                            drawScalableLine(graphics,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 60.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 60.0f,
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 60.0f - (float) Math.sin(-playerEyeRollRad) * 5.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 60.0f - (float) Math.cos(-playerEyeRollRad) * 5.0f,
                                    1.0f, hudColor);
                            drawFont(graphics,
                                    String.valueOf(displayDeg),
                                    hudHeadingX - (float) Math.cos(playerEyeRollRad) * 80.0f,
                                    hudHeadingY - (float) Math.sin(playerEyeRollRad) * 80.0f,
                                    (float) Math.toDegrees(playerEyeRollRad),
                                    hudColor,
                                    false);
                            drawFont(graphics,
                                    String.valueOf(displayDeg),
                                    hudHeadingX + (float) Math.cos(playerEyeRollRad) * 80.0f,
                                    hudHeadingY + (float) Math.sin(playerEyeRollRad) * 80.0f,
                                    (float) Math.toDegrees(playerEyeRollRad),
                                    hudColor,
                                    false);
                        }
                    }
                }
            }
        }
    }

    public static CalibrationMode changeCalibrationMode(){
        calibrationMode = calibrationMode.nextMode();
        return calibrationMode;
    }

    public static boolean ToggleHudVisible(){
        hudVisible = !hudVisible;
        return hudVisible;
    }

    private static void drawScalableLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float thickness, int color){

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);

        if(len == 0) return;

        float nx = -dy / len;
        float ny = dx / len;

        float halfWidth = thickness / 2.0f;
        float offsetX = nx * halfWidth;
        float offsetY = ny * halfWidth;
        float adjustX = dx / len * halfWidth;
        float adjustY = dy / len * halfWidth;

        x1 -= adjustX;
        y1 -= adjustY;
        x2 += adjustX;
        y2 += adjustY;

        float p1x = x1 - offsetX;
        float p1y = y1 - offsetY;
        float p2x = x1 + offsetX;
        float p2y = y1 + offsetY;
        float p3x = x2 + offsetX;
        float p3y = y2 + offsetY;
        float p4x = x2 - offsetX;
        float p4y = y2 - offsetY;

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());

        vertexConsumer.addVertex(matrix, p1x, p1y, 0.0f).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, p2x, p2y, 0.0f).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, p3x, p3y, 0.0f).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, p4x, p4y, 0.0f).setColor(r, g, b, a);

        graphics.flush();
    }

    private static void drawFont(GuiGraphics graphics, String text, float x, float y, float degrees, int color, boolean dropShadow){
        Font font = Minecraft.getInstance().font;

        graphics.pose().pushPose();

        graphics.pose().translate(x, y, 0.0f);

        float radians = (float) Math.toRadians(degrees);

        graphics.pose().mulPose((new Quaternionf()).rotationZ(radians));

        graphics.drawString(font, text, - font.width(text) / 2, - font.lineHeight / 2, color, dropShadow);

        graphics.pose().popPose();
    }

    private static double mathMod(double a, double b){
        return a - b * Math.floor(a / b);
    }
}