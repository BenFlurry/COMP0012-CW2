package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.ILOAD;

public class ConstantFolder {

    ClassParser parser = null;
    ClassGen gen = null;
    JavaClass original = null;
    JavaClass optimized = null;

    public ConstantFolder(String classFilePath) {
        try {
            this.parser = new ClassParser(classFilePath);
            this.original = this.parser.parse();
            this.gen = new ClassGen(this.original);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void optimize() {
        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        // task3 completed here
        cgen = dynamicVariableFoldingMethod(cgen, cpgen);

        this.optimized = cgen.getJavaClass();
    }

    /*
    method to implement task3 on the handout, completing dynamic variable folding as required, returning the class generator. 
    */  
    private ClassGen dynamicVariableFoldingMethod(ClassGen cgen, ConstantPoolGen cpgen) {
        for (org.apache.bcel.classfile.Method method : cgen.getMethods()) {
            MethodGen mg = new MethodGen(method, cgen.getClassName(), cpgen);
            InstructionList il = mg.getInstructionList();
            if (il == null)
                continue;
            boolean modified = false;
            // Map for tracking constant integer values per local variable index.
            java.util.Map<Integer, Integer> intConsts = new HashMap<>();

            // Traverse the instruction list.
            for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
                Instruction inst = ih.getInstruction();

                // Look for constant load instructions of type int.
                if ((inst instanceof ICONST) || (inst instanceof BIPUSH) || (inst instanceof SIPUSH)) {
                    int constVal = 0;
                    if (inst instanceof ICONST)
                        constVal = ((ICONST) inst).getValue().intValue();
                    else if (inst instanceof BIPUSH)
                        constVal = ((BIPUSH) inst).getValue().intValue();
                    else if (inst instanceof SIPUSH)
                        constVal = ((SIPUSH) inst).getValue().intValue();
                    // Check if the next instruction is an ISTORE to associate the constant value.
                    InstructionHandle next = ih.getNext();
                    if (next != null && next.getInstruction() instanceof ISTORE) {
                        int index = ((ISTORE) next.getInstruction()).getIndex();
                        intConsts.put(index, constVal);
                    }
                }
                // Replace ILOAD with a direct constant load if available.
                else if (inst instanceof ILOAD) {
                    int index = ((ILOAD) inst).getIndex();
                    if (intConsts.containsKey(index)) {
                        int value = intConsts.get(index);
                        Instruction newInst;
                        if (value >= -1 && value <= 5)
                            newInst = new ICONST(value);
                        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
                            newInst = new BIPUSH((byte)value);
                        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
                            newInst = new SIPUSH((short)value);
                        else
                            newInst = new LDC(cpgen.addInteger(value));
                        ih.setInstruction(newInst);
                        modified = true;
                    }
                }
                // When a variable is reassigned, remove its constant from the map.
                else if (inst instanceof ISTORE) {
                    int index = ((ISTORE) inst).getIndex();
                    intConsts.remove(index);
                }
                // Similar peep-hole logic for long, float and double can be added here using LLOAD/LSTORE,
                // FLOAD/FSTORE, DLOAD/DSTORE and their constant instructions (e.g., LCONST, FCONST, DCONST, LDC2_W).
            }

            if (modified) {
                mg.setMaxStack();
                mg.setMaxLocals();
                il.setPositions();
                cgen.replaceMethod(method, mg.getMethod());
            }
        }

        return cgen;
    }
    
    
    public void write(String optimisedFilePath) {
        this.optimize();
        try {
            FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
            this.optimized.dump(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}