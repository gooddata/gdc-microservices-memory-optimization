# gdc-panther-micronaut
Project used to compare the memory consumption of microservices in GoodData
Microservice implemented in with Micronaut Framework, used as a wrapper over the Kubernetes resources [organization-api-service](microservices/organization-api-service)

## Build 
```bash
./gradlew clean build
```

### Requirements
OpenJDK Java 11 SDK or GraalVM Java 11 SDK

## Build native
```bash
./gradlew clean build
cd ./microservices/organization-api-service
native-image -cp ./build/libs/organization-api-service-0.1-all.jar \
    --no-fallback \  
    --verbose \
    -H:Name=./build/libs/organization-api-service \
    -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.configure=ALL-UNNAMED \
    -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED \
    -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED \
    -J--add-exports=org.graalvm.sdk/org.graalvm.nativeimage.impl=ALL-UNNAMED \
    -H:ConfigurationFileDirectories=./build/native/generated/generateResourcesConfigFile/ \
    -H:Class=com.gooddata.panther.organizationapi.OrganizationApiApplicationKt \
    --initialize-at-build-time=kotlin
```

### Requirements
GraalVM Java 11 SDK
