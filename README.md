# Simple Web Crawler 🕷️

<img src="./badges/jacoco.svg" style="display: flex;" alt="jacoco-test-coverage-badge">

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
./crawl https://monzo.com
```

You can simply run the above script and supply the base URL as a command line argument, the script will then build and
run the application.

## 🌍 Distributed Crawler

_**Disclaimer:** This crawler is not fully distributed but rather a first step towards making it fully distributed.
By extracting the queue out into a Kafka instance and the cache into a Redis instance, we loosely couple the queue and
cache and make them available to other worker nodes.  
So even though we don't currently have any other worker nodes, and some parts of the system are still very much
purpose-built for one machine, we are well on our way to a modular web crawler that can be run on one machine or in a
cluster or swarm of crawler containers on Kubernetes (or your choice of container orchestration platform).  
Each crawler in the distributed cluster could also be utilising concurrency across hundreds of threads to process links,
therefore we combine the power of concurrency and distribution ⚡️._

## 📝 Improvements
- The JSoup framework creates a large number of TCP connections, one for every URL. 
If HttpClient were explicitly used, it could cache connections for reuse and be more network efficient.
- Implementation of politeness
- Depth limit
- Store the URLs scraped as a graph structure and persist in a graph database such as Neo4j, this could help with:
    - Visualising the website structure 
    - Detecting loops & broken links
    - Finding the shortest path between two pages (Dijkstra, Bellman-Ford)
