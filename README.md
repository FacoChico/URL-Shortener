# URL Shortener

### Описание

**URL Shortener** — Spring Boot-приложение для сокращения ссылок с возможностью управления лимитом переходов и времени жизни ссылки.

#### Требования

* Java 21
* Gradle
* Spring Boot 3.5.7

#### Структура приложения

```
main
├── java
│   └── com
│       └── mephi
│           └── skillfactory
│               └── urlshortener
│                   ├── UrlShortenerApplication.java
│                   ├── cli
│                   │   └── CliRunner.java
│                   ├── config
│                   │   └── PropertiesConfig.java
│                   ├── controller
│                   │   ├── GlobalExceptionResolver.java
│                   │   ├── ShortLinkController.java
│                   │   └── dto
│                   │       ├── ShortenRequest.java
│                   │       └── ShortenResponse.java
│                   ├── domain
│                   │   └── Link.java
│                   ├── properties
│                   │   ├── AppProperties.java
│                   │   └── LinkProperties.java
│                   ├── repository
│                   │   ├── InMemoryLinkRepository.java
│                   │   └── LinkRepository.java
│                   └── service
│                       ├── CodeGenerator.java
│                       ├── NotificationService.java
│                       ├── ShortLinkService.java
│                       └── exception
│                           └── UniqueCodeException.java
└── resources
    └── application.yml
```

### Сборка

```bash
./gradlew clean bootJar
```

### Запуск приложения

Для работы перехода по ссылке через консоль запуск приложения должен осуществляться с параметром `-Djava.awt.headless=false`:

```bash
java -Djava.awt.headless=false -jar build/libs/url-shortener-0.0.1.jar
```

### Запуск тестов

```bash
./gradlew test
```

### Примеры запросов

1. Создать короткую ссылку:
    ```bash
    curl --location 'http://localhost:8080/api/shorten' \
    --header 'Content-Type: application/json' \
    --header 'X-User-Id: <uid>' \
    --data '{
        "url": <long link>,
        "maxClicks": 5,
        "ttlSeconds": 300
    }'
    ```

   Если в запросе не указан заголовок `X-User-Id`, нужно сохранить вернувшийся в ответе userId и в дальнейшем указывать его
   значение заголовке для идентификации.


2. Получить уведомления пользователя:
    ```bash
    curl --location 'http://localhost:8080/api/notifications' \
    --header 'X-User-Id: <uid>'
    ```

3. Удалить ссылку по ее коду:
    ```bash
    curl --location --request DELETE 'http://localhost:8080/api/links/INw43M3' \
    --header 'X-User-Id: <uid>'
   ```

4. Получить список ссылок пользователя:
    ```bash
   curl --location 'http://localhost:8080/api/links' \
   --header 'X-User-Id: <uid>'
   ```

Сокращенная ссылка имеет вид `base-url/code`, где

- `base-url` — задается в конфиге;
- `code` — генерируется в ответ на запрос сокращения ссылки

### Основные команды CLI

- `help` — показать список команд;
- `create <url>` — создать короткую ссылку;
- `open <code>` — открыть ссылку по коду в браузере;
- `list` — вывести список ссылок текущего пользователя;
- `delete <code>` — удалить ссылку по коду;
- `notifications` — вывести список уведомлений текущего пользователя;
- `uid` — вывод текущего user id;
- `setuid <id|new>` — установить текущий user id или сгенерировать новый.

### Notes

- Лимиты и TTL короткой ссылки считаются частью её неизменяемого контракта. Изменение для уже опубликованной ссылки нарушает предсказуемость
  поведения, усложняет логику и аудит. Если лимиты редактируемы, с точки зрения пользователя невозможно однозначно определить, почему
  ссылка всё ещё активна и почему лимит не сработал вовремя. Изменение лимитов реализуется через удаление старой ссылки и создание новой;
- При открытии ссылки в консоли через `open <code>` происходит инкремент количества кликов по ссылке;
- **Недопустимо** создавать различные короткие ссылки для одной и той же исходной ссылки с теми же параметрами кликов и TTL, т.к. это ломает
  детерминизм, управляемость и доверие к системе, не дает технической пользы.
