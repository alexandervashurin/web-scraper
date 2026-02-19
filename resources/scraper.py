"""
Python Bridge для web-scraper.
Используется для парсинга динамических сайтов через Selenium или requests.
Выводит результат в формате JSON.
"""
import sys
import json
import requests
from bs4 import BeautifulSoup


def parse_html(url: str) -> dict:
    """Парсит HTML и возвращает структурированные данные."""
    try:
        resp = requests.get(url, timeout=10)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.content, "html.parser")

        # Извлекаем заголовок
        title = "N/A"
        for tag in ["h1", "h2"]:
            el = soup.find(tag)
            if el and el.get_text(strip=True):
                title = el.get_text(strip=True)
                break

        # Извлекаем параграфы
        paragraphs = []
        for p in soup.find_all("p"):
            text = p.get_text(strip=True)
            if text:
                paragraphs.append(text)

        content = "\n\n".join(paragraphs) if paragraphs else "N/A"

        return {
            "source": url,
            "type": "python-hybrid",
            "title": title,
            "content": content
        }
    except Exception as e:
        return {
            "source": url,
            "type": "python-hybrid",
            "title": "Error",
            "content": str(e)
        }


def main(url: str) -> None:
    """Основная функция - выводит JSON в stdout."""
    result = parse_html(url)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    if len(sys.argv) > 1:
        main(sys.argv[1])
    else:
        print(json.dumps({"error": "No URL provided"}))
