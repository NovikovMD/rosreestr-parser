package org.example;

import java.util.function.Supplier;

public class ExceptionRepeater {
    private ExceptionRepeater() {
    }

    public static <T> T aroundInvoke(Supplier<T> task) throws Exception {
        byte repeater = 5;
        Exception thrown = new RuntimeException("Какая-то неизвестная ошибка");
        while (repeater > 0) {
            try {
                return task.get();
            } catch (Exception exception) {
                thrown = exception;
            }

            repeater--;

            if (repeater > 0) {
                System.err.printf("Выполняется повтор загрузки%n");
            }
        }

        System.err.println("Критичная ошибка. Программа будет экстренно закрыта");
        throw thrown;
    }

}
