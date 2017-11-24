package com.ilusons.harmony.ref;

public class JavaEx {

    public interface Action {
        void execute();
    }

    public interface ActionT<T> {
        void execute(T t);
    }

    public interface ActionExT<T> {
        void execute(T t) throws Exception;
    }

    public interface ActionTU<T, U> {
        void execute(T t, U u);
    }

    public interface ActionTUV<T, U, V> {
        void execute(T t, U u, V v);
    }

    public interface Function<TReturn> {
        TReturn execute();
    }

    public interface FunctionT<T, TReturn> {
        TReturn execute(T t);
    }

}
