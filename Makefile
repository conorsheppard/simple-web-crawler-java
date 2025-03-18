SHELL := /bin/bash

default: build

build:
	mvn clean package

.SILENT:
.PHONY: default run
