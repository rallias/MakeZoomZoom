package xyz.ajp.makezoomzoom.asm.transformers.cofh.thermalexpansion;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.ajp.makezoomzoom.asmutil.squeek502.ASMHelper;

public class TransformTRCF implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        ClassNode classNode = ASMHelper.readClassFromBytes(basicClass);
        for ( MethodNode methodNode : classNode.methods ) {
            if ( methodNode.name.equals("getRecipes") ) {
//                System.out.println(ASMHelper.getMethodAsString(methodNode));
                InsnList replacementList = new InsnList();
                replacementList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                replacementList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                replacementList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xyz/ajp/makezoomzoom/asmreimpl/cofh/thermalexpansion/plugins/jei/machine/transposer/TransposerRecipeCategoryFill", "getRecipes", "(Lmezz/jei/api/IGuiHelper;Lmezz/jei/api/ingredients/IIngredientRegistry;)Ljava/util/List;", false));
                replacementList.add(new InsnNode(Opcodes.ARETURN));
                methodNode.instructions = replacementList;
//                System.out.println(ASMHelper.getMethodAsString(methodNode));
            }

        }
        return ASMHelper.writeClassToBytes(classNode);
    }
}
