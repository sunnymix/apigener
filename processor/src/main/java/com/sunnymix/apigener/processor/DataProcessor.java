package com.sunnymix.apigener.processor;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sunnymix.apigener.annotation.Data;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * @author Sunny
 */
@SupportedAnnotationTypes("com.sunnymix.apigener.annotation.Data")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DataProcessor extends AbstractProcessor {

    private Messager messager;
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
        this.trees = JavacTrees.instance(env);
        Context context = ((JavacProcessingEnvironment) env).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Set<? extends Element> elements = env.getElementsAnnotatedWith(Data.class);
        elements.forEach(element -> {
            JCTree elementTree = trees.getTree(element);
            elementTree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl classDecl) {
                    List<JCTree.JCVariableDecl> varDecls = List.nil();

                    for (JCTree memberTree : classDecl.defs) {
                        if (memberTree.getKind().equals(Tree.Kind.VARIABLE)) {
                            JCTree.JCVariableDecl varDecl = (JCTree.JCVariableDecl) memberTree;
                            varDecls = varDecls.append(varDecl);
                        }
                    }

                    varDecls.forEach(varDecl -> {
                        messager.printMessage(Diagnostic.Kind.NOTE, "Visit: " + varDecl.getName());
                        classDecl.defs = classDecl.defs.prepend(_makeGetterMethodDecl(varDecl));
                    });
                }
            });
        });
        return true;
    }

    private JCTree.JCMethodDecl _makeGetterMethodDecl(JCTree.JCVariableDecl varDecl) {
        JCTree.JCMethodDecl methodDecl;
        Name varName = varDecl.getName();
        JCTree.JCReturn returnStatement = treeMaker.Return(
            treeMaker.Select(treeMaker.Ident(names.fromString("this")), varName)
        );
        JCTree.JCBlock body = treeMaker.Block(0, List.of(returnStatement));
        methodDecl = treeMaker.MethodDef(
            treeMaker.Modifiers(Flags.PUBLIC),
            _makeGetterMethodName(varName),
            varDecl.vartype,
            List.nil(),
            List.nil(),
            List.nil(),
            body,
            null);
        return methodDecl;
    }

    private Name _makeGetterMethodName(Name varName) {
        String varNameStr = varName.toString();
        String getterNameStr = "get" + varNameStr.substring(0, 1).toUpperCase() + varNameStr.substring(1);
        return names.fromString(getterNameStr);
    }

}
