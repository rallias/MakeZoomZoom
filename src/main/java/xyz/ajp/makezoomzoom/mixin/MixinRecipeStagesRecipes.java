package xyz.ajp.makezoomzoom.mixin;

import com.blamejared.recipestages.handlers.Recipes;
import com.blamejared.recipestages.recipes.RecipeStage;
import crafttweaker.IAction;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.mc1120.recipes.MCRecipeBase;
import crafttweaker.mc1120.recipes.MCRecipeManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.joor.Reflect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xyz.ajp.makezoomzoom.asmutil.MZZThreadFactory;

import java.util.*;
import java.util.concurrent.*;

@Mixin(Recipes.class)
public class MixinRecipeStagesRecipes {
    @Mixin(targets = "com.blamejared.recipestages.handlers.Recipes$ActionSetOutputStages")
    private static abstract class ActionSetOutputStages implements IAction {

        @Final
        @Shadow private final Map<String, List<IItemStack>> outputs = new HashMap<>();

        @Overwrite(remap=false)
        public void apply() {
            Executor executor = Executors.newFixedThreadPool(64, new MZZThreadFactory("RecipeStages Apply Output Stage"));
            Queue<Future<Object>> futuresList = new ArrayDeque<>(128);
            List<IRecipe> values = new ArrayList<>(ForgeRegistries.RECIPES.getValues());
            for(IRecipe recipe : values) {
                futuresList.add(((ExecutorService) executor).submit(() -> applyHandler(recipe)));
                while(futuresList.size() > 64 ) {
                    try {
                        futuresList.remove().get();
                    } catch (InterruptedException | ExecutionException e) {
                        // We really don't care, but if something DOES come up...
                        e.printStackTrace();
                    }
                }
            }

            while(!futuresList.isEmpty()) {
                try {
                    futuresList.remove().get();
                } catch (InterruptedException | ExecutionException e) {
                    // We really don't care, but if something DOES come up...
                    e.printStackTrace();
                }
            }

            // Me shakes fist.
            // Can't call a private class, can't call a mixin class, can't shadow a class.
            Reflect.on("com.blamejared.recipestages.handlers.Recipes").set("actionSetOutputStages", null);
            ((ExecutorService) executor).shutdown();
        }

        private Object applyHandler(IRecipe recipe) {
            IItemStack stack = CraftTweakerMC.getIItemStack(recipe.getRecipeOutput());
            if(stack != null) {
                for(Map.Entry<String, List<IItemStack>> entry : outputs.entrySet()) {
                    for(IItemStack output : entry.getValue()) {
                        if(output.matches(stack)) {
                            replaceRecipe(entry.getKey(), recipe);
                            break;
                        }
                    }
                }
            }
            return null;
        }

        private static void replaceRecipe(String stage, IRecipe iRecipe) {
            ResourceLocation registryName = iRecipe.getRegistryName();
            if(registryName == null)
                return;

            int width = 0, height = 0;
            if(iRecipe instanceof IShapedRecipe) {
                width = ((IShapedRecipe) iRecipe).getRecipeWidth();
                height = ((IShapedRecipe) iRecipe).getRecipeHeight();
            }

            boolean shapeless = (width == 0 && height == 0);
            IRecipe recipe = new RecipeStage(stage, iRecipe, shapeless, width, height);
            setRecipeRegistryName(recipe, registryName);
            ForgeRegistries.RECIPES.register(recipe);

            //List<IRecipe> list = recipes.getOrDefault(stage, new LinkedList<>());
            Map<String, List<IRecipe>> recipes = Reflect.on(Recipes.class).field("recipes").get();
            List<IRecipe> list = recipes.getOrDefault(stage, new LinkedList<>());
            list.add(recipe);
            recipes.put(stage, list);

            if(iRecipe instanceof MCRecipeBase) {
                MCRecipeManager.recipesToAdd.removeIf(baseAddRecipe -> baseAddRecipe.getRecipe() == iRecipe);
            }
        }

        private static void setRecipeRegistryName(IRecipe recipe, ResourceLocation registryName) {
            Loader loader = Loader.instance();
            ModContainer activeModContainer = loader.activeModContainer();
            ModContainer modContainer = loader.getIndexedModList().get(registryName.getResourceDomain());
            if(modContainer != null) {
                loader.setActiveModContainer(modContainer);
            }
            recipe.setRegistryName(registryName);
            loader.setActiveModContainer(activeModContainer);
        }
    }
}
