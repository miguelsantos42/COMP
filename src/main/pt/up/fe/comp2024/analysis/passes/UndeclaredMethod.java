package pt.up.fe.comp2024.analysis.passes;


import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredMethod extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit("MethodClassCallExpr", this::visitMethodClassCallExpr);
        addVisit("FunctionParameters", this::checkFunctionParameters);
        addVisit(Kind.METHOD_DECL,this::checkMethodtype);
    }

    //todo add fluid type to method parameters
    private Void visitMethodClassCallExpr(JmmNode node, SymbolTable table) {
        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var nodeName = node.get("name");
        Optional<String> method = table.getMethods().stream().filter(param->param.equals(nodeName)).findFirst();
        if(method.isPresent()){
            node.put("type",table.getReturnType(nodeName).getName());
            node.put("isArray",String.valueOf(table.getReturnType(nodeName).isArray()));
            return null;
        }
        //todo: missing valid function retrun for retrun, and parameters; alse e carefull with assigns
        else if((!table.getSuper().equals("not extended") && node.getChild(0).getKind().equals("ThisExpr")) ||
                (!node.getChild(0).getKind().equals("ThisExpr") && (!node.getChild(0).get("type").equals(table.getClassName()) || !table.getSuper().equals("not extended")))){
            if(node.getParent().getKind().equals("LogicalExpr") && node.getParent().get("op").equals("&&")){
                node.put("type", "boolean");
                node.put("isArray", "false");
            }
            else if(node.getParent().getKind().equals("AssignStmt")){
                node.put("type", node.getParent().get("type"));
                node.put("isArray", "false");
            }
            else if(node.getParent().getKind().equals("IfStmt") || node.getParent().getKind().equals("WhileStmt")){
                node.put("type", "boolean");
                node.put("isArray", "false");
            }
            else if(node.getParent().getKind().equals("ArrayLengthExpr") || (node.getParent().getKind().equals("ArrayAccessExpr") && (node.getIndexOfSelf() == 0))){
                node.put("type", "int");
                node.put("isArray", "true");
            }
            else{
                node.put("type", "int");
                node.put("isArray", "false");
            }
            return null;
        }

        //Create error report
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
        if(node.getChild(0).getKind().equals("MethodCodeBlockWithoutReturn")){
            return null;
        }
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
