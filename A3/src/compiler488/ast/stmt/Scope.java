package compiler488.ast.stmt;

import java.io.PrintStream;
import java.util.List;
import java.util.Vector;

import compiler488.ast.AST;
import compiler488.ast.ASTList;
import compiler488.ast.Indentable;
import compiler488.ast.SourceLoc;
import compiler488.ast.decl.Declaration;

/**
 * Represents the declarations and instructions of a scope construct.
 */
public class Scope extends Stmt {
    private ASTList<Declaration> declarations; // The declarations at the top.

    private ASTList<Stmt> statements; // The statements to execute.

    public Scope(SourceLoc loc) {
        super(loc);

        declarations = new ASTList<Declaration>();
        statements = new ASTList<Stmt>();
        declarations.setParent(this);
        statements.setParent(this);
    }

    public Scope(ASTList<Declaration> decls, ASTList<Stmt> stmts, SourceLoc loc) {
        super(loc);

        if (decls == null) {
            declarations = new ASTList<Declaration>();
        } else {
            declarations = decls;
        }

        if (stmts == null) {
            statements = new ASTList<Stmt>();
        } else {
            statements = stmts;
        }

        declarations.setParent(this);
        statements.setParent(this);
    }

    /**
     * Print a description of the <b>scope</b> construct.
     *
     * @param out
     *            Where to print the description.
     * @param depth
     *            How much indentation to use while printing.
     */
    @Override
    public void printOn(PrintStream out, int depth) {
        Indentable.printIndentOnLn(out, depth, "{");
        Indentable.printIndentOnLn(out, depth, "declarations");

        declarations.printOnSeperateLines(out, depth + 1);

        Indentable.printIndentOnLn(out, depth, "statements");

        statements.printOnSeperateLines(out, depth + 1);

        Indentable.printIndentOnLn(out, depth, "}");
    }

    public ASTList<Declaration> getDeclarations() {
        return declarations;
    }

    public ASTList<Stmt> getStatements() {
        return statements;
    }

    public List<AST> getChildren() {
        Vector<AST> children = new Vector<AST>();

        children.add(declarations);
        children.add(statements);

        return children;
    }
}

