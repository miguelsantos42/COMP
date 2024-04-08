package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.lang.invoke.StringConcatFactory;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(NEW_OBJECT_EXPR, this::visitNewObjectExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitBoolean(JmmNode jmmNode, Void unused) {
        System.out.println("visiting boolean");
        var boolType = new Type("boolean", false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = jmmNode.get("name") + ollirBoolType;
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

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        System.out.println("visiting var ref");
        StringBuilder code = new StringBuilder();
        var isField = false;
        var temp = "";

        for(var fields : table.getFields()) {
            if(fields.getName().equals(node.get("name"))) {
                isField = true;
                temp = OptUtils.getTemp();
                code.append(temp).append(OptUtils.toOllirType(fields.getType())).append(SPACE);
                code.append(ASSIGN).append(OptUtils.toOllirType(fields.getType())).append(SPACE);
                code.append("getfield(this, ").append(fields.getName()).append(OptUtils.toOllirType(fields.getType())).append(")"); //this might be something else
                code.append(OptUtils.toOllirType(fields.getType())).append(END_STMT);
            }
        }

        var isArray = node.get("isArray") == "true" ? true : false;
        var type = new Type(node.get("type"), isArray);
        String ollirType = OptUtils.toOllirType(type);
        String real_code = "";

        if(!isField) {
            var id = node.get("name");
            real_code = id + ollirType;
        }
        else {
            var id = temp;
            real_code = temp + ollirType;
        }

        return new OllirExprResult(real_code,code);
    }


    private OllirExprResult visitNewObjectExpr(JmmNode jmmNode, Void unused) {

        System.out.println("visiting new object expr");

        StringBuilder computation = new StringBuilder();
        var type = "." + jmmNode.get("type");
        var name = jmmNode.get("name");
        var tmp = OptUtils.getTemp();

        computation.append(tmp).append(type).append(SPACE)
                .append(ASSIGN).append(type).append(SPACE)
                .append("new").append("(").append(name).append(")").append(type).append(END_STMT);

        computation.append("invokespecial(").append(tmp).append(type).append(", \"\").V").append(END_STMT);

        StringBuilder code = new StringBuilder();
        code.append(tmp).append(type);

        return new OllirExprResult(code.toString(), computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
