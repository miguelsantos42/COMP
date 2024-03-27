package pt.up.fe.comp2024.analysis.passes;

import org.antlr.v4.runtime.atn.SemanticContext;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisPosVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class IntLit extends AnalysisPosVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.INTEGER_LITERAL, this::visitIntLit);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("LogicalExpr", this::visitLogicalExpr);
        addVisit("BooleanLiteral", this::visitBoolLit);
    }

    private Void visitIntLit(JmmNode node, SymbolTable table){
        node.put("type","int");
        return null;
    }

    private Void visitBinaryExpr(JmmNode node, SymbolTable table){
        node.put("type", "int");
        return null;
    }

    private Void visitLogicalExpr(JmmNode node, SymbolTable table){
        node.put("type", "boolean");
        if(node.get("op").equals("&&") && (!node.getChild(0).get("type").equals("boolean")||(!node.getChild(1).get("type").equals("boolean")))){
            String message = "Invalid AND operation between types!";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        else if(node.get("op").equals("<") && (!node.getChild(0).get("type").equals("int")||(!node.getChild(1).get("type").equals("int")))){
            String message = "Invalid LT operation between types!";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private Void visitBoolLit(JmmNode node, SymbolTable table){
        node.put("type", "boolean");
        return null;
    }

}
