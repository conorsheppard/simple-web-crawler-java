SHELL := /bin/bash

default: build

build:
	mvn clean package

build-native:
	docker build -f Dockerfile.native -t simple-web-crawler-java-native .

run-native:
	docker run -it --rm simple-web-crawler-java-native http://quotes.toscrape.com

docker-build:
	docker build -t simple-web-crawler-java .

docker-run:
	docker run -it --rm simple-web-crawler-java http://quotes.toscrape.com

run-jar:
	java -jar simple-web-crawler-java.jar http://quotes.toscrape.com

.SILENT:
.PHONY: default run
