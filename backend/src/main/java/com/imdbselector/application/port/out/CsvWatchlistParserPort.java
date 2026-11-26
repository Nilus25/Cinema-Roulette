package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.MediaItem;

import java.io.InputStream;
import java.util.List;

public interface CsvWatchlistParserPort {
    List<MediaItem> parse(InputStream csvStream);
}
