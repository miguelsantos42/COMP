package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private int stackLimit = 0;
    private int localsLimit = 0;


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
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCondInstruction);
        generators.put(OpCondInstruction.class, this::generateOpCondInstruction);
        generators.put(GotoInstruction.class, this::generateGoToInstruction);
        generators.put(ArrayOperand.class, this::generateArrayOperand);
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
        code.append(".class ").append(className).append(NL);
        System.out.println("Class: " + className);
        String superClass = "";
        if (classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")) {
            superClass = "java/lang/Object";
        } else {
            for (var imp : ollirResult.getOllirClass().getImports()) {
                if (imp.contains(classUnit.getSuperClass())) {
                    var split = imp.split("\\.");
                    superClass = String.join(".", split);
                }
            }
        }

        code.append(".super ").append(superClass).append(NL).append(NL);

        // generate fields
        for (var field : ollirResult.getOllirClass().getFields()) {
            System.out.println("Field: " + field);
            var type = getFieldType(field.getFieldType().toString());
            var modifier = field.getFieldAccessModifier().name().equals("DEFAULT") ? "public" : field.getFieldAccessModifier().name().toLowerCase();
            code.append(".field ").append(modifier).append(" ").append(field.getFieldName()).append(" ").append(type).append(NL);
        }

        String constructur = ".method public <init>()V\n" +
                "aload_0\n" +
                "invokespecial " + superClass + ".<init>()V\n" +
                "return\n" +
                ".end method\n";
        // generate a single constructor method
        code.append(constructur);

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

        stackLimit = 0;
        localsLimit = 0;
        // set method
        currentMethod = method;
        System.out.println("Method: " + method.getMethodName());

        var code = new StringBuilder();

        // calculate modifier

        System.out.println("Method access modifier: " + method.getMethodAccessModifier());
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


        StringBuilder finalCode = new StringBuilder();

        for (var inst : method.getInstructions()) {
            System.out.println("Instruction: " + inst);

            for (Map.Entry<String, Instruction> label : method.getLabels().entrySet()) {
                if (label.getValue().equals(inst)) {
                    finalCode.append(NL).append(label.getKey())
                            .append(":").append(NL);
                }
            }

            var generatedCode = generators.apply(inst);

            var instCode = StringLines.getLines(generatedCode).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            if (!generatedCode.isEmpty())
                finalCode.append(instCode);
        }

/*        for (var line : finalCode.toString().split("\n")) {
            if (line.charAt(line.length() - 1) == ':') {
                line = line.substring(3);
            }
        }*/

        if (!method.getReturnType().toString().equals("VOID")) {
            if (stackLimit < 1) {
                stackLimit = 1;
            }
        }

        code.append(TAB).append(".limit stack ").append(stackLimit).append(NL);
        code.append(TAB).append(".limit locals ").append(localsLimit + 1).append(NL);

        code.append(finalCode);

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }


    private String generateAssign(AssignInstruction assign) {
        System.out.println("\n\nAssign: " + assign);
        var code = new StringBuilder();

        if (stackLimit < 1) {
            stackLimit = 1;
        }

        // generate code for loading what's on the right
        var rhs = generators.apply(assign.getRhs());
        if (assign.getRhs().toString().contains("BINARYOPER")) {
            if (stackLimit < 2) {
                stackLimit = 2;
            }
        }
        System.out.println("RHS: " + rhs);

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        if (lhs instanceof ArrayOperand)
            code.append(generators.apply(lhs));

        code.append(rhs);

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        for (var var : currentMethod.getVarTable().values()) {
            System.out.println("Var: " + var.getVarType() + " " + var.getVirtualReg() + " " + var.getScope() + " ");
        }

        System.out.println("lhs: " + lhs);
        System.out.println("Register: " + reg);

        if (assign.getRhs().getInstType().toString().equals("NOPER") && !assign.getRhs().toString().contains("LiteralElement")) {
            String var = assign.getRhs().toString().substring(assign.getRhs().toString().lastIndexOf(' ') + 1, assign.getRhs().toString().indexOf('.'));
            var loadReg = currentMethod.getVarTable().get(var).getVirtualReg();

            String typeOfAssign = assign.getRhs().toString().substring(assign.getRhs().toString().lastIndexOf('.') + 1);
            var loadInstruction = getLoadInstruction(typeOfAssign, loadReg);
            if (loadReg >= this.localsLimit) this.localsLimit = loadReg;

            if (!var.contains("tmp")) {
                code.append(loadInstruction).append(loadReg).append(NL);
            }
        }

        var storeInstruction = getStoreInstruction(operand.getType().toString(), reg);
        if (reg >= this.localsLimit) this.localsLimit = reg;

        if (lhs instanceof ArrayOperand)
            code.append("iastore").append(NL);
        else
            code.append(storeInstruction).append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        StringBuilder code = new StringBuilder();
        if (operand.getType().toString().equals("INT32[]")) {
            var name = operand.toString().substring(operand.toString().lastIndexOf(' ') + 1, operand.toString().indexOf("."));
            String arrayLength = "";
            if (!name.contains("tmp")) return "";

            for (var inst : currentMethod.getInstructions()) {
                var operandNameVar = inst.toString().substring(inst.toString().indexOf("Operand: ") + 9, inst.toString().indexOf("."));
                if (operandNameVar.equals(name)) {
                    arrayLength = inst.toString().substring(inst.toString().indexOf("LiteralElement: ") + 16, inst.toString().lastIndexOf("."));
                    break;
                }
            }
            code.append("iconst_").append(arrayLength).append(NL);
            code.append("newarray int").append(NL);
        } else {
            var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
            var type = getLoadInstruction(operand.getType().toString(), reg);
            code.append(type).append(reg).append(NL);
        }

        return code.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        if (binaryOp.getOperation().getOpType().toString().equals("LTH")) {
            if (binaryOp.getLeftOperand().toString().contains("LiteralElement"))
                code.append(generators.apply(binaryOp.getLeftOperand()));
            else {
                var reg1 = this.currentMethod.getVarTable().get(binaryOp.getLeftOperand().toString().substring(binaryOp.getLeftOperand().toString().lastIndexOf(' ') + 1, binaryOp.getLeftOperand().toString().indexOf('.'))).getVirtualReg();
                code.append("iload_").append(reg1).append(NL);
            }

            if (binaryOp.getRightOperand().toString().contains("LiteralElement"))
                code.append(generators.apply(binaryOp.getRightOperand()));
            else {
                var reg2 = this.currentMethod.getVarTable().get(binaryOp.getRightOperand().toString().substring(binaryOp.getRightOperand().toString().lastIndexOf(' ') + 1, binaryOp.getRightOperand().toString().indexOf('.'))).getVirtualReg();
                code.append("iload_").append(reg2).append(NL);
            }
            code.append("isub").append(NL);
        } else {
            // load values on the left and on the right
            code.append(generators.apply(binaryOp.getLeftOperand()));
            code.append(generators.apply(binaryOp.getRightOperand()));

            // apply operation
            var op = switch (binaryOp.getOperation().getOpType()) {
                case ADD -> "iadd";
                case MUL -> "imul";
                case SUB -> "isub";
                case DIV -> "idiv";
                default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
            };
            code.append(op).append(NL);
        }

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        System.out.println("GetField: " + getFieldInstruction);
        var code = new StringBuilder();

        var className = ollirResult.getOllirClass().getClassName();

        if (getFieldInstruction.getOperands().get(1).getType().toString().contains("OBJECTREF")) {
            for (var imp : ollirResult.getOllirClass().getImports()) {
                if (imp.contains(getFieldInstruction.getOperands().get(1).getType().toString().substring(getFieldInstruction.getOperands().get(1).getType().toString().indexOf("(") + 1, getFieldInstruction.getOperands().get(1).getType().toString().indexOf(")")))) {
                    var split = imp.split("\\.");
                    className = String.join(".", split);
                }
            }
        }

        code.append("aload_0").append(NL);

        var type = getFieldType(getFieldInstruction.getFieldType().toString());

        code.append("getfield ").append(className).append(".").append(getFieldInstruction.getField().getName()).append(" ").append(type).append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        System.out.println("PutField: " + putFieldInstruction);

        if (stackLimit < 2) {
            stackLimit = 2;
        }

        var code = new StringBuilder();

        var className = ollirResult.getOllirClass().getClassName();

        if (putFieldInstruction.getOperands().get(1).getType().toString().contains("OBJECTREF")) {
            for (var imp : ollirResult.getOllirClass().getImports()) {
                if (imp.contains(putFieldInstruction.getOperands().get(1).getType().toString().substring(putFieldInstruction.getOperands().get(1).getType().toString().indexOf("(") + 1, putFieldInstruction.getOperands().get(1).getType().toString().indexOf(")")))) {
                    var split = imp.split("\\.");
                    className = String.join(".", split);
                }
            }
        }


        code.append("aload_0").append(NL);

        var value = putFieldInstruction.getValue();

        code.append(generators.apply(value));


        System.out.println("Field Type: " + putFieldInstruction.getField().getName()); //might not be the best way, what the I after the putfield instruction refer to?

        var type = getFieldType(putFieldInstruction.getOperands().get(1).getType().toString());

        code.append("putfield ").append(className).append(".").append(putFieldInstruction.getField().getName()).append(" ").append(type).append(NL);

        return code.toString();
    }


    private String generateCall(CallInstruction callInstruction) {
        System.out.println("Call: " + callInstruction);
        var code = new StringBuilder();
        var className = ollirResult.getOllirClass().getClassName();
        if (callInstruction.getCaller().getType().toString().contains("OBJECTREF")) {
            className = callInstruction.getCaller().getType().toString().substring(callInstruction.getCaller().getType().toString().indexOf('(') + 1, callInstruction.getCaller().getType().toString().indexOf(')'));
        }
        var imports = this.ollirResult.getOllirClass().getImports();
        if (callInstruction.getCaller().getType().toString().contains("OBJECTREF")) {
            for (var imp : imports) {
                if (imp.contains(callInstruction.getCaller().getType().toString().substring(callInstruction.getCaller().getType().toString().indexOf('(') + 1, callInstruction.getCaller().getType().toString().indexOf(')'))) && !imp.contains(ollirResult.getOllirClass().getClassName())) {
                    var split = imp.split("\\.");
                    className = String.join(".", split);
                }
            }
        }

        // detect wether its necessary a invokespecial or a invokevirtual

        String callerName = callInstruction.getCaller().toString().substring(callInstruction.getCaller().toString().indexOf(' ') + 1, callInstruction.getCaller().toString().indexOf('.'));
        var params = generateParams((ArrayList<Element>) callInstruction.getArguments());
        if (callInstruction.getInvocationType().toString().equals("invokevirtual")) {
            String literal = callInstruction.getMethodName().toString().substring(callInstruction.getMethodName().toString().indexOf('"') + 1, callInstruction.getMethodName().toString().lastIndexOf('"'));

            if (callerName.equals("this")) {
                code.append("aload_0").append(NL);
            } else {
                var reg = currentMethod.getVarTable().get(callerName).getVirtualReg();
                var instruction = getLoadInstruction(callInstruction.getCaller().getType().toString(), reg);
                code.append(instruction).append(reg).append(NL);
            }

            int paramCount = callInstruction.getArguments().size();
            if (paramCount + 1 > stackLimit) stackLimit = paramCount + 1;

            for (var param : callInstruction.getArguments()) {
                if (param.toString().contains("LiteralElement")) {
                    code.append(generators.apply(param));
                    continue;
                }
                String paramName = param.toString().substring(param.toString().indexOf(' ') + 1, param.toString().indexOf('.'));
                var reg = currentMethod.getVarTable().get(paramName).getVirtualReg();

                var instruction = getLoadInstruction(param.getType().toString(), reg);
                code.append(instruction).append(reg).append(NL);
            }

            code.append("invokevirtual ").append(className).append(".").append(literal);

            var returnType = getReturnType(callInstruction.getReturnType().toString());

            code.append("(").append(params).append(")").append(returnType).append(NL);
        } else if (callInstruction.getInvocationType().toString().equals("invokespecial")) {
            if (callerName.equals("this")) {
                code.append("aload_0").append(NL);
            } else {
                var reg = currentMethod.getVarTable().get(callerName).getVirtualReg();
                var instruction = getLoadInstruction(callInstruction.getCaller().getType().toString(), reg);
                code.append(instruction).append(reg).append(NL);
            }

            code.append("invokespecial ").append(className).append(".").append("<init>()V").append(NL);
        } else if (callInstruction.getInvocationType().toString().equals("NEW")
                && !callInstruction.getReturnType().toString().equals("INT32[]"))
            code.append("new ").append(className).append(NL);
        else if (callInstruction.getInvocationType().toString().equals("invokestatic")) {
            String literal = callInstruction.getMethodName().toString().substring(callInstruction.getMethodName().toString().indexOf('"') + 1, callInstruction.getMethodName().toString().lastIndexOf('"'));

            int paramCount = callInstruction.getArguments().size();
            if (paramCount + 1 > stackLimit) stackLimit = paramCount + 1;

            for (var param : callInstruction.getArguments()) {
                if (param.toString().contains("LiteralElement")) {
                    code.append(generators.apply(param));
                    continue;
                }
                String paramName = param.toString().substring(param.toString().indexOf(' ') + 1, param.toString().indexOf('.'));
                var reg = currentMethod.getVarTable().get(paramName).getVirtualReg();

                var instruction = getLoadInstruction(param.getType().toString(), reg);

                code.append(instruction).append(reg).append(NL);
            }

            code.append("invokestatic ").append(callerName).append("/").append(literal);

            var returnType = getReturnType(callInstruction.getReturnType().toString());

            code.append("(").append(params).append(")").append(returnType).append(NL);
        } else if (callInstruction.getInvocationType().toString().equals("invokeinterface"))
            System.out.println("todo");
        else
            System.out.println("Error: Invocation type not found");

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        System.out.println(returnInst);
        if (returnInst.getOperand() == null)
            code.append("return").append(NL);
        else {
            var visit = generators.apply(returnInst.getOperand());
            if (visit.contains("load")) {
                var locals = Integer.parseInt(String.valueOf(visit.charAt(visit.length() - 2)));
                if (locals >= this.localsLimit)
                    this.localsLimit = locals;
            }
            code.append(visit);
            var type = switch (returnInst.getOperand().getType().toString()) {
                case "INT32", "BOOLEAN" -> "i";
                default -> "a";
            };
            code.append(type).append("return").append(NL);
        }

        return code.toString();
    }

    private StringBuilder generateParams(ArrayList<Element> paramList) {
        var imports = this.ollirResult.getOllirClass().getImports();
        var params = new StringBuilder();
        String paramS = "";
        for (var param : paramList) {
            if (param.getType().toString().contains("OBJECTREF")) {
                paramS = param.getType().toString().substring(param.getType().toString().indexOf('(') + 1, param.getType().toString().indexOf(')'));

                for (var imp : imports) {
                    if (imp.contains(paramS)) {
                        var split = imp.split("\\.");
                        paramS = String.join(".", split);
                    }
                }
            }

            System.out.println("Param: " + param.getType());
            switch (param.getType().toString()) {
                case "INT32" -> params.append("I");
                case "BOOLEAN" -> params.append("Z");
                case "STRING" -> params.append("Ljava/lang/String;");
                case "INT32[]" -> params.append("[I");
                case "BOOLEAN[]" -> params.append("[Z");
                case "STRING[]" -> params.append("[Ljava/lang/String;");
                default -> params.append("L").append(paramS).append(";");
            }
        }
        return params;
    }

    private String generateSingleOpCondInstruction(SingleOpCondInstruction singleOpCondInstruction) {
        System.out.println("SingleOpCondInstruction: " + singleOpCondInstruction);
        var code = new StringBuilder();

        if (stackLimit < 1) {
            stackLimit = 1;
        }

        var operand = singleOpCondInstruction.getOperands().get(0);

        for (var inst: currentMethod.getInstructions()) {
            if (inst.toString().contains("ASSIGN " + operand.toString())) {
                var ifNumber = singleOpCondInstruction.getLabel().substring(singleOpCondInstruction.getLabel().indexOf('_') + 1);
                code.append("iflt ").append("cmp_lt_").append(ifNumber).append("_true").append(NL);
                code.append("iconst_0").append(NL);
                code.append("goto cmp_lt_").append(ifNumber).append("_end").append(NL);
                code.append("cmp_lt_").append(ifNumber).append("_true:").append(NL);
                code.append("iconst_1").append(NL);
                code.append("cmp_lt_").append(ifNumber).append("_end:").append(NL);

                var regNum = this.currentMethod.getVarTable().size();

                if (regNum >= this.localsLimit) this.localsLimit = regNum;

                code.append("istore_").append(regNum).append(NL);
                code.append("iload_").append(regNum).append(NL);
                code.append("ifne ").append(singleOpCondInstruction.getLabel()).append(NL);
                return code.toString();
            }
        }

        if(singleOpCondInstruction.toString().contains("NOPER")) {
            code.append("iconst_").append(singleOpCondInstruction.getOperands().get(0).toString(), singleOpCondInstruction.getOperands().get(0).toString().lastIndexOf(' ') + 1, singleOpCondInstruction.getOperands().get(0).toString().indexOf('.')).append(NL);
            code.append("ifne ").append(singleOpCondInstruction.getLabel()).append(NL);
        }

        return code.toString();
    }

    private String generateOpCondInstruction(OpCondInstruction opCondInstruction) {
        System.out.println("OpCondInstruction: " + opCondInstruction);
        var code = new StringBuilder();

        if (stackLimit < 1) {
            stackLimit = 1;
        }

        if (opCondInstruction.toString().contains("LTH")) {
            if (opCondInstruction.getOperands().get(0).toString().contains("LiteralElement"))
                code.append(generators.apply(opCondInstruction.getOperands().get(0)));
            else {
                var reg1 = this.currentMethod.getVarTable().get(opCondInstruction.getOperands().get(0).toString().substring(opCondInstruction.getOperands().get(0).toString().lastIndexOf(' ') + 1, opCondInstruction.getOperands().get(0).toString().indexOf('.'))).getVirtualReg();
                code.append("iload_").append(reg1).append(NL);

            }

            if (opCondInstruction.getOperands().get(1).toString().contains("LiteralElement"))
                code.append(generators.apply(opCondInstruction.getOperands().get(1)));
            else {
                var reg2 = this.currentMethod.getVarTable().get(opCondInstruction.getOperands().get(1).toString().substring(opCondInstruction.getOperands().get(1).toString().lastIndexOf(' ') + 1, opCondInstruction.getOperands().get(1).toString().indexOf('.'))).getVirtualReg();
                code.append("iload_").append(reg2).append(NL);
            }

            code.append("isub").append(NL);

            var ifNumber = opCondInstruction.getLabel().substring(opCondInstruction.getLabel().indexOf('_') + 1);
            code.append("iflt ").append("cmp_lt_").append(ifNumber).append("_true").append(NL);
            code.append("iconst_0").append(NL);
            code.append("goto cmp_lt_").append(ifNumber).append("_end").append(NL);
            code.append("cmp_lt_").append(ifNumber).append("_true:").append(NL);
            code.append("iconst_1").append(NL);
            code.append("cmp_lt_").append(ifNumber).append("_end:").append(NL);

            var regNum = this.currentMethod.getVarTable().size();

            if (regNum >= this.localsLimit) this.localsLimit = regNum;

            code.append("istore_").append(regNum).append(NL);
            code.append("iload_").append(regNum).append(NL);
            code.append("ifne ").append(opCondInstruction.getLabel()).append(NL);
        }
        else if (true) { // OTHER OPERATIONS

        }

        return code.toString();
    }

    private String generateGoToInstruction(GotoInstruction gotoInstruction) {
        System.out.println("GoToInstruction: " + gotoInstruction);

        return "goto " + gotoInstruction.getLabel() + NL;
    }

    private String generateArrayOperand(ArrayOperand arrayOperand) {
        System.out.println("ArrayOperand: " + arrayOperand);
        var code = new StringBuilder();

        if (stackLimit < 1) {
            stackLimit = 1;
        }

        var name = arrayOperand.toString().substring(arrayOperand.toString().indexOf(":") + 1, arrayOperand.toString().indexOf("[")).strip();
        var indexArray = arrayOperand.getIndexOperands().get(0);

        var reg = this.currentMethod.getVarTable().get(name).getVirtualReg();
        code.append("aload_").append(reg).append(NL);
        code.append(generators.apply(indexArray));

        return code.toString();
    }

    private String getReturnType(String returnType) {
        var imports = this.ollirResult.getOllirClass().getImports();
        if (returnType.contains("OBJECTREF")) {
            returnType = returnType.substring(returnType.indexOf('(') + 1, returnType.indexOf(')'));

            for (var imp : imports) {
                if (imp.contains(returnType)) {
                    var split = imp.split("\\.");
                    // Join using "/" except for the last part
                    returnType = String.join("/", Arrays.copyOf(split, split.length - 1))
                            + "/" + split[split.length - 1];
                }
            }
        }

        return switch (returnType) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            case "VOID" -> "V";
            default -> "L" + returnType + ";";
        };
    }

    private String getFieldType(String fieldInstructionType) {
        var imports = this.ollirResult.getOllirClass().getImports();
        if (fieldInstructionType.contains("OBJECTREF")) {
            fieldInstructionType = fieldInstructionType.substring(fieldInstructionType.indexOf('(') + 1, fieldInstructionType.indexOf(')'));

            for (var imp : imports) {
                if (imp.contains(fieldInstructionType)) {
                    var split = imp.split("\\.");
                    fieldInstructionType = String.join(".", split);
                }
            }
        }


        return switch (fieldInstructionType) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            default -> "L" + fieldInstructionType + ";";
        };
    }

    private String getStoreInstruction(String type, int reg) {
        return switch (type) {
            case "INT32", "BOOLEAN" -> reg > 3 ? "istore " : "istore_";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            default -> reg > 3 ? "astore " : "astore_";
        };
    }

    private String getLoadInstruction(String type, int reg) {
        return switch (type) {
            case "INT32", "BOOLEAN" -> reg > 3 ? "iload " : "iload_";
            case "STRING" -> "Ljava/lang/String;";
            case "INT[]" -> "[I";
            case "BOOLEAN[]" -> "[Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            default -> reg > 3 ? "aload " : "aload_";
        };
    }
}
