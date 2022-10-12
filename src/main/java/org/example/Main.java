package org.example;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;

import java.io.File;
import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        buildCGFromJar();
    }

    public static void buildCGFromJar() {
        try {
            AnalysisScope scope = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(
                    "src/main/resources/input/JLex.jar", new File("exclusion.txt")
            );
            IClassHierarchy cha = ClassHierarchyFactory.make(scope);

            AnalysisOptions options = new AnalysisOptions();
            options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
            IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());

            // Build the IR and cache it.

            for (IClass c : cha
            ) {
                if (c.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
                    for (IMethod m : c.getDeclaredMethods()) {
                        if (m.equals(cha.resolveMethod(c, Selector.make("addncase(C)V")))) {
//                            System.out.println(m);
                            IR ir = cache.getIR(m, Everywhere.EVERYWHERE);
//                            System.out.println(ir);
                            for (ISSABasicBlock bb : ir.getControlFlowGraph()
                            ) {
//                                System.out.println(bb);
                                // get the ir
                                for (int i = bb.getFirstInstructionIndex(); i <= bb.getLastInstructionIndex(); i++) {
                                    SSAInstruction irElem = ir.getInstructions()[i];
                                    if (irElem != null) {
                                        // u can see some details about addncase
                                        System.out.println(irElem.toString(ir.getSymbolTable()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
//            Iterable<Entrypoint> e = Util.makeMainEntrypoints(scope, cha);
//            AnalysisOptions o = new AnalysisOptions(scope, e);
//            CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(
//                    Language.JAVA, o, new AnalysisCacheImpl(), cha, scope
//            );
//            CallGraph cg = builder.makeCallGraph(o, null);
//            // 如上过程能够顺利建立一个jar包的cg，后续API在后续分析中使用。
//            System.out.println("Construction Finished");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassHierarchyException e) {
            throw new RuntimeException(e);
        }

    }
}