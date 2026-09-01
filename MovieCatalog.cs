// MovieCatalog.cs
// Movie Catalog (Posters) на C#

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Diagnostics;

class MovieCatalog
{
    private const string Reset = "\u001B[0m";
    private const string Bold = "\u001B[1m";
    private const string Cyan = "\u001B[96m";
    private const string Green = "\u001B[92m";
    private const string Yellow = "\u001B[93m";
    private const string Red = "\u001B[91m";

    private static string Colorize(string text, string color) => color + text + Reset;

    class Movie
    {
        public int Id { get; set; }
        public string Title { get; set; }
        public int Year { get; set; }
        public double Rating { get; set; }
        public string Poster { get; set; }
        public string Description { get; set; }
    }

    class Data
    {
        public List<Movie> Movies { get; set; } = new List<Movie>();
        public int NextId { get; set; } = 1;
    }

    private readonly string dataFile;
    private Data data;

    public MovieCatalog(string dataFile)
    {
        this.dataFile = dataFile;
        Load();
    }

    private void Load()
    {
        if (!File.Exists(dataFile))
        {
            data = new Data();
            return;
        }
        try
        {
            string json = File.ReadAllText(dataFile);
            data = JsonSerializer.Deserialize<Data>(json) ?? new Data();
        }
        catch
        {
            data = new Data();
        }
    }

    private void Save()
    {
        string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(dataFile, json);
    }

    public Movie Add(string title, int year, double rating, string poster, string description = "")
    {
        var movie = new Movie { Id = data.NextId, Title = title, Year = year, Rating = rating, Poster = poster, Description = description };
        data.Movies.Add(movie);
        data.NextId++;
        Save();
        return movie;
    }

    public bool Remove(int id)
    {
        var movie = data.Movies.FirstOrDefault(m => m.Id == id);
        if (movie == null) return false;
        data.Movies.Remove(movie);
        Save();
        return true;
    }

    public Movie Get(int id) => data.Movies.FirstOrDefault(m => m.Id == id);

    public List<Movie> Search(string query)
    {
        var q = query.ToLower();
        return data.Movies.Where(m => m.Title.ToLower().Contains(q)).ToList();
    }

    public List<Movie> List() => data.Movies;

    public bool OpenPoster(int id)
    {
        var movie = Get(id);
        if (movie == null) return false;
        if (!File.Exists(movie.Poster))
        {
            Console.WriteLine(Colorize($"Постер не найден: {movie.Poster}", Red));
            return false;
        }
        Process.Start(new ProcessStartInfo(movie.Poster) { UseShellExecute = true });
        return true;
    }

    static void Main(string[] args)
    {
        if (args.Length == 0 || args[0] == "help")
        {
            Console.WriteLine(@"Использование: MovieCatalog <команда> [опции]
  add       --title <title> --year <year> --rating <rating> --poster <poster> [--description <desc>]
  remove    --id <id>
  list
  search    --query <query>
  info      --id <id>
  open      --id <id>");
            return;
        }

        string command = args[0];
        var opts = new Dictionary<string, string>();
        for (int i = 1; i < args.Length; i++)
        {
            if (args[i].StartsWith("--"))
            {
                string key = args[i].Substring(2);
                if (i + 1 < args.Length && !args[i + 1].StartsWith("--"))
                    opts[key] = args[++i];
                else
                    opts[key] = "";
            }
        }

        string dataFile = opts.GetValueOrDefault("data", "movies.json");
        var catalog = new MovieCatalog(dataFile);

        switch (command)
        {
            case "add":
                if (!opts.ContainsKey("title") || !opts.ContainsKey("year") || !opts.ContainsKey("rating") || !opts.ContainsKey("poster"))
                {
                    Console.WriteLine("Ошибка: требуются --title, --year, --rating, --poster");
                    return;
                }
                var movie = catalog.Add(opts["title"], int.Parse(opts["year"]), double.Parse(opts["rating"]), opts["poster"], opts.GetValueOrDefault("description", ""));
                Console.WriteLine(Colorize($"Фильм добавлен: ID {movie.Id} - {movie.Title}", Green));
                break;
            case "remove":
                if (!opts.ContainsKey("id"))
                {
                    Console.WriteLine("Ошибка: укажите --id");
                    return;
                }
                int id = int.Parse(opts["id"]);
                if (catalog.Remove(id))
                    Console.WriteLine(Colorize($"Фильм с ID {id} удалён", Green));
                else
                    Console.WriteLine(Colorize($"Фильм с ID {id} не найден", Red));
                break;
            case "list":
                var movies = catalog.List();
                if (movies.Count == 0) Console.WriteLine("Нет фильмов.");
                else
                {
                    foreach (var m in movies)
                        Console.WriteLine($"{Colorize(m.Id.ToString(), Cyan)} | {Colorize(m.Title, Bold)} | {m.Year} | {Colorize(m.Rating.ToString(), Yellow)} | {m.Poster}");
                }
                break;
            case "search":
                if (!opts.ContainsKey("query"))
                {
                    Console.WriteLine("Ошибка: укажите --query");
                    return;
                }
                var results = catalog.Search(opts["query"]);
                if (results.Count == 0) Console.WriteLine("Ничего не найдено.");
                else
                {
                    foreach (var m in results)
                        Console.WriteLine($"{Colorize(m.Id.ToString(), Cyan)} | {Colorize(m.Title, Bold)} | {m.Year} | {Colorize(m.Rating.ToString(), Yellow)} | {m.Poster}");
                }
                break;
            case "info":
                if (!opts.ContainsKey("id"))
                {
                    Console.WriteLine("Ошибка: укажите --id");
                    return;
                }
                int infoId = int.Parse(opts["id"]);
                var infoMovie = catalog.Get(infoId);
                if (infoMovie == null)
                    Console.WriteLine(Colorize($"Фильм с ID {infoId} не найден", Red));
                else
                {
                    Console.WriteLine(Colorize($"ID: {infoMovie.Id}", Cyan));
                    Console.WriteLine(Colorize($"Название: {infoMovie.Title}", Bold));
                    Console.WriteLine($"Год: {infoMovie.Year}");
                    Console.WriteLine($"Рейтинг: {Colorize(infoMovie.Rating.ToString(), Yellow)}");
                    Console.WriteLine($"Постер: {infoMovie.Poster}");
                    Console.WriteLine($"Описание: {infoMovie.Description}");
                }
                break;
            case "open":
                if (!opts.ContainsKey("id"))
                {
                    Console.WriteLine("Ошибка: укажите --id");
                    return;
                }
                int openId = int.Parse(opts["id"]);
                if (catalog.OpenPoster(openId))
                    Console.WriteLine(Colorize($"Постер фильма ID {openId} открыт", Green));
                else
                    Console.WriteLine(Colorize($"Не удалось открыть постер для ID {openId}", Red));
                break;
            default:
                Console.WriteLine("Неизвестная команда.");
                break;
        }
    }
}
