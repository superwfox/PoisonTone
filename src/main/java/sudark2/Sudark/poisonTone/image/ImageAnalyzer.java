package sudark2.Sudark.poisonTone.image;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sudark2.Sudark.poisonTone.api.DouBaoApi;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ImageAnalyzer {

    private static final String MINI_MODEL = "doubao-seed-2-0-mini-260215";
    private static final String PROMPT = "判断这张图片是否为表情包/梗图。" +
            "如果是，从以下类别中选一个最匹配的情绪输出：开心、满足、沮丧、悲伤、观望、不满、愤怒。" +
            "如果不是表情包或无法归类，直接输出：忽略";
    private static final Set<String> urlSeen = Collections.synchronizedSet(new HashSet<>());

    public static void analyzeAndStore(String imageUrl) {
        if (!urlSeen.add(imageUrl))
            return;
        try {
            byte[] bytes = download(imageUrl);
            if (bytes == null)
                return;
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null)
                return;
            if (ImageStore.isDuplicate(img))
                return;

            String emotion = classify(imageUrl);
            if (emotion == null)
                return;

            ImageStore.add(emotion, img, bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String classify(String imageUrl) {
        try {
            JSONObject req = new JSONObject();
            req.put("model", MINI_MODEL);

            JSONArray input = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");

            JSONArray content = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("type", "input_text");
            textPart.put("text", PROMPT);
            content.add(textPart);

            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "input_image");
            imagePart.put("image_url", imageUrl);
            content.add(imagePart);

            userMsg.put("content", content);
            input.add(userMsg);
            req.put("input", input);

            JSONObject thinking = new JSONObject();
            thinking.put("type", "disabled");
            req.put("thinking", thinking);

            String response = DouBaoApi.getClient().post("/responses", req.toString());
            return parseEmotion(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String parseEmotion(String response) {
        JSONObject json = JSONObject.fromObject(response);
        JSONArray output = json.getJSONArray("output");
        for (int i = 0; i < output.size(); i++) {
            JSONObject item = output.getJSONObject(i);
            if (!"message".equals(item.optString("type")))
                continue;
            JSONArray content = item.getJSONArray("content");
            for (int j = 0; j < content.size(); j++) {
                JSONObject part = content.getJSONObject(j);
                if (!"output_text".equals(part.optString("type")))
                    continue;
                String text = part.getString("text").trim();
                for (String e : ImageStore.EMOTIONS) {
                    if (text.contains(e))
                        return e;
                }
            }
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
