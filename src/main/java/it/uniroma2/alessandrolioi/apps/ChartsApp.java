package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.charts.Charts;
import it.uniroma2.alessandrolioi.common.Projects;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChartsApp {
    private static final Logger logger = Logger.getLogger("Charts");

    public static void main(String[] args) {
        for (String project : Projects.names()) {
            try {
                Charts charts = new Charts(project);
                charts.generateNoFilterComparison();
                charts.generateFeatureSelectionComparison();
                charts.generateSamplingComparison();
                charts.generateCostSensitiveComparison();
            } catch (Exception e) {
                if (logger.isLoggable(Level.SEVERE))
                    logger.severe(e.getMessage());
            }
        }
    }
}
