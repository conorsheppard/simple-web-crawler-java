# Simple Web Crawler 🕷️

A simple command-line web crawler built in **Java** using **JSoup**.  
It starts from a given URL, visits each page on the **same domain**, and prints all discovered links.

## 🚀 Features
- Crawls a website starting from a **base URL**
- Only follows links on the **same subdomain**
- Uses [JSoup](https://jsoup.org/) for HTML parsing
- Runs as a **CLI tool** with a clean wrapper script

---

## 📦 Build and Run 🏃🏻

```shell
make build
```

```shell
./crawl https://monzo.com
```
