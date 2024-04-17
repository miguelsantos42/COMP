package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.passes.*;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class JmmAnalysisImpl implements JmmAnalysis {


    private final List<AnalysisPass> analysisPasses;

    public JmmAnalysisImpl() {

        this.analysisPasses = List.of(
                new UndeclaredVariable(),
                new UndeclaredMethod(),
                new IntLit(),
                new ReturnType(),
                new StaticMethod()
        );

    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();
        JmmNode rootNode = parserResult.getRootNode();
        JmmSymbolTableBuilder builder = null;
        try {
            builder = new JmmSymbolTableBuilder(rootNode);
            var tableReports = builder.getReports();
            reports.addAll(tableReports);
        } catch (Exception e) {
            reports.add(Report.newError(Stage.SEMANTIC,
                    -1,
                    -1,
                    "Problem while building symbol table '",
                    e)
            );
        }
        assert builder != null;
        JmmSymbolTable table = builder.getTable();

        if(!reports.isEmpty()){
            return new JmmSemanticsResult(parserResult, table, reports);
        }

        System.out.println("\n\nPrinting Symbol Table:");

        System.out.println("\nImports:");
        System.out.println(table.getImports());

        System.out.println("\nClass Name: " + table.getClassName());

        System.out.println("\nSuper: " + table.getSuper());

        System.out.println("\nFields: " + table.getFields());

        System.out.println("\nMethods: " + table.getMethods());

        for (var method : table.getMethods()) {
            System.out.println("\nMethod: " + method);
            System.out.println("Return Type: " + table.getReturnType(method));
            System.out.println("Parameters: " + table.getParameters(method));
            System.out.println("Local Variables: " + table.getLocalVariables(method));
        }

        // Visit all nodes in the AST
        for (var analysisPass : analysisPasses) {
            try {
                var passReports = analysisPass.analyze(rootNode, table);
                reports.addAll(passReports);
            } catch (Exception e) {
                reports.add(Report.newError(Stage.SEMANTIC,
                        -1,
                        -1,
                        "Problem while executing analysis pass '" + analysisPass.getClass() + "'",
                        e)
                );
            }

        }

        return new JmmSemanticsResult(parserResult, table, reports);
    }
}