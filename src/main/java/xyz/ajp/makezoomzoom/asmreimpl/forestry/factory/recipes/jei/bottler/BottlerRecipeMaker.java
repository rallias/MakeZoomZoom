package xyz.ajp.makezoomzoom.asmreimpl.forestry.factory.recipes.jei.bottler;

import forestry.factory.recipes.jei.bottler.BottlerRecipeWrapper;
import mezz.jei.api.ingredients.IIngredientRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import xyz.ajp.makezoomzoom.MZZThreadFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;

public class BottlerRecipeMaker {
    private static ExecutorService executorService = Executors.newCachedThreadPool(new MZZThreadFactory("JEI Forestry Bottler"));

    public static List<BottlerRecipeWrapper> getBottlerRecipes(IIngredientRegistry ingredientRegistry) {
        Deque<Future<List<BottlerRecipeWrapper>>> wrapperFutures = new ArrayDeque<>(64);

        List<BottlerRecipeWrapper> recipes = new ArrayList<>();
        for (ItemStack stack : ingredientRegistry.getAllIngredients(ItemStack.class)) {
            wrapperFutures.add(executorService.submit(new Callable<List<BottlerRecipeWrapper>>() {
                @Override
                public List<BottlerRecipeWrapper> call() throws Exception {
                    List<BottlerRecipeWrapper> retList = new ArrayList<>(2);
                    if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                        IFluidHandlerItem fluidHandler = stack.copy().getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                        if (fluidHandler != null) {
                            final boolean canDrain = canDrain(fluidHandler);
                            final boolean canFill = canFill(fluidHandler);

                            if (canDrain) {
                                FluidStack drainedFluid = fluidHandler.drain(Integer.MAX_VALUE, true);
                                if (drainedFluid != null) {
                                    ItemStack drained = fluidHandler.getContainer();
                                    retList.add(new BottlerRecipeWrapper(stack, drainedFluid, drained, false));
                                }
                            }

                            if (canFill) {
                                for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
                                    IFluidHandlerItem fillingCapability = stack.copy().getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                                    if (fillingCapability != null) {
                                        int fill = fillingCapability.fill(new FluidStack(fluid, Integer.MAX_VALUE), true);
                                        if (fill > 0) {
                                            FluidStack filledFluid = new FluidStack(fluid, fill);
                                            ItemStack filled = fillingCapability.getContainer();
                                            retList.add(new BottlerRecipeWrapper(stack, filledFluid, filled, true));
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    return retList;
                }
            }));

            if (wrapperFutures.size() == 64) {
                try {
                    List<BottlerRecipeWrapper> list = wrapperFutures.remove().get();
                    recipes.addAll(list);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        while (!wrapperFutures.isEmpty()) {
            try {
                List<BottlerRecipeWrapper> list = wrapperFutures.remove().get();
                recipes.addAll(list);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return recipes;
    }

    private static boolean canDrain(IFluidHandler fluidHandler) {
        for (IFluidTankProperties properties : fluidHandler.getTankProperties()) {
            if (properties.canDrain()) {
                return true;
            }
        }
        return false;
    }

    private static boolean canFill(IFluidHandler fluidHandler) {
        for (IFluidTankProperties properties : fluidHandler.getTankProperties()) {
            if (properties.canFill()) {
                return true;
            }
        }
        return false;
    }
}
