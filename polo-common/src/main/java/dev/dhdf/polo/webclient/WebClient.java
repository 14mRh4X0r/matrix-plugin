package dev.dhdf.polo.webclient;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.types.ChatResult;
import dev.dhdf.polo.types.MCMessage;
import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.types.VibeCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * This is Polo. It interacts with Marco to establish a bridge with a room.
 * Polo periodically sends GET requests to see all the new messages in the
 * Matrix room. It also sends POST requests to Marco including all the new
 * events that occurred (which is only chat messages at the moment)
 */
public class WebClient {
    private static final Gson GSON = new Gson();
    private final String address;
    private final int port;
    private final String token;

    private final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private final PoloPlugin plugin;

    public WebClient(PoloPlugin plugin, Config config) {
        this.address = config.address;
        this.port = config.port;
        this.token = config.token;
        this.plugin = plugin;
    }

    /**
     * Send new chat messages to Marco
     *
     * @param player Player object representing a Minecraft player, it
     *                 must be parsed before sent to Marco
     * @param context  The body of the message
     */
    public void postChat(PoloPlayer player, String context) {
        MCMessage message = new MCMessage(player, context);

        this.doRequest(
                "POST",
                "/chat",
                message,
                void.class
        );
    }

    /**
     * Get new messages from Marco and the Matrix room
     */
    public void getChat() {
        ChatResult chatResponse = this.doRequest(
                "GET",
                "/chat",
                null,
                ChatResult.class
        );
        if (chatResponse == null)
            return;

        // Send all the new messages to the minecraft chat
        for (String message : chatResponse.getChat()) {
            onRoomMessage(message);
        }
    }

    public void onRoomMessage(String message) {
        this.plugin.broadcastMessage(message);
    }

    /**
     * See if we're connecting to Marco properly / the token we have is
     * valid
     *
     * @return boolean
     */
    public boolean vibeCheck() {
        try {
            VibeCheckResult check = this.doRequest(
                    "GET",
                    "/vibeCheck",
                    null,
                    VibeCheckResult.class
            );

            return check.isOK();

        } catch (NullPointerException err) {
            return false;
        }
    }

    public <T> T doRequest(String method, String endpoint, Object body, Class<T> resultType) {
        try {
            URL url = new URL(
                    "http://" + address + ":" + port + endpoint
            );

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + this.token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Marco Plugin");

            if (!method.equals("GET") && body != null) {
                connection.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                GSON.toJson(body, writer);
                writer.flush();
                writer.close();
            }

            InputStream stream = connection.getErrorStream();
            int resCode = connection.getResponseCode();

            if (resCode != 404) {
                if (stream == null)
                    stream = connection.getInputStream();

                return GSON.fromJson(new InputStreamReader(stream), resultType);
            } else {
                logger.error("An invalid endpoint was called for.");
                return null;
            }
        } catch (java.net.ConnectException e) {
            logger.warn(e.getMessage());
            return null;
        } catch (IOException | JsonParseException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }
}
