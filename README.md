# gdc-microservices-memory-optimization
Source code for Juraj Samuel Salon's diploma thesis

This repository consist of directories containing the sourcecode used for the purpose of memory optimization in our
current microservices. The directories contain the projects: 
 - _gdc-api-support_ - dependencies for projects build, contains simple wrappers over the exceptions and logging to unify usage across the projects
 - _gdc-panther_ - original project containing _organization-api-service_, and it's dependencies, which is simple microservice serving as a wrapper over Kubernetes resources
 - _gdc-panther-micronaut_ - project containing _organization-api-service_ reimplemented into Micronaut framework
 - _gdc-panther-spring-native_ - project containing _organization-api-service_ upgraded to Spring Boot 3 and Spring Framework 6 to support ahead-of-time combination into native binary 

## License
This code is released under version 3.0 of the [BSD-3 License](https://opensource.org/license/bsd-3-clause/).