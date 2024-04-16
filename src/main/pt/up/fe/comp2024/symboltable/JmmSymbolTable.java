package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {

    private final String className;
    private final List<String> methods;
    private final String parent;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;

    private final List<String> imports;

    private final List<Symbol> fields;
    private final Map<String, List<Symbol>> locals;
    private final Map<String, Boolean> varArgs;

    public JmmSymbolTable(String className,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          List<Symbol> fields,
                          Map<String, List<Symbol>> locals,
                          List<String> imports,
                          Map<String, Boolean> varArgs,
                          String parent) {
        this.parent=parent;
        this.className = className;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        this.fields = fields;
        this.imports = imports;
        this.varArgs = varArgs;
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return this.parent;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return this.methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return this.returnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return this.locals.get(methodSignature);
    }
    public Boolean getVarArgs(String methodSignature){
        return this.varArgs.get(methodSignature);
    }
}
