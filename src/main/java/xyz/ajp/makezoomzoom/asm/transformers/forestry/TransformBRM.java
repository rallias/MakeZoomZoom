package xyz.ajp.makezoomzoom.asm.transformers.forestry;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.ajp.makezoomzoom.asmutil.squeek502.ASMHelper;

public class TransformBRM implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        ClassNode classNode = ASMHelper.readClassFromBytes(basicClass);
        for ( MethodNode methodNode : classNode.methods ) {
            if ( methodNode.name.equals("getBottlerRecipes") ) {
                //System.out.println(ASMHelper.getMethodAsString(methodNode));
                InsnList replacementList = new InsnList();
                replacementList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                //    INVOKESTATIC xyz/ajp/makezoomzoom/asmreimpl/forestry/factory/recipes/jei/bottler/BottlerRecipeMaker.getBottlerRecipes (Lmezz/jei/api/ingredients/IIngredientRegistry;)Ljava/util/List;
                replacementList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xyz/ajp/makezoomzoom/asmreimpl/forestry/factory/recipes/jei/bottler/BottlerRecipeMaker", "getBottlerRecipes", "(Lmezz/jei/api/ingredients/IIngredientRegistry;)Ljava/util/List;", false));
                replacementList.add(new InsnNode(Opcodes.ARETURN));
                methodNode.instructions = replacementList;
                //System.out.println(ASMHelper.getMethodAsString(methodNode));
            }

        }
        return ASMHelper.writeClassToBytes(classNode);
    }
}
