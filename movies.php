<?php
// movies.php
// Movie Catalog (Posters) на PHP

if (php_sapi_name() !== 'cli') {
    die("Это консольное приложение.\n");
}

// ANSI-цвета
define('RESET', "\033[0m");
define('BOLD', "\033[1m");
define('CYAN', "\033[96m");
define('GREEN', "\033[92m");
define('YELLOW', "\033[93m");
define('RED', "\033[91m");

function colorize($text, $color) {
    return $color . $text . RESET;
}

class Movie {
    public $id;
    public $title;
    public $year;
    public $rating;
    public $poster;
    public $description;

    public function __construct($id, $title, $year, $rating, $poster, $description = '') {
        $this->id = $id;
        $this->title = $title;
        $this->year = $year;
        $this->rating = $rating;
        $this->poster = $poster;
        $this->description = $description;
    }
}

class MovieCatalog {
    private $dataFile;
    private $movies = [];
    private $nextId = 1;

    public function __construct($dataFile = 'movies.json') {
        $this->dataFile = $dataFile;
        $this->load();
    }

    private function load() {
        if (!file_exists($this->dataFile)) {
            $this->movies = [];
            $this->nextId = 1;
            return;
        }
        $json = file_get_contents($this->dataFile);
        $data = json_decode($json, true);
        if (!$data) {
            $this->movies = [];
            $this->nextId = 1;
            return;
        }
        $this->movies = array_map(function($m) {
            return new Movie($m['id'], $m['title'], $m['year'], $m['rating'], $m['poster'], $m['description'] ?? '');
        }, $data['movies'] ?? []);
        $this->nextId = $data['next_id'] ?? 1;
    }

    private function save() {
        $data = [
            'movies' => array_map(function($m) {
                return ['id' => $m->id, 'title' => $m->title, 'year' => $m->year, 'rating' => $m->rating, 'poster' => $m->poster, 'description' => $m->description];
            }, $this->movies),
            'next_id' => $this->nextId
        ];
        file_put_contents($this->dataFile, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    }

    public function add($title, $year, $rating, $poster, $description = '') {
        $movie = new Movie($this->nextId, $title, $year, $rating, $poster, $description);
        $this->movies[] = $movie;
        $this->nextId++;
        $this->save();
        return $movie;
    }

    public function remove($id) {
        foreach ($this->movies as $i => $m) {
            if ($m->id == $id) {
                array_splice($this->movies, $i, 1);
                $this->save();
                return true;
            }
        }
        return false;
    }

    public function get($id) {
        foreach ($this->movies as $m) {
            if ($m->id == $id) return $m;
        }
        return null;
    }

    public function search($query) {
        $q = strtolower($query);
        return array_filter($this->movies, function($m) use ($q) {
            return strpos(strtolower($m->title), $q) !== false;
        });
    }

    public function list() {
        return $this->movies;
    }

    public function openPoster($id) {
        $movie = $this->get($id);
        if (!$movie) return false;
        $posterPath = $movie->poster;
        if (!file_exists($posterPath)) {
            echo colorize("Постер не найден: $posterPath", RED) . "\n";
            return false;
        }
        if (PHP_OS_FAMILY === 'Windows') {
            exec("start $posterPath");
        } elseif (PHP_OS_FAMILY === 'Darwin') {
            exec("open $posterPath");
        } else {
            exec("xdg-open $posterPath");
        }
        return true;
    }
}

$args = array_slice($argv, 1);
if (empty($args) || $args[0] == 'help') {
    echo "Использование: php movies.php <команда> [опции]\n";
    echo "  add       --title <title> --year <year> --rating <rating> --poster <poster> [--description <desc>]\n";
    echo "  remove    --id <id>\n";
    echo "  list\n";
    echo "  search    --query <query>\n";
    echo "  info      --id <id>\n";
    echo "  open      --id <id>\n";
    exit(0);
}

$command = $args[0];
$options = [];
for ($i = 1; $i < count($args); $i++) {
    if (strpos($args[$i], '--') === 0) {
        $key = substr($args[$i], 2);
        if (isset($args[$i+1]) && strpos($args[$i+1], '--') !== 0) {
            $options[$key] = $args[++$i];
        } else {
            $options[$key] = '';
        }
    }
}

$dataFile = $options['data'] ?? 'movies.json';
$catalog = new MovieCatalog($dataFile);

switch ($command) {
    case 'add':
        if (empty($options['title']) || empty($options['year']) || empty($options['rating']) || empty($options['poster'])) {
            echo "Ошибка: требуются --title, --year, --rating, --poster\n";
            exit(1);
        }
        $movie = $catalog->add($options['title'], (int)$options['year'], (float)$options['rating'], $options['poster'], $options['description'] ?? '');
        echo colorize("Фильм добавлен: ID $movie->id - $movie->title", GREEN) . "\n";
        break;
    case 'remove':
        if (empty($options['id'])) {
            echo "Ошибка: укажите --id\n";
            exit(1);
        }
        $id = (int)$options['id'];
        if ($catalog->remove($id)) {
            echo colorize("Фильм с ID $id удалён", GREEN) . "\n";
        } else {
            echo colorize("Фильм с ID $id не найден", RED) . "\n";
        }
        break;
    case 'list':
        $movies = $catalog->list();
        if (empty($movies)) {
            echo "Нет фильмов.\n";
        } else {
            foreach ($movies as $m) {
                echo colorize($m->id, CYAN) . " | " . colorize($m->title, BOLD) . " | $m->year | " . colorize($m->rating, YELLOW) . " | $m->poster\n";
            }
        }
        break;
    case 'search':
        if (empty($options['query'])) {
            echo "Ошибка: укажите --query\n";
            exit(1);
        }
        $results = $catalog->search($options['query']);
        if (empty($results)) {
            echo "Ничего не найдено.\n";
        } else {
            foreach ($results as $m) {
                echo colorize($m->id, CYAN) . " | " . colorize($m->title, BOLD) . " | $m->year | " . colorize($m->rating, YELLOW) . " | $m->poster\n";
            }
        }
        break;
    case 'info':
        if (empty($options['id'])) {
            echo "Ошибка: укажите --id\n";
            exit(1);
        }
        $id = (int)$options['id'];
        $movie = $catalog->get($id);
        if (!$movie) {
            echo colorize("Фильм с ID $id не найден", RED) . "\n";
        } else {
            echo colorize("ID: $movie->id", CYAN) . "\n";
            echo colorize("Название: $movie->title", BOLD) . "\n";
            echo "Год: $movie->year\n";
            echo "Рейтинг: " . colorize($movie->rating, YELLOW) . "\n";
            echo "Постер: $movie->poster\n";
            echo "Описание: $movie->description\n";
        }
        break;
    case 'open':
        if (empty($options['id'])) {
            echo "Ошибка: укажите --id\n";
            exit(1);
        }
        $id = (int)$options['id'];
        if ($catalog->openPoster($id)) {
            echo colorize("Постер фильма ID $id открыт", GREEN) . "\n";
        } else {
            echo colorize("Не удалось открыть постер для ID $id", RED) . "\n";
        }
        break;
    default:
        echo "Неизвестная команда.\n";
}
