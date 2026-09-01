// movies.go
// Movie Catalog (Posters) на Go

package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"runtime"
	"strconv"
	"strings"
)

// ANSI-цвета (упрощённо)
const (
	reset  = "\033[0m"
	bold   = "\033[1m"
	cyan   = "\033[96m"
	green  = "\033[92m"
	yellow = "\033[93m"
	red    = "\033[91m"
)

func colorize(text, color string) string {
	return color + text + reset
}

type Movie struct {
	ID          int     `json:"id"`
	Title       string  `json:"title"`
	Year        int     `json:"year"`
	Rating      float64 `json:"rating"`
	Poster      string  `json:"poster"`
	Description string  `json:"description"`
}

type Data struct {
	Movies []Movie `json:"movies"`
	NextID int     `json:"next_id"`
}

type MovieCatalog struct {
	dataFile string
	data     Data
}

func NewMovieCatalog(dataFile string) *MovieCatalog {
	c := &MovieCatalog{dataFile: dataFile}
	c.load()
	return c
}

func (c *MovieCatalog) load() {
	file, err := os.ReadFile(c.dataFile)
	if err != nil {
		c.data = Data{Movies: []Movie{}, NextID: 1}
		return
	}
	var d Data
	if err := json.Unmarshal(file, &d); err != nil {
		c.data = Data{Movies: []Movie{}, NextID: 1}
	} else {
		c.data = d
	}
}

func (c *MovieCatalog) save() {
	data, _ := json.MarshalIndent(c.data, "", "  ")
	os.WriteFile(c.dataFile, data, 0644)
}

func (c *MovieCatalog) add(title string, year int, rating float64, poster, description string) Movie {
	movie := Movie{ID: c.data.NextID, Title: title, Year: year, Rating: rating, Poster: poster, Description: description}
	c.data.Movies = append(c.data.Movies, movie)
	c.data.NextID++
	c.save()
	return movie
}

func (c *MovieCatalog) remove(id int) bool {
	for i, m := range c.data.Movies {
		if m.ID == id {
			c.data.Movies = append(c.data.Movies[:i], c.data.Movies[i+1:]...)
			c.save()
			return true
		}
	}
	return false
}

func (c *MovieCatalog) get(id int) *Movie {
	for _, m := range c.data.Movies {
		if m.ID == id {
			return &m
		}
	}
	return nil
}

func (c *MovieCatalog) search(query string) []Movie {
	q := strings.ToLower(query)
	var result []Movie
	for _, m := range c.data.Movies {
		if strings.Contains(strings.ToLower(m.Title), q) {
			result = append(result, m)
		}
	}
	return result
}

func (c *MovieCatalog) list() []Movie {
	return c.data.Movies
}

func (c *MovieCatalog) openPoster(id int) bool {
	movie := c.get(id)
	if movie == nil {
		return false
	}
	posterPath := movie.Poster
	if _, err := os.Stat(posterPath); os.IsNotExist(err) {
		fmt.Println(colorize("Постер не найден: "+posterPath, red))
		return false
	}
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("open", posterPath)
	case "windows":
		cmd = exec.Command("start", posterPath)
	default:
		cmd = exec.Command("xdg-open", posterPath)
	}
	err := cmd.Start()
	return err == nil
}

func main() {
	var (
		command     string
		title, poster, description, query string
		year        int
		rating      float64
		id          int
		dataFile    string
	)
	flag.StringVar(&command, "cmd", "", "Команда: add, remove, list, search, info, open")
	flag.StringVar(&title, "title", "", "Название")
	flag.IntVar(&year, "year", 0, "Год")
	flag.Float64Var(&rating, "rating", 0, "Рейтинг")
	flag.StringVar(&poster, "poster", "", "Путь к постеру")
	flag.StringVar(&description, "description", "", "Описание")
	flag.IntVar(&id, "id", 0, "ID")
	flag.StringVar(&query, "query", "", "Поисковый запрос")
	flag.StringVar(&dataFile, "data", "movies.json", "Файл данных")
	flag.Usage = func() {
		fmt.Println(`Использование: go run movies.go -cmd <команда> [опции]
  add       -title <title> -year <year> -rating <rating> -poster <poster> [-description <desc>]
  remove    -id <id>
  list
  search    -query <query>
  info      -id <id>
  open      -id <id>`)
	}
	flag.Parse()

	if command == "" {
		fmt.Println("Укажите -cmd")
		os.Exit(1)
	}

	catalog := NewMovieCatalog(dataFile)

	switch command {
	case "add":
		if title == "" || year == 0 || rating == 0 || poster == "" {
			fmt.Println("Ошибка: требуются -title, -year, -rating, -poster")
			os.Exit(1)
		}
		movie := catalog.add(title, year, rating, poster, description)
		fmt.Printf(colorize("Фильм добавлен: ID %d - %s\n", green), movie.ID, movie.Title)
	case "remove":
		if id == 0 {
			fmt.Println("Ошибка: укажите -id")
			os.Exit(1)
		}
		if catalog.remove(id) {
			fmt.Printf(colorize("Фильм с ID %d удалён\n", green), id)
		} else {
			fmt.Printf(colorize("Фильм с ID %d не найден\n", red), id)
		}
	case "list":
		movies := catalog.list()
		if len(movies) == 0 {
			fmt.Println("Нет фильмов.")
		} else {
			for _, m := range movies {
				fmt.Printf("%s | %s | %d | %s | %s\n",
					colorize(strconv.Itoa(m.ID), cyan),
					colorize(m.Title, bold),
					m.Year,
					colorize(strconv.FormatFloat(m.Rating, 'f', 1, 64), yellow),
					m.Poster)
			}
		}
	case "search":
		if query == "" {
			fmt.Println("Ошибка: укажите -query")
			os.Exit(1)
		}
		results := catalog.search(query)
		if len(results) == 0 {
			fmt.Println("Ничего не найдено.")
		} else {
			for _, m := range results {
				fmt.Printf("%s | %s | %d | %s | %s\n",
					colorize(strconv.Itoa(m.ID), cyan),
					colorize(m.Title, bold),
					m.Year,
					colorize(strconv.FormatFloat(m.Rating, 'f', 1, 64), yellow),
					m.Poster)
			}
		}
	case "info":
		if id == 0 {
			fmt.Println("Ошибка: укажите -id")
			os.Exit(1)
		}
		movie := catalog.get(id)
		if movie == nil {
			fmt.Printf(colorize("Фильм с ID %d не найден\n", red), id)
		} else {
			fmt.Printf("%s: %d\n", colorize("ID", cyan), movie.ID)
			fmt.Printf("%s: %s\n", colorize("Название", bold), movie.Title)
			fmt.Printf("Год: %d\n", movie.Year)
			fmt.Printf("Рейтинг: %s\n", colorize(strconv.FormatFloat(movie.Rating, 'f', 1, 64), yellow))
			fmt.Printf("Постер: %s\n", movie.Poster)
			fmt.Printf("Описание: %s\n", movie.Description)
		}
	case "open":
		if id == 0 {
			fmt.Println("Ошибка: укажите -id")
			os.Exit(1)
		}
		if catalog.openPoster(id) {
			fmt.Printf(colorize("Постер фильма ID %d открыт\n", green), id)
		} else {
			fmt.Printf(colorize("Не удалось открыть постер для ID %d\n", red), id)
		}
	default:
		fmt.Println("Неизвестная команда.")
	}
}
