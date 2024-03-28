package pt.up.fe.comp2024.utils;

import pt.up.fe.comp.jmm.ast.JmmNode;

public class Comparator {
    public static  boolean isArr(JmmNode node){
        return node.get("array").equals("true");
    }
}
