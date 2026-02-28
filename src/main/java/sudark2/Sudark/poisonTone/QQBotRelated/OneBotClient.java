package sudark2.Sudark.poisonTone.QQBotRelated;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import sudark2.Sudark.poisonTone.DouBaoRelated.DouBaoApi;

import java.net.URI;

public class OneBotClient extends WebSocketClient {

    static String GroupNum = "";
    static final String SELF_QQ = "3101965697";

    public OneBotClient(URI serverUri) {
        super(serverUri);
    }

    public void sendG(String message, String group) {
        JSONObject json = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("group_id", group);
        params.put("message", message);
        params.put("auto_escape", "false");
        json.put("action", "send_group_msg");
        json.put("params", params);
        try {
            this.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setb(String qq) {
        JSONObject json = new JSONObject();
        JSONObject inner = new JSONObject();
        json.put("action", "set_group_ban");
        inner.put("user_id", qq);
        inner.put("group_id", "1007142639");
        inner.put("duration", "10");
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
        for (int i = 0; i < message.size(); i++) {
            JSONObject obj = message.getJSONObject(i);
            switch (obj.optString("type")) {
                case "text":
                    msg += obj.getJSONObject("data").getString("text");
                    break;
                case "face":
                    msg += "[表情]";
                    break;
                case "image":
                    msg += "[图片]";
                    break;
                case "at":
                    msg += "@" + obj.getJSONObject("data").getString("name");
                    break;
                case "reply":
                    msg += "[回复]";
                    break;
                case "video":
                    msg += "[视频]";
                    break;
                default:
                    msg += "%";
            }
        }

        if (!msg.startsWith("."))
            return;

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

        String textIn = card + "[" + qq + "]:" + msg;
        String answer = DouBaoApi.ask(textIn);
        if (answer == null || answer.equals("pass"))
            return;

        // TODO: 后续消息处理由用户完善
        sendG(answer, qqGroup);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
    }

    @Override
    public void onError(Exception e) {
    }
}
