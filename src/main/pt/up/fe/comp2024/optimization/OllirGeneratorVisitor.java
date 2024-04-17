package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {
    private static final String SPACE = " ";
    private static final String IMPORT = "import ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final SymbolTable table;
    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDeclaration);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(CLASS_BODY, this::visitClassBody);
        addVisit(MAIN_METHOD_DECL, this::visitMainMethodDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(METHOD_CODE_BLOCK_WITHOUT_RETURN, this::visitMethodCodeBlock);
        addVisit(METHOD_CODE_BLOCK, this::visitMethodCodeBlock);
        addVisit(METHOD_CLASS_CALL_EXPR, this::visitMethodClassCallExpr);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        setDefaultVisit(this::defaultVisit);
    }



    private String visitMethodClassCallExpr(JmmNode jmmNode, Void unused) {
        System.out.println("visiting method call expr");

        StringBuilder code = new StringBuilder();

        var child = jmmNode.getJmmChild(0);

        StringBuilder params = new StringBuilder();
        // params
        if(jmmNode.getNumChildren() > 1) {
            for (int i = 1; i < jmmNode.getNumChildren(); i++){
                var expr = exprVisitor.visit(jmmNode.getJmmChild(i));
                code.append(expr.getComputation());
                params.append(", ").append(expr.getCode());
            }
        }

        String type = OptUtils.toOllirType(new Type(jmmNode.get("type"), false));

        if(Objects.equals(child.get("type"), child.get("name"))){ // static import call
            code.append("invokestatic(").append(child.get("type"));
            type = ".V";
        }
        else { // class call or Object import call
            var class_name = OptUtils.toOllirType(new Type(child.get("type"), false));
            code.append("invokevirtual(").append(child.get("name")).append(class_name);
            for(var imp : table.getImports()){
                if (Objects.equals(class_name, "." + imp)) {
                    type = ".V";
                    break;
                }
            }
        }

        var name = jmmNode.get("name");

        code.append(", ").append('"').append(name).append('"')
            .append(params).append(")").append(type).append(END_STMT);


        System.out.println("code: " + code);

        return code.toString();
    }

    private String visitImportDeclaration(JmmNode jmmNode, Void unused) {
        System.out.println("visiting import declaration");

        StringBuilder finalImport = new StringBuilder(IMPORT);
        for(var name : jmmNode.get("name").substring(1, jmmNode.get("name").length() - 1).split(","))
            finalImport.append(name.replace(' ', '.'));

        return finalImport + END_STMT;
    }

    private String visitMainMethodDecl(JmmNode jmmNode, Void unused) {
        System.out.println("visiting main method decl");

        StringBuilder code = new StringBuilder(".method public static main(args.array.String).V {\n");

        for (var child : jmmNode.getChildren())
            code.append(visit(child));

        code.append(R_BRACKET).append(NL);

        return code.toString();
    }


    private String visitClassBody(JmmNode jmmNode, Void unused) {
            System.out.println("visiting class body");

            StringBuilder code = new StringBuilder();

            for (var child : jmmNode.getChildren())
                code.append(visit(child));

            return code.toString();
    }

    private String visitMethodCodeBlock(JmmNode jmmNode, Void unused) {

        StringBuilder code = new StringBuilder();

        System.out.println("visiting method code block " + jmmNode);

        for (var child : jmmNode.getChildren())
            code.append(visit(child));

        if(jmmNode.getKind().equals("MethodCodeBlockWithoutReturn"))
            code.append("ret.V").append(END_STMT);

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        System.out.println("visiting assign stmt: " + node);

        var lhs_type = OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
        var lhs = node.get("name") + lhs_type;
        System.out.println("lhs: " + lhs);

        System.out.println("s: " + node.getChildren());
        System.out.println(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(0));

        System.out.println("rhs: " + rhs.getCode());
        System.out.println("rhs computation: " + rhs.getComputation());
        StringBuilder code = new StringBuilder();

        var isField_lhs = false;

        code.append(rhs.getComputation());

        for(var f : table.getFields()) {
            if(f.getName().equals(node.get("name"))) {
                isField_lhs = true;
                code.append("putfield(this, ").append(node.get("name")).append(lhs_type).append(", ").append(rhs.getCode()).append(").V").append(END_STMT);
                break;
            }
        }

        if(!isField_lhs){// code to compute the children
            System.out.println("rhs computations: " + rhs.getComputation());

            // code to compute self
            // statement has type of lhs
            Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
            String typeString = OptUtils.toOllirType(thisType);

            code.append(lhs).append(SPACE).append(ASSIGN).append(typeString)
                .append(SPACE).append(rhs.getCode()).append(END_STMT);
        }

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        System.out.println("visiting return");

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0)
            expr = exprVisitor.visit(node.getJmmChild(0));

        code.append(expr.getComputation()).append("ret")
                .append(OptUtils.toOllirType(retType)).append(SPACE)
                .append(expr.getCode()).append(END_STMT);

        return code.toString();
    }



    private String visitParam(JmmNode node, Void unused) {
        System.out.println("visiting param");
        StringBuilder code = new StringBuilder();
        var id_array = stringToArray(node.get("name"));

        for(int i = 0; i < node.getNumChildren(); i++) {
            var typeCode = OptUtils.toOllirType(node.getJmmChild(i));
            code.append(id_array[i]).append(typeCode);
            if(i != node.getNumChildren() - 1) code.append(", ");
        }

        return code.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        System.out.println("visiting method decl");

        StringBuilder code = new StringBuilder(".method ");

        if (NodeUtils.getBooleanAttribute(node, "isPublic", "true"))
            code.append("public ");


        // name
        code.append(node.get("name"));
        var afterParam = 0;


        // type
        String retType;
        if (node.getNumChildren() >= 2) {
            if (node.getJmmChild(0).getKind().contains("FunctionParameters")) {
                retType = ".V";
                afterParam = 1;
                code.append("(").append(visit(node.getJmmChild(0))).append(")");
            } else if (node.getJmmChild(1).getKind().contains("FunctionParameters")) {
                retType = OptUtils.toOllirType(node.getJmmChild(0));
                afterParam = 2;
                code.append("(").append(visit(node.getJmmChild(1))).append(")");
            }
            else {
                retType = OptUtils.toOllirType(node.getJmmChild(0));;
                code.append("()");
            }
        }
        else {
            retType = ".V";
            code.append("()");
        }

        code.append(retType).append(L_BRACKET);

        // rest of its children stmts
        for (int i = afterParam; i < node.getNumChildren(); i++)
            code.append(visit(node.getJmmChild(i)));

        code.append(R_BRACKET).append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {
        System.out.println("visiting class");

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        if(!Objects.equals(table.getSuper(), "not extended")) code.append(" extends ").append(table.getSuper());
        else code.append(" extends Object");

        code.append(L_BRACKET).append(NL);


        for(var field : table.getFields())
            code.append(".field public ").append(field.getName())
                .append(OptUtils.toOllirType(field.getType())).append(END_STMT);

        code.append(NL);

        var needNl = true;
        for (var child : node.getChildren()) {
            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }
            code.append(visit(child));
        }

        code.append(buildConstructor()).append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        System.out.println("visiting program") ;
        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        System.out.println("visiting default: " + node);
        System.out.println("children: " + node.getChildren());

        StringBuilder code = new StringBuilder();

        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }


    public static String[] stringToArray(String input) {
        // Remove square brackets and split the string by commas
        String[] elements = input.replaceAll("[\\[\\]]", "").split(",");

        // Create an array of the same size as the number of elements
        String[] array = new String[elements.length];

        // Copy elements to the array
        System.arraycopy(elements, 0, array, 0, elements.length);

        return array;
    }
}


