## Quarkus Hibernate ORM Panache Quickstart

GitHub Repository: [hibernate-orm-panache-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart)

These application configurations are needed before starting the wrk tests.

### Step I

Append/override these properties to the [application.properties](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/src/main/resources/application.properties), otherwise the application cannot cope with an increased/concurrent load.

```properties
# Enable H2 data source
%prod.quarkus.datasource.db-kind=h2
%prod.quarkus.datasource.jdbc.url=jdbc:h2:mem:default;DB_CLOSE_DELAY=-1

# Increase the number of JDBC connections
%prod.quarkus.datasource.jdbc.max-size=4096
%prod.quarkus.datasource.jdbc.min-size=4096
%prod.quarkus.datasource.jdbc.max-lifetime=40M
%prod.quarkus.datasource.jdbc.idle-removal-interval=40M

# Disable ORM logging
quarkus.hibernate-orm.log.sql=false

quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.sql-load-script=import.sql
```

References: [Using Hibernate and ORM](https://quarkus.io/guides/hibernate-orm)

### Step II

Replace the `quarkus-jdbc-postgresql` with `quarkus-jdbc-h2` data source in the [pom.xml](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/pom.xml).

Having a dedicated PostgreSQL database involves running it on another machine and makes the overall architecture more complex. An in-memory database is just fine for our goal.


```
    <!-- Remove the PostgreSQL dependency -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-jdbc-postgresql</artifactId>
    </dependency>

    <!-- Add the H2 dependency -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-jdbc-h2</artifactId>
    </dependency>
``` 
 
### Step III

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
