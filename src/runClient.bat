javac -classpath ".;\javagaming\jinput\lib;src;src\*;\javagaming\jbullet\jbullet.jar;\javagaming\jinput\jinput.jar;\javagaming\rage165\ray.jar;\javagaming\vecmath\vecmath.jar;\javagaming\jogamp-all-platforms\jar\gluegen-rt.jar;\javagaming\jogamp-all-platforms\jar\jogl-all.jar" client\*.java ActionClasses\*.java Controllers\*.java InterfaceClasses\*.java server\*.java
set /p address="Enter Server address:"
java  -Djava.library.path=\javagaming\jinput\lib -Dsun.java2d.d3d=false -cp ".;\javagaming\jinput\lib;bin\client;bin\client\chainedGame.class;src;src\ActionClasses;src\assets\config;src\client;src\Controllers;src\InterfaceClasses;src\server;\javagaming\jbullet\jbullet.jar;\javagaming\jinput\jinput.jar;\javagaming\rage165\ray.jar;\javagaming\vecmath\vecmath.jar;\javagaming\jogamp-all-platforms\jar\gluegen-rt.jar;\javagaming\jogamp-all-platforms\jar\jogl-all.jar" -Xdiag client.chainedGame %address% 20
pause