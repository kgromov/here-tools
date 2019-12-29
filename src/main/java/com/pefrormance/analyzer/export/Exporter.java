package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by konstantin on 22.12.2019.
 */
public interface Exporter
{

    void init(Settings settings) throws IOException;

    void export (String product, Collection<String> data) throws IOException;
}
