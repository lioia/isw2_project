package it.uniroma2.alessandrolioi.charts;

import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.charts.controllers.ReaderController;
import it.uniroma2.alessandrolioi.charts.exceptions.ReaderException;
import it.uniroma2.alessandrolioi.charts.extensions.CustomBoxAndWhiskerRenderer;
import it.uniroma2.alessandrolioi.charts.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.DatasetPaths;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class Charts {
    private final String project;
    private final List<CsvEntry> entries;

    public Charts(String project) throws ReaderException {
        ReaderController controller = new ReaderController();
        this.project = project;
        this.entries = controller.readCsv(project);
    }

    public void generateNoFilterComparison() throws IOException {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
        List<CsvEntry> filter = entries.stream().filter(e ->
                e.featureSelection() == AnalysisType.FeatureSelection.NONE &&
                        e.sampling() == AnalysisType.Sampling.NONE
        ).toList();
        populateDatasets(datasets, filter, "");
        createImages("fs_none", datasets);
    }

    public void generateFeatureSelectionComparison() throws IOException {
        Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
        List<CsvEntry> filter = entries.stream().filter(e ->
                e.featureSelection() == AnalysisType.FeatureSelection.BEST_FIRST &&
                        e.sampling() == AnalysisType.Sampling.NONE
        ).toList();
        populateDatasets(datasets, filter, AnalysisType.FeatureSelection.BEST_FIRST.name());
        createImages("fs_best_first", datasets);
    }

    public void generateSamplingComparison() throws IOException {
        for (AnalysisType.Sampling sampling : AnalysisType.Sampling.values()) {
            if (sampling == AnalysisType.Sampling.NONE) continue;
            Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets = createEmptyDatasets();
            List<CsvEntry> filter = entries.stream().filter(e ->
                    e.featureSelection() == AnalysisType.FeatureSelection.BEST_FIRST &&
                            e.sampling() == sampling
            ).toList();
            populateDatasets(datasets, filter, AnalysisType.Sampling.SMOTE.name());
            createImages("sampling_" + sampling.name().toLowerCase(), datasets);
        }
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

    private void createImages(String folder, Map<String, DefaultBoxAndWhiskerCategoryDataset> datasets) throws IOException {
        for (Map.Entry<String, DefaultBoxAndWhiskerCategoryDataset> entry : datasets.entrySet()) {
            JFreeChart chart = ChartFactory.createBoxAndWhiskerChart("", "", entry.getKey(), entry.getValue(), true);
            CustomBoxAndWhiskerRenderer renderer = new CustomBoxAndWhiskerRenderer();
            chart.getCategoryPlot().setRenderer(renderer);
            Font font = new Font("Tahoma", Font.PLAIN, 20);
            chart.getCategoryPlot().getRangeAxis().setLabelFont(font);
            BufferedImage image = chart.createBufferedImage(500, 500);
            Path folderPath = DatasetPaths.fromProject(project).resolve(folder);
            if (!Files.exists(folderPath)) Files.createDirectory(folderPath);
            Path path = folderPath.resolve("%s.png".formatted(entry.getKey()));
            ImageIO.write(image, "png", path.toFile());
        }
    }

    public void generateFinalImages() throws IOException {
        for (AnalysisType.Sampling sampling : AnalysisType.Sampling.values()) {
            if (sampling == AnalysisType.Sampling.NONE) continue;
            String folder = "sampling_%s".formatted(sampling.name().toLowerCase());
            Path path = DatasetPaths.fromProject(project).resolve(folder);
            combineImages(path);
        }
        for (AnalysisType.FeatureSelection featureSelection : AnalysisType.FeatureSelection.values()) {
            String folder = "fs_%s".formatted(featureSelection.name().toLowerCase());
            Path path = DatasetPaths.fromProject(project).resolve(folder);
            combineImages(path);
        }
    }

    private void combineImages(Path folder) throws IOException {
        BufferedImage precision = ImageIO.read(folder.resolve("Precision.png").toFile());
        BufferedImage recall = ImageIO.read(folder.resolve("Recall.png").toFile());
        BufferedImage auc = ImageIO.read(folder.resolve("AUC.png").toFile());
        BufferedImage kappa = ImageIO.read(folder.resolve("Kappa.png").toFile());
        BufferedImage output = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        g.drawImage(precision, 0, 0, null);
        g.drawImage(recall, 500, 0, null);
        g.drawImage(kappa, 0, 500, null);
        g.drawImage(auc, 500, 500, null);

        g.dispose();

        ImageIO.write(output, "png", folder.resolve("output.png").toFile());
    }
}
