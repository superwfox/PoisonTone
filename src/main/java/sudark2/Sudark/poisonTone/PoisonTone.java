package sudark2.Sudark.poisonTone;

import org.bukkit.plugin.java.JavaPlugin;
import sudark2.Sudark.poisonTone.api.DouBaoApi;
import sudark2.Sudark.poisonTone.image.ImageStore;
import sudark2.Sudark.poisonTone.bot.OneBotClient;

import java.net.URI;

public final class PoisonTone extends JavaPlugin {

    private OneBotClient wsClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DouBaoApi.init();
        ImageStore.init(getDataFolder());
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
