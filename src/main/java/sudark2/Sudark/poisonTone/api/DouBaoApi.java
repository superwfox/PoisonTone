package sudark2.Sudark.poisonTone.api;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.Response;
import org.bukkit.configuration.file.FileConfiguration;
import sudark2.Sudark.poisonTone.PoisonTone;
import sudark2.Sudark.poisonTone.bot.OneBotClient;
import sudark2.Sudark.poisonTone.image.ImageStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;

public class DouBaoApi {

    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static String model;
    private static String lastResponseId;
    private static String prompt;
    private static HttpRequest client;

    public static void init() {
        FileConfiguration config = getPlugin(PoisonTone.class).getConfig();
        model = config.getString("MODEL", "doubao-seed-2-0-lite-260215");

        lastResponseId = config.getString("LAST-RESPONSE-ID", "");
        client = new HttpRequest(BASE_URL, config.getString("API-KEY"));
        loadPrompt();
    }

    public static void save() {
        FileConfiguration config = getPlugin(PoisonTone.class).getConfig();
        config.set("LAST-RESPONSE-ID", lastResponseId);
        getPlugin(PoisonTone.class).saveConfig();
    }

    public static void switchModel(String newModel) {
        model = newModel;
    }

    public static HttpRequest getClient() {
        return client;
    }

    public static String getModel() {
        return model;
    }

    public static void reloadPrompt() {
        loadPrompt();
        lastResponseId = "";
        save();
    }

    private static void loadPrompt() {
        File file = new File(getPlugin(PoisonTone.class).getDataFolder(), "prompt.md");
        try {
            prompt = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            prompt = "";
            e.printStackTrace();
        }
    }

    public static void updatePrompt(String newPrompt) {
        File file = new File(getPlugin(PoisonTone.class).getDataFolder(), "prompt.md");
        try {
            Files.writeString(file.toPath(), newPrompt, StandardCharsets.UTF_8);
            reloadPrompt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getPrompt() {
        return prompt;
    }

    public static void askStream(String textIn, OneBotClient bot, String group) {
        new Thread(() -> {
            try {
                String json = assembleJson(textIn);
                Response response = client.postStream("/responses", json);
                parseStream(response, bot, group);
            } catch (IOException e) {
                if (lastResponseId != null && !lastResponseId.isEmpty()) {
                    lastResponseId = "";
                    save();
                    askStream(textIn, bot, group);
                    return;
                }
                System.out.println("[PoisonTone] API请求遇到异常或超时已自动释放: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String assembleJson(String textIn) {
        JSONObject req = new JSONObject();
        req.put("model", model);
        req.put("stream", true);

        if (lastResponseId != null && !lastResponseId.isEmpty())
            req.put("previous_response_id", lastResponseId);

        JSONArray input = new JSONArray();
        if ((lastResponseId == null || lastResponseId.isEmpty())
                && prompt != null && !prompt.isEmpty()) {
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", prompt);
            input.add(sysMsg);
        }
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

        JSONObject thinking = new JSONObject();
        thinking.put("type", "disabled");
        req.put("thinking", thinking);
        return req.toString();
    }

    private static void parseStream(Response response, OneBotClient bot, String group) {
        StringBuilder buf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:"))
                    continue;
                String jsonStr = line.substring(5).trim();
                if ("[DONE]".equals(jsonStr))
                    break;

                JSONObject data = JSONObject.fromObject(jsonStr);
                String type = data.optString("type");

                if ("response.output_text.delta".equals(type)) {
                    buf.append(data.getString("delta"));
                    int idx;
                    while ((idx = buf.indexOf("+")) != -1) {
                        String s = buf.substring(0, idx).trim();
                        buf.delete(0, idx + 1);
                        if (!s.isEmpty())
                            dispatch(s, bot, group);
                    }
                } else if ("response.completed".equals(type)) {
                    lastResponseId = data.getJSONObject("response").getString("id");
                    save();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        String tail = buf.toString().trim();
        if (!tail.isEmpty())
            dispatch(tail, bot, group);
    }

    private static void dispatch(String s, OneBotClient bot, String group) {
        System.out.println(s);
        if ("pass".equals(s))
            return;
        if (s.contains("<") && s.contains(">") || s.contains("function") || s.contains("parameter"))
            return;
        if ("repeat".equals(s)) {
            bot.repeat();
            return;
        }

        if (s.startsWith("sendPicture(") && s.endsWith(")")) {
            String name = s.substring(12, s.length() - 1);
            String b64 = ImageStore.getRandom(name);
            if (b64 != null)
                bot.sendPicture(b64);
            return;
        }

        if (s.startsWith("setb(") && s.endsWith(")")) {
            String[] p = s.substring(5, s.length() - 1).split(",");
            if (p.length == 2)
                bot.setb(p[0].trim(), p[1].trim());
            return;
        }

        bot.sendG(s, group);
    }
}
