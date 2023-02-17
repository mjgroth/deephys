===================
GitHub Repositories
===================

The main repository for the Deephys Visualizer is `deephys-aio <https://github.com/mjgroth/deephys-aio>`_. **AIO** stands for all-in-one. It organized its code into several submodules.

To properly clone this repository, the argument ``--recurse-submodules`` must be used or else the submodules will be empty. ``-j10`` will clone the submodules in parallel, saving you time.

.. code-block:: console

  git clone --recurse-submodules -j10 https://github.com/mjgroth/deephys-aio
  cd deephys-aio

To properly update these repositories, ``git pull`` will not be enough. You will need to use:

.. code-block:: console

  git submodule update --recursive

The core submodule repository is `deephys <https://github.com/mjgroth/deephys>`_. The source code for the visualizer in this repository is only functional as a submodule within deephys-aio. It is not standalone. This GitHub repository also contains:

- the CI actions that build the app on several platforms
  
- the code for the python package
  
- the code for these docs
  
- the app releases and changelists
  