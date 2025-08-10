package store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.Workout;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FileStore {
    private final Path path;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type listType = new TypeToken<List<Workout>>(){}.getType();

    public FileStore(String filePath) {
        this.path = Paths.get(filePath);
        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (Files.notExists(path)) {
                Files.write(path, "[]".getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to init store: " + e.getMessage(), e);
        }
    }

    public synchronized List<Workout> getAll() {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            List<Workout> list = gson.fromJson(json, listType);
            return (list == null) ? new ArrayList<>() : list;
        } catch (IOException e) {
            throw new RuntimeException("Read error: " + e.getMessage(), e);
        }
    }

    public synchronized void add(Workout w) {
        List<Workout> list = getAll();
        list.add(w);
        saveAll(list);
    }

    public synchronized void saveAll(List<Workout> list) {
        String json = gson.toJson(list, listType);
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            Files.writeString(tmp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Write error: " + e.getMessage(), e);
        }
    }
}
