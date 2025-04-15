package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

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
        ClassGen cgen = new ClassGen(original);
        ConstantPoolGen cpgen = cgen.getConstantPool();

		// task2 completed here
		simpleVariableFoldingMethod(cgen, cpgen);
        constantFoldingMethod(cgen, cpgen);
        // task3 completed here
        cgen = dynamicVariableFoldingMethod(cgen, cpgen);
        this.optimized = cgen.getJavaClass();
    }
    private void constantFoldingMethod(ClassGen cgen, ConstantPoolGen cpgen) {
        for (org.apache.bcel.classfile.Method method : cgen.getMethods()) {
            MethodGen mg = new MethodGen(method, cgen.getClassName(), cpgen);
            InstructionList il = mg.getInstructionList();
            if (il == null)
                continue;
            boolean modified = false;
            // Use an array of handles to avoid issues when deleting instructions.
            InstructionHandle[] handles = il.getInstructionHandles();
            for (int i = 0; i < handles.length; i++) {
                InstructionHandle ih = handles[i];
                if (ih == null)
                    continue;
                Instruction inst = ih.getInstruction();
                if (inst instanceof org.apache.bcel.generic.ArithmeticInstruction) {
                    // Look two instructions back.
                    InstructionHandle prev1 = ih.getPrev();
                    InstructionHandle prev2 = (prev1 != null) ? prev1.getPrev() : null;
                    if (prev1 != null && prev2 != null) {
                        Integer c1 = extractConstant(prev2.getInstruction(), cpgen);
                        Integer c2 = extractConstant(prev1.getInstruction(), cpgen);
                        if (c1 != null && c2 != null) {
                            int result = 0;
                            if (inst instanceof IADD) {
                                result = c1 + c2;
                            } else if (inst instanceof ISUB) {
                                result = c1 - c2;
                            } else if (inst instanceof IMUL) {
                                result = c1 * c2;
                            } else if (inst instanceof IDIV) {
                                if (c2 == 0)
                                    continue;
                                result = c1 / c2;
                            } else if (inst instanceof IREM) {
                                if (c2 == 0)
                                    continue;
                                result = c1 % c2;
                            } else {
                                continue;
                            }
                            Instruction newInst;
                            if (result >= -1 && result <= 5)
                                newInst = new ICONST(result);
                            else if (result >= Byte.MIN_VALUE && result <= Byte.MAX_VALUE)
                                newInst = new BIPUSH((byte) result);
                            else if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE)
                                newInst = new SIPUSH((short) result);
                            else
                                newInst = new LDC(cpgen.addInteger(result));
                            try {
                                // Remove the two constant push instructions.
                                il.delete(prev2);
                                il.delete(prev1);
                                // Replace the arithmetic op with the new constant push.
                                ih.setInstruction(newInst);
                                modified = true;
                            } catch (org.apache.bcel.generic.TargetLostException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (modified) {
                mg.setMaxStack();
                mg.setMaxLocals();
                il.setPositions();
                cgen.replaceMethod(method, mg.getMethod());
            }
        }
    }

    /*
    method to implement task2 on the handout, completing simple variable folding as required
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

            // Iterate over the instruction list.
            // (We use an array of handles to avoid issues when deleting instructions.)
            InstructionHandle[] handles = il.getInstructionHandles();
            for (int i = 0; i < handles.length; i++) {
                InstructionHandle ih = handles[i];
                if (ih == null) continue; // could have been deleted in an earlier pass.
                Instruction inst = ih.getInstruction();

                // --- Constant propagation: record constant assignments ---
                if ((inst instanceof ICONST) || (inst instanceof BIPUSH) || (inst instanceof SIPUSH)) {
                    int constVal = 0;
                    if (inst instanceof ICONST)
                        constVal = ((ICONST) inst).getValue().intValue();
                    else if (inst instanceof BIPUSH)
                        constVal = ((BIPUSH) inst).getValue().intValue();
                    else if (inst instanceof SIPUSH)
                        constVal = ((SIPUSH) inst).getValue().intValue();
                    InstructionHandle next = ih.getNext();
                    if (next != null && next.getInstruction() instanceof ISTORE) {
                        int index = ((ISTORE) next.getInstruction()).getIndex();
                        intConsts.put(index, constVal);
                    }
                }
                // Replace ILOAD with constant load if available.
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

                // --- Dynamic folding: Fold binary arithmetic instructions ---
                // Check if current instruction is a binary arithmetic op.
                if (inst instanceof org.apache.bcel.generic.ArithmeticInstruction) {
                    // Backtrack: the two previous instructions should be constant pushes.
                    InstructionHandle prev1 = ih.getPrev();
                    InstructionHandle prev2 = (prev1 != null) ? prev1.getPrev() : null;
                    if (prev1 != null && prev2 != null) {
                        Integer c1 = extractConstant(prev2.getInstruction(), cpgen);
                        Integer c2 = extractConstant(prev1.getInstruction(), cpgen);
                        if (c1 != null && c2 != null) {
                            int result = 0;
                            if (inst instanceof org.apache.bcel.generic.IADD) {
                                result = c1 + c2;
                            } else if (inst instanceof org.apache.bcel.generic.ISUB) {
                                result = c1 - c2;
                            } else if (inst instanceof org.apache.bcel.generic.IMUL) {
                                result = c1 * c2;
                            } else if (inst instanceof org.apache.bcel.generic.IDIV) {
                                // Avoid division by zero.
                                if (c2 == 0)
                                    continue;
                                result = c1 / c2;
                            } else if (inst instanceof org.apache.bcel.generic.IREM) {
                                if (c2 == 0)
                                    continue;
                                result = c1 % c2;
                            } else {
                                continue; // if op is not supported, skip.
                            }
                            // Create a new constant load instruction with the folded result.
                            Instruction newInst;
                            if (result >= -1 && result <= 5)
                                newInst = new ICONST(result);
                            else if (result >= Byte.MIN_VALUE && result <= Byte.MAX_VALUE)
                                newInst = new BIPUSH((byte) result);
                            else if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE)
                                newInst = new SIPUSH((short) result);
                            else
                                newInst = new LDC(cpgen.addInteger(result));
                            try {
                                // Delete the two constant push instructions.
                                il.delete(prev2);
                                il.delete(prev1);
                                // Replace the arithmetic op with the new constant.
                                ih.setInstruction(newInst);
                                modified = true;
                            } catch (org.apache.bcel.generic.TargetLostException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            // If modifications were made, update the method.
            if (modified) {
                mg.setMaxStack();
                mg.setMaxLocals();
                il.setPositions();
                cgen.replaceMethod(method, mg.getMethod());
            }
        }
        return cgen;
    }

/*
 * Helper method to extract an integer constant from a constant push instruction.
 * It returns the integer value if the instruction is ICONST, BIPUSH, SIPUSH, or an LDC with an Integer; otherwise, null.
 */
private Integer extractConstant(Instruction inst, ConstantPoolGen cpgen) {
    if (inst instanceof ICONST) {
        return ((ICONST) inst).getValue().intValue();
    } else if (inst instanceof BIPUSH) {
        return ((BIPUSH) inst).getValue().intValue();
    } else if (inst instanceof SIPUSH) {
        return ((SIPUSH) inst).getValue().intValue();
    } else if (inst instanceof LDC) {
        LDC ldc = (LDC) inst;
        Object value = ldc.getValue(cpgen);
        if (value instanceof Integer)
            return (Integer) value;
    }
    return null;
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
				case "iadd":  result = c1 + c2; break;
				case "isub":  result = c1 - c2; break;
				case "imul":  result = c1 * c2; break;
				case "idiv":  result = c1 / c2; break;
			}

			Instruction inst = null;
			if (result >= -128 && result <= 127) {
				inst = new BIPUSH((byte) result);
			}else if (result >= -32768 && result <= 32767) {
				inst = new SIPUSH((short) result);
			}else {
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
				case "ladd": result = l1 + l2; break;
				case "lsub": result = l1 - l2; break;
				case "lmul": result = l1 * l2; break;
				case "ldiv": result = l1 / l2; break;
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
				case "fadd": result = f1 + f2; break;
				case "fsub": result = f1 - f2; break;
				case "fmul": result = f1 * f2; break;
				case "fdiv": result = f1 / f2; break;
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
				case "dadd": result = d1 + d2; break;
				case "dsub": result = d1 - d2; break;
				case "dmul": result = d1 * d2; break;
				case "ddiv": result = d1 / d2; break;
			}

			Instruction inst = new LDC2_W(cpgen.addDouble(result));
			replaceInst(match, inst, instList);
		}
	}

	// task2 helper method
	private <T> Object getConstant(org.apache.bcel.generic.Instruction inst, ConstantPoolGen cpgen, Class<T> constantType) {
		if (constantType == int.class) {
			if (inst instanceof LDC) {
				return ((ConstantInteger)((cpgen.getConstantPool()).getConstant(((LDC) inst).getIndex()))).getBytes();
			} else if (inst instanceof ICONST) {
				return ((ICONST) inst).getValue();
			}
		} else if (constantType == long.class) {
			if (inst instanceof LDC2_W) {
				return ((ConstantLong)((cpgen.getConstantPool()).getConstant(((LDC2_W) inst).getIndex()))).getBytes();
			} else if (inst instanceof LCONST) {
				return ((LCONST) inst).getValue();
			}
		} else if (constantType == float.class) {
			return ((ConstantFloat)((cpgen.getConstantPool()).getConstant(((LDC_W) inst).getIndex()))).getBytes();
		} else if (constantType == double.class) {
			return ((ConstantDouble)((cpgen.getConstantPool()).getConstant(((LDC2_W) inst).getIndex()))).getBytes();
		}
		return 0;
	}

	// task2 helper method
	private void replaceInst(InstructionHandle[] toReplace, Instruction replacement, InstructionList instList){
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