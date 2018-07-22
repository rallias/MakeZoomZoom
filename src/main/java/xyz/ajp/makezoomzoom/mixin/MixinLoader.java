package xyz.ajp.makezoomzoom.mixin;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.mixin.transformer.Proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;

@Mixin(value = Loader.class, priority = 999)
public class MixinLoader {
    @Shadow private List<ModContainer> mods;
    @Shadow private ModClassLoader modClassLoader;

    /**
     * @reason Load all mods now and load mod support mixin configs. This can't be done later
     * since constructing mods loads classes from them.
     */
    @Inject(method = "loadMods", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/LoadController;transition(Lnet/minecraftforge/fml/common/LoaderState;Z)V", ordinal = 1), remap = false)
    private void mzzBeforeConstructingMods(List<String> injectedModContainers, CallbackInfo ci) {
        // Add all mods to class loader
        for (ModContainer mod : mods) {
            try {
                modClassLoader.addFile(mod.getSource());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        // Add and reload mixin configs
        Mixins.addConfiguration("mixins.mzz.charsetv1.json");
        mods.stream().filter(mod -> mod.getModId().equals("recipestages")).findFirst().ifPresent(modContainer -> {
            if ( modContainer.getVersion().equals("1.1.1")) {
                Mixins.addConfiguration("mixins.mzz.recipestages.v1.json");
            }
        });

        // TODO: Only do this stuff if the hash matches.

//        InputStream inputStream = modClassLoader.getResourceAsStream("pl/asie/charset/lib/material/ItemMaterialHeuristics.class");
//        byte[] inputStreamBytes;
//        try {
//            inputStreamBytes = IOUtils.toByteArray(inputStream);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (NullPointerException e) {
//            inputStreamBytes = new byte[0];
//        }
//        String hash = Hex.encodeHexString(Hashing.sha512().hashBytes(inputStreamBytes).asBytes());
//        System.out.println("Hash "+hash);

        Proxy mixinProxy = (Proxy) Launch.classLoader.getTransformers().stream().filter(transformer -> transformer instanceof Proxy).findFirst().get();
        try {
            Field transformerField = Proxy.class.getDeclaredField("transformer");
            transformerField.setAccessible(true);
            MixinTransformer transformer = (MixinTransformer) transformerField.get(mixinProxy);

            Method selectConfigsMethod = MixinTransformer.class.getDeclaredMethod("selectConfigs", MixinEnvironment.class);
            selectConfigsMethod.setAccessible(true);
            selectConfigsMethod.invoke(transformer, MixinEnvironment.getCurrentEnvironment());

            Method prepareConfigsMethod = MixinTransformer.class.getDeclaredMethod("prepareConfigs", MixinEnvironment.class);
            prepareConfigsMethod.setAccessible(true);
            prepareConfigsMethod.invoke(transformer, MixinEnvironment.getCurrentEnvironment());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
