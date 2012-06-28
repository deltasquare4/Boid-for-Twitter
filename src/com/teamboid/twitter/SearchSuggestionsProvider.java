package com.teamboid.twitter;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.teamboid.twitter.SearchSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;
    public SearchSuggestionsProvider() { setupSuggestions(AUTHORITY, MODE); }
}