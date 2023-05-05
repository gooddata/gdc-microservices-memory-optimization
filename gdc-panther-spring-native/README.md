# gdc-panther
Project used to compare the memory consumption of microservices in GoodData
Original Microservice implemented in with Spring Framework, used as a wrapper over the Kubernetes resources [organization-api-service](microservices/organization-api-service)

## Build
```bash
./gradlew clean build
```

### Requirements
OpenJDK Java 17 SDK or GraalVM Java 17 SDK

## Build native
```bash
./gradlew clean nativeCompile
```

### Requirements
GraalVM Java 17 SDK
