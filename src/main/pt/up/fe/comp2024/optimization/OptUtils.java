package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OptUtils {
    private static int tempNumber = -1;

    private static int tempArrayNumber = -1;

    public static String getTemp() {
        return getTemp("tmp");
    }

    public static String getTempArray() {
        return String.valueOf(getNextTempArrayNum());
    }

    public static String getTemp(String prefix) {
        return prefix + getNextTempNum();
    }


    public static int getNextTempNum() {
        tempNumber += 1;
        return tempNumber;
    }

    public static int getNextTempArrayNum() {
        tempArrayNumber += 1;
        return tempArrayNumber;
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
            case "int[]", "int..." -> "array.i32";
            case "String" -> "string";
            case "void" -> "V";
            default -> typeName;
        };
    }
}
