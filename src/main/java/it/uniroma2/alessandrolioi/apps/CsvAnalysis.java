package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.Analysis;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;

import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        try {
            Analysis analysis = new Analysis("bookkeeperDataset6r.csv");
            var entries = analysis.entries();
            entries.keySet().forEach(version -> logger.info("%d, %d%n".formatted(version, entries.get(version).size())));
        } catch (CsvException e) {
            logger.severe(e.getMessage());
        }
    }
}
