package xyz.ajp.makezoomzoom.mixin;

import com.google.common.collect.ImmutableList;
import hellfirepvp.modularmachinery.common.util.FuelItemHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xyz.ajp.makezoomzoom.asmutil.MZZThreadFactory;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

@Mixin(FuelItemHelper.class)
public class MixinModularMachineryFuelItemHelper {
    @Shadow private static List<ItemStack> knownFuelStacks = null;

    @Overwrite
    public static void initialize() {
        Executor executor = Executors.newFixedThreadPool(64, new MZZThreadFactory("Modular Machinery Fuel Item Helper"));
        NonNullList<ItemStack> stacks = NonNullList.create();
        for (Item i : ForgeRegistries.ITEMS) {
            CreativeTabs tab = i.getCreativeTab();
            if(tab != null) {
                i.getSubItems(tab, stacks);
            }
        }
        List<ItemStack> out = new LinkedList<>();

        Queue<Future<ItemStack>> stackFutures = new ArrayDeque<>(128);

        for (ItemStack stack : stacks) {
            stackFutures.add(((ExecutorService) executor).submit(() -> {
                if ( TileEntityFurnace.getItemBurnTime(stack) > 0 ) {
                    return stack;
                }
                return null;
            }));
            while(stackFutures.size() > 64) {
                try {
                    ItemStack thisStack = stackFutures.remove().get();
                    if ( thisStack != null ) {
                        out.add(thisStack);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        while(!stackFutures.isEmpty()) {
            try {
                ItemStack thisStack = stackFutures.remove().get();
                if ( thisStack != null ) {
                    out.add(thisStack);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        knownFuelStacks = ImmutableList.copyOf(out);
    }
}
