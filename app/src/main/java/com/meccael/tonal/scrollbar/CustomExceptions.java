package com.meccael.tonal.scrollbar;

import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
class CustomExceptions {

    static class AdapterNotSetupForCustomScrollingException extends RuntimeException {
        AdapterNotSetupForCustomScrollingException(Class cls) {
            super("In order to use custom scrolling, the adapter for your recyclerView, " + cls.getName() + ", MUST implement CustomScroller.");
        }
    }

    static class AdapterNotSetupForIndicatorException extends RuntimeException {
        AdapterNotSetupForIndicatorException(Class cls, String str) {
            super("In order to add this indicator, the adapter for your recyclerView, " + cls.getName() + ", MUST implement " + str + ".");
        }
    }

    static class MissingAttributesException extends RuntimeException {
        MissingAttributesException(ArrayList<String> arrayList) {
            super("You are missing the following required attributes from a scroll bar in your XML: " + arrayList);
        }
    }

    static class UnsupportedParentException extends RuntimeException {
        UnsupportedParentException() {
            super("The recyclerView which is associated with a programmatically added scroll bar must be the child of a relative layout.");
        }
    }

    CustomExceptions() {
    }
}
