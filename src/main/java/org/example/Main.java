package org.example;


import com.ibm.wala.cast.java.client.impl.ZeroOneCFABuilderFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.util.CallGraphSearchUtil;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.SlicerUtil;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.CancelException;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;


public class Main {
    public static void main(String[] args) {
        learnSlice();
    }

    public static void learnSlice() {
        try {
            AnalysisScope scope = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(
                    "src/main/resources/com.ibm.wala.core.testdata_1.0.0.jar", new File("exclusion.txt")
            );
            IClassHierarchy cha = ClassHierarchyFactory.make(scope);
            // debug for all classes will be analyzed.
            for (IClass iClass : cha
            ) {
                if (iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
                    System.out.println(iClass);
                }
            }
            // generate options

            // generate entry point
            // Lslice represent for package
            Iterable<Entrypoint> entrypoints =
                    com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lslice/Slice1");
            AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

            // build call graph
            CallGraphBuilder<InstanceKey> builder = new ZeroOneCFABuilderFactory().make(new AnalysisOptions(scope, entrypoints), new AnalysisCacheImpl(), cha);
            CallGraph cg = builder.makeCallGraph(options, null);
            CGNode main = CallGraphSearchUtil.findMainMethod(cg);

            Statement s = SlicerUtil.findCallTo(main, "println");
            System.out.println("Statement : " + s);

            // calcluate the param of println function
            PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
            Collection<Statement> statementCollection = Slicer.computeBackwardSlice(
                    s, cg, pointerAnalysis, Slicer.DataDependenceOptions.FULL, Slicer.ControlDependenceOptions.NONE
            );
             SlicerUtil.dumpSlice(statementCollection);
            int i = 0;
            for (Statement statement :statementCollection
                    ) {
                System.out.println(statement.getNode());
            }


        } catch (IOException | ClassHierarchyException e) {
            throw new RuntimeException(e);
        } catch (CallGraphBuilderCancelException e) {
            throw new RuntimeException(e);
        } catch (CancelException e) {
            throw new RuntimeException(e);
        }
    }

    public static void learnIR() {
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
                            // wala given some special methods to visit ir
                            ir.iterateAllInstructions(); //

                            for (ISSABasicBlock bb : ir.getControlFlowGraph()
                            ) {
//                                System.out.println(bb);
                                // get the ir
                                for (int i = bb.getFirstInstructionIndex(); i <= bb.getLastInstructionIndex(); i++) {
                                    SSAInstruction irElem = ir.getInstructions()[i];
                                    if (irElem != null) {
                                        // why some instructions is empty ???
                                        // I can see some details about addncase
                                        // System.out.println(irElem);
                                        System.out.println(i + "    " + irElem.toString(ir.getSymbolTable()));
                                    }
                                }
                            }
                            for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext(); ) {
                                SSAInstruction irElem = it.next();
                                if (irElem != null) {
                                    // why some instructions is empty ???
                                    // I can see some details about addncase
                                    // System.out.println(irElem);
                                    //     add(c);
                                    // invokevirtual< add(I)V> v1 v2
                                    // non-static method v1-> this.;
                                    System.out.println(irElem.toString(ir.getSymbolTable()));
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
//            System.out.println("Construction Finished");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassHierarchyException e) {
            throw new RuntimeException(e);
        }

    }
}