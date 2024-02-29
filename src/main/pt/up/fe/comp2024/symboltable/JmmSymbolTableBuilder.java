package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder extends AJmmVisitor<String, String>  {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getJmmChild(0) ;
        System.out.println(root.getChildren());
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals);
    }


    public void buildVisitor() {
        addVisit("ImportDeclaration", this::visitImportDeclaration);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("ClassBodyDeclaration", this::visitClassBodyDeclaration);
        addVisit("MethodDeclaration", this::visitMethodDeclaration);
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        addVisit("MethodCodeBlock", this::visitMethodCodeBlock);
        addVisit("FunctionParameters", this::visitFunctionParameters);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("IntType", this::visitIntType);
        addVisit("IDType", this::visitIDType);
        addVisit("BoolType", this::visitBoolType);
        addVisit("IntVectorType1", this::visitIntVectorType1);
        addVisit("IntVectorType2", this::visitIntVectorType2);
        addVisit("StingType", this::visitStingType);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("BlockStmt", this::visitBlockStmt);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ArrayAssignStmt", this::visitArrayAssignStmt);
        addVisit("ExprStmt", this::visitExprStmt);
        addVisit("ParanthesisExpr", this::visitParanthesisExpr);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);
        addVisit("ArrayLengthExpr", this::visitArrayLengthExpr);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit("NegationExpr", this::visitNegationExpr);
        addVisit("NewObjectExpr", this::visitNewObjectExpr);
        addVisit("NewObjectExpr", this::visitNewObjectExpr);
        addVisit("BinaryExpr", this::visitBinaryExpr);
        addVisit("VarRefExpr", this::visitVarRefExpr);
        addVisit("ThisExpr", this::visitThisExpr);
        addVisit("LogicalExpr", this::visitLogicalExpr);
        addVisit("IntegerLiteral", this::visitIntegerLiteral);
        addVisit("BooleanLiteral", this::visitBooleanLiteral);
    }

    private String visitImportDeclaration(JmmNode node, String arg) {
        return null;
    }

    private String visitClassDeclaration(JmmNode node, String arg) {
        return null;
    }

    private String visitClassBodyDeclaration(JmmNode node, String arg) {
        return null;
    }

    private String visitMethodDeclaration(JmmNode node, String arg) {
        return null;
    }

    private String visitMainMethodDeclaration(JmmNode node, String arg) {
        return null;
    }

    private String visitMethodCodeBlock(JmmNode node, String arg) {
        return null;
    }

    private String visitFunctionParameters(JmmNode node, String arg) {
        return null;
    }

    private String visitVarDeclaration(JmmNode node, String arg) {
        return null;
    }

    private String visitIntType(JmmNode node, String arg) {
        return null;
    }

    private String visitIDType(JmmNode node, String arg) {
        return null;
    }

    private String visitBoolType(JmmNode node, String arg) {
        return null;
    }

    private String visitIntVectorType1(JmmNode node, String arg) {
        return null;
    }

    private String visitIntVectorType2(JmmNode node, String arg) {
        return null;
    }

    private String visitStingType(JmmNode node, String arg) {
        return null;
    }

    private String visitAssignStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitReturnStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitBlockStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitIfStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitWhileStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitArrayAssignStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitExprStmt(JmmNode node, String arg) {
        return null;
    }

    private String visitParanthesisExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitArrayAccessExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitArrayLengthExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitMethodCallExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitNegationExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitNewObjectExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitBinaryExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitVarRefExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitThisExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitLogicalExpr(JmmNode node, String arg) {
        return null;
    }

    private String visitIntegerLiteral(JmmNode node, String arg) {
        return null;
    }

    private String visitBooleanLiteral(JmmNode node, String arg) {
        return null;
    }





    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
