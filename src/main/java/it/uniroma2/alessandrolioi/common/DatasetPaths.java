package it.uniroma2.alessandrolioi.common;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DatasetPaths {
    private DatasetPaths() {
        throw new IllegalStateException("Utility class");
    }

    // TODO change to results/{project}
    public static Path fromProject(String project) {
        return Paths.get("dataset", project);
    }
}
