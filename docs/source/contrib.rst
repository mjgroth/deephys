============
Contributing
============

- https://github.com/mjgroth/deephys
  
Development Instructions 🤓
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This is kotlin/gradle project. To compile and run from source requires JDK. If you have never worked on java or kotlin code before, you likely do not have them. There are multiple ways to install these.

JDK Setup
~~~~~~~~~~~~~~~~~~

JDK 17 is recommended: We recommend installing openjdk17. Alternatively, JDK 17 corretto, which can be installed through IntelliJ, has worked for some users.

- Mac Installation: `Homebrew <https://brew.sh/>`_ with ``brew install openjdk@17``
  
- Linux Installation: ``sudo apt install openjdk-17-jdk``
  
- Windows Installation: (TBD)
  
Project Setup
~~~~~~~~~~~~~~~~~~~~~~~~~~

#. ``git clone --recurse-submodules -j10 https://github.com/mjgroth/deephys-aio``
   
#. ``cd deephys-aio``
   
Running From Source
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The main command is ``./gradlew :k:nn:deephys:run --stacktrace``. If experience any issues try to reset the settings and state
with ``./gradlew :k:nn:deephys:run --stacktrace --args="reset"`` and then try the command above again.

If you have never worked on a java project on your machine, you likely will get an error complaining about no JDK being found. There are a few ways you can handle this. One is to set your JAVA_HOME environmental variable e.g. ``JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :k:nn:deephys:run``. Another is to append ``org.gradle.java.home=/path/to/your/jdk/home`` to your ``~/.gradle/gradle.properties``. More information can be found in `Gradle documentation <https://docs.gradle.org/current/userguide/build_environment.html>`_. 
