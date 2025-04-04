# [autosqueal](https://git.beatrice.wtf/Tools/autosqeal)
[![Build Status](https://drone.beatrice.wtf/api/badges/Tools/autosqeal/status.svg?ref=refs/heads/main)](https://drone.beatrice.wtf/Tools/autosqeal)
[![Quality Gate Status](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=alert_status&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Reliability Rating](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=reliability_rating&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Security Rating](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=security_rating&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Maintainability Rating](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=sqale_rating&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
[![Lines of Code](https://sonar.beatrice.wtf/api/project_badges/measure?project=autosqueal&metric=ncloc&token=sqb_49dde556c032d0130640ea1e48875905b158d368)](https://sonar.beatrice.wtf/dashboard?id=autosqueal)
  
  
*little java tool to automatically perform mouse actions*  
  
## supported systems  
| system    | support    |
|-----------|------------|
| macOS     | ✅ complete |
| GNU/Linux | ⏳ planned  |
| Windows   | ⏳ planned  |
  
## building  
**required tools**  
 - java 21 sdk  
 - git  
 - maven  
  
**build steps**  
 1. clone the official repository linked below using `git clone`.  
 2. `cd` into the directory and run `mvn clean package`.  
 3. you will find a runnable jar with dependencies in the `target/` folder.  
 4. run the built jar file with `java -jar target/autosqueal-*-dependencies.jar`.
  
## support  
| category            | info                                                          |
|---------------------|---------------------------------------------------------------|
| official repository | [gitea src](https://git.beatrice.wtf/Tools/autosqeal.git)     |
| mirror repository   | [github src](https://github.com/mind-overflow/autosqueal.git) |
| build status        | [drone-ci](https://drone.beatrice.wtf/Tools/autosqeal)        |
| dev email           | [hello@beatrice.wtf](mailto:hello@beatrice.wtf)               |

