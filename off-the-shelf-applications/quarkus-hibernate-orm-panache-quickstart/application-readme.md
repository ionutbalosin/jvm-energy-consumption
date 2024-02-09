## Quarkus Hibernate ORM Panache Quickstart

GitHub Repository: [hibernate-orm-panache-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart)

These application configurations are needed before starting the wrk tests.

### Step I

Append/override these properties to the [application.properties](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/src/main/resources/application.properties), otherwise the application cannot cope with an increased/concurrent load.

```properties
# Increase the number of JDBC connections
%prod.quarkus.datasource.jdbc.max-size=4096
%prod.quarkus.datasource.jdbc.min-size=4096
%prod.quarkus.datasource.jdbc.max-lifetime=40M
%prod.quarkus.datasource.jdbc.idle-removal-interval=40M

# Disable ORM logging
quarkus.hibernate-orm.log.sql=false
```
 
### Step II

Remove the `unique = true` constraint on the
[Fruit.java](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/src/main/java/org/acme/hibernate/orm/panache/repository/Fruit.java)
and
[FruitEntity.java](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/src/main/java/org/acme/hibernate/orm/panache/entity/FruitEntity.java)
class entities to prevent duplicate entries during high concurrency testing, which could lead to unnecessary database exceptions.

```
// Fruit.class

@Column(length = 40, unique = true)
public String name;
```

```
// FruitEntity.class

@Column(length = 40, unique = true)
public String name;
```
