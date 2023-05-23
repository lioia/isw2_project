package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.boundaries.Analysis;
import it.uniroma2.alessandrolioi.analysis.boundaries.CsvToArff;
import it.uniroma2.alessandrolioi.analysis.boundaries.ReportHandler;
import it.uniroma2.alessandrolioi.analysis.models.Report;
import it.uniroma2.alessandrolioi.common.Projects;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        for (String project : Projects.names) {
            if (logger.isLoggable(Level.INFO))
                logger.info("Analysis: %s".formatted(project));
            try {
                List<Report> reports = new ArrayList<>();
                CsvToArff reader = new CsvToArff(project);
                int numberOfReleases = reader.getTotalReleases();
                reader.convertToArff();
                for (int i = 2; i < numberOfReleases; i++) {
                    if (logger.isLoggable(Level.INFO))
                        logger.info("\tRelease: %d".formatted(i));
                    Analysis analysis = new Analysis(project, i);
                    reports.addAll(analysis.performAnalysis());
                }
                ReportHandler report = new ReportHandler(project, reports);
                report.writeToFile();
            } catch (Exception e) {
                if (logger.isLoggable(Level.SEVERE))
                    logger.severe(e.getMessage());
            }
        }
    }
}
