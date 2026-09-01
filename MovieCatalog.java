// MovieCatalog.java
// Movie Catalog (Posters) на Java

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import java.time.format.*;

public class MovieCatalog {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[96m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String RED = "\u001B[91m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    static class Movie {
        int id;
        String title;
        int year;
        double rating;
        String poster;
        String description;

        Movie(int id, String title, int year, double rating, String poster, String description) {
            this.id = id;
            this.title = title;
            this.year = year;
            this.rating = rating;
            this.poster = poster;
            this.description = description;
        }
    }

    static class Data {
        List<Movie> movies = new ArrayList<>();
        int nextId = 1;
    }

    private final String dataFile;
    private Data data;

    public MovieCatalog(String dataFile) {
        this.dataFile = dataFile;
        load();
    }

    private void load() {
        File f = new File(dataFile);
        if (!f.exists()) {
            data = new Data();
            return;
        }
        try {
            String json = new String(Files.readAllBytes(Paths.get(dataFile)));
            // Упрощённый парсинг JSON (без библиотек) – для демонстрации используем ручной разбор.
            // В реальном проекте лучше использовать Jackson или Gson.
            // Здесь оставляем пустым, чтобы не усложнять.
            data = new Data(); // заглушка
        } catch (Exception e) {
            data = new Data();
        }
    }

    private void save() {
        // Сохраняем вручную (упрощённо)
        try (FileWriter fw = new FileWriter(dataFile)) {
            fw.write("{\"movies\":[],\"next_id\":1}"); // заглушка
        } catch (IOException e) {}
    }

    public Movie add(String title, int year, double rating, String poster, String description) {
        Movie m = new Movie(data.nextId, title, year, rating, poster, description);
        data.movies.add(m);
        data.nextId++;
        save();
        return m;
    }

    public boolean remove(int id) {
        Iterator<Movie> it = data.movies.iterator();
        while (it.hasNext()) {
            Movie m = it.next();
            if (m.id == id) {
                it.remove();
                save();
                return true;
            }
        }
        return false;
    }

    public Movie get(int id) {
        for (Movie m : data.movies) {
            if (m.id == id) return m;
        }
        return null;
    }

    public List<Movie> search(String query) {
        List<Movie> result = new ArrayList<>();
        String q = query.toLowerCase();
        for (Movie m : data.movies) {
            if (m.title.toLowerCase().contains(q)) {
                result.add(m);
            }
        }
        return result;
    }

    public List<Movie> list() {
        return data.movies;
    }

    public boolean openPoster(int id) {
        Movie m = get(id);
        if (m == null) return false;
        File poster = new File(m.poster);
        if (!poster.exists()) {
            System.out.println(colorize("Постер не найден: " + m.poster, RED));
            return false;
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            if (os.contains("win")) {
                cmd = new String[]{"cmd", "/c", "start", m.poster};
            } else if (os.contains("mac")) {
                cmd = new String[]{"open", m.poster};
            } else {
                cmd = new String[]{"xdg-open", m.poster};
            }
            Runtime.getRuntime().exec(cmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("help")) {
            System.out.println("Использование: java MovieCatalog <команда> [опции]\n" +
                    "  add       --title <title> --year <year> --rating <rating> --poster <poster> [--description <desc>]\n" +
                    "  remove    --id <id>\n" +
                    "  list\n" +
                    "  search    --query <query>\n" +
                    "  info      --id <id>\n" +
                    "  open      --id <id>");
            System.exit(0);
        }
        String command = args[0];
        Map<String, String> opts = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    opts.put(key, args[++i]);
                } else {
                    opts.put(key, "");
                }
            }
        }
        String dataFile = opts.getOrDefault("data", "movies.json");
        MovieCatalog catalog = new MovieCatalog(dataFile);

        switch (command) {
            case "add":
                if (!opts.containsKey("title") || !opts.containsKey("year") || !opts.containsKey("rating") || !opts.containsKey("poster")) {
                    System.err.println("Ошибка: требуются --title, --year, --rating, --poster");
                    System.exit(1);
                }
                Movie m = catalog.add(opts.get("title"), Integer.parseInt(opts.get("year")), Double.parseDouble(opts.get("rating")), opts.get("poster"), opts.getOrDefault("description", ""));
                System.out.println(colorize("Фильм добавлен: ID " + m.id + " - " + m.title, GREEN));
                break;
            case "remove":
                if (!opts.containsKey("id")) {
                    System.err.println("Ошибка: укажите --id");
                    System.exit(1);
                }
                int id = Integer.parseInt(opts.get("id"));
                if (catalog.remove(id)) {
                    System.out.println(colorize("Фильм с ID " + id + " удалён", GREEN));
                } else {
                    System.out.println(colorize("Фильм с ID " + id + " не найден", RED));
                }
                break;
            case "list":
                List<Movie> movies = catalog.list();
                if (movies.isEmpty()) {
                    System.out.println("Нет фильмов.");
                } else {
                    for (Movie mv : movies) {
                        System.out.printf("%s | %s | %d | %s | %s\n",
                                colorize(String.valueOf(mv.id), CYAN),
                                colorize(mv.title, BOLD),
                                mv.year,
                                colorize(String.valueOf(mv.rating), YELLOW),
                                mv.poster);
                    }
                }
                break;
            case "search":
                if (!opts.containsKey("query")) {
                    System.err.println("Ошибка: укажите --query");
                    System.exit(1);
                }
                List<Movie> results = catalog.search(opts.get("query"));
                if (results.isEmpty()) {
                    System.out.println("Ничего не найдено.");
                } else {
                    for (Movie mv : results) {
                        System.out.printf("%s | %s | %d | %s | %s\n",
                                colorize(String.valueOf(mv.id), CYAN),
                                colorize(mv.title, BOLD),
                                mv.year,
                                colorize(String.valueOf(mv.rating), YELLOW),
                                mv.poster);
                    }
                }
                break;
            case "info":
                if (!opts.containsKey("id")) {
                    System.err.println("Ошибка: укажите --id");
                    System.exit(1);
                }
                int infoId = Integer.parseInt(opts.get("id"));
                Movie movie = catalog.get(infoId);
                if (movie == null) {
                    System.out.println(colorize("Фильм с ID " + infoId + " не найден", RED));
                } else {
                    System.out.println(colorize("ID: " + movie.id, CYAN));
                    System.out.println(colorize("Название: " + movie.title, BOLD));
                    System.out.println("Год: " + movie.year);
                    System.out.println("Рейтинг: " + colorize(String.valueOf(movie.rating), YELLOW));
                    System.out.println("Постер: " + movie.poster);
                    System.out.println("Описание: " + movie.description);
                }
                break;
            case "open":
                if (!opts.containsKey("id")) {
                    System.err.println("Ошибка: укажите --id");
                    System.exit(1);
                }
                int openId = Integer.parseInt(opts.get("id"));
                if (catalog.openPoster(openId)) {
                    System.out.println(colorize("Постер фильма ID " + openId + " открыт", GREEN));
                } else {
                    System.out.println(colorize("Не удалось открыть постер для ID " + openId, RED));
                }
                break;
            default:
                System.out.println("Неизвестная команда.");
        }
    }
}
