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
import pt.up.fe.comp2024.utils.Comparator;

public class IntLit extends AnalysisPosVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.INTEGER_LITERAL, this::visitIntLit);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("LogicalExpr", this::visitLogicalExpr);
        addVisit("BooleanLiteral", this::visitBoolLit);
        addVisit("NegationExpr", this::visitNegationExpr);
        addVisit("ParenthesisExpr", this::visitParenthesisExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ArrayExpr", this::visitArrayExpr);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);
        addVisit("ArrayLengthExpr", this::visitArrayLengthExpr);
        addVisit("ThisExpr", this::visitExpr);
    }

    private Void visitExpr(JmmNode node, SymbolTable table){
        node.put("type", table.getClassName());
        return null;
    }


    private Void visitArrayAccessExpr(JmmNode node, SymbolTable table){

        node.put("type", "int");
        if(!node.getChild(0).get("type").equals("int[]") && !node.getChild(0).get("type").equals("int...")){
            String message = "Array access index must be type int[]";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        if(!node.getChild(1).get("type").equals("int")){
            String message = "Array access index must be type int";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }

        return null;
    }

    private Void visitArrayExpr(JmmNode node, SymbolTable table){

        node.put("type", "int[]");
        for(JmmNode number : node.getChildren()){
            if(!number.get("type").equals("int")){
                String message = "Array size must be of type int";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null));
            }
        }
        return null;
    }

    private  Void visitArrayLengthExpr(JmmNode node, SymbolTable table){
        if(!node.getChild(0).get("type").equals("int[]") && !node.getChild(0).get("type").equals("int...")){
            String message = "Length can not be used on type" + node.getChild(0).get("type");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private Void visitIntLit(JmmNode node, SymbolTable table){
        node.put("type","int");
        return null;
    }

    private Void visitBinaryExpr(JmmNode node, SymbolTable table){
        node.put("type", "int");

        String operator = node.get("op");

        if(operator.equals("+")){
            if((!node.getChild(0).get("type").equals("int") || !node.getChild( 1).get("type").equals("int"))&&(!node.getChild(0).get("type").equals("string") || !node.getChild(1).get("type").equals("string"))){
                String message = "Binary operation '+' is invalid between types that are not integer or string.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null));
            }
        }

        else if(operator.equals("-") || operator.equals("*") || operator.equals("/")){
            if(!node.getChild(0).get("type").equals("int") || !node.getChild( 1).get("type").equals("int")){
                String message = "Binary operation " + operator + " is invalid between types that are not integer.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null));
            }
        }
        return null;
    }

    private Void visitNegationExpr(JmmNode node, SymbolTable table){
        node.put("type", "boolean");
        if(!node.getChild(0).get("type").equals("boolean")){
            String message = "Negation Operation '!' is invalid on a not boolean type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private Void visitParenthesisExpr(JmmNode node, SymbolTable table){
        String childType = node.getChild(0).get("type");
        node.put("type", childType);
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

    private Void visitAssignStmt(JmmNode node, SymbolTable table){
        if(!node.get("type").equals(node.getChild(0).get("type")) && !(node.get("type").equals(table.getSuper()) && node.getChild(0).get("type").equals(table.getClassName()))){
            String message = "Invalid = operation between types!";
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

    private Void visitIfStmt(JmmNode node, SymbolTable table){
        if(!node.getChild(0).get("type").equals("boolean")){
            String message = "This If statement is not boolean type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private Void visitWhileStmt(JmmNode node, SymbolTable table){
        if(!node.getChild(0).get("type").equals("boolean")){
            String message = "The While statement is not boolean type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private Void visitMethodCallExpr(JmmNode node, SymbolTable table){
        String nodeName = node.get("name");
        boolean hasVarArg = table.getParameters(nodeName).get(table.getParameters(nodeName).size()-1).getType().getName().equals("int...");
        if((node.getNumChildren() != table.getParameters(nodeName).size()+1)){
            if(hasVarArg){
                for(int i = 1; i < node.getNumChildren(); i++){
                    if(i >= table.getParameters(nodeName).size()){
                        if(!node.getChild(i).get("type").equals("int")){
                            String message = "Type " + node.getChild(i).get("type") + " incompetible with var arg of type int";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(node),
                                    NodeUtils.getColumn(node),
                                    message,
                                    null));
                            return null;
                        }
                    }
                    else if((!node.getChild(i).get("type").equals(table.getParameters(node.get("name")).get(i-1).getType().getName())) && !(table.getParameters(node.get("name")).get(i-1).getType().getName()).equals(table.getSuper()) && node.getChild(i).get("type").equals(table.getClassName())){
                        String message = "It doesnt match the parameter " + i + " in method " + node.get("name") + ". Expected " + table.getParameters(node.get("name")).get(i-1).getType().getName() + ". Found " + node.getChild(i).get("type");
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(node),
                                NodeUtils.getColumn(node),
                                message,
                                null));
                        break;
                    }
                }
            }
            else{
                String message = "Wrong number of parameters";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null));}
                return null;
            }
        else{
            for(int i = 1; i < node.getNumChildren(); i++){
                if((!node.getChild(i).get("type").equals(table.getParameters(node.get("name")).get(i-1).getType().getName())) && !(table.getParameters(node.get("name")).get(i-1).getType().getName().equals("int...") && (node.getChild(i).get("type").equals("int[]") || node.getChild(i).get("type").equals("int"))) && !(table.getParameters(node.get("name")).get(i-1).getType().getName()).equals(table.getSuper()) && node.getChild(i).get("type").equals(table.getClassName())){
                    String message = "It doesnt match the parameter " + i + " in method " + node.get("name") + ". Expected " + table.getParameters(node.get("name")).get(i-1).getType().getName() + ". Found " + node.getChild(i).get("type");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            message,
                            null));
                    break;
                }
            }
        }
        return null;
    }

}