package sudark2.Sudark.poisonTone.image;

import sudark2.Sudark.poisonTone.Utils.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class ImageStore {

    static final String[] EMOTIONS = { "开心", "满足", "沮丧", "悲伤", "观望", "不满", "愤怒" };
    private static final Map<String, List<String>> images = new HashMap<>();
    private static final Set<String> hashSet = Collections.synchronizedSet(new HashSet<>());
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
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    images.get(e).add(Base64.getEncoder().encodeToString(bytes));
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (img != null)
                        hashSet.add(buildKey(img));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        int total = images.values().stream().mapToInt(List::size).sum();
        System.out.println("[PoisonTone] 已加载 " + total + " 张表情, 已知哈希 " + hashSet.size() + " 条");
    }

    public static boolean isDuplicate(BufferedImage img) {
        return hashSet.contains(buildKey(img));
    }

    public static void add(String emotion, BufferedImage img, byte[] imageBytes) {
        if (!images.containsKey(emotion))
            return;
        hashSet.add(buildKey(img));
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        images.get(emotion).add(b64);
        File dir = new File(baseDir, emotion);
        dir.mkdirs();
        String name = Long.toHexString(ImageUtil.aHash(img)) + "_" + Integer.toHexString(ImageUtil.colorHash(img));
        try {
            Files.write(new File(dir, name + ".jpg").toPath(), imageBytes);
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

    private static String buildKey(BufferedImage img) {
        return ImageUtil.aHash(img) + "_" + ImageUtil.colorHash(img);
    }
}
