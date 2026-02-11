import sys
import requests
from bs4 import BeautifulSoup


def main(url):
    try:
        resp = requests.get(url)
        soup = BeautifulSoup(resp.content, "html.parser")
        title = soup.title.string if soup.title else "No Title"
        # Просто выводим title, Clojure примет это как строку
        print(title)
    except Exception as e:
        print(f"Error: {e}")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        main(sys.argv[1])
    else:
        print("No URL provided")
