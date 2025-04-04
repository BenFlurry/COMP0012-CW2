package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here
		for (Method method : cgen.getMethods()) {
			InstructionList instList = new InstructionList(method.getCode().getCode());

			simpleInt(instList, cpgen);
			simpleLong(instList, cpgen);
			simpleFloat(instList, cpgen);
			simpleDouble(instList, cpgen);
		}
		this.optimized = gen.getJavaClass();
	}

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

	private void replaceInst(InstructionHandle[] toReplace, Instruction replacement, InstructionList instList){
		for (InstructionHandle handle : toReplace) {
			handle.setInstruction(replacement);
		}
	}

	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}




}