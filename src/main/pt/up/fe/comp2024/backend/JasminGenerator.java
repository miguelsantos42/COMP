package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);
        System.out.println("Class: " + className);
        // TODO: Hardcoded to Object, needs to be expanded
        code.append(".super java/lang/Object").append(NL);

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }
            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;
        System.out.println("Method: " + method.getMethodName());

        var code = new StringBuilder();

        // calculate modifier

        System.out.println("Method: " + method.getMethodAccessModifier());
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";
        if (method.isStaticMethod()) {
            modifier += "static ";
        }

        var methodName = method.getMethodName();

        var params = generateParams(method.getParams());

        var returnType = getReturnType(method.getReturnType().toString());


        code.append("\n.method ").append(modifier).append(methodName).append("(").append(params).append(")").append(returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            System.out.println("Instruction: " + inst);
            if (!(inst instanceof CallInstruction)) {

                var instCode = StringLines.getLines(generators.apply(inst)).stream()
                        .collect(Collectors.joining(NL + TAB, TAB, NL));

                code.append(instCode);
            }
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }


    private String generateAssign(AssignInstruction assign) {
        System.out.println("Assign: " + assign);
        var code = new StringBuilder();

        // generate code for loading what's on the right
        System.out.println("RHS: " + generators.apply(assign.getRhs()));


        code.append(generators.apply(assign.getRhs()));
        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = switch (operand.getType().toString()) {
            case "INT32", "BOOLEAN" -> "istore_";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            default -> "astore_";
        };

        code.append(type).append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload_" + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        System.out.println("GetField: " + getFieldInstruction);
        var code = new StringBuilder();

        var className = ollirResult.getOllirClass().getClassName();

        code.append("aload_0").append(NL);

        var type = getFieldType(getFieldInstruction.getFieldType().toString());

        code.append("getfield ").append(className).append("/").append(getFieldInstruction.getField().getName()).append(" ").append(type).append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        System.out.println("PutField: " + putFieldInstruction);
        var code = new StringBuilder();

        var className = ollirResult.getOllirClass().getClassName();


        code.append("aload_0").append(NL);

        var value = putFieldInstruction.getValue();

        code.append(generators.apply(value));


        System.out.println("Field Type: " + putFieldInstruction.getOperands().get(1).getType()); //might not be the best way, what the I after the putfield instruction refer to?
        var type = getFieldType(putFieldInstruction.getOperands().get(1).getType().toString());

        code.append("putfield ").append(className).append("/").append(putFieldInstruction.getField().getName()).append(" ").append(type).append(NL);

        return code.toString();
    }


    private String generateCall(CallInstruction callInstruction) {
        System.out.println("Call: " + callInstruction);
        var code = new StringBuilder();

        var className = ollirResult.getOllirClass().getClassName();

        code.append("new ").append(className).append(NL);
        code.append("dup").append(NL);


        //code.append(callInstruction.getOperands());

        // detect wether its necessary a invokespecial or a invokevirtual
        /*
            invokespecial is used to call constructors (<init>()V) in the main and <init> methods.
            invokevirtual is used to call the add method in the main method.
         */


        if(callInstruction.getInvocationType().toString().equals("invokevirtual")) {
            code.append("invokevirtual ").append(className).append("/").append(callInstruction.getMethodName());

            var params = generateParams((ArrayList<Element>) callInstruction.getArguments());
            var returnType = getReturnType(callInstruction.getReturnType().toString());

            code.append("(").append(params).append(")").append(returnType).append(NL);
        }
        else if (callInstruction.getInvocationType().toString().equals("invokespecial")
                || callInstruction.getInvocationType().toString().equals("invokestatic")
                || callInstruction.getInvocationType().toString().equals("invokeinterface")
                || callInstruction.getInvocationType().toString().equals("NEW"))
            code.append("invokespecial ").append(className).append("/").append("<init>()V").append(NL);
        else
            System.out.println("Error: Invocation type not found");


        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        System.out.println(returnInst);
        if(returnInst.getOperand() == null) {
            code.append("return").append(NL);
        } else {
            code.append(generators.apply(returnInst.getOperand()));
            var type = switch (returnInst.getOperand().getType().toString()) {
                case "INT32", "BOOLEAN" -> "i";
                case "STRING" -> "a";
                default -> throw new NotImplementedException(returnInst.getOperand().getType());
            };
            code.append(type).append("return").append(NL);
        }

        return code.toString();
    }

    private StringBuilder generateParams(ArrayList<Element> paramList) {
        var params = new StringBuilder();
        for (var param : paramList) {
            System.out.println("Param: " + param.getType());
            switch (param.getType().toString()) {
                case "INT32" -> params.append("I");
                case "BOOLEAN" -> params.append("Z");
                case "STRING" -> params.append("Ljava/lang/String;");
                case "INT[]" -> params.append("[I");
                case "BOOLEAN[]" -> params.append("[Z");
                case "STRING[]" -> params.append("[Ljava/lang/String;");
                default -> throw new NotImplementedException(param.getType());
            }
        }
        return params;
    }

    private String getReturnType(String returnType) {
        return switch (returnType) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            case "VOID" -> "V";
            default -> throw new NotImplementedException(returnType);
        };
    }

    private String getFieldType(String fieldInstructionType) {
        return switch (fieldInstructionType) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            default -> throw new NotImplementedException(fieldInstructionType);
        };
    }

}
