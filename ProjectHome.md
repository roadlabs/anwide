Android Web Based Integrated Development Environment (anwide) is an open source project that allows users to easily write programs using a web browser as development tool, so any computer/tablet or device with a browser can use it.

It consists of an Android application with an embedded webserver. After installing and opening the application on an Android device, a IP address will be shown on the mobile device's screen. Then you can use any browser to access that IP and connect to anwide.

After accessing anwide, you'll have the options of typing your source code or drawing your program with blocks from Blockly. After blocks are ready, simply click in execute that the system will translate the blocks into code, send to the device, and execute it.

The program's standard error and output can be seen on the anwide page.

In order to work, anwide relies heavily on Android Scripting layer (SL4A) and Blockly:

http://code.google.com/p/blockly/?redir=1

http://code.google.com/p/android-scripting/


We are working on blocks for Android built-in functions from SL4A, such as text to speech and for Lego NXT, so that robots can be programmed with these blocks using a cell phone as robot's controller (a CellBot).

Some references about CellBots can be found at:

http://www.natalnet.br/~aroca/


The idea came from discussions in the NatalNet laboratory (http://www.natalnet.br) about educational robotics with Prof. Aquiles Burlamaqui, Rummenigge Dantas and Luiz Gonçalves.

The actual code is a proof of concept written in a couple of hours, so it needs huge refactoring and error handling.


A screenshot explains the concept better:

![http://anwide.googlecode.com/files/anwide.png](http://anwide.googlecode.com/files/anwide.png)


Just after finishing to code anwide, a friend told me about MIT's App Inventor:

http://beta.appinventor.mit.edu/

I loved it! It works similarly to this project, but the project is designed on their web servers and not on the phone's webserver.


2012, NatalNet Laboratory

![http://www.natalnet.br/drupal/sites/default/files/natalnet_logo.png](http://www.natalnet.br/drupal/sites/default/files/natalnet_logo.png)

