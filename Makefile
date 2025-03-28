SHELL := /bin/bash

default: build

clean:
	mvn clean

build: clean
	mvn clean package -Dorg.jline.terminal.dumb=true

install: clean
	mvn install -U

test:
	mvn test

up:
	docker compose up

down:
	docker compose down -v

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

test-coverage:
	./mvnw clean org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.12:report

check-coverage:
	open -a Google\ Chrome target/jacoco-report/index.html

coverage-badge-gen:
	python3 -m jacoco_badge_generator -j target/jacoco-report/jacoco.csv

test-suite: test-coverage check-coverage coverage-badge-gen

.SILENT:
.PHONY: default clean build install test build-native run-native docker-build docker-run run-jar test-coverage check-coverage coverage-badge-gen test-suite
