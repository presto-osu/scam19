# Prerequisites

The experiments in our SCAM'19 paper are conducted on two machines
with Intel(R) Xeon(R) CPU E5-2630, 64GB RAM and Ubuntu 18.04 LTS.
Before the next steps, Android SDK and platform 27 need to be
installed, as well as Java 1.8+ and Python 3.

# Analysis and Instrumentation

The source code for the static analysis and instrumentation in the
paper is in the `code` folder.  We provide a script `gator` for the
ease of invocations of the Java program. 

## Static analysis

The source code for the static analysis is in a module called
`sootandroid`. It is build upon the Soot analysis framework.  To
build the project, first create a file `local.properties` indicating
the location of the Android SDK. This file complies with Android
Studio's configuration file. A sample is in the `code` directory.

Next, run the following commands:

```bash
$ cd code
$ $ANDROID_SDK/tools/bin/sdkmanager "platforms;android-27" # if you don't have it installed
$ ./gator b
```

To analyze an app, the APK file must be provided. Take the `parking`
app as an example, the command below invokes the analysis:

```bash
$ ./gator a -p ../apks/il.talent.parking.apk
```

The output (a dictionary of screen names in XML format) is by default
printed to the console. For other options, run the script with `-h`,
as follows:

```bash
$ ./gator a -h
```

The results for the 15 experimental subjects are stored in `xml`.

## Instrumentation and Monkey

As introduced in the paper, the instrumentation is to insert the
implemenation layer into closed-sourced APKs for recording of
run-time events. The source code for the instrumentation is in
`code/instrument`.  The implemenation layer is in package
`edu.osu.cse.presto.android.gator.instrument.runtime`.  We are
working on adapting other analytics frameworks, e.g., Firebase.

To run the instrumentation, use the `i` command for `gator`.  For the
example of the `parking` app, the command is as follows:

```bash
$ ./gator i -x xml/il.talent.parking.apk.xml -p ../apks/il.talent.parking.apk
```

The `-x` indicates the XML file for the dictionary and `-p` specifies
the original APK for the app.  Other options can be checked by
passing `-h` in a similar way as above.

The resulting instrumented APK will be stored in
`code/sootOutput/mani` folder.  There are several scripts to help
signing, installation and running of the APK. Script
`simulate_by_monkey.py` is a all-in-one script that atomatically
instruments and the run an APK using Monkey. Again, we take the
`parking` app as an example and show the usage of the script below:

```bash
$ cd sootOutput/mani
$ $ANDROID_SDK/tools/bin/sdkmanager "system-images;android-27;google_apis;x86" # if you don't have it installed
$ ./simulate_by_monkey.py -p ../../../apks/il.talent.parking.apk -v api_27_0 -d emulator-5554
```

Parameter `-p` indicates the path of the original APK. Parameters
`-v` and `-d` specify the name for the virtual device (AVD) and the
name for the run-time emulator. While an AVD's name can be arbitrary,
the emulator's name has to be in the form as `emulator-xxxx` where
`xxxx` is a four digit number that conforms to the requirements for
the `-port` option of Android SDK's `emulator` command. More
information can be found
[here](https://developer.android.com/studio/run/emulator-commandline).
Run the script with `-h` option shows other options/parameters.

This script will keep triggering the Monkey tool to generate random
events to be fed into the emulator. In our experiments, we stop this
process when the number of screen view events goes beyond `10x` the
size of the dictionary. The results are SQLite databases stored in
`code/sootOutput/mani/db`. All screen view events are in table
`hits`.


# Simulation and Plotting

Directory `simulation` includes the scripts for simulation of the
local randomizer in the paper, as well as for plotting the results.
Subfolder `figure` contains the generated figures (shown in the
evaluation section of the paper).  Use the following command to run
the script w/ and w/o sampling

```bash
$ ./simulate_randomization_and_plot.py 100 # without sampling
$ ./simulate_randomization_and_plot.py 100 10 # with sampling
```

This script reads the resulting databses from the above step.  The
first parameter is the number of simulated users per Monkey run.
When it is 100, `k=100x1`, `k=100x10`, and `k=100x1000` users are
simulated and their results will all be recorded for plotting. The
second parameter is the sampling size `t`. If not given, no sampling
is conducted.  The script also plots the results in the `figure`
folder.
