package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;
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

        // Iterate over each method and optimize its instruction list.
        for (Method method : cgen.getMethods()) {
            Code code = method.getCode();
            if (code == null)
                continue; // Skip abstract or native methods.
            MethodGen mg = new MethodGen(method, cgen.getClassName(), cpgen);
            InstructionList ilist = mg.getInstructionList();
            if (ilist == null)
                continue;
            
            InstructionFinder finder = new InstructionFinder(ilist);
            Map<Integer, List<Object[]>> varMap = new HashMap<>();
            
            // Record variable assignments.
            findVariableAssignments(finder, ilist, cpgen, varMap);
            
            // Replace variable loads with constant pushes.
            replaceVariableLoads(finder, ilist, cpgen, varMap);
            
            try {
                mg.setMaxStack();
                mg.setMaxLocals();
                mg.update();
            } catch (TargetLostException e) {
                e.printStackTrace();
            }
            cgen.replaceMethod(method, mg.getMethod());
        }
        this.optimized = cgen.getJavaClass();
    }
    
    /**
     * Scans the instruction list for assignments where a constant is pushed and stored.
     */
    private void findVariableAssignments(InstructionFinder finder, InstructionList ilist, ConstantPoolGen cpgen,
            Map<Integer, List<Object[]>> varMap) {
        // Handles LDC + ISTORE
        for (Iterator<?> it = finder.search("LDC ISTORE"); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();
            LDC ldc = (LDC) match[0].getInstruction();
            ISTORE istore = (ISTORE) match[1].getInstruction();

            int varIndex = istore.getIndex();
            int value = ((org.apache.bcel.classfile.ConstantInteger) cpgen.getConstant(ldc.getIndex())).getValue();
            int position = match[0].getPosition();

            addVariableValue(varMap, varIndex, value, position);
        }

        // Handles BIPUSH + ISTORE
        for (Iterator<?> it = finder.search("BIPUSH ISTORE"); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();
            BIPUSH bipush = (BIPUSH) match[0].getInstruction();
            ISTORE istore = (ISTORE) match[1].getInstruction();

            int varIndex = istore.getIndex();
            int value = bipush.getValue();
            int position = match[0].getPosition();

            addVariableValue(varMap, varIndex, value, position);
        }

        // Handles SIPUSH + ISTORE
        for (Iterator<?> it = finder.search("SIPUSH ISTORE"); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();
            SIPUSH sipush = (SIPUSH) match[0].getInstruction();
            ISTORE istore = (ISTORE) match[1].getInstruction();

            int varIndex = istore.getIndex();
            int value = sipush.getValue();
            int position = match[0].getPosition();

            addVariableValue(varMap, varIndex, value, position);
        }

        // Handles small constants: ICONST_0 to ICONST_5.
        for (int i = 0; i <= 5; i++) {
            String pattern = "ICONST_" + i + " ISTORE";
            for (Iterator<?> it = finder.search(pattern); it.hasNext();) {
                InstructionHandle[] match = (InstructionHandle[]) it.next();
                ISTORE istore = (ISTORE) match[1].getInstruction();

                int varIndex = istore.getIndex();
                int position = match[0].getPosition();

                addVariableValue(varMap, varIndex, i, position);
            }
        }

        // Handles ICONST_M1 ISTORE (i.e. -1).
        for (Iterator<?> it = finder.search("ICONST_M1 ISTORE"); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();
            ISTORE istore = (ISTORE) match[1].getInstruction();

            int varIndex = istore.getIndex();
            int position = match[0].getPosition();

            addVariableValue(varMap, varIndex, -1, position);
        }
    }

    /**
     * Adds an assignment for a variable at a specific position.
     */
    private void addVariableValue(Map<Integer, List<Object[]>> varMap, int varIndex, Object value, int position) {
        if (!varMap.containsKey(varIndex)) {
            varMap.put(varIndex, new ArrayList<>());
        }
        List<Object[]> values = varMap.get(varIndex);
        // Ensure that previous entries have a proper third element for end position.
        for (int i = 0; i < values.size(); i++) {
            Object[] entry = values.get(i);
            if (entry.length < 3) {
                entry = Arrays.copyOf(entry, 3);
                values.set(i, entry);
            } else if (entry[2] == null) {
                entry[2] = position;
            }
        }
        values.add(new Object[] { value, position, null });
    }
    
    /**
     * Finds the constant value assigned to a variable at a given position.
     * Returns null if no valid assignment exists.
     */
    private Object findValueAtPosition(Map<Integer, List<Object[]>> varMap, int varIndex, int position) {
        if (!varMap.containsKey(varIndex)) {
            return null;
        }
        List<Object[]> values = varMap.get(varIndex);
        for (Object[] entry : values) {
            int startPos = (Integer) entry[1];
            Integer endPos = (entry.length > 2 && entry[2] != null) ? (Integer) entry[2] : Integer.MAX_VALUE;
            if (position >= startPos && position < endPos) {
                return entry[0];
            }
        }
        return null;
    }
    
    /**
     * Creates an instruction that pushes a constant onto the stack.
     */
    private Instruction constantInstruction(Object value, ConstantPoolGen cpgen) {
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            if (intValue >= -1 && intValue <= 5) {
                return new ICONST(intValue);
            } else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
                return new BIPUSH((byte) intValue);
            } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
                return new SIPUSH((short) intValue);
            } else {
                return new LDC(cpgen.addInteger(intValue));
            }
        }
        return null;
    }
    
    /**
     * Replaces variable load instructions (ILOAD) with constant push instructions when
     * a constant value is known.
     */
    private boolean replaceVariableLoads(InstructionFinder finder, InstructionList ilist,
            ConstantPoolGen cpgen, Map<Integer, List<Object[]>> varMap) {
        boolean modified = false;
        for (Iterator<?> it = finder.search("ILOAD"); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();
            ILOAD iload = (ILOAD) match[0].getInstruction();

            int varIndex = iload.getIndex();
            int position = match[0].getPosition();
            Object value = findValueAtPosition(varMap, varIndex, position);

            if (value != null) {
                Instruction replacedInstruction = constantInstruction(value, cpgen);
                // Replace the ILOAD instruction with the constant instruction.
                ilist.insert(match[0], replacedInstruction);
                try {
                    ilist.delete(match[0]);
                } catch (TargetLostException e) {
                    e.printStackTrace();
                }
                modified = true;
            }
        }
        return modified;
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