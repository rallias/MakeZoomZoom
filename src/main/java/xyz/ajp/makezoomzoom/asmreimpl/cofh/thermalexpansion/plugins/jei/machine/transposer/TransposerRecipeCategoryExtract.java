package xyz.ajp.makezoomzoom.asmreimpl.cofh.thermalexpansion.plugins.jei.machine.transposer;

import cofh.thermalexpansion.plugins.jei.RecipeUidsTE;
import cofh.thermalexpansion.plugins.jei.machine.transposer.TransposerRecipeWrapper;
import cofh.thermalexpansion.util.managers.machine.TransposerManager;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import xyz.ajp.makezoomzoom.MZZThreadFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class TransposerRecipeCategoryExtract {
    private static ExecutorService executorService = Executors.newCachedThreadPool(new MZZThreadFactory("JEI CoFH Transposer Extract"));

    public static List<TransposerRecipeWrapper> getRecipes(IGuiHelper guiHelper, IIngredientRegistry ingredientRegistry) {
        Queue<Future<TransposerRecipeWrapper>> wrapperFutures = new ArrayDeque<>(64);

        List<TransposerRecipeWrapper> recipes = new ArrayList<>();

        for (TransposerManager.TransposerRecipe recipe : TransposerManager.getExtractRecipeList()) {
            recipes.add(new TransposerRecipeWrapper(guiHelper, recipe, RecipeUidsTE.TRANSPOSER_EXTRACT));
        }
        List<ItemStack> ingredients = ingredientRegistry.getIngredients(ItemStack.class);

        for (ItemStack ingredient : ingredients) {
            if (ingredient.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
                    wrapperFutures.add(addExtractRecipe(ingredient, fluid, recipes, guiHelper));
                    if ( wrapperFutures.size() == 64 ) {
                        try {
                            TransposerRecipeWrapper recipe = wrapperFutures.remove().get();
                            if (recipe != null) {
                                recipes.add(recipe);
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        while(!wrapperFutures.isEmpty()) {
            try {
                TransposerRecipeWrapper recipe = wrapperFutures.remove().get();
                if (recipe != null) {
                    recipes.add(recipe);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return recipes;
    }

    private static Future<TransposerRecipeWrapper> addExtractRecipe(ItemStack baseStack, Fluid fluid, List<TransposerRecipeWrapper> recipes, IGuiHelper guiHelper) {
        return executorService.submit(() -> {
            ItemStack filledStack = baseStack.copy();
            IFluidHandlerItem handler = filledStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            int fill = handler.fill(new FluidStack(fluid, Fluid.BUCKET_VOLUME), true);

            if (fill > 0) {
                filledStack = handler.getContainer().copy();
                FluidStack drainedFluid = handler.drain(Fluid.BUCKET_VOLUME, true);

                if (drainedFluid != null) {
                    ItemStack drainedStack = handler.getContainer();
                    TransposerManager.TransposerRecipe recipe = new TransposerManager.TransposerRecipe(filledStack, drainedStack, drainedFluid, TransposerManager.DEFAULT_ENERGY, drainedStack.getCount() <= 0 ? 0 : 100);
                    return new TransposerRecipeWrapper(guiHelper, recipe, RecipeUidsTE.TRANSPOSER_EXTRACT);
                }
            }
            return null;
        });
    }
}
