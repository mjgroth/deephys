=========================
Contribution Instructions
=========================

.. tabs::

  .. tab:: Python
  
    To contribute to the python package, please follow these simple steps
    
    #. Fork `deephys <https://github.com/mjgroth/deephys>`_ (not the deephys-aio repository)
       
    #. Edit the python code
       
    #. Run the `python tests <https://github.com/mjgroth/deephys/tree/master/src/test/python>`_
       
    #. Check to make sure your edits are still compatible with these docs. If they are not, please edit the docs and include it in your pull request.
       
    #. Add your changes to the `changelist <https://github.com/mjgroth/deephys/blob/master/docs/source/pip/changelist.rst>`_
       
    #. Run `these google colabs <https://drive.google.com/drive/u/0/folders/1Phh_iLZvToAZ1BZVbOTam5hD4MlN2Vg->`_ (additional python tests)
       
    #. Submit a pull request
       
  
  .. tab:: MATLAB
  
    Our current method for exporting data from MATLAB deep learning pipelines is simply to access the python exporter tool through an embedded interpreter in MATLAB. Here is the :doc:`example code <../export/matlab>`.
    
    If you would like to help us on the MATLAB front, here are a few ideas:
    
    - You could flesh out our MATLAB example code, and perhaps create a full MATLAB demo with real deep learning data
      
    - You could create a formal MATLAB toolbox that wraps around the python package
      
    - You could write a independent MATLAB exporter tool that has the same set of features as the python tool, but without requiring the python tool. This will presumably be the most ideal solution in the long run, but will take the most work.
      
  
  .. tab:: Visualizer Tool
  
    .. note::
    
      If you are considering contributing to the kotlin app, please :doc:`reach out to us <../../contact>` so we can help you get started.
    
    Before contributing to the kotlin app, please consider the following:
    
    - The app must be rigorously tested with each change to prevent memory leaks, cpu bottlenecks, and other issues
      
    - The app is built for multiple operating systems (Mac OS Silicon, Mac OS Intel, Windows, Linux AArch64, Linux x86). The app should be tested on all of these platforms after any significant changes
      
    - The app uses a set of git submodules as libraries. Any changes to these submodules will need their own pull requests
      
    - Being performance-intensive software, using a profiler tool such as YourKit is highly recommended
      
    - Compiling the app will take a significant amount of time on older machines
      
    .. note::
    
      The main developer of this software uses a Silicon Mac. If you have a different operating system, you may face issues. If you do, please :doc:`let us know <../../contact>`.
    
    If you are still here, you must be very brave. Read on for step by step instructions...
    
    .. raw:: html
    
      <h2>Setting up JDK</h2>
    
    This is kotlin/gradle project. To compile and run from source requires JDK. If you have never worked on java or kotlin code before, you likely do not have them. There are multiple ways to install these.
    
    JDK 17 is recommended: We recommend installing openjdk17. Alternatively, JDK 17 corretto, which can be installed through IntelliJ, has worked for some users.
    
    - Mac Installation: `Homebrew <https://brew.sh/>`_ with ``brew install openjdk@17``
      
    - Linux Installation: ``sudo apt install openjdk-17-jdk``
      
    - Windows Installation: (TBD)
      
    .. raw:: html
    
      <h2>Setting up the Project</h2>
    
    Run the following commands to properly clone the repository
    
    .. code-block:: console
    
      git clone --recurse-submodules -j10 https://github.com/mjgroth/deephys-aio
      cd deephys-aio
    
    If you have never worked on a java project on your machine, you will need to tell gradle which JDK to use. There are a few ways you can do this.
    
    The recommended approach is to append ``org.gradle.java.home=/path/to/your/jdk/home`` to your ``~/.gradle/gradle.properties``. For more detailed information on setting up your gradle environment, see the `Gradle documentation <https://docs.gradle.org/current/userguide/build_environment.html>`_
    
    Common paths to java include:
    
    - **Mac:** ``/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home``
      
    .. raw:: html
    
      <h2>Running From Source</h2>
    
    Now we should make sure that the app runs from source. This is not strictly neccesary to edit the code, but it will be neccesary for tests later on. Use the following command:
    
    .. code-block:: console
    
      ./gradlew :k:nn:deephys:run --stacktrace --no-configuration-cache
    
    Note that when running from source through Gradle, command line arguments are passed to the app through a special argument. So if, for example, you need to reset the app, you will need to use a different syntax.
    
    Normally if running the compiled app from source, you would use:
    
    .. code-block:: console
    
      ./deephys reset
    
    But through gradle, you will need to use:
    
    .. code-block:: console
    
      ./gradlew :k:nn:deephys:run --args="reset" --no-configuration-cache --stacktrace
    
    If you successfully ran the app, then you are almost ready to start editing the code!
    
    .. raw:: html
    
      <h2>Concepts to understand before editing the source code</h2>
    
    Before editing the code, it may be helpful to understand some concepts:
    
    - Strong and weak references in java (critical for memory management and used throughout the code)
      
    - Kotlin lambdas (important to understand when a reference is strongly held in a lambda)
      
    .. raw:: html
    
      <h2>Testing</h2>
    
    If you would like to submit your edits, we request that you rigorously test the app.
    
    You can run the tests with the following command: 
    
    .. code-block:: console
    
      ./gradlew :k:nn:deephys:test --stacktrace --no-configuration-cache
    
    You may also add new tests to the test source code to test your new features.
    
    .. raw:: html
    
      <h2>Profiling</h2>
    
    Ideally, you can also profile your code. We understand this may not always be possible since most profilers are not free. But if you can, using a java profiler will really help you understand the memory and cpu consumption. This is not required for small edits but if you are adding new features to the app is likely a must.
    
    .. raw:: html
    
      <h2>Testing on other Operating Systems</h2>
    
    Your edits may behave unexpectedly on other operating systems. We request you try to test your changes on as many different operating systems as possible.
    
  
