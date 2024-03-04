package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {

    private final String className;
    private final String extendedClassName;
    private final ArrayList<String> imports;
    private final ArrayList<Symbol> fields;
    private final ArrayList<String> methods;
    private final HashMap<String, Type> methodReturnTypes;
    private final HashMap<String, List<Symbol>> methodParameters;
    private final HashMap<String, List<Symbol>> methodLocalVariables;

    public JmmSymbolTable(String className,
                          String extendedClassName,
                          ArrayList<String> imports, ArrayList<Symbol> fields,
                          ArrayList<String> methods,
                          HashMap<String, Type> methodReturnTypes,
                          HashMap<String, List<Symbol>> methodParameters,
                          HashMap<String, List<Symbol>> methodLocalVariables) {
        this.className = className;
        this.extendedClassName = extendedClassName;
        this.imports = imports;
        this.fields = fields;
        this.methods = methods;
        this.methodReturnTypes = methodReturnTypes;
        this.methodParameters = methodParameters;
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
        return this.fields;
    }

    @Override
    public ArrayList<String> getMethods() {
        return this.methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return this.methodReturnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return this.methodParameters.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return this.methodLocalVariables.get(methodSignature);
    }

}
