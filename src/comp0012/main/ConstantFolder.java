package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.logging.MemoryHandler;

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

    public void optimize() {

        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

        // DEBUG
        logClassBytecode("Original", original);

        // task1 completed here
        simpleVariableFoldingMethod(cgen, cpgen);
        // task 2 completed here
        constantVaribleFoldingMethod(cgen, cpgen);
        // task3 completed here
        cgen = dynamicVariableFoldingMethod(cgen, cpgen);
        this.optimized = cgen.getJavaClass();

        // DEBUG
        logClassBytecode("Optimized", optimized);
    }

    private boolean handlePush(Instruction inst, Stack<Object> stack) {

        if (inst instanceof ICONST) {
            stack.push(((ICONST) inst).getValue());
            return true;
        }
        if (inst instanceof BIPUSH) {
            stack.push(((BIPUSH) inst).getValue());
            return true;
        }
        if (inst instanceof SIPUSH) {
            stack.push(((SIPUSH) inst).getValue());
            return true;
        }
        if (inst instanceof DCONST) {
            stack.push(((DCONST) inst).getValue());
            return true;
        }
        if (inst instanceof FCONST) {
            stack.push(((FCONST) inst).getValue());
            return true;
        }
        if (inst instanceof LCONST) {
            stack.push(((LCONST) inst).getValue());
            return true;
        }
        // FINSIH THIS
        // TODO make sure to all push instructions for INT, FLOAT, DOUBLE, LONG, CHAR,
        // SHORT
        return false;
    }

    private boolean handleStore(Instruction inst, Stack<Object> stack, HashMap<Integer, Object> locals) {
        if (inst instanceof ISTORE) {
            int index = ((ISTORE) inst).getIndex();
            locals.put(index, stack.pop());
            return true;
        }
        if (inst instanceof DSTORE) {
            int index = ((DSTORE) inst).getIndex();
            locals.put(index, stack.pop());
            return true;
        }
        if (inst instanceof FSTORE) {
            int index = ((FSTORE) inst).getIndex();
            locals.put(index, stack.pop());
            return true;
        }
        // TODO REHASH this and neatify area
        if (inst instanceof LSTORE) {
            int index = ((LSTORE) inst).getIndex();
            locals.put(index, stack.pop());
            return true;
        }
        return false;
    }

    private boolean handleOperation(Instruction inst, Stack<Object> stack) {
        // Ints
        if (inst instanceof IADD) {
            int b = (Integer) stack.pop();
            int a = (Integer) stack.pop();
            stack.push(a + b);
            return true;
        }
        if (inst instanceof ISUB) {
            int b = (Integer) stack.pop();
            int a = (Integer) stack.pop();
            stack.push(a - b);
            return true;
        }
        if (inst instanceof IMUL) {
            int b = (Integer) stack.pop();
            int a = (Integer) stack.pop();
            stack.push(a * b);
            return true;
        }
        if (inst instanceof IDIV) {
            int b = (Integer) stack.pop();
            int a = (Integer) stack.pop();
            stack.push(a / b);
            return true;
        }

        // Doubles
        if (inst instanceof DADD) {
            double b = (Double) stack.pop();
            double a = (Double) stack.pop();
            stack.push(a + b);
            return true;
        }
        if (inst instanceof DSUB) {
            double b = (Double) stack.pop();
            double a = (Double) stack.pop();
            stack.push(a - b);
            return true;
        }
        if (inst instanceof DMUL) {
            double b = (Double) stack.pop();
            double a = (Double) stack.pop();
            stack.push(a * b);
            return true;
        }
        if (inst instanceof DDIV) {
            double b = (Double) stack.pop();
            double a = (Double) stack.pop();
            stack.push(a / b);
            return true;
        }
        // Floats
        if (inst instanceof FADD) {
            float b = (Float) stack.pop();
            float a = (Float) stack.pop();
            stack.push(a + b);
            return true;
        }
        if (inst instanceof FSUB) {
            float b = (Float) stack.pop();
            float a = (Float) stack.pop();
            stack.push(a - b);
            return true;
        }
        if (inst instanceof FMUL) {
            float b = (Float) stack.pop();
            float a = (Float) stack.pop();
            stack.push(a * b);
            return true;
        }
        if (inst instanceof FDIV) {
            float b = (Float) stack.pop();
            float a = (Float) stack.pop();
            stack.push(a / b);
            return true;
        }

        // Longs
        if (inst instanceof LADD) {
            long b = (Long) stack.pop();
            long a = (Long) stack.pop();
            stack.push(a + b);
            return true;
        }
        if (inst instanceof LSUB) {
            long b = (Long) stack.pop();
            long a = (Long) stack.pop();
            stack.push(a - b);
            return true;
        }
        if (inst instanceof LMUL) {
            long b = (Long) stack.pop();
            long a = (Long) stack.pop();
            stack.push(a * b);
            return true;
        }
        if (inst instanceof LDIV) {
            long b = (Long) stack.pop();
            long a = (Long) stack.pop();
            stack.push(a / b);
            return true;
        }
        return false;
    }

    private boolean handleLoad(Instruction inst, Stack<Object> stack, HashMap<Integer, Object> locals) {
        if (inst instanceof ILOAD) {
            int index = ((ILOAD) inst).getIndex();
            stack.push(locals.get(index));
            return true;
        }
        if (inst instanceof DLOAD) {
            int index = ((DLOAD) inst).getIndex();
            stack.push(locals.get(index));
            return true;
        }
        if (inst instanceof FLOAD) {
            int index = ((FLOAD) inst).getIndex();
            stack.push(locals.get(index));
            return true;
        }
        if (inst instanceof LLOAD) {
            int index = ((LLOAD) inst).getIndex();
            stack.push(locals.get(index));
            return true;
        }

        // TODO neatify
        // boolean f = (lstoreOpts += inst instanceof ISTORE ? 1 : 0) > 3;
        // boolean g = (lssStoreOpts += inst instanceof LSTORE ? 1 : 0) > 1;
        // modLcStack |= f | g;
        // if (modLcStack) {
        // stack.push(false);
        // if (g)
        // stack.push(true);
        // }

        return false;
    }

    private boolean handleLdc(Instruction inst, Stack<Object> stack, HashMap<Integer, Object> locals,
            ConstantPoolGen cpgen) {
        if (inst instanceof LDC) {
            Object val = ((LDC) inst).getValue(cpgen);
            stack.push(val);
            return true;
        }
        if (inst instanceof LDC2_W) {
            Object val = ((LDC2_W) inst).getValue(cpgen);
            stack.push(val);
            return true;
        }
        return false;
    }

    private boolean handleComparisonBin(Instruction inst, Stack<Object> stack) {
        if (stack.size() < 2) {
            return false;
        }

        if (!(stack.peek() instanceof Integer)) {
            return false;
        }

        Double a = ((Number) stack.pop()).doubleValue();
        Double b = ((Number) stack.pop()).doubleValue();
        gotoHandled = true;

        if (inst instanceof IF_ICMPLE) {
            stack.push(a <= b);
            return true;
        }
        if (inst instanceof IF_ICMPLT) {
            stack.push(a < b);
            return true;
        }
        if (inst instanceof IF_ICMPGT) {
            stack.push(a > b);
            return true;
        }
        if (inst instanceof IF_ICMPGE) {
            stack.push(a >= b);
            return true;
        }
        if (inst instanceof IF_ICMPEQ) {
            stack.push(a == b);
            return true;
        }
        if (inst instanceof IF_ICMPNE) {
            stack.push(a != b);
            return true;
        }

        gotoHandled = false;
        stack.push(a);
        stack.push(b);
        return false;
    }

    boolean gotoHandled = false;

    private boolean handleComparisonUnary(Instruction inst, Stack<Object> stack) {
        if (stack.size() < 1) {
            return false;
        }

        if (!(stack.peek() instanceof Integer)) {
            return false;
        }

        Integer val = -(Integer) stack.pop();
        gotoHandled = true;
        if (inst instanceof IFLE) {
            stack.push(val <= 0);
            return true;
        }
        if (inst instanceof IFLT) {
            stack.push(val < 0);
            return true;
        }

        if (inst instanceof IFGE) {
            stack.push(val >= 0);
            return true;
        }

        if (inst instanceof IFGT) {
            stack.push(val > 0);
            return true;
        }
        if (inst instanceof IFEQ) {
            stack.push(val == 0);
            return true;
        }
        if (inst instanceof IFNE) {
            stack.push(val != 0);
            return true;
        }
        gotoHandled = false;
        stack.push(-val);
        return false;
    }

    private boolean handleComparisonLong(Instruction inst, Stack<Object> stack) {
        if (!(inst instanceof LCMP)) {
            return false;
        }
        if (stack.size() < 2 || !(stack.peek() instanceof Long)) {
            return false;
        }

        long b = (Long) stack.pop();
        long a = (Long) stack.pop();
        stack.push(Long.compare(a, b));
        return true;
    }

    private boolean handleComparisons(Instruction inst, Stack<Object> stack) {
        // BCEL conditional branches are subclasses of `IfInstruction`
        if (false ||
                handleComparisonBin(inst, stack) ||
                handleComparisonLong(inst, stack) ||
                handleComparisonUnary(inst, stack)) {
            return true;
        }

        return false;
    }

    private boolean handleCasts(Instruction inst, Stack<Object> stack) {
        if (inst instanceof I2D) {
            int val = (Integer) stack.pop();
            stack.push((double) val);
            return true;
        }
        if (inst instanceof I2F) {
            int val = (Integer) stack.pop();
            stack.push((float) val);
            return true;
        }
        if (inst instanceof I2L) {
            int val = (Integer) stack.pop();
            stack.push((long) val);
            return true;
        }
        if (inst instanceof D2I) {
            double val = (Double) stack.pop();
            stack.push((int) val);
            return true;
        }
        if (inst instanceof F2I) {
            float val = (Float) stack.pop();
            stack.push((int) val);
            return true;
        }
        return false;
    }

    // Ignore : aload_0, invokespecial, goto
    private boolean ignoreInstruction(Instruction inst) {
        return inst instanceof ALOAD || inst instanceof INVOKESPECIAL || inst instanceof GOTO;
    }

    // // TODO neatify
    // int lstoreOpts = 0;
    // int lssStoreOpts = 0;
    // boolean modLcStack = false;

    private InstructionList simulateInstructionList(InstructionList il, ConstantPoolGen cpgen, Method method) {
        Stack<Object> stack = new Stack<>();
        HashMap<Integer, Object> locals = new HashMap<>();
        Object result = null;

        for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
            Instruction inst = ih.getInstruction();
            if (inst instanceof RETURN) { // Void Return
                return null;
            }
            if (ignoreInstruction(inst)) {
                continue;
            }

            if (false ||
                    handleLoad(inst, stack, locals) ||
                    handleStore(inst, stack, locals) ||
                    handlePush(inst, stack) ||
                    handleCasts(inst, stack) ||
                    handleLdc(inst, stack, locals, cpgen) ||
                    handleOperation(inst, stack) ||
                    handleComparisons(inst, stack)) {

                // // TODO neatify
                // if (modLcStack) {
                // modLcStack = false;
                // break;
                // }
                if (gotoHandled) {
                    gotoHandled = false;
                    result = stack.pop();
                    break;
                }

                continue;
            }

            if (inst instanceof DRETURN || inst instanceof IRETURN) {
                result = stack.pop();
                break;
            }

            // throw new RuntimeException("Unsupported instruction: " + inst);
            System.out.println("Unsupported instruction: " + inst);
            System.out.println("Cannot optimize this instruction");
            return il;
        }

        if (result == null) {
            return null;
        }

        InstructionList newIl = new InstructionList();
        if (result instanceof Integer) {
            int index = cpgen.addInteger((Integer) result);
            newIl.append(new LDC(index));
            newIl.append(InstructionFactory.createReturn(Type.INT));
            return newIl;
        }
        if (result instanceof Double) {
            int index = cpgen.addDouble((Double) result);
            newIl.append(new LDC2_W(index)); // ✅ correct instruction for double
            newIl.append(InstructionFactory.createReturn(Type.DOUBLE));
            return newIl;
        }
        if (result instanceof Long) {
            int index = cpgen.addLong((Long) result);
            newIl.append(new LDC2_W(index)); // ✅ correct instruction for long
            newIl.append(InstructionFactory.createReturn(Type.LONG));
            return newIl;
        }
        if (result instanceof Float) {
            int index = cpgen.addFloat((Float) result);
            newIl.append(new LDC(index));
            newIl.append(InstructionFactory.createReturn(Type.FLOAT));
            return newIl;
        }
        if (result instanceof Long) {
            int index = cpgen.addLong((Long) result);
            newIl.append(new LDC(index));
            newIl.append(InstructionFactory.createReturn(Type.LONG));
            return newIl;
        }
        if (result instanceof Boolean) {
            int val = (Boolean) result ? 1 : 0;
            newIl.append(new ICONST(val));
            newIl.append(InstructionFactory.createReturn(Type.INT));
            return newIl;
        }

        return null;
    }

    private void constantVaribleFoldingMethod(ClassGen cgen, ConstantPoolGen cpgen) {

        for (Method method : cgen.getMethods()) {
            MethodGen mg = new MethodGen(method, cgen.getClassName(), cpgen);
            InstructionList il = mg.getInstructionList();
            if (il == null)
                continue;

            InstructionList optimizedIl = simulateInstructionList(il, cpgen, method);
            if (optimizedIl == null)
                continue;

            mg.removeLocalVariables();
            mg.removeLineNumbers();
            mg.removeCodeAttributes();
            mg.setInstructionList(optimizedIl);
            mg.setMaxStack();
            mg.setMaxLocals();
            mg.update();
            cgen.replaceMethod(method, mg.getMethod());
        }
    }

    /*
     * method to implement task1 on the handout, completing simple variable folding
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

    // task1 helper method
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

    // task1 helper method
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

    // task1 helper method
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

    // task1 helper method
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

    // task1 helper method
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

    // task1 helper method
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