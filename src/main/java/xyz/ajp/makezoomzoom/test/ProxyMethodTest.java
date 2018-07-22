package xyz.ajp.makezoomzoom.test;

import forestry.factory.recipes.jei.bottler.BottlerRecipeWrapper;
import mezz.jei.api.ingredients.IIngredientRegistry;
import xyz.ajp.makezoomzoom.asmreimpl.forestry.factory.recipes.jei.bottler.BottlerRecipeMaker;

import java.util.List;

public class ProxyMethodTest {
    public static List<BottlerRecipeWrapper> getBottlerRecipes(IIngredientRegistry ingredientRegistry) {
        return BottlerRecipeMaker.getBottlerRecipes(ingredientRegistry);
    }

    public static void doNothing() {}
}
