package xyz.ajp.makezoomzoom.asmutil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {
    public static Object readField(Object instance, String fieldName) {
        try {
            Field f = instance.getClass().getDeclaredField(fieldName);
            ((Field) f).setAccessible(true);
            return f.get(instance);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Unused used to define class as static. In case you want to access a private field on the Class interface (are there any?)

    public static Object readField(Class declaredClass, String fieldName, boolean unused) {
        try {
            Field f = declaredClass.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void writeField(Object instance, String fieldName, Object value) {
        try {
            Field f = instance.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(instance, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void writeField(Class instance, String fieldName, Object value, boolean unused) {
        try {
            Field f = instance.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static Object callFunction(Object instance, Method method, Object... value) {
        if ( method == null ) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(instance, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
