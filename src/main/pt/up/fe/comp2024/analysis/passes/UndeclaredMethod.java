package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Optional;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredMethod extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit("FunctionParameters", this::checkFunctionParameters);
        addVisit(Kind.METHOD_DECL,this::checkMethodtype);
    }

    private Void visitMethodCallExpr(JmmNode node, SymbolTable table) {
        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var nodeName = node.get("name");
        Optional<String> method = table.getMethods().stream().filter(param->param.equals(nodeName)).findFirst();
        if(method.isPresent()){
            node.put("type",table.getReturnType(nodeName).getName());
            return null;
        }

        // Create error report
        var message = String.format("Method '%s' does not exist.", nodeName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );

        return null;
    }

    private Void checkFunctionParameters(JmmNode node, SymbolTable table){
        for(int i = 0; i < node.getNumChildren(); i++){
            if(i != node.getNumChildren() - 1 && node.getChild(i).get("name").equals("int...")){
                String message = "Vararg must be the last parameter";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

    private Void checkMethodtype(JmmNode node, SymbolTable table){
        if(node.getChild(0).get("name").equals("int...")){
            String message = "Method can not be declared with vararg type!";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }
        return null;
    }

}
