package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {

    private String className;
    private String extendedClassName;
    private ArrayList<String> imports;
    private ArrayList<String> methods;
    private HashMap<String, Type> methodReturnTypes;
    private HashMap<String, List<Symbol>> methodParams;
    private HashMap<String, List<Symbol>> methodLocalVariables;

    public JmmSymbolTable(String className,
                          String extendedClassName,
                          ArrayList<String> imports,
                          ArrayList<String> methods,
                          HashMap<String, Type> methodReturnTypes,
                          HashMap<String, List<Symbol>> methodParams,
                          HashMap<String, List<Symbol>> methodLocalVariables) {
        this.className = className;
        this.extendedClassName = extendedClassName;
        this.imports = imports;
        this.methods = methods;
        this.methodReturnTypes = methodReturnTypes;
        this.methodParams = methodParams;
        this.methodLocalVariables = methodLocalVariables;
    }

    @Override
    public ArrayList<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.extendedClassName;
    }

    @Override
    public List<Symbol> getFields() {
        throw new NotImplementedException();
    }

    @Override
    public ArrayList<String> getMethods() {
        return null; //Collections.unmodifiableList(methods);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        // TODO: Simple implementation that needs to be expanded
        return new Type(TypeUtils.getIntTypeName(), false);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(methodParams.get(methodSignature));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(methodLocalVariables.get(methodSignature));
    }

}
