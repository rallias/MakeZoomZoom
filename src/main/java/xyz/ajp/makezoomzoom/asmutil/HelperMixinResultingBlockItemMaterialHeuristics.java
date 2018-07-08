package xyz.ajp.makezoomzoom.asmutil;

import net.minecraft.item.ItemStack;
import pl.asie.charset.lib.material.ItemMaterial;

public class HelperMixinResultingBlockItemMaterialHeuristics {
    public ItemMaterial base;
    public ItemStack result;
    public String source;
    public String target;

    public HelperMixinResultingBlockItemMaterialHeuristics(ItemMaterial base, ItemStack result, String source, String target) {
        this.base = base;
        this.result = result;
        this.source = source;
        this.target = target;
    }
}
