# Current Architecture Documentation (Legacy)

This document captures the **as-is** architecture of IMDBSelector prior to modernization.

---

## 1. Component & Dependency Overview

Notice the **inverted dependencies**: `Domain` directly imports and instantiates both `Presentation` (`MainView`) and `DataLayer` (`UserGateway`, `WatchListGateway`, `UserFinder`, `WatchListFinder`).

```mermaid
flowchart TB
    Entry["main.java (Entry Point)"] --> ModelImpl["Domain.ModelImplementation"]

    subgraph Presentation["Presentation (Swing + FlatLaf)"]
        MainView["MainView (JFrame)"]
        MainCtrl["MainViewController"]
        FiltersView["FiltersView"]
        FiltersCtrl["FiltersViewController"]
        WatchlistView["WatchlistView"]
        WatchlistCtrl["WatchlistViewController"]
    end

    subgraph Domain["Domain (Business Logic + State)"]
        Model["Model (interface)"]
        ModelImpl
        Movie["Movie"]
        User["User"]
        Watchlist["Watchlist"]
        Filter["Filter / FiltersDTO"]
        Selector["RandomMovieSelector + SelectorMode"]
        CSVParser["CSVParser (OpenCSV)"]
        URLOpener["URLOpener (java.awt.Desktop)"]
    end

    subgraph DataLayer["DataLayer (JSON File Persistence)"]
        UserFinder["UserFinder (static)"]
        UserGateway["UserGateway (Gson)"]
        WatchListFinder["WatchListFinder (static)"]
        WatchListGateway["WatchListGateway (Gson)"]
    end

    Storage[("Local Filesystem\ndata/user/*.json\ndata/watchlist/*.json")]

    ModelImpl -.->|implements| Model
    ModelImpl -->|instantiates directly| MainView
    ModelImpl -->|observes| MainView
    MainView --> MainCtrl
    MainCtrl --> Model
    MainCtrl --> FiltersView
    MainCtrl --> WatchlistView
    FiltersView --> FiltersCtrl
    WatchlistView --> WatchlistCtrl

    ModelImpl -->|instantiates directly| UserGateway
    ModelImpl -->|instantiates directly| WatchListGateway
    ModelImpl -->|static calls| UserFinder
    ModelImpl -->|static calls| WatchListFinder

    User -->|accepts in constructor| UserGateway
    Watchlist -->|accepts in constructor| WatchListGateway
    Watchlist --> CSVParser
    Movie --> URLOpener

    UserFinder --> Storage
    UserGateway --> Storage
    WatchListFinder --> Storage
    WatchListGateway --> Storage
```

---

## 2. UML Class Diagram (Domain & DataLayer)

```mermaid
classDiagram
    class Model {
        <<interface>>
        +getUser() User
        +getWatchlist() Watchlist
        +loadUser(name)
        +loadWatchList(name)
        +newUser(name)
        +newWatchList(name, movies)
        +actualizeWatchList(movies)
        +deleteUser()
        +deleteWatchList()
        +setFilters(filters)
        +getFilters() FiltersDTO
        +filterWatchlist()
        +selectRandomMovie()
        +setModeRandom()
        +setModeNotSeen()
        +addMovieToSeen()
        +addMovieToSeen(movie)
        +deleteMovieFromSeen(movie)
    }

    class ModelImplementation {
        -User user
        -Watchlist watchlist
        -Watchlist filteredWatchlist
        -Movie currentMovie
        -Filter filter
        -RandomMovieSelector selector
        -MainView mainView
        -List~Observer~ observers
        -List~ObserverComboBoxes~ observerComboBoxes
        +ModelImplementation()
    }

    class User {
        -String name
        -List~Movie~ seenMovies
        -Movie lastMovieGenerated
        +User(UserGateway)
        +addMovie(Movie)
        +deleteMovie(Movie)
        +hasSeen(Movie) boolean
        +LastMovieGeneratedAlreadySeen() boolean
    }

    class Watchlist {
        -String name
        -List~Movie~ watchlist
        +Watchlist(name, csvData)
        +Watchlist(WatchListGateway)
        +getMovieByTitle(name) Movie
        +getMovieByTag(tag) Movie
        +getTags() List~String~
    }

    class Movie {
        -String title
        -String IMDBTag
        -String URL
        -String director
        -String year
        -String rating
        -String type
        -List~String~ genres
        +openIMDB()
        +getInformation() List~String~
        +equals(Object) boolean
    }

    class Filter {
        -Double minimumRatingValue
        -Double maximumRatingValue
        -int minimumYearValue
        -int maximumYearValue
        -boolean[] checkedTypes
        -boolean[] checkedGenres
        -List~String~ keywords
        +filterWatchlist(Watchlist) Watchlist
        +getFilters() FiltersDTO
        +setFilters(FiltersDTO)
    }

    class FiltersDTO {
        -boolean[] checkedTypes
        -boolean[] checkedGenres
        -int minimumYearValue
        -int maximumYearValue
        -Double minimumRatingValue
        -Double maximumRatingValue
        -ArrayList~String~ keywords
    }

    class RandomMovieSelector {
        -Model model
        -SelectorMode mode
        +execute() Movie
        +setModeRandom()
        +setModeNotSeen()
    }

    class SelectorMode {
        <<interface>>
        +selectRandomMovie() String
        +netejaMode(watchListTags)
    }

    class SelectorModeAbstract {
        <<abstract>>
        #RandomMovieSelector randomMovieSelector
        +selectRandomMovie() String
    }

    class RandomSelectorMode {
        +netejaMode(watchListTags)
    }

    class NotSeenSelectorMode {
        +netejaMode(watchListTags)
    }

    class UserGateway {
        -String name
        -List~Movie~ seenMovies
        -Movie lastMovieGenerated
        +save()
        +delete()
    }

    class UserFinder {
        +findUser(name)$ UserGateway
        +getAllUsersName()$ List~String~
    }

    class WatchListGateway {
        -String name
        -List~Movie~ watchList
        +save()
        +delete()
    }

    class WatchListFinder {
        +findWatchList(name)$ WatchListGateway
        +getAllWatchListsName()$ List~String~
    }

    Model <|.. ModelImplementation
    ModelImplementation --> User
    ModelImplementation --> Watchlist
    ModelImplementation --> Movie
    ModelImplementation --> Filter
    ModelImplementation --> RandomMovieSelector
    User "1" o-- "*" Movie : seenMovies
    Watchlist "1" o-- "*" Movie : watchlist
    Filter ..> FiltersDTO : creates/reads
    Filter ..> Watchlist : filters
    RandomMovieSelector --> SelectorMode
    SelectorMode <|.. SelectorModeAbstract
    SelectorModeAbstract <|-- RandomSelectorMode
    SelectorModeAbstract <|-- NotSeenSelectorMode
    User ..> UserGateway : constructed from
    Watchlist ..> WatchListGateway : constructed from
    UserFinder ..> UserGateway : returns
    WatchListFinder ..> WatchListGateway : returns
```

---

## 3. Key Sequence Flows

### Startup & User Selection
```mermaid
sequenceDiagram
    actor UserActor as User
    participant Main as main
    participant Model as ModelImplementation
    participant Finder as UserFinder / WatchListFinder
    participant View as MainView
    participant Ctrl as MainViewController

    Main->>Model: new ModelImplementation()
    Model->>Finder: getAllUsersName() / getAllWatchListsName()
    Model->>View: new MainView(this)
    Model->>View: initializeController()
    View->>Ctrl: new MainViewController(model, view)
    Model->>View: notifyObservers()

    UserActor->>View: Select user in comboBox
    View->>Ctrl: actionPerformed(event)
    Ctrl->>Model: loadUser(name)
    Model->>Finder: UserFinder.findUser(name)
    Finder-->>Model: UserGateway
    Model->>Model: new User(gateway)
    alt lastMovieGenerated exists and not seen
        Model->>View: throwLastMovieGeneratedForUser(title, year)
    end
    Model->>View: notifyObservers() / notifyObserverComboBoxes()
```

### Random Movie Selection
```mermaid
sequenceDiagram
    actor UserActor as User
    participant Ctrl as MainViewController
    participant Model as ModelImplementation
    participant Sel as RandomMovieSelector
    participant Mode as SelectorModeAbstract
    participant Movie as Movie
    participant GW as UserGateway
    participant View as MainView

    UserActor->>Ctrl: Click Generate
    Ctrl->>Model: selectRandomMovie()
    Model->>Sel: execute()
    Sel->>Mode: selectRandomMovie()
    Mode->>Mode: netejaMode(watchListTags)
    Note right of Mode: NotSeenSelectorMode removes seenMovies
    Mode-->>Sel: random IMDBTag
    Sel->>Movie: openIMDB()
    Note right of Movie: Calls URLOpener -> Desktop.browse(URL)
    Sel-->>Model: Movie
    Model->>Model: user.setLastMovie(currentMovie)
    Model->>GW: new UserGateway(user).save()
    Model->>View: notifyObservers()
```
