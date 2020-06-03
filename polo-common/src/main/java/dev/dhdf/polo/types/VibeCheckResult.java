package dev.dhdf.polo.types;

public class VibeCheckResult {
    private String status;

    public boolean isOK() {
        return status.equals("OK");
    }
}
