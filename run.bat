@echo off
mvn -q clean package && java -jar target\SmartExpenseManager.jar
