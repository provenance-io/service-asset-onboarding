# service-asset-onboarding

Asset onboarding to Provenance blockchain

```
Service
                        _                          _                              _  _               
                       | |                        | |                            | |(_)              
  __ _  ___  ___   ___ | |_  ______   ___   _ __  | |__    ___    __ _  _ __   __| | _  _ __    __ _ 
 / _` |/ __|/ __| / _ \| __||______| / _ \ | &#39;_ \ | &#39;_ \  / _ \  / _` || &#39;__| / _` || || &#39;_ \  / _` |
| (_| |\__ \\__ \|  __/| |_         | (_) || | | || |_) || (_) || (_| || |   | (_| || || | | || (_| |
 \__,_||___/|___/ \___| \__|         \___/ |_| |_||_.__/  \___/  \__,_||_|    \__,_||_||_| |_| \__, |
                                                                                                __/ |
                                                                                               |___/ 

Figure Technologies
```

## Toolchain

These tools are known to work. You can use others if you wish, but YMMV.

- Azul JDK 11
- Kotlin 1.5.0
- IntelliJ 2021.x or newer

## Run the service locally

```shell
$ ./gradlew build service:bootrun
```

## Architecture

This service tries to adhere to [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html). This architecture is designed to
encourage loosely coupled code. Please read the linked article and ensure you have a high level understanding of the concept before making commits.

The most important thing to remember in order to keep your code Clean: **_don't violate the dependency rule._**

### Package structure

Top level package is `tech.figure.asset`. Assume everything below is prefixed with this.

### `domain`

The business logic and data model.

- `client`: Interfaces to other Figure APIs.
- `data`: Data access layer, using the repository pattern. Data models are generally pulled from stream-data, but
  app specific models (POJOs) can go here as well.
- `usecase`: The use-cases of the system. This package is where the meat of the business logic should go. It also defines the inputs
  and outputs of the system.

### `frameworks`

This is where framework specific implementation code goes.

- `client.retrofit`: [Retrofit](https://square.github.io/retrofit/) implementations for `domain.client` interfaces.
- `web`: Rest API configuration & routing, using [WebFlux Functional Endpoints](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-fn)
  - Child package structure should mirror the route structure.
    - APIs use two components:
      - Router: define routes and their HTTP methods
      - Handler: consume requests and invoke the appropriate usecase.

## Development Standards

We stick to a few simple rules to keep this codebase in good shape.

1. Formatting and linting is handled by [ktlint](https://github.com/pinterest/ktlint)
   Jenkins builds will fail if your code does not pass these checks.
    1. To lint your code: `./gradlew ktlintFormat detekt`
2. Test your code.
    1. `domain` code should have almost always have unit tests.
    2. `frameworks` code should include some amount of integration tests, unless implementing them is difficult/impossible. Use your best judgement here.
3. Document your code.
    1. Try to imagine what a new developer with no context will need to know about your code. If it's not obvious, document it.
    2. Include links to reference materials you use.
    3. Use [KDoc](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) syntax.
