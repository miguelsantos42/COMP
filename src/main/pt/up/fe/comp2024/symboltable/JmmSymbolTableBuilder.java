package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder extends AJmmVisitor<String, String>  {

    private String className = "";
    private String extendedClassName = "";
    private final ArrayList<String> imports = new ArrayList<>();
    private final ArrayList<String> methods = new ArrayList<>();
    private final HashMap<String, Type> methodReturnTypes = new HashMap<>();
    private final HashMap<String, List<Symbol>> methodParams = new HashMap<>();
    private final HashMap<String, List<Symbol>> methodLocalVariables = new HashMap<>();

    private final JmmSymbolTable table;

    public JmmSymbolTableBuilder(JmmNode rootNode){
        visit(rootNode, "");
        this.table = new JmmSymbolTable(className, extendedClassName, imports, methods, methodReturnTypes, methodParams, methodLocalVariables);
    }

    public JmmSymbolTable getTable(){
        return this.table;
    }

    public void buildVisitor() {
        addVisit("Program", this::visitProgram);
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
        addVisit("StringType", this::visitStringType);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("BlockStmt", this::visitBlockStmt);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ArrayAssignStmt", this::visitArrayAssignStmt);
        addVisit("ExprStmt", this::visitExprStmt);
        addVisit("ParenthesisExpr", this::visitParenthesisExpr);
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

    private String visitProgram(JmmNode jmmNode, String s) {
        System.out.println("Visiting Program\n");
        for(var child : jmmNode.getChildren()) visit(child, s);
        return s;
    }

    private String visitImportDeclaration(JmmNode node, String s) {
        System.out.println("Visiting Import\n");
        String importName = node.get("name");
        String importNormalized = importName.replace(']', ' ')
                                            .replace('[',' ')
                                            .replace(", ", ".")
                                            .strip();
        this.imports.add(importNormalized);
        System.out.println("Import Name: " + importNormalized);
        return s;
    }

    private String visitClassDeclaration(JmmNode node, String s) {
        System.out.println("\nVisiting Class\n");
        this.className = node.get("name");
        this.extendedClassName = node.get("extendedName");
        System.out.println("Class Name: " + this.className);
        System.out.println("Extended Class Name: " + this.extendedClassName);

        for (JmmNode child : node.getChildren()) {
            visit(child, s);
        }

        return s;
    }

    private String visitClassBodyDeclaration(JmmNode node, String s) {
        System.out.println("\nVisiting Class Body\n");

        for (JmmNode child : node.getChildren()) {
            visit(child, s);
        }

        return s;
    }

    private String visitMethodDeclaration(JmmNode node, String s) {
        System.out.println("Visiting Method Declaration\n");
        String methodName = node.get("name");
        this.methods.add(methodName);
        System.out.println("Method Name: " + methodName);
        System.out.println("Parameters:");
        System.out.println(node.getChildren());

        for (JmmNode child : node.getChildren()) {
            if(Objects.equals(child.getKind(), "FunctionParameters")){
                List<Symbol> params = new ArrayList<>();

                String parameters = child.get("name");

                String[] paramList = parameters.replace(']', ' ')
                                                .replace('[',' ')
                                                .strip()
                                                .split(", ");
                int i = 0;
                for (JmmNode param : child.getChildren()) {
                    String paramName = paramList[i++];
                    String paramType = param.getKind();
                    params.add(new Symbol(new Type(paramType, false), paramName));
                }

                this.methodParams.put(methodName, params);
            }
            else if(Objects.equals(child.getKind(), "MethodCodeBlock")){
                visit(child, s);
            }
            else {
                this.methodReturnTypes.put(methodName, new Type(child.getKind(), false));
            }
        }


        return s;
    }

    private String visitMainMethodDeclaration(JmmNode node, String s) {
        System.out.println("Visiting Main Method Declaration\n");

        for (JmmNode child : node.getChildren()) {
            visit(child, s);
        }

        return s;
    }

    private String visitMethodCodeBlock(JmmNode node, String s) {
        return null;
    }

    // not needed imo
    private String visitFunctionParameters(JmmNode node, String s) {
        return null;
    }

    private String visitVarDeclaration(JmmNode node, String s) {
        return null;
    }

    private String visitIntType(JmmNode node, String s) {
        return null;
    }

    private String visitIDType(JmmNode node, String s) {
        return null;
    }

    private String visitBoolType(JmmNode node, String s) {
        return null;
    }

    private String visitIntVectorType1(JmmNode node, String s) {
        return null;
    }

    private String visitIntVectorType2(JmmNode node, String s) {
        return null;
    }

    private String visitStringType(JmmNode node, String s) {
        return null;
    }

    private String visitAssignStmt(JmmNode node, String s) {
        String value;

        if (node.getChildren().size() != 0)
            value = visit(node.getChildren().get(0), "");
        else
            value = node.get("value");

        // add to methodLocalVariables

        return s;
    }

    private String visitReturnStmt(JmmNode node, String s) {
        return null;
    }

    private String visitBlockStmt(JmmNode node, String s) {
        return null;
    }

    private String visitIfStmt(JmmNode node, String s) {
        return null;
    }

    private String visitWhileStmt(JmmNode node, String s) {
        return null;
    }

    private String visitArrayAssignStmt(JmmNode node, String s) {
        return null;
    }

    private String visitExprStmt(JmmNode node, String s) {
        return null;
    }

    private String visitParenthesisExpr(JmmNode node, String s) {
        return null;
    }

    private String visitArrayAccessExpr(JmmNode node, String s) {
        return null;
    }

    private String visitArrayLengthExpr(JmmNode node, String s) {
        return null;
    }

    private String visitMethodCallExpr(JmmNode node, String s) {
        return null;
    }

    private String visitNegationExpr(JmmNode node, String s) {
        return null;
    }

    private String visitNewObjectExpr(JmmNode node, String s) {
        return null;
    }

    private String visitBinaryExpr(JmmNode node, String s) {
        return null;
    }

    private String visitVarRefExpr(JmmNode node, String s) {
        return null;
    }

    private String visitThisExpr(JmmNode node, String s) {
        return null;
    }

    private String visitLogicalExpr(JmmNode node, String s) {
        return null;
    }

    private String visitIntegerLiteral(JmmNode node, String s) {
        return null;
    }

    private String visitBooleanLiteral(JmmNode node, String s) {
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
                .forEach(method -> map.put(method.get("name"), List.of(new Symbol(intType, method.getJmmChild(1).get("name")))));

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
