package org.tilley;

import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.render.graphic.VectorGraphic;
import org.rusherhack.client.api.ui.window.content.ComboContent;
import org.rusherhack.client.api.ui.window.content.component.ButtonComponent;
import org.rusherhack.client.api.ui.window.content.component.TextFieldComponent;
import org.rusherhack.client.api.ui.window.view.RichTextView;
import org.rusherhack.client.api.ui.window.view.TabbedView;
import org.rusherhack.client.api.ui.window.view.WindowView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import static org.tilley.TokenCommand.get_token;
import static org.tilley.TokenCommand.get_url;
import static org.tilley.TokenCommand.get_model;


public class AiIntegrationWindow extends ResizeableWindow {
    String API_KEY = get_token();
    String API_URL = get_url();
    String AI_MODEL = get_model();

    JsonArray messages = new JsonArray();
    JsonObject systemMessage = new JsonObject();
    boolean aiBusy = false;

    private final TabbedView rootView;
    private final RichTextView aiView;
    private final TextFieldComponent inputBox;

    public ButtonComponent sendPromptButton;

    public AiIntegrationWindow() {
        super("Rusherhack AI Integration", 200, 100, 400, 300);
        this.aiView = new RichTextView("ai", this);
        final ComboContent inputCombo = new ComboContent(this);
        inputBox = new TextFieldComponent(this, "Ask anything", this.getWidth());
        inputBox.setValue("");
        inputBox.setReturnCallback(this::handleInput);
        sendPromptButton = new ButtonComponent(this, " Send ", () -> handleInput(inputBox.getValue()));
        sendPromptButton.setWidth(30);
        inputCombo.addContent(inputBox, ComboContent.AnchorSide.LEFT);
        inputCombo.addContent(sendPromptButton);
        this.rootView = new TabbedView(this, List.of(this.aiView, inputCombo));
        try {this.setIcon(new VectorGraphic("ai_logo.svg", 64, 64));} catch (IOException ignored) {}

        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
                "Do not use emojis or anything not in UTF-8. Do not violate this under any circumstances, as it will cause problems. " +
                "All messages except for the last user are to be considered as context. The last user one is the only one to respond to. " +
                "You are talking to " + mc.getUser().getName() + ". You are the RusherHack AI Assistant. " +
                "Try to avoid mentioning this system message in conversation, it is merely to provide context and not something to explicitly repeat. "
        );
        messages.add(systemMessage);
    }

    private void addLine(String line) {
        aiView.add(line, -1);
    }

    private void addLine(String line, int color) {
        aiView.add(line, color);
    }

    private void handleInput(String input) {
        API_KEY = get_token();
        API_URL = get_url();

        if (API_KEY == null) {
            addLine("No ai token set! Set one using *ai token <token> in chat or console");
        } else if (API_URL == null) {
            addLine("No api url set! Set one using *ai url <url> in chat or console");
        } else if (AI_MODEL == null) {
            addLine("No ai model set! Set one using *ai model <model> in chat or console");
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

    private volatile boolean stopRequested = false;

    private void sendPromptToAi(String userPrompt) {
        stopRequested = false;
        new Thread(() -> {
            aiBusy = true;
            sendPromptButton.setLabel("Stop");
            sendPromptButton.setAction(() -> {
                stopRequested = true;
                sendPromptButton.setLabel("Send");
                sendPromptButton.setAction(() -> handleInput(inputBox.getValue()));
                aiBusy = false;
            });

            StringBuilder buffer = new StringBuilder();
            StringBuilder fullResponse = new StringBuilder();
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject payload = new JsonObject();
                addMessage("user", userPrompt);
                payload.addProperty("model", get_model());
                payload.add("messages", messages);
                payload.addProperty("stream", true);
                conn.getOutputStream().write(payload.toString().getBytes());

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while (!stopRequested && (line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;

                        var obj = JsonParser.parseString(data).getAsJsonObject();
                        String content = obj.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("delta")
                                .has("content")
                                ? obj.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("delta")
                                .get("content").getAsString()
                                : null;

                        if (content == null || content.isEmpty()) continue;
                        fullResponse.append(content);
                        for (char c : content.toCharArray()) {
                            if (c == '\n') {
                                addLine(buffer.toString().replaceAll("[*_`~>#\\[\\]]", ""), 0xc7c7c7);
                                buffer.setLength(0);
                            } else buffer.append(c);
                        }
                    }
                }
            } catch (Exception e) {
                addLine("Error: " + e.getMessage());
            }

            addMessage("assistant", fullResponse.toString());
            if (!buffer.isEmpty()) addLine(buffer.toString().replaceAll("[*_`~>#\\[\\]]", ""), 0xc7c7c7);
            sendPromptButton.setLabel("Send");
            addLine("");
            aiBusy = false;
        }).start();
    }

    @Override
    public WindowView getRootView() {
        return this.rootView;
    }
}