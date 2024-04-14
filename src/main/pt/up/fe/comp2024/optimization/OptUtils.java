package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {
        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {
        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {
        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {
        //TYPE.checkOrThrow(typeNode);
        return toOllirType(typeNode.get("name"));
    }

    public static String toOllirType(Type type) {
        if(type.isArray())
            return toOllirType(type.getName() + "[]");
        return toOllirType(type.getName());
    }

    private static String toOllirType(String typeName) {
        return "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "int[]" -> "array.i32";
            case "boolean[]" -> "array.bool";
            case "void" -> ".V";
            case "string" -> "string";
            default -> typeName;
        };
    }
}
