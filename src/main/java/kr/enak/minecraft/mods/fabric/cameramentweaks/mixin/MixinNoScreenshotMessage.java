package kr.enak.minecraft.mods.fabric.cameramentweaks.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public abstract class MixinNoScreenshotMessage {
    @Shadow
    public static NativeImage takeScreenshot(Framebuffer framebuffer) {
        return null;
    }

    @Shadow
    protected static File getScreenshotFilename(File directory) {
        return null;
    }

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "saveScreenshotInner", at = @At("HEAD"), cancellable = true)
    private static void saveScreenshotInner(File gameDirectory, String fileName, Framebuffer framebuffer, Consumer<Text> messageReceiver, CallbackInfo ci) {
        NativeImage nativeImage = takeScreenshot(framebuffer);
        File file = new File(gameDirectory, "screenshots");
        file.mkdir();
        File file2;
        if (fileName == null) {
            file2 = getScreenshotFilename(file);
        } else {
            file2 = new File(file, fileName);
        }

        Util.getIoWorkerExecutor().execute(() -> {
            try {
                nativeImage.writeTo(file2);
                Text text = Text.literal(file2.getName()).formatted(Formatting.UNDERLINE).styled((style) -> {
                    return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file2.getAbsolutePath()));
                });
                text = Text.translatable("screenshot.success", new Object[]{text});

                LOGGER.info(text.getString());
                MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.5f, 1.0f);
            } catch (Exception var7) {
                Exception exception = var7;
                LOGGER.warn("Couldn't save screenshot", exception);
                messageReceiver.accept(Text.translatable("screenshot.failure", new Object[]{exception.getMessage()}));
            } finally {
                nativeImage.close();
            }
        });
        ci.cancel();
    }
}
