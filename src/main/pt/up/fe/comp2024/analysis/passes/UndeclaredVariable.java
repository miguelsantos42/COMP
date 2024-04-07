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
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitVarRefExpr);
        addVisit("IDType", this::visitIDType);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");
        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a field, return
        Optional<Symbol> variable = table.getFields().stream()
                .filter(param -> param.getName().equals(varRefName))
                .findFirst();

        if (variable.isPresent()) {
            Optional<Symbol> finalVariable = variable;
            if(varRefExpr.getParent().getKind().equals("AssignStmt") && table.getImports().stream().anyMatch(value -> value.equals(finalVariable.get().getType().getName()))){
                varRefExpr.put("type", varRefExpr.getParent().get("type"));
                varRefExpr.put("isArray", String.valueOf(variable.get().getType().isArray()));
            }else{
                varRefExpr.put("type", variable.get().getType().getName());
                varRefExpr.put("isArray", String.valueOf(variable.get().getType().isArray()));
            }
            return null;
        }

        // Var is a parameter, return
        variable = table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(varRefName)).findFirst();
        if(variable.isPresent()){
            Optional<Symbol> finalVariable = variable;
            if(varRefExpr.getParent().getKind().equals("AssignStmt") && table.getImports().stream().anyMatch(value -> value.equals(finalVariable.get().getType().getName()))){
                varRefExpr.put("type", varRefExpr.getParent().get("type"));
                varRefExpr.put("isArray", String.valueOf(variable.get().getType().isArray()));
            }else{
                varRefExpr.put("type", variable.get().getType().getName());
                varRefExpr.put("isArray", String.valueOf(variable.get().getType().isArray()));
            }
            return null;
        }

        // Var is a declared variable, return
        variable = table.getLocalVariables(currentMethod).stream()
                .filter(varDecl -> varDecl.getName().equals(varRefName)).findFirst();
        if (variable.isPresent()) {
            Optional<Symbol> finalVariable = variable;
            if(varRefExpr.getParent().getKind().equals("AssignStmt") && table.getImports().stream().anyMatch(value -> value.equals(finalVariable.get().getType().getName()))){
                varRefExpr.put("type", varRefExpr.getParent().get("type"));
                varRefExpr.put("isArray", String.valueOf(variable.get().getType().isArray()));
            }else{
                varRefExpr.put("type", variable.get().getType().getName());
                varRefExpr.put("isArray", String.valueOf(variable.get().getType().isArray()));
            }
            return null;
        }

        var variable_import = table.getImports().stream()
                .filter(varDecl -> varDecl.equals(varRefName)).findFirst();

        if (variable_import.isPresent()) {
            varRefExpr.put("type", varRefName);
            varRefExpr.put("isArray", "false");
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );
        return null;
    }
    private Void visitVarDecl(JmmNode node, SymbolTable table){
        if(node.getChild(0).get("name").equals("int...")){
            var message = String.format("Variable '%s' can not be declared with vararg type.", node);
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

    private Void visitIDType(JmmNode node, SymbolTable table){
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
        return null;
    }
}