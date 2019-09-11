---
layout: docs
title: Polymorphic Programs
permalink: /docs/patterns/polymorphic_programs/ru/
---

## Как писать полиморфические программы

{:.advanced}
advanced

[English](/docs/patterns/polymorphic_programs/)

Что если мы могли бы писать приложения не задумываясь о типах данных, которые будут использованы в рантайме, а просто описывать то, **как эти данные будут обработаны**?

Представим, что у нас есть приложение, которое работает с типом `Observable` из библиотеки RxJava. Этот тип позволяет нам написать цепочки вызовов и манипуляций с данными, но в итоге не будет ли этот `Observable` просто контейнером с дополнительными свойствами?

Та же история с типами вроде `Flowable`, `Deferred` (корутины), `Future`, `IO`, и множеством других.

Концептуально все эти типы представляют собой операцию (уже сделанную или планируемую к выполнению в будущем), которая поддерживает манипуляции вроде приведения внутреннего значения к другому типу (`map`), использование `flatMap` для создания цепочки операций схожего типа, объединение с другими инстансами этого же типа (`zip`), и т.п.

Для того, чтобы писать программы, основываясь на этих поведениях, при этом сохраняя декларативность описания, а также чтобы сделать свои программы независимыми от конкретных типов данных вроде `Observable` достаточно того, чтобы используемые типы данных соответствовали определенным контрактам, таким как `map`, `flatMap`, и прочие.

Такой подход может показаться странным или чересчур усложненным, но у него есть интересные преимущества. Давай сначала рассмотрим простой пример, а потом поговорим о них.

### Каноническая проблема

Примеры кода взяты из статьи моего друга [Raúl Raja](https://twitter.com/raulraja), который помог с редактурой этого поста.

Представим, что у нас есть приложение со списком дел, и мы хотели бы извлечь из локального кэша список объектов типа `Task`. Если они не будут найдены в локальном хранилище, мы попробуем запросить их по сети. Нам нужен единый контракт для обоих источников данных, чтобы они оба могли получить список объектов типа `Task` для подходящего объекта `User`, вне зависимости от источника:

```kotlin
interface DataSource {
  fun allTasksByUser(user: User): Observable<List<Task>>
}
```

Здесь для простоты мы возвращаем `Observable`, но это может быть `Single`, `Maybe`, `Flowable`, `Deferred` - что угодно, подходящее для достижения цели.

Добавим пару моковых имплементаций источников данных, одну для **локального**, вторую для **дистанционного**.

```Kotlin
class LocalDataSource : DataSource {
  private val localCache: Map<User, List<Task>> =
    mapOf(User(UserId("user1")) to listOf(Task("LocalTask assigned to user1")))

  override fun allTasksByUser(user: User): Observable<List<Task>> = 
    Observable.create { emitter ->
      val cachedUser = localCache[user]
      if (cachedUser != null) {
        emitter.onNext(cachedUser)
      } else {
        emitter.onError(UserNotInLocalStorage(user))
      }
    }
}

class RemoteDataSource : DataSource {
  private val internetStorage: Map<User, List<Task>> =
    mapOf(User(UserId("user2")) to listOf(Task("Remote Task assigned to user2")))

  override fun allTasksByUser(user: User): Observable<List<Task>> = 
    Observable.create { emitter ->
      val networkUser = internetStorage[user]
      if (networkUser != null) {
        emitter.onNext(networkUser)
      } else {
        emitter.onError(UserNotInRemoteStorage(user))
      }
    }
}
```

Имплементации обоих источников данных практически идентичны. Это просто мокированные версии этих источников, которые в идеальном случае достают данные из локального хранилища или сетевого API. В обоих случаях для хранения данных используется сохраненный в память `Map<User, List<Task>>`.

Т.к. у нас два источника данных, нам надо как-то их координировать. Давай создадим `репозиторий`:

```kotlin
class TaskRepository(private val localDS: DataSource, 
                     private val remoteDS: RemoteDataSource) {

  fun allTasksByUser(user: User): Observable<List<Task>> =
    localDS.allTasksByUser(user)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.computation())
      .onErrorResumeNext { _: Throwable -> remoteDS.allTasksByUser(user) }
}
```

Он просто пытается загрузить `List<Task>` из `LocalDataSource`, и если тот не найден — пробует запросить их из Сети с помощью `RemoteDataSource`.

Давай создадим простой модуль для предоставления зависимостей при этом не пользуясь никакими фреймворками для иньекции зависимостей (DI):

```kotlin
class Module {
  private val localDataSource: LocalDataSource = LocalDataSource()
  private val remoteDataSource: RemoteDataSource = RemoteDataSource()
  val repository: TaskRepository = TaskRepository(localDataSource, remoteDataSource)
}
```

И наконец, нам нужен простой тест, прогоняющий весь стек операций:

```kotlin
object test {

  @JvmStatic
  fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val dependenciesModule = Module()
    dependenciesModule.run {
      repository.allTasksByUser(user1).subscribe({ println(it) }, { println(it) })
      repository.allTasksByUser(user2).subscribe({ println(it) }, { println(it) })
      repository.allTasksByUser(user3).subscribe({ println(it) }, { println(it) })
    }
  }
}
```

[Весь вышеприведенный код можно найти на гитхабе](https://gist.github.com/JorgeCastilloPrz/05793f11497e0e31f207d2a3e6522bdb).

Эта программа композирует цепочку выполнения для трех пользователей, затем подписывается на полученный в результате [`Observable`]({{ '/docs/integrations/rx2' | relative_url }}).

Первые два объекта типа `User` доступны, с этим нам повезло. `User1` доступен в местном DataSource, и `User2` доступен на дистанционном.

Но есть проблема с `User3`, т.к., он недоступен в локальном хранилище. Программа попытается загрузить его из дистанционного сервиса - но там его тоже нет. Поиск закончится неудачей и мы выведем в консоль сообщение о ошибке.

Вот что будет выведено в консоль для всех трех случаев:

```
> [Task(value=LocalTask assigned to user1)]
> [Task(value=Remote Task assigned to user2)]
> UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user)))
```

Мы закончили с примером. Давай теперь попробуем запрограммировать эту логику в стиле **функционального полиморфизма**

### Абстрагирование типов данных

Теперь контракт для интерфейса `DataSource` будет выглядеть так:

```kotlin
interface DataSource<F> {
  fun allTasksByUser(user: User): Kind<F, List<Task>>
}
```

Всё вроде бы похоже, но есть два важных отличия:
 
 * Появилось зависимость на обобщенный тип (generic) `F`.
 * Тип, возвращаемый функцией теперь `Kind<F, List<Task>>`.


`Kind` это [то, как Arrow кодирует то, что обычно называеют **высоким типом (higher kind)**]({{ '/docs/patterns/glossary/#type-constructors' | relative_url }}).
Поясню этот концепт на простом примере.

У `Observable<A>` есть 2 части:

* `Observable`: контейнер, фиксированный тип.
* `A`: аргумент обобщенного типа. Абстракция, в которую можно передать другие типы.

Мы привыкли воспринимать обобщенные типы вроде `A`, как абстракции. Но не многие знают, что мы можем также абстрагировать типы контейнеров вроде `Observable`. Для этого и существуют **высокие типы**.

Идея в том, что у нас может быть конструктор вроде `F<A>` в котором и `F` и `A` могут быть параметризированным типом. Этот синтаксис еще не поддерживается компилятором Kotlin ([всё ещё?](https://github.com/Kotlin/KEEP/pull/87)), поэтому мы мимикрируем его подобным подходом.

Arrow поддерживает подобное через [использование промежуточного мета интерфейса `Kind<F, A>`]({{ '/docs/patterns/glossary/#type-constructors' | relative_url }}), который держит в себе ссылки на оба типа, а также во время компиляции генерирует конвертеры в обоих направлениям таким образом, чтобы можно было проделать путь от `Kind<Observable, List<Task>>` до `Observable<List<Task>>` и наоборот. Не идеальная решение, зато рабочее.

Поэтому снова посмотрим на интерфес нашего репозитория:

```kotlin
interface DataSource<F> {
  fun allTasksByUser(user: User): Kind<F, List<Task>>
}
```

Функция `DataSource` **возвращает высокий тип**: `Kind<F, List<Task>>`. Он транслируется в `F<List<Task>>`, где `F` остается обобщенным.

Мы фиксируем в сигнатуре только`List<Task>`. Другими словами, нам всё равно, какой будет использован контейнер типа `F`, до тех пор, пока он содержит в себе `List<Task>`. Мы можем **передавать в функцию разные контейнеры данных**. Уже понятней? Идем дальше. 

Давай взглянем на имплементированные таким образом `DataSource`, но на этот раз на каждый по отдельности. Сначала на локальный: 

```kotlin
class LocalDataSource<F>(A: ApplicativeError<F, Throwable>) : DataSource<F>, ApplicativeError<F, Throwable> by A {
      
    private val localCache: Map<User, List<Task>> =
      mapOf(User(UserId("user1")) to listOf(Task("LocalTask assigned to user1")))

    override fun allTasksByUser(user: User): Kind<F, List<Task>> =
      Option.fromNullable(localCache[user]).fold(
        { raiseError(UserNotInLocalStorage(user)) },
        { just(it) }
      )
}
```

Добавилось много нового, разберем все шаг за шагом.

Этот `DataSource` сохраняет обобщенный тип `F` т.к., имплементирует `DataSource<F>`. Мы хотим сохранить возможность передачи этого типа извне.

Теперь, забудь о возможно незнакомой [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}) в конструкторе и сфокусируйся на функции `allTasksByUser()`. А к `ApplicativeError` мы еще вернемся.

```kotlin
override fun allTasksByUser(user: User): Kind<F, List<Task>> =
    Option.fromNullable(localCache[user]).fold(
      { raiseError(UserNotInLocalStorage(user)) },
      { just(it) }
    )
```

Как видишь она возвращает `Kind<F, List<Task>>`. Нам по прежнему все равно что из себя представляет контейнер `F` до тех пор, пока он содержит `List<Task>`.

Но есть проблема. В зависимости от того, можем ли мы найти список объектов `Task` для нужного пользователя в локальном хранилище или нет, мы хотим **сообщить о ошибке** (`Task` не найдены) или **вернуть `Task` уже обернутыми в `F` (`Task` найдены).

И для обоих случаев нам надо вернуть: `Kind<F, List<Task>>`.

Другими словами: есть тип **о котором мы ничего не знаем (`F`)**, и нам нужен способ возвращения ошибки, завернутой в этот тип. Плюс, нам нужен способ создания инстанса этого типа, в который будет завернуто значение, полученное после успешного заверщения функции. Звучит как что-то невозможное?

Давай вернемся к декларации класса и обратим внимание что [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}) передается в конструктор и потом используется как делегат для класса (`by A`).

```kotlin
class LocalDataSource<F>(A: ApplicativeError<F, Throwable>) : DataSource<F>, ApplicativeError<F, Throwable> by A {
    //...
}
```

[`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}) наследуется от [`Applicative`]({{ '/docs/arrow/typeclasses/applicative' | relative_url }}), они оба - [**классы типа**]({{ '/docs/typeclasses/intro' | relative_url }}).

**Классы типа определяют поведения (контракты)**. Они закодированы как интерфейсы, которые работают с аргументами в виде обобщенных типов, как в [`Monad<F>`]({{ '/docs/arrow/typeclasses/monad' | relative_url }}), [`Functor<F>`]({{ '/docs/arrow/typeclasses/functor' | relative_url }}) и многих других. Этот `F` является типом данных. Таким образом мы можем передать типы вроде [`Either`]({{ '/docs/arrow/core/either/ru' |  relative_url }}), [`Option`]({{ '/docs/arrow/core/option/ru' | relative_url }}), [`IO`]({{ '/docs/effects/io' | relative_url }}), [`Observable`]({{ '/docs/integrations/rx2' | relative_url }}), [`Flowable`]({{ '/docs/integrations/rx2' | relative_url }}) и множество других.

Итак, вернемся к двум нашим проблемам:

* **Обернуть значение, полученное после успешного завершения функции в `Kind<F, List<Task>>`**

Для этого мы можем использовать класс типа [`Applicative`]({{ '/docs/arrow/typeclasses/applicative' | relative_url }}). Т.к., [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}) наследутеся от него, мы можем делегировать его свойства.

[`Applicative`]({{ '/docs/arrow/typeclasses/applicative' | relative_url }}) просто предоставляет функцию `just(a)`. `just(a)` **оборачивает значение в контекст любого высокого типа**. Таким образом, если у нас есть `Applicative<F>`, он может вызвать `just(a)`, чтобы обернуть значение в контейнер `F`, каким бы это значение не было. Допустим, мы используем `Observable`, у нас будет `Applicative<Observable>`, который знает, как обернуть `a` в `Observable`, чтобы в итоге получить `Observable.just(a)`.

* **Обернуть ошибку в инстанс `Kind<F, List<Task>>`**

Для этого мы можем использовать [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}). Он предоставляет функцию `raiseError(e)`, которая оборачивает ошибку в контейнер типа `F`. Для примера с `Observable`, появление ошибки создаст что-то вроде `Observable.error<A>(t)`, где `t` это `Throwable`, раз мы задекларировали наш тип ошибки в виде класса типа `ApplicativeError<F, Throwable>`.

Посмотрим на нашу абстрактную имплементацию `LocalDataSource<F>`.

```kotlin
class LocalDataSource<F>(A: ApplicativeError<F, Throwable>) : 
    DataSource<F>, ApplicativeError<F, Throwable> by A {

    private val localCache: Map<User, List<Task>> =
      mapOf(User(UserId("user1")) to listOf(Task("LocalTask assigned to user1")))

    override fun allTasksByUser(user: User): Kind<F, List<Task>> =
      Option.fromNullable(localCache[user]).fold(
        { raiseError(UserNotInLocalStorage(user)) },
        { just(it) }
      )
}
```

Сохраненная в память `Map<User, List<Task>>` осталась той же, но теперь функция делает пару вещей, которые могут быть для тебя новыми:

* Она пробует загрузить список `Task` из локального кэша и т.к., возвращаемое значение может быть `null` (`Task` могут быть не найдены), мы моделируем это через использование [`Option`]({{ '/docs/arrow/core/option/ru' | relative_url }}). Если непонятно, как работает [`Option`]({{ '/docs/arrow/core/option/ru' | relative_url }}), то он моделирует присутствие или отсутствие значения, которое в него завернуто.

* После получения опционального значения, мы вызываем поверх него `fold`. Это эквивалент использования условного выражения `when` над опциональным значением. Если значение **отсутствует**, то `Option` оборвачивает ошибку в тип данных `F` (первая переданная лямбда). А если значение **присутствует** `Option` создает инстанс обертки для типа данных `F` (вторая лямбда). В обоих случаях используются свойства [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}) упомянутые до этого: `raiseError()` и `just()`.

Таким образом мы абстрагировали имплементации источников данных с помощью классов так, что они не знают какой контейнер будет использован для используемого типа `F`.

Имплеметация сетевого `DataSource` выглядит схожим образом:

```kotlin
class RemoteDataSource<F>(A: Async<F>) : DataSource<F>, Async<F> by A {
  private val internetStorage: Map<User, List<Task>> =
    mapOf(User(UserId("user2")) to listOf(Task("Remote Task assigned to user2")))

  override fun allTasksByUser(user: User): Kind<F, List<Task>> =
    async { callback: (Either<Throwable, List<Task>>) -> Unit ->
      Option.fromNullable(internetStorage[user]).fold(
        { callback(UserNotInRemoteStorage(user).left()) },
        { callback(it.right()) }
      )
    }
}
```

Но есть одно небольшое различие: вместо делегирования в инстанс [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror' | relative_url }}) мы используем другой класс типа: [`Async`]({{ '/docs/effects/async/' | relative_url }}).

Это делается из-за того, что по своей природе сетевые вызовы асинхронны. Мы хотим написать код, который будет исполняться асинхронно, логично использовать класс типа, предназначенный для этого.

[`Async`]({{ '/docs/effects/async/' | relative_url }}) используется для моделирования асинхронных операций. Он может моделировать любую операцию основанную на колбеках. Заметим, что нам все еще неизвестны конкретные типы данных, мы просто описываем **асинхронную по природе** операцию.

Рассмотрим следующую функцию:

```kotlin
override fun allTasksByUser(user: User): Kind<F, List<Task>> =
    async { callback: (Either<Throwable, List<Task>>) -> Unit ->
      Option.fromNullable(internetStorage[user]).fold(
        { callback(UserNotInRemoteStorage(user).left()) },
        { callback(it.right()) }
      )
    }
```

Мы можем использовать функцию `async {}`, которую нам предоставляет класс типа [`Async`]({{ '/docs/effects/async/' | relative_url }}) для моделирования операции и создать инстанс типа `Kind<F, List<Task>>` который будет создан асинхронно.

Если бы мы использовали фиксированных тип данных вроде `Observable`, `Async.async {}` был бы эквивалентен `Observable.create()`, т.е. созданию операции, которая может быть вызвана из синхронного или асинхронного кода, например `Thread` или `AsyncTask`.

Параметр `callback` используется для связки результирующих колбеков в контекст контейнера `F`, который является высоким типом.

Таким образом наш `RemoteDataSource` абстрагирован и зависит от всё ещё неизвестного контейнера типа `F`.

Поднимемся на уровень абстракции повыше и еще раз взглянем на наш репозиторий. Если ты помнишь, сначала нам необходимо выполнить поиск объектов `Task` в `LocalDataSource`, и только затем (если их не было найдено локально) запросить их из `RemoteLocalDataSource`.

```kotlin
class TaskRepository<F>(
  private val localDS: DataSource<F>,
  private val remoteDS: RemoteDataSource<F>,
  AE: ApplicativeError<F, Throwable>) : ApplicativeError<F, Throwable> by AE {

  fun allTasksByUser(user: User): Kind<F, List<Task>> =
    localDS.allTasksByUser(user).handleErrorWith {
      when (it) {
        is UserNotInLocalStorage -> remoteDS.allTasksByUser(user)
        else -> raiseError(UnknownError(it))
      }
    }
}
```

[`ApplicativeError<F, Throwable>`]({{ '/docs/arrow/typeclasses/applicativeerror/' | relative_url }}) снова с нами! Он также предоставляет функцию `handleErrorWith()`, которая работает поверх любого ресивера высокого типа.

Выглядит она так:

```kotlin
fun <A> Kind<F, A>.handleErrorWith(f: (E) -> Kind<F, A>): Kind<F, A>
```

Т.к. `localDS.allTasksByUser(user)` возвращает `Kind<F, List<Task>>`, который можно рассматривать как `F<List<Task>>`, где `F` остается обобщенным типом, мы можем вызвать `handleErrorWith()` поверх него.

`handleErrorWith()` позволяет реагировать на ошибки используя переданную лямбду. Рассмотрим функцию поближе:

```kotlin
fun allTasksByUser(user: User): Kind<F, List<Task>> =
    localDS.allTasksByUser(user).handleErrorWith {
      when (it) {
        is UserNotInLocalStorage -> remoteDS.allTasksByUser(user)
        else -> raiseError(UnknownError(it))
      }
    }
```

Таким образом мы получаем результат первой операции за исключением случаев, когда было брошено исключение. Исключение будет обработано лямбдой. В случае если ошибка принадлежит к типу `UserNotInLocalStorage`, мы попробуем найти объекты типа `Tasks` в дистанционном `DataSource`. Во всех остальных случаях мы оборачиваем неизвестную ошибку в контейнер типа `F`.

Модуль предоставления зависимостей остается очень похожим на прошлую версию:

```kotlin
class Module<F>(A: Async<F>) {
  private val localDataSource: LocalDataSource<F> = LocalDataSource(A)
  private val remoteDataSource: RemoteDataSource<F> = RemoteDataSource(A)
  val repository: TaskRepository<F> = 
      TaskRepository(localDataSource, remoteDataSource, A)
}
```

Единственное отличие — теперь он абстрактен и зависит от `F`, которая остается полиморфной. Я осознанно не уделил этому внимание, чтобы снизить уровень шума, но [`Async`]({{ '/docs/effects/async/' | relative_url }}) наследуется от [`ApplicativeError`]({{ '/docs/arrow/typeclasses/applicativeerror/' | relative_url }}), поэтому может быть использован как его инстанс на всех уровнях исполнения программы.

### Тестируя полиморфизм

Наконец-то наше приложение **полностью абстрагировано** от использования конкрентных типов данных для контейнеров (`F`) и мы можем сфокусироваться на тестировании полиформизма в рантайме. Мы протестируем один и тот же участок кода передавая в него различные типы данных для типа `F`. Сценарий тот же самый, как когда мы использовали `Observable`.

Программа написана таким образом, что мы полностью избавились от границ абстракций и можем передавать детали имплементации, как пожелается.

Для начала попробуем использовать в качестве контейнера для `F` `Single` из RxJava.

```kotlin
object test {

  @JvmStatic
  fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val singleModule = Module(SingleK.async())
    singleModule.run {
      repository.allTasksByUser(user1).fix().single.subscribe(::println, ::println)
      repository.allTasksByUser(user2).fix().single.subscribe(::println, ::println)
      repository.allTasksByUser(user3).fix().single.subscribe(::println, ::println)
    }
  }
}
```

Совместимости ради Arrow предоставляет обертки для известных библиотечных типов данных. Например, есть удобная обертка [`SingleK`]({{ '/docs/integrations/rx2/' | relative_url }}). Эти обертки **позволяют использовать классы типа совместно с типами данных как высокими типами**.

На консоль будет выведено следующее:

```
[Task(value=LocalTask assigned to user1)]
[Task(value=Remote Task assigned to user2)]
UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user)))
```

Тот же результат будет, если использовать `Observable`. 🎉

Теперь поработаем с `Maybe`, для которой доступна обертка [`MaybeK`]({{ '/docs/integrations/rx2/' | relative_url }}):

```kotlin
@JvmStatic
fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val maybeModule = Module(MaybeK.async())
    maybeModule.run {
      repository.allTasksByUser(user1).fix().maybe.subscribe(::println, ::println)
      repository.allTasksByUser(user2).fix().maybe.subscribe(::println, ::println)
      repository.allTasksByUser(user3).fix().maybe.subscribe(::println, ::println)
    }
}
```

На консоль будет выведен тот же результат, но теперь с использованием другого типа данных:

```kotlin
[Task(value=LocalTask assigned to user1)]
[Task(value=Remote Task assigned to user2)]
UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user)))
```

Что насчет [`ObservableK`]({{ '/docs/integrations/rx2/' | relative_url }}) / [`FlowableK`]({{ '/docs/integrations/rx2/' | relative_url }})? 
Давай попробуем:

```kotlin
object test {

  @JvmStatic
  fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val observableModule = Module(ObservableK.async())
    observableModule.run {
      repository.allTasksByUser(user1).fix().observable.subscribe(::println, ::println)
      repository.allTasksByUser(user2).fix().observable.subscribe(::println, ::println)
      repository.allTasksByUser(user3).fix().observable.subscribe(::println, ::println)
    }

    val flowableModule = Module(FlowableK.async())
    flowableModule.run {
      repository.allTasksByUser(user1).fix().flowable.subscribe(::println)
      repository.allTasksByUser(user2).fix().flowable.subscribe(::println)
      repository.allTasksByUser(user3).fix().flowable.subscribe(::println, ::println)
    }
  }
}
```

Увидим в консоли:

```
[Task(value=LocalTask assigned to user1)]
[Task(value=Remote Task assigned to user2)]
UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user)))

[Task(value=LocalTask assigned to user1)]
[Task(value=Remote Task assigned to user2)]
UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user)))
```

Всё работает, как и ожидалось. 💪

Давай попробуем использовать [`DeferredK`]({{ '/docs/integrations/kotlinxcoroutines/' | relative_url }}), обертку для типа
`kotlinx.coroutines.Deferred`:

```kotlin
object test {

  @JvmStatic
  fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val deferredModule = Module(DeferredK.async())
    deferredModule.run {
      runBlocking {
        try {
          println(repository.allTasksByUser(user1).fix().deferred.await())
          println(repository.allTasksByUser(user2).fix().deferred.await())
          println(repository.allTasksByUser(user3).fix().deferred.await())
        } catch (e: UserNotInRemoteStorage) {
          println(e)
        }
      }
    }
  }
}
```

Как известно, обработку исключений при использовании корутин приходится прописывать явно. Такие детали имплементации, как обработка исключения зависят от используемого типа данных, а поэтому и определяются на высшем уровне абстракции.

Еще раз — тот же результат:

```
[Task(value=LocalTask assigned to user1)]
[Task(value=Remote Task assigned to user2)]
UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user)))
```

В Arrow есть альтернативное API для более утонченного использования [`DeferredK`]({{ '/docs/integrations/kotlinxcoroutines/' | relative_url }}). Оно берет заботу о `runBlocking` и отложенных операциях на себя:

```kotlin
object test {

  @JvmStatic
  fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val deferredModuleAlt = Module(DeferredK.async())
    deferredModuleAlt.run {
      println(repository.allTasksByUser(user1).fix().unsafeAttemptSync())
      println(repository.allTasksByUser(user2).fix().unsafeAttemptSync())
      println(repository.allTasksByUser(user3).fix().unsafeAttemptSync())
    }
  }
}
```

Пример выше оборачивает результат в [`Try`]({{ '/docs/arrow/core/try/ru' | relative_url }}) (т.е., может быть`Success` или  `Failure`).

```
Success(value=[Task(value=LocalTask assigned to user1)])
Success(value=[Task(value=Remote Task assigned to user2)])
Failure(exception=UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user))))
```

Напоследок, давай попробуем использовать такой известный в мире ФП тип данных, как [`IO`]({{ '/docs/effects/io' | relative_url }}).
`IO` существует, чтобы изолировать in/out операции, которые привносят в код нежелательные эффекты, и тем самым делать эти операции чистыми.

```kotlin
object test {

  @JvmStatic
  fun main(args: Array<String>): Unit {
    val user1 = User(UserId("user1"))
    val user2 = User(UserId("user2"))
    val user3 = User(UserId("unknown user"))

    val ioModule = Module(IO.async())
    ioModule.run {
      println(repository.allTasksByUser(user1).fix().attempt().unsafeRunSync())
      println(repository.allTasksByUser(user2).fix().attempt().unsafeRunSync())
      println(repository.allTasksByUser(user3).fix().attempt().unsafeRunSync())
    }
  }
}
```

```
Right(b=[Task(value=LocalTask assigned to user1)])
Right(b=[Task(value=Remote Task assigned to user2)])
Left(a=UserNotInRemoteStorage(user=User(userId=UserId(value=unknown user))))
```

[`IO`]({{ '/docs/effects/io' | relative_url }}) - особенный случай. Он возвращает ошибки или результат успешного выполнения с помощью [`Either<L,R>`]({{ '/docs/arrow/core/either/ru' | relative_url }}) (это другой тип данных). По конвенции, "левая" сторона `Either` содержит в себе ошибки, а "правая" хранит в себе данные, полученные в случае успеха. Именно поэтому результат успеха будет выведен в консоли как `Right(...)`, а неудача, как `Left(...)`.

Но концептуально результат будет тем же.

Всё, с тестированием покончено. Как ты видишь, мы смогли переиспользовать один и тот же участок кода, передавая в него различные типы данных, что сделало нашу программу полностью независимой от использования конкретного типа данных. 

[Код полностью полиморфического приложения можно найти на гитхабе](https://gist.github.com/JorgeCastilloPrz/c0a4604b9a5dedc89be82b13cfcc1315).

### Всё это отлично звучит...но стоит ли оно того?

Выбор всегда за тобой, но есть определенные преимущества, которые ФП привносит в кодовую базу. И о них полезно знать.

* В итоге мы получаем полное разделение ответственностей: то, как данные обрабатываются и композируются (собственно, твоя программа) и отдельно — рантайм. Это значит, что наш код **проще тестировать**.

* Твоя программа так или иначе будет подразумевать использование конкретных абстракций, подоходящих под её задачи. Поэтому естественно она может быть написана без использования ФП. Но средства ФП позволяют разделить декларативные вычисления (операции) от рантайма (и типов им используемых) именно там, где важны детали.

* Композирование твоей программы с помощью алгебр (операций), основанных на абстракциях, позволяет сохранить твою кодовую базу детерминированной и свободной от эффектов (чистой). Если хочется узнать больше о чистоте кода и тому, как это помогает избежать ошибок или неожиданного поведения [можно взглянуть на этот пост](https://medium.com/@JorgeCastilloPr/kotlin-functional-programming-does-it-make-sense-36ad07e6bacf).

* В продолжение сказанного, все сторонние эффекты твоей программы контролируются на высшем уровне абстракции. Эффекты, вызванные деталями имплементации приходят в исполнение программы из единой точки системы (вне высшего уровня программы всё остается чистым). 

* Если ты решишь работать с [классами типа]({{ '/docs/typeclasses/intro' | relative_url }}), то итогом этого станет унифицированное API для всех возможных типов данных. Воспроизводимость способствует глубокому пониманию изначальных концептов (воспроизводимость в данном случае это использование операций вроде `map`, `flatMap`, `fold`, во всех случаях вне зависимости от решаемой проблемы). Естественно, тут многое зависит от билиотек, которые позволяют писать функциональные программы средствами Kotlin, и Arrow - одна из них.

* Эти паттерны **убирают нужду в конкретном фреймворке для реализации DI (инъекции зависимостей)**, т.к., поддерживают все концепци DI "из коробки". За тобой оставется свобода предоставления деталей имплементации чуть позже, эти же детали могут быть заменены с большей прозрачностью, и до этого момента твоя программа не привязана ни к каким деталям сторонним эффектам. Этот подход можно рассматривать как собственно говоря DI, т.к., он основан на предоставлении абстракций, детали имплементации которых предоставляются из верхнего уровня абстракции. 

* В качестве заключения, я бы предложил использовать подход, более подходящий под конкретную задачу. ФП не решит всех твоих проблем, т.к., не существует серебрянной пули, но оно является проверенным временем подходом с кучей преимуществ.

### Дополнительно

Если хочется ближе ознакомиться с **классами типа**, это можно сделать [в документации по ним]({{ '/docs/typeclasses/intro' | relative_url }}). 
Я буду рад, если после прочтения статьи у тебя уложиться в голове, что **они используются, как контракты для композиции полиморфических программ, основанных на абстракции**.

Если есть сомнения, незамедлительно связывайся со мной. Наиболее быстрый способ связи - через мой Twitter: [@JorgeCastilloPR](https://www.twitter.com/JorgeCastilloPR).

Некоторые из озвученных концепций (например, чистота функций) описаны в следующих постах:

* [Kotlin Functional Programming: Does it make sense?](https://medium.com/@JorgeCastilloPr/kotlin-functional-programming-does-it-make-sense-36ad07e6bacf) от [Jorge Castillo](https://www.twitter.com/JorgeCastilloPR))
* [Kotlin purity and Function Memoization](https://medium.com/@JorgeCastilloPr/kotlin-purity-and-function-memoization-b12ab35d70a5) от [Jorge Castillo](https://www.twitter.com/JorgeCastilloPR))

Также советую посмотреть видео [FP to the max](https://youtu.be/sxudIMiOo68) от [John De Goes](https://twitter.com/jdegoes) и ознакомиться с примером `FpToTheMax.kt`, расположенным в модуле `arrow-examples`. Использование данной техники может показаться чрезмерным для такого простого примера, но это потмоу, что она должна быть использована на праграммах намного большего масштаба.