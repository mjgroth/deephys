🐛 Reporting Bugs
=================

Please submit feature requests, bug reports, and other issues to our `Bug Tracker <https://deephys.youtrack.cloud/>`_

In order for us to most effectively resolve the issue, please provide the following information in your report:

#. Your operating system
   
#. A description of what you expected to happen
   
#. A description of what happened instead
   
#. **The full error output**. This is very critical for us to fix an error.
   
   - For most errors, a pop up will show with the error output. Please copy and paste this.
     
   - For some errors, there is no error pop up. For example, if the app crashes. If this is the case, please open up the app and go to the settings (click the gear). In the settings, there is a button that will open the logs folder. Please find the error text here. If you are not sure which file to send, please zip the entire "log" folder and send attach it to the issue.
     
   - If you cannot open the app, you won't be able to click the button to find the log folder. Here is where you might find it on each os:
     
     - Mac: ``/Users/<you>/Library/Application Support/Deephys/Log``
       
     - Windows: ``C:\\Users\<you>\AppData\Roaming\Deephys\Log``
       
     - Linux ``<your home folder>/.matt/Deephys/Log``
       
#. **A complete list of steps in order to reproduce the issue**. Sometimes this is not possible or easy. But if you can tell us, step by step, how to reproduce the error on our end it makes the bug-fixing process much easier.
   
   - If you generated your own data (.test and .model files) and you think that the bug has something to do with these files:
     
     - please share the files
       
     - If possible, please share the code that generated these files. Ideally, share a functional google colab.
       
the **error output** and **reproduce steps** are the most important. With both of these provided, we can provide a speedy bug-extermination 😳🐛😵

If you cannot find the error message in the log file or would find it more convenient to see it appear in real time in your terminal, you can follow these steps on Mac:

#. Navigate in terminal to the folder ``deephy.app/Contents/MacOS``
   
#. Run the command ``./deephy``
   