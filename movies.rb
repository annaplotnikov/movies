# movies.rb
# Movie Catalog (Posters) на Ruby

require 'json'
require 'optparse'

# ANSI-цвета
RESET = "\033[0m"
BOLD = "\033[1m"
CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RED = "\033[91m"

def colorize(text, color)
  "#{color}#{text}#{RESET}"
end

class Movie
  attr_accessor :id, :title, :year, :rating, :poster, :description

  def initialize(id, title, year, rating, poster, description = '')
    @id = id
    @title = title
    @year = year
    @rating = rating
    @poster = poster
    @description = description
  end

  def to_h
    { id: @id, title: @title, year: @year, rating: @rating, poster: @poster, description: @description }
  end
end

class MovieCatalog
  attr_reader :movies, :next_id

  def initialize(data_file = 'movies.json')
    @data_file = data_file
    load_data
  end

  def load_data
    if File.exist?(@data_file)
      data = JSON.parse(File.read(@data_file), symbolize_names: true)
      @movies = data[:movies].map { |m| Movie.new(m[:id], m[:title], m[:year], m[:rating], m[:poster], m[:description]) }
      @next_id = data[:next_id] || 1
    else
      @movies = []
      @next_id = 1
    end
  end

  def save_data
    data = {
      movies: @movies.map(&:to_h),
      next_id: @next_id
    }
    File.write(@data_file, JSON.pretty_generate(data))
  end

  def add(title, year, rating, poster, description = '')
    movie = Movie.new(@next_id, title, year, rating, poster, description)
    @movies << movie
    @next_id += 1
    save_data
    movie
  end

  def remove(id)
    @movies.delete_if { |m| m.id == id }.tap { |removed| save_data if removed.any? }.any?
  end

  def get(id)
    @movies.find { |m| m.id == id }
  end

  def search(query)
    q = query.downcase
    @movies.select { |m| m.title.downcase.include?(q) }
  end

  def list
    @movies
  end

  def open_poster(id)
    movie = get(id)
    return false unless movie
    poster_path = movie.poster
    unless File.exist?(poster_path)
      puts colorize("Постер не найден: #{poster_path}", RED)
      return false
    end
    if RUBY_PLATFORM =~ /darwin/
      system("open #{poster_path}")
    elsif RUBY_PLATFORM =~ /mingw|mswin/
      system("start #{poster_path}")
    else
      system("xdg-open #{poster_path}")
    end
    true
  end
end

options = {}
OptionParser.new do |opts|
  opts.banner = "Использование: ruby movies.rb <команда> [опции]\n" +
                "  add       --title <title> --year <year> --rating <rating> --poster <poster> [--description <desc>]\n" +
                "  remove    --id <id>\n" +
                "  list\n" +
                "  search    --query <query>\n" +
                "  info      --id <id>\n" +
                "  open      --id <id>"
  opts.on("--title TITLE") { |v| options[:title] = v }
  opts.on("--year YEAR") { |v| options[:year] = v.to_i }
  opts.on("--rating RATING") { |v| options[:rating] = v.to_f }
  opts.on("--poster POSTER") { |v| options[:poster] = v }
  opts.on("--description DESC") { |v| options[:description] = v }
  opts.on("--id ID") { |v| options[:id] = v.to_i }
  opts.on("--query QUERY") { |v| options[:query] = v }
  opts.on("--data FILE") { |v| options[:data] = v }
end.parse!

command = ARGV[0]
unless command
  puts "Укажите команду"
  exit 1
end

data_file = options[:data] || 'movies.json'
catalog = MovieCatalog.new(data_file)

case command
when 'add'
  unless options[:title] && options[:year] && options[:rating] && options[:poster]
    puts "Ошибка: требуются --title, --year, --rating, --poster"
    exit 1
  end
  movie = catalog.add(options[:title], options[:year], options[:rating], options[:poster], options[:description] || '')
  puts colorize("Фильм добавлен: ID #{movie.id} - #{movie.title}", GREEN)

when 'remove'
  unless options[:id]
    puts "Ошибка: укажите --id"
    exit 1
  end
  if catalog.remove(options[:id])
    puts colorize("Фильм с ID #{options[:id]} удалён", GREEN)
  else
    puts colorize("Фильм с ID #{options[:id]} не найден", RED)
  end

when 'list'
  movies = catalog.list
  if movies.empty?
    puts "Нет фильмов."
  else
    movies.each do |m|
      puts "#{colorize(m.id.to_s, CYAN)} | #{colorize(m.title, BOLD)} | #{m.year} | #{colorize(m.rating.to_s, YELLOW)} | #{m.poster}"
    end
  end

when 'search'
  unless options[:query]
    puts "Ошибка: укажите --query"
    exit 1
  end
  results = catalog.search(options[:query])
  if results.empty?
    puts "Ничего не найдено."
  else
    results.each do |m|
      puts "#{colorize(m.id.to_s, CYAN)} | #{colorize(m.title, BOLD)} | #{m.year} | #{colorize(m.rating.to_s, YELLOW)} | #{m.poster}"
    end
  end

when 'info'
  unless options[:id]
    puts "Ошибка: укажите --id"
    exit 1
  end
  movie = catalog.get(options[:id])
  if movie.nil?
    puts colorize("Фильм с ID #{options[:id]} не найден", RED)
  else
    puts colorize("ID: #{movie.id}", CYAN)
    puts colorize("Название: #{movie.title}", BOLD)
    puts "Год: #{movie.year}"
    puts "Рейтинг: #{colorize(movie.rating.to_s, YELLOW)}"
    puts "Постер: #{movie.poster}"
    puts "Описание: #{movie.description}"
  end

when 'open'
  unless options[:id]
    puts "Ошибка: укажите --id"
    exit 1
  end
  if catalog.open_poster(options[:id])
    puts colorize("Постер фильма ID #{options[:id]} открыт", GREEN)
  else
    puts colorize("Не удалось открыть постер для ID #{options[:id]}", RED)
  end

else
  puts "Неизвестная команда."
end
