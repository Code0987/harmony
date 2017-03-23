package com.ilusons.harmony.ref;

public class JavaEx {

    public interface Action {
        void execute();
    }

    public interface ActionT<T> {
        void execute(T t);
    }

    public interface Function<TReturn> {
        TReturn execute();
    }

    public interface FunctionT<T, TReturn> {
        TReturn execute(T t);
    }

}
