package it.uniroma2.alessandrolioi.common;

public class Projects {
    private Projects() {
        throw new IllegalStateException("Utility class");
    }

    public static String[] names() {
        return new String[]{"bookkeeper", "avro"};
    }

    public static String[] coldStarts() {
        return new String[]{"openjpa", "storm", "zookeeper", "syncope", "tajo"};
    }
}
