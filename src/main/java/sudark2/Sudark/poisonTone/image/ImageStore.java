package sudark2.Sudark.poisonTone.image;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class ImageStore {

    static final String[] EMOTIONS = { "开心", "满足", "沮丧", "悲伤", "观望", "不满", "愤怒" };
    private static final Map<String, List<String>> images = new HashMap<>();
    private static final Map<String, String> urlCache = new HashMap<>();
    private static File baseDir;
    private static File cacheFile;

    public static void init(File dataFolder) {
        baseDir = new File(dataFolder, "emojis");
        cacheFile = new File(dataFolder, "url_cache.dat");
        loadCache();
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
        System.out.println("[PoisonTone] 已加载 " + total + " 张表情, 缓存 " + urlCache.size() + " 条URL");
    }

    public static synchronized String getCached(String url) {
        return urlCache.get(url);
    }

    public static synchronized void putCache(String url, String emotion) {
        urlCache.put(url, emotion);
        saveCache();
    }

    public static synchronized void add(String emotion, String url, byte[] imageBytes) {
        if (!images.containsKey(emotion))
            return;
        String hash = Integer.toHexString(url.hashCode());
        images.get(emotion).add(Base64.getEncoder().encodeToString(imageBytes));
        File dir = new File(baseDir, emotion);
        dir.mkdirs();
        try {
            Files.write(new File(dir, hash + ".jpg").toPath(), imageBytes);
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

    @SuppressWarnings("unchecked")
    private static void loadCache() {
        if (!cacheFile.exists())
            return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(cacheFile))) {
            urlCache.putAll((Map<String, String>) in.readObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveCache() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
            out.writeObject(new HashMap<>(urlCache));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
