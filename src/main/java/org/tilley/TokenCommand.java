package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

    public class TokenCommand extends Command {
        private static final File tokenFile = new File(System.getProperty("user.home"), ".rusherhack/ai_token");
        private static final File urlFile = new File(System.getProperty("user.home"), ".rusherhack/ai_url");
        private static final File modelFile = new File(System.getProperty("user.home"), ".rusherhack/ai_model");

        public TokenCommand() {
            super("ai", "configs for ai integration");
        }

        @CommandExecutor(subCommand = "token")
        @CommandExecutor.Argument("token")
        private String set_token(String token) {
            if (!token.startsWith("sk-")) return "invalid token";
            try {
                tokenFile.getParentFile().mkdirs();
                Files.writeString(tokenFile.toPath(), Base64.getEncoder().encodeToString(token.getBytes()));
            } catch (IOException e) {
                return "error saving token to file";
            }
            return "token saved to" + tokenFile.getPath();
        }

        @CommandExecutor(subCommand = "url")
        @CommandExecutor.Argument("url")
        private String set_url(String url) {
            if (!url.startsWith("https://api.")) return "not an api url";
            if (!url.endsWith("/chat/completions")) return "please provide a url with completions endpoint (ends with /chat/completions)";
            try {
                urlFile.getParentFile().mkdirs();
                Files.writeString(urlFile.toPath(), Base64.getEncoder().encodeToString(url.getBytes()));
            } catch (IOException e) {
                return "error saving url to file";
            }
            return "url saved to" + urlFile.getPath();
        }

        @CommandExecutor(subCommand = "model")
        @CommandExecutor.Argument("model name")
        private String set_model(String model) {
            try {
                modelFile.getParentFile().mkdirs();
                Files.writeString(modelFile.toPath(), Base64.getEncoder().encodeToString(model.getBytes()));
            } catch (IOException e) {
                return "error saving model to file";
            }
            return "model saved to" + modelFile.getPath();
        }



        public static String get_token() {
            if (!tokenFile.exists()) return null;
            try {
                return new String(Base64.getDecoder().decode(Files.readString(tokenFile.toPath())));
            } catch (IOException e) {
                return null;
            }
        }

        public static String get_url() {
            if (!urlFile.exists()) return null;
            try {
                return new String(Base64.getDecoder().decode(Files.readString(urlFile.toPath())));
            } catch (IOException e) {
                return null;
            }
        }

        public static String get_model() {
            if (!modelFile.exists()) return null;
            try {
                return new String(Base64.getDecoder().decode(Files.readString(modelFile.toPath())));
            } catch (IOException e) {
                return null;
            }
        }


}
