package xyz.ajp.makezoomzoom.mixin;

import com.google.common.base.Joiner;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.oredict.OreDictionary;
import org.joor.Reflect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import pl.asie.charset.ModCharset;
import pl.asie.charset.lib.CharsetLib;
import pl.asie.charset.lib.material.FastRecipeLookup;
import pl.asie.charset.lib.material.ItemMaterial;
import pl.asie.charset.lib.material.ItemMaterialHeuristics;
import pl.asie.charset.lib.material.ItemMaterialRegistry;
import xyz.ajp.makezoomzoom.asmutil.HelperMixinResultingBlockItemMaterialHeuristics;
import xyz.ajp.makezoomzoom.asmutil.MZZThreadFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Mixin(ItemMaterialHeuristics.class)
public abstract class MixinItemMaterialHeuristics {
    @Shadow private static int initPhase;
    @Shadow private static ItemMaterialRegistry reg;
    @Shadow private static void supplyExpandedStacks(Collection<ItemStack> stacks, Consumer<ItemStack> stackConsumer){}
    @Shadow private static void initLogMaterial(ItemStack log) {}
    @Shadow private static void initIngotLikeMaterial(String oreName, ItemStack stack) {}
    @Shadow private static void initStoneMaterial(String oreName, ItemStack stack) {}
    @Shadow private static void initCobblestoneMaterial(String oreName, ItemStack stack) {}
    @Shadow private static void initOreMaterial(String oreName) {}
    @Shadow private static boolean isBlock(ItemStack stack) {return false;}
    @Shadow private static void addResultingBlock(ItemMaterial base, ItemStack result, String source, String target) {}

    @Overwrite
    public static void init(boolean modded) {
        Executor executor = Executors.newFixedThreadPool(64, new MZZThreadFactory("MZZ - Charset Item Material Heuristics"));
        FastRecipeLookup.clearRecipeLists();
        if(modded) {
            FastRecipeLookup.initRecipeLists();
        }

        long time = System.currentTimeMillis();
        if ( initPhase >= (modded ? 2 : 1) ) {
            return;
        }

        ProgressManager.ProgressBar bar = ProgressManager.push("Material scanning", 6);
        reg = ItemMaterialRegistry.INSTANCE;

        bar.step("Wood");
        if ( !modded ) {
            ItemMaterial stick = reg.getOrCreateMaterial(new ItemStack(Items.STICK));
            reg.registerTypes(stick, "stick", "item", "wood");

            for (int i = 0; i < 6; i++) {
                ItemMaterial log = reg.getOrCreateMaterial(new ItemStack(i >= 4 ? Blocks.LOG2 : Blocks.LOG, 1, i % 4));
                ItemMaterial plank = reg.getOrCreateMaterial(new ItemStack(Blocks.PLANKS, 1, i));
                reg.registerTypes(log, "log", "block", "wood");
                reg.registerTypes(plank, "plank", "block", "wood");
                reg.registerRelation(log, plank, "plank", "log");
                if (i == 0) {
                    reg.registerRelation(plank, stick, "stick", "plank");
                    reg.registerRelation(log, stick, "stick", "log");
                } else {
                    reg.registerRelation(plank, stick, "stick");
                    reg.registerRelation(log, stick, "stick");
                }
            }
        } else {
            supplyExpandedStacks(OreDictionary.getOres("logWood", false), MixinItemMaterialHeuristics::initLogMaterial);
        }

        bar.step("Ores");

        if (modded) {
            for (String oreName : OreDictionary.getOreNames()) {
                if (oreName.startsWith("ore")) {
                    initOreMaterial(oreName);
                }
            }
        }

        bar.step("Ingots/Dusts/Gems");

        if (modded) {
            for (String oreName : OreDictionary.getOreNames()) {
                if (oreName.startsWith("ingot") || oreName.startsWith("dust") || oreName.startsWith("gem")) {
                    supplyExpandedStacks(OreDictionary.getOres(oreName, false), (s -> MixinItemMaterialHeuristics.initIngotLikeMaterial(oreName, s)));
                }
            }
        }

        bar.step("Stones");

        if (modded) {
            for (String oreName : OreDictionary.getOreNames()) {
                if (oreName.startsWith("stone")) {
                    supplyExpandedStacks(OreDictionary.getOres(oreName, false), (s -> MixinItemMaterialHeuristics.initStoneMaterial(oreName, s)));
                }
            }

            for (String oreName : OreDictionary.getOreNames()) {
                if (oreName.startsWith("cobblestone")) {
                    supplyExpandedStacks(OreDictionary.getOres(oreName, false), (s -> MixinItemMaterialHeuristics.initCobblestoneMaterial(oreName, s)));
                }
            }
        }

        bar.step("Misc");
        if (modded)
            reg.registerTypes(reg.getOrCreateMaterial(new ItemStack(Blocks.BEDROCK)), "block", "bedrock");

        bar.step("Slabs/Stairs");

        Queue<Future<HelperMixinResultingBlockItemMaterialHeuristics>> resultingBlockFutures = new ArrayDeque<>(128);

        if (modded) {
            for (ItemMaterial material : reg.getMaterialsByType("block")) {
                resultingBlockFutures.add(((ExecutorService) executor).submit(() -> findSlabRB(material)));
                resultingBlockFutures.add(((ExecutorService) executor).submit(() -> findStairRB(material)));
                while(resultingBlockFutures.size() > 64 ) {
                    try {
                        HelperMixinResultingBlockItemMaterialHeuristics resultingBlock = resultingBlockFutures.remove().get();
                        if ( resultingBlock != null ) {
                            addResultingBlock(resultingBlock.base, resultingBlock.result, resultingBlock.source, resultingBlock.target);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        while(!resultingBlockFutures.isEmpty() ) {
            try {
                HelperMixinResultingBlockItemMaterialHeuristics resultingBlock = resultingBlockFutures.remove().get();
                if ( resultingBlock != null ) {
                    addResultingBlock(resultingBlock.base, resultingBlock.result, resultingBlock.source, resultingBlock.target);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        ProgressManager.pop(bar);

        time = System.currentTimeMillis() - time;
        ModCharset.logger.info("Charset material heuristics time (phase " + initPhase + "): " + time + "ms");

        if (CharsetLib.enableDebugInfo && initPhase == 2) {
            try {
                File outputFile = new File("charsetItemMaterials.txt");
                PrintWriter writer = new PrintWriter(outputFile);
                Joiner commaJoiner = Joiner.on(",");

                for (ItemMaterial material : reg.getAllMaterials()) {
                    writer.println(material.getId());
                    writer.println("- Types: " + commaJoiner.join(material.getTypes()));
                    for (Map.Entry<String, ItemMaterial> entry : material.getRelations().entrySet()) {
                        writer.println("- Relation: " + entry.getKey() + " -> " + entry.getValue().getId());
                    }
                }

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static HelperMixinResultingBlockItemMaterialHeuristics findSlabRB(ItemMaterial base) {
        if ( !base.getTypes().contains("block") || base.getRelated("slab") != null )
            return null;

        ItemStack slab = Reflect.on(FastRecipeLookup.class).call("getCraftingResultQuickly", true, 3, null, 3, 1, new ItemStack[] {base.getStack(), base.getStack(), base.getStack()}).get();

        if ( isBlock(slab) ) {
            return new HelperMixinResultingBlockItemMaterialHeuristics(base, slab, "block", "slab");
        }
        return null;
    }

    private static HelperMixinResultingBlockItemMaterialHeuristics findStairRB(ItemMaterial base) {
        if (!base.getTypes().contains("block") || base.getRelated("stairs") != null)
            return null;

        ItemStack stair = Reflect.on(FastRecipeLookup.class).call("getCraftingResultQuickly", true, 6, null, 3, 3, new ItemStack[] {base.getStack(), null, null, base.getStack(), base.getStack(), null, base.getStack(), base.getStack(), base.getStack()}).get();

        if (isBlock(stair)) {
            return new HelperMixinResultingBlockItemMaterialHeuristics(base, stair, "block", "stairs");
        }
        return null;
    }
}
