This folder contains all the libs that are fetched using Maven dependency feature.
The hmi-jars project is empty project just created to fetch the libs.
Add the required dependencies in pom folder.
Go to this folder (e.g./Users/purushottam_d/MyWorkspace/Server)  and execute this command.

$ mvn dependency:copy-dependencies -DoutputDirectory=/Users/purushottam_d/MyWorkspace/HMI-libs