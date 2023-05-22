package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.ReportWriterException;
import it.uniroma2.alessandrolioi.analysis.models.Report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReportController {
    public void writeReports(String project, List<Report> reports) throws ReportWriterException {
        List<String> reportsString = reports.stream().map(r -> r.toCsvString(project)).toList();
        String text = "Project,#TrainingRelease,Classifier,FeatureSelection,Sampling,CostSensitive,Precision,Recall,Kappa,AUC\n%s"
                .formatted(String.join("\n", reportsString));
        Path path = Paths.get("dataset", project, "reports.csv");
        try {
            Files.write(path, text.getBytes());
        } catch (IOException e) {
            throw new ReportWriterException(e);
        }
    }
}
