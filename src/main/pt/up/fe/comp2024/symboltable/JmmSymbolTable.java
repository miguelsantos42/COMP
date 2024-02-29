package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {

    private final String className;
    private final String extendedClassName;
    private final List<String> imports;
    private final List<String> methods;
    private final Map<String, Type> methodReturnTypes;
    private final Map<String, List<Symbol>> methodParams;
    private final Map<String, List<Symbol>> methodLocalVariables;

    public JmmSymbolTable(String className,
                          String extendedClassName,
                          List<String> imports,
                          List<String> methods,
                          Map<String, Type> methodReturnTypes,
                          Map<String, List<Symbol>> methodParams,
                          Map<String, List<Symbol>> methodLocalVariables) {
        this.className = className;
        this.extendedClassName = extendedClassName;
        this.imports = imports;
        this.methods = methods;
        this.methodReturnTypes = methodReturnTypes;
        this.methodParams = methodParams;
        this.methodLocalVariables = methodLocalVariables;
    }

    @Override
    public List<String> getImports() {
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
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
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
