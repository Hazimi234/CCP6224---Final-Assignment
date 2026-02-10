# CCP6224---Final-Assignment
Before running 

Run this command in terminal to include the JAR file
javac -cp ".;lib/sqlite-jdbc-3.51.1.0.jar" src/main/Main.java 

Then this command to run the program
java -cp ".;lib/sqlite-jdbc-3.51.1.0.jar" src.main.Main 

End

Command remove all the class
Get-ChildItem -Recurse -Filter *.class | Remove-Item