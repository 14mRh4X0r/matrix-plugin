package dev.dhdf.polo.types;

public class MCMessage {
    public final PoloPlayer player;
    public final String message;

    public MCMessage(PoloPlayer player, String message) {
        this.player = player;
        this.message = message;
    }
}
