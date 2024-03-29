package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class StaticMethod extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("ThisExpr", this::visitThisExpr);
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMainMethodDeclaration(JmmNode method, SymbolTable table){
        currentMethod = "main";
        return null;
    }

    private Void visitThisExpr(JmmNode node, SymbolTable table){
        if(currentMethod.equals("main")){
            String message = "Class method can not be accessed from static method";
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
