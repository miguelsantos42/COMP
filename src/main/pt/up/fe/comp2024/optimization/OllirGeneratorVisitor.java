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


        code.append("invokestatic(");
        code.append(jmmNode.getJmmChild(0).get("name"));
        code.append(", ");
        code.append("\"");
        code.append(jmmNode.get("name"));
        code.append("\"");
        if(jmmNode.getNumChildren() > 1) {
            code.append(",");
            code.append(jmmNode.getChild(1).get("name"));
            code.append(OptUtils.toOllirType(TypeUtils.getExprType(jmmNode.getJmmChild(1), table)));
        }
            code.append(").V");

        code.append(END_STMT);


        System.out.println("code: " + code);

        return code.toString();
    }

    private String visitImportDeclaration(JmmNode jmmNode, Void unused) {
        System.out.println("visiting import declaration");

        return IMPORT + jmmNode.get("ID") + END_STMT;
    }

    private String visitMainMethodDecl(JmmNode jmmNode, Void unused) {
        System.out.println("visiting main method decl");

        StringBuilder code = new StringBuilder(".method public static main(args.array.String).V {\n");

        for (var child : jmmNode.getChildren()) {
            code.append(visit(child));
        }

        code.append("ret.V");
        code.append(END_STMT);

        code.append(R_BRACKET);
        code.append(NL);


        return code.toString();
    }


    private String visitClassBody(JmmNode jmmNode, Void unused) {
            System.out.println("visiting class body");

            StringBuilder code = new StringBuilder();

            for (var child : jmmNode.getChildren()) {
                code.append(visit(child));
            }

            return code.toString();
    }

    private String visitMethodCodeBlock(JmmNode jmmNode, Void unused) {

        StringBuilder code = new StringBuilder();

        System.out.println("visiting method code block " + jmmNode);

        for (var child : jmmNode.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        System.out.println("visiting assign stmt: " + node);

        var lhs_type = OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
        var lhs = node.get("name") + lhs_type;
        System.out.println("lhs: " + lhs);

        System.out.println(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(0));

        System.out.println("rhs: " + rhs.getCode());
        StringBuilder code = new StringBuilder();

        var isField_lhs = false;

        for(var f : table.getFields()) {
            if(f.getName().equals(node.get("name"))) {
                isField_lhs = true;
                code.append("putfield(this, ").append(node.get("name")).append(lhs_type).append(", ").append(rhs.getCode()).append(").V").append(END_STMT);
            }
        }

        if(!isField_lhs){// code to compute the children
            code.append(rhs.getComputation());
            System.out.println("rhs computations: " + rhs.getComputation());

            // code to compute self
            // statement has type of lhs
            Type thisType =TypeUtils.getExprType(node.getJmmChild(0), table);
            String typeString = OptUtils.toOllirType(thisType);


            code.append(lhs);
            code.append(SPACE);

            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);

            code.append(rhs.getCode());

            code.append(END_STMT);
        }

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        System.out.println("visiting return");

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }



    private String visitParam(JmmNode node, Void unused) {
        System.out.println("visiting param");
        StringBuilder code = new StringBuilder();
        var id_array = stringToArray(node.get("name"));

        for(int i = 0; i < node.getNumChildren(); i++) {
            var typeCode = OptUtils.toOllirType(node.getJmmChild(i));
            var id  = id_array[i];
            code.append(id).append(typeCode);
            if(i != node.getNumChildren() - 1){
                code.append(", ");
            }
        }

        return code.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        System.out.println("visiting method decl");

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "true");

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);
        var afterParam = 1;

        // param
        if(node.getNumChildren() > 2) {
            afterParam = 2;
            code.append("(");
            var paramCode = visit(node.getJmmChild(1));
            code.append(paramCode);
            code.append(")");
        } else {
            code.append("()");
        }

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {
        System.out.println("visiting class");

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        if(!Objects.equals(table.getSuper(), "not extended")) {
            code.append(" extends ");
            code.append(table.getSuper());
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for(var field : table.getFields()) {
            code.append(".field ");
            code.append("public ");
            code.append(field.getName());
            code.append(OptUtils.toOllirType(field.getType()));
            code.append(END_STMT);
        }



        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());

        code.append(R_BRACKET);

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


