del temp-messages.jar
mvn clean package spring-boot:repackage -Pproduction
copy "%cd%\target\temp-messages-0.0.1-SNAPSHOT.jar" "%cd%\temp-messages.jar"