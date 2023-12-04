package org.jackhuang.CU.util.gson;

public @interface JsonSubtype {
    Class<?> clazz();

    String name();
}
