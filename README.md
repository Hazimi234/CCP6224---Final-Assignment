# CCP6224---Final-Assignment
How to run

1. Command remove all the class
Get-ChildItem -Recurse -Filter *.class | Remove-Item

2. Run this command in terminal to compile the program
javac -cp ".;lib/sqlite-jdbc-3.51.1.0.jar" src/main/Main.java 

3. Then this command to run the program
java -cp ".;lib/sqlite-jdbc-3.51.1.0.jar" src.main.Main 

End

