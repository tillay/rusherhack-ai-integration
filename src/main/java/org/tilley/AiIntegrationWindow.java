package org.tilley;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.render.graphic.VectorGraphic;
import org.rusherhack.client.api.ui.window.content.ComboContent;
import org.rusherhack.client.api.ui.window.content.component.ButtonComponent;
import org.rusherhack.client.api.ui.window.content.component.TextFieldComponent;
import org.rusherhack.client.api.ui.window.view.RichTextView;
import org.rusherhack.client.api.ui.window.view.TabbedView;
import org.rusherhack.client.api.ui.window.view.WindowView;
import org.rusherhack.client.api.utils.ChatUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.tilley.TokenCommand.get_token;
import static org.tilley.TokenCommand.get_url;

public class AiIntegrationWindow extends ResizeableWindow {
    String API_KEY = get_token();
    String API_URL = get_url();

    JsonArray messages = new JsonArray();
    JsonObject systemMessage = new JsonObject();
    boolean aiBusy = false;

    private final TabbedView rootView;
    private final RichTextView aiView;
    private final TextFieldComponent inputBox;

    public AiIntegrationWindow() {
        super("AI Integration", 200, 100, 400, 300);
        this.aiView = new RichTextView("ai", this);
        final ComboContent inputCombo = new ComboContent(this);
        inputBox = new TextFieldComponent(this, "Ask anything", this.getWidth());
        inputBox.setValue("");
        inputBox.setReturnCallback(this::handleInput);
        final ButtonComponent sendPromptButton = new ButtonComponent(this, " ↑ ", () -> handleInput(inputBox.getValue()));
        inputCombo.addContent(inputBox, ComboContent.AnchorSide.LEFT);
        inputCombo.addContent(sendPromptButton);
        this.rootView = new TabbedView(this, List.of(this.aiView, inputCombo));
        try {
            this.setIcon(new VectorGraphic("ai_logo.svg", 64, 64));
        } catch (IOException e){
            ChatUtils.print(e.toString());
        }
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "Do not use emojis or markdown. All messages except for the last user are to be considered as context. The last user one is the only one to respond to.");
        messages.add(systemMessage);
    }

    private void addLine(String line) {
        if (line != null && !line.isEmpty()) aiView.add(line, -1);
    }

    private void handleInput(String input) {
        API_KEY = get_token();
        API_URL = get_url();

        if (API_KEY == null) {
            addLine("No ai token set! Set one using *ai token <token>");
        } else if (API_URL == null) {
            addLine("No api url set! Set one using *ai url <url>");
            addLine("Please make sure it's a streamer url!");
        } else if (!input.isEmpty() && !aiBusy) {
            addLine("> " + input);
            sendPromptToAi(input);
            inputBox.setValue("");
        }
    }

    public void addMessage(String sender, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", sender);
        msg.addProperty("content", content);
        messages.add(msg);
    }

    private void sendPromptToAi(String userPrompt) {
        new Thread(() -> {
            aiBusy = true;
            StringBuilder buffer = new StringBuilder();
            StringBuilder fullResponse = new StringBuilder();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject payload = new JsonObject();
                addMessage("user", userPrompt);

                payload.addProperty("model", "deepseek-chat");
                payload.add("messages", messages);
                payload.addProperty("stream", true);
                ChatUtils.print(payload.toString());
                conn.getOutputStream().write(payload.toString().getBytes());

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        String content = extractDelta(data);
                        if (content == null || content.isEmpty()) continue;
                        fullResponse.append(content);
                        for (char c : content.toCharArray()) {
                            if (c == '\n') {
                                addLine(buffer.toString());
                                buffer.setLength(0);
                            } else buffer.append(c);
                        }
                    }
                }
            } catch (Exception e) {
                addLine("Error: " + e.getMessage());
            }

            addMessage("assistant", fullResponse.toString());

            if (buffer.length() > 0) addLine(buffer.toString());
            aiBusy = false;
        }).start();
    }



    private static String extractDelta(String json) {
        try {
            var obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("delta")
                    .has("content")
                    ? obj.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("delta")
                    .get("content").getAsString()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public WindowView getRootView() {
        return this.rootView;
    }
}