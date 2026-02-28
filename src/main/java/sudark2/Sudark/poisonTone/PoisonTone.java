package sudark2.Sudark.poisonTone;

import org.bukkit.plugin.java.JavaPlugin;
import sudark2.Sudark.poisonTone.DouBaoRelated.DouBaoApi;
import sudark2.Sudark.poisonTone.QQBotRelated.OneBotClient;

import java.net.URI;

public final class PoisonTone extends JavaPlugin {

    private OneBotClient wsClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DouBaoApi.init();
        try {
            wsClient = new OneBotClient(new URI("ws://127.0.0.1:3001"));
            wsClient.connectBlocking();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        DouBaoApi.save();
        if (wsClient != null)
            wsClient.close();
    }
}
