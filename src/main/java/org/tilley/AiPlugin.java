package org.tilley;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.plugin.Plugin;

public class AiPlugin extends Plugin {
	
	@Override
	public void onLoad() {
		
		this.getLogger().info("Loaded Rusher AI Integration!");

		final ResizeableWindow AiTerminalWindow = new AiIntegrationWindow();
		RusherHackAPI.getWindowManager().registerFeature(AiTerminalWindow);

        final TokenCommand TokenCommand = new TokenCommand();
        RusherHackAPI.getCommandManager().registerFeature(TokenCommand);

	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("Rusher AI Integration unloaded!");
	}
	
}