# [autosqueal](https://git.beatrice.wtf/Tools/autosqeal)
[![Build Status](https://drone.beatrice.wtf/api/badges/Tools/autosqeal/status.svg?ref=refs/heads/main)](https://drone.beatrice.wtf/Tools/autosqeal)
[![Quality Gate Status](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=alert_status&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Reliability Rating](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=reliability_rating&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Security Rating](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=security_rating&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Maintainability Rating](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=sqale_rating&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Lines of Code](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=ncloc&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
  
  
*little java tool to automatically perform mouse actions*  

*once started, it only acts when you are away: it detects your activity and pauses itself while you are using the machine, so it never fights you for the mouse.*  
  
## supported systems  
| system    | support    |
|-----------|------------|
| macOS     | ✅ complete |
| GNU/Linux | ⏳ planned  |
| Windows   | ⏳ planned  |
  
## building  
**required tools**  
 - java 25 sdk  
 - git  
 - maven  
  
**build steps**  
 1. clone the official repository linked below using `git clone`.  
 2. `cd` into the directory and run `mvn clean package`.  
 3. you will find a runnable jar with dependencies in the `target/` folder.  
 4. run the built jar file with `java -jar target/autosqueal-*.jar`.

**macos .app bundle**  
 - on macos, run `mvn clean package -Pmac-app`: you will find `autosqueal.app` in `target/jpackage/`.  
 - the first time you run it, grant it the device control and data access permission (called "accessibility" on older macos versions) and the screen recording one, in system settings, like any other java app.
  
## support  
| category            | info                                                          |
|---------------------|---------------------------------------------------------------|
| official repository | [gitea src](https://git.beatrice.wtf/Tools/autosqeal.git)     |
| mirror repository   | [github src](https://github.com/mind-overflow/autosqueal.git) |
| build status        | [drone-ci](https://drone.beatrice.wtf/Tools/autosqeal)        |
| license             | copyright, all rights reserved — see [license](LICENSE)       |
| dev email           | [hello@beatrice.wtf](mailto:hello@beatrice.wtf)               |

