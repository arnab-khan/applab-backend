package com.applab.applab_backend.common.views;

public class SerializationJsonViews {
    // Fields visible to all (for API responses)
    public static class MyClass {
    }

    // Fields visible only internally
    public static class MyChild extends MyClass {
    }
}
