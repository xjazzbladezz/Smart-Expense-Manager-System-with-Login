#!/bin/sh
mvn -q clean package && java -jar target/SmartExpenseManager.jar
