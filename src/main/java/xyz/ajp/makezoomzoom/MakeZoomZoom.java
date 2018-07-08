package xyz.ajp.makezoomzoom;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import xyz.ajp.makezoomzoom.test.ProxyMethodTest;

@Mod(modid="makezoomzoom", name="Make Zoom Zoom", version="0.0.2")
public class MakeZoomZoom {
    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        ProxyMethodTest.doNothing();
        System.out.println("MakeZoomZoom loading");
    }
}
