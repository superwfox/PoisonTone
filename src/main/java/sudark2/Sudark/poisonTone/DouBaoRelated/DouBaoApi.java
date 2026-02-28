package sudark2.Sudark.poisonTone.DouBaoRelated;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bukkit.configuration.file.FileConfiguration;
import sudark2.Sudark.poisonTone.PoisonTone;

import java.io.IOException;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;

public class DouBaoApi {

    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static String model;
    private static String lastResponseId;
    private static HttpRequest client;

    public static void init() {
        FileConfiguration config = getPlugin(PoisonTone.class).getConfig();
        model = config.getString("MODEL", "doubao-seed-2-0-lite-260215");
        lastResponseId = config.getString("LAST-RESPONSE-ID", "");
        client = new HttpRequest(BASE_URL, config.getString("API-KEY"));
    }

    public static void save() {
        FileConfiguration config = getPlugin(PoisonTone.class).getConfig();
        config.set("LAST-RESPONSE-ID", lastResponseId);
        getPlugin(PoisonTone.class).saveConfig();
    }

    public static void switchModel(String newModel) {
        model = newModel;
    }

    public static String ask(String textIn) {
        try {
            String json = assembleJson(textIn);
            String response = client.post("/responses", json);
            System.out.println("\u001B[36m" + response + "\u001B[0m");
            return parseResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String assembleJson(String textIn) {
        JSONObject req = new JSONObject();
        req.put("model", model);

        if (lastResponseId != null && !lastResponseId.isEmpty()) {
            req.put("previous_response_id", lastResponseId);
        }

        JSONArray input = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", textIn);
        input.add(userMsg);
        req.put("input", input);

        if (textIn.contains("查") || textIn.contains("不知道") || textIn.contains("？") || textIn.contains("什么")) {
            JSONArray tools = new JSONArray();
            JSONObject webSearch = new JSONObject();
            webSearch.put("type", "web_search");
            webSearch.put("max_keyword", 2);
            tools.add(webSearch);
            req.put("tools", tools);
        }

        req.put("thinking", "disable");
        return req.toString();
    }

    private static String parseResponse(String response) {
        JSONObject json = JSONObject.fromObject(response);
        lastResponseId = json.getString("id");
        save();

        JSONArray output = json.getJSONArray("output");
        for (int i = 0; i < output.size(); i++) {
            JSONObject item = output.getJSONObject(i);
            if ("message".equals(item.optString("type"))) {
                JSONArray content = item.getJSONArray("content");
                for (int j = 0; j < content.size(); j++) {
                    JSONObject part = content.getJSONObject(j);
                    if ("output_text".equals(part.optString("type"))) {
                        return part.getString("text");
                    }
                }
            }
        }
        return null;
    }
}
