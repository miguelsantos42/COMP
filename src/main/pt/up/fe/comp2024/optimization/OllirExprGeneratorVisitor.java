package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.HashMap;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final SymbolTable table;
    private final HashMap<JmmNode, OllirExprResult> computedResults = new HashMap<>();

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(LOGICAL_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(NEW_OBJECT_EXPR, this::visitNewObjectExpr);
        addVisit(METHOD_CLASS_CALL_EXPR, this::visitMethodClassCallExpr);
        addVisit(PARENTHESIS_EXPR, this::visitParenthesisExpr);
        addVisit(THIS_EXPR, this::visitThisExpr);
        addVisit(NEGATION_EXPR, this::visitNegationExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitMethodClassCallExpr(JmmNode jmmNode, Void unused) {
        // Check if the computation for the current node has already been performed
        if (computedResults.containsKey(jmmNode)) {
            // If it has, return the result of the previous computation
            return computedResults.get(jmmNode);
        }

        System.out.println("visiting method class call expr");

        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var type = OptUtils.toOllirType(new Type(jmmNode.get("type"), false));
        var class_name = jmmNode.getAncestor("ClassDecl").get().get("name");
        String call_name = "";
        var childImport = jmmNode.getJmmChild(0);

        if(childImport.getKind().equals("ThisExpr")) {
            call_name = "this.";
        }
        else if(childImport.getKind().equals("VarRefExpr")) {
            var visitResult = visit(childImport);
            computedResults.put(childImport, visitResult);
            computation.append(visitResult.getComputation());
            class_name = "";
            call_name = visitResult.getCode();
        }
        else if(childImport.getKind().equals("ParenthesisExpr")){
            var visitResult = visit(childImport);
            computedResults.put(childImport, visitResult);
            computation.append(visitResult.getComputation());
            class_name = "";
            call_name = visitResult.getCode();
        }

        StringBuilder param = new StringBuilder();
        for(var child : jmmNode.getChildren()) {
            if(!child.equals(jmmNode.getChild(0))) {
                if (child.getKind().equals("VarRefExpr")) {
                    var visitResult = visit(child);
                    param.append(", ");
                    param.append(visitResult.getCode());
                    computedResults.put(child, visitResult);
                } else if (child.getKind().equals("BinaryExpr") || child.getKind().equals("LogicalExpr")) {
                    var visitResult = visit(child);
                    param.append(", ");
                    param.append(visitResult.getCode());
                    computation.append(visitResult.getComputation());
                    computedResults.put(child, visitResult);
                } else if (child.getKind().equals("MethodClassCallExpr")) {
                    var visitResult = visit(child);
                    param.append(", ");
                    param.append(visitResult.getCode());
                    computation.append(visitResult.getComputation());
                    computedResults.put(child, visitResult);
                } else {
                    param.append(", ");
                    param.append(child.get("value"));
                    param.append(OptUtils.toOllirType(TypeUtils.getExprType(child, table)));
                }
            }
        }

        param.append(")");
        var name = "\"" + jmmNode.get("name") + "\"";

        System.out.println("call_name: " + call_name);
        System.out.println("name: " + name);
        System.out.println("param: " + param);
        System.out.println("class_name: " + class_name);


        var tmp = OptUtils.getTemp();

        if(Objects.equals(childImport.get("type"), childImport.get("name"))) { // import call
            computation.append(tmp).append(type).append(SPACE).append(ASSIGN).append(type).append(SPACE)
                    .append("invokestatic(").append(childImport.get("type")).append(", ")
                    .append("\"").append(jmmNode.get("name")).append("\"")
                    .append(param).append(type).append(END_STMT);

        }
        else {
            computation.append(tmp).append(type).append(SPACE).append(ASSIGN).append(type).append(SPACE)
                    .append("invokevirtual(").append(call_name).append(class_name).append(", ").append(name).append(param).append(type).append(END_STMT);
        }

        System.out.println("computation2: " + computation);

        code.append(tmp).append(type);

        // Store the result of the computation in the HashMap
        OllirExprResult result = new OllirExprResult(code.toString(), computation);
        computedResults.put(jmmNode, result);

        return result;
    }

    private OllirExprResult visitBoolean(JmmNode jmmNode, Void unused) {
        System.out.println("visiting boolean");
        var boolType = new Type("boolean", false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        var value = Objects.equals(jmmNode.get("name"), "true") ? "1" : "0";
        String code = value + ollirBoolType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        System.out.println("visiting integer");
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        System.out.println("visiting bin expr");

        // Check if the computation for the current node has already been performed
        if (computedResults.containsKey(node)) {
            // If it has, return the result of the previous computation
            return computedResults.get(node);
        }

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;


        // short-circuit
        if(Objects.equals(node.get("op"), "&&")) {
                computation.append("if (").append(lhs.getCode()).append(") goto l1").append(END_STMT);
                computation.append(code).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE)
                        .append("0.bool").append(END_STMT);
                computation.append("goto endif").append(END_STMT);
                computation.append("l1:").append(NL)
                        .append(code).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE)
                        .append(lhs.getCode()).append(SPACE);

                Type type = TypeUtils.getExprType(node, table);
                computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                        .append(rhs.getCode()).append(END_STMT);

                computation.append("endif: ").append(NL);
        }
        else {
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(lhs.getCode()).append(SPACE);

            Type type = TypeUtils.getExprType(node, table);
            computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        }

        // Store the result of the computation in the HashMap
        OllirExprResult result = new OllirExprResult(code, computation);
        computedResults.put(node, result);

        return result;
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        // Check if the computation for the current node has already been performed
        if (computedResults.containsKey(node)) {
            // If it has, return the result of the previous computation
            return computedResults.get(node);
        }

        System.out.println("visiting var ref");
        StringBuilder code = new StringBuilder();
        var temp = "";
        var isArray = Objects.equals(node.get("isArray"), "true");
        var type = new Type(node.get("type"), isArray);
        String ollirType = OptUtils.toOllirType(type);

        for(var field : table.getFields()) {
            if(field.getName().equals(node.get("name"))) {
                temp = OptUtils.getTemp();
                code.append(temp).append(OptUtils.toOllirType(field.getType())).append(SPACE);
                code.append(ASSIGN).append(OptUtils.toOllirType(field.getType())).append(SPACE);
                code.append("getfield(this, ").append(field.getName()).append(OptUtils.toOllirType(field.getType())).append(")");
                code.append(OptUtils.toOllirType(field.getType())).append(END_STMT);
                OllirExprResult result = new OllirExprResult(temp + ollirType, code);
                computedResults.put(node, result);
                return result;
            }
        }

        // Store the result of the computation in the HashMap
        OllirExprResult result = new OllirExprResult(node.get("name") + ollirType, code);
        computedResults.put(node, result);

        return result;
    }

    private OllirExprResult visitNegationExpr(JmmNode jmmNode, Void unused) {
        System.out.println("visiting negation expr");
        var child = jmmNode.getJmmChild(0);
        var visitResult = visit(child);

        StringBuilder computation = new StringBuilder();
        computation.append(visitResult.getComputation());

        var type = OptUtils.toOllirType(new Type(jmmNode.get("type"), false));
        var code = OptUtils.getTemp() + type;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(type).append(SPACE)
                .append("!").append(type).append(SPACE)
                .append(visitResult.getCode()).append(END_STMT);

        var result = new OllirExprResult(code, computation);
        computedResults.put(jmmNode, result);

        return result;
    }

    private OllirExprResult visitNewObjectExpr(JmmNode jmmNode, Void unused) {
        if (computedResults.containsKey(jmmNode)) {
            // If it has, return the result of the previous computation
            return computedResults.get(jmmNode);
        }

        System.out.println("visiting new object expr");

        StringBuilder computation = new StringBuilder();
        var type = OptUtils.toOllirType(new Type(jmmNode.get("type"), false));
        var name = jmmNode.get("name");
        var tmp = OptUtils.getTemp();

        computation.append(tmp).append(type).append(SPACE)
                .append(ASSIGN).append(type).append(SPACE)
                .append("new").append("(").append(name).append(")").append(type).append(END_STMT);

        computation.append("invokespecial(").append(tmp).append(type).append(", \"<init>\").V").append(END_STMT);

        var result = new OllirExprResult(tmp + type, computation);
        computedResults.put(jmmNode, result);

        return result;
    }

    private OllirExprResult visitParenthesisExpr(JmmNode jmmNode, Void unused){
        if (computedResults.containsKey(jmmNode)) {
            // If it has, return the result of the previous computation
            return computedResults.get(jmmNode);
        }
        System.out.println("visiting parenthesis expr");
        var child = jmmNode.getJmmChild(0);
        var visitResult = visit(child);

        OllirExprResult result = new OllirExprResult(visitResult.getCode(), visitResult.getComputation());
        computedResults.put(jmmNode, result);

        return result;
    }

    private  OllirExprResult visitThisExpr(JmmNode jmmNode, Void unused) {
        System.out.println("visiting this expr");
        var type = jmmNode.getAncestor("ClassDecl").get().get("name");
        var code = "this." + type;
        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren())
            visit(child);

        return OllirExprResult.EMPTY;
    }

}
