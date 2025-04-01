SHELL := /bin/bash

default: build

clean:
	mvn clean

build: clean
	mvn package

install: clean
	mvn install -U

test:
	mvn test

up:
	docker compose up

down:
	docker compose down -v && rm -rf kafka/data/

docker-build:
	docker build -t simple-web-crawler-java .

test-coverage:
	./mvnw clean org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.12:report

check-coverage:
	open -a Google\ Chrome target/jacoco-report/index.html

coverage-badge-gen:
	python3 -m jacoco_badge_generator -j target/jacoco-report/jacoco.csv

test-suite: test-coverage check-coverage coverage-badge-gen

.SILENT:
.PHONY: default clean build install test up down docker-build test-coverage check-coverage coverage-badge-gen test-suite
