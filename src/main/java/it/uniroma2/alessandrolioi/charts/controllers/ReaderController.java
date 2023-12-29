package it.uniroma2.alessandrolioi.charts.controllers;

import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.charts.exceptions.ReaderException;
import it.uniroma2.alessandrolioi.charts.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.DatasetPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReaderController {
    public List<CsvEntry> readCsv(String project) throws ReaderException {
        try {
            List<CsvEntry> entries = new ArrayList<>();
            Path path = DatasetPaths.fromProject(project).resolve("reports.csv");
            List<String> lines = Files.readAllLines(path);
            for (int i = 1; i < lines.size(); i++) {
                String[] line = lines.get(i).split(",");
                int version = Integer.parseInt(line[1]);
                AnalysisType.Classifiers classifier = AnalysisType.Classifiers.valueOf(line[2]);
                AnalysisType.FeatureSelection featureSelection = AnalysisType.FeatureSelection.valueOf(line[3]);
                AnalysisType.Sampling sampling = AnalysisType.Sampling.valueOf(line[4]);
                double precision = Double.parseDouble(line[5]);
                double recall = Double.parseDouble(line[6]);
                double kappa = Double.parseDouble(line[7]);
                double auc = Double.parseDouble(line[8]);
                CsvEntry entry = new CsvEntry(version, classifier, featureSelection, sampling, precision, recall, kappa, auc);
                entries.add(entry);
            }
            return entries;
        } catch (IOException e) {
            throw new ReaderException("Could not read reports.csv", e);
        }
    }
}
