// movies.js
// Movie Catalog (Posters) на JavaScript (Node.js)

const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

// ANSI-цвета
const RESET = '\x1b[0m';
const BOLD = '\x1b[1m';
const CYAN = '\x1b[96m';
const GREEN = '\x1b[92m';
const YELLOW = '\x1b[93m';
const RED = '\x1b[91m';

function colorize(text, color) {
    return `${color}${text}${RESET}`;
}

class Movie {
    constructor(id, title, year, rating, poster, description = '') {
        this.id = id;
        this.title = title;
        this.year = year;
        this.rating = rating;
        this.poster = poster;
        this.description = description;
    }
}

class MovieCatalog {
    constructor(dataFile = 'movies.json') {
        this.dataFile = dataFile;
        this.movies = [];
        this.nextId = 1;
        this.load();
    }

    load() {
        try {
            if (fs.existsSync(this.dataFile)) {
                const data = JSON.parse(fs.readFileSync(this.dataFile, 'utf-8'));
                this.movies = data.movies.map(m => new Movie(m.id, m.title, m.year, m.rating, m.poster, m.description));
                this.nextId = data.next_id || 1;
            }
        } catch (e) {
            this.movies = [];
            this.nextId = 1;
        }
    }

    save() {
        const data = {
            movies: this.movies.map(m => ({ id: m.id, title: m.title, year: m.year, rating: m.rating, poster: m.poster, description: m.description })),
            next_id: this.nextId
        };
        fs.writeFileSync(this.dataFile, JSON.stringify(data, null, 2), 'utf-8');
    }

    add(title, year, rating, poster, description = '') {
        const movie = new Movie(this.nextId, title, year, rating, poster, description);
        this.movies.push(movie);
        this.nextId++;
        this.save();
        return movie;
    }

    remove(id) {
        const idx = this.movies.findIndex(m => m.id === id);
        if (idx === -1) return false;
        this.movies.splice(idx, 1);
        this.save();
        return true;
    }

    get(id) {
        return this.movies.find(m => m.id === id) || null;
    }

    search(query) {
        const q = query.toLowerCase();
        return this.movies.filter(m => m.title.toLowerCase().includes(q));
    }

    list() {
        return this.movies;
    }

    openPoster(id) {
        const movie = this.get(id);
        if (!movie) return false;
        const posterPath = movie.poster;
        if (!fs.existsSync(posterPath)) {
            console.log(colorize(`Постер не найден: ${posterPath}`, RED));
            return false;
        }
        const cmd = process.platform === 'darwin' ? 'open' :
                    process.platform === 'win32' ? 'start' : 'xdg-open';
        exec(`${cmd} "${posterPath}"`);
        return true;
    }
}

function main() {
    const args = process.argv.slice(2);
    if (args.length === 0 || args[0] === 'help') {
        console.log(`Использование: node movies.js <команда> [опции]
  add       --title <title> --year <year> --rating <rating> --poster <poster> [--description <desc>]
  remove    --id <id>
  list
  search    --query <query>
  info      --id <id>
  open      --id <id>
  help`);
        process.exit(0);
    }

    const command = args[0];
    const options = {};
    for (let i = 1; i < args.length; i++) {
        if (args[i].startsWith('--')) {
            const key = args[i].slice(2);
            const value = args[++i];
            options[key] = value;
        }
    }

    const dataFile = options.data || 'movies.json';
    const catalog = new MovieCatalog(dataFile);

    switch (command) {
        case 'add': {
            if (!options.title || !options.year || !options.rating || !options.poster) {
                console.error('Ошибка: требуются --title, --year, --rating, --poster');
                process.exit(1);
            }
            const movie = catalog.add(options.title, parseInt(options.year), parseFloat(options.rating), options.poster, options.description || '');
            console.log(colorize(`Фильм добавлен: ID ${movie.id} - ${movie.title}`, GREEN));
            break;
        }
        case 'remove': {
            if (!options.id) {
                console.error('Ошибка: укажите --id');
                process.exit(1);
            }
            const id = parseInt(options.id);
            if (catalog.remove(id)) {
                console.log(colorize(`Фильм с ID ${id} удалён`, GREEN));
            } else {
                console.log(colorize(`Фильм с ID ${id} не найден`, RED));
            }
            break;
        }
        case 'list': {
            const movies = catalog.list();
            if (movies.length === 0) {
                console.log('Нет фильмов.');
            } else {
                for (const m of movies) {
                    console.log(`${colorize(String(m.id), CYAN)} | ${colorize(m.title, BOLD)} | ${m.year} | ${colorize(String(m.rating), YELLOW)} | ${m.poster}`);
                }
            }
            break;
        }
        case 'search': {
            if (!options.query) {
                console.error('Ошибка: укажите --query');
                process.exit(1);
            }
            const results = catalog.search(options.query);
            if (results.length === 0) {
                console.log('Ничего не найдено.');
            } else {
                for (const m of results) {
                    console.log(`${colorize(String(m.id), CYAN)} | ${colorize(m.title, BOLD)} | ${m.year} | ${colorize(String(m.rating), YELLOW)} | ${m.poster}`);
                }
            }
            break;
        }
        case 'info': {
            if (!options.id) {
                console.error('Ошибка: укажите --id');
                process.exit(1);
            }
            const id = parseInt(options.id);
            const movie = catalog.get(id);
            if (!movie) {
                console.log(colorize(`Фильм с ID ${id} не найден`, RED));
            } else {
                console.log(colorize(`ID: ${movie.id}`, CYAN));
                console.log(colorize(`Название: ${movie.title}`, BOLD));
                console.log(`Год: ${movie.year}`);
                console.log(`Рейтинг: ${colorize(String(movie.rating), YELLOW)}`);
                console.log(`Постер: ${movie.poster}`);
                console.log(`Описание: ${movie.description}`);
            }
            break;
        }
        case 'open': {
            if (!options.id) {
                console.error('Ошибка: укажите --id');
                process.exit(1);
            }
            const id = parseInt(options.id);
            if (catalog.openPoster(id)) {
                console.log(colorize(`Постер фильма ID ${id} открыт`, GREEN));
            } else {
                console.log(colorize(`Не удалось открыть постер для ID ${id}`, RED));
            }
            break;
        }
        default:
            console.log('Неизвестная команда.');
    }
}

main();
