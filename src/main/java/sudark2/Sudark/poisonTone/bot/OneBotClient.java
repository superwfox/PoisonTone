package sudark2.Sudark.poisonTone.bot;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import sudark2.Sudark.poisonTone.api.DouBaoApi;
import sudark2.Sudark.poisonTone.image.ImageAnalyzer;

import java.net.URI;

public class OneBotClient extends WebSocketClient {

    public static String GroupNum;
    public static String SELF_QQ;
    public static String OpQQ;
    static JSONArray StoredMessage = new JSONArray();

    public OneBotClient(URI serverUri) {
        super(serverUri);
    }

    public void sendG(String message, String group) {
        JSONObject json = new JSONObject();
        JSONObject params = new JSONObject();
        JSONArray messageArr = new JSONArray();
        JSONObject message0 = new JSONObject();
        message0.put("type", "text");
        message0.put("data", new JSONObject().put("text", message));
        messageArr.add(message0);
        params.put("group_id", group);
        params.put("message", messageArr);
        params.put("auto_escape", "false");
        json.put("action", "send_group_msg");
        json.put("params", params);
        try {
            this.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void repeat() {
        JSONObject json = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("group_id", GroupNum);
        params.put("message", StoredMessage);
        json.put("action", "send_group_msg");
        json.put("params", params);
        try {
            this.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStoredMessage(JSONArray array) {
        StoredMessage = array;
    }

    public void sendPicture(String base64) {
        JSONObject root = new JSONObject();
        root.put("group_id", GroupNum);

        JSONArray messageArr = new JSONArray();

        JSONObject msg0 = new JSONObject();
        msg0.put("type", "image");

        JSONObject data = new JSONObject();
        data.put("file", "base64://" + base64);

        msg0.put("data", data);
        messageArr.add(msg0);

        root.put("message", messageArr);
        JSONObject finalJson = new JSONObject();
        finalJson.put("action", "send_group_msg");
        finalJson.put("params", root);
        try {
            this.send(finalJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setb(String qq, String duration) {
        JSONObject json = new JSONObject();
        JSONObject inner = new JSONObject();
        json.put("action", "set_group_ban");
        inner.put("user_id", qq);
        inner.put("group_id", GroupNum);
        inner.put("duration", duration);
        json.put("params", inner);
        try {
            this.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String s) {
        JSONObject json = JSONObject.fromObject(s);
        if (!json.containsKey("group_id"))
            return;
        if (!"message".equals(json.optString("post_type")))
            return;

        String qqGroup = json.getString("group_id");
        if (!qqGroup.equals(GroupNum))
            return;

        JSONObject sender = json.getJSONObject("sender");
        String card = sender.optString("card", "");
        if (card.isEmpty())
            card = sender.getString("nickname");
        String qq = sender.getString("user_id");
        if (qq.equals(SELF_QQ))
            return;

        String msg = "";
        JSONArray message = json.getJSONArray("message");
        updateStoredMessage(message);
        for (int i = 0; i < message.size(); i++) {
            JSONObject obj = message.getJSONObject(i);
            String url;
            switch (obj.optString("type")) {
                case "text":
                    msg += obj.getJSONObject("data").getString("text");
                    continue;
                case "face":
                case "mface":
                    msg += "[表情]";
                    continue;
                case "image":
                    url = obj.getJSONObject("data").getString("url");
                    msg += "[图片]（" + url + "）";
                    String imgUrl = url;
                    new Thread(() -> ImageAnalyzer.analyzeAndStore(imgUrl)).start();
                    continue;
                case "record":
                    url = obj.getJSONObject("data").getString("url");
                    msg += "[语音]（" + url + "）";
                    continue;
                case "at":
                    msg += "@" + obj.getJSONObject("data").getString("name");
                    continue;
                case "reply":
                    msg += "[回复]";
                    continue;
                case "video":
                    url = obj.getJSONObject("data").getString("url");
                    msg += "[视频](" + url + ")";
                    continue;
                default:
                    msg += "%";
            }
        }

        if (!msg.startsWith("."))
            return;

        if (qq.equals(OpQQ)) {
            if (msg.contains("使用小模型")) {
                DouBaoApi.switchModel("doubao-seed-2-0-mini-260215");
                sendG("已切换至小模型", qqGroup);
                return;
            }
            if (msg.contains("使用大模型")) {
                DouBaoApi.switchModel("doubao-seed-2-0-lite-260215");
                sendG("已切换至大模型", qqGroup);
                return;
            }
            if (msg.contains("重载prompt")) {
                DouBaoApi.reloadPrompt();
                sendG("prompt已重载,会话已重置", qqGroup);
                return;
            }
        }
        String textIn = card + "[" + qq + "]:" + msg;
        DouBaoApi.askStream(textIn, this, qqGroup);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
    }

    @Override
    public void onError(Exception e) {
    }
}
