package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.boundaries.Analysis;
import it.uniroma2.alessandrolioi.analysis.boundaries.ReportHandler;
import it.uniroma2.alessandrolioi.analysis.models.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        try {
            String project = "bookkeeper";
            List<Report> reports = new ArrayList<>();
            int numberOfReleases = 6;
            for (int i = 2; i <= numberOfReleases; i++) {
                Analysis analysis = new Analysis(project, i);
                reports.addAll(analysis.performAnalysis());
            }
            ReportHandler report = new ReportHandler(project, reports);
            report.writeToFile();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            List<String> stackTraces = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).toList();
            logger.severe(String.join("\n", stackTraces));
        }
    }
}
