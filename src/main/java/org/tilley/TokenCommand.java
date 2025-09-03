package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class TokenCommand extends Command {
    private static final File baseDir = new File(System.getProperty("user.home"), ".rusherhack");
    private static final File tokenFile = new File(baseDir, "ai_token");
    private static final File urlFile = new File(baseDir, "ai_url");
    private static final File modelFile = new File(baseDir, "ai_model");

    public TokenCommand() {
        super("ai", "configs for ai integration");
    }

    private String saveToFile(File file, String value) {
        try {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), Base64.getEncoder().encodeToString(value.getBytes()));
            return file.getPath();
        } catch (IOException e) {
            return null;
        }
    }

    private String loadFromFile(File file) {
        if (!file.exists()) return null;
        try {
            return new String(Base64.getDecoder().decode(Files.readString(file.toPath())));
        } catch (IOException e) {
            return null;
        }
    }

    @CommandExecutor(subCommand = "token")
    @CommandExecutor.Argument("token")
    private String set_token(String token) {
        if (!token.startsWith("sk-")) return "invalid token";
        String path = saveToFile(tokenFile, token);
        return path == null ? "error saving token to file" : "token saved to" + path;
    }

    @CommandExecutor(subCommand = "url")
    @CommandExecutor.Argument("url")
    private String set_url(String url) {
        if (!url.startsWith("https://api.")) return "not an api url";
        if (!url.endsWith("/chat/completions")) return "please provide a url with completions endpoint (ends with /chat/completions)";
        String path = saveToFile(urlFile, url);
        return path == null ? "error saving url to file" : "url saved to" + path;
    }

    @CommandExecutor(subCommand = "model")
    @CommandExecutor.Argument("model name")
    private String set_model(String model) {
        String path = saveToFile(modelFile, model);
        return path == null ? "error saving model to file" : "model saved to" + path;
    }

    public static String get_token() {
        return new TokenCommand().loadFromFile(tokenFile);
    }

    public static String get_url() {
        return new TokenCommand().loadFromFile(urlFile);
    }

    public static String get_model() {
        return new TokenCommand().loadFromFile(modelFile);
    }
}
