# Решение ДЗ [курса Т-Банка](https://education.tbank.ru/study/fintech_middle/all_to_scala/)

По просьбе преподавателей не были добавлены оргинальные условия заданий и их тесты.

## HW5

Задание состояло из 2-ух подзаданий:

- [tree](HW5/src/main/scala/tree/NullableTree.scala) и [самописные тесты](HW5/src/test/scala/tree/NullableTreeSpec.scala)
    Написать бинарное дерево (несбалансированное)
- [ventialtion](HW5/src/main/scala/ventilation/Ventilation.scala) и [самописные тесты](HW5/src/test/scala/ventilation/VentilationSpec.scala)
    Написать 2 решения алгоритмической задачи: простое на основе методов на
    стандартных коллекциях Scala и оптимизированное на основе работы с двумя
    очередями. Важно, что так как очереди должны были быть иммутабельны,
    была применена `State` монада. Асимптотика при этом только улучшилась:
    [complexity.md](HW5/complexity.md)

## HW7

Задание состояло из 2-ух подзаданий:

- [decoder](HW7/src/main/scala/unmarshal/decoder/Decoder.scala) 
    Написать API для декодирования json. Для избежания бойлерплейта и
    проверки на лишние поля был использован `EitherT[ParsingState, DecoderError, A]`,
    где `type ParsingState[A] = State[Map[String, Json], A]`.
    Это позволило добиться такого API декодирования:
    ```scala
    given employeeDecoder: Decoder[Employee] = objectDecoder {
        for {
            name   <- getField[String]("name")
            age    <- getField[Long]("age")
            id     <- getField[Long]("id")
            bossId <- getOptionField[Long]("bossId")
        } yield Employee(name, age, id, bossId)
    }
    ```
- [encoder](HW7/src/main/scala/unmarshal/encoder/Encoder.scala) 
    При помощи механизма `Type Class Derivation` в Scala 3 написать
    самовыводящийся для всех `case class`'ов encoder json'ов

## HW9

Написать свою урезанную версию `IO`: [MyIO.scala](HW9/src/main/scala/io/MyIO.scala)

## Проект

Написание бэкенда, используя систему эффектов. На данный момент есть
[начальная версия авторизации](Project/gateway/src/main/scala/Main.scala).

Будет дополняться.

## Мотивация участия в конференциях

Слежу за митапами по Scala, чтобы узнать о тенденциях в индустрии, узнать
о новых подходах к существующим проблемам.

Одни из моих любимых докладов:

- [Иван Лягаев о передаче контекста запроса внутри приложения](https://youtu.be/fR0hj-Lz1Vc?si=7lAxe62oPfGjBaN_)
    Иван является лектором на нашем курсе и его доклад также будет на `F[Scala] 2024`,
    поэтому очень хотел бы его увидеть
- [Олег Нижников о системах эффектов](https://youtu.be/jRVvj_2yWkE?si=sZSopD5q6bZ_l73o)
- [How 1 Software Engineer Outperforms 138 - Lichess Case Study](https://youtu.be/7VSVfQcaxFY?si=vXhtOUUblKodn1pE)
    Не совсем доклад, но хорошая реклама поддерживаемости кода на Scala
- [Объяснение виртуальных потоков в Java 21 от официального канала](https://youtu.be/5E0LU85EnTI?si=UYwYa1YsG7Ph7k_Y)