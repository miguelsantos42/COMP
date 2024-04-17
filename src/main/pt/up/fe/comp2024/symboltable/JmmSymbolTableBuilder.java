package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class JmmSymbolTableBuilder extends AJmmVisitor<String, String>  {

    private String className = "";
    private String extendedClassName = "";
    private final ArrayList<String> imports = new ArrayList<>();
    private final ArrayList<String> methods = new ArrayList<>();
    private final ArrayList<Symbol> fields = new ArrayList<>();
    private final HashMap<String, Type> methodReturnTypes = new HashMap<>();
    private final HashMap<String, List<Symbol>> methodParams = new HashMap<>();
    private final HashMap<String, List<Symbol>> methodLocalVariables = new HashMap<>();
    private final ArrayList<Report> reports = new ArrayList<>();

    private final JmmSymbolTable table;

    public JmmSymbolTableBuilder(JmmNode rootNode){
        visit(rootNode, "");
        System.out.println("\n\nFinished Visit\n\n");
        this.table = new JmmSymbolTable(className, extendedClassName, imports, fields, methods, methodReturnTypes, methodParams, methodLocalVariables);
    }

    public JmmSymbolTable getTable(){
        return this.table;
    }

    public List<Report> getReports() { return this.reports; };

    protected void addReport(Report report) {
        reports.add(report);
    }

    public void buildVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDeclaration", this::visitImportDeclaration);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("ClassBodyDeclaration", this::visitClassBodyDeclaration);
        addVisit("MethodDeclaration", this::visitMethodDeclaration);
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        addVisit("MethodCodeBlock", this::visitMethodCodeBlock);
        addVisit("MethodCodeBlockWithoutReturn", this::visitMethodCodeBlock);
        addVisit("VarDeclaration", this::visitVarDeclaration);
    }

    private String visitProgram(JmmNode jmmNode, String s) {
        System.out.println("\nVisiting Program\n");
        for(var child : jmmNode.getChildren()) visit(child, s);
        return s;
    }

    private String visitImportDeclaration(JmmNode node, String s) {
        System.out.println("\nVisiting Import\n");
        String importName = node.get("ID");

        if(this.imports.contains(importName)){
            var message = String.format("Import %s duplicated", importName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
            return s;
        }

        this.imports.add(importName);
        System.out.println("Import Name: " + importName);
        return s;
    }

    private String visitClassDeclaration(JmmNode node, String s) {
        System.out.println("\n\nVisiting Class\n");
        this.className = node.get("name");
        this.extendedClassName = node.hasAttribute("extendedName") ? node.get("extendedName") : "not extended";
        System.out.println("Class Name: " + this.className);
        System.out.println("Extended Class Name: " + this.extendedClassName);
        for (JmmNode child : node.getChildren()) {
            visit(child, s);
        }

        return s;
    }

    private String visitClassBodyDeclaration(JmmNode node, String s) {
        System.out.println("\n\nVisiting Class Body\n");
        System.out.println("Visiting classdecl children: \n" + node.getChildren());
        for (JmmNode child : node.getChildren()) {
            visit(child, s);
        }

        return s;
    }

    private String visitMethodDeclaration(JmmNode node, String s) {
        System.out.println("\nVisiting Method Declaration\n");
        String methodName = node.get("name");
        if (this.methods.contains(methodName)) {
            var message = String.format("Method %s already declared", node.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
            return s;
        }
        this.methods.add(methodName);
        System.out.println("Method Name: " + methodName);
        System.out.println("Method Children:");
        System.out.println(node.getChildren());
        this.methodParams.put(methodName, new ArrayList<>());

        for (JmmNode child : node.getChildren()) {
            if(Objects.equals(child.getKind(), "FunctionParameters")) {
                List<Symbol> params = new ArrayList<>();

                String parameters = child.get("name");

                String[] paramList = parameters.replace(']', ' ')
                        .replace('[',' ')
                        .strip()
                        .split(", ");
                int i = 0;
                for (JmmNode param : child.getChildren()) {
                    String paramName = paramList[i++];
                    boolean isArray = param.getKind().equals("IntVectorType1") || param.getKind().equals("IntVectorType2");
                    String paramType = isArray ? "int" : param.get("name");
                    if(params.stream().anyMatch(p -> p.getName().equals(paramName))){
                        var message = String.format("Parameter %s already declared", paramName);
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(param),
                                NodeUtils.getColumn(param),
                                message,
                                null)
                        );
                        return s;
                    }
                    params.add(new Symbol(new Type(paramType, isArray), paramName));
                }

                this.methodParams.put(methodName, params);
            }
            else if(Objects.equals(child.getKind(), "MethodCodeBlock")){
                visit(child, s);
            } else if (Objects.equals(child.getKind(), "MethodCodeBlockWithoutReturn")){
                boolean isArray = false;
                String returnType = "void";
                this.methodReturnTypes.put(methodName, new Type(returnType, isArray));
                visit(child, s);
            }
            else {
                boolean isArray = child.getKind().equals("IntVectorType1") || child.getKind().equals("IntVectorType2");
                String returnType = isArray ? "int" : child.get("name");
                this.methodReturnTypes.put(methodName, new Type(returnType, isArray));
            }
        }


        return s;
    }

    private String visitMainMethodDeclaration(JmmNode node, String s) {
        System.out.println("\n\nVisiting Main Method Declaration\n");
        this.methods.add("main");
        List<Symbol> params = new ArrayList<>();
        params.add(new Symbol(new Type("string", true), "args"));
        this.methodParams.put("main", params);
        this.methodReturnTypes.put("main", new Type("void", false));
        for (JmmNode child : node.getChildren()) {
            visit(child, s);
        }

        return s;
    }

    private String visitMethodCodeBlock(JmmNode node, String s) {
        System.out.println("\nVisiting Method Code Block\n");

        if(node.getParent().hasAttribute("name")) {
            ArrayList<Symbol> localVariables = new ArrayList<>();
            this.methodLocalVariables.put(node.getParent().get("name"), localVariables);
            for (JmmNode child : node.getChildren()) {
                if (Objects.equals(child.getKind(), "VarDeclaration")) {
                    Symbol var;
                    if (child.getChild(0).getKind().equals("IntVectorType2")){
                        var message = String.format("Local variable %s cannot be of type varArg", child.get("name"));
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(child),
                                NodeUtils.getColumn(child),
                                message,
                                null)
                        );
                        return s;
                    }
                    if (localVariables.stream().anyMatch(p -> p.getName().equals(child.get("name")))  || this.methodParams.get(node.getParent().get("name")).stream().anyMatch(p -> p.getName().equals(child.get("name")))){
                        var message = String.format("Field variable %s already declared", child.get("name"));
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(child),
                                NodeUtils.getColumn(child),
                                message,
                                null)
                        );
                        return s;
                    }
                    if(child.getChild(0).getKind().equals("IntVectorType1"))
                        var = new Symbol(new Type("int", true), child.get("name"));
                    else
                        var = new Symbol(new Type(child.getChild(0).get("name"), false), child.get("name"));
                    localVariables.add(var);
                    this.methodLocalVariables.put(node.getParent().get("name"), localVariables);
                }
            }
        }
        else{
            ArrayList<Symbol> localVariables = new ArrayList<>();
            this.methodLocalVariables.put("main", localVariables);
            for (JmmNode child : node.getChildren()) {
                if (Objects.equals(child.getKind(), "VarDeclaration")) {
                    Symbol var;
                    if (child.getChild(0).getKind().equals("IntVectorType2")){
                        var message = String.format("Local variable %s cannot be of type varArg", child.get("name"));
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(child),
                                NodeUtils.getColumn(child),
                                message,
                                null)
                        );
                        return s;
                    }
                    if (localVariables.stream().anyMatch(p -> p.getName().equals(child.get("name"))) || this.methodParams.get("main").stream().anyMatch(p -> p.getName().equals(child.get("name")))){
                        var message = String.format("Field variable %s already declared", child.get("name"));
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(child),
                                NodeUtils.getColumn(child),
                                message,
                                null)
                        );
                        return s;
                    }
                    if(child.getChild(0).getKind().equals("IntVectorType1"))
                        var = new Symbol(new Type("int", true), child.get("name"));
                    else
                        var = new Symbol(new Type(child.getChild(0).get("name"), false), child.get("name"));
                    localVariables.add(var);
                    this.methodLocalVariables.put("main", localVariables);
                }
            }
        }
        return s;
    }

    private String visitVarDeclaration(JmmNode node, String s) {
        String varName = node.get("name");
        String varType = node.getChild(0).get("name");
        boolean isArray = false;
        if (node.getChild(0).getKind().equals("IntVectorType2")){
            var message = String.format("Field variable %s cannot be of type varArg", varName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
            return s;
        }
        if(node.getChild(0).getKind().equals("IntVectorType1")) {
            isArray = true;
            varType = "int";
        }
        if (this.fields.stream().anyMatch(p -> p.getName().equals(varName))){
            var message = String.format("Field variable %s already declared", varName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
            return s;
        }
        this.fields.add(new Symbol(new Type(varType, isArray), varName));
        return s;
    }
}