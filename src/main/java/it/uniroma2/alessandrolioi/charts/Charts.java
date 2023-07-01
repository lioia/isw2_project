package it.uniroma2.alessandrolioi.charts;

import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.charts.controllers.ReaderController;
import it.uniroma2.alessandrolioi.charts.exceptions.ReaderException;
import it.uniroma2.alessandrolioi.charts.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.DatasetPaths;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Charts {
    private final String project;
    private final List<CsvEntry> entries;

    public Charts(String project) throws ReaderException {
        ReaderController controller = new ReaderController();
        this.project = project;
        this.entries = controller.readCsv(project);
    }

    public void generateClassifierComparison() throws IOException {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
        populateDatasets(datasets, entries, "");
        createImages("Classifier", "classifier", datasets);
    }

    public void generateFeatureSelectionComparison() throws IOException {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
        for (AnalysisType.FeatureSelection featureSelection : AnalysisType.FeatureSelection.values()) {
            List<CsvEntry> filter = entries.stream().filter(e -> e.featureSelection() == featureSelection).toList();
            populateDatasets(datasets, filter, featureSelection.name());
        }
        createImages("Feature Selection", "feature_selection", datasets);
    }

    public void generateSamplingComparison() throws IOException {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
        for (AnalysisType.Sampling sampling : AnalysisType.Sampling.values()) {
            List<CsvEntry> filter = entries.stream().filter(e -> e.sampling() == sampling).toList();
            populateDatasets(datasets, filter, sampling.name());
        }
        createImages("Sampling", "sampling", datasets);
    }

    public void generateCostSensitiveComparison() throws IOException {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
        for (AnalysisType.CostSensitive costSensitive : AnalysisType.CostSensitive.values()) {
            List<CsvEntry> filter = entries.stream().filter(e -> e.costSensitive() == costSensitive).toList();
            populateDatasets(datasets, filter, costSensitive.name());
        }
        createImages("Cost Sensitive", "cost_sensitive", datasets);
    }

    private Map<String, DefaultBoxAndWhiskerCategoryDataset> createEmptyDatasets() {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = new HashMap<>();
        datasets.put("Precision", new DefaultBoxAndWhiskerCategoryDataset());
        datasets.put("Recall", new DefaultBoxAndWhiskerCategoryDataset());
        datasets.put("AUC", new DefaultBoxAndWhiskerCategoryDataset());
        datasets.put("Kappa", new DefaultBoxAndWhiskerCategoryDataset());
        return datasets;
    }

    private void populateDatasets(Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets, List<CsvEntry> filteredEntries, String column) {
        List<Double> precisionValues = new ArrayList<>();
        List<Double> recallValues = new ArrayList<>();
        List<Double> aucValues = new ArrayList<>();
        List<Double> kappaValues = new ArrayList<>();
        for (AnalysisType.Classifiers classifier : AnalysisType.Classifiers.values()) {
            for (CsvEntry entry : filteredEntries) {
                if (entry.classifier() != classifier) continue;
                precisionValues.add(entry.precision());
                recallValues.add(entry.recall());
                aucValues.add(entry.auc());
                kappaValues.add(entry.kappa());
            }
            datasets.get("Precision").add(precisionValues, classifier.name(), column);
            datasets.get("Recall").add(recallValues, classifier.name(), column);
            datasets.get("AUC").add(aucValues, classifier.name(), column);
            datasets.get("Kappa").add(kappaValues, classifier.name(), column);
        }
    }

    private void createImages(String category, String folder, Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets) throws IOException {
        for (Map.Entry<String, DefaultBoxAndWhiskerCategoryDataset> entry : datasets.entrySet()) {
            JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                    "%s Comparison".formatted(category), category, entry.getKey(),
                    entry.getValue(), true);
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
            renderer.setMeanVisible(false);
            BufferedImage precisionImage = chart.createBufferedImage(800, 800);
            Path folderPath = DatasetPaths.fromProject(project).resolve(folder);
            if (!Files.exists(folderPath)) Files.createDirectory(folderPath);
            Path path = folderPath.resolve("%s.png".formatted(entry.getKey()));
            ImageIO.write(precisionImage, "png", path.toFile());
        }
    }
}
