package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.passes.*;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import java.util.ArrayList;
import java.util.List;

public class JmmAnalysisImpl implements JmmAnalysis {

    private final List<AnalysisPass> analysisPasses;

    public JmmAnalysisImpl() {
        this.analysisPasses = List.of(
                //new UndeclaredVariable(),
                //new CheckIdentifiers(),
                new Analyser()
                );
    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        JmmNode rootNode = parserResult.getRootNode();

        // Build symbol table
        SymbolTable table = JmmSymbolTableBuilder.build(rootNode);

        // List to hold semantic analysis reports
        List<Report> reports = new ArrayList<>();

        // Perform semantic analysis passes
        for (var analysisPass : analysisPasses) {
            try {
                List<Report> passReports = analysisPass.analyze(rootNode, table);
                reports.addAll(passReports);
            } catch (Exception e) {
                // Add error report if an exception occurs during analysis pass execution
                reports.add(Report.newError(Stage.SEMANTIC,
                        -1,
                        -1,
                        "Problem while executing analysis pass '" + analysisPass.getClass() + "'",
                        e)
                );
            }
        }

        // Return semantics result with parser result, symbol table, and reports
        return new JmmSemanticsResult(parserResult, table, reports);
    }
}
