# EventManaging_Play_ScalaJS
Repository for an event managing web application develloped during the SCALA course 2016-2017 at HEIG-VD. The application is a cross project consiting of a Play back-end and a ScalaJS front-end

## Requirements
You need to install [SBT](http://www.scala-sbt.org/0.13/docs/Setup.html).
You also need to setup a MySQL database. The configuration for the MySQL database is available in `server/conf/application.conf`.
You need the JDK in version 1.8 at least.

## Deployment
To create the tables and the schema in the MySQL database, you need to run the file `database/Tables.sql`.
To run the application on your local environment, you just have to start sbt by running `sbt` and then execute the sbt command `run`in the root folder of the project. It will create a lightweight HTTP server available at `http://localhost:9000`.
The server password is 1234 by default. If you want to change it, just edit server.password in `server/conf/application.conf`.
