package com.pefrormance.analyzer.export;

import java.util.Collection;

/**
 * Created by konstantin on 22.12.2019.
 */
public interface Exporter
{
    // probably could be removed
    void init ();

    void export (Collection<String> data);
}
