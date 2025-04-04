package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;


import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;

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
       private void findVariableAssignments(InstructionFinder finder, InstructionList ilist,  ConstantPoolGen cpgen,
           java.util.Map<Integer, java.util.List<Object[]>> varMap) {
          
           // Handles LDC + ISTORE
           for (java.util.Iterator<?> it = finder.search("LDC ISTORE"); it.hasNext();) {
               InstructionHandle[] match = (InstructionHandle[]) it.next();
               LDC ldc = (LDC) match[0].getInstruction();
               ISTORE istore = (ISTORE) match[1].getInstruction();


               int varIndex = istore.getIndex();
               int value = ((org.apache.bcel.classfile.ConstantInteger) cpgen.getConstant(ldc.getIndex())).getValue();
               int position = match[0].getPosition();
              
               addVariableValue(varMap, varIndex, value, position);
           }


           // Handles BIPUSH + ISTORE
           for (java.util.Iterator<?> it = finder.search("BIPUSH ISTORE"); it.hasNext();) {
               InstructionHandle[] match = (InstructionHandle[]) it.next();
               BIPUSH bipush = (BIPUSH) match[0].getInstruction();
               ISTORE istore = (ISTORE) match[1].getInstruction();


               int varIndex = istore.getIndex();
               int value = bipush.getValue();
               int position = match[0].getPosition();
              
               addVariableValue(varMap, varIndex, value, position);
           }


           // Handles SIPUSH + ISTORE
           for (java.util.Iterator<?> it = finder.search("SIPUSH ISTORE"); it.hasNext();) {
               InstructionHandle[] match = (InstructionHandle[]) it.next();
               SIPUSH sipush = (SIPUSH) match[0].getInstruction();
               ISTORE istore = (ISTORE) match[1].getInstruction();


               int varIndex = istore.getIndex();
               int value = sipush.getValue();
               int position = match[0].getPosition();
              
               addVariableValue(varMap, varIndex, value, position);
           }


           // Handles small constants
           for (int i = 0; i <= 5; i++){
               String pattern = "ICONST_" + i + " ISTORE";
               for (java.util.Iterator<?> it = finder.search(pattern); it.hasNext();) {
                   InstructionHandle[] match = (InstructionHandle[]) it.next();
                   ISTORE istore = (ISTORE) match[1].getInstruction();


                   int varIndex = istore.getIndex();
                   int position = match[0].getPosition();
                  
                   addVariableValue(varMap, varIndex, i, position);
               }
           }


           // Handles -1
           for (java.util.Iterator<?> it = finder.search("ICONST_M1 ISTORE"); it.hasNext();) {
               InstructionHandle[] match = (InstructionHandle[]) it.next();
               ISTORE istore = (ISTORE) match[1].getInstruction();


               int varIndex = istore.getIndex();
               int position = match[0].getPosition();
              
               addVariableValue(varMap, varIndex, -1, position);
           }
       }


       // Handles the variable map
       private void addVariableValue(java.util.Map<Integer, java.util.List<Object[]>> varMap,
           int varIndex, Object value, int position) {
          
           // Creates an array if it is the first assignment
           if (!varMap.containsKey(varIndex)) {
               varMap.put(varIndex, new Java.util.ArrayList<>());
           }


           java.util.List<Object[]> values = varMap.get(varIndex);
           for (Object[] entry: values) {
               if (entry.length < 3) {
                   entry = java.util.Arrays.copyOf(entry, 3);
                   values.set(values.indexOf(entry), entry);
               } else if (entry[2] == null) {
                   entry[2] = position;
               }
           }


           values.add(new Object[]{value, position, null});
       }


       // Checks if the value of a variable is at that position or not
       private void findValueAtPosition(java.util.Map<Integer, java.util.List<Object[]>> varMap,
           int varIndex, int position) {


           if (!varMap.containsKey(varIndex)) {
               return null;
           }


           java.util.List<Object[]> values = varMap.get(varIndex);
           for (Object[] entry: values) {
               int startPos = (Integer) entry[1];
               Integer endPos = (entry.length > 2 && entry[2] != null) ? (Integer) entry[2] : Integer.MAX_VALUE;
               if (position >= startPos && position < endPos) {
                   return entry[0];
               }
           }


           return null;
       }


       // Decides which type of instruction should be used
       private Instruction constantInstruction(Object value, ConstantPoolGen cpgen) {
           // Handles integer
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
      
       // Replaces variable loads with constant assignments
       private boolean replaceVariableLoads(InstructionFinder finder, InstructionList ilist,
           ConstantPoolGen cpgen, java.util.Map<Integer, java.util.List<Object[]>> varMap) {
          
           boolean modified = false;
           for (java.util.Iterator<?> it = finder.search("ILOAD"); it.hasNext();) {
               InstructionHandle[] match = (InstructionHandle[]) it.next();
               ILOAD iload = (ILOAD) match[0].getInstruction();


               int varIndex = istore.getIndex();
               int position = match[0].getPosition();
               Object value = findValueAtPosition(varMap, varIndex, position);
              
               // Replaces the load
               if (value != null) {
                   Instruction replacedInstruction = constantInstruction(value, cpgen);
                   ilist.insert(match[0], constInst);
                   ilist.delete(match[0]);
                   modified = true;
               }
           }


           return modified;       
       }




       this.optimized = gen.getJavaClass();
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

