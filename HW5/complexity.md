# Анализ сложности

`n = degrees.size`

## VentilationVanilla

```scala 3
degrees
  .sliding(k) // создает O(n - k) итераторов (по сути, просто указатели, поэтому копирований нет) 
  .map(_.max) // для каждого итератора проходится по k элементам
  .toList
```

Итого: `O(n - k) * O(k) = O(n*k)`

## VentilationOptimized

`AQueue#enqueue` и `AQueue#dequeEnqueue` работают за амортизированную константу:
очевидно, что это правда в случае `enqueue` и `dequeueEnqueue` в ветке `case _ :: tail2 =>`, 
где не происходит ничего сложнее получения первого элемента списка
(использующегося тут как стэк) или добавления элемента в начало. Но ветка с применением `rebalance`
также обеспечивает амортизированную константу: время исполнения `rebalance` линейно 
(ничего сложнее функций на коллекции: `scanLeft`) и при этом каждый элемент может в нем участвовать
только один раз (так как дальше переходит в `stackOut`) за время своего "жизненного цикла".

```scala 3
val firstWindow = degrees
  .slice(0, k)
  .map(enqueue)
val startState = for {
  _ <- sequence(firstWindow)  // объединили k операций enqueue
  max <- getMax               // O(1)
} yield max

val restState = degrees
  .slice(k, degrees.size)
  .map { elem =>
    for {
      _ <- dequeEnqueue(elem) // O(1)
      max <- getMax
    } yield max
  }                           // n-k операций
val initQueue = AQueue.of()
sequence(startState :: restState).run(initQueue).value._2.map(_.get)  // запустили вычисление
```

Время работы: `O(n)`
Дополнительная память: в начале в очередь добавляется `k` элементов операцией `enqueue`,
далее размер очереди остается постоянным при использовании `dequeEnqueue`.