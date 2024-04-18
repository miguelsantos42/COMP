package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisPosVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;
import java.util.Optional;

public class IntLit extends AnalysisPosVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.INTEGER_LITERAL, this::visitIntLit);
        addVisit("BooleanLiteral", this::visitBoolLit);
        addVisit("ThisExpr", this::visitThisExpr);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("LogicalExpr", this::visitLogicalExpr);
        addVisit("NegationExpr", this::visitNegationExpr);
        addVisit("ParenthesisExpr", this::visitParenthesisExpr);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ArrayExpr", this::visitArrayExpr);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);
        addVisit("ArrayLengthExpr", this::visitArrayLengthExpr);
        addVisit("ArrayAssignStmt", this::visitArrayAssignStmt);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit("NewObjectExpr", this::visitNewObjectExpr);
        addVisit("MethodClassCallExpr", this::visitMethodClassCallExpr);
        addVisit("NewArrayExpr", this::visitNewArrayExpr);
    }

    private Void visitBoolLit(JmmNode node, SymbolTable table){
        node.put("type", "boolean");
        node.put("isArray", "false");
        return null;
    }

    private Void visitIntLit(JmmNode node, SymbolTable table){
        node.put("type","int");
        node.put("isArray", "false");
        return null;
    }

    private Void visitThisExpr(JmmNode node, SymbolTable table){
        node.put("type", table.getClassName());
        node.put("isArray", "false");
        return null;
    }

    private Void visitBinaryExpr(JmmNode node, SymbolTable table){
        node.put("type", "int");
        node.put("isArray", "false");

        String operator = node.get("op");
        if(!node.getChild(0).hasAttribute("type") || !node.getChild(1).hasAttribute("type")){
            return null;
        }
        else if(node.getChild(0).get("type").equals("null") || node.getChild(1).get("type").equals("null")) {
            return null;
        }
        else if(node.getChild(0).get("type").equals("int") && node.getChild( 1).get("type").equals("int") && node.getChild(0).get("isArray").equals("false") && node.getChild(1).get("isArray").equals("false")){
            return null;
        }
        else {
            String message = "Binary operation " + operator + " is invalid between types that are not integer.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private Void visitLogicalExpr(JmmNode node, SymbolTable table){
        node.put("type", "boolean");
        node.put("isArray", "false");
        if(node.get("op").equals("&&")){
            if(node.getChild(0).get("type").equals("boolean") && node.getChild(1).get("type").equals("boolean")){
                return null;
            }else {
                String message = "Invalid AND operation between types!";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null));
            }
        }
        else if(node.get("op").equals("<")){
            if (node.getChild(0).get("type").equals("int") && node.getChild(1).get("type").equals("int") && node.getChild(0).get("isArray").equals("false") && node.getChild(1).get("isArray").equals("false")) {
                return null;
            }else {
                String message = "Invalid LT operation between types!";
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
        node.put("isArray", "false");
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
        node.put("isArray", node.getChild(0).get("isArray"));
        return null;
    }

    private Void visitArrayExpr(JmmNode node, SymbolTable table){
        node.put("type", "int");
        node.put("isArray", "true");
        for(JmmNode number : node.getChildren()){
            if(!number.get("type").equals("int") || number.get("isArray").equals("true")){
                String message = "Array elements must be of type int";
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

    private Void visitArrayAccessExpr(JmmNode node, SymbolTable table){
        node.put("type", "int");
        node.put("isArray", "false");
        if(!node.getChild(0).get("isArray").equals("true")){
            String message = "Array access index must be type int[]";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        if(!node.getChild(1).get("type").equals("int") || node.getChild(1).get("isArray").equals("true")){
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

    private Void visitArrayAssignStmt(JmmNode node, SymbolTable table){
        if (!node.get("isArray").equals("true")){
            String message = "Variable must be of type array to be accessed";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        if(!node.getChild(0).get("type").equals("int") || node.getChild(0).get("isArray").equals("true")){
            String message = "Array assignment must be of type int[]";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        if(!node.getChild(1).get("type").equals("int") || node.getChild(1).get("isArray").equals("true")){
            String message = "Array assignment must be of type int";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        return null;
    }

    private  Void visitArrayLengthExpr(JmmNode node, SymbolTable table){
        if(!node.getChild(0).get("isArray").equals("true")){
            String message = "Length can not be used on type" + node.getChild(0).get("type");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        node.put("type","int");
        node.put("isArray", "false");
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

    private Void visitNewArrayExpr(JmmNode node, SymbolTable table){
        if(!node.getChild(0).get("type").equals("int") || node.getChild(0).get("isArray").equals("true")){
            String message = "Array size must be of type int";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
        }
        node.put("type", "int");
        node.put("isArray", "true");
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

    private Void visitAssignStmt(JmmNode node, SymbolTable table){
        // in case of a static method call from an import
        if(node.getChild(0).getKind().equals("MethodClassCallExpr")){
            var child = node.getChild(0).getChild(0);
            if((child.getKind().equals("VarRefExpr") &&
                    (Objects.equals(child.get("name"), child.get("type"))
                    || table.getImports().contains(child.get("type"))))
                || (child.getKind().equals("ParenthesisExpr")
                    && child.getChild(0).getKind().equals("NewObjectExpr"))){
                return null;
            }
        }

        String childType = node.getChild(0).get("type");
        if( (node.get("type").equals(childType) && node.get("isArray").equals(node.getChild(0).get("isArray")))
                || (node.get("type").equals(table.getSuper()) && childType.equals(table.getClassName()))){
            return null;
        } else {
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


    private Void visitNewObjectExpr(JmmNode node, SymbolTable table){
        if(table.getImports().stream().noneMatch(name -> name.equals(node.get("name"))) && !(node.get("name").equals(table.getClassName()))){
            var message = String.format("Class '%s' is not defined", node);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }
        node.put("type", node.get("name"));
        node.put("isArray", "false");
        return null;
    }

    private Void visitMethodClassCallExpr(JmmNode node, SymbolTable table){
        var nodeName = node.get("name");

        Optional<String> method = table.getMethods().stream().filter(param->param.equals(nodeName)).findFirst();
        if(method.isEmpty()){
            return null;
        }

        var child = node.getChild(0);

        // in case of a static method call from an import
        if((child.getKind().equals("VarRefExpr") &&
                (Objects.equals(child.get("name"), child.get("type"))
                || table.getImports().contains(child.get("type"))
                ))
            || (child.getKind().equals("ParenthesisExpr")
                && child.getChild(0).getKind().equals("NewObjectExpr"))){
            return null;
        }

        //todo there is a possible error when last parameter is of type int[] instead of int...
        if((table.getParameters(nodeName).isEmpty() && node.getChildren().size() == 1)){
            return null;
        } else if (table.getParameters(nodeName).isEmpty() || node.getChildren().size() <= 1){
            String message = "Wrong number of parameters";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null));
            return null;
        }
        boolean hasVarArg = table.getParameters(nodeName).get(table.getParameters(nodeName).size()-1).getType().isArray();
        if((node.getNumChildren() != table.getParameters(nodeName).size()+1)){
            if(hasVarArg){
                if(node.getNumChildren() > table.getParameters(nodeName).size()+1){
                    for (int i = 1; i < node.getNumChildren(); i++) {
                        if(i < table.getParameters(nodeName).size()){
                            if(node.getChild(i).get("type").equals(table.getParameters(nodeName).get(i - 1).getType().getName()) && node.getChild(i).get("isArray").equals(String.valueOf(table.getParameters(nodeName).get(i - 1).getType().isArray()))){
                            }else {
                                String message = "It doesnt match the parameter " + i + " in method " + node.get("name") + ". Expected " + table.getParameters(node.get("name")).get(i-1).getType().getName() + ". Found " + node.getChild(i).get("type");
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        NodeUtils.getLine(node),
                                        NodeUtils.getColumn(node),
                                        message,
                                        null));
                                break;
                            }
                        }else {
                            if(node.getChild(i).get("type").equals(table.getParameters(nodeName).get(table.getParameters(nodeName).size()-1).getType().getName()) && node.getChild(i).get("isArray").equals("false")){
                            }else {
                                String message = "It doesnt match the parameter " + i + " in method " + node.get("name") + ". Expected " + table.getParameters(nodeName).get(table.getParameters(nodeName).size()-1).getType().getName() + ". Found " + node.getChild(i).get("type");
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
                } else {
                    String message = "Wrong number of parameters";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            message,
                            null));
                    return null;
                }
            } else {
                String message = "Wrong number of parameters";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null));
                return null;
            }
        } else{
            if(hasVarArg){
                for(int i = 1; i < node.getNumChildren(); i++){
                    if(i == node.getNumChildren()-1 && node.getChild(i).get("type").equals("int")){
                        continue;
                    }
                    if(node.getChild(i).get("type").equals(table.getParameters(nodeName).get(i - 1).getType().getName()) && node.getChild(i).get("isArray").equals(String.valueOf(table.getParameters(nodeName).get(i - 1).getType().isArray()))){
                    } else {
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
            }else {
                for(int i = 1; i < node.getNumChildren(); i++){
                    if(node.getChild(i).get("type").equals(table.getParameters(nodeName).get(i - 1).getType().getName()) && node.getChild(i).get("isArray").equals(String.valueOf(table.getParameters(nodeName).get(i - 1).getType().isArray()))){
                    } else {
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
        }
        return null;
    }
}