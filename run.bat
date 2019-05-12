set /p address="Enter Server address:"
java -Dsun.java2d.d3d=false /bin/client/chainedGame.class %address% 20
pause