package compiler488.semantics;

import java.io.*;

import compiler488.symbol.SymbolTable;
import compiler488.ast.AST;
import compiler488.ast.decl.Declaration;
import compiler488.ast.decl.DeclarationPart;
import compiler488.ast.decl.MultiDeclarations;
import compiler488.ast.stmt.Program;
import compiler488.ast.stmt.Scope;
import compiler488.ast.stmt.Stmt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface PreProcessor {
    String target();
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface PostProcessor {
    String target();
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Action {
    int number();
}

/** Implement semantic analysis for compiler 488 
 *  @author Daniel Bloemendal
 */
public class Semantics {
    
    /** flag for tracing semantic analysis */
    private boolean traceSemantics = false;
    /** file sink for semantic analysis trace */
    private String traceFile = new String();
    private SymbolTable symbolTable;
    public FileWriter Tracer;
    public File f;

    /** Maps for processors and actions */
    private Map<String, Method>  preProcessorsMap;
    private Map<String, Method>  postProcessorsMap;
    private Map<Integer, Method> actionsMap;

    /** Analysis state */
    private AST        analysisTop;
    private Set<AST>   analysisGrey;
    private Stack<AST> analysisStack;
    
    //
    // Stack management
    //
    
    void discoverNode(AST obj) {
        analysisStack.push(obj);
    }
    
    //
    // Processor/action management 
    //
    
    void populateMappings() {
        Class<? extends Semantics> thisClass = this.getClass();
        for(Method method : thisClass.getDeclaredMethods()) {
            PreProcessor  preProcInfo  = method.getAnnotation(PreProcessor.class);
            PostProcessor postProcInfo = method.getAnnotation(PostProcessor.class);
            Action        actInfo      = method.getAnnotation(Action.class);
            if(preProcInfo  != null) preProcessorsMap.put(preProcInfo.target(), method);
            if(postProcInfo != null) postProcessorsMap.put(postProcInfo.target(), method);
            if(actInfo      != null) actionsMap.put(actInfo.number(), method);
        }
    }
    
    boolean invokePreProcessor(AST node) {
        return invokeProcessor(node, preProcessorsMap);
    }
    
    boolean invokePostProcessor(AST node) {
        return invokeProcessor(node, postProcessorsMap);
    }    
    
    boolean invokeProcessor(AST obj, Map<String, Method> map) {
        Method m = map.get(obj.getClass().getSimpleName());
        if(m == null) return false;
        analysisTop = obj;
        
        // Invoke the processor on object
        try {
            m.invoke(this, obj); return true;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace(); return false;
        }
    }
    
    void semanticAction(int actionNumber) {
        if( traceSemantics ){
            if(traceFile.length() > 0 ){
                //output trace to the file represented by traceFile
                try{
                    //open the file for writing and append to it
                    new File(traceFile);
                    Tracer = new FileWriter(traceFile, true);
                                  
                    Tracer.write("Sematics: S" + actionNumber + "\n");
                    //always be sure to close the file
                    Tracer.close();
                }
                catch (IOException e) {
                  System.out.println(traceFile + 
                    " could be opened/created.  It may be in use.");
                }
            }
            else{
                //output the trace to standard out.
                System.out.println("Sematics: S" + actionNumber );
            }
         
        }
        
        Method m = actionsMap.get(actionNumber);
        if(m == null) System.out.println("Unhandled Semantic Action: S" + actionNumber );
        else {
            // Invoke the semantic action. 
            try {
                m.invoke(this, analysisTop);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
            
            System.out.println("Semantic Action: S" + actionNumber  );
        }
    }    
        
    //
    // Semantic analysis life cycle
    //

    public Semantics () {
        symbolTable       = new SymbolTable();
        preProcessorsMap  = new HashMap<String, Method>();
        postProcessorsMap = new HashMap<String, Method>();
        actionsMap        = new HashMap<Integer, Method>();
        analysisGrey      = new HashSet<AST>();
        analysisStack     = new Stack<AST>();
    }    

    public void Initialize() {
        populateMappings();
    }

    public void Analyze(Program ast) {
        // Add the initial element to the stack
        analysisStack.add(ast);
        
        // Traverse the AST
        while(!analysisStack.empty()) {
            // Fetch top of the analysis stack
            AST top = analysisStack.peek();

            // If the object has not yet been seen
            if(!analysisGrey.contains(top)) {
                analysisGrey.add(top);
                invokePreProcessor(top);
            }
            // Finish processing object and pop it off of the stack
            else {
                invokePostProcessor(top);
                analysisStack.pop();
            }
        }
    }
    
    public void Finalize() {  
    }
    
    //
    // Helpers
    //
    
    void exploreScope(Scope scope) {
        // Add declarations and statements to the stack
        LinkedList<Stmt>          stmts = scope.getStatements().getList();
        LinkedList<Declaration>   decls = scope.getDeclarations().getList();
        ListIterator<Stmt>        si    = stmts.listIterator(stmts.size());
        ListIterator<Declaration> di    = decls.listIterator(decls.size());
        while(si.hasPrevious()) discoverNode(si.previous());
        while(di.hasPrevious()) discoverNode(di.previous());
    }
    
    //
    // Processors
    //
    
    @PreProcessor(target = "Program")
    void preProgram(Program program) {
        semanticAction(00); // S00: Start program scope.
        exploreScope(program);
    }
    
    @PostProcessor(target = "Program")
    void postProgram(Program program) {
        semanticAction(01); // S01: End program scope.    
    }
    
    @PreProcessor(target = "MultiDeclarations")
    void preMultiDeclarations(MultiDeclarations multiDecls) {
        for(DeclarationPart part : multiDecls.getElements().getList())
            discoverNode(part);
    }

    //
    // Actions
    //
    
    @Action(number = 00)
    void actionProgramStart(Program program) {
        symbolTable.scopeEnter(SymbolTable.ScopeType.Program);
    }
    
    @Action(number = 01)
    void actionProgramEnd(Program program) {
        symbolTable.scopeExit();
    }    
}
