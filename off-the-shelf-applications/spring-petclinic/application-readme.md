## Spring PetClinic

GitHub Repository: [spring-petclinic](https://github.com/spring-projects/spring-petclinic)

These application configurations are needed before starting the wrk tests.

### Step I

Append/override these properties to the [application.properties](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/application.properties), otherwise the application cannot cope with an increased/concurrent load.

```properties
# Increase the number of Hikari pool connections
spring.datasource.hikari.maximum-pool-size=4096
spring.datasource.hikari.minimum-idle=4096
spring.datasource.hikari.connection-timeout=600000
spring.datasource.hikari.idle-timeout=2400000
spring.datasource.hikari.max-lifetime=4800000

# Tomcat properties
server.tomcat.threads.max=4096
```
