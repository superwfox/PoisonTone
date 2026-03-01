package sudark2.Sudark.poisonTone.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ImageStore {

    static final String[] EMOTIONS = { "开心", "满足", "沮丧", "悲伤", "观望", "不满", "愤怒" };
    private static final Map<String, List<String>> images = new HashMap<>();
    private static File baseDir;

    public static void init(File dataFolder) {
        baseDir = new File(dataFolder, "emojis");
        for (String e : EMOTIONS) {
            images.put(e, Collections.synchronizedList(new ArrayList<>()));
            File dir = new File(baseDir, e);
            if (!dir.exists()) {
                dir.mkdirs();
                continue;
            }
            File[] files = dir.listFiles((d, n) -> n.endsWith(".jpg") || n.endsWith(".png"));
            if (files == null)
                continue;
            for (File f : files) {
                try {
                    images.get(e).add(Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        int total = images.values().stream().mapToInt(List::size).sum();
        System.out.println("[PoisonTone] 已加载 " + total + " 张表情");
    }

    public static synchronized void add(String emotion, byte[] imageBytes) {
        if (!images.containsKey(emotion))
            return;
        images.get(emotion).add(Base64.getEncoder().encodeToString(imageBytes));
        File dir = new File(baseDir, emotion);
        dir.mkdirs();
        try {
            Files.write(new File(dir, System.currentTimeMillis() + ".jpg").toPath(), imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[PoisonTone] 新表情: " + emotion + " (共" + images.get(emotion).size() + "张)");
    }

    public static String getRandom(String emotion) {
        List<String> list = images.get(emotion);
        if (list == null || list.isEmpty())
            return null;
        return list.get(new Random().nextInt(list.size()));
    }
}
