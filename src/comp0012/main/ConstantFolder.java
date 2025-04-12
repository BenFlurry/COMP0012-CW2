package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Stack;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;

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
        logClassBytecode("Original", original);

        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        simpleVariableFoldingMethod(cgen, cpgen);
        constantVariableFoldingMethod(cgen, cpgen);
        cgen = dynamicVariableFoldingMethod(cgen, cpgen);

        this.optimized = cgen.getJavaClass();
        logClassBytecode("Optimized", this.optimized);
    }

    // START : CONSTANT FOLDING
    private void logClassBytecode(String title, JavaClass jc) {
        System.out.println("====== " + title + " ======");
        System.out.println("Class Name: " + jc.getClassName());
        ClassGen cgen = new ClassGen(jc);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        for (Method method : cgen.getMethods()) {
            System.out.println("Method: " + method.getName());
            InstructionList il = new InstructionList(method.getCode().getCode());
            System.out.println("Bytecode: \n" + il.toString());
            System.out.println();

        }
        // Print the constant pool
        System.out.println("Constant Pool: ");
        for (int i = 0; i < cpgen.getSize(); i++) {
            Constant c = cpgen.getConstant(i);
            if (c != null) {
                System.out.println("Index " + i + ": " + c.toString());
            }
        }
        System.out.println();

    }

    // Handle logic for constant folding
    private OptionalInt handleIntBinInstruction(Instruction inst, Number nlhs, Number nrhs) {
        if (!(inst instanceof IADD || inst instanceof ISUB || inst instanceof IMUL || inst instanceof IDIV)) {
            return OptionalInt.empty();
        }

        int lhs = nlhs.intValue();
        int rhs = nrhs.intValue();

        if (inst instanceof IADD) {
            return OptionalInt.of(((Integer) lhs) + ((Integer) rhs));
        }
        if (inst instanceof ISUB) {
            return OptionalInt.of(((Integer) lhs) - ((Integer) rhs));
        }
        if (inst instanceof IMUL) {
            return OptionalInt.of(((Integer) lhs) * ((Integer) rhs));
        }
        if (inst instanceof IDIV) {
            return OptionalInt.of(((Integer) lhs) / ((Integer) rhs));
        }

        throw new RuntimeException("Unreachable");
    }

    private OptionalLong handleLongBinInstruction(Instruction inst, Number nlhs, Number nrhs) {
        if (!(inst instanceof LADD || inst instanceof LSUB || inst instanceof LMUL || inst instanceof LDIV)) {
            return OptionalLong.empty();
        }

        long lhs = nlhs.longValue();
        long rhs = nrhs.longValue();

        if (inst instanceof LADD) {
            return OptionalLong.of(((Long) lhs) + ((Long) rhs));
        }
        if (inst instanceof LSUB) {
            return OptionalLong.of(((Long) lhs) - ((Long) rhs));
        }
        if (inst instanceof LMUL) {
            return OptionalLong.of(((Long) lhs) * ((Long) rhs));
        }
        if (inst instanceof LDIV) {
            return OptionalLong.of(((Long) lhs) / ((Long) rhs));
        }

        throw new RuntimeException("Unreachable");
    }

    private Optional<Float> handleFloatBinInstruction(Instruction inst, Number nlhs, Number nrhs) {
        if (!(inst instanceof FADD || inst instanceof FSUB || inst instanceof FMUL || inst instanceof FDIV)) {
            return Optional.empty();
        }

        float lhs = nlhs.floatValue();
        float rhs = nrhs.floatValue();

        if (inst instanceof FADD) {
            return Optional.of(((Float) lhs) + ((Float) rhs));
        }
        if (inst instanceof FSUB) {
            return Optional.of(((Float) lhs) - ((Float) rhs));
        }
        if (inst instanceof FMUL) {
            return Optional.of(((Float) lhs) * ((Float) rhs));
        }
        if (inst instanceof FDIV) {
            return Optional.of(((Float) lhs) / ((Float) rhs));
        }

        throw new RuntimeException("Unreachable");
    }

    private Optional<Double> handleDoubleBinInstruction(Instruction inst, Number nlhs, Number nrhs) {
        if (!(inst instanceof DADD || inst instanceof DSUB || inst instanceof DMUL || inst instanceof DDIV)) {
            return Optional.empty();
        }

        double lhs = nlhs.doubleValue();
        double rhs = nrhs.doubleValue();

        if (inst instanceof DADD) {
            return Optional.of(((Double) lhs) + ((Double) rhs));
        }
        if (inst instanceof DSUB) {
            return Optional.of(((Double) lhs) - ((Double) rhs));
        }
        if (inst instanceof DMUL) {
            return Optional.of(((Double) lhs) * ((Double) rhs));
        }
        if (inst instanceof DDIV) {
            return Optional.of(((Double) lhs) / ((Double) rhs));
        }

        throw new RuntimeException("Unreachable");
    }

    private boolean handlePushInstruction(Instruction inst, Stack<Number> stack, ConstantPoolGen cpgen) {
        if (!(inst instanceof ICONST || inst instanceof BIPUSH || inst instanceof SIPUSH
                || inst instanceof LCONST || inst instanceof FCONST || inst instanceof DCONST ||
                inst instanceof LDC || inst instanceof LDC2_W)) {
            return false;
        }

        if (inst instanceof ICONST) {
            stack.push(((ICONST) inst).getValue());
        }
        if (inst instanceof BIPUSH) {
            stack.push(((BIPUSH) inst).getValue());
        }
        if (inst instanceof SIPUSH) {
            stack.push(((SIPUSH) inst).getValue());
        }
        if (inst instanceof LCONST) {
            stack.push(((LCONST) inst).getValue());
        }
        if (inst instanceof FCONST) {
            stack.push(((FCONST) inst).getValue());
        }
        if (inst instanceof DCONST) {
            stack.push(((DCONST) inst).getValue());
        }

        if (inst instanceof LDC) {
            Object value = ((LDC) inst).getValue(cpgen);
            if (value instanceof Number)
                stack.push((Number) value);
        }

        if (inst instanceof LDC2_W) {
            Object value = ((LDC2_W) inst).getValue(cpgen);
            if (value instanceof Number)
                stack.push((Number) value);
        }

        return true;
    }

    private boolean handleStoreInstruction(Instruction inst, Stack<Number> stack,
            Map<Integer, Number> varIndexToConstVal) {

        if (!(inst instanceof StoreInstruction)) {
            return false;
        }

        if (stack.isEmpty())
            return false;

        Number val = stack.peek();
        if (inst instanceof StoreInstruction) {
            int index = ((StoreInstruction) inst).getIndex();
            varIndexToConstVal.put(index, val);
        }
        return true;
    }

    private boolean handleLoadInstruction(Instruction inst, Stack<Number> stack,
            Map<Integer, Number> varIndexToConstVal) {
        if (!(inst instanceof LoadInstruction)) {
            return false;
        }

        int index = ((LoadInstruction) inst).getIndex();
        if (varIndexToConstVal.containsKey(index)) {
            stack.push(varIndexToConstVal.get(index));
        }
        return true;
    }

    private boolean isBinOperation(Instruction inst) {
        return inst instanceof IADD || inst instanceof ISUB || inst instanceof IMUL || inst instanceof IDIV
                || inst instanceof LADD || inst instanceof LSUB || inst instanceof LMUL || inst instanceof LDIV
                || inst instanceof FADD || inst instanceof FSUB || inst instanceof FMUL || inst instanceof FDIV
                || inst instanceof DADD || inst instanceof DSUB || inst instanceof DMUL || inst instanceof DDIV;
    }

    private boolean handleBinInstruction(Instruction inst, Stack<Number> stack) {
        if (!isBinOperation(inst)) {
            return false;
        }

        if (stack.size() < 2) {
            System.out.println("[DEBUG] [Unexpected] Skipping " + inst.getName() + ". Stack size < 2.");
            throw new RuntimeException("[DEBUG] [Unexpected] Stack size < 2.");
        }

        Number rhs = stack.pop();
        Number lhs = stack.pop();

        OptionalInt intResult = handleIntBinInstruction(inst, lhs, rhs);
        if (intResult.isPresent()) {
            stack.push(intResult.getAsInt());
            return true;
        }

        OptionalLong longResult = handleLongBinInstruction(inst, lhs, rhs);
        if (longResult.isPresent()) {
            stack.push(longResult.getAsLong());
            return true;
        }

        Optional<Float> floatResult = handleFloatBinInstruction(inst, lhs, rhs);
        if (floatResult.isPresent()) {
            stack.push(floatResult.get());
            return true;
        }

        Optional<Double> doubleResult = handleDoubleBinInstruction(inst, lhs, rhs);
        if (doubleResult.isPresent()) {
            stack.push(doubleResult.get());
            return true;
        }

        throw new RuntimeException("[DEBUG] [Unexpected] Unrecognized instruction: " + inst.getName());
    }

    private void constantVariableFoldingMethod(ClassGen cgen, ConstantPoolGen cpgen) {
        for (Method method : cgen.getMethods()) {
            if (method.isAbstract() || method.isNative())
                continue;

            MethodGen mg = new MethodGen(method, cgen.getClassName(), cpgen);
            InstructionList il = mg.getInstructionList();

            if (il == null) {
                System.out.println("[DEBUG] [Unexpected] No instruction list found for method: " + method.getName()
                        + "\n\tSkipping ...");
                continue;
            }

            Map<Integer, Number> varIndexToConstVal = new HashMap<>();
            Stack<Number> stack = new Stack<>();

            for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
                Instruction inst = ih.getInstruction();

                handlePushInstruction(inst, stack, cpgen);
                handleStoreInstruction(inst, stack, varIndexToConstVal);
                handleLoadInstruction(inst, stack, varIndexToConstVal);
                handleBinInstruction(inst, stack);
            }

            if (stack.isEmpty()) {
                System.out.println("[OPTIMIZEF FAILURE] Could not optimize method: " + method.getName());
                continue;
            }

            Number result = stack.pop();
            InstructionList newIL = new InstructionList();
            InstructionFactory factory = new InstructionFactory(cgen);
            Type returnType = mg.getReturnType();

            newIL.append(new PUSH(cpgen, result));
            newIL.append(factory.createReturn(returnType));

            mg.setInstructionList(newIL);
            mg.setMaxStack();
            mg.setMaxLocals();
            cgen.replaceMethod(method, mg.getMethod());
        }
    }

    // END : CONSTANT FOLDING

    // Helper: verifies top N operands on the stack are constants
    private boolean checkOperands(java.util.Stack<Object> stack, int n) {
        if (stack.size() < n)
            return false;
        for (int i = 1; i <= n; i++) {
            if (!(stack.get(stack.size() - i) instanceof Integer))
                return false;
        }
        return true;
    }

    /*
     * method to implement task2 on the handout, completing simple variable folding
     * as required
     */
    private void simpleVariableFoldingMethod(ClassGen cgen, ConstantPoolGen cpgen) {
        for (Method method : cgen.getMethods()) {
            InstructionList instList = new InstructionList(method.getCode().getCode());
            simpleInt(instList, cpgen);
            simpleLong(instList, cpgen);
            simpleFloat(instList, cpgen);
            simpleDouble(instList, cpgen);
        }
    }

    /*
     * method to implement task3 on the handout, completing dynamic variable folding
     * as required, returning the class generator.
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
                            newInst = new BIPUSH((byte) value);
                        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
                            newInst = new SIPUSH((short) value);
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
                // Similar peep-hole logic for long, float and double can be added here using
                // LLOAD/LSTORE,
                // FLOAD/FSTORE, DLOAD/DSTORE and their constant instructions (e.g., LCONST,
                // FCONST, DCONST, LDC2_W).
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

    // task2 helper method
    private void simpleInt(InstructionList instList, ConstantPoolGen cpgen) {
        InstructionFinder finder = new InstructionFinder(instList);
        String pattern = "(ICONST|BIPUSH|SIPUSH|LDC) (ICONST|BIPUSH|SIPUSH|LDC) (IADD|ISUB|IMUL|IDIV)";

        for (Iterator<InstructionHandle[]> i = finder.search(pattern); i.hasNext();) {
            InstructionHandle[] match = i.next();
            int c1 = (int) getConstant(match[0].getInstruction(), cpgen, int.class);
            int c2 = (int) getConstant(match[1].getInstruction(), cpgen, int.class);

            int result = 0;
            switch (match[2].getInstruction().getName()) {
                case "iadd":
                    result = c1 + c2;
                    break;
                case "isub":
                    result = c1 - c2;
                    break;
                case "imul":
                    result = c1 * c2;
                    break;
                case "idiv":
                    result = c1 / c2;
                    break;
            }

            Instruction inst = null;
            if (result >= -128 && result <= 127) {
                inst = new BIPUSH((byte) result);
            } else if (result >= -32768 && result <= 32767) {
                inst = new SIPUSH((short) result);
            } else {
                inst = new LDC(cpgen.addInteger(result));
            }
            replaceInst(match, inst, instList);
        }
    }

    // task2 helper method
    private void simpleLong(InstructionList instList, ConstantPoolGen cpgen) {
        InstructionFinder finder = new InstructionFinder(instList);
        String pattern = "(LCONST|LDC2_W) (LCONST|LDC2_W) (LADD|LSUB|LMUL|LDIV)";

        for (Iterator<InstructionHandle[]> i = finder.search(pattern); i.hasNext();) {
            InstructionHandle[] match = i.next();
            long l1 = (long) getConstant(match[0].getInstruction(), cpgen, long.class);
            long l2 = (long) getConstant(match[1].getInstruction(), cpgen, long.class);

            long result = 0;
            switch (match[2].getInstruction().getName()) {
                case "ladd":
                    result = l1 + l2;
                    break;
                case "lsub":
                    result = l1 - l2;
                    break;
                case "lmul":
                    result = l1 * l2;
                    break;
                case "ldiv":
                    result = l1 / l2;
                    break;
            }

            Instruction inst = new LDC2_W(cpgen.addLong(result));
            replaceInst(match, inst, instList);
        }
    }

    // task2 helper method
    private void simpleFloat(InstructionList instList, ConstantPoolGen cpgen) {
        InstructionFinder finder = new InstructionFinder(instList);
        String pattern = "(FCONST|LDC_W) (FCONST|LDC_W) (FADD|FSUB|FMUL|FDIV)";

        for (Iterator<InstructionHandle[]> i = finder.search(pattern); i.hasNext();) {
            InstructionHandle[] match = i.next();
            float f1 = (float) getConstant(match[0].getInstruction(), cpgen, float.class);
            float f2 = (float) getConstant(match[1].getInstruction(), cpgen, float.class);

            float result = 0;
            switch (match[2].getInstruction().getName()) {
                case "fadd":
                    result = f1 + f2;
                    break;
                case "fsub":
                    result = f1 - f2;
                    break;
                case "fmul":
                    result = f1 * f2;
                    break;
                case "fdiv":
                    result = f1 / f2;
                    break;
            }

            Instruction inst = new LDC_W(cpgen.addFloat(result));
            replaceInst(match, inst, instList);
        }
    }

    // task2 helper method
    private void simpleDouble(InstructionList instList, ConstantPoolGen cpgen) {
        InstructionFinder finder = new InstructionFinder(instList);
        String pattern = "(DCONST|LDC2_W) (DCONST|LDC2_W) (DADD|DSUB|DMUL|DDIV)";

        for (Iterator<InstructionHandle[]> i = finder.search(pattern); i.hasNext();) {
            InstructionHandle[] match = i.next();
            double d1 = (double) getConstant(match[0].getInstruction(), cpgen, double.class);
            double d2 = (double) getConstant(match[1].getInstruction(), cpgen, double.class);

            // Perform folding based on operation
            double result = 0;
            switch (match[2].getInstruction().getName()) {
                case "dadd":
                    result = d1 + d2;
                    break;
                case "dsub":
                    result = d1 - d2;
                    break;
                case "dmul":
                    result = d1 * d2;
                    break;
                case "ddiv":
                    result = d1 / d2;
                    break;
            }

            Instruction inst = new LDC2_W(cpgen.addDouble(result));
            replaceInst(match, inst, instList);
        }
    }

    // task2 helper method
    private <T> Object getConstant(org.apache.bcel.generic.Instruction inst, ConstantPoolGen cpgen,
            Class<T> constantType) {
        if (constantType == int.class) {
            if (inst instanceof LDC) {
                return ((ConstantInteger) ((cpgen.getConstantPool()).getConstant(((LDC) inst).getIndex()))).getBytes();
            } else if (inst instanceof ICONST) {
                return ((ICONST) inst).getValue();
            }
        } else if (constantType == long.class) {
            if (inst instanceof LDC2_W) {
                return ((ConstantLong) ((cpgen.getConstantPool()).getConstant(((LDC2_W) inst).getIndex()))).getBytes();
            } else if (inst instanceof LCONST) {
                return ((LCONST) inst).getValue();
            }
        } else if (constantType == float.class) {
            return ((ConstantFloat) ((cpgen.getConstantPool()).getConstant(((LDC_W) inst).getIndex()))).getBytes();
        } else if (constantType == double.class) {
            return ((ConstantDouble) ((cpgen.getConstantPool()).getConstant(((LDC2_W) inst).getIndex()))).getBytes();
        }
        return 0;
    }

    // task2 helper method
    private void replaceInst(InstructionHandle[] toReplace, Instruction replacement, InstructionList instList) {
        for (InstructionHandle handle : toReplace) {
            handle.setInstruction(replacement);
        }
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