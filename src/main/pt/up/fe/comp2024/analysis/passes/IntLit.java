package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class IntLit extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.INTEGER_LITERAL, this::visitIntLit);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.TYPE, this::getType);
    }

    private Void visitIntLit(JmmNode node, SymbolTable table){
        node.put("type","int");
        return null;
    }

    private Void visitBinaryExpr(JmmNode node, SymbolTable table){
        JmmNode left =  node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String lefttype = getType(left, table);
        String righttype = getType(right, table);

        node.put("type", "int");

        return null;
    }

    private String getType(JmmNode node, SymbolTable table) {
        return node.get("type");
    }
}
