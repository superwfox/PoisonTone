package sudark2.Sudark.poisonTone.image;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sudark2.Sudark.poisonTone.api.DouBaoApi;

import java.io.IOException;

public class ImageAnalyzer {

    private static final String PROMPT = "判断这张图片是否为表情包/梗图。" +
            "如果是，从以下类别中选一个最匹配的情绪输出：开心、满足、沮丧、悲伤、观望、不满、愤怒。" +
            "如果不是表情包或无法归类，直接输出：忽略";

    public static void analyzeAndStore(String imageUrl) {
        String emotion = classify(imageUrl);
        if (emotion == null)
            return;
        try {
            byte[] bytes = download(imageUrl);
            if (bytes != null)
                ImageStore.add(emotion, bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String classify(String imageUrl) {
        try {
            JSONObject req = new JSONObject();
            req.put("model", DouBaoApi.getVisionModel());

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");

            JSONArray content = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", PROMPT);
            content.add(textPart);

            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", new JSONObject().put("url", imageUrl));
            content.add(imagePart);

            userMsg.put("content", content);
            messages.add(userMsg);
            req.put("messages", messages);

            String response = DouBaoApi.getClient().post("/chat/completions", req.toString());
            return parseEmotion(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String parseEmotion(String response) {
        JSONObject json = JSONObject.fromObject(response);
        JSONArray choices = json.getJSONArray("choices");
        if (choices.isEmpty())
            return null;
        String text = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content").trim();
        for (String e : ImageStore.EMOTIONS) {
            if (text.contains(e))
                return e;
        }
        return null;
    }

    private static byte[] download(String url) throws IOException {
        OkHttpClient dl = new OkHttpClient();
        Request req = new Request.Builder().url(url).build();
        try (Response resp = dl.newCall(req).execute()) {
            if (!resp.isSuccessful())
                return null;
            return resp.body().bytes();
        }
    }
}
