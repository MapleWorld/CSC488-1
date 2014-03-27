package compiler488.codegen;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import compiler488.ast.AST;
import compiler488.ast.decl.ArrayDeclPart;
import compiler488.ast.decl.Declaration;
import compiler488.ast.decl.DeclarationPart;
import compiler488.ast.decl.MultiDeclarations;
import compiler488.ast.decl.RoutineDecl;
import compiler488.ast.decl.ScalarDecl;
import compiler488.ast.decl.ScalarDeclPart;
import compiler488.ast.stmt.Scope;

/**
 * Stack frame for major scopes
 * 
 * @author Daniel Bloemendal
 */
public class Frame extends Visitor {
    //
    // Static configuration
    //
    
    public final static Short MACHINE_WORD_SIZE = 1;
    
    //
    // Processors
    //
    
    @PreProcessor(target="Scope")
    void preScope(Scope scope){
        // Enter a new frame
        enterFrame(scope);
        // Exclude new routines
        for(Declaration decl : scope.getDeclarations().getList())
            if(decl instanceof RoutineDecl) exclude(decl);
    }   
    @PostProcessor(target="Scope")
    void postScope(Scope scope){
        // Exit a frame
        exitFrame();
    }    

    @PreProcessor(target="MultiDeclarations")
    void preMultiDeclarations(MultiDeclarations multiDeclarations) {
        for(DeclarationPart part : multiDeclarations.getElements().getList())
            currentFrame().addVariable(part);
    }
    
    //
    // Frame management
    //
    
    MinorFrame currentFrame() {
        return frameCurrent;
    }
    
    void enterFrame(Scope scope) {
        MinorFrame frameNew = new MinorFrame(frameCurrent);
        if(frameRoot == null) frameRoot = frameNew;
        frameMap.put(scope, frameNew);
        frameCurrent = frameNew;
    }
    
    void exitFrame() {
        // Compute frame size
        MinorFrame current = currentFrame(); 
        List<MinorFrame> children = current.getChildren();
        if(!children.isEmpty()) {
            MinorFrame max = Collections.max(children);
            short size = (short)(current.getSize() + max.getSize());
            current.setSize(size);
        }

        // Switch to parent frame
        frameCurrent = frameCurrent.getParent();
    }
    
    //
    // Helpers
    //
    
    void addArguments(Scope scope) {
        if(!(scope.getParent() instanceof RoutineDecl)) return;
        RoutineDecl routine = (RoutineDecl) scope.getParent();
        List<ScalarDecl> parameterList = routine.getParameters().getList();
        for(int i = 0; i < parameterList.size(); i++)
            frameArgs.put(parameterList.get(i).getIdent().getId(), i);
    }
    
    //
    // Frame interface
    //
        
    public Frame(Scope root, short lexicalLevel) {
        // Initialize members
        frameMap = new HashMap<AST, MinorFrame>();
        frameCurrent = null;
        frameLevel = lexicalLevel;
        // Add arguments
        addArguments(root);
        // Traverse the AST
        visit(root);
    }
    
    public Short getOffset(Scope scope, String identifier) {
        // Try the scope first
        MinorFrame frame = frameMap.get(scope);
        if(frame == null) return null;
        Short offset = frame.getOffset(identifier);
        if(offset != null) return offset;
        
        // Otherwise try the arguments
        return null;
    }
    
    public short getLevel() {
        return frameLevel;
    }
    
    public short getSize() {
        return (frameRoot != null) ? frameRoot.getSize() : 0; 
    }
    
    // Internal members
    private Map<String, Integer> frameArgs;    
    private Map<AST, MinorFrame> frameMap;
    private MinorFrame           frameRoot;
    private MinorFrame           frameCurrent;    
    private short                frameLevel;
}

//
// Stack frame for minor scopes
//

class MinorFrame implements Comparable<MinorFrame> {
    MinorFrame(MinorFrame parent) {
        // Instantiate internals
        nodeMap = new HashMap<String, AST>();
        offsetMap = new HashMap<String, Short>();
        frameChildren = new LinkedList<MinorFrame>();
        frameParent = parent;        
        frameSize = 0;
        // Add this frame to the parent
        if(parent != null) parent.addChild(this);
    }
    
    public AST getNode(String identifier) {
        return nodeMap.get(identifier);
    }
    
    public Short getOffset(String identifier) {
        return offsetMap.get(identifier);
    }
    
    public void addVariable(DeclarationPart decl) {
        if(decl instanceof ScalarDeclPart)
            addVariable((ScalarDeclPart) decl);
        else if(decl instanceof ArrayDeclPart)
            addVariable((ArrayDeclPart) decl);
    }
    
    public void addVariable(ScalarDeclPart scalarDecl) {
        String ident = scalarDecl.getIdent().getId();
        nodeMap.put(ident, scalarDecl);
        offsetMap.put(ident, frameSize);
        frameSize += Frame.MACHINE_WORD_SIZE;
    }
    
    public void addVariable(ArrayDeclPart arrayDecl) {
        String ident = arrayDecl.getIdent().getId();
        nodeMap.put(ident, arrayDecl);
        offsetMap.put(ident, frameSize);
        frameSize += arrayDecl.getSize() * Frame.MACHINE_WORD_SIZE;
    }
        
    // Getters/setters
    public MinorFrame getParent() { return frameParent; }
    public List<MinorFrame> getChildren() { return Collections.unmodifiableList(frameChildren); }
    public short getSize() { return frameSize; }
    public void setSize(short size) { frameSize = size; }
    
    // Compare support
    public int compareTo(MinorFrame other) { return Short.compare(this.frameSize, other.frameSize); }    
    
    //
    // Internal routines & members
    //
    
    private void addChild(MinorFrame child) {
        frameChildren.add(child);
    }
            
    private Map<String, AST>   nodeMap;
    private Map<String, Short> offsetMap;
    private MinorFrame         frameParent;
    private List<MinorFrame>   frameChildren;
    private short              frameSize;
}