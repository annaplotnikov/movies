# movies.py
# Movie Catalog (Posters) на Python

import sys
import os
import json
import argparse
import subprocess
from datetime import datetime
from typing import List, Dict, Optional

# ANSI-цвета для красивого вывода
RESET = "\033[0m"
BOLD = "\033[1m"
CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RED = "\033[91m"

def colorize(text, color):
    return f"{color}{text}{RESET}"

class Movie:
    def __init__(self, id: int, title: str, year: int, rating: float, poster: str, description: str = ""):
        self.id = id
        self.title = title
        self.year = year
        self.rating = rating
        self.poster = poster
        self.description = description

    def to_dict(self):
        return {
            "id": self.id,
            "title": self.title,
            "year": self.year,
            "rating": self.rating,
            "poster": self.poster,
            "description": self.description
        }

    @classmethod
    def from_dict(cls, data):
        return cls(data["id"], data["title"], data["year"], data["rating"], data["poster"], data.get("description", ""))

class MovieCatalog:
    def __init__(self, data_file="movies.json"):
        self.data_file = data_file
        self.movies: List[Movie] = []
        self.next_id = 1
        self.load()

    def load(self):
        if os.path.exists(self.data_file):
            try:
                with open(self.data_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    self.movies = [Movie.from_dict(m) for m in data.get("movies", [])]
                    self.next_id = data.get("next_id", 1)
            except:
                self.movies = []
                self.next_id = 1
        else:
            self.movies = []
            self.next_id = 1

    def save(self):
        data = {
            "movies": [m.to_dict() for m in self.movies],
            "next_id": self.next_id
        }
        with open(self.data_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

    def add(self, title: str, year: int, rating: float, poster: str, description: str = "") -> Movie:
        movie = Movie(self.next_id, title, year, rating, poster, description)
        self.movies.append(movie)
        self.next_id += 1
        self.save()
        return movie

    def remove(self, movie_id: int) -> bool:
        for i, m in enumerate(self.movies):
            if m.id == movie_id:
                del self.movies[i]
                self.save()
                return True
        return False

    def get(self, movie_id: int) -> Optional[Movie]:
        for m in self.movies:
            if m.id == movie_id:
                return m
        return None

    def search(self, query: str) -> List[Movie]:
        q = query.lower()
        return [m for m in self.movies if q in m.title.lower()]

    def list_all(self) -> List[Movie]:
        return self.movies

    def open_poster(self, movie_id: int) -> bool:
        movie = self.get(movie_id)
        if not movie:
            return False
        poster_path = movie.poster
        if not os.path.exists(poster_path):
            print(colorize(f"Постер не найден: {poster_path}", RED))
            return False
        # Открываем во внешнем просмотрщике
        if sys.platform == 'darwin':
            subprocess.run(['open', poster_path])
        elif sys.platform == 'win32':
            subprocess.run(['start', poster_path], shell=True)
        else:
            subprocess.run(['xdg-open', poster_path])
        return True

def main():
    parser = argparse.ArgumentParser(description="Movie Catalog (Posters)")
    parser.add_argument("command", choices=["add", "remove", "list", "search", "info", "open", "help"],
                        help="Команда")
    parser.add_argument("--title", help="Название фильма")
    parser.add_argument("--year", type=int, help="Год выпуска")
    parser.add_argument("--rating", type=float, help="Рейтинг")
    parser.add_argument("--poster", help="Путь к постеру")
    parser.add_argument("--description", help="Описание")
    parser.add_argument("--id", type=int, help="ID фильма")
    parser.add_argument("--query", help="Поисковый запрос")
    parser.add_argument("--data", default="movies.json", help="Файл данных")
    args = parser.parse_args()

    catalog = MovieCatalog(args.data)

    if args.command == "help":
        print(__doc__)
        sys.exit(0)

    elif args.command == "add":
        if not args.title or not args.year or not args.rating or not args.poster:
            print("Ошибка: требуются --title, --year, --rating, --poster")
            sys.exit(1)
        movie = catalog.add(args.title, args.year, args.rating, args.poster, args.description or "")
        print(colorize(f"Фильм добавлен: ID {movie.id} - {movie.title}", GREEN))

    elif args.command == "remove":
        if args.id is None:
            print("Ошибка: укажите --id")
            sys.exit(1)
        if catalog.remove(args.id):
            print(colorize(f"Фильм с ID {args.id} удалён", GREEN))
        else:
            print(colorize(f"Фильм с ID {args.id} не найден", RED))

    elif args.command == "list":
        movies = catalog.list_all()
        if not movies:
            print("Нет фильмов.")
        else:
            for m in movies:
                print(f"{colorize(str(m.id), CYAN)} | {colorize(m.title, BOLD)} | {m.year} | {colorize(str(m.rating), YELLOW)} | {m.poster}")

    elif args.command == "search":
        if not args.query:
            print("Ошибка: укажите --query")
            sys.exit(1)
        results = catalog.search(args.query)
        if not results:
            print("Ничего не найдено.")
        else:
            for m in results:
                print(f"{colorize(str(m.id), CYAN)} | {colorize(m.title, BOLD)} | {m.year} | {colorize(str(m.rating), YELLOW)} | {m.poster}")

    elif args.command == "info":
        if args.id is None:
            print("Ошибка: укажите --id")
            sys.exit(1)
        movie = catalog.get(args.id)
        if not movie:
            print(colorize(f"Фильм с ID {args.id} не найден", RED))
        else:
            print(colorize(f"ID: {movie.id}", CYAN))
            print(colorize(f"Название: {movie.title}", BOLD))
            print(f"Год: {movie.year}")
            print(f"Рейтинг: {colorize(str(movie.rating), YELLOW)}")
            print(f"Постер: {movie.poster}")
            print(f"Описание: {movie.description}")

    elif args.command == "open":
        if args.id is None:
            print("Ошибка: укажите --id")
            sys.exit(1)
        if catalog.open_poster(args.id):
            print(colorize(f"Постер фильма ID {args.id} открыт", GREEN))
        else:
            print(colorize(f"Не удалось открыть постер для ID {args.id}", RED))

if __name__ == "__main__":
    main()
