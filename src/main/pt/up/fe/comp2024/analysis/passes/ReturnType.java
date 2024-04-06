package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;


/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ReturnType extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitReturnStmt(JmmNode node, SymbolTable table){
        if(node.getChild(0).get("type").equals(table.getReturnType(currentMethod).getName()) && node.getChild(0).get("isArray").equals(String.valueOf(table.getReturnType(currentMethod).isArray()))){
            return null;
        } else {
            String message = "It doesnt match the method " + currentMethod + " Expected: " + table.getReturnType(currentMethod) + " found: " + node.getChild(0).get("type");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }
}